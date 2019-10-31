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

    private static List<KeyCache> sessionCaches = new ArrayList<>();

    private KeyCacheHolder() {
    }

    public static void addSessionCache(KeyCache sessionCache){
        sessionCaches.add(sessionCache);
    }

    public static List<KeyCache> getAllSessionCaches(){
        return Collections.unmodifiableList(sessionCaches);
    }
}
