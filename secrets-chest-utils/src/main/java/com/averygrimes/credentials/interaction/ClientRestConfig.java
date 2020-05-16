package com.averygrimes.credentials.interaction;

import com.averygrimes.servicediscovery.RestFeignClientBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

@Configuration("CredentialsClientRestConfig")
public class ClientRestConfig {

    private Environment environment;

    @Inject
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private static final String DISCOVERY_PROPERTY = "discovery.environment";
    private static final String SECRETS_CHEST_CLIENT_SERVICE = "secrets-chest-service";
    private static final String SECRETS_CHEST_CLIENT_VERSION = "1.0.0";

    @Bean
    @DependsOn({"createConsulClient"})
    public RestFeignClientBean<SecretsChestClient> createS3GatewayClient(){
        String instance = getS3GatewayEnvironment();
        return new RestFeignClientBean<>(SecretsChestClient.class, SECRETS_CHEST_CLIENT_SERVICE, SECRETS_CHEST_CLIENT_VERSION, instance);
    }

    private String getS3GatewayEnvironment(){
        if(environment.getProperty(DISCOVERY_PROPERTY).equalsIgnoreCase("QA")){
            return "QA";
        }
        else if(environment.getProperty(DISCOVERY_PROPERTY).equalsIgnoreCase("PROD")){
            return "PROD";
        }
        return null;
    }
}
