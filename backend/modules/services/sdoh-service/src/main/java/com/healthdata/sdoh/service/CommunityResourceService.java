package com.healthdata.sdoh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.CommunityResourceEntity;
import com.healthdata.sdoh.entity.ResourceReferralEntity;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.CommunityResourceRepository;
import com.healthdata.sdoh.repository.ResourceReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Community Resource Directory Integration Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityResourceService {

    private final CommunityResourceRepository resourceRepository;
    private final ResourceReferralRepository referralRepository;
    private final ObjectMapper objectMapper;

    public List<CommunityResource> searchByCategory(ResourceCategory category) {
        return resourceRepository.findByCategory(category).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public List<CommunityResource> searchByLocation(String city, String state) {
        return resourceRepository.findByCityAndState(city, state).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public List<CommunityResource> searchByZipCode(String zipCode) {
        return resourceRepository.findByZipCode(zipCode).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public List<CommunityResource> findNearby(double latitude, double longitude, double radiusMiles) {
        double radiusKm = radiusMiles * 1.60934;
        return resourceRepository.findWithinRadius(latitude, longitude, radiusKm).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResourceReferral createReferral(String tenantId, String patientId, String resourceId,
                                          SdohCategory category, String referralReason, String referredBy) {
        ResourceReferral referral = ResourceReferral.builder()
                .patientId(patientId)
                .tenantId(tenantId)
                .resourceId(resourceId)
                .category(category)
                .referralReason(referralReason)
                .status(ResourceReferral.ReferralStatus.PENDING)
                .referredBy(referredBy)
                .referralDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResourceReferralEntity entity = convertToEntity(referral);
        entity = referralRepository.save(entity);

        return convertToModel(entity);
    }

    public List<ResourceReferral> getPatientReferrals(String tenantId, String patientId) {
        return referralRepository.findByTenantIdAndPatientId(tenantId, patientId).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateReferralStatus(String referralId, ResourceReferral.ReferralStatus status) {
        referralRepository.findById(referralId).ifPresent(entity -> {
            entity.setStatus(status);
            referralRepository.save(entity);
        });
    }

    public List<ResourceReferral> getActiveReferrals(String tenantId, String patientId) {
        return referralRepository.findActiveByTenantIdAndPatientId(tenantId, patientId).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommunityResource addResource(CommunityResource resource) {
        CommunityResourceEntity entity = convertToEntity(resource);
        entity = resourceRepository.save(entity);
        return convertToModel(entity);
    }

    @Transactional
    public CommunityResource updateResource(String resourceId, CommunityResource resource) {
        return resourceRepository.findById(resourceId).map(entity -> {
            updateEntityFromModel(entity, resource);
            return convertToModel(resourceRepository.save(entity));
        }).orElse(null);
    }

    @Transactional
    public void deleteResource(String resourceId) {
        resourceRepository.deleteById(resourceId);
    }

    public Optional<CommunityResource> getResourceById(String resourceId) {
        return resourceRepository.findById(resourceId).map(this::convertToModel);
    }

    public List<CommunityResource> findWalkInResources() {
        return resourceRepository.findByAcceptsWalkIns(true).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public List<CommunityResource> findNoReferralResources() {
        return resourceRepository.findByRequiresReferral(false).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 3959; // Radius of Earth in miles
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private CommunityResourceEntity convertToEntity(CommunityResource resource) {
        try {
            return CommunityResourceEntity.builder()
                    .resourceId(resource.getResourceId())
                    .organizationName(resource.getOrganizationName())
                    .category(resource.getCategory())
                    .description(resource.getDescription())
                    .address(resource.getAddress())
                    .city(resource.getCity())
                    .state(resource.getState())
                    .zipCode(resource.getZipCode())
                    .phoneNumber(resource.getPhoneNumber())
                    .email(resource.getEmail())
                    .website(resource.getWebsite())
                    .servicesProvidedJson(resource.getServicesProvided() != null ?
                            objectMapper.writeValueAsString(resource.getServicesProvided()) : null)
                    .eligibilityCriteria(resource.getEligibilityCriteria())
                    .hoursOfOperation(resource.getHoursOfOperation())
                    .latitude(resource.getLatitude())
                    .longitude(resource.getLongitude())
                    .acceptsWalkIns(resource.isAcceptsWalkIns())
                    .requiresReferral(resource.isRequiresReferral())
                    .language(resource.getLanguage())
                    .createdAt(resource.getCreatedAt() != null ? resource.getCreatedAt() : LocalDateTime.now())
                    .updatedAt(resource.getUpdatedAt() != null ? resource.getUpdatedAt() : LocalDateTime.now())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting resource to entity", e);
        }
    }

    @SuppressWarnings("unchecked")
    private CommunityResource convertToModel(CommunityResourceEntity entity) {
        try {
            return CommunityResource.builder()
                    .resourceId(entity.getResourceId())
                    .organizationName(entity.getOrganizationName())
                    .category(entity.getCategory())
                    .description(entity.getDescription())
                    .address(entity.getAddress())
                    .city(entity.getCity())
                    .state(entity.getState())
                    .zipCode(entity.getZipCode())
                    .phoneNumber(entity.getPhoneNumber())
                    .email(entity.getEmail())
                    .website(entity.getWebsite())
                    .servicesProvided(entity.getServicesProvidedJson() != null ?
                            objectMapper.readValue(entity.getServicesProvidedJson(), List.class) : null)
                    .eligibilityCriteria(entity.getEligibilityCriteria())
                    .hoursOfOperation(entity.getHoursOfOperation())
                    .latitude(entity.getLatitude())
                    .longitude(entity.getLongitude())
                    .acceptsWalkIns(entity.isAcceptsWalkIns())
                    .requiresReferral(entity.isRequiresReferral())
                    .language(entity.getLanguage())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting entity to resource", e);
        }
    }

    private ResourceReferralEntity convertToEntity(ResourceReferral referral) {
        return ResourceReferralEntity.builder()
                .referralId(referral.getReferralId())
                .patientId(referral.getPatientId())
                .tenantId(referral.getTenantId())
                .resourceId(referral.getResourceId())
                .category(referral.getCategory())
                .referralReason(referral.getReferralReason())
                .status(referral.getStatus())
                .referredBy(referral.getReferredBy())
                .referralDate(referral.getReferralDate())
                .contactDate(referral.getContactDate())
                .outcome(referral.getOutcome())
                .notes(referral.getNotes())
                .createdAt(referral.getCreatedAt())
                .updatedAt(referral.getUpdatedAt())
                .build();
    }

    private ResourceReferral convertToModel(ResourceReferralEntity entity) {
        return ResourceReferral.builder()
                .referralId(entity.getReferralId())
                .patientId(entity.getPatientId())
                .tenantId(entity.getTenantId())
                .resourceId(entity.getResourceId())
                .category(entity.getCategory())
                .referralReason(entity.getReferralReason())
                .status(entity.getStatus())
                .referredBy(entity.getReferredBy())
                .referralDate(entity.getReferralDate())
                .contactDate(entity.getContactDate())
                .outcome(entity.getOutcome())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void updateEntityFromModel(CommunityResourceEntity entity, CommunityResource resource) {
        try {
            entity.setOrganizationName(resource.getOrganizationName());
            entity.setCategory(resource.getCategory());
            entity.setDescription(resource.getDescription());
            entity.setAddress(resource.getAddress());
            entity.setCity(resource.getCity());
            entity.setState(resource.getState());
            entity.setZipCode(resource.getZipCode());
            entity.setPhoneNumber(resource.getPhoneNumber());
            entity.setEmail(resource.getEmail());
            entity.setWebsite(resource.getWebsite());
            if (resource.getServicesProvided() != null) {
                entity.setServicesProvidedJson(objectMapper.writeValueAsString(resource.getServicesProvided()));
            }
            entity.setEligibilityCriteria(resource.getEligibilityCriteria());
            entity.setHoursOfOperation(resource.getHoursOfOperation());
            entity.setLatitude(resource.getLatitude());
            entity.setLongitude(resource.getLongitude());
            entity.setAcceptsWalkIns(resource.isAcceptsWalkIns());
            entity.setRequiresReferral(resource.isRequiresReferral());
            entity.setLanguage(resource.getLanguage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error updating entity from model", e);
        }
    }
}
