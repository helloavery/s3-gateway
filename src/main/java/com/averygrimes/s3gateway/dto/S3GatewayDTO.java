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
    private String symmetricKeyUUID;
    private byte[] symmetricKey;
    private byte[] cipherText;
    private String keyPairUUID;
    private byte[] publicKey;
    private byte[] signature;

    public S3GatewayDTO(){
        super();
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getBucketObject() {
        return bucketObject;
    }

    public void setBucketObject(String bucketObject) {
        this.bucketObject = bucketObject;
    }

    public String getSymmetricKeyUUID() {
        return symmetricKeyUUID;
    }

    public void setSymmetricKeyUUID(String symmetricKeyUUID) {
        this.symmetricKeyUUID = symmetricKeyUUID;
    }

    public byte[] getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(byte[] symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public void setCipherText(byte[] cipherText) {
        this.cipherText = cipherText;
    }

    public String getKeyPairUUID() {
        return keyPairUUID;
    }

    public void setKeyPairUUID(String keyPairUUID) {
        this.keyPairUUID = keyPairUUID;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

}