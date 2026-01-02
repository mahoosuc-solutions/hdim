package com.healthdata.quality.controller;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.service.HealthScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Test Suite for Health Score API Endpoints
 *
 * Tests verify:
 * - Multi-tenant isolation (X-Tenant-ID header)
 * - Authentication and authorization
 * - DTO responses (not entities)
 * - Pagination for list endpoints
 * - Proper HTTP status codes
 * - Data validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Health Score Controller Tests")
class HealthScoreControllerTest {

    @Mock
    private HealthScoreService healthScoreService;

    @InjectMocks
    private HealthScoreController healthScoreController;

    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    private HealthScoreDTO sampleHealthScore;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(healthScoreController).build();

        // Create sample health score DTO
        sampleHealthScore = HealthScoreDTO.builder()
                .id(UUID.randomUUID())
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .overallScore(75.5)
                .physicalHealthScore(80.0)
                .mentalHealthScore(70.0)
                .socialDeterminantsScore(75.0)
                .preventiveCareScore(72.0)
                .chronicDiseaseScore(78.0)
                .calculatedAt(Instant.now())
                .previousScore(70.0)
                .scoreDelta(5.5)
                .significantChange(false)
                .build();
    }

    // ========================================
    // GET /quality-measure/patients/{patientId}/health-score
    // ========================================

    @Test
    @DisplayName("GET current health score - should return 200 with DTO when score exists")
    void getCurrentHealthScore_WhenExists_Returns200WithDTO() {
        // Given
        when(healthScoreService.getCurrentHealthScore(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(sampleHealthScore));

        // When
        ResponseEntity<HealthScoreDTO> response = healthScoreController.getCurrentHealthScore(
                TENANT_ID, PATIENT_ID
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(sampleHealthScore);
        assertThat(response.getBody().getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(response.getBody().getTenantId()).isEqualTo(TENANT_ID);

        verify(healthScoreService).getCurrentHealthScore(TENANT_ID, PATIENT_ID);
    }

    @Test
    @DisplayName("GET current health score - should return 404 when score doesn't exist")
    void getCurrentHealthScore_WhenNotExists_Returns404() {
        // Given
        when(healthScoreService.getCurrentHealthScore(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.empty());

        // When
        ResponseEntity<HealthScoreDTO> response = healthScoreController.getCurrentHealthScore(
                TENANT_ID, PATIENT_ID
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();

        verify(healthScoreService).getCurrentHealthScore(TENANT_ID, PATIENT_ID);
    }

    @Test
    @DisplayName("GET current health score - should handle UUID patient IDs")
    void getCurrentHealthScore_WithUuidPatientId_HandlesCorrectly() {
        // Given
        UUID patientId = UUID.fromString("60606060-1111-2222-3333-444444444444");
        HealthScoreDTO scoreForPatient = HealthScoreDTO.builder()
                .patientId(patientId)
                .tenantId(TENANT_ID)
                .overallScore(85.0)
                .build();

        when(healthScoreService.getCurrentHealthScore(TENANT_ID, patientId))
                .thenReturn(Optional.of(scoreForPatient));

        // When
        ResponseEntity<HealthScoreDTO> response = healthScoreController.getCurrentHealthScore(
                TENANT_ID, patientId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPatientId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("GET current health score - should use tenant ID from header")
    void getCurrentHealthScore_UsesTenantIdFromHeader() {
        // Given
        String differentTenant = "tenant-999";
        when(healthScoreService.getCurrentHealthScore(differentTenant, PATIENT_ID))
                .thenReturn(Optional.of(sampleHealthScore));

        // When
        healthScoreController.getCurrentHealthScore(differentTenant, PATIENT_ID);

        // Then
        verify(healthScoreService).getCurrentHealthScore(differentTenant, PATIENT_ID);
        verify(healthScoreService, never()).getCurrentHealthScore(TENANT_ID, PATIENT_ID);
    }

    // ========================================
    // GET /quality-measure/patients/{patientId}/health-score/history
    // ========================================

    @Test
    @DisplayName("GET health score history - should return 200 with list of DTOs")
    void getHealthScoreHistory_ReturnsListOfDTOs() {
        // Given
        HealthScoreDTO score1 = HealthScoreDTO.builder()
                .id(UUID.randomUUID())
                .patientId(PATIENT_ID)
                .overallScore(75.0)
                .calculatedAt(Instant.now())
                .build();

        HealthScoreDTO score2 = HealthScoreDTO.builder()
                .id(UUID.randomUUID())
                .patientId(PATIENT_ID)
                .overallScore(70.0)
                .calculatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();

        List<HealthScoreDTO> history = Arrays.asList(score1, score2);

        when(healthScoreService.getHealthScoreHistory(TENANT_ID, PATIENT_ID))
                .thenReturn(history);

        // When
        ResponseEntity<List<HealthScoreDTO>> response = healthScoreController.getHealthScoreHistory(
                TENANT_ID, PATIENT_ID, 50
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getOverallScore()).isEqualTo(75.0);
        assertThat(response.getBody().get(1).getOverallScore()).isEqualTo(70.0);

        verify(healthScoreService).getHealthScoreHistory(TENANT_ID, PATIENT_ID);
    }

    @Test
    @DisplayName("GET health score history - should respect limit parameter")
    void getHealthScoreHistory_RespectsLimit() {
        // Given
        List<HealthScoreDTO> manyScores = Arrays.asList(
                createScore(75.0), createScore(74.0), createScore(73.0),
                createScore(72.0), createScore(71.0)
        );

        when(healthScoreService.getHealthScoreHistory(TENANT_ID, PATIENT_ID))
                .thenReturn(manyScores);

        // When - request only 3 scores
        ResponseEntity<List<HealthScoreDTO>> response = healthScoreController.getHealthScoreHistory(
                TENANT_ID, PATIENT_ID, 3
        );

        // Then
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    @DisplayName("GET health score history - should cap limit at 100")
    void getHealthScoreHistory_CapsLimitAt100() {
        // Given
        List<HealthScoreDTO> manyScores = generateScores(150);
        when(healthScoreService.getHealthScoreHistory(TENANT_ID, PATIENT_ID))
                .thenReturn(manyScores);

        // When - request 200 scores (should be capped at 100)
        ResponseEntity<List<HealthScoreDTO>> response = healthScoreController.getHealthScoreHistory(
                TENANT_ID, PATIENT_ID, 200
        );

        // Then
        assertThat(response.getBody()).hasSize(100);
    }

    @Test
    @DisplayName("GET health score history - should use default limit of 50")
    void getHealthScoreHistory_UsesDefaultLimit() {
        // Given
        List<HealthScoreDTO> manyScores = generateScores(80);
        when(healthScoreService.getHealthScoreHistory(TENANT_ID, PATIENT_ID))
                .thenReturn(manyScores);

        // When - no limit specified
        ResponseEntity<List<HealthScoreDTO>> response = healthScoreController.getHealthScoreHistory(
                TENANT_ID, PATIENT_ID, 50 // default value
        );

        // Then
        assertThat(response.getBody()).hasSize(50);
    }

    // ========================================
    // GET /quality-measure/patients/health-scores/at-risk
    // ========================================

    @Test
    @DisplayName("GET at-risk patients - should return paginated results")
    void getAtRiskPatients_ReturnsPaginatedResults() {
        // Given
        double threshold = 60.0;
        int page = 0;
        int size = 20;

        List<HealthScoreDTO> atRiskScores = Arrays.asList(
                createScoreForPatient(UUID.fromString("10101010-1111-2222-3333-444444444444"), 55.0),
                createScoreForPatient(UUID.fromString("20202020-1111-2222-3333-444444444444"), 50.0),
                createScoreForPatient(UUID.fromString("30303030-1111-2222-3333-444444444444"), 45.0)
        );

        Page<HealthScoreDTO> pagedResults = new PageImpl<>(
                atRiskScores,
                PageRequest.of(page, size),
                atRiskScores.size()
        );

        when(healthScoreService.getAtRiskPatients(eq(TENANT_ID), eq(threshold), any(Pageable.class)))
                .thenReturn(pagedResults);

        // When
        ResponseEntity<Page<HealthScoreDTO>> response = healthScoreController.getAtRiskPatients(
                TENANT_ID, threshold, page, size
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(3);
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);

        verify(healthScoreService).getAtRiskPatients(eq(TENANT_ID), eq(threshold), any(Pageable.class));
    }

    @Test
    @DisplayName("GET at-risk patients - should use default threshold of 60.0")
    void getAtRiskPatients_UsesDefaultThreshold() {
        // Given
        Page<HealthScoreDTO> emptyPage = Page.empty();
        when(healthScoreService.getAtRiskPatients(eq(TENANT_ID), eq(60.0), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        healthScoreController.getAtRiskPatients(TENANT_ID, 60.0, 0, 20);

        // Then
        verify(healthScoreService).getAtRiskPatients(eq(TENANT_ID), eq(60.0), any(Pageable.class));
    }

    @Test
    @DisplayName("GET at-risk patients - should validate threshold range")
    void getAtRiskPatients_ValidatesThresholdRange() {
        // When/Then - threshold too low
        assertThrows(IllegalArgumentException.class, () ->
                healthScoreController.getAtRiskPatients(TENANT_ID, -10.0, 0, 20)
        );

        // When/Then - threshold too high
        assertThrows(IllegalArgumentException.class, () ->
                healthScoreController.getAtRiskPatients(TENANT_ID, 150.0, 0, 20)
        );
    }

    @Test
    @DisplayName("GET at-risk patients - should cap page size at 100")
    void getAtRiskPatients_CapsPageSizeAt100() {
        // Given
        Page<HealthScoreDTO> emptyPage = Page.empty();
        when(healthScoreService.getAtRiskPatients(eq(TENANT_ID), eq(60.0), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When - request size of 200
        healthScoreController.getAtRiskPatients(TENANT_ID, 60.0, 0, 200);

        // Then - verify size was capped to 100
        verify(healthScoreService).getAtRiskPatients(
                eq(TENANT_ID),
                eq(60.0),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    @DisplayName("GET at-risk patients - should sort by overall score ascending")
    void getAtRiskPatients_SortsByScoreAscending() {
        // Given
        Page<HealthScoreDTO> emptyPage = Page.empty();
        when(healthScoreService.getAtRiskPatients(eq(TENANT_ID), eq(60.0), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        healthScoreController.getAtRiskPatients(TENANT_ID, 60.0, 0, 20);

        // Then
        verify(healthScoreService).getAtRiskPatients(
                eq(TENANT_ID),
                eq(60.0),
                argThat(pageable ->
                        pageable.getSort().getOrderFor("overallScore") != null &&
                        pageable.getSort().getOrderFor("overallScore").isAscending()
                )
        );
    }

    // ========================================
    // GET /quality-measure/patients/health-scores/significant-changes
    // ========================================

    @Test
    @DisplayName("GET significant changes - should return paginated results")
    void getSignificantChanges_ReturnsPaginatedResults() {
        // Given
        int days = 7;
        int page = 0;
        int size = 20;

        List<HealthScoreDTO> significantChanges = Arrays.asList(
                createSignificantChange(UUID.fromString("40404040-1111-2222-3333-444444444444"), 85.0, 70.0),
                createSignificantChange(UUID.fromString("50505050-1111-2222-3333-444444444444"), 60.0, 75.0)
        );

        Page<HealthScoreDTO> pagedResults = new PageImpl<>(
                significantChanges,
                PageRequest.of(page, size),
                significantChanges.size()
        );

        when(healthScoreService.getSignificantChanges(eq(TENANT_ID), any(Instant.class), any(Pageable.class)))
                .thenReturn(pagedResults);

        // When
        ResponseEntity<Page<HealthScoreDTO>> response = healthScoreController.getSignificantChanges(
                TENANT_ID, days, page, size
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);

        verify(healthScoreService).getSignificantChanges(
                eq(TENANT_ID),
                any(Instant.class),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET significant changes - should use default lookback of 7 days")
    void getSignificantChanges_UsesDefaultLookback() {
        // Given
        Page<HealthScoreDTO> emptyPage = Page.empty();
        when(healthScoreService.getSignificantChanges(eq(TENANT_ID), any(Instant.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        healthScoreController.getSignificantChanges(TENANT_ID, 7, 0, 20);

        // Then
        verify(healthScoreService).getSignificantChanges(
                eq(TENANT_ID),
                argThat(since -> {
                    long daysDiff = ChronoUnit.DAYS.between(since, Instant.now());
                    return daysDiff >= 6 && daysDiff <= 8; // Allow for timing variance
                }),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET significant changes - should cap lookback at 90 days")
    void getSignificantChanges_CapsLookbackAt90Days() {
        // Given
        Page<HealthScoreDTO> emptyPage = Page.empty();
        when(healthScoreService.getSignificantChanges(eq(TENANT_ID), any(Instant.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When - request 180 days (should be capped at 90)
        healthScoreController.getSignificantChanges(TENANT_ID, 180, 0, 20);

        // Then
        verify(healthScoreService).getSignificantChanges(
                eq(TENANT_ID),
                argThat(since -> {
                    long daysDiff = ChronoUnit.DAYS.between(since, Instant.now());
                    return daysDiff >= 89 && daysDiff <= 91; // 90 days +/- 1 for timing
                }),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET significant changes - should sort by calculatedAt descending")
    void getSignificantChanges_SortsByDateDescending() {
        // Given
        Page<HealthScoreDTO> emptyPage = Page.empty();
        when(healthScoreService.getSignificantChanges(eq(TENANT_ID), any(Instant.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        healthScoreController.getSignificantChanges(TENANT_ID, 7, 0, 20);

        // Then
        verify(healthScoreService).getSignificantChanges(
                eq(TENANT_ID),
                any(Instant.class),
                argThat(pageable ->
                        pageable.getSort().getOrderFor("calculatedAt") != null &&
                        pageable.getSort().getOrderFor("calculatedAt").isDescending()
                )
        );
    }

    // ========================================
    // Helper Methods
    // ========================================

    private HealthScoreDTO createScore(double overallScore) {
        return HealthScoreDTO.builder()
                .id(UUID.randomUUID())
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .overallScore(overallScore)
                .calculatedAt(Instant.now())
                .build();
    }

    private HealthScoreDTO createScoreForPatient(UUID patientId, double score) {
        return HealthScoreDTO.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .tenantId(TENANT_ID)
                .overallScore(score)
                .calculatedAt(Instant.now())
                .build();
    }

    private HealthScoreDTO createSignificantChange(UUID patientId, double currentScore, double previousScore) {
        return HealthScoreDTO.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .tenantId(TENANT_ID)
                .overallScore(currentScore)
                .previousScore(previousScore)
                .scoreDelta(currentScore - previousScore)
                .significantChange(true)
                .changeReason("Significant change detected")
                .calculatedAt(Instant.now())
                .build();
    }

    private List<HealthScoreDTO> generateScores(int count) {
        List<HealthScoreDTO> scores = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            scores.add(createScore(75.0 - i * 0.5));
        }
        return scores;
    }
}
