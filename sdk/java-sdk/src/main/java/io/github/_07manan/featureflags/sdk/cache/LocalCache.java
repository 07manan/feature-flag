package io.github._07manan.featureflags.sdk.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalCache<T> {
    private static final Logger logger = LoggerFactory.getLogger(LocalCache.class);
    
    private final ConcurrentHashMap<String, CacheEntry<T>> cache;
    private final long ttlMillis;
    private final ScheduledExecutorService cleanupScheduler;

    /**
     * Creates a new cache with the specified TTL.
     *
     * @param ttl the time-to-live value
     * @param unit the time unit for the TTL
     */
    public LocalCache(long ttl, TimeUnit unit) {
        this.cache = new ConcurrentHashMap<>();
        this.ttlMillis = unit.toMillis(ttl);
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cache-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        // Schedule cleanup task to run periodically (every 30 seconds)
        this.cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                30,
                30,
                TimeUnit.SECONDS
        );
        
        logger.debug("LocalCache initialized with TTL: {}ms", ttlMillis);
    }

    /**
     * Retrieves a value from the cache if it exists and has not expired.
     *
     * @param key the cache key
     * @return the cached value, or null if not found or expired
     */
    public T get(String key) {
        CacheEntry<T> entry = cache.get(key);
        
        if (entry == null) {
            logger.trace("Cache miss for key: {}", key);
            return null;
        }
        
        if (entry.isExpired(ttlMillis)) {
            logger.trace("Cache entry expired for key: {}", key);
            cache.remove(key);
            return null;
        }
        
        logger.trace("Cache hit for key: {}", key);
        return entry.getValue();
    }

    public void put(String key, T value) {
        cache.put(key, new CacheEntry<>(value));
        logger.trace("Cached value for key: {}", key);
    }

    public void invalidate(String key) {
        cache.remove(key);
        logger.trace("Invalidated cache entry for key: {}", key);
    }

    public void clear() {
        cache.clear();
        logger.debug("Cache cleared");
    }

    public int size() {
        return cache.size();
    }

    private void cleanupExpiredEntries() {
        int removed = 0;
        for (var iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
            var mapEntry = iterator.next();
            if (mapEntry.getValue().isExpired(ttlMillis)) {
                // Use computeIfPresent for atomic check-and-remove to avoid
                // racing with a concurrent put() that inserted a fresh entry.
                boolean[] wasRemoved = {false};
                cache.computeIfPresent(mapEntry.getKey(), (k, v) -> {
                    if (v.isExpired(ttlMillis)) {
                        wasRemoved[0] = true;
                        return null; // removes the entry
                    }
                    return v; // keep — a fresh entry was inserted concurrently
                });
                if (wasRemoved[0]) {
                    removed++;
                }
            }
        }
        
        if (removed > 0) {
            logger.debug("Cleaned up {} expired cache entries", removed);
        }
    }

    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        cache.clear();
        logger.debug("LocalCache shut down");
    }
}
