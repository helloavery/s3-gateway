package com.averygrimes.s3gateway.config;

import com.averygrimes.s3gateway.interaction.client.AWSClient;
import com.averygrimes.servicediscovery.EnableServiceDiscovery;
import com.averygrimes.servicediscovery.SimpleFeignClientBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/3/19
 * https://github.com/helloavery
 */

@Configuration
@EnableServiceDiscovery
public class ClientRestConfiguration {

    private ProgramArguments programArguments;

    @Inject
    public void setProgramArguments(ProgramArguments programArguments) {
        this.programArguments = programArguments;
    }

    @Bean
    public SimpleFeignClientBean<AWSClient> createAWSClient(){
        return new SimpleFeignClientBean<>(AWSClient.class,
                programArguments.getAWSS3APIEndpoint() + programArguments.getAWSAPIGatewayStage());
    }

}
