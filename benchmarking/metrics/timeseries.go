package metrics

import (
	"sort"
	"sync"
	"time"
)

// TimeSeriesPoint records a per-second snapshot of test metrics.
type TimeSeriesPoint struct {
	Second      int       `json:"second"`
	Timestamp   time.Time `json:"timestamp"`
	RPS         float64   `json:"rps"`
	P50Latency  float64   `json:"p50_latency_ms"`
	P99Latency  float64   `json:"p99_latency_ms"`
	MeanLatency float64   `json:"mean_latency_ms"`
	Errors      int64     `json:"errors"`
	InFlight    int64     `json:"in_flight"`
	BytesRecv   int64     `json:"bytes_recv"`
}

type secondBucket struct {
	latencies []float64
	errors    int64
	requests  int64
	bytes     int64
	inFlight  int64
}

// TimeSeriesCollector accumulates per-second data and flushes it periodically.
type TimeSeriesCollector struct {
	mu         sync.Mutex
	startTime  time.Time
	current    *secondBucket
	currentSec int
	points     []TimeSeriesPoint
	latest     TimeSeriesPoint
}

// NewTimeSeriesCollector creates a time-series collector and marks the start time.
func NewTimeSeriesCollector() *TimeSeriesCollector {
	return &TimeSeriesCollector{
		startTime:  time.Now(),
		current:    &secondBucket{},
		currentSec: 0,
	}
}

// Record adds a single request observation to the current second bucket.
func (ts *TimeSeriesCollector) Record(latencyMs float64, isError bool, bytesRecv int64, inFlight int64) {
	ts.mu.Lock()
	defer ts.mu.Unlock()

	sec := int(time.Since(ts.startTime).Seconds())
	if sec != ts.currentSec && ts.current.requests > 0 {
		ts.flushLocked()
		ts.currentSec = sec
	}

	ts.current.latencies = append(ts.current.latencies, latencyMs)
	ts.current.requests++
	ts.current.bytes += bytesRecv
	ts.current.inFlight = inFlight
	if isError {
		ts.current.errors++
	}
}

// Flush forces the current bucket into a point.
func (ts *TimeSeriesCollector) Flush() {
	ts.mu.Lock()
	defer ts.mu.Unlock()
	if ts.current.requests > 0 {
		ts.flushLocked()
	}
}

func (ts *TimeSeriesCollector) flushLocked() {
	b := ts.current
	p := TimeSeriesPoint{
		Second:    ts.currentSec,
		Timestamp: ts.startTime.Add(time.Duration(ts.currentSec) * time.Second),
		RPS:       float64(b.requests),
		Errors:    b.errors,
		InFlight:  b.inFlight,
		BytesRecv: b.bytes,
	}

	if len(b.latencies) > 0 {
		sorted := make([]float64, len(b.latencies))
		copy(sorted, b.latencies)
		sortFloat64s(sorted)

		p.P50Latency = percentile(sorted, 50)
		p.P99Latency = percentile(sorted, 99)

		var sum float64
		for _, v := range sorted {
			sum += v
		}
		p.MeanLatency = sum / float64(len(sorted))
	}

	ts.points = append(ts.points, p)
	ts.latest = p
	ts.current = &secondBucket{}
}

// Latest returns the most recently flushed time-series point.
func (ts *TimeSeriesCollector) Latest() TimeSeriesPoint {
	ts.mu.Lock()
	defer ts.mu.Unlock()
	return ts.latest
}

// Points returns all collected time-series points.
func (ts *TimeSeriesCollector) Points() []TimeSeriesPoint {
	ts.mu.Lock()
	defer ts.mu.Unlock()
	out := make([]TimeSeriesPoint, len(ts.points))
	copy(out, ts.points)
	return out
}

func percentile(sorted []float64, pct float64) float64 {
	if len(sorted) == 0 {
		return 0
	}
	idx := int(float64(len(sorted)-1) * pct / 100.0)
	if idx >= len(sorted) {
		idx = len(sorted) - 1
	}
	return sorted[idx]
}

func sortFloat64s(data []float64) {
	sort.Float64s(data)
}
