package com.github._manan.featureflags.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes cache invalidation events to Redis pub/sub channels.
 * The evaluation-api subscribes to these events to invalidate its cache.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Channel names
    private static final String CHANNEL_FLAG_CREATED = "flag:created";
    private static final String CHANNEL_FLAG_UPDATED = "flag:updated";
    private static final String CHANNEL_FLAG_DELETED = "flag:deleted";
    private static final String CHANNEL_FLAG_VALUE_CREATED = "flag-value:created";
    private static final String CHANNEL_FLAG_VALUE_UPDATED = "flag-value:updated";
    private static final String CHANNEL_FLAG_VALUE_DELETED = "flag-value:deleted";
    private static final String CHANNEL_ENVIRONMENT_DELETED = "environment:deleted";
    private static final String CHANNEL_ENVIRONMENT_API_KEY_REGENERATED = "environment:apikey-regenerated";

    /**
     * Publishes a flag created event
     */
    public void publishFlagCreated(String flagKey) {
        publish(CHANNEL_FLAG_CREATED, buildFlagEvent("created", flagKey));
    }

    /**
     * Publishes a flag updated event
     */
    public void publishFlagUpdated(String flagKey) {
        publish(CHANNEL_FLAG_UPDATED, buildFlagEvent("updated", flagKey));
    }

    /**
     * Publishes a flag deleted event
     */
    public void publishFlagDeleted(String flagKey) {
        publish(CHANNEL_FLAG_DELETED, buildFlagEvent("deleted", flagKey));
    }

    /**
     * Publishes a flag value created event
     */
    public void publishFlagValueCreated(String environmentKey, UUID environmentId, String flagKey) {
        publish(CHANNEL_FLAG_VALUE_CREATED, buildFlagValueEvent("created", environmentKey, environmentId, flagKey));
    }

    /**
     * Publishes a flag value updated event
     */
    public void publishFlagValueUpdated(String environmentKey, UUID environmentId, String flagKey) {
        publish(CHANNEL_FLAG_VALUE_UPDATED, buildFlagValueEvent("updated", environmentKey, environmentId, flagKey));
    }

    /**
     * Publishes a flag value deleted event
     */
    public void publishFlagValueDeleted(String environmentKey, UUID environmentId, String flagKey) {
        publish(CHANNEL_FLAG_VALUE_DELETED, buildFlagValueEvent("deleted", environmentKey, environmentId, flagKey));
    }

    /**
     * Publishes an environment deleted event
     */
    public void publishEnvironmentDeleted(String environmentKey, UUID environmentId) {
        publish(CHANNEL_ENVIRONMENT_DELETED, buildEnvironmentEvent("deleted", environmentKey, environmentId));
    }

    /**
     * Publishes an environment API key regenerated event
     */
    public void publishEnvironmentApiKeyRegenerated(String environmentKey, UUID environmentId) {
        publish(CHANNEL_ENVIRONMENT_API_KEY_REGENERATED, buildEnvironmentEvent("apikey-regenerated", environmentKey, environmentId));
    }

    private Map<String, Object> buildFlagEvent(String type, String flagKey) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("flagKey", flagKey);
        return event;
    }

    private Map<String, Object> buildFlagValueEvent(String type, String environmentKey, UUID environmentId, String flagKey) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("environmentKey", environmentKey);
        event.put("environmentId", environmentId.toString());
        event.put("flagKey", flagKey);
        return event;
    }

    private Map<String, Object> buildEnvironmentEvent(String type, String environmentKey, UUID environmentId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("environmentKey", environmentKey);
        event.put("environmentId", environmentId.toString());
        return event;
    }

    private void publish(String channel, Map<String, Object> event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, message);
            log.debug("Published cache invalidation event to channel '{}': {}", channel, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cache invalidation event", e);
        } catch (Exception e) {
            log.error("Failed to publish cache invalidation event to channel '{}'", channel, e);
        }
    }
}
