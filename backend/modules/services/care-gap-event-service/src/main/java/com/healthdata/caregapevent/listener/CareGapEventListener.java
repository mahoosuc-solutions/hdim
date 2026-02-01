package com.healthdata.caregapevent.listener;

import com.healthdata.caregapevent.projection.CareGapProjection;
import com.healthdata.caregapevent.repository.CareGapProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Care Gap Event Listener (CQRS Event Handler)
 *
 * Consumes domain events from Kafka and updates the care gap projection.
 *
 * Events consumed:
 * - care-gap.identified: New care gap detected
 * - care-gap.closed: Care gap closed (closed_reason provided)
 * - care-gap.auto-closed: Care gap automatically closed by system
 * - care-gap.priority.changed: Priority updated
 * - care-gap.waived: Care gap waived (closed_reason provided)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapEventListener {

    private final CareGapProjectionRepository careGapRepository;

    /**
     * Handle care-gap.identified event
     * Creates a new care gap projection when gap is identified
     */
    @KafkaListener(
        topics = "care-gap.identified",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapIdentified(String tenantId, UUID careGapId, UUID patientId, String measureId,
                                    String priority, LocalDate dueDate, String description) {
        log.debug("Processing care-gap.identified event for care gap {} (patient: {}, priority: {})",
            careGapId, patientId, priority);

        CareGapProjection projection = CareGapProjection.builder()
            .tenantId(tenantId)
            .careGapId(careGapId)
            .patientId(patientId)
            .measureId(measureId)
            .priority(priority)
            .status("OPEN")
            .dueDate(dueDate)
            .description(description)
            .daysOverdue(0)
            .createdAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .eventVersion(1L)
            .build();

        careGapRepository.save(projection);
        log.info("Created care gap projection for care gap {} in tenant {}", careGapId, tenantId);
    }

    /**
     * Handle care-gap.closed event
     * Updates care gap status to CLOSED with reason
     */
    @KafkaListener(
        topics = "care-gap.closed",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapClosed(String tenantId, UUID careGapId, String closedReason, String closureMethod) {
        log.debug("Processing care-gap.closed event for care gap {}: reason={}, method={}",
            careGapId, closedReason, closureMethod);

        careGapRepository.findByTenantIdAndCareGapId(tenantId, careGapId)
            .ifPresentOrElse(
                projection -> {
                    projection.setStatus("CLOSED");
                    projection.setClosedAt(Instant.now());
                    projection.setClosedReason(closedReason);
                    projection.setClosureMethod(closureMethod);
                    projection.setLastUpdatedAt(Instant.now());
                    careGapRepository.save(projection);
                    log.info("Closed care gap projection for care gap {}", careGapId);
                },
                () -> log.warn("Care gap projection not found for care gap {} in tenant {}", careGapId, tenantId)
            );
    }

    /**
     * Handle care-gap.auto-closed event
     * Updates care gap status to CLOSED with AUTO_CLOSED method
     */
    @KafkaListener(
        topics = "care-gap.auto-closed",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapAutoClosed(String tenantId, UUID careGapId, String reason) {
        log.debug("Processing care-gap.auto-closed event for care gap {}: reason={}", careGapId, reason);

        onCareGapClosed(tenantId, careGapId, reason, "AUTO_CLOSED");
    }

    /**
     * Handle care-gap.priority.changed event
     * Updates care gap priority
     */
    @KafkaListener(
        topics = "care-gap.priority.changed",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPriorityChanged(String tenantId, UUID careGapId, String oldPriority, String newPriority) {
        log.debug("Processing care-gap.priority.changed for care gap {}: {} -> {}",
            careGapId, oldPriority, newPriority);

        careGapRepository.findByTenantIdAndCareGapId(tenantId, careGapId)
            .ifPresentOrElse(
                projection -> {
                    projection.setPriority(newPriority);
                    projection.setLastUpdatedAt(Instant.now());
                    careGapRepository.save(projection);
                    log.info("Updated priority for care gap {}: {}", careGapId, newPriority);
                },
                () -> log.warn("Care gap projection not found for care gap {} in tenant {}", careGapId, tenantId)
            );
    }

    /**
     * Handle care-gap.waived event
     * Updates care gap status to CLOSED with WAIVED method
     */
    @KafkaListener(
        topics = "care-gap.waived",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapWaived(String tenantId, UUID careGapId, String waiveReason, String waiveAuthor) {
        log.debug("Processing care-gap.waived for care gap {}: reason={}, author={}",
            careGapId, waiveReason, waiveAuthor);

        careGapRepository.findByTenantIdAndCareGapId(tenantId, careGapId)
            .ifPresentOrElse(
                projection -> {
                    projection.setStatus("CLOSED");
                    projection.setClosedAt(Instant.now());
                    projection.setClosedReason(waiveReason);
                    projection.setClosureMethod("WAIVED");
                    projection.setAssignedTo(waiveAuthor);
                    projection.setLastUpdatedAt(Instant.now());
                    careGapRepository.save(projection);
                    log.info("Waived care gap projection for care gap {}", careGapId);
                },
                () -> log.warn("Care gap projection not found for care gap {} in tenant {}", careGapId, tenantId)
            );
    }

    /**
     * Handle care-gap.assigned event
     * Updates care gap assignment
     */
    @KafkaListener(
        topics = "care-gap.assigned",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onCareGapAssigned(String tenantId, UUID careGapId, String assignedTo) {
        log.debug("Processing care-gap.assigned for care gap {}: assigned to {}", careGapId, assignedTo);

        careGapRepository.findByTenantIdAndCareGapId(tenantId, careGapId)
            .ifPresentOrElse(
                projection -> {
                    projection.setAssignedTo(assignedTo);
                    projection.setLastUpdatedAt(Instant.now());
                    careGapRepository.save(projection);
                    log.debug("Updated assignment for care gap {}", careGapId);
                },
                () -> log.warn("Care gap projection not found for care gap {} in tenant {}", careGapId, tenantId)
            );
    }

    /**
     * Handle care-gap.due-date-updated event
     * Updates due date and calculates days overdue
     */
    @KafkaListener(
        topics = "care-gap.due-date-updated",
        groupId = "care-gap-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onDueDateUpdated(String tenantId, UUID careGapId, LocalDate newDueDate) {
        log.debug("Processing care-gap.due-date-updated for care gap {}: new due date {}", careGapId, newDueDate);

        careGapRepository.findByTenantIdAndCareGapId(tenantId, careGapId)
            .ifPresentOrElse(
                projection -> {
                    projection.setDueDate(newDueDate);
                    // Calculate days overdue
                    LocalDate today = LocalDate.now();
                    if (newDueDate.isBefore(today)) {
                        projection.setDaysOverdue((int) java.time.temporal.ChronoUnit.DAYS.between(newDueDate, today));
                    } else {
                        projection.setDaysOverdue(0);
                    }
                    projection.setLastUpdatedAt(Instant.now());
                    careGapRepository.save(projection);
                    log.debug("Updated due date for care gap {}", careGapId);
                },
                () -> log.warn("Care gap projection not found for care gap {} in tenant {}", careGapId, tenantId)
            );
    }
}
