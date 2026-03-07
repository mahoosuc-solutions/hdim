package com.healthdata.healthixadapter.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Bean
    public AtnaAuditService atnaAuditService() {
        return new AtnaAuditService("healthix-adapter-service", "FULL");
    }
}
