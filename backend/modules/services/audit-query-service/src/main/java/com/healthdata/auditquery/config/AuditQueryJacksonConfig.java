package com.healthdata.auditquery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensure Java time types (Instant, OffsetDateTime) are supported in responses.
 */
@Configuration
public class AuditQueryJacksonConfig {

    @Bean
    public ObjectMapper auditQueryObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
