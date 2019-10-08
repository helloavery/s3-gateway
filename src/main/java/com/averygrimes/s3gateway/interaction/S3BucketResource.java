package com.averygrimes.s3gateway.interaction;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.pojo.S3GatewayDTO;
import com.averygrimes.s3gateway.service.S3BucketOperationsService;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Component
@Path("/s3bucketOperations")
public class S3BucketResource {

    private S3BucketOperationsService s3BucketOperationsService;

    public S3BucketResource(S3BucketOperationsService s3BucketOperationsService){
        this.s3BucketOperationsService = s3BucketOperationsService;
    }

    @Path("/uploadAsset")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadAsset(S3GatewayDTO s3GatewayDTO){
        return s3BucketOperationsService.uploadAsset(s3GatewayDTO);
    }

    @Path("/getItemRequest")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemRequest(S3GatewayDTO s3GatewayDTO){
        return s3BucketOperationsService.fetchAsset(s3GatewayDTO);
    }
}
