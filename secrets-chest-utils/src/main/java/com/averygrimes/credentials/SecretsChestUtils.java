package com.averygrimes.credentials;

import com.averygrimes.credentials.pojo.CredentialsResponse;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

@Named
public class SecretsChestUtils {

    private SecretsChestService secretsChestService;

    @Inject
    public void setSecretsChestService(SecretsChestService secretsChestService) {
        this.secretsChestService = secretsChestService;
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
