package com.healthdata.healthixadapter.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AuditConfig {

    @Bean
    public AtnaAuditService atnaAuditService(KafkaTemplate<String, Object> kafkaTemplate) {
        return new AtnaAuditService("healthix-adapter-service", "FULL", kafkaTemplate);
    }
}
