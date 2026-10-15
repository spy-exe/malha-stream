import unittest
from benchmark.events import EventMatcher

class EventMatcherTest(unittest.TestCase):
    def test_matches_delayed_and_same_frame_edges(self):
        matcher=EventMatcher(10)
        matcher.feed(0,False,False)
        matcher.feed(1,True,False)
        matcher.feed(4,True,True)
        matcher.feed(5,False,False)
        matcher.feed(6,True,True)
        self.assertEqual(matcher.valid,[(1,4),(6,6)])
        self.assertEqual(matcher.invalid,[])
    def test_does_not_invent_event_from_initial_brightness(self):
        matcher=EventMatcher(10)
        matcher.feed(0,True,True)
        matcher.feed(1,True,True)
        self.assertEqual(matcher.valid,[])
    def test_reports_timeout_and_overlapping_inputs(self):
        matcher=EventMatcher(2)
        matcher.feed(0,False,False)
        matcher.feed(1,True,False)
        matcher.feed(4,False,False)
        self.assertEqual(matcher.invalid,[{'input_frame':1,'reason':'timeout'}])
        matcher=EventMatcher(10)
        for row in [(0,False,False),(1,True,False),(2,False,False),(3,True,False),(4,True,True)]:
            matcher.feed(*row)
        self.assertEqual(matcher.valid,[])
        self.assertEqual(len(matcher.invalid),2)
    def test_records_unfinished_trial_and_validates_frame_order(self):
        matcher=EventMatcher(2)
        matcher.feed(0,False,False);matcher.feed(1,True,False);matcher.finish()
        self.assertEqual(matcher.invalid,[{'input_frame':1,'reason':'recording_ended'}])
        with self.assertRaises(ValueError):matcher.feed(0,False,False)
        with self.assertRaises(ValueError):EventMatcher(0)
