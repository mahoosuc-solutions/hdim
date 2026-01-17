package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import com.healthdata.clinicalworkflow.domain.repository.PatientCheckInRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
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

    // ========== checkInPatient Tests ==========

    @Test
    void checkInPatient_ShouldCreateCheckIn_WhenValid() {
        // Given
        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.empty());
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.checkInPatient(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        verify(checkInRepository).save(any(PatientCheckInEntity.class));
    }

    @Test
    void checkInPatient_ShouldThrowException_WhenAlreadyCheckedIn() {
        // Given
        when(checkInRepository.findByTenantIdAndAppointmentId(TENANT_ID, APPOINTMENT_ID))
                .thenReturn(Optional.of(testCheckIn));

        // When/Then
        assertThatThrownBy(() -> checkInService.checkInPatient(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already checked in");

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void checkInPatient_ShouldAllowCheckIn_WhenNoAppointmentId() {
        // Given
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.checkInPatient(
                PATIENT_ID, null, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
        verify(checkInRepository).save(any(PatientCheckInEntity.class));
        verify(checkInRepository, never()).findByTenantIdAndAppointmentId(any(), any());
    }

    // ========== verifyInsurance Tests ==========

    @Test
    void verifyInsurance_ShouldMarkVerified_WhenCheckInExists() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.verifyInsurance(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result.getInsuranceVerified()).isTrue();
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getInsuranceVerified().equals(true)));
    }

    @Test
    void verifyInsurance_ShouldThrowException_WhenNoCheckInFound() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> checkInService.verifyInsurance(PATIENT_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No check-in record found");

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void verifyInsurance_ShouldUseMostRecent_WhenMultipleCheckIns() {
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
        checkInService.verifyInsurance(PATIENT_ID, TENANT_ID);

        // Then
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getId().equals(testCheckIn.getId())));
    }

    // ========== obtainConsent Tests ==========

    @Test
    void obtainConsent_ShouldMarkObtained_WhenCheckInExists() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.obtainConsent(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result.getConsentObtained()).isTrue();
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getConsentObtained().equals(true)));
    }

    @Test
    void obtainConsent_ShouldThrowException_WhenNoCheckInFound() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> checkInService.obtainConsent(PATIENT_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No check-in record found");
    }

    // ========== updateDemographics Tests ==========

    @Test
    void updateDemographics_ShouldMarkUpdated_WhenCheckInExists() {
        // Given
        Map<String, Object> demographics = Map.of("address", "123 Main St");
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenReturn(testCheckIn);

        // When
        PatientCheckInEntity result = checkInService.updateDemographics(
                PATIENT_ID, demographics, TENANT_ID);

        // Then
        assertThat(result.getDemographicsUpdated()).isTrue();
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getDemographicsUpdated().equals(true)));
    }

    @Test
    void updateDemographics_ShouldAppendToNotes_WhenDemographicsProvided() {
        // Given
        Map<String, Object> demographics = Map.of("phone", "555-1234");
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any(PatientCheckInEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        checkInService.updateDemographics(PATIENT_ID, demographics, TENANT_ID);

        // Then
        verify(checkInRepository).save(argThat(checkIn ->
                checkIn.getNotes() != null &&
                checkIn.getNotes().contains("Demographics updated")));
    }

    @Test
    void updateDemographics_ShouldWork_WhenNoDemographicsProvided() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(List.of(testCheckIn));
        when(checkInRepository.save(any())).thenReturn(testCheckIn);

        // When
        checkInService.updateDemographics(PATIENT_ID, null, TENANT_ID);

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

    // ========== getCheckInHistory Tests ==========

    @Test
    void getCheckInHistory_ShouldReturnAllCheckIns_WhenMultipleExist() {
        // Given
        List<PatientCheckInEntity> checkIns = List.of(testCheckIn, testCheckIn);
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(checkIns);

        // When
        List<PatientCheckInEntity> result = checkInService.getCheckInHistory(
                PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void getCheckInHistory_ShouldReturnEmpty_WhenNoCheckIns() {
        // Given
        when(checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                TENANT_ID, PATIENT_ID))
                .thenReturn(Collections.emptyList());

        // When
        List<PatientCheckInEntity> result = checkInService.getCheckInHistory(
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
