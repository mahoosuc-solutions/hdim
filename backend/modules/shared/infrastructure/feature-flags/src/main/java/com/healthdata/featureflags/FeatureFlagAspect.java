package com.healthdata.featureflags;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Feature Flag AOP Aspect
 *
 * Intercepts methods annotated with @FeatureFlag and checks if the feature
 * is enabled for the tenant before allowing execution.
 *
 * Tenant Extraction Strategy:
 * 1. Check method parameters for @RequestHeader("X-Tenant-ID")
 * 2. Fall back to SecurityContextHolder for authenticated tenant
 * 3. If no tenant found, throw IllegalStateException
 *
 * If feature is disabled:
 * - failSilently = false: throw FeatureFlagDisabledException (default)
 * - failSilently = true: return null and log warning
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagAspect {

    private final TenantFeatureFlagService featureFlagService;

    @Around("@annotation(featureFlag)")
    public Object checkFeatureFlag(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) throws Throwable {
        String featureKey = featureFlag.value();

        // Extract tenant ID from method parameters or security context
        String tenantId = extractTenantId(joinPoint);

        if (tenantId == null || tenantId.isBlank()) {
            log.error("Cannot check feature flag '{}': tenant ID not found in method parameters or security context",
                    featureKey);
            throw new IllegalStateException("Tenant ID is required for feature flag check");
        }

        // Check if feature is enabled
        boolean enabled = featureFlagService.isFeatureEnabled(tenantId, featureKey);

        if (!enabled) {
            log.warn("Feature '{}' is disabled for tenant '{}' - method: {}",
                    featureKey, tenantId, joinPoint.getSignature().toShortString());

            if (featureFlag.failSilently()) {
                log.debug("Feature flag disabled but failSilently=true, returning null");
                return null;
            } else {
                throw new FeatureFlagDisabledException(tenantId, featureKey);
            }
        }

        log.debug("Feature '{}' is enabled for tenant '{}', proceeding with method execution",
                featureKey, tenantId);

        return joinPoint.proceed();
    }

    /**
     * Extract tenant ID from method parameters or security context
     *
     * Priority:
     * 1. @RequestHeader("X-Tenant-ID") parameter
     * 2. SecurityContextHolder (TenantAuthenticationToken)
     *
     * @param joinPoint Method invocation context
     * @return Tenant ID or null if not found
     */
    private String extractTenantId(ProceedingJoinPoint joinPoint) {
        // Strategy 1: Extract from @RequestHeader("X-Tenant-ID") parameter
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);

            if (requestHeader != null && "X-Tenant-ID".equals(requestHeader.value())) {
                Object arg = args[i];
                if (arg instanceof String) {
                    return (String) arg;
                }
            }
        }

        // Strategy 2: Extract from SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();

            // Check if principal has getTenantId() method (duck typing)
            try {
                Method getTenantIdMethod = principal.getClass().getMethod("getTenantId");
                Object tenantId = getTenantIdMethod.invoke(principal);
                if (tenantId instanceof String) {
                    return (String) tenantId;
                }
            } catch (Exception e) {
                log.debug("Principal does not have getTenantId() method: {}", principal.getClass().getName());
            }
        }

        return null;
    }
}
