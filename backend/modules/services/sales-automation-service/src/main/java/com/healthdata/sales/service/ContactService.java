package com.healthdata.sales.service;

import com.healthdata.sales.dto.ContactDTO;
import com.healthdata.sales.entity.Contact;
import com.healthdata.sales.entity.ContactType;
import com.healthdata.sales.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    @Transactional(readOnly = true)
    public Page<ContactDTO> findAll(UUID tenantId, Pageable pageable) {
        return contactRepository.findByTenantId(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public ContactDTO findById(UUID tenantId, UUID id) {
        Contact contact = contactRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + id));
        return toDTO(contact);
    }

    @Transactional
    public ContactDTO create(UUID tenantId, ContactDTO dto) {
        dto.setTenantId(tenantId);
        Contact contact = toEntity(dto);
        contact = contactRepository.save(contact);
        log.info("Created contact {} for tenant {}", contact.getId(), tenantId);
        return toDTO(contact);
    }

    @Transactional
    public ContactDTO update(UUID tenantId, UUID id, ContactDTO dto) {
        Contact contact = contactRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + id));

        updateEntity(contact, dto);
        contact = contactRepository.save(contact);
        log.info("Updated contact {}", contact.getId());
        return toDTO(contact);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Contact contact = contactRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + id));
        contactRepository.delete(contact);
        log.info("Deleted contact {}", id);
    }

    @Transactional(readOnly = true)
    public Page<ContactDTO> findByAccount(UUID tenantId, UUID accountId, Pageable pageable) {
        return contactRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ContactDTO> findAllByAccount(UUID accountId) {
        return contactRepository.findByAccountId(accountId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContactDTO findPrimaryContact(UUID accountId) {
        return contactRepository.findPrimaryContactByAccountId(accountId)
            .map(this::toDTO)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<ContactDTO> findByContactType(UUID tenantId, ContactType type, Pageable pageable) {
        return contactRepository.findByTenantIdAndContactType(tenantId, type, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ContactDTO> search(UUID tenantId, String query, Pageable pageable) {
        return contactRepository.searchContacts(tenantId, query, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ContactDTO> findContactableContacts(UUID tenantId, Pageable pageable) {
        return contactRepository.findContactableContacts(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional
    public ContactDTO setPrimaryContact(UUID tenantId, UUID accountId, UUID contactId) {
        // First, unset any existing primary contact
        List<Contact> accountContacts = contactRepository.findByAccountId(accountId);
        for (Contact c : accountContacts) {
            if (Boolean.TRUE.equals(c.getPrimary())) {
                c.setPrimary(false);
                contactRepository.save(c);
            }
        }

        // Set the new primary contact
        Contact contact = contactRepository.findByIdAndTenantId(contactId, tenantId)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        if (!accountId.equals(contact.getAccountId())) {
            throw new RuntimeException("Contact does not belong to the specified account");
        }

        contact.setPrimary(true);
        contact = contactRepository.save(contact);
        log.info("Set contact {} as primary for account {}", contactId, accountId);
        return toDTO(contact);
    }

    @Transactional
    public ContactDTO recordContactActivity(UUID tenantId, UUID id) {
        Contact contact = contactRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + id));

        contact.setLastContactedAt(LocalDateTime.now());
        contact = contactRepository.save(contact);
        return toDTO(contact);
    }

    private ContactDTO toDTO(Contact entity) {
        return ContactDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .accountId(entity.getAccountId())
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .mobile(entity.getMobile())
            .title(entity.getTitle())
            .department(entity.getDepartment())
            .contactType(entity.getContactType())
            .isPrimary(entity.getPrimary())
            .doNotCall(entity.getDoNotCall())
            .doNotEmail(entity.getDoNotEmail())
            .linkedinUrl(entity.getLinkedinUrl())
            .notes(entity.getNotes())
            .zohoContactId(entity.getZohoContactId())
            .ownerUserId(entity.getOwnerUserId())
            .lastContactedAt(entity.getLastContactedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Contact toEntity(ContactDTO dto) {
        Contact entity = new Contact();
        entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
        entity.setTenantId(dto.getTenantId());
        entity.setAccountId(dto.getAccountId());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setMobile(dto.getMobile());
        entity.setTitle(dto.getTitle());
        entity.setDepartment(dto.getDepartment());
        entity.setContactType(dto.getContactType());
        entity.setPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        entity.setDoNotCall(dto.getDoNotCall() != null ? dto.getDoNotCall() : false);
        entity.setDoNotEmail(dto.getDoNotEmail() != null ? dto.getDoNotEmail() : false);
        entity.setLinkedinUrl(dto.getLinkedinUrl());
        entity.setNotes(dto.getNotes());
        entity.setOwnerUserId(dto.getOwnerUserId());
        return entity;
    }

    private void updateEntity(Contact entity, ContactDTO dto) {
        if (dto.getAccountId() != null) entity.setAccountId(dto.getAccountId());
        if (dto.getFirstName() != null) entity.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) entity.setLastName(dto.getLastName());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getMobile() != null) entity.setMobile(dto.getMobile());
        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getDepartment() != null) entity.setDepartment(dto.getDepartment());
        if (dto.getContactType() != null) entity.setContactType(dto.getContactType());
        if (dto.getIsPrimary() != null) entity.setPrimary(dto.getIsPrimary());
        if (dto.getDoNotCall() != null) entity.setDoNotCall(dto.getDoNotCall());
        if (dto.getDoNotEmail() != null) entity.setDoNotEmail(dto.getDoNotEmail());
        if (dto.getLinkedinUrl() != null) entity.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getOwnerUserId() != null) entity.setOwnerUserId(dto.getOwnerUserId());
    }
}
