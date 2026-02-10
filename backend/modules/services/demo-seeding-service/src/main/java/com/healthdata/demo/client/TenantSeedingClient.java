package com.healthdata.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;

/**
 * Client for seeding demo tenants into gateway and FHIR databases.
 */
@Component
public class TenantSeedingClient {

    private static final Logger logger = LoggerFactory.getLogger(TenantSeedingClient.class);

    private final String gatewayDbUrl;
    private final String gatewayDbUser;
    private final String gatewayDbPassword;
    private final String fhirDbUrl;
    private final String fhirDbUser;
    private final String fhirDbPassword;

    public TenantSeedingClient(
            @Value("${demo.services.gateway-db.url:jdbc:postgresql://postgres:5432/gateway_db}") String gatewayDbUrl,
            @Value("${demo.services.gateway-db.username:healthdata}") String gatewayDbUser,
            @Value("${demo.services.gateway-db.password:healthdata_password}") String gatewayDbPassword,
            @Value("${demo.services.fhir-db.url:jdbc:postgresql://postgres:5432/fhir_db}") String fhirDbUrl,
            @Value("${demo.services.fhir-db.username:healthdata}") String fhirDbUser,
            @Value("${demo.services.fhir-db.password:healthdata_password}") String fhirDbPassword) {
        this.gatewayDbUrl = gatewayDbUrl;
        this.gatewayDbUser = gatewayDbUser;
        this.gatewayDbPassword = gatewayDbPassword;
        this.fhirDbUrl = fhirDbUrl;
        this.fhirDbUser = fhirDbUser;
        this.fhirDbPassword = fhirDbPassword;
    }

    public int seedTenants(List<TenantDefinition> tenants) {
        int created = 0;
        for (TenantDefinition tenant : tenants) {
            created += seedTenant(gatewayDbUrl, gatewayDbUser, gatewayDbPassword, tenant, "gateway");
            created += seedTenant(fhirDbUrl, fhirDbUser, fhirDbPassword, tenant, "fhir");
        }
        return created;
    }

    public boolean areDatabasesAvailable() {
        return isDatabaseAvailable(gatewayDbUrl, gatewayDbUser, gatewayDbPassword)
            && isDatabaseAvailable(fhirDbUrl, fhirDbUser, fhirDbPassword);
    }

    private int seedTenant(String dbUrl, String dbUser, String dbPassword, TenantDefinition tenant, String label) {
        try (Connection conn = getConnection(dbUrl, dbUser, dbPassword)) {
            if (!tableExists(conn, "tenants")) {
                logger.warn("Tenants table does not exist in {} database", label);
                return 0;
            }

            if (tenantExists(conn, tenant.id())) {
                return 0;
            }

            String insertSql = """
                INSERT INTO tenants (id, name, status, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
                """;
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, tenant.id());
                stmt.setString(2, tenant.name());
                stmt.setString(3, tenant.status());
                stmt.executeUpdate();
            }

            logger.info("Created tenant {} in {} database", tenant.id(), label);
            return 1;
        } catch (Exception e) {
            logger.warn("Failed to seed tenant {} in {} database: {}", tenant.id(), label, e.getMessage());
            return 0;
        }
    }

    private boolean tenantExists(Connection conn, String tenantId) throws Exception {
        String checkSql = "SELECT 1 FROM tenants WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Connection getConnection(String dbUrl, String dbUser, String dbPassword) throws Exception {
        Class.forName("org.postgresql.Driver");
        return java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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

    private boolean isDatabaseAvailable(String dbUrl, String dbUser, String dbPassword) {
        try (Connection conn = getConnection(dbUrl, dbUser, dbPassword)) {
            return conn.isValid(5);
        } catch (Exception e) {
            logger.warn("Database not available at {}: {}", dbUrl, e.getMessage());
            return false;
        }
    }

    public record TenantDefinition(String id, String name, String status) {
        public TenantDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(status, "status");
        }
    }
}
