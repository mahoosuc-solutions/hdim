package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.DiagnosticReportEntity;
import com.healthdata.fhir.persistence.DiagnosticReportRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for DiagnosticReportService.
 * Tests service layer logic including FHIR conversions, business rules, and event publishing.
 */
class DiagnosticReportServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final UUID REPORT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID ENCOUNTER_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
    private static final String CODE = "58410-2";
    private static final String CODE_DISPLAY = "Complete Blood Count";

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    @Mock
    private DiagnosticReportRepository diagnosticReportRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private DiagnosticReportService diagnosticReportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        diagnosticReportService = new DiagnosticReportService(diagnosticReportRepository, kafkaTemplate);
    }

    @Test
    void createDiagnosticReportShouldPersistAndPublishEvent() {
        // Given
        DiagnosticReport report = createFhirDiagnosticReport();
        DiagnosticReportEntity savedEntity = createDiagnosticReportEntity();

        when(diagnosticReportRepository.save(any(DiagnosticReportEntity.class))).thenReturn(savedEntity);

        // When
        DiagnosticReport result = diagnosticReportService.createDiagnosticReport(TENANT, report, "user-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(REPORT_ID.toString());

        verify(diagnosticReportRepository).save(any(DiagnosticReportEntity.class));
        verify(kafkaTemplate).send(eq("fhir.diagnostic-reports.created"), eq(REPORT_ID.toString()), any());
    }

    @Test
    void createDiagnosticReportShouldAssignIdIfNotPresent() {
        // Given
        DiagnosticReport report = new DiagnosticReport();
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        report.setSubject(new Reference("Patient/" + PATIENT_ID));
        report.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode(CODE)
                        .setDisplay(CODE_DISPLAY)));

        DiagnosticReportEntity savedEntity = DiagnosticReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("final")
                .code(CODE)
                .build();

        when(diagnosticReportRepository.save(any(DiagnosticReportEntity.class))).thenReturn(savedEntity);

        // When
        DiagnosticReport result = diagnosticReportService.createDiagnosticReport(TENANT, report, "user-1");

        // Then
        assertThat(result.hasId()).isTrue();
        verify(diagnosticReportRepository).save(any(DiagnosticReportEntity.class));
    }

    @Test
    void createDiagnosticReportShouldRejectMissingSubject() {
        // Given
        DiagnosticReport report = new DiagnosticReport();
        report.setId(REPORT_ID.toString());
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        // No subject set

        // When/Then
        assertThatThrownBy(() -> diagnosticReportService.createDiagnosticReport(TENANT, report, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void getDiagnosticReportShouldReturnFhirResource() {
        // Given
        DiagnosticReportEntity entity = createDiagnosticReportEntityWithJson();

        when(diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, REPORT_ID))
                .thenReturn(Optional.of(entity));

        // When
        Optional<DiagnosticReport> result = diagnosticReportService.getDiagnosticReport(TENANT, REPORT_ID);

        // Then
        assertThat(result).isPresent();
        DiagnosticReport report = result.get();
        assertThat(report.getIdElement().getIdPart()).isEqualTo(REPORT_ID.toString());
        assertThat(report.getStatus().toCode()).isEqualTo("final");
    }

    @Test
    void getDiagnosticReportShouldReturnEmptyWhenNotFound() {
        // Given
        when(diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, REPORT_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<DiagnosticReport> result = diagnosticReportService.getDiagnosticReport(TENANT, REPORT_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateDiagnosticReportShouldUpdateAndPublishEvent() {
        // Given
        DiagnosticReportEntity existingEntity = createDiagnosticReportEntityWithJson();
        DiagnosticReport updatedReport = createFhirDiagnosticReport();
        updatedReport.setStatus(DiagnosticReport.DiagnosticReportStatus.AMENDED);

        when(diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, REPORT_ID))
                .thenReturn(Optional.of(existingEntity));
        when(diagnosticReportRepository.save(any(DiagnosticReportEntity.class))).thenReturn(existingEntity);

        // When
        DiagnosticReport result = diagnosticReportService.updateDiagnosticReport(TENANT, REPORT_ID, updatedReport, "user-2");

        // Then
        assertThat(result).isNotNull();
        verify(diagnosticReportRepository).save(any(DiagnosticReportEntity.class));
        verify(kafkaTemplate).send(eq("fhir.diagnostic-reports.updated"), eq(REPORT_ID.toString()), any());
    }

    @Test
    void updateDiagnosticReportShouldThrowWhenNotFound() {
        // Given
        DiagnosticReport report = createFhirDiagnosticReport();
        when(diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, REPORT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> diagnosticReportService.updateDiagnosticReport(TENANT, REPORT_ID, report, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteDiagnosticReportShouldSoftDeleteAndPublishEvent() {
        // Given
        DiagnosticReportEntity entity = createDiagnosticReportEntityWithJson();
        when(diagnosticReportRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, REPORT_ID))
                .thenReturn(Optional.of(entity));
        when(diagnosticReportRepository.save(any(DiagnosticReportEntity.class))).thenReturn(entity);

        // When
        diagnosticReportService.deleteDiagnosticReport(TENANT, REPORT_ID, "user-3");

        // Then
        ArgumentCaptor<DiagnosticReportEntity> captor = ArgumentCaptor.forClass(DiagnosticReportEntity.class);
        verify(diagnosticReportRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("fhir.diagnostic-reports.deleted"), eq(REPORT_ID.toString()), any());
    }

    @Test
    void getReportsByPatientShouldReturnList() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(
                createDiagnosticReportEntityWithJson(),
                createDiagnosticReportEntityWithJson()
        );

        when(diagnosticReportRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getReportsByPatient(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void getFinalReportsShouldReturnOnlyFinal() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findFinalReportsForPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getFinalReports(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getReportsByEncounterShouldReturnList() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(TENANT, ENCOUNTER_ID))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getReportsByEncounter(TENANT, ENCOUNTER_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getLabReportsShouldReturnLabCategory() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findLabReportsForPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getLabReports(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getImagingReportsShouldReturnImagingCategory() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findImagingReportsForPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getImagingReports(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getPendingReportsShouldReturnPreliminary() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findPendingReports(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getPendingReports(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getLatestReportByCodeShouldReturnMostRecent() {
        // Given
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findLatestByCode(eq(TENANT), eq(PATIENT_ID), eq(CODE), any(PageRequest.class)))
                .thenReturn(entities);

        // When
        Optional<DiagnosticReport> result = diagnosticReportService.getLatestReportByCode(TENANT, PATIENT_ID, CODE);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void searchReportsShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());
        Page<DiagnosticReportEntity> entityPage = new PageImpl<>(entities, pageable, 1);

        when(diagnosticReportRepository.searchReports(eq(TENANT), eq(PATIENT_ID), any(), eq("final"), any(), any(), eq(pageable)))
                .thenReturn(entityPage);

        // When
        Page<DiagnosticReport> results = diagnosticReportService.searchReports(
                TENANT, PATIENT_ID, null, "final", null, null, pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getReportsByDateRangeShouldReturnReportsInRange() {
        // Given
        Instant startDate = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        Instant endDate = Instant.now();
        List<DiagnosticReportEntity> entities = List.of(createDiagnosticReportEntityWithJson());

        when(diagnosticReportRepository.findByEffectiveDateRange(TENANT, PATIENT_ID, startDate, endDate))
                .thenReturn(entities);

        // When
        List<DiagnosticReport> results = diagnosticReportService.getReportsByDateRange(TENANT, PATIENT_ID, startDate, endDate);

        // Then
        assertThat(results).hasSize(1);
    }

    // Helper methods

    private DiagnosticReport createFhirDiagnosticReport() {
        DiagnosticReport report = new DiagnosticReport();
        report.setId(REPORT_ID.toString());
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        report.setSubject(new Reference("Patient/" + PATIENT_ID));
        report.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode(CODE)
                        .setDisplay(CODE_DISPLAY)));
        report.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0074")
                        .setCode("LAB")
                        .setDisplay("Laboratory")));
        report.setEffective(new DateTimeType(new Date()));
        report.setIssued(new Date());
        report.setConclusion("All values within normal limits");
        report.addPerformer(new Reference("Organization/lab-1").setDisplay("Main Lab"));
        return report;
    }

    private DiagnosticReportEntity createDiagnosticReportEntity() {
        return DiagnosticReportEntity.builder()
                .id(REPORT_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("final")
                .code(CODE)
                .codeSystem("http://loinc.org")
                .codeDisplay(CODE_DISPLAY)
                .categoryCode("LAB")
                .categoryDisplay("Laboratory")
                .effectiveDatetime(Instant.now())
                .issuedDatetime(Instant.now())
                .conclusion("All values within normal limits")
                .performerReference("Organization/lab-1")
                .resultCount(0)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private DiagnosticReportEntity createDiagnosticReportEntityWithJson() {
        DiagnosticReport report = createFhirDiagnosticReport();
        String json = JSON_PARSER.encodeResourceToString(report);

        return DiagnosticReportEntity.builder()
                .id(REPORT_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .resourceJson(json)
                .status("final")
                .code(CODE)
                .codeSystem("http://loinc.org")
                .codeDisplay(CODE_DISPLAY)
                .categoryCode("LAB")
                .categoryDisplay("Laboratory")
                .effectiveDatetime(Instant.now())
                .issuedDatetime(Instant.now())
                .conclusion("All values within normal limits")
                .performerReference("Organization/lab-1")
                .resultCount(0)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
