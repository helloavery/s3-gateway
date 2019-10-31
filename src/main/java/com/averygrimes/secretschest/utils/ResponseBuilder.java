package com.averygrimes.secretschest.utils;

import com.averygrimes.secretschest.pojo.RetrieveDataResponse;
import com.averygrimes.secretschest.pojo.UploadDataResponse;

import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

public class ResponseBuilder {

    public static Response createSuccessfulUploadDataResponse(String secretReference){
        UploadDataResponse uploadDataResponse = new UploadDataResponse();
        uploadDataResponse.setSuccessful(true);
        uploadDataResponse.setSecretReference(secretReference);
        return Response.ok(uploadDataResponse).build();
    }

    public static Response createFailureUploadDataResponse(String error, int status){
        UploadDataResponse uploadDataResponse = new UploadDataResponse();
        uploadDataResponse.setSuccessful(false);
        uploadDataResponse.setError(error);
        return Response.status(status).entity(uploadDataResponse).build();
    }

    public static Response createSuccessfulRetrieveDataResponse(byte[] dataToReturn){
        RetrieveDataResponse retrieveDataResponse = new RetrieveDataResponse();
        retrieveDataResponse.setSuccessful(true);
        retrieveDataResponse.setData(dataToReturn);
        return Response.ok(retrieveDataResponse).build();
    }

    public static Response createFailureRetrieveDataResponse(String error, int status){
        RetrieveDataResponse retrieveDataResponse = new RetrieveDataResponse();
        retrieveDataResponse.setSuccessful(false);
        retrieveDataResponse.setError(error);
        return Response.status(status).entity(retrieveDataResponse).build();
    }
}
