package com.github._manan.featureflags.sdk;

import java.util.concurrent.TimeUnit;

/**
 * Builder for creating {@link FeatureFlagClient} instances.
 * Example usage:
 * <pre>
 * FeatureFlagClient client = FeatureFlagClient.builder()
 *     .apiKey("ff_production_xxxxx")
 *     .build();
 * </pre>
 */
public class FeatureFlagClientBuilder {
    private static final String DEFAULT_BASE_URL = "https://strong-lorena-07manan-b3c1d402.koyeb.app";
    private static final long DEFAULT_CACHE_TTL = 30;
    private static final TimeUnit DEFAULT_CACHE_TTL_UNIT = TimeUnit.SECONDS;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 5;
    private static final long DEFAULT_SOCKET_TIMEOUT = 10;
    private static final TimeUnit DEFAULT_HTTP_TIMEOUT_UNIT = TimeUnit.SECONDS;
    private static final String SYSTEM_PROPERTY_BASE_URL = "featureflags.baseUrl";
    
    private String apiKey;
    private String baseUrl;
    private long cacheTTL = DEFAULT_CACHE_TTL;
    private TimeUnit cacheTTLUnit = DEFAULT_CACHE_TTL_UNIT;
    private long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private long socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private TimeUnit httpTimeoutUnit = DEFAULT_HTTP_TIMEOUT_UNIT;

    FeatureFlagClientBuilder() {
    }

    /**
     * Sets the API key for authentication (required).
     *
     * @param apiKey the API key from your environment
     * @return this builder
     */
    public FeatureFlagClientBuilder apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Sets the base URL of the evaluation API (optional).
     * If not set, uses system property "featureflags.baseUrl" or defaults to production URL.
     *
     * @param baseUrl the base URL (e.g., "http://localhost:8081")
     * @return this builder
     */
    public FeatureFlagClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Sets the cache TTL (time-to-live) for local caching (optional).
     * Default is 30 seconds.
     *
     * @param ttl the TTL value
     * @param unit the time unit
     * @return this builder
     */
    public FeatureFlagClientBuilder cacheTTL(long ttl, TimeUnit unit) {
        this.cacheTTL = ttl;
        this.cacheTTLUnit = unit;
        return this;
    }

    /**
     * Sets HTTP timeouts for API requests (optional).
     * Default is 5s connection timeout, 10s socket timeout.
     *
     * @param connectionTimeout connection timeout value
     * @param socketTimeout socket/read timeout value
     * @param unit the time unit for both timeouts
     * @return this builder
     */
    public FeatureFlagClientBuilder httpTimeout(long connectionTimeout, long socketTimeout, TimeUnit unit) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.httpTimeoutUnit = unit;
        return this;
    }

    /**
     * Builds the {@link FeatureFlagClient} instance.
     *
     * @return a new FeatureFlagClient
     * @throws IllegalArgumentException if the API key is not set or invalid
     */
    public FeatureFlagClient build() {
        validateApiKey();
        resolveBaseUrl();
        
        return new FeatureFlagClient(
                apiKey,
                baseUrl,
                cacheTTL,
                cacheTTLUnit,
                connectionTimeout,
                socketTimeout,
                httpTimeoutUnit
        );
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }
        
        if (!apiKey.startsWith("ff_")) {
            throw new IllegalArgumentException(
                    "Invalid API key format. API key should start with 'ff_'"
            );
        }
    }

    private void resolveBaseUrl() {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            return;
        }
        
        String systemPropertyUrl = System.getProperty(SYSTEM_PROPERTY_BASE_URL);
        if (systemPropertyUrl != null && !systemPropertyUrl.trim().isEmpty()) {
            baseUrl = systemPropertyUrl;
            return;
        }
        
        baseUrl = DEFAULT_BASE_URL;
    }
}
