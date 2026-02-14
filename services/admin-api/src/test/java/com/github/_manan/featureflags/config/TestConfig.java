package com.github._manan.featureflags.config;

import com.github._manan.featureflags.event.CacheInvalidationPublisher;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public CacheInvalidationPublisher cacheInvalidationPublisher() {
        return Mockito.mock(CacheInvalidationPublisher.class);
    }
}
