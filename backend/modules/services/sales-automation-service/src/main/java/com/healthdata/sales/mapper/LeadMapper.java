package com.healthdata.sales.mapper;

import com.healthdata.sales.dto.LeadCaptureRequest;
import com.healthdata.sales.dto.LeadDTO;
import com.healthdata.sales.entity.Lead;
import com.healthdata.sales.entity.LeadStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LeadMapper {

    public LeadDTO toDTO(Lead entity) {
        if (entity == null) return null;

        return LeadDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .company(entity.getCompany())
            .title(entity.getTitle())
            .website(entity.getWebsite())
            .source(entity.getSource())
            .status(entity.getStatus())
            .organizationType(entity.getOrganizationType())
            .patientCount(entity.getPatientCount())
            .ehrCount(entity.getEhrCount())
            .state(entity.getState())
            .score(entity.getScore())
            .notes(entity.getNotes())
            .zohoLeadId(entity.getZohoLeadId())
            .assignedToUserId(entity.getAssignedToUserId())
            .lastContactedAt(entity.getLastContactedAt())
            .convertedAt(entity.getConvertedAt())
            .convertedContactId(entity.getConvertedContactId())
            .convertedOpportunityId(entity.getConvertedOpportunityId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public Lead toEntity(LeadDTO dto) {
        if (dto == null) return null;

        Lead entity = new Lead();
        entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
        entity.setTenantId(dto.getTenantId());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setCompany(dto.getCompany());
        entity.setTitle(dto.getTitle());
        entity.setWebsite(dto.getWebsite());
        entity.setSource(dto.getSource());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : LeadStatus.NEW);
        entity.setOrganizationType(dto.getOrganizationType());
        entity.setPatientCount(dto.getPatientCount());
        entity.setEhrCount(dto.getEhrCount());
        entity.setState(dto.getState());
        entity.setNotes(dto.getNotes());
        entity.setAssignedToUserId(dto.getAssignedToUserId());
        return entity;
    }

    public Lead fromCaptureRequest(LeadCaptureRequest request, UUID tenantId) {
        Lead entity = new Lead();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setCompany(request.getCompany());
        entity.setTitle(request.getTitle());
        entity.setWebsite(request.getWebsite());
        entity.setSource(request.getSource());
        entity.setStatus(LeadStatus.NEW);
        entity.setOrganizationType(request.getOrganizationType());
        entity.setPatientCount(request.getPatientCount());
        entity.setEhrCount(request.getEhrCount());
        entity.setState(request.getState());
        entity.setNotes(request.getNotes());
        return entity;
    }

    public void updateEntity(Lead entity, LeadDTO dto) {
        if (dto.getFirstName() != null) entity.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) entity.setLastName(dto.getLastName());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getCompany() != null) entity.setCompany(dto.getCompany());
        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getWebsite() != null) entity.setWebsite(dto.getWebsite());
        if (dto.getSource() != null) entity.setSource(dto.getSource());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getOrganizationType() != null) entity.setOrganizationType(dto.getOrganizationType());
        if (dto.getPatientCount() != null) entity.setPatientCount(dto.getPatientCount());
        if (dto.getEhrCount() != null) entity.setEhrCount(dto.getEhrCount());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getAssignedToUserId() != null) entity.setAssignedToUserId(dto.getAssignedToUserId());
    }
}
