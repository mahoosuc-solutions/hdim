package com.healthdata.corehiveadapter.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Bean
    public AtnaAuditService atnaAuditService() {
        return new AtnaAuditService("corehive-adapter-service", "NONE");
    }
}
