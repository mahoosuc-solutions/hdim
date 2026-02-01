package com.healthdata.fhir.config;

import com.healthdata.fhir.security.HdimMethodSecurityExpressionHandler;
import com.healthdata.gateway.security.HdimPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * Enable permission-based @PreAuthorize checks for the FHIR service.
 */
@Configuration
public class MethodSecurityConfig {

    @Bean
    public HdimPermissionEvaluator hdimPermissionEvaluator() {
        return new HdimPermissionEvaluator();
    }

    @Bean
    @Primary
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            HdimPermissionEvaluator permissionEvaluator) {
        HdimMethodSecurityExpressionHandler handler =
            new HdimMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
