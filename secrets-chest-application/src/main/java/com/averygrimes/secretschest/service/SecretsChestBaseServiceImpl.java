package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.secretschest.cache.KeyCache;
import com.averygrimes.secretschest.config.ProgramArguments;
import com.averygrimes.secretschest.exceptions.SecretsChestServerException;
import com.averygrimes.secretschest.external.AWSService;
import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import com.averygrimes.secretschest.pojo.SecretsChestResponse;
import com.averygrimes.secretschest.utils.SecretsChestCredUtils;
import com.averygrimes.secretschest.utils.UUIDUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Service
public class SecretsChestBaseServiceImpl implements SecretsChestBaseService {

    private static final Logger LOGGER = LogManager.getLogger(SecretsChestBaseServiceImpl.class);

    private ProgramArguments programArguments;
    private CryptoService cryptoService;
    private KeyCache keyCache;
    private ExecutorService executorService;
    private SecretsChestCredUtils chestCredUtils;
    private Lock lock;
    private AWSService awsService;
    private String AWSS3DataBucket;
    private String AWSS3KeyBucket;

    @Inject
    public void setProgramArguments(ProgramArguments programArguments) {
        this.programArguments = programArguments;
    }

    @Inject
    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Inject
    public void setKeyCache(KeyCache keyCache) {
        this.keyCache = keyCache;
    }

    @Inject
    public void setChestCredUtils(SecretsChestCredUtils chestCredUtils) {
        this.chestCredUtils = chestCredUtils;
    }

    @Inject
    public void setAwsService(AWSService awsService) {
        this.awsService = awsService;
    }

    @PostConstruct
    public void init(){
        this.AWSS3DataBucket = programArguments.getAWSS3DataBucket();
        this.AWSS3KeyBucket = programArguments.getAWSS3KeyBucket();
        this.executorService = Executors.newFixedThreadPool(100);
        this.lock = new ReentrantLock(true);
    }

    @Override
    public SecretsChestResponse uploadAsset(byte[] dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if (lock.tryLock(1500, TimeUnit.MILLISECONDS)) {
                try {
                    CountDownLatch countDownLatch = new CountDownLatch(2);
                    Map<String, byte[]> encryptedDataMap = cryptoService.generateDataKeyAndEncryptData(dataToUpload);
                    String bucketObjectReference = UUIDUtils.generateRandomId();
                    encryptedDataMap.forEach((mapKey, encryptedData) -> {
                        String bucket = mapKey.equals(SecretsChestConstants.ENCRYPTED_KEY_MAP_KEY) ? AWSS3KeyBucket : AWSS3DataBucket;
                        CompletableFuture<SecretsChestResponse> completableFuture = sendEncryptedUploadTasks(bucket, encryptedData, bucketObjectReference, requestId);
                        completableFuture.whenComplete((uploadResponse, exception) -> {
                            if (uploadResponse == null || exception != null) {
                                LOGGER.error("Exception occurred while uploading data to S3 bucket", exception);
                                throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
                            }
                            if (!uploadResponse.isSuccessful()) {
                                LOGGER.error("Operation uploading data to S3 bucket unsuccessful");
                                throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
                            }
                            countDownLatch.countDown();
                        });
                    });
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        LOGGER.warn("Thread has been interrupted");
                    }
                    secretsChestResponse.setSecretReference(bucketObjectReference);
                    secretsChestResponse.setSuccessful(true);
                } catch (Exception e) {
                    LOGGER.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
                    throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Thread has been interrupted while acquiring lock");
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse uploadPlainTextAsset(String dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String bucketObjectReference = UUIDUtils.generateRandomId();
            awsService.sendUploadBucketObjectRequest(AWSS3DataBucket, bucketObjectReference, dataToUpload, requestId);
            secretsChestResponse.setSecretReference(bucketObjectReference);
            secretsChestResponse.setSuccessful(true);
        }
        catch (Exception e) {
            LOGGER.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
            throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse updateAsset(String secretsReference, byte[] dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            ByteBuffer encryptedKey = attemptToGetKeyFromCacheThenBucket(secretsReference, requestId).asByteBuffer();
            byte[] encryptedData = cryptoService.encryptDataWithoutGeneratingDataKey(dataToUpload, SdkBytes.fromByteBuffer(encryptedKey));
            String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);
            awsService.sendUploadBucketObjectRequest(AWSS3DataBucket, secretsReference, hexEncodedEncryptedData, requestId);
            secretsChestResponse.setSecretReference(secretsReference);
            secretsChestResponse.setSuccessful(true);
        }
        catch (Exception e) {
            LOGGER.error("Error updating secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
            throw SecretsChestServerException.buildResponse("Error updating secrets data for request id: " + requestId);
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse retrieveAsset(String secretReference, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if(lock.tryLock(3500, TimeUnit.MILLISECONDS)){
                try{
                    String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(AWSS3DataBucket, secretReference, requestId, true);
                    byte[] hexDecodedBucketObject = Hex.decodeHex(s3ObjectOutput);
                    SdkBytes encryptedKey = attemptToGetKeyFromCacheThenBucket(secretReference, requestId);
                    byte[] decryptedData = cryptoService.decryptData(hexDecodedBucketObject, encryptedKey.asByteBuffer());
                    secretsChestResponse.setData(decryptedData);
                    secretsChestResponse.setSuccessful(true);
                }catch(Exception e){
                    LOGGER.error("Error fetching secrets for object reference {}", secretReference, e);
                    throw SecretsChestServerException.buildResponse("Error fetching secrets for secret" + secretReference + " requestId: " + requestId);
                }finally{
                    lock.unlock();
                }
            }
        } catch(InterruptedException e) {
            LOGGER.warn("Thread has been interrupted while acquiring lock");
        }
        return secretsChestResponse;
    }

    private CompletableFuture<SecretsChestResponse> sendEncryptedUploadTasks(String bucket, byte[] dataToUpload, String bucketObjectReference, String requestId){
        CompletableFuture<SecretsChestResponse> completableFuture = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            SecretsChestResponse secretsChestResponse = null;
            try{
                secretsChestResponse = new SecretsChestResponse();
                String hexEncodedBytes = Hex.encodeHexString(dataToUpload);
                awsService.sendUploadBucketObjectRequest(bucket, bucketObjectReference, hexEncodedBytes, requestId);
                if(bucket.equals(AWSS3KeyBucket)){
                    putEncryptedKeyInCache(bucketObjectReference, dataToUpload);
                }
                secretsChestResponse.setSuccessful(true);
                completableFuture.complete(secretsChestResponse);
            }
            catch(Exception e){
                LOGGER.error("Error completing upload data task", e);
            }
            return secretsChestResponse;
        }, executorService).applyToEither(chestCredUtils.timeoutRetrieveInvocationResponse(completableFuture, 10, TimeUnit.SECONDS), Function.identity());
        return completableFuture;
    }

    private void putEncryptedKeyInCache(String bucketObjectReference, byte[] encryptedKey){
        try{
            keyCache.put(bucketObjectReference, encryptedKey);
        }
        catch(Exception e){
            LOGGER.warn("Error putting encrypted key in key cache", e);
        }
    }

    private SdkBytes attemptToGetKeyFromCacheThenBucket(String secretReference, String requestId){
        if(keyCache.get(secretReference) != null){
            byte[] encryptedKeyByteArray = (byte[]) keyCache.get(secretReference);
            return SdkBytes.fromByteArray(encryptedKeyByteArray);
        }
        else{
            try{
                String s3ObjectOutput = (String) awsService.sendRetrieveBucketObjectResponse(AWSS3KeyBucket, secretReference, requestId, true);
                byte[] hexDecodedKey = Hex.decodeHex(s3ObjectOutput);
                return SdkBytes.fromByteArray(hexDecodedKey);
            }
            catch(DecoderException e){
                LOGGER.error("Error decoding hex encrypted key for request id {}", requestId, e);
                throw SecretsChestServerException.buildResponse("Error decoding hex encrypted key for request id: " + requestId);
            }
        }
    }

    @PreDestroy
    public void preDestroy(){
        executorService.shutdown();
    }
}
