package com.averygrimes.secretschest.service;


import com.averygrimes.secretschest.pojo.S3OperationMethod;
import com.averygrimes.secretschest.pojo.SecretsChestResponse;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

public interface SecretsChestBaseService {

    SecretsChestResponse uploadAsset(byte[] dataToUpload, S3OperationMethod method, String requestId);

    SecretsChestResponse uploadPlainTextAsset(String dataToUpload, String requestId);

    SecretsChestResponse updateAsset(String secretsReference, byte[] dataToUpload, String requestId);

    SecretsChestResponse retrieveAsset(String secretReference, String requestId);
}
