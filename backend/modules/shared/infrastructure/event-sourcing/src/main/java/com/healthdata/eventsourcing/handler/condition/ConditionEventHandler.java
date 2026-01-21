package com.healthdata.eventsourcing.handler.condition;

import com.healthdata.eventsourcing.command.condition.ConditionDiagnosedEvent;
import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.projection.condition.ConditionProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionEventHandler {

    private final ConditionProjectionService projectionService;

    @KafkaListener(
        topics = "condition-events",
        groupId = "condition-projection-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleConditionDiagnosedEvent(ConditionDiagnosedEvent event) {
        log.debug("Handling ConditionDiagnosedEvent for patient: {} in tenant: {}",
            event.getPatientId(), event.getTenantId());

        try {
            ConditionProjection projection = transformToProjection(event);
            projectionService.saveProjection(projection);

            log.info("Successfully created condition projection for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId());

        } catch (Exception e) {
            log.error("Failed to handle ConditionDiagnosedEvent for patient: {} in tenant: {}",
                event.getPatientId(), event.getTenantId(), e);
            throw e;
        }
    }

    private ConditionProjection transformToProjection(ConditionDiagnosedEvent event) {
        return ConditionProjection.builder()
            .tenantId(event.getTenantId())
            .patientId(event.getPatientId())
            .icdCode(event.getIcdCode())
            .status(event.getClinicalStatus())
            .verificationStatus(event.getVerificationStatus())
            .onsetDate(event.getOnsetDate())
            .build();
    }
}
