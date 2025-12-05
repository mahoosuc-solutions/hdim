package com.healthdata.sdoh.service;

import com.healthdata.sdoh.model.CommunityResource;
import com.healthdata.sdoh.model.ResourceCategory;
import com.healthdata.sdoh.model.ResourceReferral;
import com.healthdata.sdoh.model.SdohCategory;
import com.healthdata.sdoh.repository.CommunityResourceRepository;
import com.healthdata.sdoh.repository.ResourceReferralRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for CommunityResourceService
 *
 * Testing community resource directory integration and referral management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Community Resource Service Tests")
class CommunityResourceServiceTest {

    @Mock
    private CommunityResourceRepository resourceRepository;

    @Mock
    private ResourceReferralRepository referralRepository;

    @InjectMocks
    private CommunityResourceService resourceService;

    private String tenantId;
    private String patientId;
    private CommunityResource foodResource;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        patientId = "patient-001";

        foodResource = CommunityResource.builder()
                .resourceId("resource-001")
                .organizationName("Local Food Bank")
                .category(ResourceCategory.FOOD)
                .address("123 Main St")
                .city("Boston")
                .state("MA")
                .zipCode("02101")
                .phoneNumber("617-555-1234")
                .build();
    }

    @Test
    @DisplayName("Should search resources by category")
    void testSearchByCategory() {
        // Given
        ResourceCategory category = ResourceCategory.FOOD;
        List<CommunityResource> resources = Arrays.asList(foodResource);

        when(resourceRepository.findByCategory(category)).thenReturn(resources);

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
        List<CommunityResource> resources = Arrays.asList(foodResource);

        when(resourceRepository.findByCityAndState(city, state)).thenReturn(resources);

        // When
        List<CommunityResource> result = resourceService.searchByLocation(city, state);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(city, result.get(0).getCity());
    }

    @Test
    @DisplayName("Should search resources by zip code")
    void testSearchByZipCode() {
        // Given
        String zipCode = "02101";
        List<CommunityResource> resources = Arrays.asList(foodResource);

        when(resourceRepository.findByZipCode(zipCode)).thenReturn(resources);

        // When
        List<CommunityResource> result = resourceService.searchByZipCode(zipCode);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(zipCode, result.get(0).getZipCode());
    }

    @Test
    @DisplayName("Should find resources within radius")
    void testFindResourcesWithinRadius() {
        // Given
        double latitude = 42.3601;
        double longitude = -71.0589;
        double radiusMiles = 5.0;
        List<CommunityResource> resources = Arrays.asList(foodResource);

        when(resourceRepository.findWithinRadius(latitude, longitude, radiusMiles))
                .thenReturn(resources);

        // When
        List<CommunityResource> result = resourceService.findNearby(latitude, longitude, radiusMiles);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Should create resource referral")
    void testCreateReferral() {
        // Given
        String resourceId = "resource-001";
        SdohCategory category = SdohCategory.FOOD_INSECURITY;
        String referredBy = "Provider-001";

        ResourceReferral savedReferral = ResourceReferral.builder()
                .referralId("ref-001")
                .patientId(patientId)
                .resourceId(resourceId)
                .build();

        when(referralRepository.save(any(ResourceReferral.class))).thenReturn(savedReferral);

        // When
        ResourceReferral result = resourceService.createReferral(
                tenantId, patientId, resourceId, category, "Food assistance needed", referredBy);

        // Then
        assertNotNull(result);
        assertEquals(patientId, result.getPatientId());
        assertEquals(resourceId, result.getResourceId());
        verify(referralRepository, times(1)).save(any(ResourceReferral.class));
    }

    @Test
    @DisplayName("Should get referrals for patient")
    void testGetPatientReferrals() {
        // Given
        List<ResourceReferral> referrals = Arrays.asList(
                ResourceReferral.builder().referralId("r1").patientId(patientId).build(),
                ResourceReferral.builder().referralId("r2").patientId(patientId).build()
        );

        when(referralRepository.findByTenantIdAndPatientId(tenantId, patientId))
                .thenReturn(referrals);

        // When
        List<ResourceReferral> result = resourceService.getPatientReferrals(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should update referral status")
    void testUpdateReferralStatus() {
        // Given
        String referralId = "ref-001";
        ResourceReferral referral = ResourceReferral.builder()
                .referralId(referralId)
                .status(ResourceReferral.ReferralStatus.PENDING)
                .build();

        when(referralRepository.findById(referralId)).thenReturn(Optional.of(referral));
        when(referralRepository.save(any(ResourceReferral.class))).thenReturn(referral);

        // When
        resourceService.updateReferralStatus(referralId, ResourceReferral.ReferralStatus.COMPLETED);

        // Then
        verify(referralRepository, times(1)).save(any(ResourceReferral.class));
    }

    @Test
    @DisplayName("Should get active referrals")
    void testGetActiveReferrals() {
        // Given
        List<ResourceReferral> activeReferrals = Arrays.asList(
                ResourceReferral.builder()
                        .referralId("r1")
                        .status(ResourceReferral.ReferralStatus.PENDING)
                        .build()
        );

        when(referralRepository.findActiveByTenantIdAndPatientId(tenantId, patientId))
                .thenReturn(activeReferrals);

        // When
        List<ResourceReferral> result = resourceService.getActiveReferrals(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should add new community resource")
    void testAddResource() {
        // Given
        when(resourceRepository.save(any(CommunityResource.class))).thenReturn(foodResource);

        // When
        CommunityResource result = resourceService.addResource(foodResource);

        // Then
        assertNotNull(result);
        assertEquals(foodResource.getResourceId(), result.getResourceId());
        verify(resourceRepository, times(1)).save(any(CommunityResource.class));
    }

    @Test
    @DisplayName("Should update existing resource")
    void testUpdateResource() {
        // Given
        String resourceId = "resource-001";
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(foodResource));
        when(resourceRepository.save(any(CommunityResource.class))).thenReturn(foodResource);

        // When
        CommunityResource result = resourceService.updateResource(resourceId, foodResource);

        // Then
        assertNotNull(result);
        verify(resourceRepository, times(1)).save(any(CommunityResource.class));
    }

    @Test
    @DisplayName("Should delete resource")
    void testDeleteResource() {
        // Given
        String resourceId = "resource-001";

        // When
        resourceService.deleteResource(resourceId);

        // Then
        verify(resourceRepository, times(1)).deleteById(resourceId);
    }

    @Test
    @DisplayName("Should get resource by ID")
    void testGetResourceById() {
        // Given
        String resourceId = "resource-001";
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(foodResource));

        // When
        Optional<CommunityResource> result = resourceService.getResourceById(resourceId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(resourceId, result.get().getResourceId());
    }

    @Test
    @DisplayName("Should filter resources by walk-in availability")
    void testFilterByWalkIns() {
        // Given
        foodResource.setAcceptsWalkIns(true);
        List<CommunityResource> resources = Arrays.asList(foodResource);

        when(resourceRepository.findByAcceptsWalkIns(true)).thenReturn(resources);

        // When
        List<CommunityResource> result = resourceService.findWalkInResources();

        // Then
        assertNotNull(result);
        assertTrue(result.get(0).isAcceptsWalkIns());
    }

    @Test
    @DisplayName("Should filter resources requiring no referral")
    void testFilterNoReferralRequired() {
        // Given
        foodResource.setRequiresReferral(false);
        List<CommunityResource> resources = Arrays.asList(foodResource);

        when(resourceRepository.findByRequiresReferral(false)).thenReturn(resources);

        // When
        List<CommunityResource> result = resourceService.findNoReferralResources();

        // Then
        assertNotNull(result);
        assertFalse(result.get(0).isRequiresReferral());
    }

    @Test
    @DisplayName("Should calculate distance to resource")
    void testCalculateDistance() {
        // Given
        double patientLat = 42.3601;
        double patientLon = -71.0589;
        foodResource.setLatitude(42.3656);
        foodResource.setLongitude(-71.0596);

        // When
        double distance = resourceService.calculateDistance(
                patientLat, patientLon,
                foodResource.getLatitude(), foodResource.getLongitude());

        // Then
        assertTrue(distance >= 0);
        assertTrue(distance < 1.0); // Should be less than 1 mile
    }
}
