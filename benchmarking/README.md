# Feature-Flag Stress Tester

A CLI stress-testing tool for the Feature Flag Evaluation API. It auto-discovers environments, flags, and API keys from the Admin API—so you only need a single admin JWT token to run a full multi-environment benchmark.

## Architecture

```
┌──────────────┐  discover   ┌───────────┐
│  Admin API   │◄────────────│  stress-  │
│  :8080       │  envs/flags │  tester   │
└──────────────┘             │           │
                             │  N        │
┌──────────────┐  evaluate   │  workers  │
│  Eval API    │◄────────────│           │
│  :8081       │  X-API-Key  └───────────┘
└──────────────┘                  │
                                  ▼
                          results/<mode>.json
```

**Packages:**

| Package | Purpose |
|---------|---------|
| `admin` | Discovers environments, flags, and API keys via Admin API |
| `client` | Evaluation API HTTP client with `httptrace` timing (TTFB, DNS, TCP, TLS) |
| `config` | CLI flag parsing and validation |
| `runner` | Test orchestrator with 4 modes and token-bucket rate limiting |
| `metrics` | HdrHistogram-based latency collection, per-env breakdown, ANSI terminal reporter |
| `exporter` | JSON export with rotation scheme |

## Prerequisites

- Go 1.21+
- Running Admin API (default `localhost:8080`)
- Running Evaluation API (default `localhost:8081`)
- A valid Admin API JWT token

## Build

```bash
cd benchmarking
go build -o stress-tester .
```

## Quick Start

```bash
# Minimal — discovers all environments and flags automatically
./stress-tester -admin-token "your-jwt-token"

# Constant load: 2000 RPS for 60s with 100 workers
./stress-tester \
  -admin-token "your-jwt-token" \
  -mode constant \
  -rps 2000 \
  -concurrency 100 \
  -duration 60s

# Ramp-up: 100 → 5000 RPS over 2 minutes
./stress-tester \
  -admin-token "your-jwt-token" \
  -mode rampup \
  -ramp-start 100 \
  -ramp-end 5000 \
  -ramp-step 5s \
  -duration 2m

# Test only specific environments and flags
./stress-tester \
  -admin-token "your-jwt-token" \
  -envs "production,staging" \
  -flags "dark-mode,new-checkout"
```

## Test Modes

### `constant` (default)

Sustains a fixed request rate for the entire test duration.

```bash
./stress-tester -admin-token $TOKEN -mode constant -rps 1000 -duration 30s
```

### `rampup`

Linearly increases RPS from a start value to an end value, stepping at fixed intervals.

```bash
./stress-tester -admin-token $TOKEN -mode rampup \
  -ramp-start 100 \    # starting RPS
  -ramp-end 5000 \     # ending RPS
  -ramp-step 5s \      # time at each step before increasing
  -duration 2m
```

### `spike`

Holds a baseline RPS, then spikes to a peak at the midpoint of the test for a configured duration, then drops back.

```bash
./stress-tester -admin-token $TOKEN -mode spike \
  -spike-base 500 \    # baseline RPS
  -spike-peak 5000 \   # peak RPS during spike
  -spike-dur 5s \      # how long the spike lasts
  -duration 60s
```

### `soak`

Extended-duration constant load designed for detecting memory leaks, connection exhaustion, and degradation over time.

```bash
./stress-tester -admin-token $TOKEN -mode soak \
  -soak-rps 500 \
  -duration 30m
```

## All Options

| Flag | Default | Description |
|------|---------|-------------|
| `-admin-token` | *(required)* | JWT token for Admin API authentication |
| `-admin-url` | `http://localhost:8080` | Admin API base URL |
| `-eval-url` | `http://localhost:8081` | Evaluation API base URL |
| `-envs` | *(all)* | Comma-separated environment keys/names to test |
| `-flags` | *(all)* | Comma-separated flag keys/names to test |
| `-endpoint` | `both` | Which eval endpoint to hit: `single`, `bulk`, or `both` |
| `-mode` | `constant` | Test mode: `constant`, `rampup`, `spike`, `soak` |
| `-duration` | `30s` | Total test duration (e.g., `60s`, `5m`) |
| `-concurrency` | `50` | Number of concurrent worker goroutines |
| `-rps` | `1000` | Target requests/sec (constant mode) |
| `-ramp-start` | `100` | Starting RPS (ramp-up mode) |
| `-ramp-end` | `5000` | Ending RPS (ramp-up mode) |
| `-ramp-step` | `5s` | Time at each RPS step before increasing |
| `-spike-base` | `500` | Baseline RPS (spike mode) |
| `-spike-peak` | `5000` | Peak RPS during spike |
| `-spike-dur` | `5s` | Duration of the spike burst |
| `-soak-rps` | `500` | Steady RPS for soak test |
| `-users` | `1000` | Size of the synthetic user ID pool |
| `-user-prefix` | `user-` | Prefix for generated user IDs (`user-0`, `user-1`, …) |
| `-timeout` | `10s` | Per-request HTTP timeout |
| `-warmup` | `5s` | Warm-up period (requests sent but results excluded) |
| `-max-idle-conns` | `200` | HTTP transport max idle connections |
| `-output` | `results` | Directory for JSON results (files named by mode) |
| `-error-threshold` | `5.0` | Error rate (%) above which the tool exits with code 1 |

## Metrics Collected

### Latency (via HdrHistogram)
- **p50, p90, p95, p99, p999** — accurate to 3 significant digits
- **Min / Max / Mean / StdDev**

### Time to First Byte (TTFB)
- Same percentile breakdown as latency, measured via `httptrace.GotFirstResponseByte`

### Connection Timing (via `net/http/httptrace`)
- **DNS lookup** duration
- **TCP connect** duration
- **TLS handshake** duration

### Throughput
- **Actual RPS** (measured vs. target)
- **Total requests / successes / failures**
- **Peak RPS** (per-second max)
- **Total bytes received**

### Error Analysis
- **Error rate** (%)
- Breakdown **by HTTP status code** (4xx, 5xx)
- Breakdown **by error type** (timeout, network, connection refused)

### Per-Environment Breakdown
All of the above metrics are also tracked **per environment**, so you can compare performance across staging, production, etc.

### Time Series
Per-second snapshots of RPS, p50/p99 latency, error count, in-flight requests, and bytes received — exported in the JSON results for graphing.

## Output

### Terminal

The tool prints a colored, table-formatted report directly to the terminal with sections for throughput, latency percentiles, TTFB, connection timing, errors, and per-environment comparison.

A live progress line updates during the test showing elapsed time, current RPS, p99 latency, errors, and in-flight count.

### JSON Export

Results are written to a per-mode file in the output directory (e.g., `results/constant.json`, `results/rampup.json`). Each mode keeps its own latest result. When the same mode is tested again, the previous result is rotated to `results/archive/<mode>_YYYYMMDD_HHMMSS.json`.

```
results/
├── constant.json        ← latest constant mode run
├── rampup.json          ← latest rampup mode run
├── spike.json           ← latest spike mode run
├── soak.json            ← latest soak mode run
└── archive/             ← previous runs per mode (gitignored)
    ├── constant_20260213_143022.json
    ├── rampup_20260212_091500.json
    └── spike_20260211_180000.json
```

### JSON Structure

```jsonc
{
  "metadata": {
    "mode": "rampup",
    "duration": "2m0s",
    "concurrency": 100,
    "targetRps": 100,
    "evalUrl": "http://localhost:8081",
    "adminUrl": "http://localhost:8080",
    "timestamp": "2026-02-13T14:30:22Z",
    "goVersion": "go1.25.7",
    "discoveredEnvironments": ["staging", "production"],
    "discoveredFlags": ["dark-mode", "new-checkout"],
    "userPoolSize": 1000,
    "endpoint": "both"
  },
  "global": {
    "throughput": { "totalRequests": 150000, "successCount": 149850, "..." },
    "latency": { "p50": 2.1, "p99": 12.5, "max": 89.3, "..." },
    "ttfb": { "p50": 1.8, "p99": 10.2, "..." },
    "connection": { "avgDns": 0.0, "avgTcp": 0.1, "..." },
    "errors": { "total": 150, "rate": 0.001, "..." }
  },
  "perEnvironment": {
    "staging": { "..." },
    "production": { "..." }
  },
  "timeSeries": [
    { "second": 0, "rps": 102, "p50_latency_ms": 1.9, "p99_latency_ms": 8.1, "errors": 0 },
    { "second": 1, "rps": 198, "..." }
  ]
}
```

## Generating Graphs

To regenerate the SVG charts from the benchmark results:

```bash
cd data/graphs
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python generate.py
```

The script reads JSON results from `benchmarking/results/` and outputs 5 SVG files to `benchmarking/results/graphs/` for the root README.

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Test completed, error rate within threshold |
| `1` | Error rate exceeded `-error-threshold`, or setup/runtime failure |

This makes the tool CI-friendly — use `-error-threshold 1.0` to fail a pipeline if more than 1% of requests error.

## Examples

```bash
# CI gate: fail if error rate > 1%
./stress-tester -admin-token $TOKEN -duration 30s -rps 500 -error-threshold 1.0

# Capacity planning: find the breaking point
./stress-tester -admin-token $TOKEN -mode rampup -ramp-start 100 -ramp-end 10000 -duration 5m

# Reliability: 30-minute soak test
./stress-tester -admin-token $TOKEN -mode soak -soak-rps 500 -duration 30m

# Only test the bulk endpoint on staging
./stress-tester -admin-token $TOKEN -envs staging -endpoint bulk -rps 2000

# Custom eval API URL (e.g., remote environment)
./stress-tester -admin-token $TOKEN \
  -admin-url https://admin.example.com \
  -eval-url https://eval.example.com
```
