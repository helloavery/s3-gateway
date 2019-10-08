package com.averygrimes.s3gateway.interaction;

import com.averygrimes.s3gateway.pojo.S3GatewayDTO;
import com.averygrimes.s3gateway.service.CryptoService;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2019-01-04
 * https://github.com/helloavery
 */

@Component
@Path("/crypto")
public class CryptoResource {

    private CryptoService cryptoService;

    public CryptoResource(CryptoService cryptoService){
        this.cryptoService = cryptoService;
    }

    @Path("/generateSymmetricKey")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addItemRequest(byte[] publicKey){
        S3GatewayDTO symmetricKey  = cryptoService.generateSymmetricKey(publicKey);
        return Response.ok(symmetricKey).build();
    }
}
