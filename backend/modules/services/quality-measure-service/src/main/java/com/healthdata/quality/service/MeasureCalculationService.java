package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.metrics.HealthcareMetrics;
import com.healthdata.quality.audit.QualityMeasureAuditIntegration;
import com.healthdata.quality.client.CareGapServiceClient;
import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.client.PatientServiceClient;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import feign.FeignException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.UUID;

/**
 * Measure Calculation Service
 * Calculates quality measures using CQL and patient data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureCalculationService {

    private final QualityMeasureResultRepository repository;
    private final PatientServiceClient patientServiceClient;
    private final CareGapServiceClient careGapServiceClient;
    private final CqlEngineServiceClient cqlEngineServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final QualityMeasureAuditIntegration auditIntegration;
    private final Tracer tracer;
    private final HealthcareMetrics healthcareMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calculate quality measure for a patient
     * Transaction boundary optimized: external I/O happens outside transaction
     */
    public QualityMeasureResultEntity calculateMeasure(
            String tenantId,
            UUID patientId,
            String measureId,
            String createdBy
    ) {
        Span span = tracer.spanBuilder("quality_measure.calculate")
                .setAttribute("tenant.id", tenantId)
                .setAttribute("patient.id", patientId.toString())
                .setAttribute("measure.id", measureId)
                .startSpan();
        long calcStartTime = System.currentTimeMillis();
        try (Scope scope = span.makeCurrent()) {
            log.info("Calculating measure {} for patient: {}", measureId, patientId);

            // Evaluate CQL library (external I/O - no transaction needed).
            // Bridge raw measure IDs (e.g. "SPC") and canonical library IDs (e.g. "HEDIS-SPC").
            String cqlResult = evaluateCqlWithLibraryFallback(tenantId, measureId, patientId);

            // Parse CQL result (in-memory - no transaction needed)
            JsonNode result = objectMapper.readTree(cqlResult);

            // Save result (transactional - only DB operation)
            QualityMeasureResultEntity saved = saveCalculationResult(
                    tenantId, patientId, measureId, createdBy, cqlResult, result);

            // Publish event (async - no transaction needed)
            publishCalculationEvent(tenantId, patientId, measureId);

            // Publish audit event
            boolean measureMet = result.has("measure_met") && result.get("measure_met").asBoolean();
            Map<String, Object> measureResultMap = objectMapper.convertValue(result, Map.class);
            auditIntegration.publishMeasureCalculationEvent(
                    tenantId, patientId, measureId,
                    measureMet, measureResultMap, 0, createdBy
            );

            healthcareMetrics.recordEvaluation(measureId, true, java.time.Duration.ofMillis(System.currentTimeMillis() - calcStartTime));
            span.setStatus(StatusCode.OK);
            return saved;
        } catch (Exception e) {
            healthcareMetrics.recordEvaluation(measureId, false, java.time.Duration.ofMillis(System.currentTimeMillis() - calcStartTime));
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            log.error("Error calculating measure {}: {}", measureId, e.getMessage());
            throw new RuntimeException("Measure calculation failed", e);
        } finally {
            span.end();
        }
    }

    /**
     * Save calculation result in a separate transaction (optimized for minimal transaction duration).
     * Evicts measureResults and qualityScore caches for this patient so the next read
     * reflects the freshly stored result.
     */
    @Transactional
    @CacheEvict(value = {"measureResults", "qualityScore"}, key = "#tenantId + ':' + #patientId")
    private QualityMeasureResultEntity saveCalculationResult(
            String tenantId,
            UUID patientId,
            String measureId,
            String createdBy,
            String cqlResult,
            JsonNode result
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName(extractMeasureName(measureId, result))
                .measureCategory(extractMeasureCategory(measureId))
                .measureYear(LocalDate.now().getYear())
                .numeratorCompliant(extractNumeratorCompliance(result))
                .denominatorEligible(extractDenominatorEligibility(result))
                .complianceRate(extractComplianceRate(result))
                .score(extractScore(result))
                .calculationDate(LocalDate.now())
                .cqlLibrary(measureId)
                .cqlResult(cqlResult)
                .createdBy(createdBy)
                .build();

        return repository.save(entity);
    }

    /**
     * Get measure results for a patient.
     * Cached in Redis with a 2-minute HIPAA-compliant TTL (see spring.cache.redis.time-to-live).
     * unless = "#result == null" prevents caching empty/null responses.
     */
    @Cacheable(value = "measureResults", key = "#tenantId + ':' + #patientId", unless = "#result == null")
    public List<QualityMeasureResultEntity> getPatientMeasureResults(
            String tenantId,
            UUID patientId
    ) {
        return repository.findByTenantIdAndPatientId(tenantId, patientId);
    }

    /**
     * Get all measure results for a tenant with pagination
     */
    public List<QualityMeasureResultEntity> getAllMeasureResults(
            String tenantId,
            Integer page,
            Integer size
    ) {
        log.info("Getting all measure results for tenant: {} (page: {}, size: {})", tenantId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByTenantIdWithPagination(tenantId, pageable);
    }

    /**
     * Get quality score for a patient (percentage of compliant measures).
     * Cached in Redis with a 2-minute HIPAA-compliant TTL.
     * Derives the compliant count in-memory from the already-loaded results list,
     * eliminating the redundant countCompliantMeasures DB roundtrip.
     */
    @Cacheable(value = "qualityScore", key = "#tenantId + ':' + #patientId", unless = "#result == null")
    public QualityScore getQualityScore(String tenantId, UUID patientId) {
        List<QualityMeasureResultEntity> results = getPatientMeasureResults(tenantId, patientId);

        long total = results.size();
        // Derive compliant count in-memory — avoids a redundant DB COUNT query
        long compliant = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getNumeratorCompliant()))
                .count();

        double score = total > 0 ? (double) compliant / total * 100 : 0.0;

        return new QualityScore(total, compliant, score);
    }

    private String extractMeasureName(String measureId, JsonNode result) {
        if (result.has("measureResult") && result.get("measureResult").has("measureName")) {
            return result.get("measureResult").get("measureName").asText();
        }
        if (result.has("libraryName")) {
            return result.get("libraryName").asText();
        }
        return measureId.replace("_", " ");
    }

    private String extractMeasureCategory(String measureId) {
        if (measureId.startsWith("HEDIS_")) return "HEDIS";
        if (measureId.startsWith("CMS_")) return "CMS";
        return "custom";
    }

    private Boolean extractNumeratorCompliance(JsonNode result) {
        if (result.has("measureResult") && result.get("measureResult").has("inNumerator")) {
            return result.get("measureResult").get("inNumerator").asBoolean();
        }
        return false;
    }

    private Boolean extractDenominatorEligibility(JsonNode result) {
        if (result.has("measureResult") && result.get("measureResult").has("inDenominator")) {
            return result.get("measureResult").get("inDenominator").asBoolean();
        }
        return true;
    }

    private Double extractComplianceRate(JsonNode result) {
        if (result.has("measureResult") && result.get("measureResult").has("complianceRate")) {
            return result.get("measureResult").get("complianceRate").asDouble();
        }
        return null;
    }

    private Double extractScore(JsonNode result) {
        if (result.has("measureResult") && result.get("measureResult").has("score")) {
            return result.get("measureResult").get("score").asDouble();
        }
        if (result.has("score")) {
            return result.get("score").asDouble();
        }
        return null;
    }

    private void publishCalculationEvent(String tenantId, UUID patientId, String measureId) {
        try {
            String event = String.format(
                    "{\"tenantId\":\"%s\",\"patientId\":\"%s\",\"measureId\":\"%s\",\"timestamp\":\"%s\"}",
                    tenantId, patientId, measureId, LocalDate.now()
            );
            kafkaTemplate.send("measure-calculated", event);
        } catch (Exception e) {
            log.error("Error publishing calculation event: {}", e.getMessage());
        }
    }

    private String evaluateCqlWithLibraryFallback(String tenantId, String measureId, UUID patientId) {
        List<String> candidates = new ArrayList<>();
        candidates.add(measureId);

        if (measureId != null && !measureId.isBlank()) {
            if (measureId.startsWith("HEDIS-")) {
                candidates.add(measureId.substring("HEDIS-".length()));
            } else {
                candidates.add("HEDIS-" + measureId);
            }
        }

        FeignException.NotFound lastNotFound = null;
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                return cqlEngineServiceClient.evaluateCql(tenantId, candidate, patientId, "{}");
            } catch (FeignException.NotFound e) {
                lastNotFound = e;
                log.warn("CQL library not found for candidate '{}' (tenant={}, patient={})",
                        candidate, tenantId, patientId);
            }
        }

        if (lastNotFound != null) {
            throw lastNotFound;
        }
        throw new RuntimeException("No valid CQL library candidates for measureId: " + measureId);
    }

    public record QualityScore(long totalMeasures, long compliantMeasures, double scorePercentage) {}
}
