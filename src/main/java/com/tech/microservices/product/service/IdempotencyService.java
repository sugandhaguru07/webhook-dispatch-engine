package com.tech.microservices.product.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
	private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_PREFIX = "webhook:idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

   
    public boolean lockEvent(String eventId) {
        String key = IDEMPOTENCY_PREFIX + eventId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", TTL);
        return Boolean.TRUE.equals(isNew);
    }

    public void markCompleted(String eventId) {
        String key = IDEMPOTENCY_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "COMPLETED", TTL);
    }
}
