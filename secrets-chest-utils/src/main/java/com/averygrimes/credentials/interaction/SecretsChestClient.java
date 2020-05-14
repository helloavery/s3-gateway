package com.averygrimes.credentials.interaction;

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
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

@Named
@Path("/secretsChestBase")
public interface SecretsChestClient {

    @POST
    @Path("/uploadSecrets")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    Response uploadSecrets(byte[] dataToUpload);

    @PUT
    @Path("/updateSecrets/{secretsReference}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    Response updateSecrets(@PathParam("secretsReference") String secretsReference, byte[] data);

    @POST
    @Path("/retrieveSecrets/{secretsReference}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response retrieveSecrets(@PathParam("secretsReference") String secretReference);

}
