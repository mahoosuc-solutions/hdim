package com.healthdata.caregap.config;

import com.healthdata.gateway.security.HdimMethodSecurityExpressionHandler;
import com.healthdata.gateway.security.HdimPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method-level security configuration for Care Gap Service.
 *
 * Disabled in demo profile to allow demo-mode bypass of @PreAuthorize checks.
 */
@Configuration
@Profile("!demo & !test")
@EnableMethodSecurity(prePostEnabled = true)
public class CareGapMethodSecurityConfig {

    /**
     * Configure permission evaluator for @PreAuthorize hasPermission('PERM') checks.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            HdimPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new HdimMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    @Bean
    public HdimPermissionEvaluator hdimPermissionEvaluator() {
        return new HdimPermissionEvaluator();
    }
}
