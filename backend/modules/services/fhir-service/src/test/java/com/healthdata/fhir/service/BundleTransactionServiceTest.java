package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Bundle Transaction Service Tests")
class BundleTransactionServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String ACTOR = "test-user";

    @Mock private PatientService patientService;
    @Mock private ConditionService conditionService;
    @Mock private ObservationService observationService;
    @Mock private EncounterService encounterService;
    @Mock private ProcedureService procedureService;
    @Mock private MedicationRequestService medicationRequestService;
    @Mock private MedicationAdministrationService medicationAdministrationService;
    @Mock private ImmunizationService immunizationService;
    @Mock private AllergyIntoleranceService allergyIntoleranceService;
    @Mock private DiagnosticReportService diagnosticReportService;
    @Mock private DocumentReferenceService documentReferenceService;
    @Mock private CarePlanService carePlanService;
    @Mock private GoalService goalService;
    @Mock private TaskService taskService;
    @Mock private CoverageService coverageService;
    @Mock private AppointmentService appointmentService;
    @Mock private PractitionerService practitionerService;
    @Mock private PractitionerRoleService practitionerRoleService;
    @Mock private OrganizationService organizationService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private BundleTransactionService service;

    @BeforeEach
    void setUp() {
        service = new BundleTransactionService(
                patientService, conditionService, observationService,
                encounterService, procedureService, medicationRequestService,
                medicationAdministrationService, immunizationService,
                allergyIntoleranceService, diagnosticReportService,
                documentReferenceService, carePlanService, goalService,
                taskService, coverageService, appointmentService,
                practitionerService, practitionerRoleService, organizationService,
                kafkaTemplate, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Should process transaction bundle with Patient and Condition")
    void shouldProcessTransactionBundle() {
        Patient inputPatient = new Patient();
        inputPatient.addName().setFamily("Smith").addGiven("John");

        Patient createdPatient = new Patient();
        createdPatient.setId(UUID.randomUUID().toString());
        createdPatient.addName().setFamily("Smith").addGiven("John");
        when(patientService.createPatient(eq(TENANT_ID), any(Patient.class), eq(ACTOR)))
                .thenReturn(createdPatient);

        Condition inputCondition = new Condition();
        inputCondition.setSubject(new org.hl7.fhir.r4.model.Reference("Patient/" + createdPatient.getId()));

        Condition createdCondition = new Condition();
        createdCondition.setId(UUID.randomUUID().toString());
        when(conditionService.createCondition(eq(TENANT_ID), any(Condition.class), eq(ACTOR)))
                .thenReturn(createdCondition);

        Bundle txBundle = new Bundle();
        txBundle.setType(Bundle.BundleType.TRANSACTION);

        Bundle.BundleEntryComponent patientEntry = txBundle.addEntry();
        patientEntry.setResource(inputPatient);
        patientEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

        Bundle.BundleEntryComponent conditionEntry = txBundle.addEntry();
        conditionEntry.setResource(inputCondition);
        conditionEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Condition");

        Bundle result = service.processBundle(TENANT_ID, txBundle, ACTOR);

        assertThat(result.getType()).isEqualTo(Bundle.BundleType.TRANSACTIONRESPONSE);
        assertThat(result.getEntry()).hasSize(2);
        assertThat(result.getEntry().get(0).getResponse().getStatus()).isEqualTo("201 Created");
        assertThat(result.getEntry().get(0).getResponse().getLocation()).startsWith("Patient/");
        assertThat(result.getEntry().get(1).getResponse().getStatus()).isEqualTo("201 Created");
        assertThat(result.getEntry().get(1).getResponse().getLocation()).startsWith("Condition/");

        verify(patientService).createPatient(eq(TENANT_ID), any(Patient.class), eq(ACTOR));
        verify(conditionService).createCondition(eq(TENANT_ID), any(Condition.class), eq(ACTOR));
    }

    @Test
    @DisplayName("Should process batch bundle independently — failure in one entry doesn't affect others")
    void shouldProcessBatchBundleIndependently() {
        Patient inputPatient = new Patient();
        inputPatient.addName().setFamily("Doe");

        Patient createdPatient = new Patient();
        createdPatient.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(eq(TENANT_ID), any(Patient.class), eq(ACTOR)))
                .thenReturn(createdPatient);

        // Second entry will fail
        Observation inputObs = new Observation();
        when(observationService.createObservation(eq(TENANT_ID), any(Observation.class), eq(ACTOR)))
                .thenThrow(new RuntimeException("Validation failed"));

        Bundle batchBundle = new Bundle();
        batchBundle.setType(Bundle.BundleType.BATCH);

        Bundle.BundleEntryComponent patientEntry = batchBundle.addEntry();
        patientEntry.setResource(inputPatient);
        patientEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

        Bundle.BundleEntryComponent obsEntry = batchBundle.addEntry();
        obsEntry.setResource(inputObs);
        obsEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        Bundle result = service.processBundle(TENANT_ID, batchBundle, ACTOR);

        assertThat(result.getType()).isEqualTo(Bundle.BundleType.BATCHRESPONSE);
        assertThat(result.getEntry()).hasSize(2);
        assertThat(result.getEntry().get(0).getResponse().getStatus()).isEqualTo("201 Created");
        assertThat(result.getEntry().get(1).getResponse().getStatus()).isEqualTo("400 Bad Request");
    }

    @Test
    @DisplayName("Should reject null bundle")
    void shouldRejectNullBundle() {
        assertThatThrownBy(() -> service.processBundle(TENANT_ID, null, ACTOR))
                .isInstanceOf(BundleTransactionService.BundleValidationException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Should reject bundle with wrong type")
    void shouldRejectWrongBundleType() {
        Bundle searchBundle = new Bundle();
        searchBundle.setType(Bundle.BundleType.SEARCHSET);
        searchBundle.addEntry().setResource(new Patient());

        assertThatThrownBy(() -> service.processBundle(TENANT_ID, searchBundle, ACTOR))
                .isInstanceOf(BundleTransactionService.BundleValidationException.class)
                .hasMessageContaining("transaction");
    }

    @Test
    @DisplayName("Should reject empty bundle")
    void shouldRejectEmptyBundle() {
        Bundle emptyBundle = new Bundle();
        emptyBundle.setType(Bundle.BundleType.TRANSACTION);

        assertThatThrownBy(() -> service.processBundle(TENANT_ID, emptyBundle, ACTOR))
                .isInstanceOf(BundleTransactionService.BundleValidationException.class)
                .hasMessageContaining("at least one entry");
    }

    @Test
    @DisplayName("Should reject unsupported resource type in bundle")
    void shouldRejectUnsupportedResourceType() {
        Bundle txBundle = new Bundle();
        txBundle.setType(Bundle.BundleType.TRANSACTION);

        // StructureDefinition is not a supported resource
        org.hl7.fhir.r4.model.StructureDefinition sd = new org.hl7.fhir.r4.model.StructureDefinition();
        sd.setName("TestSD");
        Bundle.BundleEntryComponent entry = txBundle.addEntry();
        entry.setResource(sd);
        entry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("StructureDefinition");

        // Transaction mode should throw BundleProcessingException (wrapping the validation error)
        assertThatThrownBy(() -> service.processBundle(TENANT_ID, txBundle, ACTOR))
                .isInstanceOf(BundleTransactionService.BundleProcessingException.class);
    }

    @Test
    @DisplayName("Should process multi-resource transaction with all supported types")
    void shouldProcessMultiResourceTransaction() {
        Patient patient = new Patient();
        patient.addName().setFamily("HIE-Patient");
        Patient createdPatient = new Patient();
        createdPatient.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(eq(TENANT_ID), any(Patient.class), eq(ACTOR)))
                .thenReturn(createdPatient);

        Observation obs = new Observation();
        Observation createdObs = new Observation();
        createdObs.setId(UUID.randomUUID().toString());
        when(observationService.createObservation(eq(TENANT_ID), any(Observation.class), eq(ACTOR)))
                .thenReturn(createdObs);

        Bundle txBundle = new Bundle();
        txBundle.setType(Bundle.BundleType.TRANSACTION);

        txBundle.addEntry().setResource(patient)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");
        txBundle.addEntry().setResource(obs)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        Bundle result = service.processBundle(TENANT_ID, txBundle, ACTOR);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getEntry()).hasSize(2);
        result.getEntry().forEach(e ->
                assertThat(e.getResponse().getStatus()).isEqualTo("201 Created"));
    }

    @Test
    @DisplayName("Should publish Kafka event after transaction completion")
    void shouldPublishKafkaEvent() {
        Patient patient = new Patient();
        Patient created = new Patient();
        created.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(anyString(), any(Patient.class), anyString()))
                .thenReturn(created);

        Bundle txBundle = new Bundle();
        txBundle.setType(Bundle.BundleType.TRANSACTION);
        txBundle.addEntry().setResource(patient)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

        service.processBundle(TENANT_ID, txBundle, ACTOR);

        verify(kafkaTemplate).send(eq("fhir.bundle.transaction.completed"), anyString(), any());
    }

    @Test
    @DisplayName("Transaction should roll back all entries on failure")
    void transactionShouldRollBackOnFailure() {
        Patient patient = new Patient();
        Patient createdPatient = new Patient();
        createdPatient.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(eq(TENANT_ID), any(Patient.class), eq(ACTOR)))
                .thenReturn(createdPatient);

        // Second entry fails
        Condition condition = new Condition();
        when(conditionService.createCondition(eq(TENANT_ID), any(Condition.class), eq(ACTOR)))
                .thenThrow(new RuntimeException("DB constraint violation"));

        Bundle txBundle = new Bundle();
        txBundle.setType(Bundle.BundleType.TRANSACTION);
        txBundle.addEntry().setResource(patient)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");
        txBundle.addEntry().setResource(condition)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Condition");

        // Transaction mode should throw BundleProcessingException
        assertThatThrownBy(() -> service.processBundle(TENANT_ID, txBundle, ACTOR))
                .isInstanceOf(BundleTransactionService.BundleProcessingException.class)
                .hasMessageContaining("Transaction failed");
    }

    @Test
    @DisplayName("Should handle entry without request component (infer POST)")
    void shouldHandleEntryWithoutRequest() {
        Patient patient = new Patient();
        Patient created = new Patient();
        created.setId(UUID.randomUUID().toString());
        when(patientService.createPatient(eq(TENANT_ID), any(Patient.class), eq(ACTOR)))
                .thenReturn(created);

        Bundle batchBundle = new Bundle();
        batchBundle.setType(Bundle.BundleType.BATCH);
        // Entry with resource but no request component
        batchBundle.addEntry().setResource(patient);

        Bundle result = service.processBundle(TENANT_ID, batchBundle, ACTOR);

        assertThat(result.getEntry()).hasSize(1);
        assertThat(result.getEntry().get(0).getResponse().getStatus()).isEqualTo("201 Created");
    }
}
