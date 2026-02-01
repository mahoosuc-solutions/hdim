package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.VitalsHistoryResponse;
import com.healthdata.clinicalworkflow.client.FhirServiceClient;
import com.healthdata.clinicalworkflow.client.PatientServiceClient;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for pagination support in VitalSignsService
 *
 * Tests issue #290 implementation: Pagination for vital signs history
 * with database-level pagination for efficient memory usage.
 */
@ExtendWith(MockitoExtension.class)
class VitalSignsPaginationTest {

    @Mock
    private VitalSignsRecordRepository vitalsRepository;

    @Mock
    private RoomAssignmentRepository roomAssignmentRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private VitalSignsService vitalSignsService;

    private UUID patientId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        tenantId = "test-tenant";
    }

    @Test
    void shouldReturnFirstPageOfVitalsHistory() {
        // Given: 50 total vitals records, requesting first page (20 per page)
        List<VitalSignsRecordEntity> vitals = createVitalRecords(20);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(0, 20), 50);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history for first page
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(0, 20));

        // Then: Response contains first page with pagination metadata
        assertThat(response.getVitals()).hasSize(20);
        assertThat(response.getTotalRecords()).isEqualTo(50L);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(20);
        assertThat(response.getTotalPages()).isEqualTo(3); // 50 records / 20 per page = 3 pages
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getHasPrevious()).isFalse();
    }

    @Test
    void shouldReturnMiddlePageOfVitalsHistory() {
        // Given: 50 total vitals records, requesting second page (page 1, 0-indexed)
        List<VitalSignsRecordEntity> vitals = createVitalRecords(20);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(1, 20), 50);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history for second page
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(1, 20));

        // Then: Response has both next and previous pages available
        assertThat(response.getVitals()).hasSize(20);
        assertThat(response.getTotalRecords()).isEqualTo(50L);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getHasPrevious()).isTrue();
    }

    @Test
    void shouldReturnLastPageOfVitalsHistory() {
        // Given: 50 total vitals records, requesting last page (10 records on page 2)
        List<VitalSignsRecordEntity> vitals = createVitalRecords(10);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(2, 20), 50);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history for last page
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(2, 20));

        // Then: Response contains partial last page with no next page
        assertThat(response.getVitals()).hasSize(10);
        assertThat(response.getTotalRecords()).isEqualTo(50L);
        assertThat(response.getCurrentPage()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getHasPrevious()).isTrue();
    }

    @Test
    void shouldReturnEmptyPageWhenNoVitalsExist() {
        // Given: Patient has no vitals records
        Page<VitalSignsRecordEntity> emptyPage = new PageImpl<>(
                new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When: Get vitals history
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(0, 20));

        // Then: Response contains empty list with correct metadata
        assertThat(response.getVitals()).isEmpty();
        assertThat(response.getTotalRecords()).isEqualTo(0L);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getHasPrevious()).isFalse();
    }

    @Test
    void shouldHandleCustomPageSize() {
        // Given: 100 total vitals records, requesting page with size 10
        List<VitalSignsRecordEntity> vitals = createVitalRecords(10);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(0, 10), 100);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history with custom page size
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(0, 10));

        // Then: Response respects custom page size
        assertThat(response.getVitals()).hasSize(10);
        assertThat(response.getTotalRecords()).isEqualTo(100L);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(10); // 100 / 10 = 10 pages
    }

    @Test
    void shouldHandleSinglePageResult() {
        // Given: Only 5 vitals records (less than page size)
        List<VitalSignsRecordEntity> vitals = createVitalRecords(5);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(0, 20), 5);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(0, 20));

        // Then: Response contains all records in single page
        assertThat(response.getVitals()).hasSize(5);
        assertThat(response.getTotalRecords()).isEqualTo(5L);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getHasPrevious()).isFalse();
    }

    @Test
    void shouldPassPageableToRepository() {
        // Given: Pageable with specific parameters
        Pageable pageable = PageRequest.of(2, 15);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(
                createVitalRecords(15), pageable, 50);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history with custom pageable
        vitalSignsService.getVitalsHistory(tenantId, patientId.toString(), pageable);

        // Then: Pageable is passed to repository unchanged
        verify(vitalsRepository).findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), eq(pageable));
    }

    @Test
    void shouldHandleExactlyOneFullPage() {
        // Given: Exactly 20 records (one full page)
        List<VitalSignsRecordEntity> vitals = createVitalRecords(20);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(0, 20), 20);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(0, 20));

        // Then: Response contains exactly one page
        assertThat(response.getVitals()).hasSize(20);
        assertThat(response.getTotalRecords()).isEqualTo(20L);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getHasPrevious()).isFalse();
    }

    @Test
    void shouldHandleExactlyTwoFullPages() {
        // Given: Exactly 40 records (two full pages)
        List<VitalSignsRecordEntity> vitals = createVitalRecords(20);
        Page<VitalSignsRecordEntity> page = new PageImpl<>(vitals, PageRequest.of(0, 20), 40);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history for first page
        VitalsHistoryResponse response = vitalSignsService.getVitalsHistory(
                tenantId, patientId.toString(), PageRequest.of(0, 20));

        // Then: Response shows 2 pages total with next page available
        assertThat(response.getTotalRecords()).isEqualTo(40L);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getHasNext()).isTrue();
    }

    @Test
    void shouldConvertStringPatientIdToUuid() {
        // Given: String patient ID
        String patientIdString = patientId.toString();
        Page<VitalSignsRecordEntity> page = new PageImpl<>(
                createVitalRecords(10), PageRequest.of(0, 20), 10);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history with string patient ID
        vitalSignsService.getVitalsHistory(tenantId, patientIdString, PageRequest.of(0, 20));

        // Then: String is converted to UUID and passed to repository
        verify(vitalsRepository).findByTenantIdAndPatientIdWithPagination(
                eq(tenantId), eq(patientId), any(Pageable.class));
    }

    @Test
    void shouldEnforceMultiTenantIsolation() {
        // Given: Different tenant ID
        String differentTenant = "other-tenant";
        Page<VitalSignsRecordEntity> page = new PageImpl<>(
                new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(vitalsRepository.findByTenantIdAndPatientIdWithPagination(
                eq(differentTenant), eq(patientId), any(Pageable.class)))
                .thenReturn(page);

        // When: Get vitals history for different tenant
        vitalSignsService.getVitalsHistory(differentTenant, patientId.toString(), PageRequest.of(0, 20));

        // Then: Repository query uses correct tenant ID
        verify(vitalsRepository).findByTenantIdAndPatientIdWithPagination(
                eq(differentTenant), eq(patientId), any(Pageable.class));
    }

    // ========== HELPER METHODS ==========

    /**
     * Create test vital signs records
     *
     * @param count number of records to create
     * @return list of vital signs entities
     */
    private List<VitalSignsRecordEntity> createVitalRecords(int count) {
        List<VitalSignsRecordEntity> records = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            VitalSignsRecordEntity record = VitalSignsRecordEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .encounterId("encounter-" + i)
                    .recordedBy("ma-smith")
                    .systolicBp(new BigDecimal("120"))
                    .diastolicBp(new BigDecimal("80"))
                    .heartRate(new BigDecimal("75"))
                    .temperatureF(new BigDecimal("98.6"))
                    .respirationRate(new BigDecimal("16"))
                    .oxygenSaturation(new BigDecimal("98"))
                    .recordedAt(now.minusSeconds(i * 3600)) // 1 hour apart
                    .alertStatus("normal")
                    .build();
            records.add(record);
        }

        return records;
    }
}
