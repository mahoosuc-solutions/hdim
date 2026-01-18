package com.healthdata.caregap.service;

import com.healthdata.caregap.api.v1.dto.DetectGapRequest;
import com.healthdata.caregap.api.v1.dto.CareGapEventResponse;
import com.healthdata.caregap.event.CareGapDetectedEvent;
import com.healthdata.caregap.event.GapClosedEvent;
import com.healthdata.caregap.eventhandler.CareGapEventHandler;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.PopulationHealthRepository;
import com.healthdata.caregap.projection.CareGapProjection;
import com.healthdata.caregap.projection.PopulationHealthProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;

/**
 * Care Gap Event Application Service
 *
 * Orchestrates care gap detection and closure:
 * - Detects gaps for patients (CRITICAL, HIGH, MEDIUM, LOW)
 * - Tracks gap lifecycle and closure
 * - Aggregates population health metrics
 * - Calculates gap closure rates and days open
 *
 * ★ Insight ─────────────────────────────────────
 * Care gap management enables proactive quality improvement:
 * - Severity-based prioritization (CRITICAL gaps get urgent attention)
 * - Population-level analytics reveal patterns and trends
 * - Closure tracking measures intervention effectiveness
 * - Days open indicates intervention timeliness
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CareGapEventApplicationService {

    private final CareGapEventHandler gapEventHandler;
    private final CareGapProjectionRepository gapRepository;
    private final PopulationHealthRepository populationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String GAP_EVENTS_TOPIC = "gap.events";

    /**
     * Detect care gap for patient
     */
    public CareGapEventResponse detectGap(DetectGapRequest request, String tenantId) {
        log.info("Detecting gap: {}, patient: {}, severity: {}, tenant: {}",
            request.getGapCode(), request.getPatientId(), request.getSeverity(), tenantId);

        // Create domain event
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            request.getPatientId(),
            request.getGapCode(),
            tenantId,
            request.getDescription(),
            request.getSeverity()
        );

        // Delegate to Phase 4 event handler
        gapEventHandler.handle(event);

        // Update population health
        updatePopulationHealth(tenantId, request.getSeverity(), true);

        // Publish to Kafka
        kafkaTemplate.send(GAP_EVENTS_TOPIC, request.getPatientId(), event);

        log.info("Gap detected: {}, severity: {}", request.getGapCode(), request.getSeverity());

        return CareGapEventResponse.builder()
            .gapCode(request.getGapCode())
            .patientId(request.getPatientId())
            .severity(request.getSeverity())
            .status("OPEN")
            .detectionDate(LocalDate.now())
            .daysOpen(0)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Close care gap
     */
    public CareGapEventResponse closeGap(String gapId, String tenantId) {
        log.info("Closing gap: {}, tenant: {}", gapId, tenantId);

        Optional<CareGapProjection> gap = gapRepository.findById(gapId);
        if (gap.isEmpty()) {
            throw new RuntimeException("Gap not found: " + gapId);
        }

        CareGapProjection projection = gap.get();
        String severity = projection.getSeverity();

        // Create domain event
        GapClosedEvent event = new GapClosedEvent(
            projection.getPatientId(),
            gapId,
            projection.getGapCode(),
            tenantId
        );

        // Delegate to Phase 4 event handler
        gapEventHandler.handle(event);

        // Update population health (decrement open, increment closed)
        updatePopulationHealth(tenantId, severity, false);

        // Publish to Kafka
        kafkaTemplate.send(GAP_EVENTS_TOPIC, projection.getPatientId(), event);

        log.info("Gap closed: {}", gapId);

        return CareGapEventResponse.builder()
            .gapCode(projection.getGapCode())
            .patientId(projection.getPatientId())
            .severity(severity)
            .status("CLOSED")
            .closureDate(LocalDate.now())
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Get population health metrics
     */
    @Transactional(readOnly = true)
    public CareGapEventResponse getPopulationHealth(String tenantId) {
        log.info("Getting population health for tenant: {}", tenantId);

        Optional<PopulationHealthProjection> health = populationRepository.findByTenantId(tenantId);
        if (health.isEmpty()) {
            throw new RuntimeException("No population health data found for tenant: " + tenantId);
        }

        PopulationHealthProjection projection = health.get();

        return CareGapEventResponse.builder()
            .totalGapsOpen(projection.getTotalGapsOpen())
            .criticalGaps(projection.getCriticalGaps())
            .highGaps(projection.getHighGaps())
            .mediumGaps(projection.getMediumGaps())
            .lowGaps(projection.getLowGaps())
            .gapsClosed(projection.getGapsClosed())
            .closureRate(projection.getClosureRate())
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Update population health metrics
     */
    private void updatePopulationHealth(String tenantId, String severity, boolean isOpen) {
        PopulationHealthProjection health = populationRepository.findByTenantId(tenantId)
            .orElseGet(() -> {
                PopulationHealthProjection newHealth = new PopulationHealthProjection();
                newHealth.setTenantId(tenantId);
                newHealth.setVersion(0);
                return newHealth;
            });

        if (isOpen) {
            // Gap detected - increment open and severity counts
            health.setTotalGapsOpen(health.getTotalGapsOpen() + 1);
            switch (severity) {
                case "CRITICAL" -> health.setCriticalGaps(health.getCriticalGaps() + 1);
                case "HIGH" -> health.setHighGaps(health.getHighGaps() + 1);
                case "MEDIUM" -> health.setMediumGaps(health.getMediumGaps() + 1);
                case "LOW" -> health.setLowGaps(health.getLowGaps() + 1);
            }
        } else {
            // Gap closed - decrement open and severity, increment closed
            health.setTotalGapsOpen(Math.max(0, health.getTotalGapsOpen() - 1));
            switch (severity) {
                case "CRITICAL" -> health.setCriticalGaps(Math.max(0, health.getCriticalGaps() - 1));
                case "HIGH" -> health.setHighGaps(Math.max(0, health.getHighGaps() - 1));
                case "MEDIUM" -> health.setMediumGaps(Math.max(0, health.getMediumGaps() - 1));
                case "LOW" -> health.setLowGaps(Math.max(0, health.getLowGaps() - 1));
            }
            health.setGapsClosed(health.getGapsClosed() + 1);
        }

        // Calculate closure rate
        long totalGaps = health.getTotalGapsOpen() + health.getGapsClosed();
        if (totalGaps > 0) {
            health.setClosureRate((float) health.getGapsClosed() / totalGaps);
        }

        health.incrementVersion();
        populationRepository.save(health);
    }
}
