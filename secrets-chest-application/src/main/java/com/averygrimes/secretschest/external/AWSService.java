package com.averygrimes.secretschest.external;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

public interface AWSService {

    void sendUploadBucketObjectRequest(String bucket, String bucketObjectReference, String dataToUpload, String requestId);

    Object sendRetrieveBucketObjectResponse(String bucket, String secretReference, String requestId, boolean returnAsString);
}
