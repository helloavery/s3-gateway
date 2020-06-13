package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.averygrimes.secretschest.cache.KeyCache;
import com.averygrimes.secretschest.config.ProgramArguments;
import com.averygrimes.secretschest.exceptions.SecretsChestServerException;
import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import com.averygrimes.secretschest.pojo.SecretsChestResponse;
import com.averygrimes.secretschest.utils.SecretsChestCredUtils;
import com.averygrimes.secretschest.utils.UUIDUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    private AmazonS3 amazonS3;
    private KeyCache keyCache;
    private ExecutorService executorService;
    private SecretsChestCredUtils chestCredUtils;
    private Lock lock;
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

    @PostConstruct
    public void init(){
        this.amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(iamCredentialsSetup()).build();
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
            sendUploadBucketObjectResponse(dataToUpload, AWSS3DataBucket, bucketObjectReference, requestId);
            secretsChestResponse.setSecretReference(bucketObjectReference);
            secretsChestResponse.setSuccessful(true);
        }
        catch (Exception e) {
            LOGGER.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
            throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
        }
        return secretsChestResponse;
    }

    public SecretsChestResponse updateAsset(String secretsReference, byte[] dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            ByteBuffer encryptedKey = attemptToGetKeyFromCacheThenBucket(secretsReference, requestId).asByteBuffer();
            byte[] encryptedData = cryptoService.encryptDataWithoutGeneratingDataKey(dataToUpload, SdkBytes.fromByteBuffer(encryptedKey));
            String hexEncodedEncryptedData = Hex.encodeHexString(encryptedData);
            sendUploadBucketObjectResponse(hexEncodedEncryptedData, AWSS3DataBucket, secretsReference, requestId);
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
                    S3Object bucketObject = sendRetrieveBucketObjectResponse(AWSS3DataBucket, secretReference, requestId);
                    String s3ObjectOutput = IOUtils.toString(bucketObject.getObjectContent().getDelegateStream(), StandardCharsets.UTF_8);
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
                if(bucket.equals(AWSS3KeyBucket)){
                    amazonS3.putObject(AWSS3KeyBucket,bucketObjectReference, hexEncodedBytes);
                    putEncryptedKeyInCache(bucketObjectReference, dataToUpload);
                    secretsChestResponse.setSuccessful(true);
                }else if(bucket.equals(AWSS3DataBucket)){
                    amazonS3.putObject(AWSS3DataBucket,bucketObjectReference, hexEncodedBytes);
                    secretsChestResponse.setSuccessful(true);
                }
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
                S3Object encryptedKeyResponse = sendRetrieveBucketObjectResponse(AWSS3KeyBucket, secretReference, requestId);
                String s3ObjectOutput = IOUtils.toString(encryptedKeyResponse.getObjectContent().getDelegateStream(), StandardCharsets.UTF_8);
                byte[] hexDecodedKey = Hex.decodeHex(s3ObjectOutput);
                return SdkBytes.fromByteArray(hexDecodedKey);
            }
            catch(DecoderException | IOException e){
                LOGGER.error("Error decoding hex encrypted key for request id {}", requestId, e);
                throw SecretsChestServerException.buildResponse("Error decoding hex encrypted key for request id: " + requestId);
            }
        }
    }

    private void sendUploadBucketObjectResponse(String dataToUpload, String bucket, String bucketObjectReference, String requestId){
        LOGGER.info("Uploading data to bucket {} for requestId {}", bucket, requestId);
        try{
            amazonS3.putObject(AWSS3KeyBucket,bucketObjectReference, dataToUpload);
        }
        catch(Exception e){
            LOGGER.error("Error uploading object to bucket {} for request id {}", bucket, requestId, e);
            LOGGER.error(((WebApplicationException) e).getResponse().getEntity().toString());
        }
    }

    private S3Object sendRetrieveBucketObjectResponse(String bucket, String secretReference, String requestId){
        LOGGER.info("Retrieving data from bucket {} for requestId {}", bucket, requestId);
        S3Object s3Object = null;
        try{
            s3Object = amazonS3.getObject(bucket, secretReference);
        }
        catch(Exception e){
            LOGGER.error("Error uploading object to bucket {} for request id {}", bucket, requestId, e);
            LOGGER.error(((WebApplicationException) e).getResponse().getEntity().toString());
        }
        validateS3Response(s3Object);
        return s3Object;
    }

    private void validateS3Response(S3Object s3Object){
        if(s3Object == null){
            throw SecretsChestServerException.buildResponse("Secrets Chest: Error calling AWS S3 in order to retrieve/upload secrets");
        }
    }

    private AWSCredentialsProvider iamCredentialsSetup(){
        LOGGER.info("Retrieving IAM credentials");
        return new ProfileCredentialsProvider("default");
    }

    @PreDestroy
    public void preDestroy(){
        executorService.shutdown();
    }
}
