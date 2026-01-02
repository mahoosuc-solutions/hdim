package com.healthdata.persistence.tenant;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Aspect that prepares database connections with tenant context for RLS.
 *
 * This aspect intercepts repository method calls and sets the current tenant
 * in the PostgreSQL session before executing the query. This enables Row-Level
 * Security policies to enforce tenant isolation.
 *
 * The tenant ID is retrieved from TenantContext, which should be set by
 * a filter or interceptor at the beginning of each request.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TenantConnectionPreparer {

    private final DataSource dataSource;

    public TenantConnectionPreparer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Intercepts all JPA repository method calls to set tenant context.
     *
     * This advice runs around all methods in Spring Data repositories.
     * Before the method executes, it sets the current tenant in the database session.
     */
    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object setTenantForRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        setTenantInSession();
        return joinPoint.proceed();
    }

    /**
     * Intercepts all JdbcTemplate method calls to set tenant context.
     */
    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.*(..))")
    public Object setTenantForJdbc(ProceedingJoinPoint joinPoint) throws Throwable {
        setTenantInSession();
        return joinPoint.proceed();
    }

    private void setTenantInSession() {
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null && !TenantContext.isSystemMode()) {
            log.warn("No tenant context set for database operation - RLS will block access");
            return;
        }

        if (TenantContext.isSystemMode()) {
            // System mode - use bypass role
            executeSystemModeSetup();
        } else {
            // Normal mode - set tenant for RLS
            executeTenantSetup(tenantId);
        }
    }

    private void executeTenantSetup(String tenantId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT set_current_tenant(?)")) {
            stmt.setString(1, tenantId);
            stmt.execute();
            log.trace("Set tenant context to: {}", tenantId);
        } catch (SQLException e) {
            log.error("Failed to set tenant context: {}", e.getMessage());
            // Don't throw - let RLS handle the access denial
        }
    }

    private void executeSystemModeSetup() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SET ROLE hdim_system")) {
            stmt.execute();
            log.trace("Enabled system mode (RLS bypass)");
        } catch (SQLException e) {
            log.warn("Failed to set system role - RLS may block access: {}", e.getMessage());
        }
    }
}
