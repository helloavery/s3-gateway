package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.averygrimes.secretschest.cache.KeyCache;
import com.averygrimes.secretschest.config.ProgramArguments;
import com.averygrimes.secretschest.exceptions.AWSSignatureException;
import com.averygrimes.secretschest.exceptions.SecretsChestServerException;
import com.averygrimes.secretschest.interaction.AWSClient;
import com.averygrimes.secretschest.pojo.S3OperationMethod;
import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import com.averygrimes.secretschest.pojo.SecretsChestResponse;
import com.averygrimes.secretschest.utils.SecretsChestCredUtils;
import com.averygrimes.secretschest.utils.UUIDUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Service
public class SecretsChestBaseServiceImpl <T> implements SecretsChestBaseService {

    private static final Logger LOGGER = LogManager.getLogger(SecretsChestBaseServiceImpl.class);

    private ProgramArguments programArguments;
    private CryptoService cryptoService;
    private AWSClient awsClient;
    private AmazonS3 amazonS3;
    private KeyCache keyCache;
    private ExecutorService executorService;
    private SecretsChestCredUtils chestCredUtils;
    private Lock lock;
    private String AWSS3APIEndpoint;
    private String AWSAPIGatewayStage;
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
    public void setAwsClient(AWSClient awsClient) {
        this.awsClient = awsClient;
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
        this.AWSS3APIEndpoint = programArguments.getAWSS3APIEndpoint();
        this.AWSAPIGatewayStage = programArguments.getAWSAPIGatewayStage();
        this.AWSS3DataBucket = programArguments.getAWSS3DataBucket();
        this.AWSS3KeyBucket = programArguments.getAWSS3KeyBucket();
        this.executorService = Executors.newFixedThreadPool(100);
        this.lock = new ReentrantLock(true);
    }

    @Override
    public SecretsChestResponse uploadAsset(byte[] dataToUpload, S3OperationMethod method, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            if(lock.tryLock(2100, TimeUnit.MILLISECONDS)){
                CountDownLatch countDownLatch = new CountDownLatch(2);
                ConcurrentHashMap<String, byte[]> encryptedDataMap = cryptoService.generateDataKeyAndEncryptData(dataToUpload);
                String bucketObjectReference = UUIDUtils.generateRandomId();
                encryptedDataMap.forEach((mapKey, encryptedData) ->{
                    AtomicBoolean isApiGatewayRequest = new AtomicBoolean(method == S3OperationMethod.GATEWAY);
                    String bucket = mapKey.equals(SecretsChestConstants.ENCRYPTED_KEY_MAP_KEY) ? AWSS3KeyBucket : AWSS3DataBucket;
                    CompletableFuture<SecretsChestResponse> completableFuture = sendEncryptedUploadTasks(isApiGatewayRequest.get(), bucket, encryptedData, bucketObjectReference, requestId);
                    completableFuture.whenComplete((uploadResponse, exception) ->{
                        if(uploadResponse == null || exception != null){
                            LOGGER.error("Exception occurred while uploading data to S3 bucket", exception);
                            throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
                        }
                        if(!uploadResponse.isSuccessful()){
                            LOGGER.error("Operation uploading data to S3 bucket unsuccessful");
                            throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
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
                secretsChestResponse.setSecretReference(bucketObjectReference);
                secretsChestResponse.setSuccessful(true);
            }
        } catch (Exception e) {
            LOGGER.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId, e);
            throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
        }finally{
            lock.unlock();
        }
        return secretsChestResponse;
    }

    @Override
    public SecretsChestResponse uploadPlainTextAsset(String dataToUpload, String requestId){
        SecretsChestResponse secretsChestResponse = new SecretsChestResponse();
        try {
            String bucketObjectReference = UUIDUtils.generateRandomId();
            DefaultRequest<T> dataSignedRequest = createAWSSignature(AWSS3DataBucket, bucketObjectReference, HttpMethodName.PUT, dataToUpload, requestId);
            sendUploadBucketObjectResponse(dataSignedRequest, dataToUpload, AWSS3DataBucket, bucketObjectReference, requestId);
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
            DefaultRequest<T> dataSignedRequest = createAWSSignature(AWSS3DataBucket, secretsReference, HttpMethodName.PUT, hexEncodedEncryptedData, requestId);
            String hexEncodedData = Hex.encodeHexString(dataToUpload);
            sendUploadBucketObjectResponse(dataSignedRequest, hexEncodedData, AWSS3DataBucket, secretsReference, requestId);
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
                DefaultRequest<T> signedRequest = createAWSSignature(AWSS3DataBucket, secretReference, HttpMethodName.GET, null, requestId);
                Response bucketObjectResponse = sendRetrieveBucketObjectResponse(signedRequest, AWSS3DataBucket, secretReference, requestId);
                byte[] hexDecodedBucketObject = Hex.decodeHex(bucketObjectResponse.getEntity().toString());
                SdkBytes encryptedKey = attemptToGetKeyFromCacheThenBucket(secretReference, requestId);
                byte[] decryptedData = cryptoService.decryptData(hexDecodedBucketObject, encryptedKey.asByteBuffer());
                secretsChestResponse.setData(decryptedData);
                secretsChestResponse.setSuccessful(true);
            }
        } catch(Exception e) {
            LOGGER.error("Error fetching secrets for object reference {}", secretReference, e);
            throw SecretsChestServerException.buildResponse("Error fetching secrets for secret" + secretReference + " requestId: " + requestId);
        }finally{
            lock.unlock();
        }
        return secretsChestResponse;
    }

    private CompletableFuture<SecretsChestResponse> sendEncryptedUploadTasks(boolean apiGatewayRequest, String bucket, byte[] dataToUpload, String bucketObjectReference, String requestId){
        CompletableFuture<SecretsChestResponse> completableFuture = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            SecretsChestResponse secretsChestResponse = null;
            try{
                secretsChestResponse = new SecretsChestResponse();
                String hexEncodedBytes = Hex.encodeHexString(dataToUpload);
                if(bucket.equals(AWSS3KeyBucket)){
                    if(apiGatewayRequest){
                        DefaultRequest<T> keySignedRequest = createAWSSignature(AWSS3KeyBucket, bucketObjectReference, HttpMethodName.PUT, hexEncodedBytes, requestId);
                        sendUploadBucketObjectResponse(keySignedRequest, hexEncodedBytes, AWSS3KeyBucket, bucketObjectReference, requestId);
                    }else{
                        amazonS3.putObject(AWSS3KeyBucket,bucketObjectReference, hexEncodedBytes);
                    }
                    putEncryptedKeyInCache(bucketObjectReference, dataToUpload);
                    secretsChestResponse.setSuccessful(true);
                }else if(bucket.equals(AWSS3DataBucket)){
                    if(apiGatewayRequest){
                        DefaultRequest<T> dataSignedRequest = createAWSSignature(AWSS3DataBucket, bucketObjectReference, HttpMethodName.PUT, hexEncodedBytes, requestId);
                        sendUploadBucketObjectResponse(dataSignedRequest, hexEncodedBytes, AWSS3DataBucket, bucketObjectReference, requestId);
                    }else{
                        amazonS3.putObject(AWSS3KeyBucket,bucketObjectReference, hexEncodedBytes);
                    }
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
                DefaultRequest<T> encryptedKeySignedRequest = createAWSSignature(AWSS3KeyBucket, secretReference, HttpMethodName.GET, null, requestId);
                Response encryptedKeyResponse = sendRetrieveBucketObjectResponse(encryptedKeySignedRequest, AWSS3KeyBucket, secretReference, requestId);
                byte[] hexDecodedKey = Hex.decodeHex(encryptedKeyResponse.getEntity().toString());
                return SdkBytes.fromByteArray(hexDecodedKey);
            }
            catch(DecoderException e){
                LOGGER.error("Error decoding hex encrypted key for request id {}", requestId, e);
                throw SecretsChestServerException.buildResponse("Error decoding hex encrypted key for request id: " + requestId);
            }
        }
    }

    private void sendUploadBucketObjectResponse(DefaultRequest<T> signedRequest, String dataToUpload, String bucket, String bucketObjectReference, String requestId){
        Response response;
        LOGGER.info("Uploading data to bucket {} for requestId {}", bucket, requestId);
        try{
            response = awsClient.uploadBucketObject(signedRequest.getHeaders().get("Host"), signedRequest.getHeaders().get("X-Amz-Date"),
                    signedRequest.getHeaders().get("Authorization"), String.valueOf(dataToUpload.length()), "no-cache",
                    bucket, bucketObjectReference, dataToUpload);
            validateResponse(response);
        }
        catch(Exception e){
            LOGGER.error("Error uploading object to bucket {} for request id {}", bucket, requestId, e);
            LOGGER.error(((WebApplicationException) e).getResponse().getEntity().toString());
        }
    }

    private Response sendRetrieveBucketObjectResponse(DefaultRequest<T> signedRequest, String bucket, String secretReference, String requestId){
        LOGGER.info("Retrieving data from bucket {} for requestId {}", bucket, requestId);
        Response response = null;
        try{
            response = awsClient.getBucketObject(signedRequest.getHeaders().get("Host"), signedRequest.getHeaders().get("X-Amz-Date"),
                    signedRequest.getHeaders().get("Authorization"), bucket, secretReference);
            validateResponse(response);
        }
        catch(Exception e){
            LOGGER.error("Error uploading object to bucket {} for request id {}", bucket, requestId, e);
            LOGGER.error(((WebApplicationException) e).getResponse().getEntity().toString());
        }
        return response;
    }

    private void validateResponse(Response response){
        if(response == null || response.getStatus() != 200){
            throw SecretsChestServerException.buildResponse("Secrets Chest: Error calling AWS S3 in order to retrieve/upload secrets");
        }
    }

    private synchronized DefaultRequest<T> createAWSSignature(String bucket, String bucketObject, HttpMethodName httpMethod, String content, String requestId){
        try{
            LOGGER.info("Generating AWS Signature for bucket {} and object {}", bucket, bucketObject);
            AmazonWebServiceRequest amazonWebServiceRequest = new AmazonWebServiceRequest() {};
            AWS4Signer aws4Signer = new AWS4Signer();
            aws4Signer.setRegionName(Region.US_EAST_2.toString());
            aws4Signer.setEndpointPrefix(AWSS3APIEndpoint);
            aws4Signer.setServiceName("execute-api");
            DefaultRequest<T> defaultRequest = new DefaultRequest<>(amazonWebServiceRequest, "execute-api");
            defaultRequest.setResourcePath(AWSAPIGatewayStage + "/" + bucket + "/" + bucketObject);
            defaultRequest.setHttpMethod(httpMethod);
            if(httpMethod == HttpMethodName.PUT){
                defaultRequest.setHttpMethod(httpMethod);
                defaultRequest.addHeader("Content-Type", "text/plain");
                defaultRequest.addHeader("cache-control", "no-cache");
                defaultRequest.addHeader("Content-Length", String.valueOf(content.length()));
                defaultRequest.setContent(IOUtils.toInputStream(content, StandardCharsets.UTF_8));
            }
            defaultRequest.setEndpoint(URI.create(AWSS3APIEndpoint));
            aws4Signer.sign(defaultRequest,iamCredentialsSetup().getCredentials());
            validateDefaultRequest(defaultRequest, requestId);
            return defaultRequest;
        }
        catch(Exception e){
            LOGGER.error("Error generating AWS Signature for bucket {} and bucketObject {}", bucket, bucketObject, e);
            throw new AWSSignatureException("Error generating AWS Signature", e);
        }
    }

    private AWSCredentialsProvider iamCredentialsSetup(){
        LOGGER.info("Retrieving IAM credentials");
        return new ProfileCredentialsProvider("default");
    }

    private void validateDefaultRequest(DefaultRequest<T> defaultRequest, String requestId){
        LOGGER.info("Validating default request for request {}", requestId);
        if (MapUtils.isEmpty(defaultRequest.getHeaders())) {
            LOGGER.error("Error generating AWS Signature for request id {}", requestId);
            throw SecretsChestServerException.buildResponse("Error generating AWS Signature for request id: " + requestId);
        }
    }
}