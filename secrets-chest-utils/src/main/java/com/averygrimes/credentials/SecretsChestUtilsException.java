package com.averygrimes.credentials;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/16/20
 * https://github.com/helloavery
 */

public class SecretsChestUtilsException extends RuntimeException{

    public SecretsChestUtilsException(String message){
        super(message);
    }

    public SecretsChestUtilsException(String message, Throwable e){
        super(message,e);
    }
}
