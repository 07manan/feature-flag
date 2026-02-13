package runner

import (
	"context"
	"sync/atomic"
	"time"
)

// runRampUp gradually increases RPS from RampStartRPS to RampEndRPS over the test duration.
func (r *Runner) runRampUp(ctx context.Context) {
	startRPS := r.cfg.RampStartRPS
	endRPS := r.cfg.RampEndRPS
	stepDuration := r.cfg.RampStepDuration

	totalSteps := int(r.cfg.Duration / stepDuration)
	if totalSteps < 1 {
		totalSteps = 1
	}
	rpsIncrement := float64(endRPS-startRPS) / float64(totalSteps)

	var currentRPS atomic.Int64
	currentRPS.Store(int64(startRPS))

	// Step updater goroutine
	go func() {
		ticker := time.NewTicker(stepDuration)
		defer ticker.Stop()
		step := 0
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				step++
				newRPS := startRPS + int(float64(step)*rpsIncrement)
				if newRPS > endRPS {
					newRPS = endRPS
				}
				currentRPS.Store(int64(newRPS))
			}
		}
	}()

	r.executeWithDynamicRPS(ctx, func() int {
		return int(currentRPS.Load())
	})
}
