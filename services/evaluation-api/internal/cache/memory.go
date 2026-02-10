package cache

import (
	"context"
	"log/slog"
	"reflect"
	"time"

	"github.com/dgraph-io/ristretto"

	"github.com/manan/feature-flag/evaluation-api/internal/config"
)

// MemoryCache implements Cache using in-memory storage with ristretto
type MemoryCache struct {
	cache  *ristretto.Cache
	ttl    time.Duration
	logger *slog.Logger
}

// NewMemoryCache creates a new in-memory cache
func NewMemoryCache(cfg config.MemoryCacheConfig, logger *slog.Logger) (*MemoryCache, error) {
	cache, err := ristretto.NewCache(&ristretto.Config{
		NumCounters: cfg.NumCounters, // number of keys to track frequency of
		MaxCost:     cfg.MaxSize,     // maximum size in bytes
		BufferItems: 64,              // number of keys per Get buffer
		Metrics:     true,            // enable metrics for debugging
	})
	if err != nil {
		return nil, err
	}

	logger.Info("initialized memory cache", "maxSize", cfg.MaxSize, "ttl", cfg.TTL)

	return &MemoryCache{
		cache:  cache,
		ttl:    cfg.TTL,
		logger: logger,
	}, nil
}

// Get retrieves a value from in-memory cache
func (c *MemoryCache) Get(ctx context.Context, key string, dest interface{}) error {
	start := time.Now()

	value, found := c.cache.Get(key)
	if !found {
		c.logger.Info("L1 cache miss", "key", key, "duration", time.Since(start))
		return ErrCacheMiss
	}

	// Use reflection to copy the value to dest
	destVal := reflect.ValueOf(dest)
	if destVal.Kind() != reflect.Ptr {
		return ErrCacheMiss
	}

	srcVal := reflect.ValueOf(value)
	if srcVal.Kind() == reflect.Ptr {
		srcVal = srcVal.Elem()
	}

	destVal.Elem().Set(srcVal)

	c.logger.Info("L1 cache hit", "key", key, "duration", time.Since(start))
	return nil
}

// Set stores a value in in-memory cache
func (c *MemoryCache) Set(ctx context.Context, key string, value interface{}) error {
	start := time.Now()

	// Make a copy of the value to store
	valueCopy := deepCopy(value)

	// Estimate cost as 1 for simplicity (ristretto will track actual memory)
	cost := int64(1)

	c.cache.SetWithTTL(key, valueCopy, cost, c.ttl)

	// Wait for value to pass through buffers (optional, for consistency)
	c.cache.Wait()

	c.logger.Debug("L1 cache set", "key", key, "duration", time.Since(start))
	return nil
}

// Delete removes keys from in-memory cache
func (c *MemoryCache) Delete(ctx context.Context, keys ...string) error {
	for _, key := range keys {
		c.cache.Del(key)
	}
	c.logger.Debug("L1 cache delete", "keys", keys)
	return nil
}

// DeletePattern removes all keys matching a pattern from in-memory cache
// Note: ristretto doesn't support pattern deletion, so we clear the entire cache
func (c *MemoryCache) DeletePattern(ctx context.Context, pattern string) error {
	c.cache.Clear()
	c.logger.Debug("L1 cache cleared due to pattern delete", "pattern", pattern)
	return nil
}

// Close closes the in-memory cache
func (c *MemoryCache) Close() error {
	c.cache.Close()
	return nil
}

// Metrics returns cache statistics for debugging
func (c *MemoryCache) Metrics() *ristretto.Metrics {
	return c.cache.Metrics
}

// deepCopy creates a copy of the value to avoid storing references
func deepCopy(value interface{}) interface{} {
	if value == nil {
		return nil
	}

	val := reflect.ValueOf(value)

	// If it's a pointer, dereference and copy the underlying value
	if val.Kind() == reflect.Ptr {
		if val.IsNil() {
			return nil
		}
		val = val.Elem()
	}

	// Create a new instance of the same type
	copyVal := reflect.New(val.Type()).Elem()
	copyVal.Set(val)

	return copyVal.Interface()
}
