package com.averygrimes.secretschest.exceptions;

import org.apache.http.HttpStatus;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/29/19
 * https://github.com/helloavery
 */

public class SecretsChestServerError extends WebApplicationException {

    private SecretsChestServerError(Response response){
        super(response);
    }

    private static Response generateResponse(String message){
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        errorResponse.put("message", message);
        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(errorResponse).build();
    }
}
