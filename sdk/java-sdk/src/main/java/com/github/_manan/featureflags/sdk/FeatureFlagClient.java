package com.github._manan.featureflags.sdk;

import com.github._manan.featureflags.sdk.cache.LocalCache;
import com.github._manan.featureflags.sdk.exception.AuthenticationException;
import com.github._manan.featureflags.sdk.exception.FeatureFlagException;
import com.github._manan.featureflags.sdk.exception.FlagNotFoundException;
import com.github._manan.featureflags.sdk.http.HttpClient;
import com.github._manan.featureflags.sdk.model.EvaluationResult;
import com.github._manan.featureflags.sdk.model.FlagType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main client for evaluating feature flags with local caching.
 * <p>
 * This client provides type-safe methods for evaluating boolean, string, and numeric flags.
 * Results are cached locally with a configurable TTL (default 30 seconds) to reduce API calls.
 * <p>
 * Example usage:
 * <pre>
 * FeatureFlagClient client = FeatureFlagClient.builder()
 *     .apiKey("ff_env_xxxxx")
 *     .build();
 * 
 * boolean feature = client.getBooleanFlag("new-feature", "user-123", false);
 * String theme = client.getStringFlag("theme-color", "user-123", "blue");
 * int limit = client.getIntFlag("rate-limit", "user-123", 100);
 * 
 * client.close(); // Clean up resources when done
 * </pre>
 */
public class FeatureFlagClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagClient.class);
    
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final LocalCache<EvaluationResult> cache;

    FeatureFlagClient(
            String apiKey,
            String baseUrl,
            long cacheTTL,
            TimeUnit cacheTTLUnit,
            long connectionTimeout,
            long socketTimeout,
            TimeUnit httpTimeoutUnit) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient(baseUrl, apiKey, connectionTimeout, socketTimeout, httpTimeoutUnit);
        this.cache = new LocalCache<>(cacheTTL, cacheTTLUnit);
        
        logger.info("FeatureFlagClient initialized with baseUrl: {}", baseUrl);
    }

    /**
     * Creates a new builder for constructing a {@link FeatureFlagClient}.
     *
     * @return a new builder instance
     */
    public static FeatureFlagClientBuilder builder() {
        return new FeatureFlagClientBuilder();
    }

    /**
     * Evaluates a boolean flag.
     *
     * @param flagKey the flag key
     * @param userId the user ID (can be null for non-percentage rollouts)
     * @param defaultValue the default value to return if flag not found or on error
     * @return the evaluated boolean value
     */
    public boolean getBooleanFlag(String flagKey, String userId, boolean defaultValue) {
        try {
            EvaluationResult result = evaluateFlag(flagKey, userId);
            
            if (result.getType() != FlagType.BOOLEAN) {
                logger.warn("Flag '{}' type mismatch: expected BOOLEAN, got {}", flagKey, result.getType());
                return defaultValue;
            }
            
            return result.getBooleanValue();
        } catch (FlagNotFoundException e) {
            logger.debug("Flag '{}' not found, returning default: {}", flagKey, defaultValue);
            return defaultValue;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error evaluating boolean flag '{}', returning default: {}", flagKey, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Evaluates a string flag.
     *
     * @param flagKey the flag key
     * @param userId the user ID (can be null for non-percentage rollouts)
     * @param defaultValue the default value to return if flag not found or on error
     * @return the evaluated string value
     */
    public String getStringFlag(String flagKey, String userId, String defaultValue) {
        try {
            EvaluationResult result = evaluateFlag(flagKey, userId);
            
            if (result.getType() != FlagType.STRING) {
                logger.warn("Flag '{}' type mismatch: expected STRING, got {}", flagKey, result.getType());
                return defaultValue;
            }
            
            return result.getStringValue();
        } catch (FlagNotFoundException e) {
            logger.debug("Flag '{}' not found, returning default: {}", flagKey, defaultValue);
            return defaultValue;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error evaluating string flag '{}', returning default: {}", flagKey, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Evaluates an integer flag.
     *
     * @param flagKey the flag key
     * @param userId the user ID (can be null for non-percentage rollouts)
     * @param defaultValue the default value to return if flag not found or on error
     * @return the evaluated integer value
     */
    public int getIntFlag(String flagKey, String userId, int defaultValue) {
        try {
            EvaluationResult result = evaluateFlag(flagKey, userId);
            
            if (result.getType() != FlagType.NUMBER) {
                logger.warn("Flag '{}' type mismatch: expected NUMBER, got {}", flagKey, result.getType());
                return defaultValue;
            }
            
            return result.getIntValue();
        } catch (FlagNotFoundException e) {
            logger.debug("Flag '{}' not found, returning default: {}", flagKey, defaultValue);
            return defaultValue;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error evaluating int flag '{}', returning default: {}", flagKey, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Evaluates a double flag.
     *
     * @param flagKey the flag key
     * @param userId the user ID (can be null for non-percentage rollouts)
     * @param defaultValue the default value to return if flag not found or on error
     * @return the evaluated double value
     */
    public double getDoubleFlag(String flagKey, String userId, double defaultValue) {
        try {
            EvaluationResult result = evaluateFlag(flagKey, userId);
            
            if (result.getType() != FlagType.NUMBER) {
                logger.warn("Flag '{}' type mismatch: expected NUMBER, got {}", flagKey, result.getType());
                return defaultValue;
            }
            
            return result.getDoubleValue();
        } catch (FlagNotFoundException e) {
            logger.debug("Flag '{}' not found, returning default: {}", flagKey, defaultValue);
            return defaultValue;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error evaluating double flag '{}', returning default: {}", flagKey, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Evaluates all active flags for a user.
     * Returns a map of flag keys to their raw values (Boolean, String, or Number).
     *
     * @param userId the user ID (can be null for non-percentage rollouts)
     * @return a map of flag keys to their evaluated values
     * @throws AuthenticationException if authentication fails
     * @throws FeatureFlagException for other errors
     */
    public Map<String, Object> getAllFlags(String userId) {
        try {
            Map<String, EvaluationResult> results = httpClient.evaluateAllFlags(userId);
            Map<String, Object> flags = new HashMap<>();
            
            for (Map.Entry<String, EvaluationResult> entry : results.entrySet()) {
                EvaluationResult result = entry.getValue();
                
                String cacheKey = buildCacheKey(result.getFlagKey(), userId);
                cache.put(cacheKey, result);
                
                flags.put(entry.getKey(), result.getValue());
            }
            
            logger.debug("Evaluated {} flags for user: {}", flags.size(), userId);
            return flags;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error evaluating all flags", e);
            throw new FeatureFlagException("Failed to evaluate all flags", e);
        }
    }

    public void invalidateCache(String flagKey, String userId) {
        String cacheKey = buildCacheKey(flagKey, userId);
        cache.invalidate(cacheKey);
        logger.debug("Invalidated cache for flag: {}, user: {}", flagKey, userId);
    }

    public void clearCache() {
        cache.clear();
        logger.debug("Cleared all cached flags");
    }

    /**
     * Closes the client and releases all resources.
     * Should be called when the client is no longer needed.
     */
    @Override
    public void close() {
        try {
            cache.shutdown();
            httpClient.close();
            logger.info("FeatureFlagClient closed");
        } catch (Exception e) {
            logger.error("Error closing FeatureFlagClient", e);
        }
    }

    private EvaluationResult evaluateFlag(String flagKey, String userId) {
        String cacheKey = buildCacheKey(flagKey, userId);
        
        EvaluationResult cached = cache.get(cacheKey);
        if (cached != null) {
            logger.trace("Cache hit for flag: {}, user: {}", flagKey, userId);
            return cached;
        }
        
        logger.trace("Cache miss for flag: {}, user: {}", flagKey, userId);
        EvaluationResult result = httpClient.evaluateFlag(flagKey, userId);
        
        cache.put(cacheKey, result);
        
        return result;
    }

    private String buildCacheKey(String flagKey, String userId) {
        return flagKey + ":" + (userId != null ? userId : "null");
    }
}
