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
import com.averygrimes.secretschest.cache.KeyCache;
import com.averygrimes.secretschest.config.ProgramArguments;
import com.averygrimes.secretschest.exceptions.AWSSignatureException;
import com.averygrimes.secretschest.exceptions.SecretsChestServerException;
import com.averygrimes.secretschest.interaction.AWSClient;
import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import com.averygrimes.secretschest.utils.ResponseBuilder;
import com.averygrimes.secretschest.utils.UUIDUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

@Service
public class SecretsChestBaseServiceImpl implements SecretsChestBaseService {

    private static final Logger LOGGER = LogManager.getLogger(SecretsChestBaseServiceImpl.class);

    private ProgramArguments programArguments;
    private CryptoService cryptoService;
    private AWSClient awsClient;
    private KeyCache keyCache;

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

    @PostConstruct
    public void setUp(){
        this.AWSS3APIEndpoint = programArguments.getAWSS3APIEndpoint();
        this.AWSAPIGatewayStage = programArguments.getAWSAPIGatewayStage();
        this.AWSS3DataBucket = programArguments.getAWSS3DataBucket();
        this.AWSS3KeyBucket = programArguments.getAWSS3KeyBucket();
    }

    @Override
    public Response uploadAsset(byte[] dataToUpload, String requestId){
        try {
            Map<String, Object> encryptedDataMap = cryptoService.generateDataKeyAndEncryptData(dataToUpload);
            ByteBuffer encryptedKey = (ByteBuffer) encryptedDataMap.get(SecretsChestConstants.ENCRYPTED_DATA_MAP_KEY);
            byte[] encryptedData = (byte[]) encryptedDataMap.get(SecretsChestConstants.ENCRYPTED_KEY_MAP_KEY);

            String bucketObjectReference = UUIDUtils.generateUUID();

            DefaultRequest dataSignedRequest = createAWSSignature(AWSS3DataBucket, bucketObjectReference, HttpMethodName.PUT, encryptedData);
            DefaultRequest keySignedRequest = createAWSSignature(AWSS3KeyBucket, bucketObjectReference, HttpMethodName.PUT, null);

            validateDefaultRequest(dataSignedRequest, requestId);
            validateDefaultRequest(keySignedRequest, requestId);

            Response uploadEncryptedDataResponse = sendUploadBucketObjectResponse(dataSignedRequest, encryptedData, AWSS3DataBucket, bucketObjectReference);
            Response uploadEncryptedKeyResponse = sendUploadBucketObjectResponse(keySignedRequest, convertByteBufferToByteArray(encryptedKey), AWSS3KeyBucket, bucketObjectReference);
            validateResponse(uploadEncryptedDataResponse);
            validateResponse(uploadEncryptedKeyResponse);

            keyCache.put(bucketObjectReference, encryptedKey);

            return ResponseBuilder.createSuccessfulUploadDataResponse(bucketObjectReference);
        }
        catch (Exception e) {
            LOGGER.error("Error uploading new secrets for bucket {} for requestId {}", "dataToUpload", requestId);
            throw SecretsChestServerException.buildResponse("Error uploading secrets data for request id: " + requestId);
        }
    }

    public Response updateAsset(String secretsReference, byte[] dataToUpload, String requestId){
        try {
            ByteBuffer encryptedKey = attemptToGetKeyFromCacheThenBucket(secretsReference, requestId);
            byte[] encryptedData = cryptoService.encryptDataWithoutGeneratingDataKey(dataToUpload, encryptedKey);

            DefaultRequest dataSignedRequest = createAWSSignature(AWSS3DataBucket, secretsReference, HttpMethodName.PUT, encryptedData);
            validateDefaultRequest(dataSignedRequest, requestId);

            Response replaceEncryptedDataResponse = sendUploadBucketObjectResponse(dataSignedRequest, encryptedData, AWSS3DataBucket, secretsReference);
            validateResponse(replaceEncryptedDataResponse);

            return ResponseBuilder.createSuccessfulUploadDataResponse(secretsReference);

        }
        catch (Exception e) {
            LOGGER.error("Error updating secrets for bucket {} for requestId {}", "dataToUpload", requestId);
            throw SecretsChestServerException.buildResponse("Error updating secrets data for request id: " + requestId);
        }
    }

    @Override
    public Response retrieveAsset(String secretReference, String requestId){
        try {
            DefaultRequest signedRequest = createAWSSignature(AWSS3DataBucket, secretReference, HttpMethodName.GET, null);
            validateDefaultRequest(signedRequest, requestId);

            Response bucketObjectResponse = sendRetrieveBucketObjectResponse(signedRequest, AWSS3DataBucket, secretReference);
            validateResponse(bucketObjectResponse);
            byte[] encryptedData = bucketObjectResponse.readEntity(byte[].class);

            ByteBuffer encryptedKey = attemptToGetKeyFromCacheThenBucket(secretReference, requestId);

            byte[] decryptedData = cryptoService.decryptData(encryptedData, encryptedKey);
            return ResponseBuilder.createSuccessfulRetrieveDataResponse(decryptedData);
        }
        catch(Exception e) {
            LOGGER.error("Error fetching secrets for object reference {}", secretReference);
            throw SecretsChestServerException.buildResponse("Error fetching secrets for secret" + secretReference + " requestId: " + requestId);
        }
    }

    private AWSCredentialsProvider iamCredentialsSetup(){
        LOGGER.info("Retrieving IAM credentials");
        return new ProfileCredentialsProvider("default");
    }

    private DefaultRequest createAWSSignature(String bucket, String bucketObject, HttpMethodName httpMethod, byte[] content){
        try{
            LOGGER.info("Generating AWS Signature for bucket {} and object {}", bucket, bucketObject);
            AmazonWebServiceRequest amazonWebServiceRequest = new AmazonWebServiceRequest() {};
            AWS4Signer aws4Signer = new AWS4Signer();
            aws4Signer.setRegionName(Region.US_EAST_2.toString());
            aws4Signer.setEndpointPrefix(AWSS3APIEndpoint);
            aws4Signer.setServiceName("execute-api");
            DefaultRequest request = new DefaultRequest(amazonWebServiceRequest, "execute-api");
            request.setResourcePath(AWSAPIGatewayStage + "/" + bucket + "/" + bucketObject);
            request.setHttpMethod(httpMethod);
            if(httpMethod == HttpMethodName.PUT){
                request.setHttpMethod(httpMethod);
                request.addHeader("Content-Type", "text/plain");
                request.addHeader("cache-control", "no-cache");
                request.addHeader("Content-Length", String.valueOf(content.length));
                request.setContent(new ByteArrayInputStream(content));
            }
            request.setEndpoint(URI.create(AWSS3APIEndpoint));
            aws4Signer.sign(request,iamCredentialsSetup().getCredentials());
            return request;
        }
        catch(Exception e){
            LOGGER.error("Error generating AWS Signature for bucket {} and bucketObject {}", bucket, bucketObject);
            throw new AWSSignatureException("Error generating AWS Signature", e);
        }
    }

    private void validateDefaultRequest(DefaultRequest defaultRequest, String requestId){
        if (MapUtils.isEmpty(defaultRequest.getHeaders())) {
            LOGGER.error("Error generating AWS Signature for request id {}", requestId);
            throw SecretsChestServerException.buildResponse("Error generating AWS Signature for request id: " + requestId);
        }
    }

    private ByteBuffer attemptToGetKeyFromCacheThenBucket(String secretReference, String requestId){
        if(keyCache.get(secretReference) != null){
            return (ByteBuffer) keyCache.get(secretReference);
        }
        else{
            DefaultRequest encryptedKeySignedRequest = createAWSSignature(AWSS3KeyBucket, secretReference, HttpMethodName.GET, null);
            validateDefaultRequest(encryptedKeySignedRequest, requestId);
            Response encryptedKeyResponse = sendRetrieveBucketObjectResponse(encryptedKeySignedRequest, AWSS3KeyBucket, secretReference);
            validateResponse(encryptedKeyResponse);
            byte[] encryptedKeyBytes = encryptedKeyResponse.readEntity(byte[].class);
            return convertByteArrayToByteBuffer(encryptedKeyBytes);
        }
    }

    private Response sendUploadBucketObjectResponse(DefaultRequest signedRequest, byte[] dataToUpload, String bucket, String bucketObjectReference){
        return awsClient.uploadBucketObject((String) signedRequest.getHeaders().get("Host"), (String) signedRequest.getHeaders().get("X-Amz-Date"),
                (String) signedRequest.getHeaders().get("Authorization"), String.valueOf(dataToUpload.length), "no-cache",
                bucket, bucketObjectReference, dataToUpload);
    }

    private Response sendRetrieveBucketObjectResponse(DefaultRequest signedRequest, String bucket, String secretReference){
        return awsClient.getBucketObject((String) signedRequest.getHeaders().get("Host"), (String) signedRequest.getHeaders().get("X-Amz-Date"),
                (String) signedRequest.getHeaders().get("Authorization"), bucket, secretReference);
    }

    private void validateResponse(Response response){
        if(response == null || response.getStatus() != 200){
            throw SecretsChestServerException.buildResponse("Secrets Chest: Error calling AWS S3 in order to retrieve/upload secrets");
        }
    }

    private byte[] convertByteBufferToByteArray(ByteBuffer byteBuffer){
        byte[] bytesArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytesArray, 0, bytesArray.length);
        return bytesArray;
    }

    private ByteBuffer convertByteArrayToByteBuffer(byte[] bytes){
        return ByteBuffer.wrap(bytes);
    }
}
