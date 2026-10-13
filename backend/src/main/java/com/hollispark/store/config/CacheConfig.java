package com.hollispark.store.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory (Caffeine) caches; sizes and expiry are in application.yml under
 * spring.cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
