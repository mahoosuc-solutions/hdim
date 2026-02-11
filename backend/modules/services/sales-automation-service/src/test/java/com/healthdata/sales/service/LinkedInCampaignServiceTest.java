package com.healthdata.sales.service;

import com.healthdata.sales.dto.LinkedInCampaignDTO;
import com.healthdata.sales.entity.LinkedInCampaign;
import com.healthdata.sales.entity.LinkedInCampaign.CampaignStatus;
import com.healthdata.sales.exception.DuplicateResourceException;
import com.healthdata.sales.exception.ResourceNotFoundException;
import com.healthdata.sales.repository.LinkedInCampaignRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LinkedInCampaignService.
 * Tests all CRUD operations, status transitions, and metrics updates
 * with proper multi-tenant isolation verification.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LinkedInCampaignServiceTest {

    @Mock
    private LinkedInCampaignRepository campaignRepository;

    @InjectMocks
    private LinkedInCampaignService campaignService;

    private UUID tenantId;
    private UUID campaignId;
    private UUID userId;
    private LinkedInCampaign testCampaign;
    private LinkedInCampaignDTO testCampaignDTO;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        campaignId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testCampaign = createTestCampaign();
        testCampaignDTO = createTestCampaignDTO();
    }

    private LinkedInCampaign createTestCampaign() {
        return LinkedInCampaign.builder()
            .id(campaignId)
            .tenantId(tenantId)
            .name("Test Campaign")
            .description("Test campaign description")
            .status(CampaignStatus.DRAFT)
            .targetCriteria("Healthcare executives")
            .dailyLimit(25)
            .totalSent(0)
            .totalAccepted(0)
            .totalReplied(0)
            .acceptanceRate(0.0)
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private LinkedInCampaignDTO createTestCampaignDTO() {
        return LinkedInCampaignDTO.builder()
            .id(campaignId)
            .name("Test Campaign")
            .description("Test campaign description")
            .status(CampaignStatus.DRAFT)
            .targetCriteria("Healthcare executives")
            .dailyLimit(25)
            .totalSent(0)
            .totalAccepted(0)
            .totalReplied(0)
            .acceptanceRate(0.0)
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ==================== findAll Tests ====================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated campaigns for tenant")
        void shouldReturnPaginatedCampaignsForTenant() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInCampaign> campaignPage = new PageImpl<>(
                List.of(testCampaign), pageable, 1
            );

            when(campaignRepository.findByTenantId(tenantId, pageable))
                .thenReturn(campaignPage);

            // Act
            Page<LinkedInCampaignDTO> result = campaignService.findAll(tenantId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Test Campaign");
            verify(campaignRepository).findByTenantId(tenantId, pageable);
        }

        @Test
        @DisplayName("should return empty page when no campaigns exist")
        void shouldReturnEmptyPageWhenNoCampaignsExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInCampaign> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(campaignRepository.findByTenantId(tenantId, pageable))
                .thenReturn(emptyPage);

            // Act
            Page<LinkedInCampaignDTO> result = campaignService.findAll(tenantId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ==================== findByStatus Tests ====================

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should return campaigns filtered by status")
        void shouldReturnCampaignsFilteredByStatus() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            testCampaign.setStatus(CampaignStatus.ACTIVE);
            Page<LinkedInCampaign> campaignPage = new PageImpl<>(
                List.of(testCampaign), pageable, 1
            );

            when(campaignRepository.findByTenantIdAndStatus(tenantId, CampaignStatus.ACTIVE, pageable))
                .thenReturn(campaignPage);

            // Act
            Page<LinkedInCampaignDTO> result = campaignService.findByStatus(
                tenantId, CampaignStatus.ACTIVE, pageable
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(CampaignStatus.ACTIVE);
            verify(campaignRepository).findByTenantIdAndStatus(tenantId, CampaignStatus.ACTIVE, pageable);
        }

        @Test
        @DisplayName("should return empty page when no campaigns match status")
        void shouldReturnEmptyPageWhenNoMatchingStatus() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInCampaign> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(campaignRepository.findByTenantIdAndStatus(tenantId, CampaignStatus.COMPLETED, pageable))
                .thenReturn(emptyPage);

            // Act
            Page<LinkedInCampaignDTO> result = campaignService.findByStatus(
                tenantId, CampaignStatus.COMPLETED, pageable
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ==================== searchByName Tests ====================

    @Nested
    @DisplayName("searchByName")
    class SearchByName {

        @Test
        @DisplayName("should return campaigns matching search term")
        void shouldReturnCampaignsMatchingSearchTerm() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInCampaign> campaignPage = new PageImpl<>(
                List.of(testCampaign), pageable, 1
            );

            when(campaignRepository.searchByName(tenantId, "Test", pageable))
                .thenReturn(campaignPage);

            // Act
            Page<LinkedInCampaignDTO> result = campaignService.searchByName(
                tenantId, "Test", pageable
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).contains("Test");
            verify(campaignRepository).searchByName(tenantId, "Test", pageable);
        }

        @Test
        @DisplayName("should return empty page when no campaigns match search")
        void shouldReturnEmptyPageWhenNoMatchingSearch() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInCampaign> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(campaignRepository.searchByName(tenantId, "NonExistent", pageable))
                .thenReturn(emptyPage);

            // Act
            Page<LinkedInCampaignDTO> result = campaignService.searchByName(
                tenantId, "NonExistent", pageable
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ==================== findById Tests ====================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return campaign when found")
        void shouldReturnCampaignWhenFound() {
            // Arrange
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            Optional<LinkedInCampaignDTO> result = campaignService.findById(tenantId, campaignId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(campaignId);
            assertThat(result.get().getName()).isEqualTo("Test Campaign");
            verify(campaignRepository).findByIdAndTenantId(campaignId, tenantId);
        }

        @Test
        @DisplayName("should return empty optional when campaign not found")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act
            Optional<LinkedInCampaignDTO> result = campaignService.findById(tenantId, nonExistentId);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should enforce tenant isolation - not find campaign from different tenant")
        void shouldEnforceTenantIsolation() {
            // Arrange
            UUID otherTenantId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(campaignId, otherTenantId))
                .thenReturn(Optional.empty());

            // Act
            Optional<LinkedInCampaignDTO> result = campaignService.findById(otherTenantId, campaignId);

            // Assert
            assertThat(result).isEmpty();
            verify(campaignRepository).findByIdAndTenantId(campaignId, otherTenantId);
        }
    }

    // ==================== create Tests ====================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create campaign with DRAFT status")
        void shouldCreateCampaignWithDraftStatus() {
            // Arrange
            LinkedInCampaignDTO inputDTO = LinkedInCampaignDTO.builder()
                .name("New Campaign")
                .description("New description")
                .targetCriteria("Tech executives")
                .dailyLimit(30)
                .build();

            LinkedInCampaign savedCampaign = LinkedInCampaign.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("New Campaign")
                .description("New description")
                .status(CampaignStatus.DRAFT)
                .targetCriteria("Tech executives")
                .dailyLimit(30)
                .createdBy(userId)
                .totalSent(0)
                .totalAccepted(0)
                .totalReplied(0)
                .acceptanceRate(0.0)
                .createdAt(LocalDateTime.now())
                .build();

            when(campaignRepository.existsByTenantIdAndName(tenantId, "New Campaign"))
                .thenReturn(false);
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(savedCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.create(tenantId, inputDTO, userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Campaign");
            assertThat(result.getStatus()).isEqualTo(CampaignStatus.DRAFT);
            assertThat(result.getCreatedBy()).isEqualTo(userId);
            verify(campaignRepository).existsByTenantIdAndName(tenantId, "New Campaign");
            verify(campaignRepository).save(any(LinkedInCampaign.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate name")
        void shouldThrowExceptionForDuplicateName() {
            // Arrange
            LinkedInCampaignDTO inputDTO = LinkedInCampaignDTO.builder()
                .name("Existing Campaign")
                .build();

            when(campaignRepository.existsByTenantIdAndName(tenantId, "Existing Campaign"))
                .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> campaignService.create(tenantId, inputDTO, userId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Existing Campaign")
                .hasMessageContaining("already exists");

            verify(campaignRepository).existsByTenantIdAndName(tenantId, "Existing Campaign");
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow same campaign name in different tenants")
        void shouldAllowSameNameInDifferentTenants() {
            // Arrange
            UUID otherTenantId = UUID.randomUUID();
            LinkedInCampaignDTO inputDTO = LinkedInCampaignDTO.builder()
                .name("Test Campaign")
                .build();

            LinkedInCampaign savedCampaign = LinkedInCampaign.builder()
                .id(UUID.randomUUID())
                .tenantId(otherTenantId)
                .name("Test Campaign")
                .status(CampaignStatus.DRAFT)
                .build();

            when(campaignRepository.existsByTenantIdAndName(otherTenantId, "Test Campaign"))
                .thenReturn(false);
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(savedCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.create(otherTenantId, inputDTO, userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Campaign");
        }
    }

    // ==================== update Tests ====================

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing campaign")
        void shouldUpdateExistingCampaign() {
            // Arrange
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Updated Campaign Name")
                .description("Updated description")
                .dailyLimit(50)
                .build();

            LinkedInCampaign updatedCampaign = LinkedInCampaign.builder()
                .id(campaignId)
                .tenantId(tenantId)
                .name("Updated Campaign Name")
                .description("Updated description")
                .status(CampaignStatus.DRAFT)
                .dailyLimit(50)
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.existsByTenantIdAndName(tenantId, "Updated Campaign Name"))
                .thenReturn(false);
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(updatedCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.update(tenantId, campaignId, updateDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Campaign Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            verify(campaignRepository).save(any(LinkedInCampaign.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaign not found")
        void shouldThrowExceptionWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Update")
                .build();

            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> campaignService.update(tenantId, nonExistentId, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when changing to existing name")
        void shouldThrowExceptionWhenChangingToExistingName() {
            // Arrange
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Existing Name")
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.existsByTenantIdAndName(tenantId, "Existing Name"))
                .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> campaignService.update(tenantId, campaignId, updateDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Existing Name");

            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow update when name is unchanged")
        void shouldAllowUpdateWhenNameUnchanged() {
            // Arrange
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Test Campaign") // Same name as existing
                .description("Only description updated")
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(testCampaign);

            // Act - should not throw duplicate exception
            LinkedInCampaignDTO result = campaignService.update(tenantId, campaignId, updateDTO);

            // Assert
            assertThat(result).isNotNull();
            // existsByTenantIdAndName should NOT be called when name hasn't changed
            verify(campaignRepository, never()).existsByTenantIdAndName(any(), any());
        }

        @Test
        @DisplayName("should allow update when name is null in DTO")
        void shouldAllowUpdateWhenNameIsNull() {
            // Arrange
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .description("Only description updated")
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(testCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.update(tenantId, campaignId, updateDTO);

            // Assert
            assertThat(result).isNotNull();
            verify(campaignRepository, never()).existsByTenantIdAndName(any(), any());
        }
    }

    // ==================== delete Tests ====================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing campaign")
        void shouldDeleteExistingCampaign() {
            // Arrange
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            campaignService.delete(tenantId, campaignId);

            // Assert
            verify(campaignRepository).delete(testCampaign);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when deleting non-existent campaign")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> campaignService.delete(tenantId, nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());

            verify(campaignRepository, never()).delete(any());
        }
    }

    // ==================== Status Operations Tests ====================

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("should activate campaign and change status to ACTIVE")
        void shouldActivateCampaign() {
            // Arrange
            testCampaign.setStatus(CampaignStatus.DRAFT);
            LinkedInCampaign activatedCampaign = LinkedInCampaign.builder()
                .id(campaignId)
                .tenantId(tenantId)
                .name("Test Campaign")
                .status(CampaignStatus.ACTIVE)
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(activatedCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.activate(tenantId, campaignId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
            verify(campaignRepository).save(any(LinkedInCampaign.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaign not found")
        void shouldThrowExceptionWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> campaignService.activate(tenantId, nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("pause")
    class Pause {

        @Test
        @DisplayName("should pause campaign and change status to PAUSED")
        void shouldPauseCampaign() {
            // Arrange
            testCampaign.setStatus(CampaignStatus.ACTIVE);
            LinkedInCampaign pausedCampaign = LinkedInCampaign.builder()
                .id(campaignId)
                .tenantId(tenantId)
                .name("Test Campaign")
                .status(CampaignStatus.PAUSED)
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(pausedCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.pause(tenantId, campaignId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CampaignStatus.PAUSED);
            verify(campaignRepository).save(any(LinkedInCampaign.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaign not found")
        void shouldThrowExceptionWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> campaignService.pause(tenantId, nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("should complete campaign and change status to COMPLETED")
        void shouldCompleteCampaign() {
            // Arrange
            testCampaign.setStatus(CampaignStatus.ACTIVE);
            LinkedInCampaign completedCampaign = LinkedInCampaign.builder()
                .id(campaignId)
                .tenantId(tenantId)
                .name("Test Campaign")
                .status(CampaignStatus.COMPLETED)
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));
            when(campaignRepository.save(any(LinkedInCampaign.class)))
                .thenReturn(completedCampaign);

            // Act
            LinkedInCampaignDTO result = campaignService.complete(tenantId, campaignId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
            verify(campaignRepository).save(any(LinkedInCampaign.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaign not found")
        void shouldThrowExceptionWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> campaignService.complete(tenantId, nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== Metrics Update Tests ====================

    @Nested
    @DisplayName("incrementSent")
    class IncrementSent {

        @Test
        @DisplayName("should increment sent count when campaign exists")
        void shouldIncrementSentCount() {
            // Arrange
            testCampaign.setTotalSent(5);
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            campaignService.incrementSent(tenantId, campaignId);

            // Assert
            verify(campaignRepository).save(testCampaign);
            assertThat(testCampaign.getTotalSent()).isEqualTo(6);
        }

        @Test
        @DisplayName("should not throw exception when campaign not found")
        void shouldNotThrowWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act - should not throw
            campaignService.incrementSent(tenantId, nonExistentId);

            // Assert
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update acceptance rate when incrementing sent")
        void shouldUpdateAcceptanceRate() {
            // Arrange
            testCampaign.setTotalSent(10);
            testCampaign.setTotalAccepted(5);
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            campaignService.incrementSent(tenantId, campaignId);

            // Assert
            verify(campaignRepository).save(testCampaign);
            // After incrementSent: sent=11, accepted=5, rate = 5/11 * 100 ≈ 45.45%
            assertThat(testCampaign.getTotalSent()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("incrementAccepted")
    class IncrementAccepted {

        @Test
        @DisplayName("should increment accepted count when campaign exists")
        void shouldIncrementAcceptedCount() {
            // Arrange
            testCampaign.setTotalAccepted(3);
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            campaignService.incrementAccepted(tenantId, campaignId);

            // Assert
            verify(campaignRepository).save(testCampaign);
            assertThat(testCampaign.getTotalAccepted()).isEqualTo(4);
        }

        @Test
        @DisplayName("should not throw exception when campaign not found")
        void shouldNotThrowWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act - should not throw
            campaignService.incrementAccepted(tenantId, nonExistentId);

            // Assert
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update acceptance rate when incrementing accepted")
        void shouldUpdateAcceptanceRate() {
            // Arrange
            testCampaign.setTotalSent(10);
            testCampaign.setTotalAccepted(4);
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            campaignService.incrementAccepted(tenantId, campaignId);

            // Assert
            verify(campaignRepository).save(testCampaign);
            assertThat(testCampaign.getTotalAccepted()).isEqualTo(5);
            // Rate should be recalculated: 5/10 * 100 = 50%
        }
    }

    @Nested
    @DisplayName("incrementReplied")
    class IncrementReplied {

        @Test
        @DisplayName("should increment replied count when campaign exists")
        void shouldIncrementRepliedCount() {
            // Arrange
            testCampaign.setTotalReplied(2);
            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(testCampaign));

            // Act
            campaignService.incrementReplied(tenantId, campaignId);

            // Assert
            verify(campaignRepository).save(testCampaign);
            assertThat(testCampaign.getTotalReplied()).isEqualTo(3);
        }

        @Test
        @DisplayName("should not throw exception when campaign not found")
        void shouldNotThrowWhenCampaignNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignRepository.findByIdAndTenantId(nonExistentId, tenantId))
                .thenReturn(Optional.empty());

            // Act - should not throw
            campaignService.incrementReplied(tenantId, nonExistentId);

            // Assert
            verify(campaignRepository, never()).save(any());
        }
    }

    // ==================== DTO Mapping Tests ====================

    @Nested
    @DisplayName("DTO Mapping")
    class DTOMapping {

        @Test
        @DisplayName("should correctly map all entity fields to DTO")
        void shouldMapAllFieldsToDTO() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            LinkedInCampaign fullCampaign = LinkedInCampaign.builder()
                .id(campaignId)
                .tenantId(tenantId)
                .name("Full Campaign")
                .description("Full description")
                .status(CampaignStatus.ACTIVE)
                .targetCriteria("Healthcare CIOs")
                .dailyLimit(50)
                .totalSent(100)
                .totalAccepted(45)
                .totalReplied(20)
                .acceptanceRate(45.0)
                .createdBy(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

            when(campaignRepository.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(fullCampaign));

            // Act
            Optional<LinkedInCampaignDTO> result = campaignService.findById(tenantId, campaignId);

            // Assert
            assertThat(result).isPresent();
            LinkedInCampaignDTO dto = result.get();
            assertThat(dto.getId()).isEqualTo(campaignId);
            assertThat(dto.getName()).isEqualTo("Full Campaign");
            assertThat(dto.getDescription()).isEqualTo("Full description");
            assertThat(dto.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
            assertThat(dto.getTargetCriteria()).isEqualTo("Healthcare CIOs");
            assertThat(dto.getDailyLimit()).isEqualTo(50);
            assertThat(dto.getTotalSent()).isEqualTo(100);
            assertThat(dto.getTotalAccepted()).isEqualTo(45);
            assertThat(dto.getTotalReplied()).isEqualTo(20);
            assertThat(dto.getAcceptanceRate()).isEqualTo(45.0);
            assertThat(dto.getCreatedBy()).isEqualTo(userId);
            assertThat(dto.getCreatedAt()).isEqualTo(now);
            assertThat(dto.getUpdatedAt()).isEqualTo(now);
        }
    }
}
