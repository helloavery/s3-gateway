package com.averygrimes.secretschest.interaction;

import com.google.gson.Gson;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@Named
@Path("/health")
public class HealthCheckResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck(){
        Map<String, String> healthMap = new HashMap<>();
        Gson gson = new Gson();
        healthMap.put("Secrets Chest Service", gson.toJson(Calendar.getInstance()));
        return Response.ok(healthMap).build();
    }
}
