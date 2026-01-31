package edu.ntu.ds.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * At-Most-Once (AMO) Reply Cache for duplicate request suppression.
 * 
 * Key: (clientId, requestId) - uniquely identifies a request
 * Value: cached reply bytes
 * 
 * When a duplicate request arrives (same clientId + requestId), the server
 * returns the cached reply instead of re-executing the operation. This ensures
 * non-idempotent operations are executed at most once.
 */
public class AmoCache {
    
    /**
     * Cache key combining clientId and requestId
     */
    private static class CacheKey {
        final int clientId;
        final long requestId;
        
        CacheKey(int clientId, long requestId) {
            this.clientId = clientId;
            this.requestId = requestId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return clientId == cacheKey.clientId && requestId == cacheKey.requestId;
        }
        
        @Override
        public int hashCode() {
            return 31 * clientId + Long.hashCode(requestId);
        }
        
        @Override
        public String toString() {
            return "(" + clientId + ", " + requestId + ")";
        }
    }
    
    /**
     * Cache entry with reply bytes and timestamp
     */
    private static class CacheEntry {
        final byte[] replyBytes;
        final long timestamp;
        
        CacheEntry(byte[] replyBytes) {
            this.replyBytes = replyBytes;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private final Map<CacheKey, CacheEntry> cache;
    private final long maxAgeMs;  // Maximum age of cache entries
    
    // Statistics
    private long hits;
    private long misses;
    
    /**
     * Create AMO cache with default max age (5 minutes)
     */
    public AmoCache() {
        this(5 * 60 * 1000); // 5 minutes
    }
    
    /**
     * Create AMO cache with specified max age
     * @param maxAgeMs maximum age of cache entries in milliseconds
     */
    public AmoCache(long maxAgeMs) {
        this.cache = new ConcurrentHashMap<>();
        this.maxAgeMs = maxAgeMs;
        this.hits = 0;
        this.misses = 0;
    }
    
    /**
     * Check if a reply exists in cache for the given request
     * @param clientId client identifier
     * @param requestId request identifier
     * @return cached reply bytes, or null if not found
     */
    public byte[] get(int clientId, long requestId) {
        CacheKey key = new CacheKey(clientId, requestId);
        CacheEntry entry = cache.get(key);
        
        if (entry != null) {
            // Check if entry is still valid
            if (System.currentTimeMillis() - entry.timestamp < maxAgeMs) {
                hits++;
                return entry.replyBytes;
            } else {
                // Entry expired, remove it
                cache.remove(key);
            }
        }
        
        misses++;
        return null;
    }
    
    /**
     * Store a reply in cache
     * @param clientId client identifier
     * @param requestId request identifier
     * @param replyBytes the reply message bytes to cache
     */
    public void put(int clientId, long requestId, byte[] replyBytes) {
        CacheKey key = new CacheKey(clientId, requestId);
        cache.put(key, new CacheEntry(replyBytes));
    }
    
    /**
     * Check if a request exists in cache (without updating statistics)
     */
    public boolean contains(int clientId, long requestId) {
        CacheKey key = new CacheKey(clientId, requestId);
        CacheEntry entry = cache.get(key);
        return entry != null && (System.currentTimeMillis() - entry.timestamp < maxAgeMs);
    }
    
    /**
     * Remove expired entries from cache
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> now - e.getValue().timestamp >= maxAgeMs);
    }
    
    /**
     * Clear all cache entries
     */
    public void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Get cache hit count
     */
    public long getHits() {
        return hits;
    }
    
    /**
     * Get cache miss count
     */
    public long getMisses() {
        return misses;
    }
    
    /**
     * Get cache hit ratio
     */
    public double getHitRatio() {
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("AmoCache{size=%d, hits=%d, misses=%d, hitRatio=%.2f%%}",
            size(), hits, misses, getHitRatio() * 100);
    }
}
