package com.github._manan.featureflags.sdk;

import com.github._manan.featureflags.sdk.exception.AuthenticationException;
import com.github._manan.featureflags.sdk.exception.FlagNotFoundException;
import com.github._manan.featureflags.sdk.http.HttpClient;
import com.github._manan.featureflags.sdk.model.EvaluationResult;
import com.github._manan.featureflags.sdk.model.FlagType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagClientTest {

    @Mock
    private HttpClient mockHttpClient;

    private FeatureFlagClient client;

    @BeforeEach
    void setUp() throws Exception {
        // Create a real client
        client = FeatureFlagClient.builder()
                .apiKey("ff_test_key123")
                .baseUrl("http://localhost:8081")
                .build();
        
        // Replace the httpClient with our mock using reflection
        Field httpClientField = FeatureFlagClient.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(client, mockHttpClient);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void testGetBooleanFlag_Success() {
        EvaluationResult result = new EvaluationResult("test-flag", true, FlagType.BOOLEAN, false, null);
        when(mockHttpClient.evaluateFlag("test-flag", "user-123")).thenReturn(result);

        boolean value = client.getBooleanFlag("test-flag", "user-123", false);

        assertTrue(value);
        verify(mockHttpClient).evaluateFlag("test-flag", "user-123");
    }

    @Test
    void testGetBooleanFlag_TypeMismatch() {
        EvaluationResult result = new EvaluationResult("test-flag", "string-value", FlagType.STRING, false, null);
        when(mockHttpClient.evaluateFlag("test-flag", "user-123")).thenReturn(result);

        boolean value = client.getBooleanFlag("test-flag", "user-123", false);

        // Should return default value on type mismatch
        assertFalse(value);
    }

    @Test
    void testGetBooleanFlag_NotFound() {
        when(mockHttpClient.evaluateFlag("test-flag", "user-123"))
                .thenThrow(new FlagNotFoundException("test-flag"));

        boolean value = client.getBooleanFlag("test-flag", "user-123", true);

        // Should return default value
        assertTrue(value);
    }

    @Test
    void testGetBooleanFlag_AuthenticationError() {
        when(mockHttpClient.evaluateFlag("test-flag", "user-123"))
                .thenThrow(new AuthenticationException("Invalid API key"));

        // Should bubble up authentication exception
        assertThrows(AuthenticationException.class, () -> {
            client.getBooleanFlag("test-flag", "user-123", false);
        });
    }

    @Test
    void testGetStringFlag_Success() {
        EvaluationResult result = new EvaluationResult("color-flag", "blue", FlagType.STRING, false, null);
        when(mockHttpClient.evaluateFlag("color-flag", "user-123")).thenReturn(result);

        String value = client.getStringFlag("color-flag", "user-123", "red");

        assertEquals("blue", value);
        verify(mockHttpClient).evaluateFlag("color-flag", "user-123");
    }

    @Test
    void testGetIntFlag_Success() {
        EvaluationResult result = new EvaluationResult("limit-flag", 100, FlagType.NUMBER, false, null);
        when(mockHttpClient.evaluateFlag("limit-flag", "user-123")).thenReturn(result);

        int value = client.getIntFlag("limit-flag", "user-123", 50);

        assertEquals(100, value);
    }

    @Test
    void testGetDoubleFlag_Success() {
        EvaluationResult result = new EvaluationResult("rate-flag", 0.15, FlagType.NUMBER, false, null);
        when(mockHttpClient.evaluateFlag("rate-flag", "user-123")).thenReturn(result);

        double value = client.getDoubleFlag("rate-flag", "user-123", 0.1);

        assertEquals(0.15, value, 0.001);
    }

    @Test
    void testGetAllFlags_Success() {
        Map<String, EvaluationResult> apiResults = new HashMap<>();
        apiResults.put("flag1", new EvaluationResult("flag1", true, FlagType.BOOLEAN, false, null));
        apiResults.put("flag2", new EvaluationResult("flag2", "value", FlagType.STRING, false, null));
        apiResults.put("flag3", new EvaluationResult("flag3", 42, FlagType.NUMBER, false, null));

        when(mockHttpClient.evaluateAllFlags("user-123")).thenReturn(apiResults);

        Map<String, Object> flags = client.getAllFlags("user-123");

        assertEquals(3, flags.size());
        assertEquals(true, flags.get("flag1"));
        assertEquals("value", flags.get("flag2"));
        assertEquals(42, flags.get("flag3"));
    }

    @Test
    void testCaching_SameRequestUsesCache() {
        EvaluationResult result = new EvaluationResult("test-flag", true, FlagType.BOOLEAN, false, null);
        when(mockHttpClient.evaluateFlag("test-flag", "user-123")).thenReturn(result);

        // First call - should hit API
        boolean value1 = client.getBooleanFlag("test-flag", "user-123", false);
        assertTrue(value1);

        // Second call - should use cache
        boolean value2 = client.getBooleanFlag("test-flag", "user-123", false);
        assertTrue(value2);

        // Verify API was called only once
        verify(mockHttpClient, times(1)).evaluateFlag("test-flag", "user-123");
    }

    @Test
    void testCaching_DifferentUsersDontShareCache() {
        EvaluationResult result1 = new EvaluationResult("test-flag", true, FlagType.BOOLEAN, false, null);
        EvaluationResult result2 = new EvaluationResult("test-flag", false, FlagType.BOOLEAN, false, null);
        
        when(mockHttpClient.evaluateFlag("test-flag", "user-1")).thenReturn(result1);
        when(mockHttpClient.evaluateFlag("test-flag", "user-2")).thenReturn(result2);

        boolean value1 = client.getBooleanFlag("test-flag", "user-1", false);
        boolean value2 = client.getBooleanFlag("test-flag", "user-2", false);

        assertTrue(value1);
        assertFalse(value2);

        // Both users should hit the API
        verify(mockHttpClient).evaluateFlag("test-flag", "user-1");
        verify(mockHttpClient).evaluateFlag("test-flag", "user-2");
    }

    @Test
    void testInvalidateCache() {
        EvaluationResult result1 = new EvaluationResult("test-flag", true, FlagType.BOOLEAN, false, null);
        EvaluationResult result2 = new EvaluationResult("test-flag", false, FlagType.BOOLEAN, false, null);
        
        when(mockHttpClient.evaluateFlag("test-flag", "user-123"))
                .thenReturn(result1)
                .thenReturn(result2);

        // First call
        boolean value1 = client.getBooleanFlag("test-flag", "user-123", false);
        assertTrue(value1);

        // Invalidate cache
        client.invalidateCache("test-flag", "user-123");

        // Next call should hit API again
        boolean value2 = client.getBooleanFlag("test-flag", "user-123", false);
        assertFalse(value2);

        // Verify API was called twice
        verify(mockHttpClient, times(2)).evaluateFlag("test-flag", "user-123");
    }

    @Test
    void testClearCache() {
        EvaluationResult result = new EvaluationResult("test-flag", true, FlagType.BOOLEAN, false, null);
        when(mockHttpClient.evaluateFlag(anyString(), anyString())).thenReturn(result);

        // Make some cached calls
        client.getBooleanFlag("flag1", "user-1", false);
        client.getBooleanFlag("flag2", "user-2", false);

        // Clear cache
        client.clearCache();

        // Next calls should hit API again
        client.getBooleanFlag("flag1", "user-1", false);
        client.getBooleanFlag("flag2", "user-2", false);

        // Verify each flag was called twice (once before clear, once after)
        verify(mockHttpClient, times(2)).evaluateFlag("flag1", "user-1");
        verify(mockHttpClient, times(2)).evaluateFlag("flag2", "user-2");
    }

    @Test
    void testBuilder_RequiresApiKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            FeatureFlagClient.builder().build();
        });
    }

    @Test
    void testBuilder_ValidatesApiKeyFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            FeatureFlagClient.builder()
                    .apiKey("invalid_key")
                    .build();
        });
    }

    @Test
    void testBuilder_WithCustomConfiguration() {
        FeatureFlagClient customClient = FeatureFlagClient.builder()
                .apiKey("ff_test_key")
                .baseUrl("http://custom-url:8081")
                .cacheTTL(60, TimeUnit.SECONDS)
                .httpTimeout(10, 20, TimeUnit.SECONDS)
                .build();

        assertNotNull(customClient);
        customClient.close();
    }
}
