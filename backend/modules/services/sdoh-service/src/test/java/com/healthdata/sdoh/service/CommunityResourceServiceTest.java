package com.healthdata.sdoh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.CommunityResourceEntity;
import com.healthdata.sdoh.entity.ResourceReferralEntity;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.CommunityResourceRepository;
import com.healthdata.sdoh.repository.ResourceReferralRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for CommunityResourceService
 *
 * Testing community resource directory integration and referral management
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Community Resource Service Tests")
class CommunityResourceServiceTest {

    @Mock
    private CommunityResourceRepository resourceRepository;

    @Mock
    private ResourceReferralRepository referralRepository;

    private CommunityResourceService resourceService;

    private String tenantId;
    private String patientId;
    private CommunityResourceEntity foodResourceEntity;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        resourceService = new CommunityResourceService(resourceRepository, referralRepository, objectMapper);

        tenantId = "tenant-001";
        patientId = "patient-001";

        foodResourceEntity = CommunityResourceEntity.builder()
                .resourceId("resource-001")
                .organizationName("Local Food Bank")
                .category(ResourceCategory.FOOD)
                .address("123 Main St")
                .city("Boston")
                .state("MA")
                .zipCode("02101")
                .phoneNumber("617-555-1234")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should search resources by category")
    void testSearchByCategory() {
        // Given
        ResourceCategory category = ResourceCategory.FOOD;
        List<CommunityResourceEntity> entities = Arrays.asList(foodResourceEntity);

        when(resourceRepository.findByCategory(category)).thenReturn(entities);

        // When
        List<CommunityResource> result = resourceService.searchByCategory(category);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(category, result.get(0).getCategory());
    }

    @Test
    @DisplayName("Should search resources by location")
    void testSearchByLocation() {
        // Given
        String city = "Boston";
        String state = "MA";
        List<CommunityResourceEntity> entities = Arrays.asList(foodResourceEntity);

        when(resourceRepository.findByCityAndState(city, state)).thenReturn(entities);

        // When
        List<CommunityResource> result = resourceService.searchByLocation(city, state);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(city, result.get(0).getCity());
    }

    @Test
    @DisplayName("Should search resources by ZIP code")
    void testSearchByZipCode() {
        // Given
        String zipCode = "02101";
        List<CommunityResourceEntity> entities = Arrays.asList(foodResourceEntity);

        when(resourceRepository.findByZipCode(zipCode)).thenReturn(entities);

        // When
        List<CommunityResource> result = resourceService.searchByZipCode(zipCode);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(zipCode, result.get(0).getZipCode());
    }

    @Test
    @DisplayName("Should find nearby resources within radius")
    void testFindNearby() {
        // Given
        double latitude = 42.3601;
        double longitude = -71.0589;
        double radiusMiles = 10.0;
        foodResourceEntity.setLatitude(latitude);
        foodResourceEntity.setLongitude(longitude);

        when(resourceRepository.findWithinRadius(eq(latitude), eq(longitude), anyDouble()))
                .thenReturn(Arrays.asList(foodResourceEntity));

        // When
        List<CommunityResource> result = resourceService.findNearby(latitude, longitude, radiusMiles);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should create referral for patient")
    void testCreateReferral() {
        // Given
        ResourceReferralEntity savedEntity = ResourceReferralEntity.builder()
                .referralId("referral-001")
                .patientId(patientId)
                .tenantId(tenantId)
                .resourceId("resource-001")
                .category(SdohCategory.FOOD_INSECURITY)
                .referralReason("Patient needs food assistance")
                .status(ResourceReferral.ReferralStatus.PENDING)
                .referredBy("Dr. Smith")
                .referralDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(referralRepository.save(any(ResourceReferralEntity.class))).thenReturn(savedEntity);

        // When
        ResourceReferral result = resourceService.createReferral(
                tenantId, patientId, "resource-001",
                SdohCategory.FOOD_INSECURITY, "Patient needs food assistance", "Dr. Smith");

        // Then
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertEquals(patientId, result.getPatientId());
        verify(referralRepository).save(any(ResourceReferralEntity.class));
    }

    @Test
    @DisplayName("Should get patient referrals")
    void testGetPatientReferrals() {
        // Given
        ResourceReferralEntity referralEntity = ResourceReferralEntity.builder()
                .referralId("referral-001")
                .patientId(patientId)
                .tenantId(tenantId)
                .resourceId("resource-001")
                .category(SdohCategory.FOOD_INSECURITY)
                .status(ResourceReferral.ReferralStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(referralRepository.findByTenantIdAndPatientId(tenantId, patientId))
                .thenReturn(Arrays.asList(referralEntity));

        // When
        List<ResourceReferral> result = resourceService.getPatientReferrals(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(patientId, result.get(0).getPatientId());
    }

    @Test
    @DisplayName("Should update referral status")
    void testUpdateReferralStatus() {
        // Given
        String referralId = "referral-001";
        ResourceReferralEntity referralEntity = ResourceReferralEntity.builder()
                .referralId(referralId)
                .patientId(patientId)
                .tenantId(tenantId)
                .status(ResourceReferral.ReferralStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(referralRepository.findById(referralId)).thenReturn(Optional.of(referralEntity));

        // When
        resourceService.updateReferralStatus(referralId, ResourceReferral.ReferralStatus.CONTACTED);

        // Then
        verify(referralRepository).save(any(ResourceReferralEntity.class));
    }

    @Test
    @DisplayName("Should calculate distance between coordinates")
    void testCalculateDistance() {
        // Given: Boston to Cambridge coordinates
        double lat1 = 42.3601;
        double lon1 = -71.0589;
        double lat2 = 42.3736;
        double lon2 = -71.1097;

        // When
        double distance = resourceService.calculateDistance(lat1, lon1, lat2, lon2);

        // Then
        assertTrue(distance > 0);
        assertTrue(distance < 5); // Should be about 3 miles
    }

    @Test
    @DisplayName("Should add new community resource")
    void testAddResource() {
        // Given
        CommunityResource newResource = CommunityResource.builder()
                .organizationName("New Food Pantry")
                .category(ResourceCategory.FOOD)
                .city("Cambridge")
                .state("MA")
                .zipCode("02139")
                .build();

        CommunityResourceEntity savedEntity = CommunityResourceEntity.builder()
                .resourceId("resource-002")
                .organizationName("New Food Pantry")
                .category(ResourceCategory.FOOD)
                .city("Cambridge")
                .state("MA")
                .zipCode("02139")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(resourceRepository.save(any(CommunityResourceEntity.class))).thenReturn(savedEntity);

        // When
        CommunityResource result = resourceService.addResource(newResource);

        // Then
        assertNotNull(result);
        assertEquals("New Food Pantry", result.getOrganizationName());
        verify(resourceRepository).save(any(CommunityResourceEntity.class));
    }

    @Test
    @DisplayName("Should find walk-in resources")
    void testFindWalkInResources() {
        // Given
        foodResourceEntity.setAcceptsWalkIns(true);
        when(resourceRepository.findByAcceptsWalkIns(true)).thenReturn(Arrays.asList(foodResourceEntity));

        // When
        List<CommunityResource> result = resourceService.findWalkInResources();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should find resources that don't require referral")
    void testFindNoReferralResources() {
        // Given
        foodResourceEntity.setRequiresReferral(false);
        when(resourceRepository.findByRequiresReferral(false)).thenReturn(Arrays.asList(foodResourceEntity));

        // When
        List<CommunityResource> result = resourceService.findNoReferralResources();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
