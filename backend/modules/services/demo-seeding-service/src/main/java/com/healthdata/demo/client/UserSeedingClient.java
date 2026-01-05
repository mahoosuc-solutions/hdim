package com.healthdata.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Client for seeding demo users directly to the gateway database.
 *
 * Creates test users with predefined roles for demo authentication.
 */
@Component
public class UserSeedingClient {

    private static final Logger logger = LoggerFactory.getLogger(UserSeedingClient.class);

    private final String gatewayDbUrl;
    private final String gatewayDbUser;
    private final String gatewayDbPassword;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Default password for all demo users
    private static final String DEFAULT_PASSWORD = "demo123";

    public UserSeedingClient(
            @Value("${demo.services.gateway-db.url:jdbc:postgresql://postgres:5432/gateway_db}") String gatewayDbUrl,
            @Value("${demo.services.gateway-db.username:healthdata}") String gatewayDbUser,
            @Value("${demo.services.gateway-db.password:healthdata_password}") String gatewayDbPassword) {
        this.gatewayDbUrl = gatewayDbUrl;
        this.gatewayDbUser = gatewayDbUser;
        this.gatewayDbPassword = gatewayDbPassword;
    }

    /**
     * Seed demo users to the gateway database.
     *
     * @param tenantId The tenant ID for the users
     * @return Number of users created
     */
    public int seedDemoUsers(String tenantId) {
        logger.info("Seeding demo users for tenant: {}", tenantId);

        int usersCreated = 0;

        try (Connection conn = getConnection()) {
            // Check if users table exists
            if (!tableExists(conn, "users")) {
                logger.warn("Users table does not exist in gateway database");
                return 0;
            }

            // Seed each test user
            usersCreated += seedUser(conn, "test_superadmin", "superadmin@healthdata.demo",
                "Super", "Admin", tenantId, "SUPER_ADMIN");
            usersCreated += seedUser(conn, "test_admin", "admin@healthdata.demo",
                "Test", "Admin", tenantId, "ADMIN");
            usersCreated += seedUser(conn, "test_evaluator", "evaluator@healthdata.demo",
                "Test", "Evaluator", tenantId, "EVALUATOR");
            usersCreated += seedUser(conn, "test_analyst", "analyst@healthdata.demo",
                "Test", "Analyst", tenantId, "ANALYST");
            usersCreated += seedUser(conn, "test_viewer", "viewer@healthdata.demo",
                "Test", "Viewer", tenantId, "VIEWER");
            usersCreated += seedUser(conn, "demo-user", "demo@healthdata.demo",
                "Demo", "User", tenantId, "ADMIN");

            logger.info("Demo users seeded: {} users created", usersCreated);

        } catch (Exception e) {
            logger.error("Failed to seed demo users: {}", e.getMessage(), e);
        }

        return usersCreated;
    }

    private int seedUser(Connection conn, String username, String email,
            String firstName, String lastName, String tenantId, String role) {

        try {
            // Check if user already exists
            String checkSql = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        logger.debug("User {} already exists", username);
                        // Still ensure tenant and role assignments
                        UUID userId = (UUID) rs.getObject("id");
                        ensureUserTenant(conn, userId, tenantId);
                        ensureUserRole(conn, userId, role);
                        return 0;
                    }
                }
            }

            // Create user
            UUID userId = UUID.randomUUID();
            String insertSql = """
                INSERT INTO users (id, username, email, password_hash, first_name, last_name,
                                   active, email_verified, mfa_enabled, failed_login_attempts,
                                   created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, true, true, false, 0, NOW(), NOW())
                """;

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setObject(1, userId);
                insertStmt.setString(2, username);
                insertStmt.setString(3, email);
                insertStmt.setString(4, passwordEncoder.encode(DEFAULT_PASSWORD));
                insertStmt.setString(5, firstName);
                insertStmt.setString(6, lastName);
                insertStmt.executeUpdate();
            }

            // Assign tenant
            ensureUserTenant(conn, userId, tenantId);

            // Assign role
            ensureUserRole(conn, userId, role);

            logger.info("Created demo user: {} ({})", username, role);
            return 1;

        } catch (Exception e) {
            logger.warn("Failed to seed user {}: {}", username, e.getMessage());
            return 0;
        }
    }

    private void ensureUserTenant(Connection conn, UUID userId, String tenantId) {
        try {
            // Check if already assigned
            String checkSql = "SELECT 1 FROM user_tenants WHERE user_id = ? AND tenant_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setObject(1, userId);
                checkStmt.setString(2, tenantId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return; // Already assigned
                    }
                }
            }

            // Assign tenant
            String insertSql = "INSERT INTO user_tenants (user_id, tenant_id) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setObject(1, userId);
                insertStmt.setString(2, tenantId);
                insertStmt.executeUpdate();
            }

        } catch (Exception e) {
            logger.debug("Could not assign tenant {} to user: {}", tenantId, e.getMessage());
        }
    }

    private void ensureUserRole(Connection conn, UUID userId, String role) {
        try {
            // Check if already assigned
            String checkSql = "SELECT 1 FROM user_roles WHERE user_id = ? AND role = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setObject(1, userId);
                checkStmt.setString(2, role);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return; // Already assigned
                    }
                }
            }

            // Assign role
            String insertSql = "INSERT INTO user_roles (user_id, role) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setObject(1, userId);
                insertStmt.setString(2, role);
                insertStmt.executeUpdate();
            }

        } catch (Exception e) {
            logger.debug("Could not assign role {} to user: {}", role, e.getMessage());
        }
    }

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return java.sql.DriverManager.getConnection(gatewayDbUrl, gatewayDbUser, gatewayDbPassword);
    }

    private boolean tableExists(Connection conn, String tableName) {
        try {
            String sql = "SELECT 1 FROM information_schema.tables WHERE table_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the gateway database is available.
     */
    public boolean isDatabaseAvailable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            logger.warn("Gateway database not available: {}", e.getMessage());
            return false;
        }
    }
}
