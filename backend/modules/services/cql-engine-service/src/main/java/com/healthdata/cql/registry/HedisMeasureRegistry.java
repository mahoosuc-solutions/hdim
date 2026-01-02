package com.healthdata.cql.registry;

import com.healthdata.cql.measure.HedisMeasure;
import com.healthdata.cql.measure.MeasureResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for HEDIS Quality Measures
 *
 * Auto-discovers all Spring-managed HedisMeasure implementations and provides
 * a centralized lookup mechanism for measure evaluation. This enables dynamic
 * routing of evaluations to the appropriate Java measure implementation.
 *
 * Features:
 * - Auto-discovery of @Component measures via Spring injection
 * - Thread-safe measure lookup
 * - Category-based filtering
 * - Measure metadata retrieval for frontend display
 */
@Component
public class HedisMeasureRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HedisMeasureRegistry.class);

    private final Map<String, HedisMeasure> measuresByIdLower = new ConcurrentHashMap<>();
    private final Map<String, HedisMeasure> measuresById = new ConcurrentHashMap<>();
    private final List<HedisMeasure> allMeasures;

    /**
     * Constructor - receives all HedisMeasure beans from Spring context
     */
    public HedisMeasureRegistry(List<HedisMeasure> measures) {
        this.allMeasures = measures != null ? measures : Collections.emptyList();
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing HEDIS Measure Registry...");

        for (HedisMeasure measure : allMeasures) {
            String measureId = measure.getMeasureId();
            measuresById.put(measureId, measure);
            measuresByIdLower.put(measureId.toLowerCase(), measure);
            logger.debug("Registered measure: {} - {}", measureId, measure.getMeasureName());
        }

        logger.info("HEDIS Measure Registry initialized with {} measures", measuresById.size());

        // Log registered measure IDs for debugging
        if (logger.isDebugEnabled()) {
            String measureIds = measuresById.keySet().stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            logger.debug("Registered measure IDs: {}", measureIds);
        }
    }

    /**
     * Get measure by ID (case-insensitive)
     *
     * @param measureId The HEDIS measure ID (e.g., "CDC", "CBP", "BCS")
     * @return Optional containing the measure if found
     */
    public Optional<HedisMeasure> getMeasure(String measureId) {
        if (measureId == null) {
            return Optional.empty();
        }

        // Try exact match first, then case-insensitive
        HedisMeasure measure = measuresById.get(measureId);
        if (measure == null) {
            measure = measuresByIdLower.get(measureId.toLowerCase());
        }

        // Also try with HEDIS- prefix stripped
        if (measure == null && measureId.startsWith("HEDIS-")) {
            String strippedId = measureId.substring(6);
            measure = measuresById.get(strippedId);
            if (measure == null) {
                measure = measuresByIdLower.get(strippedId.toLowerCase());
            }
        }

        return Optional.ofNullable(measure);
    }

    /**
     * Check if a measure is registered
     */
    public boolean hasMeasure(String measureId) {
        return getMeasure(measureId).isPresent();
    }

    /**
     * Evaluate a measure for a patient
     *
     * @param measureId The HEDIS measure ID
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return MeasureResult or throws exception if measure not found
     */
    public MeasureResult evaluate(String measureId, String tenantId, UUID patientId) {
        HedisMeasure measure = getMeasure(measureId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown measure: " + measureId + ". Available measures: " +
                        String.join(", ", getMeasureIds())));

        logger.debug("Evaluating measure {} for patient {} (tenant: {})",
                measureId, patientId, tenantId);

        return measure.evaluate(tenantId, patientId);
    }

    /**
     * Check if patient is eligible for a measure
     */
    public boolean isEligible(String measureId, String tenantId, UUID patientId) {
        return getMeasure(measureId)
                .map(m -> m.isEligible(tenantId, patientId))
                .orElse(false);
    }

    /**
     * Get all registered measure IDs
     */
    public Set<String> getMeasureIds() {
        return Collections.unmodifiableSet(measuresById.keySet());
    }

    /**
     * Get all registered measures
     */
    public List<HedisMeasure> getAllMeasures() {
        return Collections.unmodifiableList(allMeasures);
    }

    /**
     * Get count of registered measures
     */
    public int getMeasureCount() {
        return measuresById.size();
    }

    /**
     * Get measure metadata for frontend display
     */
    public List<MeasureInfo> getMeasureInfoList() {
        return allMeasures.stream()
                .map(m -> new MeasureInfo(
                        m.getMeasureId(),
                        m.getMeasureName(),
                        m.getVersion(),
                        m.getClass().getSimpleName()))
                .sorted(Comparator.comparing(MeasureInfo::measureId))
                .collect(Collectors.toList());
    }

    /**
     * DTO for measure metadata
     */
    public record MeasureInfo(
            String measureId,
            String measureName,
            String version,
            String implementationClass
    ) {}
}
