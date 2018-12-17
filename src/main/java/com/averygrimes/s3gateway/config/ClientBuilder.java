package com.averygrimes.s3gateway.config;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-13
 * https://github.com/helloavery
 */


import com.amazonaws.opensdk.config.ConnectionConfiguration;
import com.amazonaws.opensdk.config.TimeoutConfiguration;
import com.amazonaws.opensdk.retry.RetryPolicyBuilder;
import com.averygrimes.aGateway.sdk.AGatewaySdk;
import com.averygrimes.aGateway.sdk.model.BadRequestException;

import java.net.SocketTimeoutException;

public abstract class ClientBuilder {

    protected AGatewaySdk gatewayClient;
    protected String bucket;
    protected String bucketItem;

    public void init(){
        initSdk();
        setBucketParams();
    }

    private void initSdk(){
        gatewayClient = AGatewaySdk.builder()
                .connectionConfiguration(
                        new ConnectionConfiguration()
                                .maxConnections(100)
                                .connectionMaxIdleMillis(1000))
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

    private void setBucketParams(){
        bucket = "";
        bucketItem = "";
    }
}
