package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.SdkClientException;
import com.averygrimes.aGateway.sdk.model.AGatewaySdkException;
import com.averygrimes.aGateway.sdk.model.BadRequestException;
import com.averygrimes.aGateway.sdk.model.GetFolderItemRequest;
import com.averygrimes.aGateway.sdk.model.GetFolderItemResult;
import com.averygrimes.aGateway.sdk.model.PutFolderItemRequest;
import com.averygrimes.aGateway.sdk.model.PutFolderItemResult;
import com.averygrimes.s3gateway.config.ClientBuilder;

public class S3BucketOperationsServiceImpl extends ClientBuilder implements S3BucketOperationsService{

    @Override
    public void init(){
        super.init();
    }

    @Override
    public void uploadAsset(){
        try {
            PutFolderItemRequest request = new PutFolderItemRequest();
            request.setFolder(bucket);
            request.setItem(bucketItem);
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
    public void fetchAsset(){
        try {
            GetFolderItemRequest request = new GetFolderItemRequest();
            request.setFolder(bucket);
            request.setItem(bucketItem);
            GetFolderItemResult response = gatewayClient.getFolderItem(request);
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


}
