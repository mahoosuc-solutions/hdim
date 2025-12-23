package com.healthdata.quality.service;

import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Care Gap Prioritization Service
 * Tests risk-based prioritization logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Prioritization Service - TDD")
class CareGapPrioritizationServiceTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @InjectMocks
    private CareGapPrioritizationService service;

    private String tenantId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-123";
        patientId = UUID.randomUUID();
    }

    /**
     * Test 1: HIGH risk patient → URGENT priority for chronic disease gaps
     */
    @Test
    @DisplayName("Should set URGENT priority for high-risk patient with chronic disease gap")
    void shouldSetUrgentPriority_HighRiskChronicDisease() {
        // Given: High-risk patient
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .riskScore(85)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        // When: Prioritize chronic disease gap
        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS134",  // Diabetes control
            CareGapEntity.GapCategory.CHRONIC_DISEASE
        );

        // Then: Should be URGENT
        assertThat(priority).isEqualTo(CareGapEntity.Priority.URGENT);
    }

    /**
     * Test 2: MEDIUM risk patient → HIGH priority for chronic disease
     */
    @Test
    @DisplayName("Should set HIGH priority for medium-risk patient with chronic disease gap")
    void shouldSetHighPriority_MediumRiskChronicDisease() {
        // Given: Medium-risk patient
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .riskScore(55)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        // When: Prioritize chronic disease gap
        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS134",
            CareGapEntity.GapCategory.CHRONIC_DISEASE
        );

        // Then: Should be HIGH
        assertThat(priority).isEqualTo(CareGapEntity.Priority.HIGH);
    }

    /**
     * Test 3: LOW risk patient → MEDIUM priority for preventive care
     */
    @Test
    @DisplayName("Should set MEDIUM priority for low-risk patient with preventive gap")
    void shouldSetMediumPriority_LowRiskPreventive() {
        // Given: Low-risk patient
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .riskScore(20)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        // When: Prioritize preventive care gap
        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS125",
            CareGapEntity.GapCategory.PREVENTIVE_CARE
        );

        // Then: Should be MEDIUM
        assertThat(priority).isEqualTo(CareGapEntity.Priority.MEDIUM);
    }

    /**
     * Test 4: No risk assessment → Default to HIGH priority
     */
    @Test
    @DisplayName("Should default to HIGH priority when no risk assessment available")
    void shouldDefaultToHighPriority_NoRiskAssessment() {
        // Given: No risk assessment for patient
        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());

        // When: Prioritize gap
        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS125",
            CareGapEntity.GapCategory.PREVENTIVE_CARE
        );

        // Then: Should default to HIGH (conservative approach)
        assertThat(priority).isEqualTo(CareGapEntity.Priority.HIGH);
    }

    /**
     * Test 5: Calculate due date - URGENT priority = 7 days
     */
    @Test
    @DisplayName("Should calculate 7-day due date for URGENT priority")
    void shouldCalculate7DayDueDate_UrgentPriority() {
        // When: Calculate due date for URGENT priority
        Instant dueDate = service.calculateDueDate(CareGapEntity.Priority.URGENT);

        // Then: Should be approximately 7 days from now
        Instant expectedDueDate = Instant.now().plus(7, ChronoUnit.DAYS);
        assertThat(dueDate).isBetween(
            expectedDueDate.minus(1, ChronoUnit.MINUTES),
            expectedDueDate.plus(1, ChronoUnit.MINUTES)
        );
    }

    /**
     * Test 6: Calculate due date - HIGH priority = 14 days
     */
    @Test
    @DisplayName("Should calculate 14-day due date for HIGH priority")
    void shouldCalculate14DayDueDate_HighPriority() {
        // When: Calculate due date for HIGH priority
        Instant dueDate = service.calculateDueDate(CareGapEntity.Priority.HIGH);

        // Then: Should be approximately 14 days from now
        Instant expectedDueDate = Instant.now().plus(14, ChronoUnit.DAYS);
        assertThat(dueDate).isBetween(
            expectedDueDate.minus(1, ChronoUnit.MINUTES),
            expectedDueDate.plus(1, ChronoUnit.MINUTES)
        );
    }

    /**
     * Test 7: Calculate due date - MEDIUM priority = 30 days
     */
    @Test
    @DisplayName("Should calculate 30-day due date for MEDIUM priority")
    void shouldCalculate30DayDueDate_MediumPriority() {
        // When: Calculate due date for MEDIUM priority
        Instant dueDate = service.calculateDueDate(CareGapEntity.Priority.MEDIUM);

        // Then: Should be approximately 30 days from now
        Instant expectedDueDate = Instant.now().plus(30, ChronoUnit.DAYS);
        assertThat(dueDate).isBetween(
            expectedDueDate.minus(1, ChronoUnit.MINUTES),
            expectedDueDate.plus(1, ChronoUnit.MINUTES)
        );
    }

    /**
     * Test 8: Calculate due date - LOW priority = 90 days
     */
    @Test
    @DisplayName("Should calculate 90-day due date for LOW priority")
    void shouldCalculate90DayDueDate_LowPriority() {
        // When: Calculate due date for LOW priority
        Instant dueDate = service.calculateDueDate(CareGapEntity.Priority.LOW);

        // Then: Should be approximately 90 days from now
        Instant expectedDueDate = Instant.now().plus(90, ChronoUnit.DAYS);
        assertThat(dueDate).isBetween(
            expectedDueDate.minus(1, ChronoUnit.MINUTES),
            expectedDueDate.plus(1, ChronoUnit.MINUTES)
        );
    }

    /**
     * Test 9: Mental health gaps always elevated priority
     */
    @Test
    @DisplayName("Should elevate priority for mental health gaps")
    void shouldElevatePriority_MentalHealthGaps() {
        // Given: Medium-risk patient
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .riskScore(50)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        // When: Prioritize mental health gap
        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS2",
            CareGapEntity.GapCategory.MENTAL_HEALTH
        );

        // Then: Should be elevated to URGENT (mental health is critical)
        assertThat(priority).isEqualTo(CareGapEntity.Priority.URGENT);
    }

    /**
     * Test 10: Medication gaps for chronic disease → elevated priority
     */
    @Test
    @DisplayName("Should elevate priority for medication gaps in chronic disease patients")
    void shouldElevatePriority_MedicationGapsChronicDisease() {
        // Given: Medium-risk patient with chronic disease
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .riskScore(60)
            .chronicConditionCount(2)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        // When: Prioritize medication gap
        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS238",  // Medication adherence
            CareGapEntity.GapCategory.MEDICATION
        );

        // Then: Should be HIGH (medication critical for chronic disease)
        assertThat(priority).isIn(
            CareGapEntity.Priority.URGENT,
            CareGapEntity.Priority.HIGH
        );
    }

    @Test
    @DisplayName("Should set URGENT priority for high-risk medication gaps")
    void shouldSetUrgentPriority_HighRiskMedication() {
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .riskScore(90)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS999",
            CareGapEntity.GapCategory.MEDICATION
        );

        assertThat(priority).isEqualTo(CareGapEntity.Priority.URGENT);
    }

    @Test
    @DisplayName("Should set MEDIUM priority for moderate-risk medication gaps without chronic conditions")
    void shouldSetMediumPriority_ModerateMedicationWithoutChronic() {
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .riskScore(45)
            .chronicConditionCount(0)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS238",
            CareGapEntity.GapCategory.MEDICATION
        );

        assertThat(priority).isEqualTo(CareGapEntity.Priority.MEDIUM);
    }

    @Test
    @DisplayName("Should set LOW priority for low-risk non-preventive gaps")
    void shouldSetLowPriority_LowRiskNonPreventive() {
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .riskScore(15)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(riskAssessment));

        CareGapEntity.Priority priority = service.determinePriority(
            tenantId,
            patientId,
            "CMS999",
            CareGapEntity.GapCategory.MEDICATION
        );

        assertThat(priority).isEqualTo(CareGapEntity.Priority.LOW);
    }

    @Test
    @DisplayName("Should calculate due date for measure based on priority")
    void shouldCalculateDueDateForMeasure() {
        Instant dueDate = service.calculateDueDateForMeasure(
            "CMS999",
            CareGapEntity.Priority.HIGH
        );

        Instant expectedDueDate = Instant.now().plus(14, ChronoUnit.DAYS);
        assertThat(dueDate).isBetween(
            expectedDueDate.minus(1, ChronoUnit.MINUTES),
            expectedDueDate.plus(1, ChronoUnit.MINUTES)
        );
    }
}
