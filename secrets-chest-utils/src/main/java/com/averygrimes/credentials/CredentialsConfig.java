package com.averygrimes.credentials;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.averygrimes.credentials"})
public class CredentialsConfig {
}
