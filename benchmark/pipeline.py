from .analysis import summarize

def pipeline_report(rows, metadata):
    if not metadata.get('clock','').startswith('System.nanoTime'):
        raise ValueError('Known local monotonic clock required')
    stages={name:[] for name in ('submit_picture','output_available','release_call','surface_render_callback')}
    lost=dropped=0
    for row in rows:
        name=row['event']
        if name in stages and int(row['id'])>0:
            stages[name].append(int(row['monotonic_ns']))
        elif name=='frame_loss':lost+=max(0,int(row['value']))
        elif name=='output_drop':dropped+=1
    rates={}
    for name,times in stages.items():
        times.sort()
        rates[name]=(len(times)-1)*1e9/(times[-1]-times[0]) if len(times)>1 and times[-1]>times[0] else None
    arrivals=stages['submit_picture']
    intervals=[(right-left)/1e6 for left,right in zip(arrivals,arrivals[1:])]
    duration=(arrivals[-1]-arrivals[0])/1e9 if len(arrivals)>1 else 0
    return {'test_type':'internal pipeline telemetry','clock':'local monotonic only',
            'stage_fps':rates,'visible_fps':None,'input_to_photon_ms':None,
            'interarrival_ms':summarize(intervals),'observed_input_span_s':duration,
            'network_frames_lost':lost,'decoded_outputs_dropped':dropped,
            'overwritten_events':metadata.get('overwritten_events'),
            'sampling_ready':duration>=120 and metadata.get('overwritten_events')==0,
            'rate_note':'Counts submitted/decoded/released frames or reported callbacks, never visible FPS'}
