package com.averygrimes.secretschest.interaction;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 9/2/19
 * https://github.com/helloavery
 */

@Named
public interface AWSClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{bucket}/{bucketObject}")
    Response getBucketObject(@HeaderParam("Host") String host, @HeaderParam("X-Amz-Date") String xAmzDate, @HeaderParam("Authorization") String authorization,
                             @PathParam("bucket") String bucket, @PathParam("bucketObject") String bucketObject);

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{bucket}/{bucketObject}")
    Response uploadBucketObject(@HeaderParam("Host") String host, @HeaderParam("X-Amz-Date") String xAmzDate, @HeaderParam("Authorization") String authorization,
                                @HeaderParam("Content-Length") String contentLength, @HeaderParam("cache-control") String cacheControl,
                                @PathParam("bucket") String bucket, @PathParam("bucketObject") String bucketObject, String contentToUpload);

}
