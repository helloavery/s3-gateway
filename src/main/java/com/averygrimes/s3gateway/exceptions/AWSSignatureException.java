package com.averygrimes.s3gateway.exceptions;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-24
 * https://github.com/helloavery
 */

public class AWSSignatureException extends RuntimeException {

    public  AWSSignatureException(String message){
        super(message);
    }

    public AWSSignatureException(String message, Throwable e){
        super(message, e);
    }
}
