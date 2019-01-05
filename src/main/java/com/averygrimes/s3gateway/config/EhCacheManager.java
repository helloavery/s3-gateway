package com.averygrimes.s3gateway.config;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.security.KeyPair;
import java.time.Duration;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-17
 * https://github.com/helloavery
 */

public abstract class EhCacheManager {

    protected Cache<Long,KeyPair> preConfigured;

    protected void cacheConfig(){
        try(CacheManager cacheManager = newCacheManagerBuilder()
                .withCache("preConfiguredCache", newCacheConfigurationBuilder(Long.class, KeyPair.class, ResourcePoolsBuilder.heap(10))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20))))
                .build(true)){

            preConfigured = cacheManager.getCache("preConfiguredCache", Long.class, KeyPair.class);

            cacheManager.removeCache("preConfiguredCache");
        }
    }

    public void tearDown(){

    }
}
