package com.healthdata.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Security Configuration for HIPAA Compliance.
 * Registers HTTP interceptors to ensure PHI is not cached.
 *
 * ⚠️ CRITICAL SECURITY CONTROL - DO NOT DISABLE ⚠️
 *
 * This configuration is REQUIRED for HIPAA compliance with 45 CFR 164.312(a)(2)(i).
 * It prevents browser and proxy caching of Protected Health Information (PHI).
 *
 * Disabling or removing this configuration would:
 * - Violate HIPAA Technical Safeguards requirements
 * - Allow PHI to be cached in browsers on shared workstations
 * - Create serious security vulnerabilities and compliance violations
 *
 * For complete documentation, see: /backend/HIPAA-CACHE-COMPLIANCE.md
 *
 * @see NoCacheResponseInterceptor
 * @see <a href="https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html">HIPAA Security Rule</a>
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    private final NoCacheResponseInterceptor noCacheInterceptor;

    @Autowired
    public WebSecurityConfig(NoCacheResponseInterceptor noCacheInterceptor) {
        this.noCacheInterceptor = noCacheInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ⚠️ DO NOT REMOVE: HIPAA-required cache control for PHI endpoints
        // Apply no-cache headers to all patient data endpoints
        // See: /backend/HIPAA-CACHE-COMPLIANCE.md
        registry.addInterceptor(noCacheInterceptor)
                .addPathPatterns(
                    "/patient/**",           // Patient Service endpoints
                    "/fhir/Patient/**",      // FHIR Patient resources
                    "/fhir/Observation/**",  // FHIR Clinical observations
                    "/fhir/Condition/**",    // FHIR Conditions
                    "/fhir/Procedure/**",    // FHIR Procedures
                    "/fhir/Medication/**",   // FHIR Medications
                    "/fhir/AllergyIntolerance/**", // FHIR Allergies
                    "/fhir/Immunization/**", // FHIR Immunizations
                    "/fhir/Encounter/**",    // FHIR Encounters
                    "/quality-measure/**",   // Quality measure results
                    "/api/v1/cql/**",        // CQL evaluation results
                    "/care-gap/**"           // Care gap reports
                )
                .excludePathPatterns(
                    "/health/**",            // Health check endpoints (non-PHI)
                    "/actuator/**"           // Actuator endpoints (non-PHI)
                );
    }
}
