package com.healthdata.fhir.integration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Functional Tests for FHIR R4 Resource Validation.
 *
 * Tests the complete FHIR resource handling workflow including:
 * - Resource creation and validation
 * - FHIR R4 compliance checking
 * - Resource retrieval and search
 * - Multi-tenant resource isolation
 * - Clinical terminologies (SNOMED CT, LOINC, RxNorm)
 * - Bundle processing
 * - Observation categorization for SDOH
 *
 * FHIR RESOURCE COVERAGE:
 * - Patient (demographics)
 * - Condition (diagnoses)
 * - Observation (lab results, vitals, SDOH)
 * - Procedure (procedures performed)
 * - MedicationStatement (active medications)
 * - Immunization (vaccination history)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("FHIR R4 Resource Validation E2E Tests")
class FhirResourceValidationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private IGenericClient fhirClient;

    private static final String TENANT_ID = "test-tenant-fhir";
    private static final String PATIENT_ID = "patient-fhir-001";

    @BeforeEach
    void setUp() {
        // Clean up test data
        // Note: In real tests, you'd use Testcontainers for FHIR server or mock the client
    }

    @Nested
    @DisplayName("Patient Resource Validation")
    class PatientResourceValidation {

        @Test
        @DisplayName("should create valid Patient resource")
        void shouldCreateValidPatient() throws Exception {
            Patient patient = new Patient();
            patient.setId(PATIENT_ID);
            patient.addIdentifier()
                .setSystem("http://hospital.example.org/patients")
                .setValue("MRN-12345");

            HumanName name = patient.addName();
            name.setFamily("Doe");
            name.addGiven("John");

            patient.setGender(Enumerations.AdministrativeGender.MALE);
            patient.setBirthDate(Date.from(
                LocalDate.of(1980, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            ));

            String patientJson = fhirContext.newJsonParser()
                .setPrettyPrint(true)
                .encodeResourceToString(patient);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Patient")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(patientJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.name[0].family").value("Doe"))
                .andExpect(jsonPath("$.gender").value("male"));
        }

        @Test
        @DisplayName("should reject invalid Patient resource")
        void shouldRejectInvalidPatient() throws Exception {
            // Missing required fields (no identifier, no name)
            Patient patient = new Patient();
            patient.setGender(Enumerations.AdministrativeGender.MALE);

            String patientJson = fhirContext.newJsonParser()
                .encodeResourceToString(patient);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Patient")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(patientJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issue[0].severity").value("error"))
                .andExpect(jsonPath("$.issue[0].diagnostics").value(containsString("required")));
        }

        @Test
        @DisplayName("should retrieve Patient by ID")
        void shouldRetrievePatientById() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Assume patient already exists
            mockMvc.perform(get("/fhir/Patient/" + PATIENT_ID)
                    .headers(headers)
                    .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value(PATIENT_ID));
        }
    }

    @Nested
    @DisplayName("Condition Resource Validation")
    class ConditionResourceValidation {

        @Test
        @DisplayName("should create Condition with SNOMED CT code")
        void shouldCreateConditionWithSnomedCode() throws Exception {
            Condition condition = new Condition();
            condition.setSubject(new Reference("Patient/" + PATIENT_ID));

            // Diabetes Mellitus Type 2 (SNOMED CT: 44054006)
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("44054006")
                .setDisplay("Diabetes mellitus type 2");
            condition.setCode(code);

            condition.setClinicalStatus(new CodeableConcept()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active"));

            String conditionJson = fhirContext.newJsonParser()
                .encodeResourceToString(condition);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Condition")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(conditionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("Condition"))
                .andExpect(jsonPath("$.code.coding[0].system").value("http://snomed.info/sct"))
                .andExpect(jsonPath("$.code.coding[0].code").value("44054006"));
        }

        @Test
        @DisplayName("should search Conditions by patient")
        void shouldSearchConditionsByPatient() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(get("/fhir/Condition")
                    .param("patient", PATIENT_ID)
                    .headers(headers)
                    .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("searchset"))
                .andExpect(jsonPath("$.entry").isArray());
        }
    }

    @Nested
    @DisplayName("Observation Resource Validation")
    class ObservationResourceValidation {

        @Test
        @DisplayName("should create Observation with LOINC code")
        void shouldCreateObservationWithLoincCode() throws Exception {
            Observation observation = new Observation();
            observation.setSubject(new Reference("Patient/" + PATIENT_ID));
            observation.setStatus(Observation.ObservationStatus.FINAL);

            // HbA1c (LOINC: 4548-4)
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("4548-4")
                .setDisplay("Hemoglobin A1c/Hemoglobin.total in Blood");
            observation.setCode(code);

            // Value: 7.5%
            Quantity value = new Quantity();
            value.setValue(7.5);
            value.setUnit("%");
            value.setSystem("http://unitsofmeasure.org");
            value.setCode("%");
            observation.setValue(value);

            String observationJson = fhirContext.newJsonParser()
                .encodeResourceToString(observation);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Observation")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(observationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("Observation"))
                .andExpect(jsonPath("$.code.coding[0].system").value("http://loinc.org"))
                .andExpect(jsonPath("$.code.coding[0].code").value("4548-4"))
                .andExpect(jsonPath("$.valueQuantity.value").value(7.5));
        }

        @Test
        @DisplayName("should create SDOH Observation with social-history category")
        void shouldCreateSdohObservation() throws Exception {
            Observation observation = new Observation();
            observation.setSubject(new Reference("Patient/" + PATIENT_ID));
            observation.setStatus(Observation.ObservationStatus.FINAL);

            // Add social-history category for SDOH
            CodeableConcept category = new CodeableConcept();
            category.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("social-history")
                .setDisplay("Social History");
            observation.addCategory(category);

            // Housing stability (LOINC: 71802-3)
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("71802-3")
                .setDisplay("Housing status");
            observation.setCode(code);

            CodeableConcept value = new CodeableConcept();
            value.addCoding()
                .setCode("LA31993-1")
                .setDisplay("I have a steady place to live");
            observation.setValue(value);

            String observationJson = fhirContext.newJsonParser()
                .encodeResourceToString(observation);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Observation")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(observationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category[0].coding[0].code").value("social-history"));
        }

        @Test
        @DisplayName("should search Observations by category")
        void shouldSearchObservationsByCategory() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(get("/fhir/Observation")
                    .param("patient", PATIENT_ID)
                    .param("category", "social-history")
                    .headers(headers)
                    .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("searchset"));
        }
    }

    @Nested
    @DisplayName("Procedure Resource Validation")
    class ProcedureResourceValidation {

        @Test
        @DisplayName("should create Procedure with SNOMED CT code")
        void shouldCreateProcedureWithSnomedCode() throws Exception {
            Procedure procedure = new Procedure();
            procedure.setSubject(new Reference("Patient/" + PATIENT_ID));
            procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);

            // Influenza vaccination (SNOMED CT: 86198006)
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("86198006")
                .setDisplay("Influenza vaccination");
            procedure.setCode(code);

            procedure.setPerformed(new DateTimeType(new Date()));

            String procedureJson = fhirContext.newJsonParser()
                .encodeResourceToString(procedure);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Procedure")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(procedureJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("Procedure"))
                .andExpect(jsonPath("$.code.coding[0].system").value("http://snomed.info/sct"))
                .andExpect(jsonPath("$.code.coding[0].code").value("86198006"));
        }
    }

    @Nested
    @DisplayName("MedicationStatement Resource Validation")
    class MedicationStatementResourceValidation {

        @Test
        @DisplayName("should create MedicationStatement with RxNorm code")
        void shouldCreateMedicationStatementWithRxNorm() throws Exception {
            MedicationStatement medStatement = new MedicationStatement();
            medStatement.setSubject(new Reference("Patient/" + PATIENT_ID));
            medStatement.setStatus(MedicationStatement.MedicationStatementStatus.ACTIVE);

            // Metformin 500mg (RxNorm: 860975)
            CodeableConcept medication = new CodeableConcept();
            medication.addCoding()
                .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .setCode("860975")
                .setDisplay("Metformin 500 MG Oral Tablet");
            medStatement.setMedication(medication);

            String medStatementJson = fhirContext.newJsonParser()
                .encodeResourceToString(medStatement);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/MedicationStatement")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(medStatementJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("MedicationStatement"))
                .andExpect(jsonPath("$.medicationCodeableConcept.coding[0].system")
                    .value("http://www.nlm.nih.gov/research/umls/rxnorm"));
        }
    }

    @Nested
    @DisplayName("Bundle Processing")
    class BundleProcessing {

        @Test
        @DisplayName("should create Bundle with multiple resources")
        void shouldCreateBundleWithMultipleResources() throws Exception {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.TRANSACTION);

            // Add Patient
            Patient patient = new Patient();
            patient.setId(PATIENT_ID);
            patient.addName().setFamily("Smith").addGiven("Jane");

            Bundle.BundleEntryComponent patientEntry = bundle.addEntry();
            patientEntry.setResource(patient);
            patientEntry.getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl("Patient");

            // Add Condition
            Condition condition = new Condition();
            condition.setSubject(new Reference("Patient/" + PATIENT_ID));
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("38341003")
                .setDisplay("Hypertension");
            condition.setCode(code);

            Bundle.BundleEntryComponent conditionEntry = bundle.addEntry();
            conditionEntry.setResource(condition);
            conditionEntry.getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl("Condition");

            String bundleJson = fhirContext.newJsonParser()
                .encodeResourceToString(bundle);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(bundleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("transaction-response"))
                .andExpect(jsonPath("$.entry", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Resource Isolation")
    class MultiTenantResourceIsolation {

        @Test
        @DisplayName("should isolate FHIR resources by tenant")
        void shouldIsolateFhirResourcesByTenant() throws Exception {
            String tenant1 = "tenant-fhir-001";
            String tenant2 = "tenant-fhir-002";

            Patient patient = new Patient();
            patient.setId(PATIENT_ID);
            patient.addName().setFamily("Tenant1Patient");

            String patientJson = fhirContext.newJsonParser()
                .encodeResourceToString(patient);

            // Create patient in tenant 1
            var headers1 = GatewayTrustTestHeaders.adminHeaders(tenant1);
            mockMvc.perform(post("/fhir/Patient")
                    .headers(headers1)
                    .contentType("application/fhir+json")
                    .content(patientJson))
                .andExpect(status().isCreated());

            // Tenant 2 should not see tenant 1's patient
            var headers2 = GatewayTrustTestHeaders.adminHeaders(tenant2);
            mockMvc.perform(get("/fhir/Patient/" + PATIENT_ID)
                    .headers(headers2)
                    .accept("application/fhir+json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.issue[0].severity").value("error"));
        }
    }

    @Nested
    @DisplayName("Error Handling and Validation")
    class ErrorHandlingAndValidation {

        @Test
        @DisplayName("should return OperationOutcome for invalid resource")
        void shouldReturnOperationOutcomeForInvalidResource() throws Exception {
            // Observation without required status
            Observation observation = new Observation();
            observation.setSubject(new Reference("Patient/" + PATIENT_ID));
            // Missing status (required field)

            String observationJson = fhirContext.newJsonParser()
                .encodeResourceToString(observation);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Observation")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(observationJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("error"))
                .andExpect(jsonPath("$.issue[0].diagnostics").exists());
        }

        @Test
        @DisplayName("should validate reference integrity")
        void shouldValidateReferenceIntegrity() throws Exception {
            // Condition referencing non-existent patient
            Condition condition = new Condition();
            condition.setSubject(new Reference("Patient/non-existent-patient-999"));

            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("44054006")
                .setDisplay("Diabetes mellitus type 2");
            condition.setCode(code);

            condition.setClinicalStatus(new CodeableConcept()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active"));

            String conditionJson = fhirContext.newJsonParser()
                .encodeResourceToString(condition);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Condition")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content(conditionJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issue[0].diagnostics")
                    .value(containsString("reference")));
        }

        @Test
        @DisplayName("should reject malformed FHIR JSON")
        void shouldRejectMalformedFhirJson() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/fhir/Patient")
                    .headers(headers)
                    .contentType("application/fhir+json")
                    .content("{INVALID JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
        }
    }

    @Nested
    @DisplayName("HIPAA Compliance")
    class HipaaCompliance {

        @Test
        @DisplayName("should include no-cache headers for PHI resources")
        void shouldIncludeNoCacheHeadersForPhi() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(get("/fhir/Patient/" + PATIENT_ID)
                    .headers(headers)
                    .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }
    }
}
