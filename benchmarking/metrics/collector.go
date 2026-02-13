package metrics

import (
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"github.com/manan/feature-flag/benchmarking/client"

	"github.com/HdrHistogram/hdrhistogram-go"
)

// LatencyStats holds computed latency percentiles and statistics.
type LatencyStats struct {
	Min    float64 `json:"minMs"`
	Max    float64 `json:"maxMs"`
	Mean   float64 `json:"meanMs"`
	StdDev float64 `json:"stdDevMs"`
	P50    float64 `json:"p50Ms"`
	P75    float64 `json:"p75Ms"`
	P90    float64 `json:"p90Ms"`
	P95    float64 `json:"p95Ms"`
	P99    float64 `json:"p99Ms"`
	P999   float64 `json:"p999Ms"`
	P9999  float64 `json:"p9999Ms"`
}

// ThroughputStats holds throughput metrics.
type ThroughputStats struct {
	TotalRequests    int64   `json:"totalRequests"`
	Successful       int64   `json:"successful"`
	Failed           int64   `json:"failed"`
	ActualRPS        float64 `json:"actualRps"`
	PeakRPS          int     `json:"peakRps"`
	BytesTransferred int64   `json:"bytesTransferred"`
	ThroughputMBps   float64 `json:"throughputMBps"`
}

// ConnectionStats holds averaged connection timing.
type ConnectionStats struct {
	AvgDNSMs float64 `json:"avgDnsMs"`
	AvgTCPMs float64 `json:"avgTcpMs"`
	AvgTLSMs float64 `json:"avgTlsMs"`
}

// ErrorStats holds error breakdown information.
type ErrorStats struct {
	Rate         float64          `json:"ratePercent"`
	ByStatusCode map[int]int64    `json:"byStatusCode"`
	ByType       map[string]int64 `json:"byType"`
}

// EnvironmentResult holds metrics for a single environment.
type EnvironmentResult struct {
	EnvironmentKey string          `json:"environmentKey"`
	Throughput     ThroughputStats `json:"throughput"`
	Latency        LatencyStats    `json:"latency"`
	TTFB           LatencyStats    `json:"ttfb"`
	Errors         ErrorStats      `json:"errors"`
}

// TestResult holds the complete results of a stress test.
type TestResult struct {
	Metadata       TestMetadata                 `json:"metadata"`
	Global         GlobalResult                 `json:"global"`
	PerEnvironment map[string]EnvironmentResult `json:"perEnvironment"`
	TimeSeries     []TimeSeriesPoint            `json:"timeSeries"`
}

// TestMetadata holds test configuration metadata.
type TestMetadata struct {
	Mode                   string    `json:"mode"`
	Duration               string    `json:"duration"`
	Concurrency            int       `json:"concurrency"`
	TargetRPS              int       `json:"targetRps"`
	EvalURL                string    `json:"evalUrl"`
	AdminURL               string    `json:"adminUrl"`
	Timestamp              time.Time `json:"timestamp"`
	GoVersion              string    `json:"goVersion"`
	DiscoveredEnvironments []string  `json:"discoveredEnvironments"`
	DiscoveredFlags        []string  `json:"discoveredFlags"`
	UserPoolSize           int       `json:"userPoolSize"`
	Endpoint               string    `json:"endpoint"`
}

// GlobalResult holds aggregate metrics across all environments.
type GlobalResult struct {
	Throughput   ThroughputStats `json:"throughput"`
	Latency      LatencyStats    `json:"latency"`
	TTFB         LatencyStats    `json:"ttfb"`
	Connection   ConnectionStats `json:"connection"`
	Errors       ErrorStats      `json:"errors"`
	Availability float64         `json:"availabilityPercent"`
}

// envCollector holds per-environment histogram data.
type envCollector struct {
	latencyHist *hdrhistogram.Histogram
	ttfbHist    *hdrhistogram.Histogram
	total       int64
	success     int64
	failed      int64
	bytesRecv   int64
	statusCodes map[int]int64
	errorTypes  map[string]int64
}

// Collector aggregates metrics from all workers in a thread-safe manner.
type Collector struct {
	// Global histograms
	latencyHist *hdrhistogram.Histogram
	ttfbHist    *hdrhistogram.Histogram

	// Global counters
	totalRequests   atomic.Int64
	successRequests atomic.Int64
	failedRequests  atomic.Int64
	totalBytes      atomic.Int64
	inFlight        atomic.Int32

	// Connection timing accumulators
	dnsTotal    atomic.Int64
	tcpTotal    atomic.Int64
	tlsTotal    atomic.Int64
	connSamples atomic.Int64

	// Per-environment collectors
	envMu   sync.Mutex
	envData map[string]*envCollector

	// Peak RPS tracking
	peakRPS       atomic.Int32
	currentSecRPS atomic.Int32

	// Status code tracking (global)
	statusMu    sync.Mutex
	statusCodes map[int]int64
	errorTypes  map[string]int64

	// Time series
	TimeSeries *TimeSeriesCollector

	// Timing
	startTime time.Time
	mu        sync.Mutex // protects histograms
}

// NewCollector creates a new metrics collector.
// Histograms track values from 1 microsecond to 60 seconds with 3 significant digits.
func NewCollector() *Collector {
	return &Collector{
		latencyHist: hdrhistogram.New(1, 60_000_000, 3), // 1µs to 60s in µs
		ttfbHist:    hdrhistogram.New(1, 60_000_000, 3),
		envData:     make(map[string]*envCollector),
		statusCodes: make(map[int]int64),
		errorTypes:  make(map[string]int64),
		TimeSeries:  NewTimeSeriesCollector(),
	}
}

// Start marks the beginning of the test.
func (c *Collector) Start() {
	c.startTime = time.Now()

	// Start peak RPS tracker
	go c.trackPeakRPS()
}

// InFlightInc increments the in-flight counter.
func (c *Collector) InFlightInc() {
	c.inFlight.Add(1)
}

// InFlightDec decrements the in-flight counter.
func (c *Collector) InFlightDec() {
	c.inFlight.Add(-1)
}

// Record processes a single request result.
func (c *Collector) Record(r client.RequestResult) {
	latencyUs := r.Latency.Microseconds()
	if latencyUs < 1 {
		latencyUs = 1
	}

	ttfbUs := r.TTFB.Microseconds()
	if ttfbUs < 1 {
		ttfbUs = 1
	}

	isError := r.Error != nil

	// Update global histograms
	c.mu.Lock()
	c.latencyHist.RecordValue(latencyUs)
	if r.TTFB > 0 {
		c.ttfbHist.RecordValue(ttfbUs)
	}
	c.mu.Unlock()

	// Update global counters
	c.totalRequests.Add(1)
	c.totalBytes.Add(r.BytesReceived)
	c.currentSecRPS.Add(1)

	if isError {
		c.failedRequests.Add(1)
	} else {
		c.successRequests.Add(1)
	}

	// Connection timing
	if r.DNSLookup > 0 || r.TCPConnect > 0 || r.TLSHandshake > 0 {
		c.dnsTotal.Add(r.DNSLookup.Microseconds())
		c.tcpTotal.Add(r.TCPConnect.Microseconds())
		c.tlsTotal.Add(r.TLSHandshake.Microseconds())
		c.connSamples.Add(1)
	}

	// Status codes (global)
	if r.StatusCode > 0 {
		c.statusMu.Lock()
		c.statusCodes[r.StatusCode]++
		c.statusMu.Unlock()
	}
	if r.ErrorType != "" {
		c.statusMu.Lock()
		c.errorTypes[r.ErrorType]++
		c.statusMu.Unlock()
	}

	// Per-environment
	c.recordEnv(r, latencyUs, ttfbUs)

	// Time series
	latencyMs := float64(latencyUs) / 1000.0
	c.TimeSeries.Record(latencyMs, isError, r.BytesReceived, int64(c.inFlight.Load()))
}

func (c *Collector) recordEnv(r client.RequestResult, latencyUs, ttfbUs int64) {
	c.envMu.Lock()
	ec, ok := c.envData[r.Environment]
	if !ok {
		ec = &envCollector{
			latencyHist: hdrhistogram.New(1, 60_000_000, 3),
			ttfbHist:    hdrhistogram.New(1, 60_000_000, 3),
			statusCodes: make(map[int]int64),
			errorTypes:  make(map[string]int64),
		}
		c.envData[r.Environment] = ec
	}
	c.envMu.Unlock()

	// envCollector fields are only updated by one goroutine at a time per envMu
	c.envMu.Lock()
	ec.latencyHist.RecordValue(latencyUs)
	if r.TTFB > 0 {
		ec.ttfbHist.RecordValue(ttfbUs)
	}
	ec.total++
	ec.bytesRecv += r.BytesReceived
	if r.Error != nil {
		ec.failed++
	} else {
		ec.success++
	}
	if r.StatusCode > 0 {
		ec.statusCodes[r.StatusCode]++
	}
	if r.ErrorType != "" {
		ec.errorTypes[r.ErrorType]++
	}
	c.envMu.Unlock()
}

// Snapshot returns a current metrics snapshot for live progress reporting.
func (c *Collector) Snapshot() (total, succeeded, failed int64, rps float64, inFlight int32) {
	total = c.totalRequests.Load()
	succeeded = c.successRequests.Load()
	failed = c.failedRequests.Load()
	inFlight = c.inFlight.Load()

	elapsed := time.Since(c.startTime).Seconds()
	if elapsed > 0 {
		rps = float64(total) / elapsed
	}
	return
}

// Result computes the final TestResult from all collected data.
func (c *Collector) Result() *TestResult {
	elapsed := time.Since(c.startTime).Seconds()
	total := c.totalRequests.Load()
	success := c.successRequests.Load()
	failed := c.failedRequests.Load()
	bytesTotal := c.totalBytes.Load()

	// Global latency stats
	c.mu.Lock()
	globalLatency := histToStats(c.latencyHist)
	globalTTFB := histToStats(c.ttfbHist)
	c.mu.Unlock()

	// Connection stats
	var connStats ConnectionStats
	samples := c.connSamples.Load()
	if samples > 0 {
		connStats.AvgDNSMs = float64(c.dnsTotal.Load()) / float64(samples) / 1000.0
		connStats.AvgTCPMs = float64(c.tcpTotal.Load()) / float64(samples) / 1000.0
		connStats.AvgTLSMs = float64(c.tlsTotal.Load()) / float64(samples) / 1000.0
	}

	// Error stats
	c.statusMu.Lock()
	statusCopy := make(map[int]int64, len(c.statusCodes))
	for k, v := range c.statusCodes {
		statusCopy[k] = v
	}
	errorTypesCopy := make(map[string]int64, len(c.errorTypes))
	for k, v := range c.errorTypes {
		errorTypesCopy[k] = v
	}
	c.statusMu.Unlock()

	var errorRate float64
	if total > 0 {
		errorRate = float64(failed) / float64(total) * 100
	}

	var availability float64
	if total > 0 {
		availability = float64(success) / float64(total) * 100
	}

	var actualRPS float64
	if elapsed > 0 {
		actualRPS = float64(total) / elapsed
	}

	var throughputMB float64
	if elapsed > 0 {
		throughputMB = float64(bytesTotal) / 1024 / 1024 / elapsed
	}

	globalResult := GlobalResult{
		Throughput: ThroughputStats{
			TotalRequests:    total,
			Successful:       success,
			Failed:           failed,
			ActualRPS:        actualRPS,
			PeakRPS:          int(c.peakRPS.Load()),
			BytesTransferred: bytesTotal,
			ThroughputMBps:   throughputMB,
		},
		Latency:    globalLatency,
		TTFB:       globalTTFB,
		Connection: connStats,
		Errors: ErrorStats{
			Rate:         errorRate,
			ByStatusCode: statusCopy,
			ByType:       errorTypesCopy,
		},
		Availability: availability,
	}

	// Per-environment results
	c.envMu.Lock()
	perEnv := make(map[string]EnvironmentResult, len(c.envData))
	for envKey, ec := range c.envData {
		var envErrorRate float64
		if ec.total > 0 {
			envErrorRate = float64(ec.failed) / float64(ec.total) * 100
		}

		perEnv[envKey] = EnvironmentResult{
			EnvironmentKey: envKey,
			Throughput: ThroughputStats{
				TotalRequests:    ec.total,
				Successful:       ec.success,
				Failed:           ec.failed,
				ActualRPS:        float64(ec.total) / elapsed,
				BytesTransferred: ec.bytesRecv,
				ThroughputMBps:   float64(ec.bytesRecv) / 1024 / 1024 / elapsed,
			},
			Latency: histToStats(ec.latencyHist),
			TTFB:    histToStats(ec.ttfbHist),
			Errors: ErrorStats{
				Rate:         envErrorRate,
				ByStatusCode: ec.statusCodes,
				ByType:       ec.errorTypes,
			},
		}
	}
	c.envMu.Unlock()

	c.TimeSeries.Flush()

	return &TestResult{
		Global:         globalResult,
		PerEnvironment: perEnv,
		TimeSeries:     c.TimeSeries.Points(),
	}
}

func (c *Collector) trackPeakRPS() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		current := c.currentSecRPS.Swap(0)
		if current > c.peakRPS.Load() {
			c.peakRPS.Store(current)
		}
		if c.startTime.IsZero() {
			return
		}
	}
}

func histToStats(h *hdrhistogram.Histogram) LatencyStats {
	if h.TotalCount() == 0 {
		return LatencyStats{}
	}
	return LatencyStats{
		Min:    float64(h.Min()) / 1000.0,
		Max:    float64(h.Max()) / 1000.0,
		Mean:   h.Mean() / 1000.0,
		StdDev: h.StdDev() / 1000.0,
		P50:    float64(h.ValueAtQuantile(50)) / 1000.0,
		P75:    float64(h.ValueAtQuantile(75)) / 1000.0,
		P90:    float64(h.ValueAtQuantile(90)) / 1000.0,
		P95:    float64(h.ValueAtQuantile(95)) / 1000.0,
		P99:    float64(h.ValueAtQuantile(99)) / 1000.0,
		P999:   float64(h.ValueAtQuantile(99.9)) / 1000.0,
		P9999:  float64(h.ValueAtQuantile(99.99)) / 1000.0,
	}
}

// FormatDuration formats a duration for human display.
func FormatDuration(d time.Duration) string {
	if d < time.Millisecond {
		return fmt.Sprintf("%.1fµs", float64(d.Microseconds()))
	}
	if d < time.Second {
		return fmt.Sprintf("%.2fms", float64(d.Microseconds())/1000.0)
	}
	return fmt.Sprintf("%.2fs", d.Seconds())
}
