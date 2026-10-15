class EventMatcher:
    """Provisional optical edges; retain exclusions and require recording review."""
    def __init__(self,max_frames):
        if max_frames<1:raise ValueError('Positive timeout required')
        self.max_frames=max_frames
        self.previous_led=None
        self.previous_screen=None
        self.frame=-1
        self.pending=None
        self.cooldown=-1
        self.valid=[]
        self.invalid=[]
    def feed(self,frame,led,screen):
        if frame<=self.frame:raise ValueError('Increasing frame indices required')
        self.frame=frame
        if self.previous_led is None:
            self.previous_led=led;self.previous_screen=screen
            return
        led_edge=led and not self.previous_led
        screen_edge=screen and not self.previous_screen
        if self.pending is not None and frame-self.pending>self.max_frames:
            self.invalid.append({'input_frame':self.pending,'reason':'timeout'})
            self.pending=None
        if led_edge:
            if self.pending is not None:
                self.invalid.append({'input_frame':self.pending,'reason':'overlapping_inputs'})
                self.invalid.append({'input_frame':frame,'reason':'overlapping_inputs'})
                self.pending=None
                self.cooldown=frame+self.max_frames
            elif frame<=self.cooldown:
                self.invalid.append({'input_frame':frame,'reason':'overlap_cooldown'})
            else:self.pending=frame
        if screen_edge and self.pending is not None:
            self.valid.append((self.pending,frame));self.pending=None
        self.previous_led=led;self.previous_screen=screen
    def finish(self):
        if self.pending is not None:
            self.invalid.append({'input_frame':self.pending,'reason':'recording_ended'})
            self.pending=None
