package com.smartdispatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration.
 *
 * HOW SPRING CACHING WORKS:
 *
 * @EnableCaching activates Spring's annotation-driven caching.
 * When you annotate a method with @Cacheable("drivers"), Spring
 * intercepts the call and checks Redis first:
 *   - CACHE HIT  → Returns the cached value instantly (no DB query)
 *   - CACHE MISS → Executes the method, stores the result in Redis, returns it
 *
 * @CacheEvict removes entries from the cache when data changes,
 * ensuring stale data is never served.
 *
 * Each cache name can have its own TTL (Time-To-Live):
 *   - "drivers"       → 10 min (driver profiles don't change often)
 *   - "service-zones" → 30 min (geofence zones are almost static)
 *   - "order-stats"   → 60 seconds (dashboard stats refresh frequently)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        // Default cache config: 10 minute TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Driver cache: 10 minutes (driver profile data)
        cacheConfigurations.put("drivers", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Geofence zones: 30 minutes (zones rarely change)
        cacheConfigurations.put("service-zones", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Order stats: 60 seconds (admin dashboard needs fresh-ish data)
        cacheConfigurations.put("order-stats", defaultConfig.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
