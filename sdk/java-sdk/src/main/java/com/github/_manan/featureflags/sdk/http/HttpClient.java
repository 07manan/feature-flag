package com.github._manan.featureflags.sdk.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.sdk.exception.AuthenticationException;
import com.github._manan.featureflags.sdk.exception.FeatureFlagException;
import com.github._manan.featureflags.sdk.exception.FlagNotFoundException;
import com.github._manan.featureflags.sdk.model.EvaluationResult;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    
    private final String baseUrl;
    private final String apiKey;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new HTTP client with specified configuration.
     *
     * @param baseUrl the base URL of the evaluation API
     * @param apiKey the API key for authentication
     * @param connectionTimeout connection timeout
     * @param socketTimeout socket/read timeout
     * @param timeUnit time unit for timeouts
     */
    public HttpClient(String baseUrl, String apiKey, long connectionTimeout, long socketTimeout, TimeUnit timeUnit) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(connectionTimeout, timeUnit))
                .setResponseTimeout(Timeout.of(socketTimeout, timeUnit))
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.debug("HttpClient initialized with baseUrl: {}", baseUrl);
    }

    /**
     * Evaluates a single flag for a specific user.
     *
     * @param flagKey the flag key to evaluate
     * @param userId the user ID (can be null)
     * @return the evaluation result
     * @throws AuthenticationException if authentication fails (401)
     * @throws FlagNotFoundException if the flag is not found (404)
     * @throws FeatureFlagException for other errors
     */
    public EvaluationResult evaluateFlag(String flagKey, String userId) {
        String encodedFlagKey = URLEncoder.encode(flagKey, StandardCharsets.UTF_8);
        StringBuilder urlBuilder = new StringBuilder()
                .append(baseUrl)
                .append("/evaluate/")
                .append(encodedFlagKey);
        
        if (userId != null && !userId.isEmpty()) {
            String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
            urlBuilder.append("?user=").append(encodedUserId);
        }
        
        String url = urlBuilder.toString();
        logger.debug("Evaluating flag: {} for user: {}", flagKey, userId);
        
        HttpGet request = new HttpGet(url);
        request.setHeader(API_KEY_HEADER, apiKey);
        
        try {
            return httpClient.execute(request, new EvaluationResponseHandler(flagKey));
        } catch (IOException e) {
            throw new FeatureFlagException("Failed to evaluate flag: " + flagKey, e);
        }
    }

    /**
     * Evaluates all active flags for a specific user.
     *
     * @param userId the user ID (can be null)
     * @return a map of flag keys to their evaluation results
     * @throws AuthenticationException if authentication fails (401)
     * @throws FeatureFlagException for other errors
     */
    public Map<String, EvaluationResult> evaluateAllFlags(String userId) {
        StringBuilder urlBuilder = new StringBuilder()
                .append(baseUrl)
                .append("/evaluate");
        
        if (userId != null && !userId.isEmpty()) {
            String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
            urlBuilder.append("?user=").append(encodedUserId);
        }
        
        String url = urlBuilder.toString();
        logger.debug("Evaluating all flags for user: {}", userId);
        
        HttpGet request = new HttpGet(url);
        request.setHeader(API_KEY_HEADER, apiKey);
        
        try {
            return httpClient.execute(request, new BulkEvaluationResponseHandler());
        } catch (IOException e) {
            throw new FeatureFlagException("Failed to evaluate all flags", e);
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
            logger.debug("HttpClient closed");
        } catch (IOException e) {
            logger.warn("Error closing HttpClient", e);
        }
    }

    private class EvaluationResponseHandler implements HttpClientResponseHandler<EvaluationResult> {
        private final String flagKey;

        EvaluationResponseHandler(String flagKey) {
            this.flagKey = flagKey;
        }

        @Override
        public EvaluationResult handleResponse(ClassicHttpResponse response) throws IOException {
            int statusCode = response.getCode();
            
            if (statusCode == HttpStatus.SC_OK) {
                try (InputStream content = response.getEntity().getContent()) {
                    return objectMapper.readValue(content, EvaluationResult.class);
                }
            } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new AuthenticationException("Invalid or missing API key");
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                throw new FlagNotFoundException(flagKey);
            } else {
                String errorMsg = String.format("API request failed with status code: %d", statusCode);
                logger.error(errorMsg);
                throw new FeatureFlagException(errorMsg);
            }
        }
    }

    private class BulkEvaluationResponseHandler implements HttpClientResponseHandler<Map<String, EvaluationResult>> {
        @Override
        public Map<String, EvaluationResult> handleResponse(ClassicHttpResponse response) throws IOException {
            int statusCode = response.getCode();
            
            if (statusCode == HttpStatus.SC_OK) {
                try (InputStream content = response.getEntity().getContent()) {

                    Map<String, Object> wrapper = objectMapper.readValue(content, new TypeReference<>() {});
                    Object flagsObj = wrapper.get("flags");
                    
                    if (flagsObj == null) {
                        throw new FeatureFlagException("Invalid API response: missing 'flags' field");
                    }
                    
                    return objectMapper.convertValue(flagsObj, new TypeReference<>() {});
                }
            } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new AuthenticationException("Invalid or missing API key");
            } else {
                String errorMsg = String.format("API request failed with status code: %d", statusCode);
                logger.error(errorMsg);
                throw new FeatureFlagException(errorMsg);
            }
        }
    }
}
