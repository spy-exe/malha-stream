import unittest
from benchmark.analysis import camera_latencies, compare, summarize, schedule


class AnalysisTest(unittest.TestCase):
    def test_frame_latency_uses_single_external_camera_clock(self):
        data = [{'input_frame': '10', 'photon_frame': '22', 'camera_fps': '240'}]
        self.assertEqual(camera_latencies(data), [50.0])
        with self.assertRaises(ValueError):
            camera_latencies([{'input_frame': '20', 'photon_frame': '19', 'camera_fps': '240'}])
        with self.assertRaises(ValueError):
            camera_latencies([{'input_frame': '1', 'photon_frame': '2', 'camera_fps': '0'}])
        with self.assertRaises(ValueError):
            camera_latencies([{'input_frame': 'nan', 'photon_frame': '2', 'camera_fps': '240'}])

    def test_missing_and_small_samples_do_not_invent_tails(self):
        self.assertIsNone(summarize([])['p50_ms'])
        result = summarize([1, 3, 2])
        self.assertEqual(result['p50_ms'], 2)
        self.assertIsNone(result['p95_ms'])
        self.assertIsNone(result['p99_ms'])
        self.assertEqual(summarize(list(range(100)))['p95_ms'], 94.05)
        self.assertAlmostEqual(summarize(list(range(1000)))['p99_ms'], 989.01)

    def test_mismatched_quality_cannot_be_compared(self):
        baseline = {'config': {'resolution': '1080p', 'codec': 'h264'}, 'measurement': 'input-to-photon', 'samples_ms': [100]*100}
        candidate = {'config': {'resolution': '720p', 'codec': 'h264'}, 'measurement': 'input-to-photon', 'samples_ms': [50]*100}
        with self.assertRaises(ValueError): compare(baseline, candidate)
        candidate['config'] = baseline['config']
        candidate['measurement'] = 'display-relative'
        with self.assertRaises(ValueError): compare(baseline, candidate)

    def test_internal_latency_never_verifies_input_to_photon(self):
        baseline = {'config': {'codec': 'h264'}, 'measurement': 'queue-to-output', 'samples_ms': [10]*100}
        candidate = {'config': baseline['config'], 'measurement': 'queue-to-output', 'samples_ms': [5]*100}
        result = compare(baseline, candidate)
        self.assertEqual(result['gain_percent'], 50)
        self.assertEqual(result['status'], 'BENCHMARKED')
        self.assertFalse(result['improvement_verified'])

    def test_regression_is_visible_and_missing_data_is_blocked(self):
        baseline = {'config': {}, 'measurement': 'input-to-photon', 'samples_ms': [50]*100}
        candidate = {'config': {}, 'measurement': 'input-to-photon', 'samples_ms': [100]*100}
        self.assertEqual(compare(baseline, candidate)['gain_percent'], -100)
        self.assertTrue(compare(baseline, candidate)['regression'])
        candidate['samples_ms'] = []
        self.assertEqual(compare(baseline, candidate)['status'], 'BLOCKED')

    def test_schedule_interleaves_three_sessions_per_variant(self):
        planned = schedule(['A', 'C'], 3)
        self.assertEqual([row['variant'] for row in planned], ['A', 'C', 'C', 'A', 'A', 'C'])
        self.assertTrue(all(row['duration_s'] == 120 for row in planned))
        self.assertTrue(all(row['status'] == 'PENDING' for row in planned))
        with self.assertRaises(ValueError): schedule(['A'], 3)
        with self.assertRaises(ValueError): schedule(['A','B'], 0)
