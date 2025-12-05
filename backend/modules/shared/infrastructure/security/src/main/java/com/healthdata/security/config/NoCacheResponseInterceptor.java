package com.healthdata.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP Interceptor to add cache-control headers for HIPAA compliance.
 * Prevents browser and proxy caching of Protected Health Information (PHI).
 *
 * ⚠️ CRITICAL SECURITY CONTROL - DO NOT DISABLE ⚠️
 *
 * Applied to all endpoints containing patient data, quality measures, and clinical information.
 *
 * HIPAA Compliance: 45 CFR 164.312(a)(2)(i) - Access Controls
 * Ensures PHI is not persisted in browser cache or intermediate proxies.
 *
 * Headers Added:
 * - Cache-Control: no-store, no-cache, must-revalidate, private
 * - Pragma: no-cache (HTTP/1.0 compatibility)
 * - Expires: 0 (immediate expiration)
 *
 * For complete documentation, see: /backend/HIPAA-CACHE-COMPLIANCE.md
 *
 * @see WebSecurityConfig
 */
@Component
public class NoCacheResponseInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Add cache-control headers to prevent caching of PHI
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        return true;
    }
}
