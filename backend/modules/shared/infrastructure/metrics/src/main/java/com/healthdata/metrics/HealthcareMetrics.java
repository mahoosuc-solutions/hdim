package com.healthdata.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Healthcare-specific business metrics for HDIM platform.
 *
 * Provides standardized metrics for:
 * - Patient operations (queries, updates)
 * - Clinical evaluations (CQL, quality measures)
 * - FHIR operations (reads, writes, searches)
 * - Care gaps (detection, closure)
 * - Integration operations (EHR sync, message processing)
 *
 * Usage:
 * <pre>
 * @Autowired HealthcareMetrics metrics;
 *
 * // Record patient query
 * metrics.recordPatientQuery("search", Duration.ofMillis(150));
 *
 * // Record evaluation
 * metrics.recordEvaluation("CMS125", true, Duration.ofSeconds(2));
 *
 * // Track care gaps
 * metrics.recordCareGapDetected("CMS130");
 * metrics.recordCareGapClosed("CMS130", "patient-completed");
 * </pre>
 */
public class HealthcareMetrics {

    private final MeterRegistry registry;
    private final String serviceName;
    private final ConcurrentHashMap<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

    public HealthcareMetrics(MeterRegistry registry, String serviceName) {
        this.registry = registry;
        this.serviceName = serviceName;
    }

    // ==================== Patient Metrics ====================

    /**
     * Record a patient query/search operation.
     */
    public void recordPatientQuery(String operation, Duration duration) {
        Timer.builder("hdim.patient.query.duration")
                .description("Duration of patient query operations")
                .tag("service", serviceName)
                .tag("operation", operation)
                .register(registry)
                .record(duration);
    }

    /**
     * Record patient data access.
     */
    public void recordPatientAccess(String accessType) {
        Counter.builder("hdim.patient.access.total")
                .description("Total patient data access count")
                .tag("service", serviceName)
                .tag("access_type", accessType)
                .register(registry)
                .increment();
    }

    // ==================== Clinical Evaluation Metrics ====================

    /**
     * Record a clinical evaluation (CQL, quality measure).
     */
    public void recordEvaluation(String measureId, boolean success, Duration duration) {
        Timer.builder("hdim.evaluation.duration")
                .description("Duration of clinical evaluations")
                .tag("service", serviceName)
                .tag("measure_id", measureId)
                .tag("success", String.valueOf(success))
                .register(registry)
                .record(duration);

        String counterName = success ? "hdim.evaluation.success.total" : "hdim.evaluation.failure.total";
        Counter.builder(counterName)
                .description(success ? "Successful evaluations" : "Failed evaluations")
                .tag("service", serviceName)
                .tag("measure_id", measureId)
                .register(registry)
                .increment();
    }

    /**
     * Record batch evaluation.
     */
    public void recordBatchEvaluation(String measureId, int patientCount, Duration duration) {
        Timer.builder("hdim.evaluation.batch.duration")
                .description("Duration of batch evaluations")
                .tag("service", serviceName)
                .tag("measure_id", measureId)
                .register(registry)
                .record(duration);

        Counter.builder("hdim.evaluation.batch.patients.total")
                .description("Total patients in batch evaluations")
                .tag("service", serviceName)
                .tag("measure_id", measureId)
                .register(registry)
                .increment(patientCount);
    }

    // ==================== FHIR Metrics ====================

    /**
     * Record FHIR resource operation.
     */
    public void recordFhirOperation(String resourceType, String operation, boolean success, Duration duration) {
        Timer.builder("hdim.fhir.operation.duration")
                .description("Duration of FHIR operations")
                .tag("service", serviceName)
                .tag("resource_type", resourceType)
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .register(registry)
                .record(duration);
    }

    /**
     * Record FHIR bundle size.
     */
    public void recordFhirBundleSize(String bundleType, int entryCount) {
        Counter.builder("hdim.fhir.bundle.entries.total")
                .description("Total entries in FHIR bundles")
                .tag("service", serviceName)
                .tag("bundle_type", bundleType)
                .register(registry)
                .increment(entryCount);
    }

    // ==================== Care Gap Metrics ====================

    /**
     * Record care gap detection.
     */
    public void recordCareGapDetected(String measureId) {
        Counter.builder("hdim.care_gap.detected.total")
                .description("Total care gaps detected")
                .tag("service", serviceName)
                .tag("measure_id", measureId)
                .register(registry)
                .increment();
    }

    /**
     * Record care gap closure.
     */
    public void recordCareGapClosed(String measureId, String closureReason) {
        Counter.builder("hdim.care_gap.closed.total")
                .description("Total care gaps closed")
                .tag("service", serviceName)
                .tag("measure_id", measureId)
                .tag("reason", closureReason)
                .register(registry)
                .increment();
    }

    /**
     * Track active care gaps (gauge).
     */
    public void setActiveCareGaps(String measureId, long count) {
        String key = "care_gap_active_" + measureId;
        gaugeValues.computeIfAbsent(key, k -> {
            AtomicLong value = new AtomicLong(count);
            Gauge.builder("hdim.care_gap.active", value, AtomicLong::get)
                    .description("Currently active care gaps")
                    .tag("service", serviceName)
                    .tag("measure_id", measureId)
                    .register(registry);
            return value;
        }).set(count);
    }

    // ==================== Integration Metrics ====================

    /**
     * Record EHR integration operation.
     */
    public void recordEhrIntegration(String ehrSystem, String operation, boolean success, Duration duration) {
        Timer.builder("hdim.integration.ehr.duration")
                .description("Duration of EHR integration operations")
                .tag("service", serviceName)
                .tag("ehr_system", ehrSystem)
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .register(registry)
                .record(duration);
    }

    /**
     * Record message processing.
     */
    public void recordMessageProcessed(String messageType, boolean success) {
        String counterName = success ? "hdim.message.processed.total" : "hdim.message.failed.total";
        Counter.builder(counterName)
                .description(success ? "Successfully processed messages" : "Failed message processing")
                .tag("service", serviceName)
                .tag("message_type", messageType)
                .register(registry)
                .increment();
    }

    // ==================== Tenant Metrics ====================

    /**
     * Record tenant operation.
     */
    public void recordTenantOperation(String tenantId, String operation, Duration duration) {
        Timer.builder("hdim.tenant.operation.duration")
                .description("Duration of tenant-specific operations")
                .tag("service", serviceName)
                .tag("tenant_id", tenantId)
                .tag("operation", operation)
                .register(registry)
                .record(duration);
    }

    // ==================== Utility Methods ====================

    /**
     * Create a timer sample for manual timing.
     */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop a timer and record to a specific metric.
     */
    public void stopTimer(Timer.Sample sample, String metricName, String... tags) {
        Timer.Builder builder = Timer.builder(metricName)
                .tag("service", serviceName);

        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        sample.stop(builder.register(registry));
    }

    /**
     * Increment a counter with custom tags.
     */
    public void incrementCounter(String name, String description, String... tags) {
        Counter.Builder builder = Counter.builder(name)
                .description(description)
                .tag("service", serviceName);

        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        builder.register(registry).increment();
    }

    /**
     * Register a gauge with a value supplier.
     */
    public void registerGauge(String name, String description, Supplier<Number> valueSupplier, String... tags) {
        Gauge.Builder<Supplier<Number>> builder = Gauge.builder(name, valueSupplier, s -> s.get().doubleValue())
                .description(description)
                .tag("service", serviceName);

        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        builder.register(registry);
    }
}
