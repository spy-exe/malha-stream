import math
import random
import statistics


def percentile(values, quantile):
    if not values:
        return None
    ordered = sorted(values)
    index = quantile * (len(ordered) - 1)
    lower, upper = math.floor(index), math.ceil(index)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (index - lower)


def summarize(values, allow_negative=False):
    if any(not math.isfinite(value) or (value < 0 and not allow_negative) for value in values):
        raise ValueError('Finite nonnegative measurements required')
    interval = None
    if values:
        rng = random.Random(20261014)
        medians = [statistics.median(rng.choices(values, k=len(values))) for _ in range(400)]
        interval = [percentile(medians, .025), percentile(medians, .975)]
    return {'n': len(values), 'p50_ms': percentile(values, .5), 'p90_ms': percentile(values, .9),
            'p95_ms': percentile(values, .95) if len(values) >= 100 else None,
            'p99_ms': percentile(values, .99) if len(values) >= 1000 else None,
            'sample_stdev_ms': statistics.stdev(values) if len(values) > 1 else None,
            'median_bootstrap_95_ci_ms': interval,
            'ci_note': 'Exploratory event bootstrap; does not account for correlation within sessions or camera uncertainty',
            'tail_policy': 'p95 requires n>=100; p99 requires n>=1000'}


def camera_latencies(rows, measurement='input-to-photon'):
    values = []
    for row in rows:
        first, photon, fps = (float(row[key]) for key in ('input_frame', 'photon_frame', 'camera_fps'))
        if (not all(math.isfinite(value) for value in (first, photon, fps)) or fps <= 0 or first < 0 or photon < 0
                or (photon < first and measurement != 'display-relative')):
            raise ValueError('Invalid external camera measurement')
        if first != int(first) or photon != int(photon):
            raise ValueError('Frame indices must be integers')
        values.append((photon - first) * 1000.0 / fps)
    return values


def compare(baseline, candidate):
    if baseline['config'] != candidate['config']:
        raise ValueError('Comparable stream, device, host, scene and environment configuration required')
    if baseline['measurement'] != candidate['measurement']:
        raise ValueError('Measurement types differ; cannot compare')
    relative = baseline['measurement']=='display-relative'
    before, after = summarize(baseline['samples_ms'], relative), summarize(candidate['samples_ms'], relative)
    gain = None
    if not relative and before['p50_ms'] is not None and before['p50_ms'] > 0 and after['p50_ms'] is not None:
        gain = (before['p50_ms'] - after['p50_ms']) / before['p50_ms'] * 100
    tail_change = None
    if before['p95_ms'] is not None and after['p95_ms'] is not None:
        tail_change = after['p95_ms'] - before['p95_ms']
    return {'status': 'BLOCKED' if before['n']==0 or after['n']==0 else 'BENCHMARKED',
            'measurement': baseline['measurement'], 'baseline': before, 'candidate': after,
            'gain_percent': gain, 'p95_change_ms': tail_change,
            'regression': gain is not None and (gain < 0 or (tail_change is not None and tail_change > 0)),
            'improvement_verified': False,
            'acceptance_review_required': ['external input reference and raw recordings',
                                           'three sessions and >=120 seconds telemetry per session',
                                           'camera uncertainty and session correlation',
                                           'p95, stutter, artifacts, audio, controllers and long-session stability']}


def schedule(variants, sessions):
    if len(variants) < 2 or sessions < 3:
        raise ValueError('At least two variants and three sessions required')
    planned = []
    for session in range(1, sessions + 1):
        ordered = variants if session % 2 else list(reversed(variants))
        for variant in ordered:
            planned.append({'variant': variant, 'session': session,
                            'duration_s': 120, 'physical_events_min': 30, 'status': 'PENDING'})
    return planned
