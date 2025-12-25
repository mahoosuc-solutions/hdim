package com.healthdata.sales.service;

import com.healthdata.sales.dto.ActivityDTO;
import com.healthdata.sales.entity.Activity;
import com.healthdata.sales.entity.ActivityType;
import com.healthdata.sales.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findAll(UUID tenantId, Pageable pageable) {
        return activityRepository.findByTenantId(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public ActivityDTO findById(UUID tenantId, UUID id) {
        Activity activity = activityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Activity not found: " + id));
        return toDTO(activity);
    }

    @Transactional
    public ActivityDTO create(UUID tenantId, ActivityDTO dto) {
        dto.setTenantId(tenantId);
        Activity activity = toEntity(dto);
        activity = activityRepository.save(activity);
        log.info("Created activity {} for tenant {}", activity.getId(), tenantId);
        return toDTO(activity);
    }

    @Transactional
    public ActivityDTO update(UUID tenantId, UUID id, ActivityDTO dto) {
        Activity activity = activityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Activity not found: " + id));

        updateEntity(activity, dto);
        activity = activityRepository.save(activity);
        log.info("Updated activity {}", activity.getId());
        return toDTO(activity);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Activity activity = activityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Activity not found: " + id));
        activityRepository.delete(activity);
        log.info("Deleted activity {}", id);
    }

    @Transactional
    public ActivityDTO markComplete(UUID tenantId, UUID id, String outcome) {
        Activity activity = activityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Activity not found: " + id));

        activity.markComplete(outcome);
        activity = activityRepository.save(activity);
        log.info("Marked activity {} as complete", id);
        return toDTO(activity);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findByLead(UUID tenantId, UUID leadId, Pageable pageable) {
        return activityRepository.findByTenantIdAndLeadId(tenantId, leadId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findByContact(UUID tenantId, UUID contactId, Pageable pageable) {
        return activityRepository.findByTenantIdAndContactId(tenantId, contactId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findByOpportunity(UUID tenantId, UUID opportunityId, Pageable pageable) {
        return activityRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findByAccount(UUID tenantId, UUID accountId, Pageable pageable) {
        return activityRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findByType(UUID tenantId, ActivityType type, Pageable pageable) {
        return activityRepository.findByTenantIdAndActivityType(tenantId, type, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findByAssignedUser(UUID tenantId, UUID userId, Pageable pageable) {
        return activityRepository.findByTenantIdAndAssignedTo(tenantId, userId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findOverdueActivities(UUID tenantId, Pageable pageable) {
        return activityRepository.findOverdueActivities(tenantId, LocalDateTime.now(), pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findUpcomingActivities(UUID tenantId, int daysAhead, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(daysAhead);
        return activityRepository.findUpcomingActivities(tenantId, now, endDate, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> findPendingActivitiesForUser(UUID tenantId, UUID userId, Pageable pageable) {
        return activityRepository.findPendingActivitiesForUser(tenantId, userId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Long countPendingActivities(UUID tenantId) {
        return activityRepository.countPendingActivities(tenantId);
    }

    @Transactional
    public ActivityDTO logCall(UUID tenantId, ActivityDTO dto) {
        dto.setActivityType(ActivityType.CALL);
        return create(tenantId, dto);
    }

    @Transactional
    public ActivityDTO logEmail(UUID tenantId, ActivityDTO dto) {
        dto.setActivityType(ActivityType.EMAIL);
        return create(tenantId, dto);
    }

    @Transactional
    public ActivityDTO logMeeting(UUID tenantId, ActivityDTO dto) {
        dto.setActivityType(ActivityType.MEETING);
        return create(tenantId, dto);
    }

    @Transactional
    public ActivityDTO scheduleDemo(UUID tenantId, ActivityDTO dto) {
        dto.setActivityType(ActivityType.DEMO);
        return create(tenantId, dto);
    }

    @Transactional
    public ActivityDTO createTask(UUID tenantId, ActivityDTO dto) {
        dto.setActivityType(ActivityType.TASK);
        return create(tenantId, dto);
    }

    private ActivityDTO toDTO(Activity entity) {
        return ActivityDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .leadId(entity.getLeadId())
            .contactId(entity.getContactId())
            .accountId(entity.getAccountId())
            .opportunityId(entity.getOpportunityId())
            .activityType(entity.getActivityType())
            .subject(entity.getSubject())
            .description(entity.getDescription())
            .outcome(entity.getOutcome())
            .scheduledAt(entity.getScheduledAt())
            .completedAt(entity.getCompletedAt())
            .durationMinutes(entity.getDurationMinutes())
            .isCompleted(entity.getCompleted())
            .assignedToUserId(entity.getAssignedToUserId())
            .zohoActivityId(entity.getZohoActivityId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Activity toEntity(ActivityDTO dto) {
        Activity entity = new Activity();
        entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
        entity.setTenantId(dto.getTenantId());
        entity.setLeadId(dto.getLeadId());
        entity.setContactId(dto.getContactId());
        entity.setAccountId(dto.getAccountId());
        entity.setOpportunityId(dto.getOpportunityId());
        entity.setActivityType(dto.getActivityType());
        entity.setSubject(dto.getSubject());
        entity.setDescription(dto.getDescription());
        entity.setOutcome(dto.getOutcome());
        entity.setScheduledAt(dto.getScheduledAt());
        entity.setDurationMinutes(dto.getDurationMinutes());
        entity.setCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
        entity.setAssignedToUserId(dto.getAssignedToUserId());
        return entity;
    }

    private void updateEntity(Activity entity, ActivityDTO dto) {
        if (dto.getLeadId() != null) entity.setLeadId(dto.getLeadId());
        if (dto.getContactId() != null) entity.setContactId(dto.getContactId());
        if (dto.getAccountId() != null) entity.setAccountId(dto.getAccountId());
        if (dto.getOpportunityId() != null) entity.setOpportunityId(dto.getOpportunityId());
        if (dto.getActivityType() != null) entity.setActivityType(dto.getActivityType());
        if (dto.getSubject() != null) entity.setSubject(dto.getSubject());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getOutcome() != null) entity.setOutcome(dto.getOutcome());
        if (dto.getScheduledAt() != null) entity.setScheduledAt(dto.getScheduledAt());
        if (dto.getDurationMinutes() != null) entity.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getIsCompleted() != null) entity.setCompleted(dto.getIsCompleted());
        if (dto.getAssignedToUserId() != null) entity.setAssignedToUserId(dto.getAssignedToUserId());
    }
}
