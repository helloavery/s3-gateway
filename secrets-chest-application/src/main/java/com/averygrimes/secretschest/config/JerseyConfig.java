package com.averygrimes.secretschest.config;

import com.averygrimes.secretschest.interaction.HealthCheckResource;
import com.averygrimes.secretschest.interaction.SecretsChestResource;
import com.averygrimes.servicediscovery.registration.ServiceDiscoveryRegister;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Profile;

import javax.inject.Named;
import javax.ws.rs.ApplicationPath;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

@Named
@ApplicationPath("/rest/v1")
@ServiceDiscoveryRegister(service = "secrets-chest-service", version = "1.0.0", healthCheckPath = "/health")
@Profile("!test")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(){
        registerEndpoints();;
    }

    private void registerEndpoints(){
        register(HealthCheckResource.class);
        register(SecretsChestResource.class);
    }
}
