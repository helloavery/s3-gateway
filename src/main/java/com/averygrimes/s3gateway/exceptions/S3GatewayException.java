package com.averygrimes.s3gateway.exceptions;

import org.apache.http.HttpStatus;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/3/19
 * https://github.com/helloavery
 */

public class S3GatewayException extends WebApplicationException {

    private S3GatewayException(Response response){
        super(response);
    }

    public static S3GatewayException buildResponse(String message){
        Response response = generateResponse(message);
        return new S3GatewayException(response);
    }

    private static Response generateResponse(String message){
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.SC_BAD_REQUEST);
        errorResponse.put("message", message);
        return Response.status(HttpStatus.SC_BAD_REQUEST).entity(errorResponse).build();
    }
}
