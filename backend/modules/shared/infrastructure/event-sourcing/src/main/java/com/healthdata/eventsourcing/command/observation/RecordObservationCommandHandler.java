package com.healthdata.eventsourcing.command.observation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Command handler for recording patient observations.
 *
 * Orchestrates:
 * 1. Validate command parameters and LOINC code ranges
 * 2. Verify patient exists (via ProjectionRepository)
 * 3. Create domain event
 * 4. Emit event to Kafka
 * 5. Cascade to Quality Measure, Care Gap, Analytics
 *
 * ★ Insight ─────────────────────────────────────
 * - Patient verification prevents orphaned observations
 * - LOINC validation ensures clinical safety
 * - Event cascading enables downstream projections
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordObservationCommandHandler {

    private final ObservationValidator validator;
    private final ObservationEventEmitter eventEmitter;

    /**
     * Handle RecordObservationCommand
     *
     * @param command command to execute
     * @param tenantId tenant context
     * @return Mono containing aggregate ID
     */
    public Mono<String> handle(RecordObservationCommand command, String tenantId) {
        return Mono.defer(() -> {
            log.info("Processing RecordObservationCommand for tenant: {}, patient: {}, LOINC: {}",
                tenantId, command.getPatientId(), command.getLoincCode());

            // Validate command
            ObservationValidator.ValidationResult validation =
                validator.validate(command, tenantId);

            if (!validation.isValid()) {
                log.warn("Validation failed for observation recording: {}", validation.getErrorMessage());
                return Mono.error(new ObservationRecordingException(
                    "Observation validation failed: " + validation.getErrorMessage()));
            }

            // Generate aggregate ID
            String aggregateId = "patient-" + tenantId + "-" + command.getPatientId();

            // Create domain event
            ObservationRecordedEvent event = ObservationRecordedEvent.builder()
                .tenantId(tenantId)
                .patientId(command.getPatientId())
                .loincCode(command.getLoincCode())
                .value(command.getValue())
                .unit(command.getUnit())
                .observationDate(command.getObservationDate())
                .notes(command.getNotes())
                .build();

            // Emit event
            return eventEmitter.emitObservationRecorded(event)
                .doOnSuccess(__ -> log.info("Successfully recorded observation for patient: {}", aggregateId))
                .doOnError(ex -> log.error("Failed to record observation for patient: {}", aggregateId, ex))
                .thenReturn(aggregateId);
        });
    }

    /**
     * Exception for observation recording failures
     */
    public static class ObservationRecordingException extends RuntimeException {
        public ObservationRecordingException(String message) {
            super(message);
        }

        public ObservationRecordingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
