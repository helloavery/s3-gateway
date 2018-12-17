package com.averygrimes.s3gateway.rest;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.ws.Response;

@RestController
@RequestMapping("/api/s3bucketOperations")
public class S3BucketResource {

    @RequestMapping(value = "/addItemRequest", method = RequestMethod.PUT)
    public Response addItemRequest(){

    }
}
