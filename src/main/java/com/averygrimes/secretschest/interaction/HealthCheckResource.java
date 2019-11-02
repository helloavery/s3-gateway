package com.averygrimes.secretschest.interaction;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@Named
@Path("/health")
public class HealthCheckResource {

    @GET
    public Response healthCheck(){
        return Response.ok().build();
    }

}
