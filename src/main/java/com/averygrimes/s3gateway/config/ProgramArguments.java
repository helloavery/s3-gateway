package com.averygrimes.s3gateway.config;

/**
 * @author Avery Grimes-Farrow
 * Created on: 9/2/19
 * https://github.com/helloavery
 */

public class ProgramArguments {

    private String AWSS3APIEndpoint;
    private String AWSAPIGatewayStage;

    public String getAWSS3APIEndpoint() {
        return AWSS3APIEndpoint;
    }

    public void setAWSS3APIEndpoint(String AWSS3APIEndpoint) {
        this.AWSS3APIEndpoint = AWSS3APIEndpoint;
    }

    public String getAWSAPIGatewayStage() {
        return AWSAPIGatewayStage;
    }

    public void setAWSAPIGatewayStage(String AWSAPIGatewayStage) {
        this.AWSAPIGatewayStage = AWSAPIGatewayStage;
    }
}
