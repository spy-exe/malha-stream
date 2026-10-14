import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess
import sys
from .analysis import camera_latencies, compare, summarize, schedule

PACKAGE = 'app.malhastream.debug'


def adb(serial, *args):
    if not re.fullmatch(r'[A-Za-z0-9_.:-]+', serial):
        raise ValueError('Invalid ADB serial')
    result = subprocess.run(['adb','-s',serial,*args],capture_output=True,check=True)
    return result.stdout


def shell(serial, *args):
    # ADB shell combines its arguments into one shell command: quote explicitly.
    return adb(serial,'shell',' '.join(shlex.quote(str(arg)) for arg in args)).decode().strip()


def probe(serial):
    if adb(serial,'get-state').decode().strip() != 'device':
        raise ValueError('ADB device is not authorized')
    properties = {}
    for name in ('ro.product.manufacturer','ro.product.model','ro.product.device','ro.hardware',
                 'ro.board.platform','ro.build.version.sdk','ro.build.version.release','ro.soc.model'):
        properties[name] = shell(serial,'getprop',name)
    properties['leanback'] = 'android.software.leanback' in shell(serial,'pm','list','features')
    properties['target_device'] = properties['leanback'] or properties['ro.product.model'].startswith('AFT')
    return properties


def write(path, value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,allow_nan=False)+'\n')


def main():
    parser = argparse.ArgumentParser(description='Malha benchmark: plans, authorized ADB collection and external measurements')
    sub = parser.add_subparsers(dest='command',required=True)
    plan = sub.add_parser('plan')
    plan.add_argument('--device',required=True)
    plan.add_argument('--codec',choices=['h264','hevc'],default='h264')
    plan.add_argument('--resolution',choices=['1080p','720p'],default='1080p')
    plan.add_argument('--fps',type=int,choices=[60],default=60)
    plan.add_argument('--bitrate-kbps',type=int,default=20000)
    plan.add_argument('--profile',choices=['upstream','lowest-latency','balanced','amlogic-hevc-no-low-latency'],default='upstream')
    plan.add_argument('--out',type=Path,required=True)
    for name in ('probe','prepare','collect'):
        command = sub.add_parser(name)
        command.add_argument('--serial',required=True)
        command.add_argument('--out',type=Path,required=True)
        if name=='prepare':command.add_argument('--plan',type=Path,required=True)
    physical = sub.add_parser('import-camera')
    physical.add_argument('--csv',type=Path,required=True)
    physical.add_argument('--metadata',type=Path,required=True)
    physical.add_argument('--out',type=Path,required=True)
    comparison = sub.add_parser('compare')
    comparison.add_argument('baseline',type=Path)
    comparison.add_argument('candidate',type=Path)
    comparison.add_argument('--out',type=Path,required=True)
    args=parser.parse_args()
    if args.command=='plan':
        if not 500<=args.bitrate_kbps<=150000:raise ValueError('Bitrate out of supported range')
        if args.profile=='amlogic-hevc-no-low-latency' and args.codec!='hevc':
            raise ValueError('Amlogic experiment requires HEVC')
        result={'status':'BLOCKED','dependency':'Authorized device, stream sessions and external camera',
                'test_type':'planned hardware and external-camera measurements',
                'config':{'device_label':args.device,'codec':args.codec,'resolution':args.resolution,
                          'fps':args.fps,'bitrate_kbps':args.bitrate_kbps,'hdr':False},
                'profile':args.profile,'schedule':schedule(['A-official','B1-artemis','B2-vplus','C-diagnostic','D-experiment','E-final'],3),
                'variant_note':'D and E remain pending; do not label a candidate approved before measurements',
                'metrics':None}
        write(args.out,result)
    elif args.command=='probe':
        write(args.out,{'test_type':'authorized ADB inventory','device':probe(args.serial),
                        'status':'BLOCKED','dependency':'APK execution, stream and physical benchmark not yet verified'})
    elif args.command=='prepare':
        device=probe(args.serial)
        if not device['target_device']:raise ValueError('Device is not recognized as authorized TV target')
        plan=json.loads(args.plan.read_text())
        config=plan['config']
        if (config['resolution'] not in ('1080p','720p') or config['codec'] not in ('h264','hevc')
                or config['fps']!=60 or not 500<=config['bitrate_kbps']<=150000 or config['hdr'] is not False
                or plan['profile'] not in ('upstream','lowest-latency','balanced','amlogic-hevc-no-low-latency')
                or (plan['profile']=='amlogic-hevc-no-low-latency' and config['codec']!='hevc')):
            raise ValueError('Plan contains unsupported settings')
        output=shell(args.serial,'am','start','-n',PACKAGE+'/com.limelight.diagnostics.BenchmarkActivity',
                     '--es','command','configure','--es','resolution',
                     '1920x1080' if config['resolution']=='1080p' else '1280x720',
                     '--es','codec',config['codec'],'--es','profile',plan['profile'],
                     '--ei','fps',config['fps'],'--ei','bitrate_kbps',config['bitrate_kbps'])
        if 'Error' in output or 'Exception' in output:raise ValueError('APK configuration activity failed')
        write(args.out,{'status':'BLOCKED','test_type':'automated configuration', 'device':device,
                        'config':config,'profile':plan['profile'],
                        'dependency':'Start same scene manually; record >=120s, then close session and collect',
                        'physical_metrics':None})
    elif args.command=='collect':
        device=probe(args.serial)
        names=shell(args.serial,'run-as',PACKAGE,'ls','files/benchmarks').splitlines()
        args.out.mkdir(parents=True,exist_ok=True)
        collected=[]
        for name in names:
            if not re.fullmatch(r'[a-f0-9-]{36}\.(json|csv)',name):continue
            data=adb(args.serial,'exec-out','run-as',PACKAGE,'cat','files/benchmarks/'+name)
            (args.out/name).write_bytes(data)
            collected.append({'name':name,'sha256':hashlib.sha256(data).hexdigest()})
        write(args.out/'collection.json',{'test_type':'real-device telemetry export','device':device,
                                         'files':collected,'input_to_photon_ms':None,
                                         'status':'DEVICE VERIFIED' if collected else 'BLOCKED',
                                         'scope':'Internal diagnostic events only; inspect duration, losses, session closure and configuration'})
    elif args.command=='import-camera':
        metadata=json.loads(args.metadata.read_text())
        if metadata.get('measurement') not in ('input-to-photon','display-relative'):
            raise ValueError('Explicit external measurement type required')
        if not metadata.get('recordings') or not metadata.get('input_reference'):
            raise ValueError('Raw recordings and observed input reference required')
        for recording in metadata['recordings']:
            path=args.metadata.parent/recording['path']
            with path.open('rb') as source:actual=hashlib.file_digest(source,'sha256').hexdigest()
            if actual!=recording['sha256']:raise ValueError('Raw recording checksum mismatch')
        with args.csv.open(newline='') as source:rows=list(csv.DictReader(source))
        recordings={recording['id']:recording for recording in metadata['recordings']}
        for row in rows:
            recording=recordings[row['recording_id']]
            if float(row['camera_fps'])!=float(recording['capture_fps']):
                raise ValueError('Annotation FPS differs from raw recording capture FPS')
        values=camera_latencies(rows,metadata['measurement'])
        sessions=sorted(set(row['session_id'] for row in rows))
        counts={session:sum(row['session_id']==session for row in rows) for session in sessions}
        result={'status':'BLOCKED','dependency':'Matched baseline/candidate comparison and recording review',
                'measurement_state':'COLLECTED','test_type':'external-camera annotations',
                'config':metadata['config'],'measurement':metadata['measurement'],'samples_ms':values,
                'statistics':summarize(values,metadata['measurement']=='display-relative'),'session_counts':counts,
                'sample_requirement_met':len(sessions)>=3 and all(count>=30 for count in counts.values()),
                'camera_quantization_conservative_ms':max((2000/float(row['camera_fps']) for row in rows),default=None),
                'provenance':metadata,'improvement_verified':False}
        write(args.out,result)
    elif args.command=='compare':
        result=compare(json.loads(args.baseline.read_text()),json.loads(args.candidate.read_text()))
        write(args.out,result)
        if result['regression']:return 2
    return 0


if __name__=='__main__':
    try:sys.exit(main())
    except (ValueError,OSError,subprocess.CalledProcessError) as error:
        print(json.dumps({'status':'BLOCKED','reason':str(error)}),file=sys.stderr)
        sys.exit(3)
