package com.averygrimes.secretschest.cache;

import com.averygrimes.axis.cache.CacheBase;

import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/29/19
 * https://github.com/helloavery
 */

public interface KeyCache extends CacheBase {

    String SESSION_CACHE_NAME = "keyMap";
    Long SESSION_TTL_MILLIS = 30L * 60L * 1000L;

    Map<String, Object> get(String key);

    void put(String key, Object value);

    void extend(String keyId);

    void clear(String keyId);
}
