package cache

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"time"

	"github.com/redis/go-redis/v9"

	"github.com/manan/feature-flag/evaluation-api/internal/config"
)

var (
	ErrCacheMiss = errors.New("cache miss")
)

// Cache defines the interface for caching operations
type Cache interface {
	Get(ctx context.Context, key string, dest interface{}) error
	Set(ctx context.Context, key string, value interface{}) error
	Delete(ctx context.Context, keys ...string) error
	DeletePattern(ctx context.Context, pattern string) error
	Close() error
}

// RedisCache implements Cache using Redis
type RedisCache struct {
	client *redis.Client
	ttl    time.Duration
	logger *slog.Logger
}

// NewRedisCache creates a new Redis cache client
func NewRedisCache(cfg config.RedisConfig, logger *slog.Logger) (*RedisCache, error) {
	client := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%d", cfg.Host, cfg.Port),
		Password: cfg.Password,
		DB:       cfg.DB,
		PoolSize: cfg.PoolSize,
	})

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to redis: %w", err)
	}

	logger.Info("connected to redis", "host", cfg.Host, "port", cfg.Port)

	return &RedisCache{
		client: client,
		ttl:    cfg.TTL,
		logger: logger,
	}, nil
}

// Get retrieves a value from cache
func (c *RedisCache) Get(ctx context.Context, key string, dest interface{}) error {
	data, err := c.client.Get(ctx, key).Bytes()
	if err != nil {
		if errors.Is(err, redis.Nil) {
			return ErrCacheMiss
		}
		return fmt.Errorf("redis get: %w", err)
	}

	if err := json.Unmarshal(data, dest); err != nil {
		return fmt.Errorf("unmarshal cached value: %w", err)
	}

	c.logger.Debug("cache hit", "key", key)
	return nil
}

// Set stores a value in cache
func (c *RedisCache) Set(ctx context.Context, key string, value interface{}) error {
	data, err := json.Marshal(value)
	if err != nil {
		return fmt.Errorf("marshal value for cache: %w", err)
	}

	if err := c.client.Set(ctx, key, data, c.ttl).Err(); err != nil {
		return fmt.Errorf("redis set: %w", err)
	}

	c.logger.Debug("cache set", "key", key, "ttl", c.ttl)
	return nil
}

// Delete removes keys from cache
func (c *RedisCache) Delete(ctx context.Context, keys ...string) error {
	if len(keys) == 0 {
		return nil
	}

	if err := c.client.Del(ctx, keys...).Err(); err != nil {
		return fmt.Errorf("redis delete: %w", err)
	}

	c.logger.Debug("cache delete", "keys", keys)
	return nil
}

// DeletePattern removes all keys matching a pattern
func (c *RedisCache) DeletePattern(ctx context.Context, pattern string) error {
	iter := c.client.Scan(ctx, 0, pattern, 100).Iterator()
	var keys []string

	for iter.Next(ctx) {
		keys = append(keys, iter.Val())
	}

	if err := iter.Err(); err != nil {
		return fmt.Errorf("redis scan: %w", err)
	}

	if len(keys) > 0 {
		if err := c.client.Del(ctx, keys...).Err(); err != nil {
			return fmt.Errorf("redis delete pattern: %w", err)
		}
		c.logger.Debug("cache delete pattern", "pattern", pattern, "deleted", len(keys))
	}

	return nil
}

// Close closes the Redis connection
func (c *RedisCache) Close() error {
	return c.client.Close()
}

// Client returns the underlying Redis client for pub/sub operations
func (c *RedisCache) Client() *redis.Client {
	return c.client
}

// Cache key helpers
const (
	KeyPrefixEnvAPIKey     = "env:apikey:"
	KeyPrefixFlagByKey     = "flag:key:"
	KeyAllActiveFlags      = "flags:active"
	KeyPrefixFlagValuesEnv = "flagvalues:env:"
	KeyPrefixVariants      = "variants:fv:"
	KeyPrefixFlagValue     = "flagvalue:"
)

// EnvByAPIKeyKey returns the cache key for environment by API key
func EnvByAPIKeyKey(apiKey string) string {
	return KeyPrefixEnvAPIKey + apiKey
}

// FlagByKeyKey returns the cache key for flag by key
func FlagByKeyKey(flagKey string) string {
	return KeyPrefixFlagByKey + flagKey
}

// FlagValuesEnvKey returns the cache key for flag values by environment
func FlagValuesEnvKey(envID string) string {
	return KeyPrefixFlagValuesEnv + envID
}

// VariantsKey returns the cache key for variants by flag value ID
func VariantsKey(flagValueID string) string {
	return KeyPrefixVariants + flagValueID
}

// FlagValueKey returns the cache key for a specific flag value
func FlagValueKey(flagID, envID string) string {
	return KeyPrefixFlagValue + flagID + ":" + envID
}
