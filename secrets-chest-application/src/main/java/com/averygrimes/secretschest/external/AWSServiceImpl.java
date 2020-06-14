package com.averygrimes.secretschest.external;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.averygrimes.secretschest.exceptions.AWSOperationException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

@Service
public class AWSServiceImpl implements AWSService{

    private static final Logger LOGGER = LogManager.getLogger(AWSServiceImpl.class);

    private AmazonS3 amazonS3;

    @PostConstruct
    public void init(){
        this.amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).withCredentials(iamCredentialsSetup()).build();
    }

    @Override
    public void sendUploadBucketObjectRequest(String bucket, String bucketObjectReference, String dataToUpload, String requestId) {
        try{
            LOGGER.info("Uploading data to bucket {} for requestId {}", bucket, requestId);
            amazonS3.putObject(bucket,bucketObjectReference, dataToUpload);
        }
        catch(Exception e){
            throw new AWSOperationException("Error uploading object to bucket " + bucket + " for request id " + requestId, e);
        }
    }

    @Override
    public Object sendRetrieveBucketObjectResponse(String bucket, String secretReference, String requestId, boolean returnAsString) {
        try{
            LOGGER.info("Retrieving data from bucket {} for requestId {}", bucket, requestId);
            S3Object s3Object = amazonS3.getObject(bucket, secretReference);
            validateS3Response(s3Object);
            return returnAsString ? IOUtils.toString(s3Object.getObjectContent().getDelegateStream(), StandardCharsets.UTF_8) : s3Object;
        }
        catch(Exception e){
            throw new AWSOperationException("Error uploading object to bucket " + bucket + " for request id " + requestId, e);
        }
    }

    private void validateS3Response(S3Object s3Object){
        if(s3Object == null){
            throw new AWSOperationException("Error calling AWS S3 in order to retrieve/upload secrets, response came back null");
        }
    }

    private AWSCredentialsProvider iamCredentialsSetup(){
        LOGGER.info("Retrieving IAM credentials");
        return new ProfileCredentialsProvider("default");
    }
}
