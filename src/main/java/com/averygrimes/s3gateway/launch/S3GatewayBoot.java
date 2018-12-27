package com.averygrimes.s3gateway.launch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.averygrimes.s3gateway")
public class S3GatewayBoot {

	public static void main(String[] args) {
		SpringApplication.run(S3GatewayBoot.class, args);
	}

}

