package com.smartdispatch.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:${REDIS_HOST:localhost}}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private String redisPort;
    
    @Value("${spring.redis.password:${REDIS_PASSWORD:}}")
    private String redisPassword;
    
    @Value("${spring.redis.ssl:${REDIS_SSL:false}}")
    private boolean ssl;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String prefix = ssl ? "rediss://" : "redis://";
        
        config.useSingleServer()
              .setAddress(prefix + redisHost + ":" + redisPort);
              
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
}
