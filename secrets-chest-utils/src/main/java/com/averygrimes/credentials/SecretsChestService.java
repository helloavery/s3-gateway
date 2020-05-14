package com.averygrimes.credentials;

import com.averygrimes.credentials.pojo.CredentialsResponse;

import java.util.List;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

public interface SecretsChestService {

    CredentialsResponse sendSecrets(byte[] dataToUpload);

    CredentialsResponse sendMultipleSecrets(Map<String, byte[]> listOfDataToUpload);

    CredentialsResponse updateSecrets(byte[] dataToUpload, String keyReference);

    CredentialsResponse retrieveSecrets(String keyReference);

    CredentialsResponse retrieveMultipleSecrets(List<String> keyReferences);
}
