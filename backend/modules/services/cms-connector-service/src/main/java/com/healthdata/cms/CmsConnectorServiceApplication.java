package com.healthdata.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CMS Connector Service
 * Main application entry point for Medicare Claim Data Integration
 */
@SpringBootApplication
@EnableScheduling
public class CmsConnectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmsConnectorServiceApplication.class, args);
    }
}
