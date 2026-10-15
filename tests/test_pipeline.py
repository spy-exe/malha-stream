import unittest
from benchmark.pipeline import pipeline_report

class PipelineTest(unittest.TestCase):
    def test_rates_and_jitter_are_internal_and_visible_fps_unknown(self):
        rows=[]
        for number in range(3):
            rows.append({'event':'submit_picture','id':number+1,'monotonic_ns':number*1_000_000_000,'value':number})
            rows.append({'event':'release_call','id':number+1,'monotonic_ns':number*1_000_000_000+10_000_000,'value':0})
        report=pipeline_report(rows,{'clock':'System.nanoTime; local device only','overwritten_events':0})
        self.assertEqual(report['stage_fps']['submit_picture'],1)
        self.assertEqual(report['interarrival_ms']['sample_stdev_ms'],0)
        self.assertIsNone(report['visible_fps'])
        self.assertIsNone(report['input_to_photon_ms'])
        self.assertFalse(report['sampling_ready'])
    def test_loss_and_overwritten_events_remain_visible(self):
        rows=[{'event':'frame_loss','id':1,'monotonic_ns':1,'value':5},
              {'event':'output_drop','id':2,'monotonic_ns':2,'value':0}]
        report=pipeline_report(rows,{'clock':'System.nanoTime','overwritten_events':20})
        self.assertEqual(report['network_frames_lost'],5)
        self.assertEqual(report['decoded_outputs_dropped'],1)
        self.assertEqual(report['overwritten_events'],20)
    def test_different_clock_origin_rejected(self):
        with self.assertRaises(ValueError):pipeline_report([],{'clock':'host wall clock'})
