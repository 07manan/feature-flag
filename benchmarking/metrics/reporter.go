package metrics

import (
	"fmt"
	"strings"
)

// ANSI color codes
const (
	colorReset  = "\033[0m"
	colorRed    = "\033[31m"
	colorGreen  = "\033[32m"
	colorYellow = "\033[33m"
	colorCyan   = "\033[36m"
	colorBold   = "\033[1m"
	colorDim    = "\033[2m"
)

// PrintLiveProgress prints a single-line progress update to the terminal.
func PrintLiveProgress(elapsed, total float64, rps float64, p99Ms float64, errors int64, envCount int, inFlight int32) {
	bar := buildProgressBar(elapsed, total, 20)
	fmt.Printf("\r%s[%.0fs/%.0fs]%s %s RPS: %s%s%.0f%s | p99: %s | Errs: %s | Envs: %d | Workers: %d   ",
		colorDim, elapsed, total, colorReset,
		bar,
		colorBold, colorCyan, rps, colorReset,
		colorizeLatency(p99Ms),
		colorizeErrors(errors),
		envCount,
		inFlight,
	)
}

// PrintDiscovery prints the discovered environments and flags.
func PrintDiscovery(envs []string, apiKeys map[string]string, flags []string) {
	fmt.Println()
	printBox("DISCOVERED RESOURCES")
	fmt.Println()

	// Environments table
	fmt.Printf("  %s%sEnvironments:%s\n", colorBold, colorCyan, colorReset)
	printTableHeader("  ", "Environment", "API Key")
	for _, env := range envs {
		key := apiKeys[env]
		masked := maskAPIKey(key)
		printTableRow("  ", env, masked)
	}
	printTableFooter("  ", 2)
	fmt.Println()

	// Flags
	fmt.Printf("  %s%sFlags:%s %d active", colorBold, colorCyan, colorReset, len(flags))
	if len(flags) > 0 {
		displayed := flags
		if len(displayed) > 8 {
			displayed = displayed[:8]
		}
		fmt.Printf(" (%s", strings.Join(displayed, ", "))
		if len(flags) > 8 {
			fmt.Printf(", +%d more", len(flags)-8)
		}
		fmt.Printf(")")
	}
	fmt.Println()
	fmt.Println()
}

// PrintTestConfig prints the test configuration before starting.
func PrintTestConfig(mode string, duration string, concurrency int, targetRPS int, evalURL string, endpoint string) {
	printBox("TEST CONFIGURATION")
	fmt.Println()
	fmt.Printf("  Mode: %s%s%s | Duration: %s | Concurrency: %d | Target RPS: %d\n",
		colorBold, strings.ToUpper(mode), colorReset, duration, concurrency, targetRPS)
	fmt.Printf("  Eval URL: %s | Endpoint: %s\n", evalURL, endpoint)
	fmt.Println()
}

// PrintReport prints the full test results report.
func PrintReport(result *TestResult) {
	fmt.Println()
	fmt.Println()
	mode := strings.ToUpper(result.Metadata.Mode)
	printBox(fmt.Sprintf("STRESS TEST RESULTS — %s MODE", mode))
	fmt.Println()

	// Config summary
	fmt.Printf("  Duration: %s | Concurrency: %d | Target RPS: %d | Endpoint: %s\n",
		result.Metadata.Duration, result.Metadata.Concurrency, result.Metadata.TargetRPS, result.Metadata.Endpoint)
	fmt.Println()

	// Throughput
	printSection("THROUGHPUT")
	g := result.Global
	fmt.Printf("  Total Requests ........... %s%d%s\n", colorBold, g.Throughput.TotalRequests, colorReset)
	fmt.Printf("  Successful ............... %s%d%s (%s%.2f%%%s)\n",
		colorGreen, g.Throughput.Successful, colorReset,
		colorGreen, pct(g.Throughput.Successful, g.Throughput.TotalRequests), colorReset)
	fmt.Printf("  Failed ................... %s%d%s (%s)\n",
		colorizeErrorCount(g.Throughput.Failed), g.Throughput.Failed, colorReset,
		colorizeErrorPct(pct(g.Throughput.Failed, g.Throughput.TotalRequests)))
	fmt.Printf("  Actual RPS ............... %s%.1f%s\n", colorCyan, g.Throughput.ActualRPS, colorReset)
	fmt.Printf("  Peak RPS ................. %d\n", g.Throughput.PeakRPS)
	fmt.Printf("  Data Transferred ......... %.2f MB (%.2f MB/s)\n",
		float64(g.Throughput.BytesTransferred)/1024/1024, g.Throughput.ThroughputMBps)
	fmt.Printf("  Availability ............. %s%.4f%%%s\n",
		colorizeAvailability(g.Availability), g.Availability, colorReset)
	fmt.Println()

	// Latency
	printSection("LATENCY (ms)")
	printLatencyTable(g.Latency)
	fmt.Println()

	// TTFB
	printSection("TIME TO FIRST BYTE (ms)")
	printLatencyTable(g.TTFB)
	fmt.Println()

	// Connection Timing
	printSection("CONNECTION TIMING (avg ms)")
	fmt.Printf("  DNS: %.3f | TCP: %.3f | TLS: %.3f\n",
		g.Connection.AvgDNSMs, g.Connection.AvgTCPMs, g.Connection.AvgTLSMs)
	fmt.Println()

	// Errors
	if g.Throughput.Failed > 0 {
		printSection("ERROR BREAKDOWN")
		if len(g.Errors.ByStatusCode) > 0 {
			fmt.Printf("  By Status Code:\n")
			for code, count := range g.Errors.ByStatusCode {
				fmt.Printf("    HTTP %d: %d\n", code, count)
			}
		}
		if len(g.Errors.ByType) > 0 {
			fmt.Printf("  By Error Type:\n")
			for errType, count := range g.Errors.ByType {
				fmt.Printf("    %s: %d\n", errType, count)
			}
		}
		fmt.Println()
	}

	// Per-environment breakdown
	if len(result.PerEnvironment) > 1 {
		printSection("PER-ENVIRONMENT BREAKDOWN")
		printEnvHeader()
		for _, envResult := range result.PerEnvironment {
			printEnvRow(envResult)
		}
		printEnvFooter()
		fmt.Println()
	}
}

func printLatencyTable(s LatencyStats) {
	fmt.Printf("  Min: %s%-8.3f%s | Mean: %s%-8.3f%s | Max: %s%-8.3f%s\n",
		colorGreen, s.Min, colorReset,
		colorCyan, s.Mean, colorReset,
		colorizeLatencyValue(s.Max), s.Max, colorReset)
	fmt.Printf("  p50: %s%-8.3f%s | p75:  %s%-8.3f%s | p90: %s%-8.3f%s\n",
		colorGreen, s.P50, colorReset,
		colorizeLatencyValue(s.P75), s.P75, colorReset,
		colorizeLatencyValue(s.P90), s.P90, colorReset)
	fmt.Printf("  p95: %s%-8.3f%s | p99:  %s%-8.3f%s | p99.9: %s%.3f%s\n",
		colorizeLatencyValue(s.P95), s.P95, colorReset,
		colorizeLatencyValue(s.P99), s.P99, colorReset,
		colorizeLatencyValue(s.P999), s.P999, colorReset)
	fmt.Printf("  StdDev: %.3f\n", s.StdDev)
}

func printEnvHeader() {
	fmt.Printf("  ┌──────────────────┬──────────┬──────────┬──────────┬──────────┬──────────┐\n")
	fmt.Printf("  │ %sEnvironment%s      │ %sReqs%s     │ %sRPS%s      │ %sp50%s      │ %sp99%s      │ %sErrs%%%s    │\n",
		colorBold, colorReset, colorBold, colorReset, colorBold, colorReset,
		colorBold, colorReset, colorBold, colorReset, colorBold, colorReset)
	fmt.Printf("  ├──────────────────┼──────────┼──────────┼──────────┼──────────┼──────────┤\n")
}

func printEnvRow(r EnvironmentResult) {
	name := r.EnvironmentKey
	if len(name) > 16 {
		name = name[:16]
	}
	fmt.Printf("  │ %-16s │ %8d │ %8.1f │ %s%7.3fms%s │ %s%7.3fms%s │ %s%7.2f%%%s │\n",
		name,
		r.Throughput.TotalRequests,
		r.Throughput.ActualRPS,
		colorGreen, r.Latency.P50, colorReset,
		colorizeLatencyValue(r.Latency.P99), r.Latency.P99, colorReset,
		colorizeErrorPctValue(r.Errors.Rate), r.Errors.Rate, colorReset,
	)
}

func printEnvFooter() {
	fmt.Printf("  └──────────────────┴──────────┴──────────┴──────────┴──────────┴──────────┘\n")
}

// --- Helpers ---

func printBox(title string) {
	width := len(title) + 6
	border := strings.Repeat("═", width)
	fmt.Printf("  ╔%s╗\n", border)
	fmt.Printf("  ║   %s%s%s   ║\n", colorBold, title, colorReset)
	fmt.Printf("  ╚%s╝\n", border)
}

func printSection(title string) {
	fmt.Printf("  %s%s── %s ──%s\n", colorBold, colorCyan, title, colorReset)
}

func printTableHeader(indent string, cols ...string) {
	fmt.Printf("%s  ┌", indent)
	for i := range cols {
		fmt.Printf("──────────────────")
		if i < len(cols)-1 {
			fmt.Printf("┬")
		}
	}
	fmt.Printf("┐\n")
}

func printTableRow(indent string, cols ...string) {
	fmt.Printf("%s  │", indent)
	for i, col := range cols {
		fmt.Printf(" %-16s ", col)
		if i < len(cols)-1 {
			fmt.Printf("│")
		}
	}
	fmt.Printf("│\n")
}

func printTableFooter(indent string, colCount int) {
	fmt.Printf("%s  └", indent)
	for i := 0; i < colCount; i++ {
		fmt.Printf("──────────────────")
		if i < colCount-1 {
			fmt.Printf("┴")
		}
	}
	fmt.Printf("┘\n")
}

func buildProgressBar(current, total float64, width int) string {
	if total <= 0 {
		return ""
	}
	ratio := current / total
	if ratio > 1 {
		ratio = 1
	}
	filled := int(ratio * float64(width))
	bar := strings.Repeat("█", filled) + strings.Repeat("░", width-filled)
	return fmt.Sprintf("%s%s%s", colorCyan, bar, colorReset)
}

func colorizeLatency(ms float64) string {
	if ms < 5 {
		return fmt.Sprintf("%s%.2fms%s", colorGreen, ms, colorReset)
	}
	if ms < 50 {
		return fmt.Sprintf("%s%.2fms%s", colorYellow, ms, colorReset)
	}
	return fmt.Sprintf("%s%.2fms%s", colorRed, ms, colorReset)
}

func colorizeLatencyValue(ms float64) string {
	if ms < 5 {
		return colorGreen
	}
	if ms < 50 {
		return colorYellow
	}
	return colorRed
}

func colorizeErrors(count int64) string {
	if count == 0 {
		return fmt.Sprintf("%s0%s", colorGreen, colorReset)
	}
	return fmt.Sprintf("%s%d%s", colorRed, count, colorReset)
}

func colorizeErrorCount(count int64) string {
	if count == 0 {
		return colorGreen
	}
	return colorRed
}

func colorizeErrorPct(pctVal float64) string {
	if pctVal == 0 {
		return fmt.Sprintf("%s0.00%%%s", colorGreen, colorReset)
	}
	if pctVal < 1 {
		return fmt.Sprintf("%s%.2f%%%s", colorYellow, pctVal, colorReset)
	}
	return fmt.Sprintf("%s%.2f%%%s", colorRed, pctVal, colorReset)
}

func colorizeErrorPctValue(pctVal float64) string {
	if pctVal == 0 {
		return colorGreen
	}
	if pctVal < 1 {
		return colorYellow
	}
	return colorRed
}

func colorizeAvailability(pctVal float64) string {
	if pctVal >= 99.99 {
		return colorGreen
	}
	if pctVal >= 99 {
		return colorYellow
	}
	return colorRed
}

func maskAPIKey(key string) string {
	if len(key) <= 10 {
		return key
	}
	return key[:8] + "..." + key[len(key)-4:]
}

func pct(part, total int64) float64 {
	if total == 0 {
		return 0
	}
	return float64(part) / float64(total) * 100
}
