package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.domain.model.OutreachLogEntity;
import com.healthdata.nurseworkflow.domain.repository.OutreachLogRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutreachLogService
 *
 * Uses TDD approach with clear test structure:
 * - Given: Setup test data
 * - When: Execute service method
 * - Then: Assert expected results
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutreachLogService")
class OutreachLogServiceTest {

    @Mock
    private OutreachLogRepository outreachLogRepository;

    @InjectMocks
    private OutreachLogService outreachLogService;

    private String tenantId;
    private UUID patientId;
    private UUID nurseId;
    private OutreachLogEntity testOutreachLog;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT001";
        patientId = UUID.randomUUID();
        nurseId = UUID.randomUUID();

        testOutreachLog = OutreachLogEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .nurseId(nurseId)
            .outcomeType(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT)
            .contactMethod(OutreachLogEntity.ContactMethod.PHONE)
            .reason("post-discharge")
            .notes("Patient doing well, no concerns")
            .attemptedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("should create outreach log with valid data")
    void testCreateOutreachLog_Success() {
        // Given
        when(outreachLogRepository.save(any(OutreachLogEntity.class)))
            .thenReturn(testOutreachLog);

        // When
        OutreachLogEntity result = outreachLogService.createOutreachLog(testOutreachLog);

        // Then
        assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("tenantId", tenantId)
            .hasFieldOrPropertyWithValue("patientId", patientId)
            .hasFieldOrPropertyWithValue("outcomeType", OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT);

        verify(outreachLogRepository, times(1)).save(any(OutreachLogEntity.class));
    }

    @Test
    @DisplayName("should retrieve outreach log by ID")
    void testGetOutreachLogById_Success() {
        // Given
        when(outreachLogRepository.findById(testOutreachLog.getId()))
            .thenReturn(Optional.of(testOutreachLog));

        // When
        Optional<OutreachLogEntity> result = outreachLogService.getOutreachLogById(testOutreachLog.getId());

        // Then
        assertThat(result)
            .isPresent()
            .hasValue(testOutreachLog);

        verify(outreachLogRepository, times(1)).findById(testOutreachLog.getId());
    }

    @Test
    @DisplayName("should return empty when outreach log not found")
    void testGetOutreachLogById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(outreachLogRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When
        Optional<OutreachLogEntity> result = outreachLogService.getOutreachLogById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should retrieve patient outreach history with pagination")
    void testGetPatientOutreachHistory_Success() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<OutreachLogEntity> mockPage = new PageImpl<>(List.of(testOutreachLog), pageRequest, 1);

        when(outreachLogRepository.findByTenantIdAndPatientIdOrderByAttemptedAtDesc(
            tenantId, patientId, pageRequest))
            .thenReturn(mockPage);

        // When
        Page<OutreachLogEntity> result = outreachLogService.getPatientOutreachHistory(
            tenantId, patientId, pageRequest);

        // Then
        assertThat(result)
            .isNotEmpty()
            .hasSize(1)
            .contains(testOutreachLog);

        verify(outreachLogRepository, times(1))
            .findByTenantIdAndPatientIdOrderByAttemptedAtDesc(tenantId, patientId, pageRequest);
    }

    @Test
    @DisplayName("should retrieve outreach logs by outcome type")
    void testGetOutreachByOutcomeType_Success() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<OutreachLogEntity> mockPage = new PageImpl<>(List.of(testOutreachLog), pageRequest, 1);

        when(outreachLogRepository.findByTenantIdAndOutcomeTypeOrderByAttemptedAtDesc(
            tenantId, OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT, pageRequest))
            .thenReturn(mockPage);

        // When
        Page<OutreachLogEntity> result = outreachLogService.getOutreachByOutcomeType(
            tenantId, OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT, pageRequest);

        // Then
        assertThat(result)
            .isNotEmpty()
            .hasSize(1)
            .extracting(OutreachLogEntity::getOutcomeType)
            .containsOnly(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT);
    }

    @Test
    @DisplayName("should update outreach log")
    void testUpdateOutreachLog_Success() {
        // Given
        testOutreachLog.setNotes("Updated notes after follow-up");
        when(outreachLogRepository.save(testOutreachLog))
            .thenReturn(testOutreachLog);

        // When
        OutreachLogEntity result = outreachLogService.updateOutreachLog(testOutreachLog);

        // Then
        assertThat(result.getNotes())
            .isEqualTo("Updated notes after follow-up");

        verify(outreachLogRepository, times(1)).save(testOutreachLog);
    }

    @Test
    @DisplayName("should count patient outreach attempts")
    void testCountPatientOutreach_Success() {
        // Given
        when(outreachLogRepository.countByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(5L);

        // When
        long result = outreachLogService.countPatientOutreach(tenantId, patientId);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("should retrieve successful contacts for patient")
    void testGetSuccessfulContacts_Success() {
        // Given
        OutreachLogEntity successfulContact = testOutreachLog;
        when(outreachLogRepository.findByTenantIdAndPatientIdAndOutcomeTypeOrderByAttemptedAtDesc(
            tenantId, patientId, OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT))
            .thenReturn(List.of(successfulContact));

        // When
        List<OutreachLogEntity> result = outreachLogService.getSuccessfulContacts(tenantId, patientId);

        // Then
        assertThat(result)
            .hasSize(1)
            .extracting(OutreachLogEntity::getOutcomeType)
            .containsOnly(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT);
    }

    @Test
    @DisplayName("should validate multi-tenant isolation")
    void testMultiTenantIsolation() {
        // Given - two different tenants
        String tenant1 = "TENANT001";
        String tenant2 = "TENANT002";
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<OutreachLogEntity> tenant1Page = new PageImpl<>(List.of(testOutreachLog), pageRequest, 1);
        Page<OutreachLogEntity> tenant2Page = new PageImpl<>(List.of(), pageRequest, 0);

        when(outreachLogRepository.findByTenantIdAndPatientIdOrderByAttemptedAtDesc(
            tenant1, patientId, pageRequest))
            .thenReturn(tenant1Page);

        when(outreachLogRepository.findByTenantIdAndPatientIdOrderByAttemptedAtDesc(
            tenant2, patientId, pageRequest))
            .thenReturn(tenant2Page);

        // When
        Page<OutreachLogEntity> result1 = outreachLogService.getPatientOutreachHistory(
            tenant1, patientId, pageRequest);
        Page<OutreachLogEntity> result2 = outreachLogService.getPatientOutreachHistory(
            tenant2, patientId, pageRequest);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(0);
    }
}
