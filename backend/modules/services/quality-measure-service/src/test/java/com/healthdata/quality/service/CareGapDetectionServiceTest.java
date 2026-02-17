package com.healthdata.quality.service;

import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Care Gap Detection Service
 * Tests measure result → care gap transformation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Detection Service - TDD")
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class CareGapDetectionServiceTest {

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private CareGapPrioritizationService prioritizationService;

    @InjectMocks
    private CareGapDetectionService service;

    private String tenantId;
    private UUID patientId;
    private QualityMeasureResultEntity measureResult;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-123";
        patientId = UUID.randomUUID();

        // Default stubbing for prioritization service
        when(prioritizationService.determinePriority(anyString(), any(UUID.class), anyString(), any()))
            .thenReturn(CareGapEntity.Priority.HIGH);
        when(prioritizationService.calculateDueDate(any()))
            .thenReturn(Instant.now().plus(14, ChronoUnit.DAYS));
    }

    /**
     * Test 1: Create care gap from measure calculation when not in numerator but in denominator
     */
    @Test
    @DisplayName("Should create care gap when patient is in denominator but not numerator")
    void shouldCreateCareGap_WhenNotInNumeratorButInDenominator() {
        // Given: Patient is eligible (denominator) but not compliant (numerator)
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .measureCategory("HEDIS")
            .measureYear(2024)
            .denominatorEligible(true)  // Patient is eligible
            .numeratorCompliant(false)    // But NOT compliant = GAP EXISTS
            .complianceRate(0.0)
            .calculationDate(LocalDate.now())
            .build();

        when(careGapRepository.existsOpenCareGap(eq(tenantId), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result for gaps
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Care gap should be created
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getTenantId()).isEqualTo(tenantId);
        assertThat(createdGap.getPatientId()).isEqualTo(patientId);
        assertThat(createdGap.getQualityMeasure()).isEqualTo("CMS125");
        assertThat(createdGap.getStatus()).isEqualTo(CareGapEntity.Status.OPEN);
        assertThat(createdGap.isCreatedFromMeasure()).isTrue();
        assertThat(createdGap.getMeasureResultId()).isEqualTo(measureResult.getId());
    }

    /**
     * Test 2: Do NOT create gap when patient is compliant (in numerator)
     */
    @Test
    @DisplayName("Should NOT create care gap when patient is numerator compliant")
    void shouldNotCreateCareGap_WhenNumeratorCompliant() {
        // Given: Patient is compliant
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .denominatorEligible(true)
            .numeratorCompliant(true)  // COMPLIANT - no gap
            .complianceRate(1.0)
            .calculationDate(LocalDate.now())
            .build();

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: NO care gap should be created
        verify(careGapRepository, never()).save(any());
    }

    /**
     * Test 3: Do NOT create gap when patient not in denominator (not eligible)
     */
    @Test
    @DisplayName("Should NOT create care gap when patient not in denominator")
    void shouldNotCreateCareGap_WhenNotInDenominator() {
        // Given: Patient is not eligible for measure
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .denominatorEligible(false)  // NOT ELIGIBLE
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: NO care gap should be created
        verify(careGapRepository, never()).save(any());
    }

    /**
     * Test 4: Deduplication - do NOT create duplicate gaps
     */
    @Test
    @DisplayName("Should NOT create duplicate care gap if one already exists")
    void shouldNotCreateDuplicateCareGap() {
        // Given: Patient has gap and existing open gap already exists
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        // Existing open gap already exists
        when(careGapRepository.existsOpenCareGap(eq(tenantId), eq(patientId), eq("measure-gap-cms125")))
            .thenReturn(true);

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: NO new gap should be created
        verify(careGapRepository, never()).save(any());
    }

    /**
     * Test 5: Gap prioritization based on patient risk level
     */
    @Test
    @DisplayName("Should set URGENT priority for high-risk patients")
    void shouldSetUrgentPriority_ForHighRiskPatients() {
        // Given: High-risk patient with care gap
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS134")
            .measureName("Diabetes: HbA1c Control")
            .measureCategory("HEDIS")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        // Patient has high risk assessment
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .riskScore(85)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(java.util.Optional.of(riskAssessment));
        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Override default stubbing for this test
        when(prioritizationService.determinePriority(eq(tenantId), eq(patientId), eq("CMS134"), any()))
            .thenReturn(CareGapEntity.Priority.URGENT);

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Gap should have URGENT priority
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getPriority()).isEqualTo(CareGapEntity.Priority.URGENT);
    }

    /**
     * Test 6: Gap prioritization - MEDIUM priority for low-risk patients
     */
    @Test
    @DisplayName("Should set MEDIUM priority for low-risk patients")
    void shouldSetMediumPriority_ForLowRiskPatients() {
        // Given: Low-risk patient with care gap
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        // Patient has low risk
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .patientId(patientId)
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .riskScore(25)
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(java.util.Optional.of(riskAssessment));
        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Override default stubbing for this test
        when(prioritizationService.determinePriority(eq(tenantId), eq(patientId), eq("CMS125"), any()))
            .thenReturn(CareGapEntity.Priority.MEDIUM);

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Gap should have MEDIUM priority
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getPriority()).isEqualTo(CareGapEntity.Priority.MEDIUM);
    }

    /**
     * Test 7: Due date calculation based on measure periodicity
     */
    @Test
    @DisplayName("Should calculate due date based on measure periodicity - annual screening")
    void shouldCalculateDueDate_AnnualScreening() {
        // Given: Annual screening measure (e.g., mammography)
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")  // Breast Cancer Screening - annual
            .measureName("Breast Cancer Screening")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Due date should be within next 365 days (before next annual period)
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getDueDate()).isNotNull();

        // Due date should be in the future but within reasonable timeframe
        Instant now = Instant.now();
        Instant maxDueDate = now.plus(365, ChronoUnit.DAYS);
        assertThat(createdGap.getDueDate()).isBetween(now, maxDueDate);
    }

    /**
     * Test 8: Clinical recommendation generation based on measure type
     */
    @Test
    @DisplayName("Should generate clinical recommendation based on measure type")
    void shouldGenerateRecommendation_BasedOnMeasureType() {
        // Given: Diabetes HbA1c control measure
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS134")
            .measureName("Diabetes: HbA1c Control")
            .measureCategory("HEDIS")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Recommendation should be diabetes-specific
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getRecommendation()).isNotBlank();
        assertThat(createdGap.getRecommendation().toLowerCase())
            .containsAnyOf("hba1c", "diabetes", "blood sugar", "glucose");
    }

    /**
     * Test 9: Multi-tenant isolation - gaps only created for correct tenant
     */
    @Test
    @DisplayName("Should enforce multi-tenant isolation in care gap creation")
    void shouldEnforceMultiTenantIsolation() {
        // Given: Measure result for tenant-123
        String correctTenantId = "tenant-123";
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(correctTenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Created gap must have correct tenant ID
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getTenantId()).isEqualTo(correctTenantId);

        // Verify deduplication check used correct tenant
        verify(careGapRepository).existsOpenCareGap(
            eq(correctTenantId),
            any(UUID.class),
            anyString()
        );
    }

    /**
     * Test 10: Evidence field population from measure result
     */
    @Test
    @DisplayName("Should populate evidence field with measure calculation details")
    void shouldPopulateEvidence_FromMeasureResult() {
        // Given: Measure result with calculation details
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .measureCategory("HEDIS")
            .measureYear(2024)
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .cqlLibrary("BreastCancerScreening-v1.0")
            .build();

        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Evidence should contain measure details
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getEvidence()).isNotBlank();
        assertThat(createdGap.getEvidence()).contains("CMS125", "2024");
    }

    /**
     * Test 11: Category mapping from measure type
     */
    @Test
    @DisplayName("Should map measure to appropriate care gap category")
    void shouldMapMeasure_ToAppropriateCategory() {
        // Given: Preventive screening measure
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .measureCategory("HEDIS")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Gap should be categorized as SCREENING or PREVENTIVE_CARE
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getCategory()).isIn(
            CareGapEntity.GapCategory.SCREENING,
            CareGapEntity.GapCategory.PREVENTIVE_CARE
        );
    }

    /**
     * Test 12: Gap type generation includes measure ID
     */
    @Test
    @DisplayName("Should generate gap type that includes measure ID for deduplication")
    void shouldGenerateGapType_IncludingMeasureId() {
        // Given: Measure result
        measureResult = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId("CMS125")
            .measureName("Breast Cancer Screening")
            .denominatorEligible(true)
            .numeratorCompliant(false)
            .calculationDate(LocalDate.now())
            .build();

        when(careGapRepository.existsOpenCareGap(any(), any(), any())).thenReturn(false);
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When: Analyze measure result
        service.analyzeAndCreateCareGaps(measureResult);

        // Then: Gap type should include measure ID
        ArgumentCaptor<CareGapEntity> gapCaptor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(gapCaptor.capture());

        CareGapEntity createdGap = gapCaptor.getValue();
        assertThat(createdGap.getGapType().toLowerCase()).contains("cms125");
    }
}
