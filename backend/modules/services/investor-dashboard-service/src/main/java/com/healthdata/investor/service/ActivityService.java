package com.healthdata.investor.service;

import com.healthdata.investor.dto.ActivityDTO;
import com.healthdata.investor.entity.InvestorContact;
import com.healthdata.investor.entity.OutreachActivity;
import com.healthdata.investor.exception.ResourceNotFoundException;
import com.healthdata.investor.repository.InvestorContactRepository;
import com.healthdata.investor.repository.OutreachActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing outreach activities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final OutreachActivityRepository activityRepository;
    private final InvestorContactRepository contactRepository;
    private final ContactService contactService;

    public List<ActivityDTO> getAllActivities() {
        return activityRepository.findAllByOrderByActivityDateDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ActivityDTO getActivity(UUID id) {
        return activityRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id.toString()));
    }

    public List<ActivityDTO> getActivitiesByContact(UUID contactId) {
        return activityRepository.findByContactIdOrderByActivityDateDesc(contactId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityDTO> getActivitiesByType(String activityType) {
        return activityRepository.findByActivityTypeOrderByActivityDateDesc(activityType)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityDTO> getActivitiesByDateRange(LocalDate start, LocalDate end) {
        return activityRepository.findByActivityDateBetweenOrderByActivityDateDesc(start, end)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityDTO> getLinkedInActivities() {
        return activityRepository.findLinkedInActivities()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityDTO> getPendingScheduledActivities() {
        return activityRepository.findPendingScheduledActivities()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityDTO createActivity(ActivityDTO.CreateRequest request, UUID createdBy) {
        InvestorContact contact = contactRepository.findById(request.getContactId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact", request.getContactId().toString()));

        OutreachActivity activity = OutreachActivity.builder()
                .contact(contact)
                .activityType(request.getActivityType())
                .subject(request.getSubject())
                .content(request.getContent())
                .activityDate(request.getActivityDate())
                .scheduledTime(request.getScheduledTime())
                .notes(request.getNotes())
                .status("pending")
                .createdBy(createdBy)
                .build();

        // Set LinkedIn-specific fields
        if (request.getActivityType().startsWith("linkedin_")) {
            activity.setLinkedInConnectionStatus("pending");
        }

        activity = activityRepository.save(activity);

        // Update contact's last contacted timestamp
        contactService.updateLastContacted(contact.getId());

        log.info("Created activity: {} for contact: {}", activity.getActivityType(), contact.getName());
        return toDTO(activity);
    }

    @Transactional
    public ActivityDTO updateActivity(UUID id, ActivityDTO.UpdateRequest request) {
        OutreachActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id.toString()));

        if (request.getActivityType() != null) activity.setActivityType(request.getActivityType());
        if (request.getStatus() != null) activity.setStatus(request.getStatus());
        if (request.getSubject() != null) activity.setSubject(request.getSubject());
        if (request.getContent() != null) activity.setContent(request.getContent());
        if (request.getActivityDate() != null) activity.setActivityDate(request.getActivityDate());
        if (request.getScheduledTime() != null) activity.setScheduledTime(request.getScheduledTime());
        if (request.getResponseReceived() != null) activity.setResponseReceived(request.getResponseReceived());
        if (request.getResponseContent() != null) activity.setResponseContent(request.getResponseContent());
        if (request.getNotes() != null) activity.setNotes(request.getNotes());

        activity = activityRepository.save(activity);
        log.info("Updated activity: {}", activity.getId());
        return toDTO(activity);
    }

    @Transactional
    public ActivityDTO markAsResponded(UUID id, String responseContent) {
        OutreachActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id.toString()));

        activity.setStatus("responded");
        activity.setResponseReceived(Instant.now());
        activity.setResponseContent(responseContent);

        if (activity.isLinkedInActivity()) {
            activity.setLinkedInConnectionStatus("connected");
        }

        activity = activityRepository.save(activity);

        // Update contact status to engaged
        InvestorContact contact = activity.getContact();
        if ("contacted".equals(contact.getStatus())) {
            contact.setStatus("engaged");
            contactRepository.save(contact);
        }

        log.info("Marked activity {} as responded", id);
        return toDTO(activity);
    }

    @Transactional
    public void deleteActivity(UUID id) {
        if (!activityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Activity", id.toString());
        }
        activityRepository.deleteById(id);
        log.info("Deleted activity: {}", id);
    }

    private ActivityDTO toDTO(OutreachActivity activity) {
        return ActivityDTO.builder()
                .id(activity.getId())
                .contactId(activity.getContact().getId())
                .contactName(activity.getContact().getName())
                .activityType(activity.getActivityType())
                .status(activity.getStatus())
                .subject(activity.getSubject())
                .content(activity.getContent())
                .activityDate(activity.getActivityDate())
                .scheduledTime(activity.getScheduledTime())
                .responseReceived(activity.getResponseReceived())
                .responseContent(activity.getResponseContent())
                .notes(activity.getNotes())
                .linkedInMessageId(activity.getLinkedInMessageId())
                .linkedInConnectionStatus(activity.getLinkedInConnectionStatus())
                .createdBy(activity.getCreatedBy())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
