package com.averygrimes.secretschest.cache;

import com.averygrimes.axis.cache.AbstractCache;
import com.averygrimes.axis.cache.CacheObject;
import org.apache.commons.collections4.MapUtils;

import javax.inject.Named;
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
        KeyCacheHolder.addKeyCache(this);
    }

    @Override
    public Object get(String key) {
        return super.getItemFromCache(key);
    }

    @Override
    public void put(String key, Object value) {
        super.putItemInCache(key, value, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void extend(String keyId) {
        if(cacheMap.containsKey(keyId)){
            CacheObject cacheObject = cacheMap.get(keyId);
            if((refreshMillis > 0) && System.currentTimeMillis() - cacheObject.getCreatedTimestamp() > refreshMillis){

            }
            super.updateItemInCache(keyId, cacheObject, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void clear(String keyId) {
        Map<String, CacheObject> keyMap = cacheMap;
        if(MapUtils.isNotEmpty(keyMap)){
            super.clearCache();
        }
    }
}
