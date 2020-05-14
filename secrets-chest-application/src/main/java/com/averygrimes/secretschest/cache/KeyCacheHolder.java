package com.averygrimes.secretschest.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/29/19
 * https://github.com/helloavery
 */

public class KeyCacheHolder {

    private static final List<KeyCache> keyCaches = new ArrayList<>();

    private KeyCacheHolder() {
    }

    public static void addKeyCache(KeyCache keyCache){
        keyCaches.add(keyCache);
    }

    public static List<KeyCache> getAllKeyCaches(){
        return Collections.unmodifiableList(keyCaches);
    }
}
