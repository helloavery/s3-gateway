package com.averygrimes.s3gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Avery Grimes-Farrow
 * Created on: 9/2/19
 * https://github.com/helloavery
 */

@Component
@ConfigurationProperties
public class ConfigurationParams extends ProgramArguments {
}
