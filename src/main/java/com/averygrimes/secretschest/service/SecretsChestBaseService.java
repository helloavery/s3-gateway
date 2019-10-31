package com.averygrimes.secretschest.service;

import com.averygrimes.secretschest.pojo.S3GatewayDTO;

import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

public interface S3BucketOperationsService {

    Response uploadAsset(S3GatewayDTO s3GatewayDTO);

    Response fetchAsset(S3GatewayDTO s3GatewayDTO);
}
