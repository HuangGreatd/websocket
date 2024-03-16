package com.juzipi.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    @Value("${spring.redis.password}")
    private String password;


    /**
     * redisson客户
     *
     * @return {@link RedissonClient}
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer().setAddress(address).setPassword(password);
        return Redisson.create(config);
    }
}
