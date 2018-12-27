package com.averygrimes.s3gateway.service;

import com.averygrimes.s3gateway.dto.S3GatewayDTO;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

public interface S3BucketOperationsService {

    void uploadAsset(S3GatewayDTO s3GatewayDTO);

    S3GatewayDTO fetchAsset(S3GatewayDTO s3GatewayDTO);
}
