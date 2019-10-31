package com.averygrimes.secretschest.interaction;

import com.averygrimes.secretschest.service.SecretsChestBaseService;
import com.averygrimes.secretschest.utils.UUIDUtils;

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
        String requestId = UUIDUtils.generateUUID();
        return chestBaseService.uploadAsset(dataToUpload, requestId);
    }

    @PUT
    @Path("/updateSecrets/{secretsReference}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecrets(@PathParam("secretsReference") String secretsReference, byte[] data){
        String requestId = UUIDUtils.generateUUID();
        return chestBaseService.updateAsset(secretsReference, data, requestId);
    }

    @POST
    @Path("/retrieveSecrets/{secretsReference}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response retrieveSecrets(String secretReference){
        String requestId = UUIDUtils.generateUUID();
        return chestBaseService.retrieveAsset(secretReference, requestId);
    }

}
