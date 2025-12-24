package com.healthdata.authentication.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie Service for secure JWT token management.
 *
 * SECURITY FEATURES:
 * - HttpOnly: Prevents JavaScript access (XSS protection)
 * - Secure: Only sent over HTTPS in production
 * - SameSite=Strict: CSRF protection
 * - Path=/api: Only sent to API endpoints
 *
 * This implementation complies with HIPAA requirements by:
 * 1. Preventing token theft via XSS attacks
 * 2. Ensuring tokens are only transmitted over secure connections
 * 3. Protecting against CSRF attacks
 *
 * Usage:
 * - Login: Call setAccessTokenCookie() and setRefreshTokenCookie()
 * - Request: Call getAccessTokenFromCookie() in auth filter
 * - Logout: Call clearAuthCookies()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {

    public static final String ACCESS_TOKEN_COOKIE = "hdim_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "hdim_refresh_token";

    @Value("${authentication.cookie.secure:true}")
    private boolean secureCookies;

    @Value("${authentication.cookie.same-site:Strict}")
    private String sameSite;

    @Value("${authentication.cookie.path:/api}")
    private String cookiePath;

    @Value("${authentication.cookie.domain:#{null}}")
    private String cookieDomain;

    @Value("${jwt.accessTokenExpirationMs:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refreshTokenExpirationMs:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Set the access token cookie in the response.
     *
     * @param response HTTP response
     * @param token Access token value
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        int maxAge = (int) (accessTokenExpirationMs / 1000);
        setCookie(response, ACCESS_TOKEN_COOKIE, token, maxAge);
        log.debug("Set access token cookie (expires in {} seconds)", maxAge);
    }

    /**
     * Set the refresh token cookie in the response.
     *
     * @param response HTTP response
     * @param token Refresh token value
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        int maxAge = (int) (refreshTokenExpirationMs / 1000);
        // Refresh token has stricter path - only refresh endpoint
        setCookie(response, REFRESH_TOKEN_COOKIE, token, maxAge, "/api/v1/auth");
        log.debug("Set refresh token cookie (expires in {} seconds)", maxAge);
    }

    /**
     * Get the access token from the request cookie.
     *
     * @param request HTTP request
     * @return Access token if present
     */
    public Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * Get the refresh token from the request cookie.
     *
     * @param request HTTP request
     * @return Refresh token if present
     */
    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    /**
     * Clear all authentication cookies.
     *
     * @param response HTTP response
     */
    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE, cookiePath);
        clearCookie(response, REFRESH_TOKEN_COOKIE, "/api/v1/auth");
        log.debug("Cleared authentication cookies");
    }

    /**
     * Set a secure cookie with proper security attributes.
     */
    private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        setCookie(response, name, value, maxAge, cookiePath);
    }

    /**
     * Set a secure cookie with custom path.
     */
    private void setCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append(name).append("=").append(value);
        cookieBuilder.append("; Max-Age=").append(maxAge);
        cookieBuilder.append("; Path=").append(path);

        if (secureCookies) {
            cookieBuilder.append("; Secure");
        }

        cookieBuilder.append("; HttpOnly");
        cookieBuilder.append("; SameSite=").append(sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieBuilder.append("; Domain=").append(cookieDomain);
        }

        response.addHeader("Set-Cookie", cookieBuilder.toString());
    }

    /**
     * Clear a cookie by setting it with max-age 0.
     */
    private void clearCookie(HttpServletResponse response, String name, String path) {
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append(name).append("=");
        cookieBuilder.append("; Max-Age=0");
        cookieBuilder.append("; Path=").append(path);

        if (secureCookies) {
            cookieBuilder.append("; Secure");
        }

        cookieBuilder.append("; HttpOnly");
        cookieBuilder.append("; SameSite=").append(sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieBuilder.append("; Domain=").append(cookieDomain);
        }

        response.addHeader("Set-Cookie", cookieBuilder.toString());
    }

    /**
     * Get a cookie value from the request.
     */
    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> name.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }

    /**
     * Check if the request has a valid access token cookie.
     */
    public boolean hasAccessTokenCookie(HttpServletRequest request) {
        return getAccessTokenFromCookie(request).isPresent();
    }

    /**
     * Check if the request has a valid refresh token cookie.
     */
    public boolean hasRefreshTokenCookie(HttpServletRequest request) {
        return getRefreshTokenFromCookie(request).isPresent();
    }
}
