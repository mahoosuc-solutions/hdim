package com.healthdata.eventsourcing.handler.careplan;

import com.healthdata.eventsourcing.command.careplan.CarePlanCreatedEvent;
import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.projection.careplan.CarePlanProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarePlanEventHandler {

    private final CarePlanProjectionService projectionService;

    @KafkaListener(
        topics = "careplan-events",
        groupId = "careplan-projection-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCarePlanCreatedEvent(CarePlanCreatedEvent event) {
        log.debug("Handling CarePlanCreatedEvent for patient: {} in tenant: {}",
            event.getPatientId(), event.getTenantId());

        try {
            CarePlanProjection projection = transformToProjection(event);
            projectionService.saveProjection(projection);

            log.info("Successfully created care plan projection for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId());

        } catch (Exception e) {
            log.error("Failed to handle CarePlanCreatedEvent for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId(), e);
            throw e;
        }
    }

    private CarePlanProjection transformToProjection(CarePlanCreatedEvent event) {
        int goalCount = event.getGoals() != null ? event.getGoals().size() : 0;

        return CarePlanProjection.builder()
            .tenantId(event.getTenantId())
            .patientId(event.getPatientId())
            .title(event.getCarePlanTitle())
            .coordinatorId(event.getCareCoordinatorId())
            .startDate(event.getStartDate())
            .endDate(event.getEndDate())
            .goalCount(goalCount)
            .build();
    }
}
