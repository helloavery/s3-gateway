package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.DefaultRequest;
import com.amazonaws.SdkClientException;
import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.opensdk.config.ConnectionConfiguration;
import com.amazonaws.opensdk.config.TimeoutConfiguration;
import com.amazonaws.opensdk.retry.RetryPolicyBuilder;
import com.averygrimes.aGateway.sdk.AGatewaySdk;
import com.averygrimes.aGateway.sdk.model.AGatewaySdkException;
import com.averygrimes.aGateway.sdk.model.BadRequestException;
import com.averygrimes.aGateway.sdk.model.PutFolderItemRequest;
import com.averygrimes.aGateway.sdk.model.PutFolderItemResult;
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

import java.net.SocketTimeoutException;
import java.net.URI;

@Service
public class S3BucketOperationsServiceImpl implements S3BucketOperationsService{

    private static final Logger LOGGER = LogManager.getLogger(S3BucketOperationsServiceImpl.class);

    private static final String S3_API_ENDPOINT = "https://strmq1i9be.execute-api.us-east-2.amazonaws.com";
    private static final String API_GATEWAY_STAGE = "/S3V2Prod";
    private static final String GET_S3_OBJECT ="/%s/%s";


    private CryptoService cryptoService;
    private AGatewaySdk gatewayClient;
    private RestTemplate restTemplate;

    public S3BucketOperationsServiceImpl(CryptoService cryptoService, RestTemplate restTemplate) throws Exception{
        this.cryptoService = cryptoService;
        this.restTemplate = restTemplate;
        clientConfigSetup();
    }

    private void clientConfigSetup() throws Exception{
        AWSCredentialsProvider awsCredentialsProvider = iamCredentialsSetup();
        gatewayClient = AGatewaySdk.builder()
                .connectionConfiguration(
                        new ConnectionConfiguration()
                                .maxConnections(100)
                                .connectionMaxIdleMillis(1000))
                .iamCredentials(awsCredentialsProvider)
                .timeoutConfiguration(
                        new TimeoutConfiguration()
                                .httpRequestTimeout(3000)
                                .totalExecutionTimeout(10000)
                                .socketTimeout(2000))
                .retryPolicy(RetryPolicyBuilder.standard()
                        .retryOnExceptions(BadRequestException.class, SocketTimeoutException.class)
                        .retryOnStatusCodes(429, 500)
                        .maxNumberOfRetries(10)
                        .fixedBackoff(100)
                        .build())
                .build();
    }

    @Override
    public void uploadAsset(S3GatewayDTO s3GatewayDTO){
        try {
            PutFolderItemRequest request = new PutFolderItemRequest();
            request.setFolder(s3GatewayDTO.getBucket());
            request.setItem(s3GatewayDTO.getBucketObject());
            PutFolderItemResult response = gatewayClient.putFolderItem(request);
        } catch(BadRequestException e) {
            // This is a modeled exception defined in the API
        } catch(AGatewaySdkException e) {
            // All service exceptions will extend from AGatewaySdkException.
            // Any unknown or unmodeled service exceptions will be represented as a AGatewaySdkException.
        } catch(SdkClientException e) {
            // Client exceptions include timeouts, IOExceptions, or any other exceptional situation where a response
            // is not received from the service.
        }
    }

    @Override
    public S3GatewayDTO fetchAsset(S3GatewayDTO s3GatewayDTO){
        try {
            SignableRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(),s3GatewayDTO.getBucketObject());
            if(signedRequest == null){
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            String requestUrl1 = String.format(S3_API_ENDPOINT + API_GATEWAY_STAGE + GET_S3_OBJECT, s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            HttpHeaders headers1 = new HttpHeaders();
            headers1.setContentType(MediaType.APPLICATION_JSON);
            headers1.add("Host", signedRequest.getHeaders().get("Host").toString());
            headers1.add("X-Amz-Date", signedRequest.getHeaders().get("X-Amz-Date").toString());
            headers1.add("Authorization", signedRequest.getHeaders().get("Authorization").toString());
            headers1.add("cache-control", "no-cache");
            HttpEntity<?> request1 = new HttpEntity<>(headers1);
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl1, HttpMethod.GET, request1, String.class);
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
            ((DefaultRequest) request).setResourcePath(String.format(API_GATEWAY_STAGE+GET_S3_OBJECT,bucket,bucketObject));
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
