package com.averygrimes.s3gateway.config;

import com.averygrimes.s3gateway.interaction.CryptoResource;
import com.averygrimes.s3gateway.interaction.S3BucketResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/3/19
 * https://github.com/helloavery
 */

@Component
@ApplicationPath("/rest/v1")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(){
        registerEndpoints();
    }

    private void registerEndpoints(){
        register(CryptoResource.class);
        register(S3BucketResource.class);
    }

}
