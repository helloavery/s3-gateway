package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.DefaultRequest;
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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
            String requestBody = cryptoService.decryptAndUploadSecrets(s3GatewayDTO.getCipherText(), s3GatewayDTO.getPublicKey(), s3GatewayDTO.getSignature(), s3GatewayDTO.getSymmetricKeyUUID());
            String requestUrl = String.format(S3_API_ENDPOINT + API_GATEWAY_STAGE + S3_OBJECT_ENDPOINT, s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            DefaultRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject(), HttpMethod.PUT, requestBody);
            if (signedRequest == null) {
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            HttpHeaders headers = generateHeaders(signedRequest, HttpMethod.PUT, requestBody.length());
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
            DefaultRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(),s3GatewayDTO.getBucketObject(), HttpMethod.GET,null);
            if(signedRequest == null){
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            String requestUrl = String.format(S3_API_ENDPOINT + API_GATEWAY_STAGE + S3_OBJECT_ENDPOINT, s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            HttpHeaders headers = generateHeaders(signedRequest, HttpMethod.GET, null);
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.GET, request, String.class);
            if(responseEntity == null || responseEntity.getBody() == null){
                LOGGER.error("Error retrieving secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
                throw new RuntimeException("Error retrieving secrets");
            }
            return cryptoService.encryptAndSendSecrets(responseEntity.getBody(),s3GatewayDTO.getSymmetricKeyUUID());
        }
        catch(Exception e) {
            LOGGER.error("Error fetching secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            throw new RuntimeException("Error retrieving secrets for bucket/object: " + s3GatewayDTO.getBucket() + s3GatewayDTO.getBucketObject(), e);
        }
    }

    private HttpHeaders generateHeaders(DefaultRequest signedRequest, HttpMethod httpMethod, Integer contentLength){
        HttpHeaders headers = new HttpHeaders();
        switch(httpMethod){
            case GET:
                headers.add("Host", signedRequest.getHeaders().get("Host").toString());
                headers.add("X-Amz-Date", signedRequest.getHeaders().get("X-Amz-Date").toString());
                headers.add("Authorization", signedRequest.getHeaders().get("Authorization").toString());
                headers.setContentType(MediaType.APPLICATION_JSON);
                break;
            case PUT:
                headers.add("Host", signedRequest.getHeaders().get("Host").toString());
                headers.add("X-Amz-Date", signedRequest.getHeaders().get("X-Amz-Date").toString());
                headers.add("Authorization", signedRequest.getHeaders().get("Authorization").toString());
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.add("Content-Length", String.valueOf(contentLength));
                headers.add("cache-control", "no-cache");
                break;
            default:
                headers.add("Host", signedRequest.getHeaders().get("Host").toString());
                headers.add("X-Amz-Date", signedRequest.getHeaders().get("X-Amz-Date").toString());
                headers.add("Authorization", signedRequest.getHeaders().get("Authorization").toString());
                headers.setContentType(MediaType.APPLICATION_JSON);
                break;
        }
        return headers;
    }

    private AWSCredentialsProvider iamCredentialsSetup(){
        LOGGER.info("Retrieving IAM credentials");
        return new ProfileCredentialsProvider("default");
    }

    private DefaultRequest createAWSSignature(String bucket, String bucketObject, HttpMethod httpMethod, String content){
        try{
            LOGGER.info("Generating AWS Signature for bucket {} and object {}", bucket, bucketObject);
            AmazonWebServiceRequest amazonWebServiceRequest = new AmazonWebServiceRequest() {};
            AWS4Signer aws4Signer = new AWS4Signer();
            aws4Signer.setRegionName(Region.US_EAST_2.toString());
            aws4Signer.setEndpointPrefix(S3_API_ENDPOINT);
            aws4Signer.setServiceName("execute-api");
            DefaultRequest request = new DefaultRequest(amazonWebServiceRequest, "execute-api");
            request.setResourcePath(String.format(API_GATEWAY_STAGE+S3_OBJECT_ENDPOINT,bucket,bucketObject));
            switch (httpMethod) {
                case GET:
                    request.setHttpMethod(HttpMethodName.GET);
                    break;
                case PUT:
                    request.setHttpMethod(HttpMethodName.PUT);
                    request.addHeader("Content-Type", "text/plain");
                    request.addHeader("cache-control", "no-cache");
                    request.addHeader("Content-Length", String.valueOf(content.length()));
                    request.setContent(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    request.setHttpMethod(HttpMethodName.GET);
                    break;
            }
            request.setEndpoint(URI.create(S3_API_ENDPOINT));
            aws4Signer.sign(request,iamCredentialsSetup().getCredentials());
            return request;
        }
        catch(Exception e){
            LOGGER.error("Error generating AWS Signature for bucket {} and bucketObject {}", bucket, bucketObject);
            throw new AWSSignatureException("Error generating AWS Signature", e);
        }
    }
}
