package com.healthdata.quality.event;

import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.service.CareGapDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka Event Consumer for Quality Measure Results
 * Listens for measure-calculated events and triggers care gap detection
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeasureResultEventConsumer {

    private final CareGapDetectionService careGapDetectionService;
    private final QualityMeasureResultRepository measureResultRepository;

    /**
     * Listen for measure calculation events
     * Event format: { "measureResultId": "uuid", "tenantId": "...", "patientId": "uuid" }
     */
    @KafkaListener(
        topics = "${app.kafka.topics.measure-calculated:measure-calculated}",
        groupId = "${app.kafka.consumer.group-id:quality-measure-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMeasureCalculatedEvent(
        @Payload MeasureCalculatedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received measure-calculated event: measureResultId={}, tenantId={}, patientId={} from topic={}, partition={}, offset={}",
            event.measureResultId(),
            event.tenantId(),
            event.patientId(),
            topic,
            partition,
            offset
        );

        try {
            // Fetch the measure result
            QualityMeasureResultEntity measureResult = measureResultRepository
                .findById(event.measureResultId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Measure result not found: " + event.measureResultId()
                ));

            // Verify tenant isolation
            if (!measureResult.getTenantId().equals(event.tenantId())) {
                log.error("Tenant mismatch in measure-calculated event! Event tenantId={}, Result tenantId={}",
                    event.tenantId(), measureResult.getTenantId());
                return;
            }

            // Analyze measure result and create care gaps if needed
            careGapDetectionService.analyzeAndCreateCareGaps(measureResult);

            log.info("Successfully processed measure-calculated event for measureResultId={}",
                event.measureResultId());

        } catch (IllegalArgumentException e) {
            log.error("Error processing measure-calculated event: {}", e.getMessage(), e);
            // Don't retry if measure result doesn't exist
        } catch (Exception e) {
            log.error("Unexpected error processing measure-calculated event: {}", e.getMessage(), e);
            // Will retry based on Kafka consumer configuration
            throw e;
        }
    }

    /**
     * Measure Calculated Event DTO
     */
    public record MeasureCalculatedEvent(
        UUID measureResultId,
        String tenantId,
        UUID patientId,
        String measureId,
        String measureName,
        Boolean numeratorCompliant,
        Boolean denominatorEligible
    ) {}
}
