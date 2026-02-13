package runner

import (
	"context"
)

// runSoak runs a steady-state test at SoakRPS for the configured duration.
// Designed for long-running tests (10m-1h) to detect memory leaks, connection
// pool exhaustion, latency drift, or degradation under sustained load.
func (r *Runner) runSoak(ctx context.Context) {
	r.execute(ctx, r.collector, r.cfg.SoakRPS)
}
