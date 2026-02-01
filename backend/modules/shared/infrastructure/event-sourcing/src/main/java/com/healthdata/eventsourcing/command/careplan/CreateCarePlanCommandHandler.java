package com.healthdata.eventsourcing.command.careplan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCarePlanCommandHandler {
    private final CarePlanEventEmitter eventEmitter;

    public Mono<String> handle(CreateCarePlanCommand command, String tenantId) {
        return Mono.defer(() -> {
            log.info("Processing CreateCarePlanCommand for tenant: {}, patient: {}",
                tenantId, command.getPatientId());

            String aggregateId = "patient-" + tenantId + "-" + command.getPatientId();

            CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
                .tenantId(tenantId)
                .patientId(command.getPatientId())
                .carePlanTitle(command.getCarePlanTitle())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .careCoordinatorId(command.getCareCoordinatorId())
                .targetConditions(command.getTargetConditions())
                .goals(command.getGoals())
                .build();

            return eventEmitter.emitCarePlanCreated(event)
                .doOnSuccess(__ -> log.info("Successfully created care plan for patient: {}", aggregateId))
                .thenReturn(aggregateId);
        });
    }

    public static class CarePlanCreationException extends RuntimeException {
        public CarePlanCreationException(String message) {
            super(message);
        }
    }
}
