package runner

import (
	"context"
)

// runConstant runs a constant-rate stress test at the configured TargetRPS.
func (r *Runner) runConstant(ctx context.Context) {
	r.execute(ctx, r.collector, r.cfg.TargetRPS)
}
