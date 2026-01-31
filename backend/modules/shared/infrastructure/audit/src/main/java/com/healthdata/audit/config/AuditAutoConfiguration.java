package com.healthdata.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.aspects.AuditAspect;
import com.healthdata.audit.mapper.AuditEventMapper;
import com.healthdata.audit.repository.shared.AuditEventRepository;
import com.healthdata.audit.service.AuditEncryptionService;
import com.healthdata.audit.service.AuditService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Auto-configuration for HIPAA audit module.
 *
 * Enable with property: audit.enabled=true
 */
@Configuration
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "com.healthdata.audit.repository")
@EntityScan(basePackages = "com.healthdata.audit.entity")
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    public AuditEncryptionService auditEncryptionService() {
        return new AuditEncryptionService(null);
    }

    @Bean
    public AuditEventMapper auditEventMapper(ObjectMapper objectMapper) {
        return new AuditEventMapper(objectMapper);
    }

    @Bean
    public AuditService auditService(
            ObjectMapper objectMapper,
            AuditEncryptionService encryptionService,
            AuditEventRepository repository,
            AuditEventMapper mapper) {
        return new AuditService(objectMapper, encryptionService, repository, mapper);
    }

    @Bean
    public AuditAspect auditAspect(AuditService auditService, ObjectMapper objectMapper) {
        return new AuditAspect(auditService, objectMapper);
    }
}
