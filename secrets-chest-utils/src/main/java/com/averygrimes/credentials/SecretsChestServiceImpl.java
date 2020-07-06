package com.averygrimes.credentials;

import com.averygrimes.credentials.interaction.SecretsChestClient;
import com.averygrimes.credentials.pojo.CredentialsResponse;
import com.averygrimes.credentials.pojo.TaskType;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

public class SecretsChestServiceImpl implements SecretsChestService {

    private static final Logger LOGGER = LogManager.getLogger(SecretsChestServiceImpl.class);
    private final SecretsChestClient secretsChestClient;
    private final CredentialsUtils credentialsUtils;
    private final ExecutorService executorService;

    public SecretsChestServiceImpl() {
        secretsChestClient = new SecretsChestClient();
        credentialsUtils = new CredentialsUtils();
        executorService = Executors.newFixedThreadPool(50);
    }

    public CredentialsResponse sendSecrets(byte[] dataToUpload){
        LOGGER.info("Starting Uploading data to Secrets Chest Job");
        Stopwatch stopwatch = Stopwatch.createStarted();
        ClientResponse secretsChestUploadResponse = secretsChestClient.uploadSecrets(dataToUpload);
        CredentialsResponse uploadDataResponse = validateResponseAndReturnEntity(secretsChestUploadResponse, CredentialsResponse.class);
        stopwatch.stop();
        LOGGER.info("Upload data to Secrets Chest complete, time took is {}", stopwatch.toString());
        return uploadDataResponse;
    }

    public CredentialsResponse sendMultipleSecrets(Map<String, byte[]> listOfDataToUpload){
        LOGGER.info("Starting Uploading multiple data to Secrets Chest Job");
        Stopwatch stopwatch = Stopwatch.createStarted();
        CountDownLatch countDownLatch = new CountDownLatch(listOfDataToUpload.size());
        CredentialsResponse credentialsResponse = new CredentialsResponse();
        Map<String, String> uploadResults = new ConcurrentHashMap<>();
        credentialsResponse.setSecretsReferences(uploadResults);
        listOfDataToUpload.forEach((reference, dataToUpload) -> {
            CompletableFuture<CredentialsResponse> completableFuture = sendTaskToSecretsChest(TaskType.UPLOAD, dataToUpload);
            completableFuture.whenComplete((uploadResponse, exception) -> {
                if (uploadResponse != null) {
                    uploadResults.put(reference, uploadResponse.getSecretReference());
                }
                countDownLatch.countDown();
            });
        });
        try{
            countDownLatch.await();
        }
        catch (InterruptedException e){
            LOGGER.warn("Thread has been interrupted");
        }
        stopwatch.stop();
        LOGGER.info("Upload data to Secrets Chest complete, time took is {}", stopwatch.toString());
        return credentialsResponse;
    }

    public CredentialsResponse updateSecrets(byte[] dataToUpload, String keyReference){
        LOGGER.info("Starting Replacing Secrets Job");
        Stopwatch stopwatch = Stopwatch.createStarted();
        ClientResponse secretsChestReplaceResponse = secretsChestClient.updateSecrets(keyReference, dataToUpload);
        CredentialsResponse uploadDataResponse = validateResponseAndReturnEntity(secretsChestReplaceResponse, CredentialsResponse.class);
        stopwatch.stop();
        LOGGER.info("Upload data to Secrets Chest complete, time took is {}", stopwatch.toString());
        return uploadDataResponse;
    }

    public CredentialsResponse retrieveSecrets(String keyReference){
        LOGGER.info("Starting Retrieving Secrets Job");
        Stopwatch stopwatch = Stopwatch.createStarted();
        ClientResponse secretsChestRetrievalResponse = secretsChestClient.retrieveSecrets(keyReference);
        CredentialsResponse retrieveDataResponse = validateResponseAndReturnEntity(secretsChestRetrievalResponse, CredentialsResponse.class);
        stopwatch.stop();
        LOGGER.info("Retrieval to fetch data from Secrets Chest complete, time took is {}", stopwatch.toString());
        return retrieveDataResponse;
    }

    public CredentialsResponse retrieveMultipleSecrets(List<String> keyReferences){
        LOGGER.info("Starting Retrieving multiple data from Secrets Chest Job");
        Stopwatch stopwatch = Stopwatch.createStarted();
        final CountDownLatch countDownLatch = new CountDownLatch(keyReferences.size());
        CredentialsResponse retrieveDataResponse = new CredentialsResponse();
        Map<String, byte[]> referenceSecretMap = new ConcurrentHashMap<>();
        retrieveDataResponse.setSecretsDataMap(referenceSecretMap);
        keyReferences.forEach(keyReference -> {
            CompletableFuture<CredentialsResponse> completableFuture = sendTaskToSecretsChest(TaskType.RETRIEVAL, keyReference);
            completableFuture.whenComplete((dataResponse, exception) -> {
                if(dataResponse != null){
                    referenceSecretMap.put(keyReference, dataResponse.getData());
                }
                countDownLatch.countDown();
            });
        });

        try{
            countDownLatch.await();
        }
        catch(InterruptedException e){
            LOGGER.warn("Thread has been interrupted");
        }
        stopwatch.stop();
        LOGGER.info("Retrieval to fetch data from Secrets Chest complete, time took is {}", stopwatch.toString());
        return retrieveDataResponse;
    }

    private <T> CompletableFuture<CredentialsResponse> sendTaskToSecretsChest(TaskType taskType, T dataToAction){
        CompletableFuture<CredentialsResponse> completableFuture = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            CredentialsResponse credentialsResponse = null;
            try{
                if(taskType == TaskType.RETRIEVAL){
                    ClientResponse secretsChestRetrievalResponse = secretsChestClient.retrieveSecrets((String) dataToAction);
                    credentialsResponse = validateResponseAndReturnEntity(secretsChestRetrievalResponse, CredentialsResponse.class);
                    completableFuture.complete(credentialsResponse);
                }else if(taskType == TaskType.UPLOAD){
                    ClientResponse secretsChestUploadResponse = secretsChestClient.uploadSecrets((byte[]) dataToAction);
                    credentialsResponse = validateResponseAndReturnEntity(secretsChestUploadResponse, CredentialsResponse.class);
                    completableFuture.complete(credentialsResponse);
                }
            }
            catch(Exception e){
                LOGGER.error("Error occurred while completing {}", taskType, e);
                throw new SecretsChestUtilsException("Error occurred while completing " + taskType, e);
            }
            return credentialsResponse;
        }, executorService).applyToEither(credentialsUtils.timeoutRetrieveInvocationResponse(completableFuture, 15, TimeUnit.SECONDS), Function.identity());
        return completableFuture;
    }


    private <T> T validateResponseAndReturnEntity(ClientResponse response, Class<T> entityType){
        if(response == null){
            LOGGER.error("Secrets Chest response came back as null");
            throw new SecretsChestUtilsException("Secrets Chest response came back as null");
        }
        if(response.rawStatusCode() != 200){
            LOGGER.error("Secrets Chest response came back as non 200");
            throw new SecretsChestUtilsException("Secrets Chest response came back as non 200");
        }
        Mono<ResponseEntity<T>> responseEntity = response.toEntity(entityType);
        ResponseEntity<T> responseEntityBlock = responseEntity.block();
        if(responseEntityBlock == null){
            LOGGER.error("Secrets Chest response came back as null");
            throw new SecretsChestUtilsException("Secrets Chest response came back as null");
        }
        return responseEntityBlock.getBody();
    }

    @PreDestroy
    public void preDestroy(){
        executorService.shutdown();
    }
}
