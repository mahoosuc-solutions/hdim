package com.healthdata.hedisadapter.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AuditConfig {

    @Bean
    public AtnaAuditService atnaAuditService(KafkaTemplate<String, Object> kafkaTemplate) {
        return new AtnaAuditService("hedis-adapter-service", "LIMITED", kafkaTemplate);
    }
}
