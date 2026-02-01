package com.healthdata.gateway.config;

import com.healthdata.gateway.security.HdimMethodSecurityExpressionHandler;
import com.healthdata.gateway.security.HdimPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * Method Security Configuration for Permission-Based Authorization.
 *
 * Configures Spring Security's method-level authorization with HDIM's custom
 * PermissionEvaluator to enable permission-based @PreAuthorize expressions.
 *
 * Before (Role-Based):
 * <pre>
 * @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
 * public PatientResponse getPatient(String patientId) { ... }
 * </pre>
 *
 * After (Permission-Based):
 * <pre>
 * @PreAuthorize("hasPermission('PATIENT_READ')")
 * public PatientResponse getPatient(String patientId) { ... }
 * </pre>
 *
 * Permission Evaluation Flow:
 * 1. Spring Security intercepts @PreAuthorize-annotated methods
 * 2. MethodSecurityExpressionHandler parses the SpEL expression
 * 3. HdimPermissionEvaluator.hasPermission() is called
 * 4. User roles are extracted from Spring Security Authentication
 * 5. RolePermissions utility checks if any role grants the permission
 * 6. true = method executes, false = AccessDeniedException (403)
 *
 * HIPAA Compliance:
 * - All permission checks are audit logged in HdimPermissionEvaluator
 * - PHI permissions trigger enhanced logging (HIPAA §164.312(b))
 * - Denials are logged for security incident investigation
 *
 * Multi-Tenant Support:
 * - PermissionEvaluator validates permission grants only
 * - Tenant-level access control handled by TrustedTenantAccessFilter
 * - This configuration enables global permission enforcement
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MethodSecurityConfiguration {

    private final HdimPermissionEvaluator permissionEvaluator;

    /**
     * Configure MethodSecurityExpressionHandler with custom PermissionEvaluator.
     *
     * This bean is automatically detected by @EnableMethodSecurity and used
     * for all @PreAuthorize, @PostAuthorize, @Secured annotations.
     *
     * @return Configured MethodSecurityExpressionHandler
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler =
            new HdimMethodSecurityExpressionHandler();

        // Register custom PermissionEvaluator
        expressionHandler.setPermissionEvaluator(permissionEvaluator);

        log.info("Method security configured with HdimPermissionEvaluator");

        return expressionHandler;
    }
}
