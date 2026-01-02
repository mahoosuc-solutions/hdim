package com.healthdata.sales;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.authentication.config.AuthenticationControllerAutoConfiguration;
import com.healthdata.authentication.config.AuthenticationJwtAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sales Automation Service
 *
 * Provides CRM functionality for HDIM including:
 * - Lead capture and management
 * - Contact and account management
 * - Opportunity pipeline tracking
 * - Email sequence automation
 * - Zoho CRM integration
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.sales"
    },
    exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        AuthenticationAutoConfiguration.class,
        AuthenticationJwtAutoConfiguration.class,
        AuthenticationControllerAutoConfiguration.class
    }
)
@EntityScan(basePackages = {"com.healthdata.sales.entity", "com.healthdata.sales.audit"})
@EnableJpaRepositories(basePackages = {"com.healthdata.sales.repository", "com.healthdata.sales.audit"})
@EnableFeignClients
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class SalesAutomationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesAutomationServiceApplication.class, args);
    }
}
