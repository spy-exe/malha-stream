"""Optical ROI candidates from an actual recording. Manual audit remains required."""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
from .events import EventMatcher

def roi(value):
    values=tuple(int(part) for part in value.split(','))
    if len(values)!=4 or min(values)<0 or values[2]==0 or values[3]==0:
        raise argparse.ArgumentTypeError('ROI must be x,y,width,height with positive size')
    return values

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--video',type=Path,required=True)
    parser.add_argument('--led-roi',type=roi,required=True)
    parser.add_argument('--screen-roi',type=roi,required=True)
    parser.add_argument('--threshold',type=float,default=127)
    parser.add_argument('--capture-fps',type=float,required=True)
    parser.add_argument('--max-ms',type=float,default=2000)
    parser.add_argument('--recording-id',required=True)
    parser.add_argument('--session-id',required=True)
    parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args()
    import cv2
    if (not all(math.isfinite(value) for value in (args.capture_fps,args.max_ms,args.threshold))
            or args.capture_fps<=0 or args.max_ms<=0 or not 0<=args.threshold<=255):
        raise SystemExit('Finite positive capture FPS/timeout and threshold 0..255 required')
    capture=cv2.VideoCapture(str(args.video))
    if not capture.isOpened():raise SystemExit('Raw recording could not be opened')
    matcher=EventMatcher(max(1,round(args.capture_fps*args.max_ms/1000)))
    frame_number=0
    try:
        while True:
            ok,frame=capture.read()
            if not ok:break
            gray=cv2.cvtColor(frame,cv2.COLOR_BGR2GRAY)
            brightness=[]
            for x,y,width,height in (args.led_roi,args.screen_roi):
                if x+width>gray.shape[1] or y+height>gray.shape[0]:raise ValueError('ROI exceeds frame dimensions')
                brightness.append(float(gray[y:y+height,x:x+width].mean())>args.threshold)
            matcher.feed(frame_number,*brightness)
            frame_number+=1
    finally:capture.release()
    matcher.finish()
    if frame_number==0:raise SystemExit('No decoded camera frames')
    args.out.parent.mkdir(parents=True,exist_ok=True)
    with args.out.open('w',newline='') as stream:
        writer=csv.writer(stream)
        writer.writerow(['recording_id','session_id','input_frame','photon_frame','camera_fps'])
        for first,photon in matcher.valid:
            writer.writerow([args.recording_id,args.session_id,first,photon,args.capture_fps])
    with args.video.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    report={'status':'BLOCKED','dependency':'Review optical edges, physical input reference and capture FPS calibration',
            'video_sha256':digest,'decoded_frames':frame_number,'capture_fps_declared':args.capture_fps,
            'valid_candidate_events':len(matcher.valid),'invalid_events':matcher.invalid,
            'led_roi':args.led_roi,'screen_roi':args.screen_roi,'threshold':args.threshold,
            'max_ms':args.max_ms,'input_to_photon_verified':False}
    args.out.with_suffix('.audit.json').write_text(json.dumps(report,indent=2)+'\n')

if __name__=='__main__':main()
