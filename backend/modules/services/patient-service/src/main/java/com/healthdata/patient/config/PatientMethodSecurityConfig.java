package com.healthdata.patient.config;

import com.healthdata.gateway.security.HdimMethodSecurityExpressionHandler;
import com.healthdata.gateway.security.HdimPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method-level security configuration for Patient Service.
 *
 * This configuration enables @PreAuthorize annotation processing with custom
 * permission evaluation. In demo/test profiles, method security is disabled
 * to allow easier testing and demonstration.
 *
 * SECURITY: In production, this configuration enables full RBAC permission
 * checks via HdimPermissionEvaluator, which validates that the current user's
 * roles grant the required permissions (e.g., PATIENT_READ, PATIENT_WRITE).
 */
@Configuration
@Profile("!demo & !test")
@EnableMethodSecurity(prePostEnabled = true)
public class PatientMethodSecurityConfig {

    /**
     * Configure permission evaluator for @PreAuthorize hasPermission('PERM') checks.
     *
     * This handler extends the default Spring Security expression handler to support
     * HDIM's custom hasPermission() expressions like:
     * - @PreAuthorize("hasPermission('PATIENT_READ')")
     * - @PreAuthorize("hasPermission('PATIENT_WRITE')")
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            HdimPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new HdimMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    /**
     * Create the HDIM permission evaluator bean.
     *
     * This evaluator checks user roles against the RolePermissions mapping
     * to determine if a user has the required permission.
     */
    @Bean
    public HdimPermissionEvaluator hdimPermissionEvaluator() {
        return new HdimPermissionEvaluator();
    }
}
