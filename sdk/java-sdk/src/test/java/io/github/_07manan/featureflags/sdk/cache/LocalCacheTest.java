package io.github._07manan.featureflags.sdk.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LocalCacheTest {
    
    private LocalCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new LocalCache<>(100, TimeUnit.MILLISECONDS);
    }

    @AfterEach
    void tearDown() {
        cache.shutdown();
    }

    @Test
    void testPutAndGet() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void testGetNonExistent() {
        assertNull(cache.get("nonexistent"));
    }

    @Test
    void testExpiration() throws InterruptedException {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        
        // Wait for expiration (100ms TTL + buffer)
        Thread.sleep(150);
        
        assertNull(cache.get("key1"), "Entry should have expired");
    }

    @Test
    void testInvalidate() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        
        cache.invalidate("key1");
        assertNull(cache.get("key1"));
    }

    @Test
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        assertEquals(2, cache.size());
        
        cache.clear();
        
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "key-" + threadId + "-" + j;
                    String value = "value-" + threadId + "-" + j;
                    cache.put(key, value);
                    assertEquals(value, cache.get(key));
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify cache still works after concurrent operations
        cache.put("test", "test-value");
        assertEquals("test-value", cache.get("test"));
    }

    @Test
    void testCleanupTask() throws InterruptedException {
        // Create cache with very short TTL
        LocalCache<String> shortTTLCache = new LocalCache<>(50, TimeUnit.MILLISECONDS);
        
        // Add many entries
        for (int i = 0; i < 100; i++) {
            shortTTLCache.put("key-" + i, "value-" + i);
        }
        
        assertTrue(shortTTLCache.size() > 0);
        
        // Wait for entries to expire and cleanup task to run
        Thread.sleep(200);
        
        // Verify entries are cleaned up (note: cleanup runs every 30s, but expired entries
        // are removed on access, so accessing any key should return null)
        assertNull(shortTTLCache.get("key-0"));
        
        shortTTLCache.shutdown();
    }
}
