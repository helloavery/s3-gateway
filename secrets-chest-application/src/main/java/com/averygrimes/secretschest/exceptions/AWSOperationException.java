package com.averygrimes.secretschest.exceptions;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

public class AWSOperationException extends RuntimeException {

    public AWSOperationException(String message){
        super(message);
    }

    public AWSOperationException(String message, Throwable e){
        super(message, e);
    }
}
