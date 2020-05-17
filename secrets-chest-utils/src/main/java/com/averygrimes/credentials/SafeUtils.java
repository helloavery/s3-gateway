package com.averygrimes.credentials;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/16/20
 * https://github.com/helloavery
 */

public final class SafeUtils {

    public static <T> List<T> safe(List<T> list){
        return list == null ? new ArrayList<>() : list;
    }

    public static <T> Set<T> safe(Set<T> set){
        return set == null ? new HashSet<>() : set;
    }

    public static String safe(String input){
        return input == null ? "" : input;
    }

    public static <T> T safe(T object, Class<T> instance){
        try{
            return object == null ? instance.newInstance() : object;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }


}
