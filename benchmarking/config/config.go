package config

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// Mode represents a stress testing mode.
type Mode string

const (
	ModeConstant Mode = "constant"
	ModeRampUp   Mode = "rampup"
	ModeSpike    Mode = "spike"
	ModeSoak     Mode = "soak"
)

// Endpoint represents which evaluation endpoint(s) to test.
type Endpoint string

const (
	EndpointSingle Endpoint = "single"
	EndpointBulk   Endpoint = "bulk"
	EndpointBoth   Endpoint = "both"
)

// Config holds all configurable parameters for the stress tester.
type Config struct {
	// Connection
	AdminToken string
	AdminURL   string
	EvalURL    string

	// Filtering
	EnvFilter  []string
	FlagFilter []string
	Endpoint   Endpoint

	// Test mode
	Mode        Mode
	Duration    time.Duration
	Concurrency int
	TargetRPS   int

	// Ramp-up mode
	RampStartRPS     int
	RampEndRPS       int
	RampStepDuration time.Duration

	// Spike mode
	SpikeBaseRPS  int
	SpikePeakRPS  int
	SpikeDuration time.Duration

	// Soak mode
	SoakRPS int

	// User simulation
	UserPoolSize int
	UserIDPrefix string

	// HTTP tuning
	HTTPTimeout  time.Duration
	WarmUp       time.Duration
	MaxIdleConns int

	// Output
	OutputPath     string
	ErrorThreshold float64
}

// Parse parses command-line flags into a Config.
func Parse() (*Config, error) {
	cfg := &Config{}

	// Connection
	flag.StringVar(&cfg.AdminToken, "admin-token", "", "JWT token for Admin API (required)")
	flag.StringVar(&cfg.AdminURL, "admin-url", "http://localhost:8080", "Admin API base URL")
	flag.StringVar(&cfg.EvalURL, "eval-url", "http://localhost:8081", "Evaluation API base URL")

	// Filtering
	var envFilter, flagFilter string
	flag.StringVar(&envFilter, "envs", "", "Comma-separated environment keys to test (empty = all)")
	flag.StringVar(&flagFilter, "flags", "", "Comma-separated flag keys to test (empty = all)")
	var endpoint string
	flag.StringVar(&endpoint, "endpoint", "both", "Evaluation endpoint: single, bulk, both")

	// Test mode
	var mode string
	flag.StringVar(&mode, "mode", "constant", "Test mode: constant, rampup, spike, soak")
	flag.DurationVar(&cfg.Duration, "duration", 30*time.Second, "Test duration")
	flag.IntVar(&cfg.Concurrency, "concurrency", 50, "Number of worker goroutines")
	flag.IntVar(&cfg.TargetRPS, "rps", 1000, "Target requests/sec (constant mode)")

	// Ramp-up
	flag.IntVar(&cfg.RampStartRPS, "ramp-start", 100, "Starting RPS (ramp-up mode)")
	flag.IntVar(&cfg.RampEndRPS, "ramp-end", 5000, "Ending RPS (ramp-up mode)")
	flag.DurationVar(&cfg.RampStepDuration, "ramp-step", 5*time.Second, "Duration per RPS step (ramp-up)")

	// Spike
	flag.IntVar(&cfg.SpikeBaseRPS, "spike-base", 500, "Baseline RPS (spike mode)")
	flag.IntVar(&cfg.SpikePeakRPS, "spike-peak", 5000, "Peak RPS during spike")
	flag.DurationVar(&cfg.SpikeDuration, "spike-dur", 5*time.Second, "Duration of the spike")

	// Soak
	flag.IntVar(&cfg.SoakRPS, "soak-rps", 500, "Steady RPS for soak test")

	// User simulation
	flag.IntVar(&cfg.UserPoolSize, "users", 1000, "Number of synthetic user IDs")
	flag.StringVar(&cfg.UserIDPrefix, "user-prefix", "user-", "Prefix for generated user IDs")

	// HTTP tuning
	flag.DurationVar(&cfg.HTTPTimeout, "timeout", 10*time.Second, "Per-request HTTP timeout")
	flag.DurationVar(&cfg.WarmUp, "warmup", 5*time.Second, "Warm-up period (results excluded)")
	flag.IntVar(&cfg.MaxIdleConns, "max-idle-conns", 200, "HTTP transport max idle connections")

	// Output
	flag.StringVar(&cfg.OutputPath, "output", "results/latest.json", "Path for JSON results")
	flag.Float64Var(&cfg.ErrorThreshold, "error-threshold", 5.0, "Error rate threshold (%) for exit code 1")

	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "Usage: stress-tester [options]\n\n")
		fmt.Fprintf(os.Stderr, "A stress testing tool for the Feature Flag Evaluation API.\n")
		fmt.Fprintf(os.Stderr, "Discovers environments and flags via Admin API, then stress-tests the Evaluation API.\n\n")
		fmt.Fprintf(os.Stderr, "Options:\n")
		flag.PrintDefaults()
	}

	flag.Parse()

	// Validate required fields
	if cfg.AdminToken == "" {
		return nil, fmt.Errorf("--admin-token is required")
	}

	// Parse mode
	switch Mode(mode) {
	case ModeConstant, ModeRampUp, ModeSpike, ModeSoak:
		cfg.Mode = Mode(mode)
	default:
		return nil, fmt.Errorf("invalid --mode %q: must be constant, rampup, spike, or soak", mode)
	}

	// Parse endpoint
	switch Endpoint(endpoint) {
	case EndpointSingle, EndpointBulk, EndpointBoth:
		cfg.Endpoint = Endpoint(endpoint)
	default:
		return nil, fmt.Errorf("invalid --endpoint %q: must be single, bulk, or both", endpoint)
	}

	// Parse filters
	cfg.EnvFilter = splitAndTrim(envFilter)
	cfg.FlagFilter = splitAndTrim(flagFilter)

	// Validate values
	if cfg.Concurrency < 1 {
		return nil, fmt.Errorf("--concurrency must be >= 1")
	}
	if cfg.TargetRPS < 1 {
		return nil, fmt.Errorf("--rps must be >= 1")
	}
	if cfg.Duration < 1*time.Second {
		return nil, fmt.Errorf("--duration must be >= 1s")
	}
	if cfg.UserPoolSize < 1 {
		return nil, fmt.Errorf("--users must be >= 1")
	}

	// Resolve relative output path against the executable's directory
	// so results always land in benchmarking/results/ regardless of cwd.
	if cfg.OutputPath != "" && !filepath.IsAbs(cfg.OutputPath) {
		if exe, err := os.Executable(); err == nil {
			cfg.OutputPath = filepath.Join(filepath.Dir(exe), cfg.OutputPath)
		}
	}

	return cfg, nil
}

// ModeRPS returns the active RPS for the current mode's initial state.
func (c *Config) ModeRPS() int {
	switch c.Mode {
	case ModeRampUp:
		return c.RampStartRPS
	case ModeSpike:
		return c.SpikeBaseRPS
	case ModeSoak:
		return c.SoakRPS
	default:
		return c.TargetRPS
	}
}

func splitAndTrim(s string) []string {
	if s == "" {
		return nil
	}
	parts := strings.Split(s, ",")
	result := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			result = append(result, p)
		}
	}
	return result
}
