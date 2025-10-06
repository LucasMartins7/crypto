package com.cryptotrader.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    // General API rate limiting: 60 requests per minute
    private static final int API_REQUESTS_PER_MINUTE = 60;
    
    // Trading API rate limiting: 10 requests per minute
    private static final int TRADING_REQUESTS_PER_MINUTE = 10;
    
    // Login attempts rate limiting: 5 attempts per 15 minutes
    private static final int LOGIN_ATTEMPTS_PER_15_MINUTES = 5;
    
    public Bucket createNewBucket(String key, int capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillPeriod));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
    
    public boolean tryConsumeApiRequest(String userId) {
        String key = "api_" + userId;
        Bucket bucket = cache.computeIfAbsent(key, k -> 
            createNewBucket(k, API_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        
        return bucket.tryConsume(1);
    }
    
    public boolean tryConsumeTradingRequest(String userId) {
        String key = "trading_" + userId;
        Bucket bucket = cache.computeIfAbsent(key, k -> 
            createNewBucket(k, TRADING_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        
        return bucket.tryConsume(1);
    }
    
    public boolean tryConsumeLoginAttempt(String identifier) {
        String key = "login_" + identifier;
        Bucket bucket = cache.computeIfAbsent(key, k -> 
            createNewBucket(k, LOGIN_ATTEMPTS_PER_15_MINUTES, Duration.ofMinutes(15)));
        
        return bucket.tryConsume(1);
    }
    
    public boolean tryConsumeApiKeyOperation(String userId) {
        String key = "apikey_" + userId;
        Bucket bucket = cache.computeIfAbsent(key, k -> 
            createNewBucket(k, 5, Duration.ofMinutes(5))); // 5 operations per 5 minutes
        
        return bucket.tryConsume(1);
    }
    
    public long getAvailableTokens(String key) {
        Bucket bucket = cache.get(key);
        return bucket != null ? bucket.getAvailableTokens() : 0;
    }
    
    public void resetBucket(String key) {
        cache.remove(key);
    }
    
    public void clearAllBuckets() {
        cache.clear();
    }
    
    // Custom rate limiting for specific operations
    public boolean tryConsumeCustom(String key, int capacity, Duration refillPeriod, int tokensToConsume) {
        Bucket bucket = cache.computeIfAbsent(key, k -> 
            createNewBucket(k, capacity, refillPeriod));
        
        return bucket.tryConsume(tokensToConsume);
    }
}
