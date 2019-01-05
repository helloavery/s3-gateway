package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.DefaultRequest;
import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.averygrimes.s3gateway.dto.S3GatewayDTO;
import com.averygrimes.s3gateway.exceptions.AWSSignatureException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@Service
public class S3BucketOperationsServiceImpl implements S3BucketOperationsService{

    private static final Logger LOGGER = LogManager.getLogger(S3BucketOperationsServiceImpl.class);

    private static final String S3_API_ENDPOINT = "https://strmq1i9be.execute-api.us-east-2.amazonaws.com";
    private static final String API_GATEWAY_STAGE = "/S3V2Prod";
    private static final String S3_OBJECT_ENDPOINT ="/%s/%s";


    private CryptoService cryptoService;
    private RestTemplate restTemplate;

    public S3BucketOperationsServiceImpl(CryptoService cryptoService, RestTemplate restTemplate){
        this.cryptoService = cryptoService;
        this.restTemplate = restTemplate;
    }


    @SuppressWarnings("Duplicates")
    @Override
    public boolean uploadAsset(S3GatewayDTO s3GatewayDTO) {
        try {
            SignableRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            if (signedRequest == null) {
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            String requestBody = cryptoService.cryptoPrepareSecretsS3Upload(s3GatewayDTO.getCipherText(), s3GatewayDTO.getPublicKey(), s3GatewayDTO.getSignature(), s3GatewayDTO.getEhcacheVariable());
            String requestUrl = String.format(S3_API_ENDPOINT + API_GATEWAY_STAGE + S3_OBJECT_ENDPOINT, s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            HttpHeaders headers = generateHeaders(signedRequest);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.PUT, request, String.class);
            if(!responseEntity.getStatusCode().is2xxSuccessful()){
                LOGGER.error("Error uploading secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
                throw new RuntimeException("Error uploading secrets");
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.error("Error uploading secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            throw new RuntimeException("Error uploading secrets for bucket/object: " + s3GatewayDTO.getBucket() + s3GatewayDTO.getBucketObject(), e);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public S3GatewayDTO fetchAsset(S3GatewayDTO s3GatewayDTO){
        try {
            SignableRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(),s3GatewayDTO.getBucketObject());
            if(signedRequest == null){
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            String requestUrl = String.format(S3_API_ENDPOINT + API_GATEWAY_STAGE + S3_OBJECT_ENDPOINT, s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            HttpHeaders headers = generateHeaders(signedRequest);
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.GET, request, String.class);
            if(responseEntity == null || responseEntity.getBody() == null){
                LOGGER.error("Error retrieving secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
                throw new RuntimeException("Error retrieving secrets");
            }
            return cryptoService.cryptoPrepareSendSecrets(responseEntity.getBody(),s3GatewayDTO.getPublicKey());
        }
        catch(Exception e) {
            LOGGER.error("Error fetching secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            throw new RuntimeException("Error retrieving secrets for bucket/object: " + s3GatewayDTO.getBucket() + s3GatewayDTO.getBucketObject(), e);
        }
    }

    private HttpHeaders generateHeaders(SignableRequest signedRequest){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Host", signedRequest.getHeaders().get("Host").toString());
        headers.add("X-Amz-Date", signedRequest.getHeaders().get("X-Amz-Date").toString());
        headers.add("Authorization", signedRequest.getHeaders().get("Authorization").toString());
        headers.add("cache-control", "no-cache");
        return headers;
    }

    private AWSCredentialsProvider iamCredentialsSetup(){
        LOGGER.info("Retrieving IAM credentials");
        return new ProfileCredentialsProvider("default");
    }

    private SignableRequest createAWSSignature(String bucket, String bucketObject){
        try{
            LOGGER.info("Generating AWS Signature for bucket {} and object {}", bucket, bucketObject);
            AWS4Signer aws4Signer = new AWS4Signer();
            aws4Signer.setRegionName(Region.US_EAST_2.toString());
            aws4Signer.setEndpointPrefix(S3_API_ENDPOINT);
            aws4Signer.setServiceName("execute-api");
            SignableRequest request = new DefaultRequest("execute-api");
            ((DefaultRequest) request).setResourcePath(String.format(API_GATEWAY_STAGE+S3_OBJECT_ENDPOINT,bucket,bucketObject));
            ((DefaultRequest) request).setHttpMethod(HttpMethodName.GET);
            ((DefaultRequest) request).setEndpoint(URI.create(S3_API_ENDPOINT));
            aws4Signer.sign(request,iamCredentialsSetup().getCredentials());
            return request;
        }
        catch(Exception e){
            LOGGER.error("Error generating AWS Signature for bucket {} and bucketObject {}", bucket, bucketObject);
            throw new AWSSignatureException("Error generating AWS Signature", e);
        }
    }
}
