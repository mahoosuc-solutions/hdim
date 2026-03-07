package com.healthdata.ihegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.healthdata.ihegateway", "com.healthdata.common"})
public class IheGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(IheGatewayApplication.class, args);
    }
}
