package com.averygrimes.credentials.pojo;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

public interface CredentialConstants {

    /****Service Operation Errors****/
    String SERVICE_S3_GATEWAY_ERROR = "Error with communicating with S3 Gateway";
    String SERVICE_S3_GATEWAY_BUCKET_RETRIEVAL_ERROR = "Error fetching bucket";
    String SERVICE_S3_GATEWAY_BUCKET_SEND_ERROR = "Error sending bucket";
}
