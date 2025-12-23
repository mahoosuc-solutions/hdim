package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.quality.dto.AddressCareGapRequest;
import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.service.notification.CareGapNotificationTrigger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Service Tests")
class CareGapServiceTest {

    @Mock
    private CareGapRepository repository;

    @Mock
    private CareGapNotificationTrigger notificationTrigger;

    @InjectMocks
    private CareGapService careGapService;

    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    @DisplayName("Should return care gaps filtered by status")
    void shouldGetCareGapsByStatus() {
        CareGapEntity gap = baseGap()
            .status(CareGapEntity.Status.OPEN)
            .build();
        when(repository.findByTenantIdAndPatientIdAndStatusOrderByDueDateAsc(
            TENANT_ID, PATIENT_ID, CareGapEntity.Status.OPEN)).thenReturn(List.of(gap));

        List<CareGapDTO> result = careGapService.getPatientCareGaps(
            TENANT_ID, PATIENT_ID, "open", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("open");
    }

    @Test
    @DisplayName("Should return care gaps filtered by category")
    void shouldGetCareGapsByCategory() {
        CareGapEntity gap = baseGap()
            .category(CareGapEntity.GapCategory.MENTAL_HEALTH)
            .build();
        when(repository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            TENANT_ID, PATIENT_ID, CareGapEntity.GapCategory.MENTAL_HEALTH)).thenReturn(List.of(gap));

        List<CareGapDTO> result = careGapService.getPatientCareGaps(
            TENANT_ID, PATIENT_ID, null, "mental-health");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("mental-health");
    }

    @Test
    @DisplayName("Should return open care gaps when no filters provided")
    void shouldGetOpenCareGapsByDefault() {
        CareGapEntity gap = baseGap().build();
        when(repository.findOpenCareGaps(TENANT_ID, PATIENT_ID)).thenReturn(List.of(gap));

        List<CareGapDTO> result = careGapService.getPatientCareGaps(
            TENANT_ID, PATIENT_ID, null, null);

        assertThat(result).hasSize(1);
        verify(repository).findOpenCareGaps(TENANT_ID, PATIENT_ID);
    }

    @Test
    @DisplayName("Should address care gap and trigger notification")
    void shouldAddressCareGap() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .status(CareGapEntity.Status.OPEN)
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));
        when(repository.save(any(CareGapEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressCareGapRequest request = AddressCareGapRequest.builder()
            .addressedBy("clinician-1")
            .notes("completed screening")
            .status("addressed")
            .build();

        CareGapDTO dto = careGapService.addressCareGap(TENANT_ID, gapId, request);

        assertThat(dto.getStatus()).isEqualTo("addressed");
        assertThat(gap.getAddressedDate()).isNotNull();
        verify(notificationTrigger).onCareGapAddressed(eq(TENANT_ID), any(CareGapDTO.class));
    }

    @Test
    @DisplayName("Should not set addressed date for in-progress status")
    void shouldNotSetAddressedDateForInProgressStatus() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .status(CareGapEntity.Status.OPEN)
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));
        when(repository.save(any(CareGapEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressCareGapRequest request = AddressCareGapRequest.builder()
            .addressedBy("clinician-2")
            .notes("working on it")
            .status("in_progress")
            .build();

        CareGapDTO dto = careGapService.addressCareGap(TENANT_ID, gapId, request);

        assertThat(dto.getStatus()).isEqualTo("in-progress");
        assertThat(gap.getAddressedDate()).isNull();
    }

    @Test
    @DisplayName("Should not fail address flow when notification trigger throws")
    void shouldContinueWhenNotificationFails() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .status(CareGapEntity.Status.OPEN)
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));
        when(repository.save(any(CareGapEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("notify-fail"))
            .when(notificationTrigger).onCareGapAddressed(eq(TENANT_ID), any(CareGapDTO.class));

        AddressCareGapRequest request = AddressCareGapRequest.builder()
            .addressedBy("clinician-1")
            .notes("completed screening")
            .status("closed")
            .build();

        CareGapDTO dto = careGapService.addressCareGap(TENANT_ID, gapId, request);

        assertThat(dto.getStatus()).isEqualTo("closed");
    }

    @Test
    @DisplayName("Should reject care gap addressing when tenant mismatch")
    void shouldRejectAddressWhenTenantMismatch() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .tenantId("other-tenant")
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));

        AddressCareGapRequest request = AddressCareGapRequest.builder()
            .addressedBy("clinician-1")
            .notes("notes")
            .status("addressed")
            .build();

        assertThatThrownBy(() -> careGapService.addressCareGap(TENANT_ID, gapId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to tenant");
    }

    @Test
    @DisplayName("Should skip auto-close when gap already closed")
    void shouldSkipAutoCloseWhenAlreadyClosed() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .status(CareGapEntity.Status.CLOSED)
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));

        careGapService.autoCloseCareGap(TENANT_ID, gapId, "Observation", "obs-1", "code");

        verify(repository, never()).save(any(CareGapEntity.class));
    }

    @Test
    @DisplayName("Should reject auto-close when tenant mismatch")
    void shouldRejectAutoCloseWhenTenantMismatch() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .tenantId("other-tenant")
            .status(CareGapEntity.Status.OPEN)
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));

        assertThatThrownBy(() -> careGapService.autoCloseCareGap(
            TENANT_ID, gapId, "Observation", "obs-1", "code"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to tenant");
    }

    @Test
    @DisplayName("Should auto-close care gap and append evidence")
    void shouldAutoCloseCareGap() {
        UUID gapId = UUID.randomUUID();
        CareGapEntity gap = baseGap()
            .id(gapId)
            .status(CareGapEntity.Status.OPEN)
            .evidence("initial evidence")
            .build();
        when(repository.findById(gapId)).thenReturn(Optional.of(gap));
        when(repository.save(any(CareGapEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        careGapService.autoCloseCareGap(
            TENANT_ID, gapId, "Observation", "obs-1", "code-1");

        assertThat(gap.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(gap.getAutoClosed()).isTrue();
        assertThat(gap.getEvidence()).contains("Auto-closed");
        verify(repository).save(gap);
    }

    @Test
    @DisplayName("Should skip creating mental health follow-up when gap exists")
    void shouldSkipMentalHealthGapWhenExists() {
        MentalHealthAssessmentEntity assessment = baseAssessment()
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .build();
        when(repository.existsOpenCareGap(eq(TENANT_ID), eq(PATIENT_ID), any(String.class)))
            .thenReturn(true);

        careGapService.createMentalHealthFollowupGap(TENANT_ID, assessment);

        verify(repository, never()).save(any(CareGapEntity.class));
        verify(notificationTrigger, never()).onCareGapIdentified(any(), any());
    }

    @Test
    @DisplayName("Should create mental health follow-up gap and trigger notification")
    void shouldCreateMentalHealthGap() {
        MentalHealthAssessmentEntity assessment = baseAssessment()
            .type(MentalHealthAssessmentEntity.AssessmentType.GAD_7)
            .severity("severe")
            .score(18)
            .maxScore(21)
            .build();
        when(repository.existsOpenCareGap(eq(TENANT_ID), eq(PATIENT_ID), any(String.class)))
            .thenReturn(false);
        when(repository.save(any(CareGapEntity.class))).thenAnswer(invocation -> {
            CareGapEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        careGapService.createMentalHealthFollowupGap(TENANT_ID, assessment);

        verify(repository).save(any(CareGapEntity.class));
        verify(notificationTrigger).onCareGapIdentified(eq(TENANT_ID), any(CareGapDTO.class));
    }

    @Test
    @DisplayName("Should continue when care gap notification fails on creation")
    void shouldContinueWhenCareGapNotificationFailsOnCreation() {
        MentalHealthAssessmentEntity assessment = baseAssessment()
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .severity("moderately-severe")
            .build();
        when(repository.existsOpenCareGap(eq(TENANT_ID), eq(PATIENT_ID), any(String.class)))
            .thenReturn(false);
        when(repository.save(any(CareGapEntity.class))).thenAnswer(invocation -> {
            CareGapEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        doThrow(new RuntimeException("notify down"))
            .when(notificationTrigger).onCareGapIdentified(eq(TENANT_ID), any(CareGapDTO.class));

        careGapService.createMentalHealthFollowupGap(TENANT_ID, assessment);

        verify(repository).save(any(CareGapEntity.class));
    }

    @Test
    @DisplayName("Should handle priority defaults and due date for low severity")
    void shouldHandlePriorityDefaultsAndDueDateForLowSeverity() {
        MentalHealthAssessmentEntity assessment = baseAssessment()
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_2)
            .severity("unknown")
            .build();

        CareGapEntity.Priority priority = ReflectionTestUtils.invokeMethod(
            careGapService, "determinePriority", assessment);

        assertThat(priority).isEqualTo(CareGapEntity.Priority.LOW);

        Instant dueDate = ReflectionTestUtils.invokeMethod(
            careGapService, "calculateDueDate", CareGapEntity.Priority.LOW);

        assertThat(dueDate).isAfter(Instant.now().plusSeconds(60));
    }

    @Test
    @DisplayName("Should build PHQ-2 guidance and assessment naming")
    void shouldBuildPhq2GuidanceAndAssessmentNaming() {
        MentalHealthAssessmentEntity assessment = baseAssessment()
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_2)
            .build();

        String title = ReflectionTestUtils.invokeMethod(careGapService, "buildTitle", assessment);
        String recommendation = ReflectionTestUtils.invokeMethod(
            careGapService, "buildRecommendation", assessment);
        String name = ReflectionTestUtils.invokeMethod(
            careGapService, "getAssessmentName", MentalHealthAssessmentEntity.AssessmentType.PHQ_A);

        assertThat(title).contains("PHQ-2");
        assertThat(recommendation).contains("PHQ-9");
        assertThat(name).contains("Adolescents");
    }

    @Test
    @DisplayName("Should build description, evidence, and quality measure metadata")
    void shouldBuildDescriptionEvidenceAndQualityMeasure() {
        Instant assessmentDate = Instant.parse("2024-06-01T10:15:30Z");
        MentalHealthAssessmentEntity assessment = baseAssessment()
            .type(MentalHealthAssessmentEntity.AssessmentType.GAD_7)
            .severity("positive")
            .score(11)
            .maxScore(21)
            .assessmentDate(assessmentDate)
            .build();

        String description = ReflectionTestUtils.invokeMethod(
            careGapService, "buildDescription", assessment);
        String evidence = ReflectionTestUtils.invokeMethod(
            careGapService, "buildEvidence", assessment);
        String measure = ReflectionTestUtils.invokeMethod(
            careGapService, "getQualityMeasure", assessment);
        String defaultMeasure = ReflectionTestUtils.invokeMethod(
            careGapService, "getQualityMeasure",
            baseAssessment()
                .type(MentalHealthAssessmentEntity.AssessmentType.MDQ)
                .severity("mild")
                .assessmentDate(assessmentDate)
                .build());

        assertThat(description).contains("Generalized Anxiety Disorder-7");
        assertThat(description).contains("11/21");
        assertThat(evidence).contains("GAD_7");
        assertThat(evidence).contains("positive");
        assertThat(measure).isEqualTo("CMS2");
        assertThat(defaultMeasure).isNull();
    }

    private CareGapEntity.CareGapEntityBuilder baseGap() {
        return CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .category(CareGapEntity.GapCategory.MENTAL_HEALTH)
            .gapType("mental-health-followup-phq-9")
            .title("PHQ-9 Positive Screen - Follow-up Required")
            .description("desc")
            .priority(CareGapEntity.Priority.HIGH)
            .status(CareGapEntity.Status.OPEN)
            .qualityMeasure("CMS2")
            .recommendation("follow up")
            .evidence("evidence")
            .dueDate(Instant.now().plusSeconds(3600))
            .identifiedDate(Instant.now());
    }

    private MentalHealthAssessmentEntity.MentalHealthAssessmentEntityBuilder baseAssessment() {
        return MentalHealthAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .score(20)
            .maxScore(27)
            .severity("moderate")
            .interpretation("interpretation")
            .positiveScreen(true)
            .thresholdScore(10)
            .requiresFollowup(true)
            .assessedBy("clinician-1")
            .assessmentDate(Instant.now())
            .responses(java.util.Map.of());
    }
}
