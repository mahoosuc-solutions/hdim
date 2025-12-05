package com.healthdata.test;

import com.healthdata.fhir.client.FhirServerClient;
import com.healthdata.fhir.entity.Condition;
import com.healthdata.fhir.entity.Observation;
import com.healthdata.notification.service.NotificationService;
import com.healthdata.notification.dto.NotificationRequest;
import org.hl7.fhir.r4.model.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * External Service Mocks Configuration
 *
 * Provides mock implementations of external services for integration testing.
 * Services mocked:
 * - FHIR Server Client
 * - Notification Service
 * - Email Service
 * - SMS Service
 *
 * @author TDD Swarm Agent 5B
 */
@TestConfiguration
@Profile("test")
public class ExternalServiceMocks {

    // ==================== FHIR SERVER CLIENT MOCK ====================

    @Bean
    @Primary
    public FhirServerClient fhirServerClientMock() {
        FhirServerClient mock = Mockito.mock(FhirServerClient.class);

        // Mock getObservations
        when(mock.getObservations(anyString())).thenAnswer(invocation -> {
            String patientId = invocation.getArgument(0);
            return createMockObservations(patientId);
        });

        // Mock getConditions
        when(mock.getConditions(anyString())).thenAnswer(invocation -> {
            String patientId = invocation.getArgument(0);
            return createMockConditions(patientId);
        });

        // Mock getMedicationRequests
        when(mock.getMedicationRequests(anyString())).thenAnswer(invocation -> {
            String patientId = invocation.getArgument(0);
            return createMockMedicationRequests(patientId);
        });

        // Mock createObservation
        when(mock.createObservation(any(org.hl7.fhir.r4.model.Observation.class)))
                .thenAnswer(invocation -> {
                    org.hl7.fhir.r4.model.Observation obs = invocation.getArgument(0);
                    obs.setId("mock-obs-" + UUID.randomUUID());
                    return obs;
                });

        // Mock createCondition
        when(mock.createCondition(any(org.hl7.fhir.r4.model.Condition.class)))
                .thenAnswer(invocation -> {
                    org.hl7.fhir.r4.model.Condition condition = invocation.getArgument(0);
                    condition.setId("mock-condition-" + UUID.randomUUID());
                    return condition;
                });

        // Mock searchPatients
        when(mock.searchPatients(anyMap())).thenReturn(createMockPatientBundle());

        return mock;
    }

    // ==================== NOTIFICATION SERVICE MOCK ====================

    @Bean
    @Primary
    public NotificationService notificationServiceMock() {
        NotificationService mock = Mockito.mock(NotificationService.class);

        // Mock sendNotification - returns success
        when(mock.sendNotification(any(NotificationRequest.class))).thenAnswer(invocation -> {
            NotificationRequest request = invocation.getArgument(0);
            return createMockNotificationResponse(request, true);
        });

        // Mock sendBatch
        when(mock.sendBatch(anyList())).thenAnswer(invocation -> {
            List<NotificationRequest> requests = invocation.getArgument(0);
            return createMockBatchNotificationResponse(requests);
        });

        // Mock sendEmail
        doNothing().when(mock).sendEmail(anyString(), anyString(), anyString());

        // Mock sendSMS
        doNothing().when(mock).sendSMS(anyString(), anyString());

        return mock;
    }

    // ==================== MOCK DATA FACTORIES ====================

    /**
     * Creates mock FHIR observations for a patient
     */
    private List<org.hl7.fhir.r4.model.Observation> createMockObservations(String patientId) {
        List<org.hl7.fhir.r4.model.Observation> observations = new ArrayList<>();

        // HbA1c observation
        org.hl7.fhir.r4.model.Observation hba1c = new org.hl7.fhir.r4.model.Observation();
        hba1c.setId("mock-obs-hba1c-" + patientId);
        hba1c.setStatus(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL);
        hba1c.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://loinc.org")
                        .setCode("4548-4")
                        .setDisplay("Hemoglobin A1c")));
        hba1c.setValue(new Quantity().setValue(6.8).setUnit("%"));
        hba1c.setSubject(new Reference("Patient/" + patientId));
        hba1c.setEffective(new DateTimeType(new Date()));
        observations.add(hba1c);

        // Blood pressure observation
        org.hl7.fhir.r4.model.Observation bp = new org.hl7.fhir.r4.model.Observation();
        bp.setId("mock-obs-bp-" + patientId);
        bp.setStatus(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL);
        bp.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://loinc.org")
                        .setCode("85354-9")
                        .setDisplay("Blood Pressure")));

        // Add systolic component
        org.hl7.fhir.r4.model.Observation.ObservationComponentComponent systolic =
                new org.hl7.fhir.r4.model.Observation.ObservationComponentComponent();
        systolic.setCode(new CodeableConcept().addCoding(
                new Coding().setSystem("http://loinc.org").setCode("8480-6").setDisplay("Systolic BP")));
        systolic.setValue(new Quantity().setValue(130).setUnit("mmHg"));
        bp.addComponent(systolic);

        // Add diastolic component
        org.hl7.fhir.r4.model.Observation.ObservationComponentComponent diastolic =
                new org.hl7.fhir.r4.model.Observation.ObservationComponentComponent();
        diastolic.setCode(new CodeableConcept().addCoding(
                new Coding().setSystem("http://loinc.org").setCode("8462-4").setDisplay("Diastolic BP")));
        diastolic.setValue(new Quantity().setValue(80).setUnit("mmHg"));
        bp.addComponent(diastolic);

        bp.setSubject(new Reference("Patient/" + patientId));
        bp.setEffective(new DateTimeType(new Date()));
        observations.add(bp);

        return observations;
    }

    /**
     * Creates mock FHIR conditions for a patient
     */
    private List<org.hl7.fhir.r4.model.Condition> createMockConditions(String patientId) {
        List<org.hl7.fhir.r4.model.Condition> conditions = new ArrayList<>();

        // Diabetes condition
        org.hl7.fhir.r4.model.Condition diabetes = new org.hl7.fhir.r4.model.Condition();
        diabetes.setId("mock-condition-diabetes-" + patientId);
        diabetes.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://hl7.org/fhir/sid/icd-10")
                        .setCode("E11")
                        .setDisplay("Type 2 Diabetes Mellitus")));
        diabetes.setSubject(new Reference("Patient/" + patientId));
        diabetes.setClinicalStatus(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")));
        conditions.add(diabetes);

        // Hypertension condition
        org.hl7.fhir.r4.model.Condition hypertension = new org.hl7.fhir.r4.model.Condition();
        hypertension.setId("mock-condition-htn-" + patientId);
        hypertension.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://hl7.org/fhir/sid/icd-10")
                        .setCode("I10")
                        .setDisplay("Essential Hypertension")));
        hypertension.setSubject(new Reference("Patient/" + patientId));
        hypertension.setClinicalStatus(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")));
        conditions.add(hypertension);

        return conditions;
    }

    /**
     * Creates mock FHIR medication requests
     */
    private List<org.hl7.fhir.r4.model.MedicationRequest> createMockMedicationRequests(String patientId) {
        List<org.hl7.fhir.r4.model.MedicationRequest> medications = new ArrayList<>();

        // Metformin medication
        org.hl7.fhir.r4.model.MedicationRequest metformin = new org.hl7.fhir.r4.model.MedicationRequest();
        metformin.setId("mock-med-metformin-" + patientId);
        metformin.setStatus(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ACTIVE);
        metformin.setIntent(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent.ORDER);
        metformin.setMedication(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                        .setCode("6809")
                        .setDisplay("Metformin")));
        metformin.setSubject(new Reference("Patient/" + patientId));
        medications.add(metformin);

        // Lisinopril medication
        org.hl7.fhir.r4.model.MedicationRequest lisinopril = new org.hl7.fhir.r4.model.MedicationRequest();
        lisinopril.setId("mock-med-lisinopril-" + patientId);
        lisinopril.setStatus(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ACTIVE);
        lisinopril.setIntent(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent.ORDER);
        lisinopril.setMedication(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                        .setCode("29046")
                        .setDisplay("Lisinopril")));
        lisinopril.setSubject(new Reference("Patient/" + patientId));
        medications.add(lisinopril);

        return medications;
    }

    /**
     * Creates mock patient bundle
     */
    private Bundle createMockPatientBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(2);

        // Patient 1
        org.hl7.fhir.r4.model.Patient patient1 = new org.hl7.fhir.r4.model.Patient();
        patient1.setId("mock-patient-1");
        patient1.addName().setFamily("Doe").addGiven("John");
        patient1.setGender(Enumerations.AdministrativeGender.MALE);
        bundle.addEntry().setResource(patient1);

        // Patient 2
        org.hl7.fhir.r4.model.Patient patient2 = new org.hl7.fhir.r4.model.Patient();
        patient2.setId("mock-patient-2");
        patient2.addName().setFamily("Smith").addGiven("Jane");
        patient2.setGender(Enumerations.AdministrativeGender.FEMALE);
        bundle.addEntry().setResource(patient2);

        return bundle;
    }

    /**
     * Creates mock notification response
     */
    private Map<String, Object> createMockNotificationResponse(NotificationRequest request, boolean success) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("notificationId", "mock-notif-" + UUID.randomUUID());
        response.put("sentAt", LocalDateTime.now());
        response.put("recipientId", request.getRecipientId());
        response.put("channel", request.getChannel());
        return response;
    }

    /**
     * Creates mock batch notification response
     */
    private List<Map<String, Object>> createMockBatchNotificationResponse(List<NotificationRequest> requests) {
        List<Map<String, Object>> responses = new ArrayList<>();
        for (NotificationRequest request : requests) {
            responses.add(createMockNotificationResponse(request, true));
        }
        return responses;
    }

    // ==================== ENTITY MOCK FACTORIES ====================

    /**
     * Creates mock observations entities for testing
     */
    public static List<Observation> createMockObservationEntities(String patientId) {
        List<Observation> observations = new ArrayList<>();

        observations.add(Observation.builder()
                .id("obs-1")
                .patientId(patientId)
                .code("4548-4")
                .system("http://loinc.org")
                .display("Hemoglobin A1c")
                .valueQuantity(6.8)
                .unit("%")
                .status("final")
                .effectiveDateTime(LocalDateTime.now().minusDays(30))
                .tenantId("test-tenant-1")
                .build());

        observations.add(Observation.builder()
                .id("obs-2")
                .patientId(patientId)
                .code("8480-6")
                .system("http://loinc.org")
                .display("Systolic Blood Pressure")
                .valueQuantity(130.0)
                .unit("mmHg")
                .status("final")
                .effectiveDateTime(LocalDateTime.now().minusDays(30))
                .tenantId("test-tenant-1")
                .build());

        return observations;
    }

    /**
     * Creates mock condition entities for testing
     */
    public static List<Condition> createMockConditionEntities(String patientId) {
        List<Condition> conditions = new ArrayList<>();

        conditions.add(Condition.builder()
                .id("cond-1")
                .patientId(patientId)
                .code("E11")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Type 2 Diabetes Mellitus")
                .clinicalStatus("active")
                .tenantId("test-tenant-1")
                .build());

        conditions.add(Condition.builder()
                .id("cond-2")
                .patientId(patientId)
                .code("I10")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Essential Hypertension")
                .clinicalStatus("active")
                .tenantId("test-tenant-1")
                .build());

        return conditions;
    }
}
