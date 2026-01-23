package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.CheckInRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.ConsentRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.DemographicsUpdateRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.InsuranceVerificationRequest;
import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import com.healthdata.clinicalworkflow.domain.repository.PatientCheckInRepository;
import com.healthdata.clinicalworkflow.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientCheckInService
 *
 * Tests all 12 methods with:
 * - Happy path scenarios
 * - Edge cases
 * - Error handling
 * - Multi-tenant isolation
 */
@ExtendWith(MockitoExtension.class)
class PatientCheckInServiceTest {

    @Mock
    private PatientCheckInRepository checkInRepository;

    @InjectMocks
    private PatientCheckInService checkInService;

    private static final String TENANT_ID = "TENANT001";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String APPOINTMENT_ID = "Appointment/123";

    private PatientCheckInEntity testCheckIn;

    @BeforeEach
    void setUp() {
        testCheckIn = PatientCheckInEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(Instant.now())
                .checkedInBy("test-user")
                .status("checked-in")
                .insuranceVerified(false)
                .demographicsUpdated(false)
                .consentObtained(false)
                .build();
    }

    // ========== NEW ADAPTER METHOD TESTS (Tier 1 Fixes) ==========

    // ========== 1a. checkInPatient (DTO adapter) Tests ==========

    @Test
    void checkInPatient_WithRequest_ShouldCreateCheckIn_WhenValid() {
        // Given
        CheckInRequest request = new CheckInRequest();
        request.setPatientId(PATIENT_ID.toString());
        request.setAppointmentId(APPOINTMENT_ID);
        request.setCheckInTime(LocalDateTime.now());
        request.setInsuranceVerified(true);
        request.setConsentSigned(true);
        request.setDemographicsConfirmed(false);
        request.setNotes("Test notes");

        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.empty());
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.checkInPatient(
                TENANT_ID, request, "user-123");

        // Then
        assertThat(result).isNotNull();
        verify(checkInRepository).save(argThat(entity ->
                entity.getTenantId().equals(TENANT_ID) &&
                entity.getPatientId().equals(PATIENT_ID) &&
                entity.getAppointmentId().equals(APPOINTMENT_ID) &&
                entity.getCheckedInBy().equals("user-123") &&
                entity.getInsuranceVerified() == true &&
                entity.getConsentObtained() == true &&
                entity.getDemographicsUpdated() == false
        ));
    }

    @Test
    void checkInPatient_WithRequest_ShouldThrowException_WhenAlreadyCheckedIn() {
        // Given
        CheckInRequest request = new CheckInRequest();
        request.setPatientId(PATIENT_ID.toString());
        request.setAppointmentId(APPOINTMENT_ID);

        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.of(testCheckIn));

        // When/Then
        assertThatThrownBy(() -> checkInService.checkInPatient(TENANT_ID, request, "user-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already checked in");

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void checkInPatient_WithRequest_ShouldThrowException_WhenInvalidPatientId() {
        // Given
        CheckInRequest request = new CheckInRequest();
        request.setPatientId("invalid-uuid");
        request.setAppointmentId(APPOINTMENT_ID);

        // When/Then
        assertThatThrownBy(() -> checkInService.checkInPatient(TENANT_ID, request, "user-123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid patient ID format");

        verify(checkInRepository, never()).save(any());
    }

    // ========== 1b. getCheckIn Tests ==========

    @Test
    void getCheckIn_ShouldReturnCheckIn_WhenExists() {
        // Given
        UUID checkInId = UUID.randomUUID();
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.of(testCheckIn));

        // When
        PatientCheckInEntity result = checkInService.getCheckIn(TENANT_ID, checkInId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testCheckIn);
    }

    @Test
    void getCheckIn_ShouldThrowException_WhenNotFound() {
        // Given
        UUID checkInId = UUID.randomUUID();
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.getCheckIn(TENANT_ID, checkInId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Check-in");
    }

    // ========== 1c. getTodaysCheckIn Tests ==========

    @Test
    void getTodaysCheckIn_ShouldReturnCheckIn_WhenExistsToday() {
        // Given
        when(checkInRepository.findTodayCheckIns(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(testCheckIn));

        // When
        PatientCheckInEntity result = checkInService.getTodaysCheckIn(
                TENANT_ID, PATIENT_ID.toString());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testCheckIn);
    }

    @Test
    void getTodaysCheckIn_ShouldThrowException_WhenNotFoundToday() {
        // Given
        when(checkInRepository.findTodayCheckIns(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> checkInService.getTodaysCheckIn(TENANT_ID, PATIENT_ID.toString()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Check-in for patient");
    }

    // ========== 1d. getCheckInHistory (with pagination) Tests ==========

    @Test
    void getCheckInHistory_WithDateRange_ShouldReturnHistory() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<PatientCheckInEntity> page = new PageImpl<>(List.of(testCheckIn, testCheckIn), pageable, 2);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(TENANT_ID), eq(PATIENT_ID), any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(page);

        // When
        Page<PatientCheckInEntity> result = checkInService.getCheckInHistory(
                TENANT_ID, PATIENT_ID.toString(), startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(TENANT_ID), eq(PATIENT_ID), any(Instant.class), any(Instant.class), eq(pageable));
    }

    @Test
    void getCheckInHistory_WithNullDates_ShouldUseDefaults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PatientCheckInEntity> page = new PageImpl<>(List.of(testCheckIn), pageable, 1);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(TENANT_ID), eq(PATIENT_ID), any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(page);

        // When
        Page<PatientCheckInEntity> result = checkInService.getCheckInHistory(
                TENANT_ID, PATIENT_ID.toString(), null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(TENANT_ID), eq(PATIENT_ID), any(Instant.class), any(Instant.class), eq(pageable));
    }

    // ========== 1e. verifyInsurance (new signature) Tests ==========

    @Test
    void verifyInsurance_WithRequest_ShouldMarkVerified() {
        // Given
        UUID checkInId = UUID.randomUUID();
        InsuranceVerificationRequest request = new InsuranceVerificationRequest();
        request.setInsuranceProvider("Blue Cross");
        request.setVerified(true);

        testCheckIn.setId(checkInId);
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.verifyInsurance(
                TENANT_ID, checkInId, request, "user-123");

        // Then
        assertThat(result).isNotNull();
        verify(checkInRepository).save(argThat(entity ->
                entity.getInsuranceVerified() == true &&
                entity.getVerifiedBy().equals("user-123") &&
                entity.getNotes().contains("Blue Cross")
        ));
    }

    @Test
    void verifyInsurance_WithRequest_ShouldThrowException_WhenCheckInNotFound() {
        // Given
        UUID checkInId = UUID.randomUUID();
        InsuranceVerificationRequest request = new InsuranceVerificationRequest();

        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.verifyInsurance(
                TENANT_ID, checkInId, request, "user-123"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(checkInRepository, never()).save(any());
    }

    // ========== 1f. recordConsent (renamed from obtainConsent) Tests ==========

    @Test
    void recordConsent_ShouldMarkObtained() {
        // Given
        UUID checkInId = UUID.randomUUID();
        ConsentRequest request = new ConsentRequest();
        request.setConsentType("TREATMENT");
        request.setConsentObtained(true);

        testCheckIn.setId(checkInId);
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.recordConsent(
                TENANT_ID, checkInId, request, "user-123");

        // Then
        assertThat(result).isNotNull();
        verify(checkInRepository).save(argThat(entity ->
                entity.getConsentObtained() == true &&
                entity.getConsentObtainedBy().equals("user-123") &&
                entity.getNotes().contains("Treatment Consent")
        ));
    }

    @Test
    void recordConsent_ShouldThrowException_WhenCheckInNotFound() {
        // Given
        UUID checkInId = UUID.randomUUID();
        ConsentRequest request = new ConsentRequest();

        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.recordConsent(
                TENANT_ID, checkInId, request, "user-123"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(checkInRepository, never()).save(any());
    }

    // ========== 1g. updateDemographics (new signature) Tests ==========

    @Test
    void updateDemographics_WithRequest_ShouldMarkUpdated() {
        // Given
        UUID checkInId = UUID.randomUUID();
        DemographicsUpdateRequest request = new DemographicsUpdateRequest();
        request.setAddressChanged(true);
        request.setPhoneChanged(true);

        testCheckIn.setId(checkInId);
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.updateDemographics(
                TENANT_ID, checkInId, request, "user-123");

        // Then
        assertThat(result).isNotNull();
        verify(checkInRepository).save(argThat(entity ->
                entity.getDemographicsUpdated() == true &&
                entity.getDemographicsUpdatedBy().equals("user-123") &&
                entity.getNotes().contains("Demographics updated")
        ));
    }

    @Test
    void updateDemographics_WithRequest_ShouldThrowException_WhenCheckInNotFound() {
        // Given
        UUID checkInId = UUID.randomUUID();
        DemographicsUpdateRequest request = new DemographicsUpdateRequest();

        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.updateDemographics(
                TENANT_ID, checkInId, request, "user-123"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(checkInRepository, never()).save(any());
    }

    // ========== LEGACY INTERNAL METHOD TESTS ==========

    // ========== checkInPatient (internal) Tests ==========

    @Test
    void checkInPatient_Internal_ShouldCreateCheckIn_WhenValid() {
        // Given
        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.empty());
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.checkInPatientInternal(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        verify(checkInRepository).save(any(PatientCheckInEntity.class));
    }

    @Test
    void checkInPatient_Internal_ShouldThrowException_WhenAlreadyCheckedIn() {
        // Given
        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.of(testCheckIn));

        // When/Then
        assertThatThrownBy(() -> checkInService.checkInPatientInternal(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already checked in");

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void checkInPatient_Internal_ShouldAllowCheckIn_WhenNoAppointmentId() {
        // Given
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.checkInPatientInternal(
                PATIENT_ID, null, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        verify(checkInRepository).save(any(PatientCheckInEntity.class));
        verify(checkInRepository, never()).findByTenantIdAndAppointmentId(any(), any());
    }

    // ========== verifyInsurance (internal) Tests ==========

    @Test
    void verifyInsurance_Internal_ShouldMarkVerified_WhenCheckInExists() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.verifyInsuranceInternal(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result.getInsuranceVerified()).isTrue();
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getInsuranceVerified().equals(true)));
    }

    @Test
    void verifyInsurance_Internal_ShouldThrowException_WhenNoCheckInFound() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> checkInService.verifyInsuranceInternal(PATIENT_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No check-in record found");

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void verifyInsurance_Internal_ShouldUseMostRecent_WhenMultipleCheckIns() {
        // Given
        PatientCheckInEntity older = PatientCheckInEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .checkInTime(Instant.now().minusSeconds(3600))
                .build();

        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn, older));
        when(checkInRepository.save(any())).thenReturn(testCheckIn);

        // When
        checkInService.verifyInsuranceInternal(PATIENT_ID, TENANT_ID);

        // Then
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getId().equals(testCheckIn.getId())));
    }

    // ========== obtainConsent (internal) Tests ==========

    @Test
    void obtainConsent_Internal_ShouldMarkObtained_WhenCheckInExists() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.obtainConsentInternal(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result.getConsentObtained()).isTrue();
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getConsentObtained().equals(true)));
    }

    @Test
    void obtainConsent_Internal_ShouldThrowException_WhenNoCheckInFound() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> checkInService.obtainConsentInternal(PATIENT_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No check-in record found");
    }

    // ========== updateDemographics (internal) Tests ==========

    @Test
    void updateDemographics_Internal_ShouldMarkUpdated_WhenCheckInExists() {
        // Given
        Map<String, Object> demographics = Map.of("address", "123 Main St");
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.updateDemographicsInternal(
                PATIENT_ID, demographics, TENANT_ID);

        // Then
        assertThat(result.getDemographicsUpdated()).isTrue();
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getDemographicsUpdated().equals(true)));
    }

    @Test
    void updateDemographics_Internal_ShouldAppendToNotes_WhenDemographicsProvided() {
        // Given
        Map<String, Object> demographics = Map.of("phone", "555-1234");
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        checkInService.updateDemographicsInternal(PATIENT_ID, demographics, TENANT_ID);

        // Then
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getNotes() != null &&
                checkIn.getNotes().contains("Demographics updated")));
    }

    @Test
    void updateDemographics_Internal_ShouldWork_WhenNoDemographicsProvided() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any())).thenReturn(testCheckIn);

        // When
        checkInService.updateDemographicsInternal(PATIENT_ID, null, TENANT_ID);

        // Then
        verify(checkInRepository).save(any());
    }

    // ========== calculateWaitingTime Tests ==========

    @Test
    void calculateWaitingTime_ShouldReturnZero_WhenNoCheckIn() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When
        Integer result = checkInService.calculateWaitingTime(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isZero();
    }

    @Test
    void calculateWaitingTime_ShouldReturnStored_WhenAlreadyCalculated() {
        // Given
        testCheckIn.setWaitingTimeMinutes(30);
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));

        // When
        Integer result = checkInService.calculateWaitingTime(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isEqualTo(30);
    }

    @Test
    void calculateWaitingTime_ShouldCalculateCurrent_WhenNotStored() {
        // Given
        testCheckIn.setCheckInTime(Instant.now().minusSeconds(600)); // 10 minutes ago
        testCheckIn.setWaitingTimeMinutes(null);
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));

        // When
        Integer result = checkInService.calculateWaitingTime(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isGreaterThanOrEqualTo(9).isLessThanOrEqualTo(11);
    }

    // ========== getCheckInHistory (internal) Tests ==========

    @Test
    void getCheckInHistory_Internal_ShouldReturnAllCheckIns_WhenMultipleExist() {
        // Given
        List<PatientCheckInEntity> checkIns = List.of(testCheckIn, testCheckIn);
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(checkIns);

        // When
        List<PatientCheckInEntity> result = checkInService.getCheckInHistoryInternal(
                PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void getCheckInHistory_Internal_ShouldReturnEmpty_WhenNoCheckIns() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When
        List<PatientCheckInEntity> result = checkInService.getCheckInHistoryInternal(
                PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== findTodayCheckIns Tests ==========

    @Test
    void findTodayCheckIns_ShouldReturnTodayCheckIns_WhenCalled() {
        // Given
        when(checkInRepository.findTodayCheckIns(eq(TENANT_ID), any(), any()))
                .thenReturn(List.of(testCheckIn));

        // When
        List<PatientCheckInEntity> result = checkInService.findTodayCheckIns(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
        verify(checkInRepository).findTodayCheckIns(eq(TENANT_ID), any(), any());
    }

    // ========== getCheckInById Tests ==========

    @Test
    void getCheckInById_ShouldReturnCheckIn_WhenExists() {
        // Given
        UUID checkInId = UUID.randomUUID();
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.of(testCheckIn));

        // When
        PatientCheckInEntity result = checkInService.getCheckInById(checkInId, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void getCheckInById_ShouldThrowException_WhenNotFound() {
        // Given
        UUID checkInId = UUID.randomUUID();
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.getCheckInById(checkInId, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-in not found");
    }

    // ========== getCheckInByAppointment Tests ==========

    @Test
    void getCheckInByAppointment_ShouldReturnCheckIn_WhenExists() {
        // Given
        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.of(testCheckIn));

        // When
        PatientCheckInEntity result = checkInService.getCheckInByAppointment(
                APPOINTMENT_ID, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
    }

    @Test
    void getCheckInByAppointment_ShouldThrowException_WhenNotFound() {
        // Given
        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.getCheckInByAppointment(
                APPOINTMENT_ID, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-in not found for appointment");
    }

    // ========== countCheckIns Tests ==========

    @Test
    void countCheckIns_ShouldReturnCount_WhenCalled() {
        // Given
        when(checkInRepository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                .thenReturn(5L);

        // When
        long result = checkInService.countCheckIns(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    // ========== getCheckInsByStatus Tests ==========

    @Test
    void getCheckInsByStatus_ShouldReturnFiltered_WhenStatusProvided() {
        // Given
        when(checkInRepository.findByTenantIdAndStatusOrderByCheckInTimeDesc(
                TENANT_ID, "checked-in"))
                .thenReturn(List.of(testCheckIn));

        // When
        List<PatientCheckInEntity> result = checkInService.getCheckInsByStatus(
                "checked-in", TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("checked-in");
    }

    // ========== updateStatus Tests ==========

    @Test
    void updateStatus_ShouldUpdateStatus_WhenValidCheckIn() {
        // Given
        UUID checkInId = UUID.randomUUID();
        testCheckIn.setId(checkInId);
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.of(testCheckIn));
        when(checkInRepository.save(any())).thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.updateStatus(
                checkInId, "in-room", TENANT_ID);

        // Then
        verify(checkInRepository).save(argThat(checkIn ->
                "in-room".equals(checkIn.getStatus())));
    }

    @Test
    void updateStatus_ShouldThrowException_WhenCheckInNotFound() {
        // Given
        UUID checkInId = UUID.randomUUID();
        when(checkInRepository.findByIdAndTenantId(checkInId, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checkInService.updateStatus(
                checkInId, "in-room", TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
