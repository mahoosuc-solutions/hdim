package com.healthdata.fhir.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.healthdata.fhir.admin.model.ApiPreset;
import com.healthdata.fhir.admin.model.DashboardSnapshot;
import com.healthdata.fhir.admin.model.DashboardSnapshot.Alert;
import com.healthdata.fhir.admin.model.DashboardSnapshot.BaselineSnapshot;
import com.healthdata.fhir.admin.model.DashboardSnapshot.PlatformMetric;
import com.healthdata.fhir.admin.model.DashboardSnapshot.ServiceHealth;
import com.healthdata.fhir.admin.model.DashboardSnapshot.ServiceHealthStatus;
import com.healthdata.fhir.admin.model.DashboardSnapshot.ServiceLatency;
import com.healthdata.fhir.admin.model.DashboardSnapshot.ServiceThroughput;
import com.healthdata.fhir.admin.model.DashboardSnapshot.TrendDirection;
import com.healthdata.fhir.admin.model.DashboardSnapshot.TrendSummary;
import com.healthdata.fhir.admin.model.ServiceCatalog;
import com.healthdata.fhir.admin.model.ServiceCatalog.ServiceContact;
import com.healthdata.fhir.admin.model.ServiceCatalog.ServiceDefinition;
import com.healthdata.fhir.admin.model.ServiceCatalog.ServiceLevelObjective;
import com.healthdata.fhir.admin.model.ServiceCatalog.ServiceLifecycleStatus;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot.DependencyHealth;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot.DependencyIndicator;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot.QueueMetric;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

@Service
public class AdminPortalService {

    private static final String DEFAULT_TENANT = "tenant-1";

    private final PatientRepository patientRepository;

    public AdminPortalService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public DashboardSnapshot getDashboardSnapshot(String tenantId) {
        String effectiveTenant = tenantOrDefault(tenantId);
        long patientCount = patientRepository.countByTenantId(effectiveTenant);

        double yearlyGrowth = patientCount > 0 ? Math.min(15.0, Math.log10(patientCount + 1) * 10) : 2.5;
        PlatformMetric patientMetric = new PlatformMetric(
                "patient-registry",
                "Registered Patients",
                String.valueOf(patientCount),
                "patients",
                new TrendSummary(yearlyGrowth, patientCount > 0 ? TrendDirection.UP : TrendDirection.STABLE, 0.82),
                "Since launch",
                new BaselineSnapshot("Seed cohort", "Synthetic seed data")
        );

        PlatformMetric latencyMetric = new PlatformMetric(
                "patient-latency",
                "Patient Read Latency",
                "148",
                "ms (p95)",
                new TrendSummary(-4.2, TrendDirection.DOWN, 0.67),
                "Last 24 h",
                new BaselineSnapshot("158 ms", "Rolling 7-day average")
        );

        PlatformMetric cacheMetric = new PlatformMetric(
                "cache-hit-rate",
                "Cache Hit Rate",
                "71",
                "%",
                new TrendSummary(5.1, TrendDirection.UP, 0.74),
                "Last 60 min",
                new BaselineSnapshot("64%", "Baseline prior to Redis tuning")
        );

        ServiceHealth fhir = new ServiceHealth(
                "fhir-service",
                "FHIR Service",
                ServiceHealthStatus.HEALTHY,
                "us-east-1",
                new ServiceLatency(120, 155, 210),
                new ServiceThroughput(320.4, 0.6),
                99.98,
                List.of(),
                Instant.now()
        );

        ServiceHealth consent = new ServiceHealth(
                "consent-service",
                "Consent Service",
                ServiceHealthStatus.DEGRADED,
                "us-east-1",
                new ServiceLatency(260, 310, 430),
                new ServiceThroughput(128.3, 2.1),
                99.2,
                List.of(new Alert(
                        "consent-throughput",
                        "WARNING",
                        "Throughput running above baseline, observing closely",
                        Instant.now().minusSeconds(3600)
                )),
                Instant.now()
        );

        return new DashboardSnapshot(
                Instant.now(),
                List.of(patientMetric, latencyMetric, cacheMetric),
                List.of(fhir, consent)
        );
    }

    public ServiceCatalog getServiceCatalog(String tenantId) {
        String effectiveTenant = tenantOrDefault(tenantId);
        int patientCount = Math.toIntExact(patientRepository.countByTenantId(effectiveTenant));

        ServiceDefinition fhirDefinition = new ServiceDefinition(
                "fhir-service",
                "FHIR R4 Gateway",
                "Clinical Data Platform",
                new ServiceContact("Maya Chen", "maya.chen@healthdatainmotion.io", "clinical-data-oncall"),
                "FHIR R4 compliant API with consent-aware filtering and audit logging.",
                ServiceLifecycleStatus.ACTIVE,
                new ServiceLevelObjective(99.9, 300, 44),
                List.of("us-east-1", "us-west-2"),
                List.of("HIPAA", "HITRUST"),
                "Kubernetes (Spring Boot)",
                99.9
        );

        ServiceDefinition consentDefinition = new ServiceDefinition(
                "consent-service",
                "Consent Engine",
                "Privacy Engineering",
                new ServiceContact("Rafael Iglesias", "rafael.iglesias@healthdatainmotion.io", null),
                "Granular access enforcement with policy decisions and break-glass workflows.",
                ServiceLifecycleStatus.MAINTENANCE,
                new ServiceLevelObjective(99.5, 450, 220),
                List.of("us-east-1"),
                List.of("HIPAA", "42CFRPart2"),
                "Kubernetes (Spring Boot)",
                99.5
        );

        ServiceDefinition patientRegistry = new ServiceDefinition(
                "patient-registry",
                "Patient Registry",
                "Data Engineering",
                new ServiceContact("Lina Haddad", "lina.haddad@healthdatainmotion.io", "patient-registry-pager"),
                "Authoritative patient registry storing " + patientCount + " active profiles.",
                ServiceLifecycleStatus.ACTIVE,
                new ServiceLevelObjective(99.9, 250, 44),
                List.of("us-east-1"),
                List.of("HIPAA"),
                "PostgreSQL 15 + Redis 7",
                99.9
        );

        return new ServiceCatalog(
                Instant.now(),
                List.of(fhirDefinition, consentDefinition, patientRegistry)
        );
    }

    public SystemHealthSnapshot getSystemHealth(String tenantId) {
        String effectiveTenant = tenantOrDefault(tenantId);
        long patientCount = patientRepository.countByTenantId(effectiveTenant);

        DependencyHealth postgres = new DependencyHealth(
                "postgres-fhir",
                "PostgreSQL (FHIR)",
                DependencyIndicator.OPERATIONAL,
                99.99,
                8.2,
                Instant.now()
        );

        DependencyHealth redis = new DependencyHealth(
                "redis-cache",
                "Redis Cache",
                DependencyIndicator.OPERATIONAL,
                99.95,
                2.4,
                Instant.now()
        );

        DependencyHealth kafka = new DependencyHealth(
                "kafka-primary",
                "Kafka Cluster",
                DependencyIndicator.ATTENTION,
                99.4,
                32.0,
                Instant.now()
        );

        QueueMetric patientEvents = new QueueMetric(
                "fhir.patient.events",
                Math.max(0, 5 - patientCount),
                42.7,
                1.8
        );
        QueueMetric consentDecisions = new QueueMetric(
                "consent.decisions",
                2,
                18.4,
                0.5
        );

        return new SystemHealthSnapshot(
                Instant.now(),
                List.of(postgres, redis, kafka),
                List.of(patientEvents, consentDecisions)
        );
    }

    public List<ApiPreset> getApiPresets(String tenantId) {
        String effectiveTenant = tenantOrDefault(tenantId);
        UUID samplePatient = patientRepository.findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(
                        effectiveTenant, "")
                .stream()
                .map(PatientEntity::getId)
                .findFirst()
                .orElse(UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95"));

        ApiPreset createPatient = new ApiPreset(
                "create-patient",
                "Create Patient",
                "POST",
                "/fhir/Patient",
                Map.of("Content-Type", "application/json"),
                Map.of(),
                """
                        {
                          "resourceType": "Patient",
                          "name": [
                            {
                              "family": "Chen",
                              "given": ["Maya"]
                            }
                          ],
                          "gender": "female",
                          "birthDate": "1985-05-20"
                        }
                        """
        );

        ApiPreset readPatient = new ApiPreset(
                "read-patient",
                "Read Patient",
                "GET",
                "/fhir/Patient/" + samplePatient,
                Map.of(),
                Map.of(),
                null
        );

        ApiPreset searchPatient = new ApiPreset(
                "search-patient",
                "Search Patient by Family",
                "GET",
                "/fhir/Patient",
                Map.of(),
                Map.of("family", "Chen"),
                null
        );

        return List.of(createPatient, readPatient, searchPatient);
    }

    private String tenantOrDefault(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return DEFAULT_TENANT;
        }
        return tenantId;
    }
}
