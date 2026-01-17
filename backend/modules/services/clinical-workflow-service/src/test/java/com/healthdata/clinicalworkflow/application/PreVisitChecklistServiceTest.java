package com.healthdata.clinicalworkflow.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import com.healthdata.clinicalworkflow.domain.repository.PreVisitChecklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreVisitChecklistServiceTest {

    @Mock
    private PreVisitChecklistRepository checklistRepository;

    @InjectMocks
    private PreVisitChecklistService checklistService;

    private static final String TENANT_ID = "TENANT001";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String APPOINTMENT_TYPE = "new-patient";

    private PreVisitChecklistEntity testChecklist;

    @BeforeEach
    void setUp() {
        testChecklist = PreVisitChecklistEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .appointmentType(APPOINTMENT_TYPE)
                .reviewMedicalHistory(false)
                .verifyInsurance(false)
                .updateDemographics(false)
                .reviewMedications(false)
                .reviewAllergies(false)
                .prepareVitalsEquipment(false)
                .reviewCareGaps(false)
                .obtainConsent(false)
                .completionPercentage(BigDecimal.ZERO)
                .status("pending")
                .build();
    }

    @Test
    void createChecklist_ShouldCreateNew_WhenCalled() {
        // Given
        when(checklistRepository.save(any())).thenReturn(testChecklist);

        // When
        PreVisitChecklistEntity result = checklistService.createChecklist(
                APPOINTMENT_TYPE, PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("pending");
        verify(checklistRepository).save(any(PreVisitChecklistEntity.class));
    }

    @Test
    void createChecklistForAppointment_ShouldLinkAppointment() {
        // Given
        String appointmentId = "Appointment/123";
        when(checklistRepository.save(any())).thenReturn(testChecklist);

        // When
        PreVisitChecklistEntity result = checklistService.createChecklistForAppointment(
                APPOINTMENT_TYPE, PATIENT_ID, appointmentId, TENANT_ID);

        // Then
        verify(checklistRepository, times(2)).save(any());
    }

    @Test
    void completeChecklistItem_ShouldMarkComplete_WhenValidItem() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PreVisitChecklistEntity result = checklistService.completeChecklistItem(
                checklistId, "reviewMedicalHistory", TENANT_ID);

        // Then
        assertThat(result.getReviewMedicalHistory()).isTrue();
        verify(checklistRepository).save(any());
    }

    @Test
    void completeChecklistItem_ShouldWork_WithUnderscoreFormat() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PreVisitChecklistEntity result = checklistService.completeChecklistItem(
                checklistId, "review_medical_history", TENANT_ID);

        // Then
        assertThat(result.getReviewMedicalHistory()).isTrue();
    }

    @Test
    void completeChecklistItem_ShouldThrowException_WhenInvalidItem() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When/Then
        assertThatThrownBy(() -> checklistService.completeChecklistItem(
                checklistId, "invalid_item", TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown checklist item");
    }

    @Test
    void completeChecklistItem_ShouldThrowException_WhenChecklistNotFound() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.completeChecklistItem(
                checklistId, "reviewMedicalHistory", TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Checklist not found");
    }

    @Test
    void getCompletionStatus_ShouldReturnStatus_WhenChecklist Exists() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setCompletionPercentage(new BigDecimal("50.00"));
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        PreVisitChecklistService.ChecklistCompletionStatus result =
                checklistService.getCompletionStatus(checklistId, TENANT_ID);

        // Then
        assertThat(result.getCompletionPercentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void getChecklistByAppointmentType_ShouldReturnChecklists() {
        // Given
        when(checklistRepository.findByAppointmentTypeAndTenant(
                APPOINTMENT_TYPE, TENANT_ID))
                .thenReturn(List.of(testChecklist));

        // When
        List<PreVisitChecklistEntity> result = checklistService.getChecklistByAppointmentType(
                APPOINTMENT_TYPE, TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getChecklistByPatient_ShouldReturnPatientChecklists() {
        // Given
        when(checklistRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testChecklist));

        // When
        List<PreVisitChecklistEntity> result = checklistService.getChecklistByPatient(
                PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void addCustomItem_ShouldAddItem_WhenValidChecklist() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PreVisitChecklistEntity result = checklistService.addCustomItem(
                checklistId, "Prepare consent forms", TENANT_ID);

        // Then
        assertThat(result.getCustomItems()).isNotNull();
        verify(checklistRepository).save(any());
    }

    @Test
    void addCustomItem_ShouldCreateArray_WhenNoExistingItems() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setCustomItems(null);
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PreVisitChecklistEntity result = checklistService.addCustomItem(
                checklistId, "Custom task", TENANT_ID);

        // Then
        assertThat(result.getCustomItems()).isNotNull();
        assertThat(result.getCustomItems().isArray()).isTrue();
    }

    @Test
    void getChecklistProgress_ShouldReturnProgress() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setReviewMedicalHistory(true);
        testChecklist.setVerifyInsurance(true);
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        PreVisitChecklistService.ChecklistProgress result =
                checklistService.getChecklistProgress(checklistId, TENANT_ID);

        // Then
        assertThat(result.getChecklistId()).isEqualTo(checklistId);
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
    }

    @Test
    void getChecklistById_ShouldReturnChecklist_WhenExists() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        PreVisitChecklistEntity result = checklistService.getChecklistById(
                checklistId, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void getChecklistById_ShouldThrowException_WhenNotFound() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.getChecklistById(
                checklistId, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Checklist not found");
    }

    @Test
    void getChecklistByAppointmentId_ShouldReturnChecklist() {
        // Given
        String appointmentId = "Appointment/123";
        when(checklistRepository.findChecklistByAppointmentId(appointmentId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        PreVisitChecklistEntity result = checklistService.getChecklistByAppointmentId(
                appointmentId, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void getIncompleteChecklists_ShouldReturnIncomplete() {
        // Given
        when(checklistRepository.findIncompleteChecklistsByTenant(TENANT_ID))
                .thenReturn(List.of(testChecklist));

        // When
        List<PreVisitChecklistEntity> result = checklistService.getIncompleteChecklists(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void countIncompleteChecklists_ShouldReturnCount() {
        // Given
        when(checklistRepository.countIncompleteChecklists(TENANT_ID))
                .thenReturn(3L);

        // When
        long result = checklistService.countIncompleteChecklists(TENANT_ID);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    void markCompleted_ShouldUpdateStatus_WhenValidChecklist() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PreVisitChecklistEntity result = checklistService.markCompleted(
                checklistId, "test-user", TENANT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getCompletedBy()).isEqualTo("test-user");
        assertThat(result.getCompletedAt()).isNotNull();
    }

    // ==================== 5f: getChecklistProgress parameter order adapter ====================

    @Test
    void getChecklistProgress_WithTenantFirst_ShouldReturnProgress() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setId(checklistId);
        testChecklist.setReviewMedicalHistory(true);
        testChecklist.setVerifyInsurance(true);
        testChecklist.setCompletionPercentage(new BigDecimal("25.00"));
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        PreVisitChecklistService.ChecklistProgress result =
                checklistService.getChecklistProgress(TENANT_ID, checklistId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChecklistId()).isEqualTo(checklistId);
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getAppointmentType()).isEqualTo(APPOINTMENT_TYPE);
        assertThat(result.getCompletionPercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    void getChecklistProgress_WithTenantFirst_ShouldThrowException_WhenNotFound() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.getChecklistProgress(TENANT_ID, checklistId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Checklist not found");
    }

    // ==================== 5g: getIncompleteCriticalItems ====================

    @Test
    void getIncompleteCriticalItems_ShouldReturnAll_WhenNoneCompleted() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setId(checklistId);
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        List<PreVisitChecklistService.ChecklistItemResponse> items =
                checklistService.getIncompleteCriticalItems(TENANT_ID, checklistId);

        // Then
        assertThat(items).hasSize(8); // All 8 critical items
        assertThat(items).extracting("name")
                .containsExactlyInAnyOrder(
                        "Review Medical History",
                        "Verify Insurance",
                        "Update Demographics",
                        "Review Medications",
                        "Review Allergies",
                        "Prepare Vitals Equipment",
                        "Review Care Gaps",
                        "Obtain Consent"
                );
        assertThat(items).allMatch(item -> !item.isCompleted());
        assertThat(items).allMatch(PreVisitChecklistService.ChecklistItemResponse::isRequired);
    }

    @Test
    void getIncompleteCriticalItems_ShouldReturnSubset_WhenSomeCompleted() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setId(checklistId);
        testChecklist.setReviewMedicalHistory(true);
        testChecklist.setVerifyInsurance(true);
        testChecklist.setObtainConsent(true);
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        List<PreVisitChecklistService.ChecklistItemResponse> items =
                checklistService.getIncompleteCriticalItems(TENANT_ID, checklistId);

        // Then
        assertThat(items).hasSize(5); // 5 remaining incomplete items
        assertThat(items).extracting("name")
                .containsExactlyInAnyOrder(
                        "Update Demographics",
                        "Review Medications",
                        "Review Allergies",
                        "Prepare Vitals Equipment",
                        "Review Care Gaps"
                );
        assertThat(items).allMatch(item -> !item.isCompleted());
    }

    @Test
    void getIncompleteCriticalItems_ShouldReturnEmpty_WhenAllCompleted() {
        // Given
        UUID checklistId = UUID.randomUUID();
        testChecklist.setId(checklistId);
        testChecklist.setReviewMedicalHistory(true);
        testChecklist.setVerifyInsurance(true);
        testChecklist.setUpdateDemographics(true);
        testChecklist.setReviewMedications(true);
        testChecklist.setReviewAllergies(true);
        testChecklist.setPrepareVitalsEquipment(true);
        testChecklist.setReviewCareGaps(true);
        testChecklist.setObtainConsent(true);
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.of(testChecklist));

        // When
        List<PreVisitChecklistService.ChecklistItemResponse> items =
                checklistService.getIncompleteCriticalItems(TENANT_ID, checklistId);

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    void getIncompleteCriticalItems_ShouldThrowException_WhenChecklistNotFound() {
        // Given
        UUID checklistId = UUID.randomUUID();
        when(checklistRepository.findByIdAndTenantId(checklistId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.getIncompleteCriticalItems(TENANT_ID, checklistId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Checklist not found");
    }
}
