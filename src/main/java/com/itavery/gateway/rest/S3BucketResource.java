package com.itavery.gateway.rest;
 
 /*=============================================================================
 |                Forecaster V1.0
 |
 |       File created by: Avery Grimes-Farrow
 |
 |       Created On:  2018-12-14            
 |            
 *===========================================================================*/

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
