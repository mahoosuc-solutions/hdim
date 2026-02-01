package com.healthdata.auditquery.config;

import com.healthdata.gateway.config.MethodSecurityConfiguration;
import com.healthdata.gateway.security.HdimPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method security configuration for audit query.
 * Disabled in demo profile to allow demo-mode access.
 */
@Configuration
@Profile("!demo")
@EnableMethodSecurity(prePostEnabled = true)
@Import(MethodSecurityConfiguration.class)
public class AuditQueryMethodSecurityConfig {

    @Bean
    public HdimPermissionEvaluator hdimPermissionEvaluator() {
        return new HdimPermissionEvaluator();
    }
}
