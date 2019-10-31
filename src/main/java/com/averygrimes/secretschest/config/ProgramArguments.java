package com.averygrimes.secretschest.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Avery Grimes-Farrow
 * Created on: 9/2/19
 * https://github.com/helloavery
 */

@Getter
@Setter
@NoArgsConstructor
public class ProgramArguments {

    private String AWSS3APIEndpoint;
    private String AWSAPIGatewayStage;
    private String AWSS3DataBucket;
    private String AWSS3KeyBucket;
}
