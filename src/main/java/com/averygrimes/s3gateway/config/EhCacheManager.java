package com.averygrimes.s3gateway.config;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.time.Duration;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-17
 * https://github.com/helloavery
 */

@Component
public class EhCacheManager{

    private Cache<String, SecretKey> secretKeyCache;
    private Cache<String, KeyPair> keyPairCache;

    public EhCacheManager(){
        this.cacheConfig();
    }

    private void cacheConfig (){
        CacheManager secretKeyCacheManager = newCacheManagerBuilder()
                .withCache("secretKeyCache", newCacheConfigurationBuilder(String.class, SecretKey.class, ResourcePoolsBuilder.heap(10))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20))))
                .build(true);
        secretKeyCache = secretKeyCacheManager.getCache("secretKeyCache", String.class, SecretKey.class);
        CacheManager keyPairCacheManager = newCacheManagerBuilder()
                .withCache("keyPairCache", newCacheConfigurationBuilder(String.class, KeyPair.class, ResourcePoolsBuilder.heap(10))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20))))
                .build(true);
        keyPairCache = keyPairCacheManager.getCache("keyPairCache", String.class, KeyPair.class);
    }

    public void tearDown(CacheManager cacheManager){
        cacheManager.removeCache("secretKeyCache");
        cacheManager.close();
    }

    public Cache<String, SecretKey> getSecretKeyCache() {
        return this.secretKeyCache;
    }

    public Cache<String, KeyPair> getKeyPairCache() {
        return this.keyPairCache;
    }
}
