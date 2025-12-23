package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.AllergyIntoleranceEntity;
import com.healthdata.fhir.persistence.AllergyIntoleranceRepository;
import com.healthdata.fhir.bulk.BulkExportRepository;
import com.healthdata.fhir.persistence.ConditionEntity;
import com.healthdata.fhir.persistence.ConditionRepository;
import com.healthdata.fhir.persistence.EncounterEntity;
import com.healthdata.fhir.persistence.EncounterRepository;
import com.healthdata.fhir.persistence.ImmunizationEntity;
import com.healthdata.fhir.persistence.ImmunizationRepository;
import com.healthdata.fhir.persistence.MedicationRequestEntity;
import com.healthdata.fhir.persistence.MedicationRequestRepository;
import com.healthdata.fhir.persistence.ObservationEntity;
import com.healthdata.fhir.persistence.ObservationRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;
import com.healthdata.fhir.persistence.ProcedureEntity;
import com.healthdata.fhir.persistence.ProcedureRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
class BulkExportProcessorTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @TempDir
    Path tempDir;

    @Mock
    private BulkExportRepository exportRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private MedicationRequestRepository medicationRequestRepository;

    @Mock
    private ProcedureRepository procedureRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private AllergyIntoleranceRepository allergyIntoleranceRepository;

    @Mock
    private ImmunizationRepository immunizationRepository;

    @Test
    void shouldProcessExportAndWriteOutputFiles() throws Exception {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());
        config.setChunkSize(1);
        config.setBaseUrl("http://localhost:8080/fhir");

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Patient", "AllergyIntolerance", "Immunization"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientEntity patientEntity = PatientEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson(JSON_PARSER.encodeResourceToString(new Patient()))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        when(patientRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(patientEntity)));

        AllergyIntoleranceEntity allergyEntity = AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .patientId(UUID.randomUUID())
                .code("227037002")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .build();
        when(allergyIntoleranceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(allergyEntity)));

        ImmunizationEntity immunizationEntity = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .patientId(UUID.randomUUID())
                .vaccineCode("207")
                .status("completed")
                .occurrenceDate(java.time.LocalDate.now())
                .build();
        when(immunizationRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(immunizationEntity)));

        processor.processExport(jobId).join();

        ArgumentCaptor<BulkExportJob> saveCaptor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository, org.mockito.Mockito.atLeastOnce()).save(saveCaptor.capture());
        BulkExportJob completedJob = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);

        assertThat(completedJob.getStatus()).isEqualTo(BulkExportJob.ExportStatus.COMPLETED);
        assertThat(completedJob.getOutputFiles()).hasSize(3);
        completedJob.getOutputFiles().forEach(file ->
                assertThat(Files.exists(Path.of(file.getFilePath()))).isTrue());

        verify(kafkaTemplate).send(eq("fhir.bulk-export.completed"), eq(jobId.toString()), any());
    }

    @Test
    void shouldDeleteExportFiles() throws Exception {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        Path jobDir = tempDir.resolve(jobId.toString());
        Files.createDirectories(jobDir);
        Files.writeString(jobDir.resolve("patient.ndjson"), "{}");

        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .requestedAt(Instant.now())
                .build();

        processor.deleteExportFiles(job);

        assertThat(Files.exists(jobDir)).isFalse();
    }

    @Test
    void shouldExportAdditionalResourceTypes() {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());
        config.setChunkSize(1);

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Observation", "Condition", "MedicationRequest", "Procedure", "Encounter"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Observation observation = new Observation();
        observation.setId(UUID.randomUUID().toString());
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        observation.setCode(new CodeableConcept().setText("vitals"));
        when(observationRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(ObservationEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-1")
                        .resourceType("Observation")
                        .resourceJson(JSON_PARSER.encodeResourceToString(observation))
                        .patientId(UUID.randomUUID())
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(0)
                        .build())));

        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());
        condition.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        condition.setCode(new CodeableConcept().setText("condition"));
        when(conditionRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(ConditionEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-1")
                        .resourceType("Condition")
                        .resourceJson(JSON_PARSER.encodeResourceToString(condition))
                        .patientId(UUID.randomUUID())
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(0)
                        .build())));

        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setId(UUID.randomUUID().toString());
        medicationRequest.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        when(medicationRequestRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(MedicationRequestEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-1")
                        .resourceType("MedicationRequest")
                        .resourceJson(JSON_PARSER.encodeResourceToString(medicationRequest))
                        .patientId(UUID.randomUUID())
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(0)
                        .build())));

        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID().toString());
        procedure.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setCode(new CodeableConcept().setText("procedure"));
        when(procedureRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(ProcedureEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-1")
                        .resourceType("Procedure")
                        .resourceJson(JSON_PARSER.encodeResourceToString(procedure))
                        .patientId(UUID.randomUUID())
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(0)
                        .build())));

        Encounter encounter = new Encounter();
        encounter.setId(UUID.randomUUID().toString());
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        when(encounterRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(EncounterEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-1")
                        .resourceType("Encounter")
                        .resourceJson(JSON_PARSER.encodeResourceToString(encounter))
                        .patientId(UUID.randomUUID())
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(0)
                        .build())));

        processor.processExport(jobId).join();

        ArgumentCaptor<BulkExportJob> saveCaptor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository, org.mockito.Mockito.atLeastOnce()).save(saveCaptor.capture());
        BulkExportJob completedJob = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);

        assertThat(completedJob.getStatus()).isEqualTo(BulkExportJob.ExportStatus.COMPLETED);
        assertThat(completedJob.getOutputFiles()).hasSize(5);
    }

    @Test
    void shouldMarkJobFailedWhenProcessingThrows() {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Patient"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class)))
                .thenThrow(new RuntimeException("boom"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processor.processExport(jobId).join();

        verify(kafkaTemplate).send(eq("fhir.bulk-export.failed"), eq(jobId.toString()), any());
    }

    @Test
    void shouldSkipUnknownResourceTypes() {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());
        config.setChunkSize(1);

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("UnknownResource"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        processor.processExport(jobId).join();

        ArgumentCaptor<BulkExportJob> saveCaptor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository, org.mockito.Mockito.atLeastOnce()).save(saveCaptor.capture());
        BulkExportJob completedJob = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);

        assertThat(completedJob.getStatus()).isEqualTo(BulkExportJob.ExportStatus.COMPLETED);
        assertThat(completedJob.getOutputFiles()).isEmpty();
    }

    @Test
    void shouldHandleInvalidResourceJson() {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());
        config.setChunkSize(1);

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Patient"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientEntity patientEntity = PatientEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\"")
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        when(patientRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(patientEntity)));

        processor.processExport(jobId).join();

        ArgumentCaptor<BulkExportJob> saveCaptor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository, org.mockito.Mockito.atLeastOnce()).save(saveCaptor.capture());
        BulkExportJob completedJob = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);

        assertThat(completedJob.getStatus()).isEqualTo(BulkExportJob.ExportStatus.COMPLETED);
        assertThat(completedJob.getOutputFiles()).isEmpty();
    }

    @Test
    void shouldProcessPagingAndSkipNullJson() {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.resolve("exports").toString());
        config.setChunkSize(1);
        config.setBaseUrl("http://localhost:8080/fhir");

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Patient"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientEntity emptyJsonEntity = PatientEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson(null)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        PatientEntity validEntity = PatientEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson(JSON_PARSER.encodeResourceToString(new Patient()))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        when(patientRepository.findAll(eq(PageRequest.of(0, 1))))
                .thenReturn(new PageImpl<>(List.of(emptyJsonEntity), PageRequest.of(0, 1), 2));
        when(patientRepository.findAll(eq(PageRequest.of(1, 1))))
                .thenReturn(new PageImpl<>(List.of(validEntity), PageRequest.of(1, 1), 2));

        processor.processExport(jobId).join();

        ArgumentCaptor<BulkExportJob> saveCaptor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository, org.mockito.Mockito.atLeastOnce()).save(saveCaptor.capture());
        BulkExportJob completedJob = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);

        assertThat(completedJob.getStatus()).isEqualTo(BulkExportJob.ExportStatus.COMPLETED);
        assertThat(completedJob.getOutputFiles()).hasSize(1);
        assertThat(Files.exists(Path.of(completedJob.getOutputFiles().get(0).getFilePath()))).isTrue();
    }

    @Test
    void shouldContinueWhenResourceExportFails() {
        BulkExportConfig config = new BulkExportConfig();
        config.setExportDirectory(tempDir.toString());
        config.setChunkSize(1);
        config.setBaseUrl("http://localhost:8080/fhir");

        BulkExportProcessor processor = new BulkExportProcessor(
                exportRepository,
                config,
                kafkaTemplate,
                patientRepository,
                observationRepository,
                conditionRepository,
                medicationRequestRepository,
                procedureRepository,
                encounterRepository,
                allergyIntoleranceRepository,
                immunizationRepository);

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("tenant-1")
                .status(BulkExportJob.ExportStatus.PENDING)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .resourceTypes(List.of("Patient", "Condition"))
                .requestedAt(Instant.now())
                .build();

        when(exportRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(exportRepository.save(any(BulkExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(patientRepository.findAll(any(PageRequest.class)))
                .thenThrow(new RuntimeException("patient export failed"));

        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());
        condition.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        condition.setCode(new CodeableConcept().setText("condition"));
        when(conditionRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(ConditionEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId("tenant-1")
                        .resourceType("Condition")
                        .resourceJson(JSON_PARSER.encodeResourceToString(condition))
                        .patientId(UUID.randomUUID())
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(0)
                        .build())));

        processor.processExport(jobId).join();

        ArgumentCaptor<BulkExportJob> saveCaptor = ArgumentCaptor.forClass(BulkExportJob.class);
        verify(exportRepository, org.mockito.Mockito.atLeastOnce()).save(saveCaptor.capture());
        BulkExportJob completedJob = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);

        assertThat(completedJob.getStatus()).isEqualTo(BulkExportJob.ExportStatus.COMPLETED);
        assertThat(completedJob.getOutputFiles()).hasSize(1);
        assertThat(completedJob.getOutputFiles().get(0).getType()).isEqualTo("Condition");
    }
}
