package runner

import (
	"context"
	"sync/atomic"
	"time"
)

// runSpike runs at SpikeBaseRPS, spikes to SpikePeakRPS halfway through, then returns to base.
func (r *Runner) runSpike(ctx context.Context) {
	baseRPS := r.cfg.SpikeBaseRPS
	peakRPS := r.cfg.SpikePeakRPS
	spikeDur := r.cfg.SpikeDuration
	totalDur := r.cfg.Duration

	// Spike starts at the midpoint
	spikeStart := (totalDur - spikeDur) / 2
	spikeEnd := spikeStart + spikeDur

	var currentRPS atomic.Int64
	currentRPS.Store(int64(baseRPS))

	startTime := time.Now()

	// Spike controller goroutine
	go func() {
		ticker := time.NewTicker(100 * time.Millisecond)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				elapsed := time.Since(startTime)
				if elapsed >= spikeStart && elapsed < spikeEnd {
					currentRPS.Store(int64(peakRPS))
				} else {
					currentRPS.Store(int64(baseRPS))
				}
			}
		}
	}()

	r.executeWithDynamicRPS(ctx, func() int {
		return int(currentRPS.Load())
	})
}
