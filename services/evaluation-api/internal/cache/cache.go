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

type Cache interface {
	Get(ctx context.Context, key string, dest interface{}) error
	Set(ctx context.Context, key string, value interface{}) error
	Delete(ctx context.Context, keys ...string) error
	DeletePattern(ctx context.Context, pattern string) error
	Ping(ctx context.Context) error
	Close() error
}

type RedisCache struct {
	client *redis.Client
	ttl    time.Duration
	logger *slog.Logger
}

func NewRedisCache(cfg config.RedisConfig, logger *slog.Logger) (*RedisCache, error) {
	var client *redis.Client

	// Prefer URL if provided, otherwise use individual fields
	if cfg.URL != "" {
		opt, err := redis.ParseURL(cfg.URL)
		if err != nil {
			return nil, fmt.Errorf("failed to parse redis URL: %w", err)
		}
		// Apply custom timeouts and pool size since ParseURL doesn't set them
		opt.PoolSize = cfg.PoolSize
		opt.DialTimeout = cfg.DialTimeout
		opt.ReadTimeout = cfg.ReadTimeout
		opt.WriteTimeout = cfg.WriteTimeout
		client = redis.NewClient(opt)
	} else {
		client = redis.NewClient(&redis.Options{
			Addr:         fmt.Sprintf("%s:%d", cfg.Host, cfg.Port),
			Password:     cfg.Password,
			DB:           cfg.DB,
			PoolSize:     cfg.PoolSize,
			DialTimeout:  cfg.DialTimeout,
			ReadTimeout:  cfg.ReadTimeout,
			WriteTimeout: cfg.WriteTimeout,
		})
	}

	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
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

func (c *RedisCache) Ping(ctx context.Context) error {
	return c.client.Ping(ctx).Err()
}

func (c *RedisCache) Close() error {
	return c.client.Close()
}

// Client returns the underlying Redis client, needed for pub/sub operations
func (c *RedisCache) Client() *redis.Client {
	return c.client
}

const (
	KeyPrefixEnvAPIKey     = "env:apikey:"
	KeyPrefixFlagByKey     = "flag:key:"
	KeyAllActiveFlags      = "flags:active"
	KeyPrefixFlagValuesEnv = "flagvalues:env:"
	KeyPrefixVariants      = "variants:fv:"
	KeyPrefixFlagValue     = "flagvalue:"
)

func EnvByAPIKeyKey(apiKey string) string {
	return KeyPrefixEnvAPIKey + apiKey
}

func FlagByKeyKey(flagKey string) string {
	return KeyPrefixFlagByKey + flagKey
}

func FlagValuesEnvKey(envID string) string {
	return KeyPrefixFlagValuesEnv + envID
}

func VariantsKey(flagValueID string) string {
	return KeyPrefixVariants + flagValueID
}

func FlagValueKey(flagID, envID string) string {
	return KeyPrefixFlagValue + flagID + ":" + envID
}
