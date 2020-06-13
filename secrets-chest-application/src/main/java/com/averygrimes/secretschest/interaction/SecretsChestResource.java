package com.averygrimes.secretschest.interaction;

import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import com.averygrimes.secretschest.pojo.SecretsChestResponse;
import com.averygrimes.secretschest.service.SecretsChestBaseService;
import com.averygrimes.secretschest.utils.ResponseBuilder;
import com.averygrimes.secretschest.utils.UUIDUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@Named
@Path("/secretsChestBase")
public class SecretsChestResource {

    private SecretsChestBaseService chestBaseService;

    @Inject
    public void setChestBaseService(SecretsChestBaseService chestBaseService) {
        this.chestBaseService = chestBaseService;
    }

    @POST
    @Path("/uploadSecrets")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSecrets(byte[] dataToUpload){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.uploadAsset(dataToUpload, requestId);
        return ResponseBuilder.createSuccessfulUploadDataResponse(secretsChestResponse);
    }

    @POST
    @Path("/uploadSecrets/format/{format}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSecrets(@PathParam("format") String format, String dataToUpload){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = null;
        if(StringUtils.equalsIgnoreCase(format, SecretsChestConstants.PLAIN_TEXT_DATA)){
            secretsChestResponse =  chestBaseService.uploadPlainTextAsset(dataToUpload, requestId);
        }else{
            secretsChestResponse = chestBaseService.uploadAsset(dataToUpload.getBytes(), requestId);
        }
        return ResponseBuilder.createSuccessfulUploadDataResponse(secretsChestResponse);
    }

    @PUT
    @Path("/updateSecrets/{secretsReference}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecrets(@PathParam("secretsReference") String secretsReference, byte[] data){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.updateAsset(secretsReference, data, requestId);
        return ResponseBuilder.createSuccessfulUploadDataResponse(secretsChestResponse);
    }

    @POST
    @Path("/retrieveSecrets/{secretsReference}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveSecrets(@PathParam("secretsReference") String secretReference){
        String requestId = UUIDUtils.generateRandomId();
        SecretsChestResponse secretsChestResponse = chestBaseService.retrieveAsset(secretReference, requestId);
        return ResponseBuilder.createSuccessfulRetrieveDataResponse(secretsChestResponse);
    }
}
