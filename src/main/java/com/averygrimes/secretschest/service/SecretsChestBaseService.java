package com.averygrimes.secretschest.service;


import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

public interface SecretsChestBaseService {

    Response uploadAsset(byte[] dataToUpload, String requestId);

    Response updateAsset(String secretsReference, byte[] dataToUpload, String requestId);

    Response retrieveAsset(String secretReference, String requestId);
}
