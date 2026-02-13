package main

import (
	"context"
	"fmt"
	"os"
	"os/signal"
	"runtime"
	"syscall"
	"time"

	"github.com/manan/feature-flag/benchmarking/admin"
	"github.com/manan/feature-flag/benchmarking/client"
	"github.com/manan/feature-flag/benchmarking/config"
	"github.com/manan/feature-flag/benchmarking/exporter"
	"github.com/manan/feature-flag/benchmarking/metrics"
	"github.com/manan/feature-flag/benchmarking/runner"
)

func main() {
	cfg, err := config.Parse()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	fmt.Println()
	fmt.Println("╔══════════════════════════════════════════════════╗")
	fmt.Println("║        Feature-Flag Stress Tester v1.0          ║")
	fmt.Println("╚══════════════════════════════════════════════════╝")
	fmt.Println()

	// ── Admin Discovery ─────────────────────────────────────────
	adminClient := admin.NewClient(cfg.AdminURL, cfg.AdminToken)

	fmt.Print("⏳ Pinging Admin API... ")
	if err := adminClient.Ping(); err != nil {
		fmt.Printf("✗\n  Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Println("✓")

	fmt.Print("⏳ Discovering environments & flags... ")
	discovery, err := adminClient.Discover()
	if err != nil {
		fmt.Printf("✗\n  Error: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("✓  (%d envs, %d flags)\n", len(discovery.Environments), len(discovery.Flags))

	// ── Filter ──────────────────────────────────────────────────
	envs := filterEnvironments(discovery.Environments, cfg.EnvFilter)
	flags := filterFlags(discovery.Flags, cfg.FlagFilter)

	if len(envs) == 0 {
		fmt.Println("✗ No environments matched the filter")
		os.Exit(1)
	}
	if len(flags) == 0 {
		fmt.Println("✗ No flags matched the filter")
		os.Exit(1)
	}

	// Prepare data for display and target building
	envKeys := make([]string, len(envs))
	apiKeyMap := make(map[string]string, len(envs))
	envPairs := make([]struct{ Key, APIKey string }, len(envs))
	for i, e := range envs {
		envKeys[i] = e.Key
		apiKeyMap[e.Key] = e.APIKey
		envPairs[i] = struct{ Key, APIKey string }{e.Key, e.APIKey}
	}
	flagKeys := make([]string, len(flags))
	for i, f := range flags {
		flagKeys[i] = f.Key
	}

	metrics.PrintDiscovery(envKeys, apiKeyMap, flagKeys)

	// ── Build Targets ───────────────────────────────────────────
	targets := runner.BuildTargets(envPairs, flagKeys, cfg.Endpoint)
	fmt.Printf("\n  Targets: %d (envs × flags × endpoint)\n", len(targets))

	if len(targets) == 0 {
		fmt.Println("✗ No targets to test")
		os.Exit(1)
	}

	// ── Preflight ───────────────────────────────────────────────
	evalClient := client.New(cfg.EvalURL, cfg.HTTPTimeout, cfg.MaxIdleConns)

	fmt.Print("\n⏳ Checking evaluation API readiness... ")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	if err := evalClient.CheckReady(ctx); err != nil {
		cancel()
		fmt.Printf("✗\n  Error: %v\n", err)
		os.Exit(1)
	}
	cancel()
	fmt.Println("✓")

	// ── Print Config ────────────────────────────────────────────
	metrics.PrintTestConfig(
		string(cfg.Mode),
		cfg.Duration.String(),
		cfg.Concurrency,
		cfg.ModeRPS(),
		cfg.EvalURL,
		string(cfg.Endpoint),
	)

	// ── Run ─────────────────────────────────────────────────────
	ctx, cancel = context.WithCancel(context.Background())
	defer cancel()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-sigCh
		fmt.Println("\n\n⚠ Interrupt received, stopping gracefully...")
		cancel()
	}()

	r := runner.New(cfg, evalClient, targets)

	fmt.Printf("\n🚀 Starting %s test (%s)...\n\n", cfg.Mode, cfg.Duration)

	result := r.Run(ctx)

	// ── Populate Metadata ───────────────────────────────────────
	result.Metadata = metrics.TestMetadata{
		Mode:                   string(cfg.Mode),
		Duration:               cfg.Duration.String(),
		Concurrency:            cfg.Concurrency,
		TargetRPS:              cfg.ModeRPS(),
		EvalURL:                cfg.EvalURL,
		AdminURL:               cfg.AdminURL,
		Timestamp:              time.Now(),
		GoVersion:              runtime.Version(),
		DiscoveredEnvironments: envKeys,
		DiscoveredFlags:        flagKeys,
		UserPoolSize:           cfg.UserPoolSize,
		Endpoint:               string(cfg.Endpoint),
	}

	// ── Report ──────────────────────────────────────────────────
	fmt.Println()
	metrics.PrintReport(result)

	// ── Export ───────────────────────────────────────────────────
	if cfg.OutputPath != "" {
		fmt.Printf("\n⏳ Exporting results to %s... ", cfg.OutputPath)
		if err := exporter.Export(result, cfg.OutputPath); err != nil {
			fmt.Printf("✗\n  Error: %v\n", err)
		} else {
			fmt.Println("✓")
		}
	}

	// ── Exit Code ───────────────────────────────────────────────
	if result.Global.Errors.Rate > cfg.ErrorThreshold {
		fmt.Printf("\n✗ Error rate %.2f%% exceeds threshold %.2f%%\n",
			result.Global.Errors.Rate*100, cfg.ErrorThreshold*100)
		os.Exit(1)
	}

	fmt.Println("\n✅ Stress test completed successfully.")
}

func filterEnvironments(envs []admin.Environment, filter []string) []admin.Environment {
	if len(filter) == 0 {
		return envs
	}
	allowed := make(map[string]bool, len(filter))
	for _, p := range filter {
		allowed[p] = true
	}

	var out []admin.Environment
	for _, e := range envs {
		if allowed[e.Key] || allowed[e.Name] {
			out = append(out, e)
		}
	}
	return out
}

func filterFlags(flags []admin.Flag, filter []string) []admin.Flag {
	if len(filter) == 0 {
		return flags
	}
	allowed := make(map[string]bool, len(filter))
	for _, p := range filter {
		allowed[p] = true
	}

	var out []admin.Flag
	for _, f := range flags {
		if allowed[f.Key] || allowed[f.Name] {
			out = append(out, f)
		}
	}
	return out
}
