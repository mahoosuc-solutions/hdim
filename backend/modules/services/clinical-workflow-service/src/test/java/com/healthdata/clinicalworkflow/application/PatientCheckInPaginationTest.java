package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import com.healthdata.clinicalworkflow.domain.repository.PatientCheckInRepository;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for pagination support in PatientCheckInService
 *
 * Tests issue #292 implementation: Pagination for check-in history
 * with database-level pagination for efficient memory usage.
 */
@ExtendWith(MockitoExtension.class)
class PatientCheckInPaginationTest {

    @Mock
    private PatientCheckInRepository checkInRepository;

    @InjectMocks
    private PatientCheckInService patientCheckInService;

    private UUID patientId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        tenantId = "test-tenant";
    }

    @Test
    void shouldReturnFirstPageOfCheckInHistory() {
        // Given: 50 total check-in records, requesting first page (20 per page)
        List<PatientCheckInEntity> checkIns = createCheckInRecords(20);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(0, 20), 50);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history for first page
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Response contains first page with pagination metadata
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(50L);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalPages()).isEqualTo(3); // 50 records / 20 per page = 3 pages
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void shouldReturnMiddlePageOfCheckInHistory() {
        // Given: 50 total check-in records, requesting second page (page 1, 0-indexed)
        List<PatientCheckInEntity> checkIns = createCheckInRecords(20);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(1, 20), 50);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history for second page
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(1, 20));

        // Then: Response has both next and previous pages available
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(50L);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void shouldReturnLastPageOfCheckInHistory() {
        // Given: 50 total check-in records, requesting last page (10 records on page 2)
        List<PatientCheckInEntity> checkIns = createCheckInRecords(10);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(2, 20), 50);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history for last page
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(2, 20));

        // Then: Response contains partial last page with no next page
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(50L);
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void shouldReturnEmptyPageWhenNoCheckInsExist() {
        // Given: Patient has no check-in records
        Page<PatientCheckInEntity> emptyPage = new PageImpl<>(
                new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When: Get check-in history
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Response contains empty list with correct metadata
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void shouldHandleCustomPageSize() {
        // Given: 100 total check-in records, requesting page with size 10
        List<PatientCheckInEntity> checkIns = createCheckInRecords(10);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(0, 10), 100);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history with custom page size
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 10));

        // Then: Response respects custom page size
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(100L);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(10); // 100 / 10 = 10 pages
    }

    @Test
    void shouldHandleSinglePageResult() {
        // Given: Only 5 check-in records (less than page size)
        List<PatientCheckInEntity> checkIns = createCheckInRecords(5);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(0, 20), 5);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Response contains all records in single page
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(5L);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void shouldPassPageableToRepository() {
        // Given: Pageable with specific parameters
        Pageable pageable = PageRequest.of(2, 15);
        Page<PatientCheckInEntity> page = new PageImpl<>(
                createCheckInRecords(15), pageable, 50);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history with custom pageable
        patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, pageable);

        // Then: Pageable is passed to repository unchanged
        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), eq(pageable));
    }

    @Test
    void shouldHandleExactlyOneFullPage() {
        // Given: Exactly 20 records (one full page)
        List<PatientCheckInEntity> checkIns = createCheckInRecords(20);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(0, 20), 20);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Response contains exactly one page
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.getTotalElements()).isEqualTo(20L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void shouldHandleExactlyTwoFullPages() {
        // Given: Exactly 40 records (two full pages)
        List<PatientCheckInEntity> checkIns = createCheckInRecords(20);
        Page<PatientCheckInEntity> page = new PageImpl<>(checkIns, PageRequest.of(0, 20), 40);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history for first page
        Page<PatientCheckInEntity> result = patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Response shows 2 pages total with next page available
        assertThat(result.getTotalElements()).isEqualTo(40L);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void shouldConvertStringPatientIdToUuid() {
        // Given: String patient ID
        String patientIdString = patientId.toString();
        Page<PatientCheckInEntity> page = new PageImpl<>(
                createCheckInRecords(10), PageRequest.of(0, 20), 10);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history with string patient ID
        patientCheckInService.getCheckInHistory(
                tenantId, patientIdString, null, null, PageRequest.of(0, 20));

        // Then: String is converted to UUID and passed to repository
        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    @Test
    void shouldEnforceMultiTenantIsolation() {
        // Given: Different tenant ID
        String differentTenant = "other-tenant";
        Page<PatientCheckInEntity> page = new PageImpl<>(
                new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(differentTenant), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history for different tenant
        patientCheckInService.getCheckInHistory(
                differentTenant, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Repository query uses correct tenant ID
        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(differentTenant), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    @Test
    void shouldUseDefaultStartDateWhenNull() {
        // Given: No start date provided (null)
        Page<PatientCheckInEntity> page = new PageImpl<>(
                createCheckInRecords(10), PageRequest.of(0, 20), 10);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history without start date
        patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), null, null, PageRequest.of(0, 20));

        // Then: Repository query uses default start date (12 months ago)
        // We can't verify exact Instant, but we can verify the method was called
        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    @Test
    void shouldUseProvidedDateRange() {
        // Given: Specific start and end dates
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        Page<PatientCheckInEntity> page = new PageImpl<>(
                createCheckInRecords(10), PageRequest.of(0, 20), 10);

        when(checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When: Get check-in history with date range
        patientCheckInService.getCheckInHistory(
                tenantId, patientId.toString(), startDate, endDate, PageRequest.of(0, 20));

        // Then: Repository query uses provided date range
        // Dates are converted to ZonedDateTime and then to Instant
        Instant expectedStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant expectedEnd = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        verify(checkInRepository).findByTenantIdAndPatientIdAndCheckInTimeBetweenWithPagination(
                eq(tenantId), eq(patientId), eq(expectedStart), eq(expectedEnd), any(Pageable.class));
    }

    // ========== HELPER METHODS ==========

    /**
     * Create test check-in records
     *
     * @param count number of records to create
     * @return list of check-in entities
     */
    private List<PatientCheckInEntity> createCheckInRecords(int count) {
        List<PatientCheckInEntity> records = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            PatientCheckInEntity record = PatientCheckInEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .appointmentId("appointment-" + i)
                    .checkInTime(now.minusSeconds(i * 3600)) // 1 hour apart
                    .status("checked-in")
                    .insuranceVerified(false)
                    .build();
            records.add(record);
        }

        return records;
    }
}
