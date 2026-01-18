package com.healthdata.eventsourcing.command.condition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnoseConditionCommandHandler {
    private final ConditionIncompatibilityMatrix incompatibilityMatrix;
    private final ConditionEventEmitter eventEmitter;

    public Mono<String> handle(DiagnoseConditionCommand command, String tenantId) {
        return Mono.defer(() -> {
            log.info("Processing DiagnoseConditionCommand for tenant: {}, patient: {}, ICD: {}",
                tenantId, command.getPatientId(), command.getIcdCode());

            String aggregateId = "patient-" + tenantId + "-" + command.getPatientId();

            ConditionDiagnosedEvent event = ConditionDiagnosedEvent.builder()
                .tenantId(tenantId)
                .patientId(command.getPatientId())
                .icdCode(command.getIcdCode())
                .onsetDate(command.getOnsetDate())
                .clinicalStatus(command.getClinicalStatus())
                .verificationStatus(command.getVerificationStatus())
                .build();

            return eventEmitter.emitConditionDiagnosed(event)
                .doOnSuccess(__ -> log.info("Successfully diagnosed condition for patient: {}", aggregateId))
                .thenReturn(aggregateId);
        });
    }

    public static class ConditionDiagnosisException extends RuntimeException {
        public ConditionDiagnosisException(String message) {
            super(message);
        }
    }
}
