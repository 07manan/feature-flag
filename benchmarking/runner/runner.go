package runner

import (
	"context"
	"fmt"
	"math/rand/v2"
	"sync"
	"time"

	"github.com/manan/feature-flag/benchmarking/client"
	"github.com/manan/feature-flag/benchmarking/config"
	"github.com/manan/feature-flag/benchmarking/metrics"
)

// Runner orchestrates the stress test across all modes.
type Runner struct {
	cfg       *config.Config
	client    *client.Client
	collector *metrics.Collector
	targets   []client.TestTarget
	userIDs   []string
}

// New creates a new Runner.
func New(cfg *config.Config, evalClient *client.Client, targets []client.TestTarget) *Runner {
	userIDs := make([]string, cfg.UserPoolSize)
	for i := range userIDs {
		userIDs[i] = fmt.Sprintf("%s%d", cfg.UserIDPrefix, i)
	}

	return &Runner{
		cfg:       cfg,
		client:    evalClient,
		collector: metrics.NewCollector(),
		targets:   targets,
		userIDs:   userIDs,
	}
}

// Run executes the stress test according to the configured mode.
func (r *Runner) Run(ctx context.Context) *metrics.TestResult {
	// Warm-up phase
	if r.cfg.WarmUp > 0 {
		fmt.Printf("\n  Warming up for %s...\n", r.cfg.WarmUp)
		warmupCtx, warmupCancel := context.WithTimeout(ctx, r.cfg.WarmUp)
		warmupCollector := metrics.NewCollector()
		warmupCollector.Start()
		r.execute(warmupCtx, warmupCollector, r.cfg.ModeRPS())
		warmupCancel()
		fmt.Printf("  Warm-up complete. Starting test...\n\n")
	}

	// Main test
	r.collector.Start()
	testCtx, testCancel := context.WithTimeout(ctx, r.cfg.Duration)
	defer testCancel()

	// Live progress reporter
	go r.reportProgress(testCtx)

	switch r.cfg.Mode {
	case config.ModeConstant:
		r.runConstant(testCtx)
	case config.ModeRampUp:
		r.runRampUp(testCtx)
	case config.ModeSpike:
		r.runSpike(testCtx)
	case config.ModeSoak:
		r.runSoak(testCtx)
	}

	return r.collector.Result()
}

// execute runs workers at a given RPS until context is done.
func (r *Runner) execute(ctx context.Context, collector *metrics.Collector, targetRPS int) {
	var wg sync.WaitGroup

	interval := time.Second / time.Duration(targetRPS)
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	sem := make(chan struct{}, r.cfg.Concurrency)

	for {
		select {
		case <-ctx.Done():
			wg.Wait()
			return
		case <-ticker.C:
			select {
			case sem <- struct{}{}:
				wg.Add(1)
				go func() {
					defer wg.Done()
					defer func() { <-sem }()

					target := r.randomTarget()
					userID := r.randomUserID()

					collector.InFlightInc()
					result := r.client.Do(ctx, target, userID)
					collector.InFlightDec()

					collector.Record(result)
				}()
			default:
				// Workers saturated, drop this tick
			}
		}
	}
}

// executeWithDynamicRPS runs workers with a dynamically changing RPS.
func (r *Runner) executeWithDynamicRPS(ctx context.Context, getRPS func() int) {
	var wg sync.WaitGroup
	sem := make(chan struct{}, r.cfg.Concurrency)

	baseTicker := time.NewTicker(1 * time.Millisecond)
	defer baseTicker.Stop()

	var tokenAccum float64
	lastTick := time.Now()

	for {
		select {
		case <-ctx.Done():
			wg.Wait()
			return
		case now := <-baseTicker.C:
			elapsed := now.Sub(lastTick).Seconds()
			lastTick = now

			currentRPS := getRPS()
			tokenAccum += float64(currentRPS) * elapsed

			for tokenAccum >= 1.0 {
				tokenAccum -= 1.0

				select {
				case sem <- struct{}{}:
					wg.Add(1)
					go func() {
						defer wg.Done()
						defer func() { <-sem }()

						target := r.randomTarget()
						userID := r.randomUserID()

						r.collector.InFlightInc()
						result := r.client.Do(ctx, target, userID)
						r.collector.InFlightDec()

						r.collector.Record(result)
					}()
				default:
					// Workers saturated
				}
			}
		}
	}
}

func (r *Runner) reportProgress(ctx context.Context) {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	totalSec := r.cfg.Duration.Seconds()
	envCount := countUniqueEnvs(r.targets)
	startTime := time.Now()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			_, _, failed, rps, inFlight := r.collector.Snapshot()
			elapsed := time.Since(startTime).Seconds()

			latest := r.collector.TimeSeries.Latest()
			p99 := latest.P99Latency

			metrics.PrintLiveProgress(elapsed, totalSec, rps, p99, failed, envCount, inFlight)
		}
	}
}

func (r *Runner) randomTarget() client.TestTarget {
	return r.targets[rand.IntN(len(r.targets))]
}

func (r *Runner) randomUserID() string {
	return r.userIDs[rand.IntN(len(r.userIDs))]
}

func countUniqueEnvs(targets []client.TestTarget) int {
	seen := make(map[string]struct{})
	for _, t := range targets {
		seen[t.EnvironmentKey] = struct{}{}
	}
	return len(seen)
}

// BuildTargets creates test targets from discovered environments and flags.
func BuildTargets(envs []struct{ Key, APIKey string }, flagKeys []string, endpoint config.Endpoint) []client.TestTarget {
	var targets []client.TestTarget

	for _, env := range envs {
		switch endpoint {
		case config.EndpointBulk:
			targets = append(targets, client.TestTarget{
				EnvironmentKey: env.Key,
				APIKey:         env.APIKey,
				IsBulk:         true,
			})
		case config.EndpointSingle:
			for _, fk := range flagKeys {
				targets = append(targets, client.TestTarget{
					EnvironmentKey: env.Key,
					APIKey:         env.APIKey,
					FlagKey:        fk,
					IsBulk:         false,
				})
			}
		case config.EndpointBoth:
			targets = append(targets, client.TestTarget{
				EnvironmentKey: env.Key,
				APIKey:         env.APIKey,
				IsBulk:         true,
			})
			for _, fk := range flagKeys {
				targets = append(targets, client.TestTarget{
					EnvironmentKey: env.Key,
					APIKey:         env.APIKey,
					FlagKey:        fk,
					IsBulk:         false,
				})
			}
		}
	}

	return targets
}
