package io.github._07manan.featureflags.sdk.cache;

class CacheEntry<T> {
    private final T value;
    private final long timestamp;

    public CacheEntry(T value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    public T getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this cache entry has expired.
     *
     * @param ttlMillis the time-to-live in milliseconds
     * @return true if expired, false otherwise
     */
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
}
