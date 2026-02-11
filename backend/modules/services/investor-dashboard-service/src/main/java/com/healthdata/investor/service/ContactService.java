package com.healthdata.investor.service;

import com.healthdata.investor.dto.ActivityDTO;
import com.healthdata.investor.dto.ContactDTO;
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
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing investor contacts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final InvestorContactRepository contactRepository;
    private final OutreachActivityRepository activityRepository;

    // Pattern to extract LinkedIn profile ID from URL
    private static final Pattern LINKEDIN_PROFILE_PATTERN =
            Pattern.compile("linkedin\\.com/in/([a-zA-Z0-9-]+)/?");

    public List<ContactDTO> getAllContacts() {
        return contactRepository.findAllByOrderByTierAscNameAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ContactDTO getContact(UUID id) {
        InvestorContact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", id.toString()));
        return toDTOWithActivities(contact);
    }

    public List<ContactDTO> getContactsByCategory(String category) {
        return contactRepository.findByCategoryOrderByNameAsc(category)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContactDTO> getContactsByStatus(String status) {
        return contactRepository.findByStatusOrderByNameAsc(status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContactDTO> getContactsByTier(String tier) {
        return contactRepository.findByTierOrderByNameAsc(tier)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContactDTO> searchContacts(String query) {
        return contactRepository.findByNameContainingIgnoreCaseOrOrganizationContainingIgnoreCase(query, query)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContactDTO> getContactsNeedingFollowUp() {
        return contactRepository.findContactsNeedingFollowUp()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactDTO createContact(ContactDTO.CreateRequest request) {
        InvestorContact contact = InvestorContact.builder()
                .name(request.getName())
                .title(request.getTitle())
                .organization(request.getOrganization())
                .email(request.getEmail())
                .phone(request.getPhone())
                .linkedInUrl(request.getLinkedInUrl())
                .linkedInProfileId(extractLinkedInProfileId(request.getLinkedInUrl()))
                .category(request.getCategory())
                .tier(request.getTier() != null ? request.getTier() : "B")
                .investmentThesis(request.getInvestmentThesis())
                .notes(request.getNotes())
                .status("identified")
                .build();

        contact = contactRepository.save(contact);
        log.info("Created contact: {} with ID: {}", contact.getName(), contact.getId());
        return toDTO(contact);
    }

    @Transactional
    public ContactDTO updateContact(UUID id, ContactDTO.UpdateRequest request) {
        InvestorContact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", id.toString()));

        if (request.getName() != null) contact.setName(request.getName());
        if (request.getTitle() != null) contact.setTitle(request.getTitle());
        if (request.getOrganization() != null) contact.setOrganization(request.getOrganization());
        if (request.getEmail() != null) contact.setEmail(request.getEmail());
        if (request.getPhone() != null) contact.setPhone(request.getPhone());
        if (request.getLinkedInUrl() != null) {
            contact.setLinkedInUrl(request.getLinkedInUrl());
            contact.setLinkedInProfileId(extractLinkedInProfileId(request.getLinkedInUrl()));
        }
        if (request.getCategory() != null) contact.setCategory(request.getCategory());
        if (request.getStatus() != null) contact.setStatus(request.getStatus());
        if (request.getTier() != null) contact.setTier(request.getTier());
        if (request.getInvestmentThesis() != null) contact.setInvestmentThesis(request.getInvestmentThesis());
        if (request.getNotes() != null) contact.setNotes(request.getNotes());
        if (request.getNextFollowUp() != null) contact.setNextFollowUp(request.getNextFollowUp());

        contact = contactRepository.save(contact);
        log.info("Updated contact: {}", contact.getId());
        return toDTO(contact);
    }

    @Transactional
    public void deleteContact(UUID id) {
        if (!contactRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contact", id.toString());
        }
        contactRepository.deleteById(id);
        log.info("Deleted contact: {}", id);
    }

    @Transactional
    public void updateLastContacted(UUID id) {
        InvestorContact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", id.toString()));
        contact.setLastContacted(Instant.now());
        contactRepository.save(contact);
    }

    private String extractLinkedInProfileId(String linkedInUrl) {
        if (linkedInUrl == null || linkedInUrl.isBlank()) {
            return null;
        }
        Matcher matcher = LINKEDIN_PROFILE_PATTERN.matcher(linkedInUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private ContactDTO toDTO(InvestorContact contact) {
        return ContactDTO.builder()
                .id(contact.getId())
                .name(contact.getName())
                .title(contact.getTitle())
                .organization(contact.getOrganization())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .linkedInUrl(contact.getLinkedInUrl())
                .linkedInProfileId(contact.getLinkedInProfileId())
                .category(contact.getCategory())
                .status(contact.getStatus())
                .tier(contact.getTier())
                .investmentThesis(contact.getInvestmentThesis())
                .notes(contact.getNotes())
                .lastContacted(contact.getLastContacted())
                .nextFollowUp(contact.getNextFollowUp())
                .activityCount(contact.getActivityCount())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private ContactDTO toDTOWithActivities(InvestorContact contact) {
        List<ActivityDTO> recentActivities = activityRepository.findByContactIdOrderByActivityDateDesc(contact.getId())
                .stream()
                .limit(5)
                .map(this::toActivityDTO)
                .collect(Collectors.toList());

        ContactDTO dto = toDTO(contact);
        dto.setRecentActivities(recentActivities);
        dto.setActivityCount((int) activityRepository.countByContactId(contact.getId()));
        return dto;
    }

    private ActivityDTO toActivityDTO(OutreachActivity activity) {
        return ActivityDTO.builder()
                .id(activity.getId())
                .contactId(activity.getContact().getId())
                .activityType(activity.getActivityType())
                .status(activity.getStatus())
                .subject(activity.getSubject())
                .content(activity.getContent())
                .activityDate(activity.getActivityDate())
                .scheduledTime(activity.getScheduledTime())
                .responseReceived(activity.getResponseReceived())
                .notes(activity.getNotes())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
