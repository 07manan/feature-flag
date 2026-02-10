package subscriber

import (
	"context"
	"encoding/json"
	"log/slog"
	"strings"

	"github.com/redis/go-redis/v9"

	"github.com/manan/feature-flag/evaluation-api/internal/cache"
)

// CacheInvalidationEvent represents an event published by the admin-api
type CacheInvalidationEvent struct {
	Type           string `json:"type"`
	FlagKey        string `json:"flagKey,omitempty"`
	EnvironmentKey string `json:"environmentKey,omitempty"`
	EnvironmentID  string `json:"environmentId,omitempty"`
}

// Subscriber listens for cache invalidation events
type Subscriber struct {
	client *redis.Client
	cache  cache.Cache
	logger *slog.Logger
	cancel context.CancelFunc
}

// New creates a new cache invalidation subscriber
func New(client *redis.Client, c cache.Cache, logger *slog.Logger) *Subscriber {
	return &Subscriber{
		client: client,
		cache:  c,
		logger: logger,
	}
}

// Start begins listening for cache invalidation events
func (s *Subscriber) Start(ctx context.Context) {
	ctx, cancel := context.WithCancel(ctx)
	s.cancel = cancel

	pubsub := s.client.PSubscribe(ctx,
		"flag:*",
		"flag-value:*",
		"environment:*",
	)

	go func() {
		defer pubsub.Close()

		s.logger.Info("cache invalidation subscriber started")

		ch := pubsub.Channel()
		for {
			select {
			case <-ctx.Done():
				s.logger.Info("cache invalidation subscriber stopped")
				return
			case msg := <-ch:
				if msg == nil {
					continue
				}
				s.handleMessage(ctx, msg)
			}
		}
	}()
}

// Stop gracefully stops the subscriber
func (s *Subscriber) Stop() {
	if s.cancel != nil {
		s.cancel()
	}
}

func (s *Subscriber) handleMessage(ctx context.Context, msg *redis.Message) {
	s.logger.Debug("received invalidation event", "channel", msg.Channel, "payload", msg.Payload)

	var event CacheInvalidationEvent
	if err := json.Unmarshal([]byte(msg.Payload), &event); err != nil {
		s.logger.Error("failed to unmarshal invalidation event", "error", err, "payload", msg.Payload)
		return
	}

	channel := msg.Channel

	switch {
	case strings.HasPrefix(channel, "flag:"):
		s.handleFlagEvent(ctx, channel, event)
	case strings.HasPrefix(channel, "flag-value:"):
		s.handleFlagValueEvent(ctx, channel, event)
	case strings.HasPrefix(channel, "environment:"):
		s.handleEnvironmentEvent(ctx, channel, event)
	default:
		s.logger.Warn("unknown channel", "channel", channel)
	}
}

func (s *Subscriber) handleFlagEvent(ctx context.Context, channel string, event CacheInvalidationEvent) {
	keysToDelete := []string{
		cache.FlagByKeyKey(event.FlagKey),
		cache.KeyAllActiveFlags,
	}

	if err := s.cache.Delete(ctx, keysToDelete...); err != nil {
		s.logger.Error("failed to invalidate flag cache", "error", err, "flagKey", event.FlagKey)
		return
	}

	s.logger.Info("invalidated flag cache", "channel", channel, "flagKey", event.FlagKey)
}

func (s *Subscriber) handleFlagValueEvent(ctx context.Context, channel string, event CacheInvalidationEvent) {
	keysToDelete := []string{}

	if event.EnvironmentID != "" {
		keysToDelete = append(keysToDelete, cache.FlagValuesEnvKey(event.EnvironmentID))
	}

	if event.FlagKey != "" {
		keysToDelete = append(keysToDelete, cache.FlagByKeyKey(event.FlagKey))
	}

	if len(keysToDelete) > 0 {
		if err := s.cache.Delete(ctx, keysToDelete...); err != nil {
			s.logger.Error("failed to invalidate flag-value cache", "error", err)
			return
		}
	}

	// Delete variants by pattern (we don't know the exact flag value ID)
	if err := s.cache.DeletePattern(ctx, cache.KeyPrefixVariants+"*"); err != nil {
		s.logger.Error("failed to invalidate variants cache", "error", err)
		return
	}

	// Delete flag value cache by pattern
	if err := s.cache.DeletePattern(ctx, cache.KeyPrefixFlagValue+"*"); err != nil {
		s.logger.Error("failed to invalidate flag value cache", "error", err)
		return
	}

	s.logger.Info("invalidated flag-value cache", "channel", channel, "environmentId", event.EnvironmentID)
}

func (s *Subscriber) handleEnvironmentEvent(ctx context.Context, channel string, event CacheInvalidationEvent) {
	keysToDelete := []string{}

	if event.EnvironmentID != "" {
		keysToDelete = append(keysToDelete, cache.FlagValuesEnvKey(event.EnvironmentID))
	}

	if len(keysToDelete) > 0 {
		if err := s.cache.Delete(ctx, keysToDelete...); err != nil {
			s.logger.Error("failed to invalidate environment cache", "error", err)
		}
	}

	// Delete all API key caches (we don't know which one changed)
	if err := s.cache.DeletePattern(ctx, cache.KeyPrefixEnvAPIKey+"*"); err != nil {
		s.logger.Error("failed to invalidate API key cache", "error", err)
		return
	}

	s.logger.Info("invalidated environment cache", "channel", channel, "environmentKey", event.EnvironmentKey)
}
