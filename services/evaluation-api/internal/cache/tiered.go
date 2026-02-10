package cache

import (
	"context"
	"log/slog"
	"time"

	"github.com/redis/go-redis/v9"
)

// TieredCache implements two-tier caching with L1 (memory) and L2 (Redis)
type TieredCache struct {
	l1     *MemoryCache
	l2     *RedisCache
	logger *slog.Logger
}

// NewTieredCache creates a new tiered cache with L1 memory and L2 Redis
func NewTieredCache(l1 *MemoryCache, l2 *RedisCache, logger *slog.Logger) *TieredCache {
	logger.Info("initialized tiered cache (L1: memory, L2: redis)")
	return &TieredCache{
		l1:     l1,
		l2:     l2,
		logger: logger,
	}
}

// Get retrieves a value, checking L1 first, then L2
func (c *TieredCache) Get(ctx context.Context, key string, dest interface{}) error {
	start := time.Now()

	// Try L1 (memory) first
	if err := c.l1.Get(ctx, key, dest); err == nil {
		return nil
	}

	// L1 miss - try L2 (Redis)
	if err := c.l2.Get(ctx, key, dest); err != nil {
		return err
	}

	// L2 hit - populate L1 for next time
	if err := c.l1.Set(ctx, key, dest); err != nil {
		c.logger.Warn("failed to populate L1 from L2", "error", err, "key", key)
	}

	c.logger.Debug("tiered cache L2 hit, populated L1", "key", key, "duration", time.Since(start))
	return nil
}

// Set stores a value in both L1 and L2
func (c *TieredCache) Set(ctx context.Context, key string, value interface{}) error {
	// Set in L1 first (fast)
	if err := c.l1.Set(ctx, key, value); err != nil {
		c.logger.Warn("failed to set L1 cache", "error", err, "key", key)
	}

	// Set in L2 (Redis)
	if err := c.l2.Set(ctx, key, value); err != nil {
		return err
	}

	return nil
}

// Delete removes keys from both L1 and L2
func (c *TieredCache) Delete(ctx context.Context, keys ...string) error {
	// Delete from L1
	if err := c.l1.Delete(ctx, keys...); err != nil {
		c.logger.Warn("failed to delete from L1 cache", "error", err, "keys", keys)
	}

	// Delete from L2
	if err := c.l2.Delete(ctx, keys...); err != nil {
		return err
	}

	c.logger.Debug("tiered cache delete", "keys", keys)
	return nil
}

// DeletePattern removes all keys matching a pattern from both L1 and L2
func (c *TieredCache) DeletePattern(ctx context.Context, pattern string) error {
	// Delete from L1 (clears all due to ristretto limitation)
	if err := c.l1.DeletePattern(ctx, pattern); err != nil {
		c.logger.Warn("failed to delete pattern from L1 cache", "error", err, "pattern", pattern)
	}

	// Delete from L2
	if err := c.l2.DeletePattern(ctx, pattern); err != nil {
		return err
	}

	c.logger.Debug("tiered cache delete pattern", "pattern", pattern)
	return nil
}

// Close closes both cache layers
func (c *TieredCache) Close() error {
	c.l1.Close()
	return c.l2.Close()
}

// Client returns the Redis client for pub/sub operations
func (c *TieredCache) Client() *redis.Client {
	return c.l2.Client()
}

// L1Metrics returns L1 cache metrics for debugging
func (c *TieredCache) L1Metrics() interface{} {
	return c.l1.Metrics()
}
