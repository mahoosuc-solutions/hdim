package com.healthdata.quality.service;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.persistence.*;
import com.healthdata.quality.service.notification.HealthScoreNotificationTrigger;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Health Score Service
 *
 * Calculates and maintains comprehensive health scores based on:
 * - Physical health (30% weight)
 * - Mental health (25% weight)
 * - Social determinants (15% weight)
 * - Preventive care (15% weight)
 * - Chronic disease management (15% weight)
 */
@Service
@Slf4j
public class HealthScoreService {

    private final HealthScoreRepository healthScoreRepository;
    private final HealthScoreHistoryRepository healthScoreHistoryRepository;
    private final MentalHealthAssessmentRepository mentalHealthRepository;
    private final CareGapRepository careGapRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final HealthScoreNotificationTrigger notificationTrigger;

    // Optional websocket handler - may be null when websocket is disabled
    private HealthScoreWebSocketHandler webSocketHandler;

    @Autowired
    public HealthScoreService(
            HealthScoreRepository healthScoreRepository,
            HealthScoreHistoryRepository healthScoreHistoryRepository,
            MentalHealthAssessmentRepository mentalHealthRepository,
            CareGapRepository careGapRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            HealthScoreNotificationTrigger notificationTrigger) {
        this.healthScoreRepository = healthScoreRepository;
        this.healthScoreHistoryRepository = healthScoreHistoryRepository;
        this.mentalHealthRepository = mentalHealthRepository;
        this.careGapRepository = careGapRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.notificationTrigger = notificationTrigger;
    }

    @Autowired(required = false)
    public void setWebSocketHandler(HealthScoreWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        log.info("WebSocket handler injected: {}", webSocketHandler != null);
    }

    // Scoring weights
    private static final double PHYSICAL_HEALTH_WEIGHT = 0.30;
    private static final double MENTAL_HEALTH_WEIGHT = 0.25;
    private static final double SOCIAL_DETERMINANTS_WEIGHT = 0.15;
    private static final double PREVENTIVE_CARE_WEIGHT = 0.15;
    private static final double CHRONIC_DISEASE_WEIGHT = 0.15;

    // Significant change threshold
    private static final double SIGNIFICANT_CHANGE_THRESHOLD = 10.0;

    /**
     * Calculate health score from component scores
     */
    @Transactional
    @CacheEvict(value = "healthScores", key = "#tenantId + ':' + #patientId")
    public HealthScoreDTO calculateHealthScore(
        String tenantId,
        UUID patientId,
        HealthScoreComponents components
    ) {
        log.info("Calculating health score for patient: {} in tenant: {}", patientId, tenantId);

        // Validate components
        components.validate();

        // Get previous score if exists
        Optional<HealthScoreEntity> previousScoreOpt =
            healthScoreRepository.findLatestByPatientId(tenantId, patientId);

        // Calculate weighted overall score
        double overallScore = components.calculateOverallScore();

        // Build new score entity
        HealthScoreEntity.HealthScoreEntityBuilder builder = HealthScoreEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .overallScore(overallScore)
            .physicalHealthScore(components.getPhysicalHealthScore())
            .mentalHealthScore(components.getMentalHealthScore())
            .socialDeterminantsScore(components.getSocialDeterminantsScore())
            .preventiveCareScore(components.getPreventiveCareScore())
            .chronicDiseaseScore(components.getChronicDiseaseScore())
            .calculatedAt(Instant.now());

        // Set previous score for comparison
        if (previousScoreOpt.isPresent()) {
            builder.previousScore(previousScoreOpt.get().getOverallScore());
        }

        HealthScoreEntity healthScore = builder.build();

        // Evaluate if this is a significant change
        healthScore.evaluateSignificantChange();

        // Save to database
        healthScore = healthScoreRepository.save(healthScore);
        log.info("Health score saved: {} for patient: {}", overallScore, patientId);

        // Save to history
        HealthScoreHistoryEntity history = HealthScoreHistoryEntity.fromHealthScore(healthScore);
        healthScoreHistoryRepository.save(history);
        log.debug("Health score history recorded for patient: {}", patientId);

        // Publish events
        publishHealthScoreEvents(healthScore);

        // Convert to DTO
        HealthScoreDTO healthScoreDTO = HealthScoreDTO.fromEntity(healthScore);

        // Trigger notifications for significant health score changes
        try {
            notificationTrigger.onHealthScoreCalculated(
                    tenantId,
                    healthScoreDTO,
                    previousScoreOpt.map(HealthScoreEntity::getOverallScore).orElse(null)
            );
        } catch (Exception e) {
            log.error("Failed to trigger health score notification for patient {}: {}",
                    patientId, e.getMessage(), e);
            // Don't fail the health score calculation if notification fails
        }

        return healthScoreDTO;
    }

    /**
     * Handle observation event from FHIR service
     *
     * Processes vital signs and lab results to update physical health scores:
     * - Blood Pressure (systolic/diastolic)
     * - Heart Rate
     * - Weight/BMI
     * - Blood Glucose
     * - Oxygen Saturation
     */
    @KafkaListener(
        topics = {"fhir.observations.created", "fhir.observations.updated"},
        groupId = "health-score-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleObservationEvent(Map<String, Object> event) {
        try {
            log.info("Received observation event");

            String tenantId = (String) event.get("tenantId");
            UUID patientId = extractPatientIdFromEvent(event);
            Map<String, Object> observationData = (Map<String, Object>) event.get("resource");

            if (tenantId == null || patientId == null || observationData == null) {
                log.warn("Missing required fields in observation event");
                return;
            }

            log.info("Processing observation for patient: {} in tenant: {}", patientId, tenantId);

            // Extract observation details
            VitalSignData vitalSign = extractVitalSignData(observationData);
            if (vitalSign == null) {
                log.debug("Observation is not a tracked vital sign, skipping health score update");
                return;
            }

            // Get current health score or create default
            Optional<HealthScoreEntity> currentOpt =
                healthScoreRepository.findLatestByPatientId(tenantId, patientId);

            HealthScoreComponents components;
            if (currentOpt.isPresent()) {
                HealthScoreEntity current = currentOpt.get();

                // Calculate updated physical health score based on vital signs
                double updatedPhysicalScore = calculatePhysicalHealthScoreFromVitalSign(
                    current.getPhysicalHealthScore(), vitalSign
                );

                components = HealthScoreComponents.builder()
                    .physicalHealthScore(updatedPhysicalScore)
                    .mentalHealthScore(current.getMentalHealthScore())
                    .socialDeterminantsScore(current.getSocialDeterminantsScore())
                    .preventiveCareScore(current.getPreventiveCareScore())
                    .chronicDiseaseScore(current.getChronicDiseaseScore())
                    .build();
            } else {
                // Default scores if first observation
                double initialPhysicalScore = calculateInitialPhysicalScoreFromVitalSign(vitalSign);

                components = HealthScoreComponents.builder()
                    .physicalHealthScore(initialPhysicalScore)
                    .mentalHealthScore(75.0)
                    .socialDeterminantsScore(75.0)
                    .preventiveCareScore(75.0)
                    .chronicDiseaseScore(75.0)
                    .build();
            }

            // Recalculate overall score
            calculateHealthScore(tenantId, patientId, components);

            log.info("Successfully processed observation event for patient {}", patientId);

        } catch (Exception e) {
            log.error("Error processing observation event", e);
        }
    }

    /**
     * Handle mental health assessment submission
     */
    @KafkaListener(
        topics = "mental-health-assessment.submitted",
        groupId = "health-score-service"
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleMentalHealthAssessment(String tenantId, MentalHealthAssessmentEntity assessment) {
        log.info("Processing mental health assessment for patient: {}", assessment.getPatientId());

        // Calculate mental health score from assessment
        double mentalHealthScore = calculateMentalHealthScore(assessment);

        // Get current health score or create default
        Optional<HealthScoreEntity> currentOpt =
            healthScoreRepository.findLatestByPatientId(tenantId, assessment.getPatientId());

        HealthScoreComponents components;
        if (currentOpt.isPresent()) {
            HealthScoreEntity current = currentOpt.get();
            components = HealthScoreComponents.builder()
                .physicalHealthScore(current.getPhysicalHealthScore())
                .mentalHealthScore(mentalHealthScore)  // Updated
                .socialDeterminantsScore(current.getSocialDeterminantsScore())
                .preventiveCareScore(current.getPreventiveCareScore())
                .chronicDiseaseScore(current.getChronicDiseaseScore())
                .build();
        } else {
            // Default scores if first assessment
            components = HealthScoreComponents.builder()
                .physicalHealthScore(75.0)
                .mentalHealthScore(mentalHealthScore)
                .socialDeterminantsScore(75.0)
                .preventiveCareScore(75.0)
                .chronicDiseaseScore(75.0)
                .build();
        }

        // Recalculate overall score
        calculateHealthScore(tenantId, assessment.getPatientId(), components);
    }

    /**
     * Handle care gap addressed event
     */
    @KafkaListener(
        topics = "care-gap.addressed",
        groupId = "health-score-service"
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleCareGapAddressed(String tenantId, CareGapEntity careGap) {
        log.info("Processing care gap addressed for patient: {}", careGap.getPatientId());

        // Get current health score
        Optional<HealthScoreEntity> currentOpt =
            healthScoreRepository.findLatestByPatientId(tenantId, careGap.getPatientId());

        if (currentOpt.isEmpty()) {
            log.warn("No health score found for patient: {}", careGap.getPatientId());
            return;
        }

        HealthScoreEntity current = currentOpt.get();

        // Update appropriate component score based on care gap category
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(current.getPhysicalHealthScore())
            .mentalHealthScore(current.getMentalHealthScore())
            .socialDeterminantsScore(current.getSocialDeterminantsScore())
            .preventiveCareScore(current.getPreventiveCareScore())
            .chronicDiseaseScore(current.getChronicDiseaseScore())
            .build();

        // Improve score based on gap category
        switch (careGap.getCategory()) {
            case PREVENTIVE_CARE:
                components.setPreventiveCareScore(
                    Math.min(100.0, components.getPreventiveCareScore() + 10.0)
                );
                break;
            case CHRONIC_DISEASE:
                components.setChronicDiseaseScore(
                    Math.min(100.0, components.getChronicDiseaseScore() + 10.0)
                );
                break;
            case MENTAL_HEALTH:
                components.setMentalHealthScore(
                    Math.min(100.0, components.getMentalHealthScore() + 10.0)
                );
                break;
            case SOCIAL_DETERMINANTS:
                components.setSocialDeterminantsScore(
                    Math.min(100.0, components.getSocialDeterminantsScore() + 10.0)
                );
                break;
            default:
                log.debug("Care gap category {} does not directly affect health score", careGap.getCategory());
        }

        // Recalculate overall score
        calculateHealthScore(tenantId, careGap.getPatientId(), components);
    }

    /**
     * Handle condition change event
     *
     * Processes chronic condition diagnoses to update health scores:
     * - Diabetes (Type 1, Type 2)
     * - Hypertension
     * - Heart Disease (CHF, CAD, Arrhythmia)
     * - COPD
     * - Asthma
     * - Chronic Kidney Disease
     * - Cancer
     *
     * Creates clinical alerts for severe conditions
     */
    @KafkaListener(
        topics = {"fhir.conditions.created", "fhir.conditions.updated"},
        groupId = "health-score-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleConditionEvent(Map<String, Object> event) {
        try {
            log.info("Received condition event");

            String tenantId = (String) event.get("tenantId");
            UUID patientId = extractPatientIdFromEvent(event);
            Map<String, Object> conditionData = (Map<String, Object>) event.get("resource");

            if (tenantId == null || patientId == null || conditionData == null) {
                log.warn("Missing required fields in condition event");
                return;
            }

            log.info("Processing condition for patient: {} in tenant: {}", patientId, tenantId);

            // Extract condition details
            ConditionData condition = extractConditionData(conditionData);
            if (condition == null || !condition.isActive()) {
                log.debug("Condition is not active or could not be extracted, skipping");
                return;
            }

            // Only process chronic conditions
            if (!isChronicCondition(condition)) {
                log.debug("Condition is not a chronic disease, skipping health score update");
                return;
            }

            // Get current health score or create default
            Optional<HealthScoreEntity> currentOpt =
                healthScoreRepository.findLatestByPatientId(tenantId, patientId);

            HealthScoreComponents components;
            if (currentOpt.isPresent()) {
                HealthScoreEntity current = currentOpt.get();

                // Calculate updated chronic disease and physical health scores
                ScoreImpact impact = calculateConditionScoreImpact(condition);

                double updatedChronicScore = Math.max(0.0,
                    current.getChronicDiseaseScore() - impact.getChronicDiseaseImpact());

                double updatedPhysicalScore = Math.max(0.0,
                    current.getPhysicalHealthScore() - impact.getPhysicalHealthImpact());

                components = HealthScoreComponents.builder()
                    .physicalHealthScore(updatedPhysicalScore)
                    .mentalHealthScore(current.getMentalHealthScore())
                    .socialDeterminantsScore(current.getSocialDeterminantsScore())
                    .preventiveCareScore(current.getPreventiveCareScore())
                    .chronicDiseaseScore(updatedChronicScore)
                    .build();

                log.info("Condition '{}' impact - Chronic: -{}, Physical: -{}",
                    condition.getConditionType(), impact.getChronicDiseaseImpact(),
                    impact.getPhysicalHealthImpact());

            } else {
                // Default scores if first condition
                ScoreImpact impact = calculateConditionScoreImpact(condition);

                components = HealthScoreComponents.builder()
                    .physicalHealthScore(75.0 - impact.getPhysicalHealthImpact())
                    .mentalHealthScore(75.0)
                    .socialDeterminantsScore(75.0)
                    .preventiveCareScore(75.0)
                    .chronicDiseaseScore(75.0 - impact.getChronicDiseaseImpact())
                    .build();
            }

            // Recalculate overall score
            HealthScoreDTO healthScoreDTO = calculateHealthScore(tenantId, patientId, components);

            // Create clinical alert for severe conditions
            createConditionAlertIfNeeded(tenantId, patientId, condition, healthScoreDTO);

            log.info("Successfully processed condition event for patient {}", patientId);

        } catch (Exception e) {
            log.error("Error processing condition event", e);
        }
    }

    /**
     * Get health score history for a patient
     */
    @Transactional(readOnly = true)
    public List<HealthScoreDTO> getHealthScoreHistory(String tenantId, UUID patientId) {
        log.debug("Retrieving health score history for patient: {}", patientId);

        List<HealthScoreHistoryEntity> history =
            healthScoreHistoryRepository.findByPatientIdOrderByCalculatedAtDesc(tenantId, patientId);

        return history.stream()
            .map(this::convertHistoryToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get current health score for a patient
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "healthScores", key = "#tenantId + ':' + #patientId", unless = "#result == null")
    public Optional<HealthScoreDTO> getCurrentHealthScore(String tenantId, UUID patientId) {
        return healthScoreRepository.findLatestByPatientId(tenantId, patientId)
            .map(HealthScoreDTO::fromEntity);
    }

    /**
     * Calculate mental health score from assessment
     * Converts assessment score to 0-100 scale and applies severity weighting
     */
    private double calculateMentalHealthScore(MentalHealthAssessmentEntity assessment) {
        // Convert score to percentage of max
        double scorePercent = ((double) assessment.getScore() / assessment.getMaxScore()) * 100.0;

        // Invert score (higher assessment score = worse mental health)
        double invertedScore = 100.0 - scorePercent;

        // Apply severity-based adjustments
        switch (assessment.getSeverity().toLowerCase()) {
            case "minimal":
                return Math.max(85.0, invertedScore);
            case "mild":
                return Math.max(70.0, Math.min(84.0, invertedScore));
            case "moderate":
                return Math.max(50.0, Math.min(69.0, invertedScore));
            case "moderately-severe":
                return Math.max(30.0, Math.min(49.0, invertedScore));
            case "severe":
                return Math.min(29.0, invertedScore);
            default:
                return invertedScore;
        }
    }

    /**
     * Publish health score update events
     */
    private void publishHealthScoreEvents(HealthScoreEntity healthScore) {
        Map<String, Object> event = Map.of(
            "patientId", healthScore.getPatientId(),
            "tenantId", healthScore.getTenantId(),
            "overallScore", healthScore.getOverallScore(),
            "previousScore", healthScore.getPreviousScore() != null ? healthScore.getPreviousScore() : 0.0,
            "scoreDelta", healthScore.getScoreDelta() != null ? healthScore.getScoreDelta() : 0.0,
            "calculatedAt", healthScore.getCalculatedAt().toString()
        );

        // Publish regular update event to Kafka
        kafkaTemplate.send("health-score.updated", healthScore.getPatientId().toString(), event);
        log.debug("Published health-score.updated event for patient: {}", healthScore.getPatientId());

        // Broadcast update via WebSocket for real-time UI updates (if websocket is enabled)
        if (webSocketHandler != null) {
            try {
                webSocketHandler.broadcastHealthScoreUpdate(event, healthScore.getTenantId());
                log.debug("Broadcasted health score update via WebSocket for patient: {}", healthScore.getPatientId());
            } catch (Exception e) {
                log.error("Failed to broadcast health score update via WebSocket: {}", e.getMessage());
            }
        }

        // Publish significant change event if applicable
        if (healthScore.isSignificantChange()) {
            Map<String, Object> significantChangeEvent = new HashMap<>(event);
            significantChangeEvent.put("changeReason", healthScore.getChangeReason());
            significantChangeEvent.put("significantChange", true);

            // Publish to Kafka
            kafkaTemplate.send(
                "health-score.significant-change",
                healthScore.getPatientId().toString(),
                significantChangeEvent
            );
            log.info("Published health-score.significant-change event for patient: {} - {}",
                healthScore.getPatientId(), healthScore.getChangeReason());

            // Broadcast alert via WebSocket for immediate attention (if websocket is enabled)
            if (webSocketHandler != null) {
                try {
                    webSocketHandler.broadcastSignificantChange(significantChangeEvent, healthScore.getTenantId());
                    log.info("Broadcasted significant change alert via WebSocket for patient: {}", healthScore.getPatientId());
                } catch (Exception e) {
                    log.error("Failed to broadcast significant change alert via WebSocket: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Extract patient ID from event object
     */
    private UUID extractPatientIdFromEvent(Object event) {
        if (event == null) {
            return null;
        }

        try {
            // Try to get patientId field via reflection or string parsing
            if (event instanceof Map) {
                Map<?, ?> eventMap = (Map<?, ?>) event;

                // Try direct patientId field first
                Object patientId = eventMap.get("patientId");
                if (patientId != null) {
                    return parsePatientIdValue(patientId);
                }

                // Try to extract from resource.subject.reference
                Object resourceObj = eventMap.get("resource");
                if (resourceObj instanceof Map) {
                    Map<?, ?> resource = (Map<?, ?>) resourceObj;
                    Object subjectObj = resource.get("subject");
                    if (subjectObj instanceof Map) {
                        Map<?, ?> subject = (Map<?, ?>) subjectObj;
                        Object reference = subject.get("reference");
                        if (reference != null) {
                            // Extract ID from "Patient/123"
                            String refString = reference.toString();
                            return parsePatientIdString(refString.replace("Patient/", ""));
                        }
                    }
                }
            }

            // Try reflection for POJO objects
            try {
                java.lang.reflect.Method getPatientId = event.getClass().getMethod("getPatientId");
                Object result = getPatientId.invoke(event);
                return result != null ? parsePatientIdValue(result) : null;
            } catch (Exception e) {
                log.debug("Could not extract patientId via reflection: {}", e.getMessage());
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting patient ID from event: {}", e.getMessage());
            return null;
        }
    }

    private UUID parsePatientIdValue(Object patientId) {
        if (patientId instanceof UUID) {
            return (UUID) patientId;
        }
        return parsePatientIdString(patientId.toString());
    }

    private UUID parsePatientIdString(String patientId) {
        if (patientId == null) {
            return null;
        }
        try {
            return UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==================== FHIR Observation Processing ====================

    /**
     * Extract vital sign data from FHIR Observation resource
     */
    private VitalSignData extractVitalSignData(Map<String, Object> observationData) {
        try {
            // Get LOINC code
            Map<String, Object> code = (Map<String, Object>) observationData.get("code");
            if (code == null) {
                return null;
            }

            List<Map<String, Object>> codings = (List<Map<String, Object>>) code.get("coding");
            if (codings == null || codings.isEmpty()) {
                return null;
            }

            String loincCode = (String) codings.get(0).get("code");
            String display = (String) codings.get(0).get("display");

            // Get value
            Object valueObj = observationData.get("valueQuantity");
            Double value = null;
            String unit = null;

            if (valueObj instanceof Map) {
                Map<String, Object> valueQuantity = (Map<String, Object>) valueObj;
                Object valueNumber = valueQuantity.get("value");
                if (valueNumber instanceof Number) {
                    value = ((Number) valueNumber).doubleValue();
                }
                unit = (String) valueQuantity.get("unit");
            }

            if (value == null) {
                return null;
            }

            // Map LOINC code to vital sign type
            VitalSignType type = mapLoincCodeToVitalSignType(loincCode);
            if (type == null) {
                return null;
            }

            return new VitalSignData(type, loincCode, display, value, unit);

        } catch (Exception e) {
            log.warn("Error extracting vital sign data from observation", e);
            return null;
        }
    }

    /**
     * Map LOINC code to vital sign type
     */
    private VitalSignType mapLoincCodeToVitalSignType(String loincCode) {
        return switch (loincCode) {
            case "8480-6" -> VitalSignType.BLOOD_PRESSURE_SYSTOLIC;
            case "8462-4" -> VitalSignType.BLOOD_PRESSURE_DIASTOLIC;
            case "8867-4" -> VitalSignType.HEART_RATE;
            case "29463-7" -> VitalSignType.WEIGHT;
            case "39156-5" -> VitalSignType.BMI;
            case "2339-0" -> VitalSignType.GLUCOSE;
            case "2345-7" -> VitalSignType.GLUCOSE_FASTING;
            case "4548-4" -> VitalSignType.HEMOGLOBIN_A1C;
            case "2708-6" -> VitalSignType.OXYGEN_SATURATION;
            case "8310-5" -> VitalSignType.BODY_TEMPERATURE;
            case "9279-1" -> VitalSignType.RESPIRATORY_RATE;
            default -> null;
        };
    }

    /**
     * Calculate physical health score from vital sign
     */
    private double calculatePhysicalHealthScoreFromVitalSign(double currentScore, VitalSignData vitalSign) {
        double adjustment = 0.0;

        switch (vitalSign.getType()) {
            case BLOOD_PRESSURE_SYSTOLIC:
                adjustment = calculateBPSystolicAdjustment(vitalSign.getValue());
                break;
            case BLOOD_PRESSURE_DIASTOLIC:
                adjustment = calculateBPDiastolicAdjustment(vitalSign.getValue());
                break;
            case HEART_RATE:
                adjustment = calculateHeartRateAdjustment(vitalSign.getValue());
                break;
            case BMI:
                adjustment = calculateBMIAdjustment(vitalSign.getValue());
                break;
            case GLUCOSE:
            case GLUCOSE_FASTING:
                adjustment = calculateGlucoseAdjustment(vitalSign.getValue());
                break;
            case HEMOGLOBIN_A1C:
                adjustment = calculateA1CAdjustment(vitalSign.getValue());
                break;
            case OXYGEN_SATURATION:
                adjustment = calculateOxygenSaturationAdjustment(vitalSign.getValue());
                break;
            default:
                adjustment = 0.0;
        }

        double newScore = currentScore + adjustment;
        return Math.max(0.0, Math.min(100.0, newScore));
    }

    /**
     * Calculate initial physical score from first vital sign
     */
    private double calculateInitialPhysicalScoreFromVitalSign(VitalSignData vitalSign) {
        // Start with baseline of 75 and adjust
        return calculatePhysicalHealthScoreFromVitalSign(75.0, vitalSign);
    }

    // Vital sign scoring methods
    private double calculateBPSystolicAdjustment(double systolic) {
        if (systolic < 90) return -10.0;      // Hypotension
        if (systolic < 120) return 5.0;       // Optimal
        if (systolic < 130) return 2.0;       // Elevated
        if (systolic < 140) return -3.0;      // Stage 1 hypertension
        if (systolic < 180) return -8.0;      // Stage 2 hypertension
        return -15.0;                          // Hypertensive crisis
    }

    private double calculateBPDiastolicAdjustment(double diastolic) {
        if (diastolic < 60) return -10.0;     // Hypotension
        if (diastolic < 80) return 5.0;       // Optimal
        if (diastolic < 90) return -3.0;      // Stage 1 hypertension
        if (diastolic < 120) return -8.0;     // Stage 2 hypertension
        return -15.0;                          // Hypertensive crisis
    }

    private double calculateHeartRateAdjustment(double heartRate) {
        if (heartRate < 40) return -10.0;     // Bradycardia (severe)
        if (heartRate < 60) return -3.0;      // Bradycardia
        if (heartRate <= 100) return 3.0;     // Normal
        if (heartRate <= 120) return -3.0;    // Tachycardia
        return -10.0;                          // Tachycardia (severe)
    }

    private double calculateBMIAdjustment(double bmi) {
        if (bmi < 16.0) return -15.0;         // Severely underweight
        if (bmi < 18.5) return -8.0;          // Underweight
        if (bmi < 25.0) return 5.0;           // Normal weight
        if (bmi < 30.0) return -3.0;          // Overweight
        if (bmi < 35.0) return -8.0;          // Obese Class I
        if (bmi < 40.0) return -12.0;         // Obese Class II
        return -18.0;                          // Obese Class III
    }

    private double calculateGlucoseAdjustment(double glucose) {
        if (glucose < 70) return -8.0;        // Hypoglycemia
        if (glucose <= 100) return 5.0;       // Normal
        if (glucose <= 125) return -2.0;      // Prediabetes
        if (glucose <= 200) return -8.0;      // Diabetes
        return -15.0;                          // Severe hyperglycemia
    }

    private double calculateA1CAdjustment(double a1c) {
        if (a1c < 5.7) return 5.0;            // Normal
        if (a1c < 6.5) return -3.0;           // Prediabetes
        if (a1c < 7.0) return -6.0;           // Diabetes (controlled)
        if (a1c < 9.0) return -10.0;          // Diabetes (poorly controlled)
        return -15.0;                          // Diabetes (very poorly controlled)
    }

    private double calculateOxygenSaturationAdjustment(double spo2) {
        if (spo2 >= 95) return 3.0;           // Normal
        if (spo2 >= 90) return -5.0;          // Mild hypoxemia
        if (spo2 >= 85) return -10.0;         // Moderate hypoxemia
        return -18.0;                          // Severe hypoxemia
    }

    // ==================== FHIR Condition Processing ====================

    /**
     * Extract condition data from FHIR Condition resource
     */
    private ConditionData extractConditionData(Map<String, Object> conditionData) {
        try {
            // Get clinical status
            Map<String, Object> clinicalStatus = (Map<String, Object>) conditionData.get("clinicalStatus");
            boolean isActive = false;
            if (clinicalStatus != null) {
                List<Map<String, Object>> codings = (List<Map<String, Object>>) clinicalStatus.get("coding");
                if (codings != null && !codings.isEmpty()) {
                    String statusCode = (String) codings.get(0).get("code");
                    isActive = "active".equals(statusCode);
                }
            }

            // Get severity
            Map<String, Object> severity = (Map<String, Object>) conditionData.get("severity");
            String severityCode = null;
            if (severity != null) {
                List<Map<String, Object>> codings = (List<Map<String, Object>>) severity.get("coding");
                if (codings != null && !codings.isEmpty()) {
                    severityCode = (String) codings.get(0).get("code");
                }
            }

            // Get condition code (SNOMED CT, ICD-10)
            Map<String, Object> code = (Map<String, Object>) conditionData.get("code");
            if (code == null) {
                return null;
            }

            List<Map<String, Object>> codings = (List<Map<String, Object>>) code.get("coding");
            if (codings == null || codings.isEmpty()) {
                return null;
            }

            String conditionCode = (String) codings.get(0).get("code");
            String display = (String) codings.get(0).get("display");
            String system = (String) codings.get(0).get("system");

            // Map to condition type
            ConditionType type = mapCodeToConditionType(conditionCode, system);

            return new ConditionData(type, conditionCode, display, severityCode, isActive);

        } catch (Exception e) {
            log.warn("Error extracting condition data", e);
            return null;
        }
    }

    /**
     * Check if condition is chronic
     */
    private boolean isChronicCondition(ConditionData condition) {
        return condition.getConditionType() != null &&
               condition.getConditionType() != ConditionType.OTHER;
    }

    /**
     * Map condition code to condition type
     */
    private ConditionType mapCodeToConditionType(String code, String system) {
        // SNOMED CT codes
        if ("http://snomed.info/sct".equals(system)) {
            return switch (code) {
                case "44054006" -> ConditionType.DIABETES_TYPE_2;
                case "46635009" -> ConditionType.DIABETES_TYPE_1;
                case "38341003" -> ConditionType.HYPERTENSION;
                case "42343007" -> ConditionType.CONGESTIVE_HEART_FAILURE;
                case "53741008" -> ConditionType.CORONARY_ARTERY_DISEASE;
                case "13645005" -> ConditionType.COPD;
                case "195967001" -> ConditionType.ASTHMA;
                case "709044004" -> ConditionType.CHRONIC_KIDNEY_DISEASE;
                case "363406005" -> ConditionType.CANCER;
                default -> ConditionType.OTHER;
            };
        }

        // ICD-10 codes
        if ("http://hl7.org/fhir/sid/icd-10-cm".equals(system)) {
            if (code.startsWith("E11")) return ConditionType.DIABETES_TYPE_2;
            if (code.startsWith("E10")) return ConditionType.DIABETES_TYPE_1;
            if (code.startsWith("I10")) return ConditionType.HYPERTENSION;
            if (code.startsWith("I50")) return ConditionType.CONGESTIVE_HEART_FAILURE;
            if (code.startsWith("I25")) return ConditionType.CORONARY_ARTERY_DISEASE;
            if (code.startsWith("J44")) return ConditionType.COPD;
            if (code.startsWith("J45")) return ConditionType.ASTHMA;
            if (code.startsWith("N18")) return ConditionType.CHRONIC_KIDNEY_DISEASE;
            if (code.startsWith("C")) return ConditionType.CANCER;
        }

        return ConditionType.OTHER;
    }

    /**
     * Calculate score impact from condition
     */
    private ScoreImpact calculateConditionScoreImpact(ConditionData condition) {
        // Base impacts by condition type
        double chronicImpact = 0.0;
        double physicalImpact = 0.0;

        switch (condition.getConditionType()) {
            case DIABETES_TYPE_1:
                chronicImpact = 15.0;
                physicalImpact = 10.0;
                break;
            case DIABETES_TYPE_2:
                chronicImpact = 12.0;
                physicalImpact = 8.0;
                break;
            case HYPERTENSION:
                chronicImpact = 8.0;
                physicalImpact = 5.0;
                break;
            case CONGESTIVE_HEART_FAILURE:
                chronicImpact = 20.0;
                physicalImpact = 18.0;
                break;
            case CORONARY_ARTERY_DISEASE:
                chronicImpact = 18.0;
                physicalImpact = 15.0;
                break;
            case COPD:
                chronicImpact = 18.0;
                physicalImpact = 15.0;
                break;
            case ASTHMA:
                chronicImpact = 10.0;
                physicalImpact = 8.0;
                break;
            case CHRONIC_KIDNEY_DISEASE:
                chronicImpact = 20.0;
                physicalImpact = 18.0;
                break;
            case CANCER:
                chronicImpact = 25.0;
                physicalImpact = 22.0;
                break;
            default:
                chronicImpact = 5.0;
                physicalImpact = 3.0;
        }

        // Adjust based on severity
        double severityMultiplier = getSeverityMultiplier(condition.getSeverityCode());
        chronicImpact *= severityMultiplier;
        physicalImpact *= severityMultiplier;

        return new ScoreImpact(chronicImpact, physicalImpact);
    }

    /**
     * Get severity multiplier
     */
    private double getSeverityMultiplier(String severityCode) {
        if (severityCode == null) {
            return 1.0; // Default/moderate
        }

        return switch (severityCode.toLowerCase()) {
            case "mild", "255604002" -> 0.6;       // SNOMED: Mild
            case "moderate", "6736007" -> 1.0;      // SNOMED: Moderate
            case "severe", "24484000" -> 1.5;       // SNOMED: Severe
            default -> 1.0;
        };
    }

    /**
     * Create clinical alert if condition is severe
     */
    private void createConditionAlertIfNeeded(
        String tenantId,
        UUID patientId,
        ConditionData condition,
        HealthScoreDTO healthScore
    ) {
        // Create alerts for severe chronic conditions
        boolean shouldAlert = false;
        ClinicalAlertEntity.AlertSeverity alertSeverity = ClinicalAlertEntity.AlertSeverity.MEDIUM;

        switch (condition.getConditionType()) {
            case CONGESTIVE_HEART_FAILURE:
            case CANCER:
            case CHRONIC_KIDNEY_DISEASE:
                shouldAlert = true;
                alertSeverity = ClinicalAlertEntity.AlertSeverity.HIGH;
                break;
            case CORONARY_ARTERY_DISEASE:
            case COPD:
                shouldAlert = true;
                alertSeverity = ClinicalAlertEntity.AlertSeverity.MEDIUM;
                break;
            case DIABETES_TYPE_1:
                if ("severe".equalsIgnoreCase(condition.getSeverityCode())) {
                    shouldAlert = true;
                    alertSeverity = ClinicalAlertEntity.AlertSeverity.MEDIUM;
                }
                break;
        }

        if (shouldAlert) {
            log.info("Creating {} severity alert for condition: {} (patient: {})",
                alertSeverity, condition.getDisplay(), patientId);

            // Note: Alert creation would happen here
            // We're logging for now as we may not have direct access to ClinicalAlertRepository
            // In production, this could be done via:
            // 1. Publishing an event to Kafka for ClinicalAlertService to consume
            // 2. Injecting ClinicalAlertService or ClinicalAlertRepository

            Map<String, Object> alertEvent = Map.of(
                "tenantId", tenantId,
                "patientId", patientId,
                "conditionType", condition.getConditionType().name(),
                "conditionDisplay", condition.getDisplay(),
                "severity", alertSeverity.name(),
                "healthScore", healthScore.getOverallScore()
            );

            kafkaTemplate.send("condition.alert.needed", patientId.toString(), alertEvent);
        }
    }

    // ==================== Inner Classes ====================

    /**
     * Vital Sign Data holder
     */
    private static class VitalSignData {
        private final VitalSignType type;
        private final String loincCode;
        private final String display;
        private final Double value;
        private final String unit;

        public VitalSignData(VitalSignType type, String loincCode, String display, Double value, String unit) {
            this.type = type;
            this.loincCode = loincCode;
            this.display = display;
            this.value = value;
            this.unit = unit;
        }

        public VitalSignType getType() { return type; }
        public String getLoincCode() { return loincCode; }
        public String getDisplay() { return display; }
        public Double getValue() { return value; }
        public String getUnit() { return unit; }
    }

    /**
     * Vital Sign Types
     */
    private enum VitalSignType {
        BLOOD_PRESSURE_SYSTOLIC,
        BLOOD_PRESSURE_DIASTOLIC,
        HEART_RATE,
        WEIGHT,
        BMI,
        GLUCOSE,
        GLUCOSE_FASTING,
        HEMOGLOBIN_A1C,
        OXYGEN_SATURATION,
        BODY_TEMPERATURE,
        RESPIRATORY_RATE
    }

    /**
     * Condition Data holder
     */
    private static class ConditionData {
        private final ConditionType conditionType;
        private final String conditionCode;
        private final String display;
        private final String severityCode;
        private final boolean active;

        public ConditionData(ConditionType conditionType, String conditionCode, String display,
                           String severityCode, boolean active) {
            this.conditionType = conditionType;
            this.conditionCode = conditionCode;
            this.display = display;
            this.severityCode = severityCode;
            this.active = active;
        }

        public ConditionType getConditionType() { return conditionType; }
        public String getConditionCode() { return conditionCode; }
        public String getDisplay() { return display; }
        public String getSeverityCode() { return severityCode; }
        public boolean isActive() { return active; }
    }

    /**
     * Condition Types
     */
    private enum ConditionType {
        DIABETES_TYPE_1,
        DIABETES_TYPE_2,
        HYPERTENSION,
        CONGESTIVE_HEART_FAILURE,
        CORONARY_ARTERY_DISEASE,
        COPD,
        ASTHMA,
        CHRONIC_KIDNEY_DISEASE,
        CANCER,
        OTHER
    }

    /**
     * Score Impact holder
     */
    private static class ScoreImpact {
        private final double chronicDiseaseImpact;
        private final double physicalHealthImpact;

        public ScoreImpact(double chronicDiseaseImpact, double physicalHealthImpact) {
            this.chronicDiseaseImpact = chronicDiseaseImpact;
            this.physicalHealthImpact = physicalHealthImpact;
        }

        public double getChronicDiseaseImpact() { return chronicDiseaseImpact; }
        public double getPhysicalHealthImpact() { return physicalHealthImpact; }
    }

    /**
     * Convert history entity to DTO
     */
    private HealthScoreDTO convertHistoryToDTO(HealthScoreHistoryEntity history) {
        return HealthScoreDTO.builder()
            .id(history.getId())
            .patientId(history.getPatientId())
            .tenantId(history.getTenantId())
            .overallScore(history.getOverallScore())
            .physicalHealthScore(history.getPhysicalHealthScore())
            .mentalHealthScore(history.getMentalHealthScore())
            .socialDeterminantsScore(history.getSocialDeterminantsScore())
            .preventiveCareScore(history.getPreventiveCareScore())
            .chronicDiseaseScore(history.getChronicDiseaseScore())
            .calculatedAt(history.getCalculatedAt())
            .previousScore(history.getPreviousScore())
            .scoreDelta(history.getScoreDelta())
            .changeReason(history.getChangeReason())
            .build();
    }

    /**
     * Get patients with health scores below threshold (paginated)
     *
     * @param tenantId Tenant identifier
     * @param threshold Score threshold (patients with scores below this are returned)
     * @param pageable Pagination parameters
     * @return Page of health scores for at-risk patients
     */
    @Transactional(readOnly = true)
    public Page<HealthScoreDTO> getAtRiskPatients(String tenantId, Double threshold, Pageable pageable) {
        log.debug("Retrieving at-risk patients for tenant: {} with threshold: {}", tenantId, threshold);

        Page<HealthScoreEntity> atRiskScores = healthScoreRepository.findLatestScoresBelowThreshold(
            tenantId, threshold, pageable
        );

        return atRiskScores.map(HealthScoreDTO::fromEntity);
    }

    /**
     * Get patients with significant health score changes (paginated)
     *
     * @param tenantId Tenant identifier
     * @param since Only include changes since this timestamp
     * @param pageable Pagination parameters
     * @return Page of health scores with significant changes
     */
    @Transactional(readOnly = true)
    public Page<HealthScoreDTO> getSignificantChanges(String tenantId, Instant since, Pageable pageable) {
        log.debug("Retrieving significant health score changes for tenant: {} since: {}", tenantId, since);

        Page<HealthScoreEntity> significantChanges = healthScoreRepository.findSignificantChangesSince(
            tenantId, since, pageable
        );

        return significantChanges.map(HealthScoreDTO::fromEntity);
    }
}
