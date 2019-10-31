package com.averygrimes.secretschest.cache;

import com.averygrimes.axis.cache.AbstractCache;
import com.averygrimes.axis.cache.CacheObject;
import org.apache.commons.collections4.MapUtils;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/29/19
 * https://github.com/helloavery
 */

@Named
public class KeyCacheImpl extends AbstractCache implements KeyCache {

    public KeyCacheImpl() {
        super(SESSION_CACHE_NAME);
        KeyCacheHolder.addSessionCache(this);
    }

    @Override
    public Map<String, Object> get(String key) {
        Map<String, Object> sessionMap = (Map<String, Object>) super.getItemFromCache(key);
        return sessionMap != null ? sessionMap : new HashMap<>();
    }

    @Override
    public void put(String key, Object value) {
        super.putItemInCache(key, value, SESSION_TTL_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void extend(String keyId) {
        if(cacheMap.containsKey(keyId)){
            CacheObject cacheObject = cacheMap.get(keyId);
            if((refreshMillis > 0) && System.currentTimeMillis() - cacheObject.getCreatedTimestamp() > refreshMillis){

            }
            super.updateItemInCache(keyId, cacheObject, SESSION_TTL_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void clear(String keyId) {
        Map<String, Object> keyMap = this.get(keyId);
        if(MapUtils.isNotEmpty(keyMap)){
            keyMap.clear();
        }
    }
}
