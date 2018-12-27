package com.averygrimes.s3gateway.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-18
 * https://github.com/helloavery
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class S3GatewayDTO {

    private String bucket;
    private String bucketObject;
    private long ehcacheVariable;
    private byte[] cipherText;
    private byte[] publicKey;
    private byte[] signature;

    public S3GatewayDTO(){
        super();
    }

    public S3GatewayDTO(String bucket, String bucketObject, long ehcacheVariable, byte[] cipherText, byte[] publicKey, byte[] signature){
        this.bucket = bucket;
        this.bucketObject = bucketObject;
        this.ehcacheVariable = ehcacheVariable;
        this.cipherText = cipherText;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public S3GatewayDTO(byte[] cipherText, byte[] publicKey, byte[] signature){
        this.cipherText = cipherText;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public S3GatewayDTO(String bucket, String bucketObject, long ehcacheVariable, byte[] cipherText){
        this.bucket = bucket;
        this.bucketObject = bucketObject;
        this.ehcacheVariable = ehcacheVariable;
        this.cipherText = cipherText;
    }

    public String getBucket() {
        return bucket;
    }


    public String getBucketObject() {
        return bucketObject;
    }

    public long getEhcacheVariable() {
        return ehcacheVariable;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

}