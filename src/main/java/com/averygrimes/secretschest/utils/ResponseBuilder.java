package com.averygrimes.secretschest.utils;

import com.averygrimes.secretschest.pojo.SecretsChestResponse;

import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

public class ResponseBuilder {

    public static Response createSuccessfulUploadDataResponse(SecretsChestResponse secretsChestResponse){
        secretsChestResponse.setSuccessful(true);
        return Response.ok(secretsChestResponse).build();
    }

    public static Response createSuccessfulRetrieveDataResponse(SecretsChestResponse secretsChestResponse){
        secretsChestResponse.setSuccessful(true);
        return Response.ok(secretsChestResponse).build();
    }
}
