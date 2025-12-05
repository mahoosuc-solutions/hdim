package com.healthdata.audit.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Audit Module with live PostgreSQL database
 *
 * Tests end-to-end functionality of audit event persistence.
 * These tests require a running PostgreSQL instance at localhost:5435.
 * Tests are automatically skipped if the database is not available.
 */
@EnabledIf("isDatabaseAvailable")
public class AuditDatabaseIntegrationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:5435/healthdata_audit";
    private static final String DB_USER = "healthdata";
    private static final String DB_PASSWORD = "dev_password";

    /**
     * Check if the database is available for integration tests.
     * This method is called by @EnabledIf to determine if tests should run.
     */
    static boolean isDatabaseAvailable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return conn.isValid(2);
        } catch (Exception e) {
            System.out.println("Database not available for integration tests: " + e.getMessage());
            return false;
        }
    }

    @Test
    public void testInsertAndRetrieveAuditEvent() throws Exception {
        // Create test audit event
        AuditEvent event = AuditEvent.builder()
                .tenantId("test-tenant-1")
                .userId("user-123")
                .username("john.doe@example.com")
                .role("DOCTOR")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .action(AuditAction.READ)
                .resourceType("Patient")
                .resourceId("patient-456")
                .outcome(AuditOutcome.SUCCESS)
                .serviceName("fhir-service")
                .methodName("getPatientById")
                .requestPath("/fhir/Patient/patient-456")
                .purposeOfUse("TREATMENT")
                .durationMs(150L)
                .encrypted(false)
                .build();

        // Insert into database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertSql = "INSERT INTO audit_events " +
                    "(id, timestamp, tenant_id, user_id, username, role, ip_address, user_agent, " +
                    "action, resource_type, resource_id, outcome, service_name, method_name, " +
                    "request_path, purpose_of_use, duration_ms, encrypted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setObject(1, event.getId());
                pstmt.setTimestamp(2, Timestamp.from(event.getTimestamp()));
                pstmt.setString(3, event.getTenantId());
                pstmt.setString(4, event.getUserId());
                pstmt.setString(5, event.getUsername());
                pstmt.setString(6, event.getRole());
                pstmt.setString(7, event.getIpAddress());
                pstmt.setString(8, event.getUserAgent());
                pstmt.setString(9, event.getAction().name());
                pstmt.setString(10, event.getResourceType());
                pstmt.setString(11, event.getResourceId());
                pstmt.setString(12, event.getOutcome().name());
                pstmt.setString(13, event.getServiceName());
                pstmt.setString(14, event.getMethodName());
                pstmt.setString(15, event.getRequestPath());
                pstmt.setString(16, event.getPurposeOfUse());
                pstmt.setLong(17, event.getDurationMs());
                pstmt.setBoolean(18, event.isEncrypted());

                int rowsInserted = pstmt.executeUpdate();
                assertEquals(1, rowsInserted, "Should insert exactly 1 row");
            }

            // Retrieve and verify
            String selectSql = "SELECT * FROM audit_events WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setObject(1, event.getId());

                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next(), "Should find the inserted audit event");

                    assertEquals(event.getTenantId(), rs.getString("tenant_id"));
                    assertEquals(event.getUserId(), rs.getString("user_id"));
                    assertEquals(event.getUsername(), rs.getString("username"));
                    assertEquals(event.getRole(), rs.getString("role"));
                    assertEquals(event.getAction().name(), rs.getString("action"));
                    assertEquals(event.getResourceType(), rs.getString("resource_type"));
                    assertEquals(event.getResourceId(), rs.getString("resource_id"));
                    assertEquals(event.getOutcome().name(), rs.getString("outcome"));
                    assertEquals(event.getDurationMs(), rs.getLong("duration_ms"));

                    assertFalse(rs.next(), "Should only have one result");
                }
            }

            // Clean up test data
            String deleteSql = "DELETE FROM audit_events WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setObject(1, event.getId());
                pstmt.executeUpdate();
            }
        }
    }

    @Test
    public void testQueryAuditEventsByTenant() throws Exception {
        String testTenantId = "tenant-test-query";
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Insert test events
            String insertSql = "INSERT INTO audit_events " +
                    "(id, timestamp, tenant_id, user_id, action, resource_type, resource_id, outcome, encrypted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                // Event 1
                pstmt.setObject(1, eventId1);
                pstmt.setTimestamp(2, Timestamp.from(Instant.now()));
                pstmt.setString(3, testTenantId);
                pstmt.setString(4, "user-1");
                pstmt.setString(5, AuditAction.CREATE.name());
                pstmt.setString(6, "Patient");
                pstmt.setString(7, "patient-1");
                pstmt.setString(8, AuditOutcome.SUCCESS.name());
                pstmt.setBoolean(9, false);
                pstmt.executeUpdate();

                // Event 2
                pstmt.setObject(1, eventId2);
                pstmt.setTimestamp(2, Timestamp.from(Instant.now()));
                pstmt.setString(3, testTenantId);
                pstmt.setString(4, "user-2");
                pstmt.setString(5, AuditAction.UPDATE.name());
                pstmt.setString(6, "Patient");
                pstmt.setString(7, "patient-2");
                pstmt.setString(8, AuditOutcome.SUCCESS.name());
                pstmt.setBoolean(9, false);
                pstmt.executeUpdate();
            }

            // Query by tenant
            String selectSql = "SELECT COUNT(*) as count FROM audit_events WHERE tenant_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, testTenantId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt("count"), "Should find 2 audit events for tenant");
                }
            }

            // Clean up
            String deleteSql = "DELETE FROM audit_events WHERE tenant_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, testTenantId);
                int deleted = pstmt.executeUpdate();
                assertEquals(2, deleted, "Should delete 2 test events");
            }
        }
    }

    @Test
    public void testIndexPerformance() throws Exception {
        String testTenantId = "tenant-perf-test";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Insert test events
            String insertSql = "INSERT INTO audit_events " +
                    "(id, timestamp, tenant_id, user_id, action, resource_type, resource_id, outcome, encrypted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < 100; i++) {
                    pstmt.setObject(1, UUID.randomUUID());
                    pstmt.setTimestamp(2, Timestamp.from(Instant.now().minusSeconds(i)));
                    pstmt.setString(3, testTenantId);
                    pstmt.setString(4, "user-" + i);
                    pstmt.setString(5, AuditAction.READ.name());
                    pstmt.setString(6, "Patient");
                    pstmt.setString(7, "patient-" + i);
                    pstmt.setString(8, AuditOutcome.SUCCESS.name());
                    pstmt.setBoolean(9, false);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // Test indexed query performance
            String selectSql = "SELECT * FROM audit_events WHERE tenant_id = ? ORDER BY timestamp DESC LIMIT 10";

            long startTime = System.currentTimeMillis();
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, testTenantId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                    }
                    assertEquals(10, count, "Should retrieve 10 most recent events");
                }
            }
            long duration = System.currentTimeMillis() - startTime;

            // Query should be fast with proper indexes (< 100ms for this small dataset)
            assertTrue(duration < 100, "Query should complete quickly with indexes: " + duration + "ms");

            // Clean up
            String deleteSql = "DELETE FROM audit_events WHERE tenant_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, testTenantId);
                pstmt.executeUpdate();
            }
        }
    }

    @Test
    public void testDatabaseConnection() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            assertTrue(conn.isValid(5), "Database connection should be valid");

            // Verify table exists
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_name = 'audit_events' AND table_schema = 'public'";

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "audit_events table should exist");
            }
        }
    }
}
