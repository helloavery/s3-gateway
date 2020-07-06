package com.averygrimes.credentials;

import com.averygrimes.credentials.pojo.CredentialsResponse;

import java.util.List;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

public class SecretsChestUtils {

    private final SecretsChestService secretsChestService;

    public SecretsChestUtils() {
        this.secretsChestService = new SecretsChestServiceImpl();
    }

    public CredentialsResponse getApplicationSecret(String secretReference){
        return secretsChestService.retrieveSecrets(secretReference);
    }

    public CredentialsResponse getMultipleApplicationSecrets(List<String> secretReferences){
        return secretsChestService.retrieveMultipleSecrets(secretReferences);
    }
    public CredentialsResponse updateApplicationSecrets(String secretReference, byte[] dataToUpload){
        return secretsChestService.updateSecrets(dataToUpload, secretReference);
    }

    public CredentialsResponse uploadApplicationSecrets(byte[] dataToUpload){
        return secretsChestService.sendSecrets(dataToUpload);
    }

    public CredentialsResponse uploadMultipleApplicationSecrets(Map<String, byte[]> listOfDataToUpload) {
        return secretsChestService.sendMultipleSecrets(listOfDataToUpload);
    }
}
