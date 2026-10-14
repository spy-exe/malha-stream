"""Collect an informational trace from an already authorized TV, without root."""
import argparse
from pathlib import Path
import subprocess
import uuid
from benchmark.__main__ import probe, adb

parser=argparse.ArgumentParser()
parser.add_argument('--serial',required=True)
parser.add_argument('--seconds',type=int,default=120)
parser.add_argument('--out',type=Path,required=True)
args=parser.parse_args()
if args.seconds<1 or args.seconds>600:raise SystemExit('Trace duration must be 1..600 seconds')
if not probe(args.serial)['target_device']:raise SystemExit('Authorized TV target required')
remote='/data/local/tmp/malha-'+uuid.uuid4().hex+'.perfetto-trace'
config=f'''buffers {{ size_kb: 32768 fill_policy: DISCARD }}
duration_ms: {args.seconds*1000}
data_sources {{ config {{ name: "linux.ftrace" ftrace_config {{
  ftrace_events: "sched/sched_switch"
  ftrace_events: "sched/sched_waking"
  ftrace_events: "power/cpu_frequency"
  atrace_categories: "gfx"
  atrace_categories: "view"
  atrace_apps: "app.malhastream.debug"
}} }} }}
data_sources {{ config {{ name: "android.surfaceflinger.frametimeline" }} }}
'''
result=subprocess.run(['adb','-s',args.serial,'shell','perfetto','--txt','-c','-','-o',remote],
                      input=config.encode(),capture_output=True)
if result.returncode or b'not found' in result.stderr or b'Permission denied' in result.stderr:
    raise SystemExit('BLOCKED: Perfetto unavailable or permissions insufficient; no device security changes made')
args.out.parent.mkdir(parents=True,exist_ok=True)
subprocess.run(['adb','-s',args.serial,'pull',remote,str(args.out)],check=True)
print('Informational device trace collected. Inspect trace stats/data-source support before interpreting.')
