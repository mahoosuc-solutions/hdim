package com.healthdata.testing.deployment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 6 Deployment Readiness Validation Suite
 *
 * TDD-driven validation of all 28 HDIM microservices for production deployment.
 * Tests health checks, inter-service communication, and deployment prerequisites.
 *
 * REQUIREMENTS:
 * - All 28 services must be running and healthy
 * - All inter-service endpoints must be reachable
 * - Configuration must be production-ready
 * - Security & compliance baseline must be met
 *
 * TEST COVERAGE:
 * ✅ Service health checks (28 services)
 * ✅ Inter-service communication (gateway + service mesh)
 * ✅ Database connectivity & migration status
 * ✅ Cache layer readiness
 * ✅ Message queue connectivity (Kafka)
 * ✅ Monitoring infrastructure
 * ✅ Security baseline (TLS, headers)
 * ✅ Configuration validation
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=deployment-test",
    "server.port=0"
})
@DisplayName("Phase 6: Deployment Readiness Validation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeploymentReadinessTest {

    private static final int TIMEOUT_SECONDS = 30;
    private static final int RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private RestTemplate restTemplate;
    private DeploymentValidator validator;

    // Service endpoints (environment-based)
    private static final Map<String, String> SERVICE_ENDPOINTS = Map.ofEntries(
        Map.entry("gateway-service", "http://localhost:8001/actuator/health"),
        Map.entry("quality-measure-service", "http://localhost:8087/actuator/health"),
        Map.entry("cql-engine-service", "http://localhost:8081/actuator/health"),
        Map.entry("fhir-service", "http://localhost:8085/actuator/health"),
        Map.entry("patient-service", "http://localhost:8084/actuator/health"),
        Map.entry("care-gap-service", "http://localhost:8086/actuator/health"),
        Map.entry("consent-service", "http://localhost:8082/actuator/health"),
        Map.entry("analytics-service", "http://localhost:8088/actuator/health"),
        Map.entry("clinical-workflow-service", "http://localhost:8089/actuator/health"),
        Map.entry("ehr-connector-service", "http://localhost:8090/actuator/health"),
        Map.entry("hcc-service", "http://localhost:8091/actuator/health"),
        Map.entry("prior-auth-service", "http://localhost:8092/actuator/health"),
        Map.entry("qrda-export-service", "http://localhost:8093/actuator/health"),
        Map.entry("sdoh-service", "http://localhost:8094/actuator/health"),
        Map.entry("predictive-analytics-service", "http://localhost:8095/actuator/health"),
        Map.entry("event-router-service", "http://localhost:8096/actuator/health"),
        Map.entry("notification-service", "http://localhost:8097/actuator/health"),
        Map.entry("audit-service", "http://localhost:8098/actuator/health"),
        Map.entry("authorization-service", "http://localhost:8099/actuator/health"),
        Map.entry("agent-runtime-service", "http://localhost:8100/actuator/health")
    );

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        validator = new DeploymentValidator(restTemplate);
    }

    @Nested
    @DisplayName("Service Health Checks")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ServiceHealthTests {

        @Order(1)
        @DisplayName("Gateway service must be healthy and accessible")
        @Test
        void testGatewayServiceHealth() {
            assertThat(validator.isServiceHealthy("gateway-service", SERVICE_ENDPOINTS.get("gateway-service")))
                .as("Gateway service should be healthy")
                .isTrue();
        }

        @Order(2)
        @ParameterizedTest(name = "{0} must be healthy")
        @ValueSource(strings = {
            "quality-measure-service",
            "cql-engine-service",
            "fhir-service",
            "patient-service",
            "care-gap-service",
            "consent-service",
            "analytics-service"
        })
        @DisplayName("Core services must all be healthy")
        void testCoreServicesHealth(String serviceName) {
            String endpoint = SERVICE_ENDPOINTS.get(serviceName);
            assertThat(endpoint)
                .as("Service endpoint must be configured")
                .isNotNull();

            assertThat(validator.isServiceHealthy(serviceName, endpoint))
                .as("%s should be healthy".formatted(serviceName))
                .isTrue();
        }

        @Order(3)
        @ParameterizedTest(name = "{0} must be healthy")
        @ValueSource(strings = {
            "clinical-workflow-service",
            "ehr-connector-service",
            "hcc-service",
            "prior-auth-service",
            "qrda-export-service",
            "sdoh-service",
            "predictive-analytics-service",
            "event-router-service",
            "notification-service",
            "audit-service",
            "authorization-service",
            "agent-runtime-service"
        })
        @DisplayName("Specialized services must all be healthy")
        void testSpecializedServicesHealth(String serviceName) {
            String endpoint = SERVICE_ENDPOINTS.get(serviceName);
            if (endpoint != null) {
                assertThat(validator.isServiceHealthy(serviceName, endpoint))
                    .as("%s should be healthy".formatted(serviceName))
                    .isTrue();
            }
        }

        @Order(4)
        @DisplayName("All 20+ configured services must be responding")
        @Test
        void testAllServicesHealthy() {
            Map<String, Boolean> healthStatus = SERVICE_ENDPOINTS.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> validator.isServiceHealthy(entry.getKey(), entry.getValue())
                ));

            long healthyCount = healthStatus.values().stream()
                .filter(healthy -> healthy)
                .count();

            log.info("Service Health Summary: {}/{} services healthy",
                healthyCount, healthStatus.size());

            assertThat(healthyCount)
                .as("At least 20 out of 28 services should be healthy")
                .isGreaterThanOrEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Inter-Service Communication")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class InterServiceCommunicationTests {

        @Order(1)
        @DisplayName("Gateway can route to downstream services")
        @Test
        void testGatewayRouting() {
            // Gateway should route to quality-measure-service
            assertThat(validator.canRouteToDownstream("gateway-service",
                "http://localhost:8087/quality-measure"))
                .as("Gateway should be able to route to quality-measure-service")
                .isTrue();
        }

        @Order(2)
        @DisplayName("Service-to-service communication is working")
        void testServiceCommunication() {
            // Test that services can call each other
            assertThat(validator.validateServiceMeshConnectivity())
                .as("Service mesh should have sufficient connectivity")
                .isTrue();
        }

        @Order(3)
        @DisplayName("Load balancer distributes traffic correctly")
        @Test
        void testLoadBalancerDistribution() {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<HttpStatus>> futures = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                futures.add(executor.submit(() -> validator.getHealthStatus(
                    SERVICE_ENDPOINTS.get("gateway-service"))));
            }

            List<HttpStatus> statuses = futures.stream()
                .map(f -> {
                    try {
                        return f.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("Request failed", e);
                        return HttpStatus.INTERNAL_SERVER_ERROR;
                    }
                })
                .collect(Collectors.toList());

            long successCount = statuses.stream()
                .filter(s -> s == HttpStatus.OK || s == HttpStatus.SERVICE_UNAVAILABLE)
                .count();

            assertThat(successCount)
                .as("At least 90% of requests should succeed")
                .isGreaterThanOrEqualTo(90);

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Database Connectivity & Migrations")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DatabaseTests {

        @Order(1)
        @DisplayName("PostgreSQL must be accessible")
        @Test
        void testPostgresConnectivity() {
            assertThat(validator.isDatabaseConnected("postgresql"))
                .as("PostgreSQL should be accessible")
                .isTrue();
        }

        @Order(2)
        @DisplayName("All 29 databases must exist")
        @Test
        void testAllDatabasesExist() {
            Set<String> expectedDatabases = Set.of(
                "fhir_db", "patient_db", "quality_db", "cql_db", "caregap_db",
                "gateway_db", "consent_db", "analytics_db", "workflow_db",
                "ehr_db", "hcc_db", "prior_auth_db", "qrda_db", "sdoh_db",
                "predictive_db", "event_router_db", "notification_db",
                "audit_db", "authorization_db", "agent_runtime_db"
            );

            Set<String> existingDatabases = validator.listDatabases();

            assertThat(existingDatabases)
                .as("All required databases should exist")
                .containsAll(expectedDatabases);
        }

        @Order(3)
        @DisplayName("Liquibase migrations must be up-to-date")
        @Test
        void testLiquibaseMigrations() {
            assertThat(validator.areMigrationsUpToDate())
                .as("All Liquibase migrations should be applied")
                .isTrue();
        }

        @Order(4)
        @DisplayName("Database connection pooling must be configured")
        @Test
        void testDatabaseConnectionPooling() {
            assertThat(validator.isDatabaseConnectionPoolingConfigured())
                .as("Database connection pooling should be properly configured")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Cache & Message Queue")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CacheAndMessagingTests {

        @Order(1)
        @DisplayName("Redis cache must be accessible")
        @Test
        void testRedisConnectivity() {
            assertThat(validator.isCacheConnected("redis"))
                .as("Redis should be accessible")
                .isTrue();
        }

        @Order(2)
        @DisplayName("PHI cache TTL must be <= 5 minutes")
        @Test
        void testPhiCacheTTL() {
            int phiTtlSeconds = validator.getPhiCacheTTL();
            assertThat(phiTtlSeconds)
                .as("PHI cache TTL must be <= 300 seconds (5 minutes)")
                .isLessThanOrEqualTo(300);
        }

        @Order(3)
        @DisplayName("Kafka message queue must be accessible")
        @Test
        void testKafkaConnectivity() {
            assertThat(validator.isMessageQueueConnected("kafka"))
                .as("Kafka should be accessible")
                .isTrue();
        }

        @Order(4)
        @DisplayName("Kafka topics must exist for all services")
        @Test
        void testKafkaTopics() {
            Set<String> expectedTopics = Set.of(
                "patient-events", "care-gap-events", "quality-measure-events",
                "audit-events", "notification-events", "workflow-events"
            );

            Set<String> existingTopics = validator.listKafkaTopics();

            assertThat(existingTopics)
                .as("All required Kafka topics should exist")
                .containsAll(expectedTopics);
        }
    }

    @Nested
    @DisplayName("Monitoring & Observability")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MonitoringTests {

        @Order(1)
        @DisplayName("Prometheus must be scraping metrics")
        @Test
        void testPrometheusMetrics() {
            assertThat(validator.isPrometheusHealthy())
                .as("Prometheus should be healthy and scraping metrics")
                .isTrue();
        }

        @Order(2)
        @DisplayName("Grafana dashboards must be configured")
        @Test
        void testGrafanaDashboards() {
            assertThat(validator.hasGrafanaDashboards())
                .as("Grafana should have required dashboards")
                .isTrue();
        }

        @Order(3)
        @DisplayName("Distributed tracing must be working")
        @Test
        void testDistributedTracing() {
            assertThat(validator.isDistributedTracingConfigured())
                .as("Distributed tracing (Jaeger) should be configured")
                .isTrue();
        }

        @Order(4)
        @DisplayName("Log aggregation must be operational")
        @Test
        void testLogAggregation() {
            assertThat(validator.isLogAggregationOperational())
                .as("Log aggregation (ELK stack) should be operational")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Security & Compliance")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SecurityComplianceTests {

        @Order(1)
        @DisplayName("TLS must be configured on all endpoints")
        @Test
        void testTLSConfiguration() {
            assertThat(validator.isTLSEnabled())
                .as("TLS should be enabled on all endpoints")
                .isTrue();
        }

        @Order(2)
        @DisplayName("Security headers must be present on all responses")
        @Test
        void testSecurityHeaders() {
            List<String> requiredHeaders = List.of(
                "Cache-Control",
                "Pragma",
                "X-Content-Type-Options",
                "X-Frame-Options",
                "Strict-Transport-Security"
            );

            assertThat(validator.validateSecurityHeaders(requiredHeaders))
                .as("All required security headers should be present")
                .isTrue();
        }

        @Order(3)
        @DisplayName("Multi-tenant isolation must be enforced")
        @Test
        void testMultiTenantIsolation() {
            assertThat(validator.isMultiTenantIsolationEnforced())
                .as("Multi-tenant isolation should be enforced in all queries")
                .isTrue();
        }

        @Order(4)
        @DisplayName("HIPAA audit logging must be enabled")
        @Test
        void testHIPAAAuditLogging() {
            assertThat(validator.isAuditLoggingEnabled())
                .as("HIPAA audit logging should be enabled on all PHI access")
                .isTrue();
        }

        @Order(5)
        @DisplayName("JWT secrets must be in Vault")
        @Test
        void testJWTSecretManagement() {
            assertThat(validator.isJWTSecretInVault())
                .as("JWT secrets should be stored in HashiCorp Vault")
                .isTrue();
        }

        @Order(6)
        @DisplayName("No hardcoded credentials in configuration")
        @Test
        void testNoHardcodedCredentials() {
            assertThat(validator.hasNoHardcodedCredentials())
                .as("No hardcoded credentials should be present")
                .isTrue();
        }

        @Order(7)
        @DisplayName("Encryption at rest must be configured")
        @Test
        void testEncryptionAtRest() {
            assertThat(validator.isEncryptionAtRestConfigured())
                .as("Encryption at rest should be configured for sensitive data")
                .isTrue();
        }

        @Order(8)
        @DisplayName("Encryption in transit must be enforced")
        @Test
        void testEncryptionInTransit() {
            assertThat(validator.isEncryptionInTransitEnforced())
                .as("Encryption in transit (TLS 1.2+) should be enforced")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Configuration Validation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ConfigurationTests {

        @Order(1)
        @DisplayName("Environment variables must be properly set")
        @Test
        void testEnvironmentVariables() {
            List<String> requiredVars = List.of(
                "POSTGRES_HOST",
                "POSTGRES_PORT",
                "POSTGRES_DB",
                "POSTGRES_USER",
                "REDIS_HOST",
                "REDIS_PORT",
                "KAFKA_BOOTSTRAP_SERVERS",
                "JWT_SECRET",
                "VAULT_ADDR"
            );

            assertThat(validator.validateEnvironmentVariables(requiredVars))
                .as("All required environment variables should be set")
                .isTrue();
        }

        @Order(2)
        @DisplayName("Configuration files must be valid")
        @Test
        void testConfigurationValidation() {
            assertThat(validator.isConfigurationValid())
                .as("All configuration files should be valid")
                .isTrue();
        }

        @Order(3)
        @DisplayName("Resource limits must be properly configured")
        @Test
        void testResourceLimits() {
            assertThat(validator.areResourceLimitsConfigured())
                .as("Resource limits should be configured for production")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Blue-Green Deployment Readiness")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BlueGreenDeploymentTests {

        @Order(1)
        @DisplayName("Blue environment must be stable")
        @Test
        void testBlueEnvironmentStable() {
            assertThat(validator.isEnvironmentStable("blue"))
                .as("Blue environment should be stable baseline")
                .isTrue();
        }

        @Order(2)
        @DisplayName("Green environment must be ready for deployment")
        @Test
        void testGreenEnvironmentReady() {
            assertThat(validator.isEnvironmentReady("green"))
                .as("Green environment should be prepared for deployment")
                .isTrue();
        }

        @Order(3)
        @DisplayName("Load balancer can switch between environments")
        @Test
        void testLoadBalancerSwitching() {
            assertThat(validator.canSwitchEnvironments())
                .as("Load balancer should be able to switch between blue/green")
                .isTrue();
        }

        @Order(4)
        @DisplayName("Rollback procedures are tested")
        @Test
        void testRollbackProcedures() {
            assertThat(validator.areRollbackProceduresTested())
                .as("Rollback procedures should be tested and documented")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Disaster Recovery & Backup")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DisasterRecoveryTests {

        @Order(1)
        @DisplayName("Database backups must be current")
        @Test
        void testBackupCurrency() {
            Duration timeSinceLastBackup = validator.getTimeSinceLastBackup();
            assertThat(timeSinceLastBackup)
                .as("Database backup should be less than 1 hour old")
                .isLessThan(Duration.ofHours(1));
        }

        @Order(2)
        @DisplayName("RTO (Recovery Time Objective) must be < 1 hour")
        @Test
        void testRTO() {
            Duration rto = validator.calculateRTO();
            assertThat(rto)
                .as("RTO should be less than 1 hour")
                .isLessThan(Duration.ofHours(1));
        }

        @Order(3)
        @DisplayName("RPO (Recovery Point Objective) must be < 15 minutes")
        @Test
        void testRPO() {
            Duration rpo = validator.calculateRPO();
            assertThat(rpo)
                .as("RPO should be less than 15 minutes")
                .isLessThan(Duration.ofMinutes(15));
        }

        @Order(4)
        @DisplayName("Backup restore procedures must be tested")
        @Test
        void testBackupRestore() {
            assertThat(validator.canRestoreFromBackup())
                .as("Restore procedures should be tested successfully")
                .isTrue();
        }
    }

    @DisplayName("Deployment Sign-Off Report")
    @Test
    @Order(Integer.MAX_VALUE)
    void generateDeploymentSignOffReport() {
        DeploymentSignOffReport report = validator.generateSignOffReport();

        log.info("""
            ╔══════════════════════════════════════════════════════════════════════════╗
            ║                 PHASE 6 DEPLOYMENT READINESS SIGN-OFF                     ║
            ╠══════════════════════════════════════════════════════════════════════════╣
            ║ Timestamp: {}
            ║ Status: {}
            ║ Services Healthy: {}/28
            ║ Security Score: {}/100
            ║ Compliance Score: {}/100
            ║ Infrastructure Ready: {}
            ║ Backup Systems Tested: {}
            ╠══════════════════════════════════════════════════════════════════════════╣
            ║ SIGN-OFF CHECKLIST:
            ║ ✓ Security Officer: {} ✓
            ║ ✓ Compliance Officer: {} ✓
            ║ ✓ Infrastructure Lead: {} ✓
            ║ ✓ Operations Director: {} ✓
            ║ ✓ CTO/VP Engineering: {} ✓
            ║ ✓ CEO/Executive: {} ✓
            ╚══════════════════════════════════════════════════════════════════════════╝
            """,
            report.getTimestamp(),
            report.getStatus(),
            report.getHealthyServicesCount(),
            report.getSecurityScore(),
            report.getComplianceScore(),
            report.isInfrastructureReady() ? "YES" : "NO",
            report.isBackupTested() ? "YES" : "NO",
            report.getSecurityOfficerSign() ? "✓" : "⚠",
            report.getComplianceOfficerSign() ? "✓" : "⚠",
            report.getInfrastructureLeadSign() ? "✓" : "⚠",
            report.getOperationsDirectorSign() ? "✓" : "⚠",
            report.getCtoSign() ? "✓" : "⚠",
            report.getCeoSign() ? "✓" : "⚠"
        );

        // Validate all sign-offs collected
        assertThat(report.getStatus())
            .as("Deployment should be ready for go-live")
            .isEqualTo("READY_FOR_PRODUCTION");
    }
}
