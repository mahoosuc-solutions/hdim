package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.client.CareGapServiceClient;
import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.client.PatientServiceClient;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calculate quality measure for a patient
     */
    @Transactional
    public QualityMeasureResultEntity calculateMeasure(
            String tenantId,
            UUID patientId,
            String measureId,
            String createdBy
    ) {
        log.info("Calculating measure {} for patient: {}", measureId, patientId);

        try {
            // Evaluate CQL library
            String cqlResult = cqlEngineServiceClient.evaluateCql(
                    tenantId, measureId, patientId, "{}");

            // Parse CQL result
            JsonNode result = objectMapper.readTree(cqlResult);

            // Create and save result
            QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .measureId(measureId)
                    .measureName(extractMeasureName(measureId, result))
                    .measureCategory(extractMeasureCategory(measureId))
                    .measureYear(LocalDate.now().getYear())
                    .numeratorCompliant(extractNumeratorCompliance(result))
                    .denominatorElligible(extractDenominatorEligibility(result))
                    .complianceRate(extractComplianceRate(result))
                    .score(extractScore(result))
                    .calculationDate(LocalDate.now())
                    .cqlLibrary(measureId)
                    .cqlResult(cqlResult)
                    .createdBy(createdBy)
                    .build();

            QualityMeasureResultEntity saved = repository.save(entity);

            // Publish event
            publishCalculationEvent(tenantId, patientId, measureId);

            return saved;
        } catch (Exception e) {
            log.error("Error calculating measure {}: {}", measureId, e.getMessage());
            throw new RuntimeException("Measure calculation failed", e);
        }
    }

    /**
     * Get measure results for a patient
     */
    @Cacheable(value = "measureResults", key = "#tenantId + ':' + #patientId")
    public List<QualityMeasureResultEntity> getPatientMeasureResults(
            String tenantId,
            UUID patientId
    ) {
        return repository.findByTenantIdAndPatientId(tenantId, patientId);
    }

    /**
     * Get all measure results for a tenant with pagination
     */
    @Cacheable(value = "allMeasureResults", key = "#tenantId + ':' + #page + ':' + #size")
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
     * Get quality score for a patient (percentage of compliant measures)
     */
    public QualityScore getQualityScore(String tenantId, UUID patientId) {
        List<QualityMeasureResultEntity> results = getPatientMeasureResults(tenantId, patientId);

        long total = results.size();
        long compliant = repository.countCompliantMeasures(tenantId, patientId);

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

    public record QualityScore(long totalMeasures, long compliantMeasures, double scorePercentage) {}
}
