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
import com.averygrimes.secretschest.config.ProgramArguments;
import com.averygrimes.secretschest.exceptions.AWSSignatureException;
import com.averygrimes.secretschest.exceptions.S3GatewayException;
import com.averygrimes.secretschest.interaction.client.AWSClient;
import com.averygrimes.secretschest.pojo.S3GatewayDTO;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class S3BucketOperationsServiceImpl implements S3BucketOperationsService{

    private static final Logger LOGGER = LogManager.getLogger(S3BucketOperationsServiceImpl.class);

    private ProgramArguments programArguments;
    private CryptoService cryptoService;
    private AWSClient awsClient;

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

    @Override
    public Response uploadAsset(S3GatewayDTO s3GatewayDTO) {
        try {
            String requestBody = cryptoService.decryptAndUploadSecrets(s3GatewayDTO.getCipherText(), s3GatewayDTO.getPublicKey(), s3GatewayDTO.getSignature(), s3GatewayDTO.getSymmetricKeyUUID());
            DefaultRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject(), HttpMethodName.PUT, requestBody);
            if (MapUtils.isEmpty(signedRequest.getHeaders())) {
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            Response uploadBucketObjectResponse = awsClient.uploadBucketObject((String) signedRequest.getHeaders().get("Host"), (String) signedRequest.getHeaders().get("X-Amz-Date"),
                    (String) signedRequest.getHeaders().get("Authorization"), String.valueOf(requestBody.length()), "no-cache",
                    s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject(), requestBody);
            validateResponse(uploadBucketObjectResponse);
            return Response.ok(true).build();
        }
        catch (Exception e) {
            LOGGER.error("Error uploading secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            throw new RuntimeException("Error uploading secrets for bucket/object: " + s3GatewayDTO.getBucket() + s3GatewayDTO.getBucketObject(), e);
        }
    }

    @Override
    public Response fetchAsset(S3GatewayDTO s3GatewayDTO){
        try {
            DefaultRequest signedRequest = createAWSSignature(s3GatewayDTO.getBucket(),s3GatewayDTO.getBucketObject(), HttpMethodName.GET,null);
            if(MapUtils.isEmpty(signedRequest.getHeaders())){
                LOGGER.error("Error generating AWS Signature");
                throw new AWSSignatureException("Error generating AWS Signature");
            }
            Response bucketObjectResponse = awsClient.getBucketObject((String) signedRequest.getHeaders().get("Host"), (String) signedRequest.getHeaders().get("X-Amz-Date"),
                    (String) signedRequest.getHeaders().get("Authorization"), s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
            validateResponse(bucketObjectResponse);
            ByteArrayOutputStream baos = (ByteArrayOutputStream) bucketObjectResponse.getEntity();
            String bucketObjectData =  baos.toString();
            baos.close();
            S3GatewayDTO encryptedSecrets = cryptoService.encryptAndSendSecrets(bucketObjectData, s3GatewayDTO.getSymmetricKeyUUID());
            if(encryptedSecrets == null){
                LOGGER.error("Error encrypting secrets for bucket {} and object {}", s3GatewayDTO.getBucket(), s3GatewayDTO.getBucketObject());
                throw S3GatewayException.buildResponse("S3Gateway: Error encrypting secrets");
            }
            return Response.ok(encryptedSecrets).build();
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

    private DefaultRequest createAWSSignature(String bucket, String bucketObject, HttpMethodName httpMethod, String content){
        try{
            LOGGER.info("Generating AWS Signature for bucket {} and object {}", bucket, bucketObject);
            AmazonWebServiceRequest amazonWebServiceRequest = new AmazonWebServiceRequest() {};
            AWS4Signer aws4Signer = new AWS4Signer();
            aws4Signer.setRegionName(Region.US_EAST_2.toString());
            aws4Signer.setEndpointPrefix(programArguments.getAWSS3APIEndpoint());
            aws4Signer.setServiceName("execute-api");
            DefaultRequest request = new DefaultRequest(amazonWebServiceRequest, "execute-api");
            request.setResourcePath(programArguments.getAWSAPIGatewayStage() + "/" + bucket + "/" + bucketObject);
            request.setHttpMethod(httpMethod);
            if(httpMethod == HttpMethodName.PUT){
                request.setHttpMethod(httpMethod);
                request.addHeader("Content-Type", "text/plain");
                request.addHeader("cache-control", "no-cache");
                request.addHeader("Content-Length", String.valueOf(content.length()));
                request.setContent(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            }
            request.setEndpoint(URI.create(programArguments.getAWSS3APIEndpoint()));
            aws4Signer.sign(request,iamCredentialsSetup().getCredentials());
            return request;
        }
        catch(Exception e){
            LOGGER.error("Error generating AWS Signature for bucket {} and bucketObject {}", bucket, bucketObject);
            throw new AWSSignatureException("Error generating AWS Signature", e);
        }
    }

    private void validateResponse(Response response){
        if(response == null || response.getStatus() != 200){
            throw S3GatewayException.buildResponse("S3Gateway: Error calling AWS S3 in order to retrieve/upload secrets");
        }
    }
}
