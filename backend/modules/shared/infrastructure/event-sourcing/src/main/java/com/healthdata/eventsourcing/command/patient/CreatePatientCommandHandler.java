package com.healthdata.eventsourcing.command.patient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Command handler for creating patients.
 *
 * Orchestrates the complete command handling flow:
 * 1. Validate command parameters
 * 2. Check business rules (MRN uniqueness per tenant)
 * 3. Create domain event
 * 4. Emit event for projection updates
 * 5. Return correlation ID for tracking
 *
 * ★ Insight ─────────────────────────────────────
 * - Deterministic aggregate ID ensures idempotency
 * - Single responsibility: validation → event creation
 * - Reactive Mono<String> for non-blocking async handling
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePatientCommandHandler {

    private final CreatePatientValidator validator;
    private final PatientEventEmitter eventEmitter;

    /**
     * Handle CreatePatientCommand
     *
     * @param command command to execute
     * @param tenantId tenant context
     * @return Mono containing aggregate ID of created patient
     */
    public Mono<String> handle(CreatePatientCommand command, String tenantId) {
        return Mono.defer(() -> {
            log.info("Processing CreatePatientCommand for tenant: {}, MRN: {}",
                tenantId, command.getMrn());

            // Validate command
            CreatePatientValidator.ValidationResult validation =
                validator.validate(command, tenantId);

            if (!validation.isValid()) {
                log.warn("Validation failed for patient creation: {}", validation.getErrorMessage());
                return Mono.error(new PatientCreationException(
                    "Patient creation validation failed: " + validation.getErrorMessage()));
            }

            // Generate deterministic aggregate ID
            String aggregateId = "patient-" + tenantId + "-" + command.getMrn();

            // Create domain event
            PatientCreatedEvent event = PatientCreatedEvent.builder()
                .tenantId(tenantId)
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .dateOfBirth(command.getDateOfBirth())
                .gender(command.getGender())
                .mrn(command.getMrn())
                .insuranceMemberId(command.getInsuranceMemberId())
                .sensitivityLevel("SENSITIVE")
                .hipaaCompliant(true)
                .build();

            // Emit event
            return eventEmitter.emitPatientCreated(event)
                .doOnSuccess(__ -> log.info("Successfully created patient: {}", aggregateId))
                .doOnError(ex -> log.error("Failed to create patient: {}", aggregateId, ex))
                .thenReturn(aggregateId);
        });
    }

    /**
     * Exception for patient creation failures
     */
    public static class PatientCreationException extends RuntimeException {
        public PatientCreationException(String message) {
            super(message);
        }

        public PatientCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
