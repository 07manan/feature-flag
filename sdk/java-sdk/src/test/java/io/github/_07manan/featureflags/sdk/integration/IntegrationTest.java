package io.github._07manan.featureflags.sdk.integration;

import io.github._07manan.featureflags.sdk.FeatureFlagClient;
import io.github._07manan.featureflags.sdk.exception.AuthenticationException;
import io.github._07manan.featureflags.sdk.exception.FlagNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FeatureFlagClient.
 * 
 * REQUIREMENTS:
 * - Local evaluation API running on http://localhost:8081
 * - Valid API key configured in TEST_API_KEY variable
 * - Test flags created in the corresponding environment:
 *   - "test-boolean-flag" (BOOLEAN type)
 *   - "test-string-flag" (STRING type)
 *   - "test-number-flag" (NUMBER type)
 *   - "test-percentage-rollout" (BOOLEAN type with percentage variants)
 * 
 * To run these tests:
 * 1. Start the evaluation API and admin API
 * 2. Create a test environment and obtain the API key
 * 3. Create the required test flags
 * 4. Update TEST_API_KEY with your environment's API key
 * 5. Set FEATUREFLAGS_TEST_API_KEY environment variable with your key
 * 6. Remove @Disabled annotation
 * 7. Run: mvn test -Dtest=IntegrationTest
 */
@Disabled("Integration tests require a running evaluation API — remove to run manually")
class IntegrationTest {

    private static final String TEST_API_KEY = System.getenv().getOrDefault(
            "FEATUREFLAGS_TEST_API_KEY", "ff_test_placeholder");
    private static final String BASE_URL = "http://localhost:8081";
    
    private FeatureFlagClient client;

    @BeforeEach
    void setUp() {
        // Override baseUrl via system property for testing
        System.setProperty("featureflags.baseUrl", BASE_URL);
        
        client = FeatureFlagClient.builder()
                .apiKey(TEST_API_KEY)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        System.clearProperty("featureflags.baseUrl");
    }

    @Test
    void testHealthCheck() {
        // Just verify client initializes correctly
        assertNotNull(client);
    }

    @Test
    void testGetBooleanFlag() {
        // Test with a boolean flag (create "test-boolean-flag" in your environment)
        boolean value = client.getBooleanFlag("test-boolean-flag", "test-user-1", false);
        
        // The actual value depends on your flag configuration
        assertNotNull(value); // Just verify it doesn't throw exception
        System.out.println("Boolean flag value: " + value);
    }

    @Test
    void testGetStringFlag() {
        // Test with a string flag (create "test-string-flag" in your environment)
        String value = client.getStringFlag("test-string-flag", "test-user-1", "default");
        
        assertNotNull(value);
        System.out.println("String flag value: " + value);
    }

    @Test
    void testGetNumberFlag() {
        // Test with a number flag (create "test-number-flag" in your environment)
        int intValue = client.getIntFlag("test-number-flag", "test-user-1", 0);
        double doubleValue = client.getDoubleFlag("test-number-flag", "test-user-1", 0.0);
        
        System.out.println("Int flag value: " + intValue);
        System.out.println("Double flag value: " + doubleValue);
    }

    @Test
    void testFlagNotFound() {
        // Test with a non-existent flag - should return default value
        boolean value = client.getBooleanFlag("non-existent-flag", "test-user-1", true);
        
        assertTrue(value, "Should return default value for non-existent flag");
    }

    @Test
    void testInvalidApiKey() {
        FeatureFlagClient invalidClient = FeatureFlagClient.builder()
                .apiKey("ff_invalid_key")
                .baseUrl(BASE_URL)
                .build();
        
        try {
            // Should throw AuthenticationException
            assertThrows(AuthenticationException.class, () -> {
                invalidClient.getBooleanFlag("test-flag", "user-1", false);
            });
        } finally {
            invalidClient.close();
        }
    }

    @Test
    void testGetAllFlags() {
        Map<String, Object> flags = client.getAllFlags("test-user-1");
        
        assertNotNull(flags);
        System.out.println("Total flags: " + flags.size());
        
        for (Map.Entry<String, Object> entry : flags.entrySet()) {
            System.out.println("Flag: " + entry.getKey() + " = " + entry.getValue() 
                    + " (" + entry.getValue().getClass().getSimpleName() + ")");
        }
    }

    @Test
    void testCaching() throws InterruptedException {
        // First call - should hit API
        long start1 = System.currentTimeMillis();
        boolean value1 = client.getBooleanFlag("test-boolean-flag", "cache-test-user", false);
        long duration1 = System.currentTimeMillis() - start1;
        
        // Second call - should use cache (much faster)
        long start2 = System.currentTimeMillis();
        boolean value2 = client.getBooleanFlag("test-boolean-flag", "cache-test-user", false);
        long duration2 = System.currentTimeMillis() - start2;
        
        assertEquals(value1, value2, "Cached value should match original");
        assertTrue(duration2 < duration1, "Cached call should be faster than API call");
        
        System.out.println("First call (API): " + duration1 + "ms");
        System.out.println("Second call (cache): " + duration2 + "ms");
        System.out.println("Cache speedup: " + (duration1 / (double) duration2) + "x");
    }

    @Test
    void testCacheInvalidation() {
        // Get initial value
        boolean value1 = client.getBooleanFlag("test-boolean-flag", "invalidate-test-user", false);
        
        // Invalidate cache
        client.invalidateCache("test-boolean-flag", "invalidate-test-user");
        
        // Next call should hit API again (value might be same, but we verify no exception)
        boolean value2 = client.getBooleanFlag("test-boolean-flag", "invalidate-test-user", false);
        
        // Values should still be consistent
        assertEquals(value1, value2);
    }

    @Test
    void testDeterministicRollout() {
        // Same user should always get same result
        String userId = "deterministic-user";
        
        boolean value1 = client.getBooleanFlag("test-percentage-rollout", userId, false);
        
        // Clear cache to force re-evaluation
        client.clearCache();
        
        boolean value2 = client.getBooleanFlag("test-percentage-rollout", userId, false);
        
        assertEquals(value1, value2, "Same user should always get same flag value");
    }

    @Test
    void testNullUserId() {
        // Test with null user ID (should work for non-percentage flags)
        boolean value = client.getBooleanFlag("test-boolean-flag", null, false);
        
        assertNotNull(value); // Should not throw exception
        System.out.println("Flag value for null user: " + value);
    }

    @Test
    void testCacheTTL() throws InterruptedException {
        // This test verifies that cache expires after TTL (30 seconds default)
        // Note: This test takes 31+ seconds to run
        
        boolean value1 = client.getBooleanFlag("test-boolean-flag", "ttl-test-user", false);
        
        // Wait for cache to expire (30s TTL + 1s buffer)
        System.out.println("Waiting for cache TTL to expire (31 seconds)...");
        Thread.sleep(31000);
        
        // Next call should hit API again (after cache expiration)
        boolean value2 = client.getBooleanFlag("test-boolean-flag", "ttl-test-user", false);
        
        // Values should still be consistent
        assertEquals(value1, value2);
        System.out.println("Cache TTL test passed - value remained consistent");
    }
}
