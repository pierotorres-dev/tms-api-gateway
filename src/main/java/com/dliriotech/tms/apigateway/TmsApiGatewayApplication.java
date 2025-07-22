package com.dliriotech.tms.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TmsApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(TmsApiGatewayApplication.class, args);
	}

}
