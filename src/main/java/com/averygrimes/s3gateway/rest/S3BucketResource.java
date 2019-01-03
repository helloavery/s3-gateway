package com.averygrimes.s3gateway.rest;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.dto.S3GatewayDTO;
import com.averygrimes.s3gateway.service.S3BucketOperationsService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Response;


@RestController
@RequestMapping("/v1/s3bucketOperations")
public class S3BucketResource {

    private S3BucketOperationsService s3BucketOperationsService;

    public S3BucketResource(S3BucketOperationsService s3BucketOperationsService){
        this.s3BucketOperationsService = s3BucketOperationsService;
    }

    @RequestMapping(value = "/addItemRequest", method = RequestMethod.POST)
    public Response addItemRequest(){
        return null;

    }

    @RequestMapping(value = "/getItemRequest", method = RequestMethod.POST)
    public S3GatewayDTO getItemRequest(@RequestBody S3GatewayDTO s3GatewayDTO){
        return s3BucketOperationsService.fetchAsset(s3GatewayDTO);
    }
}
