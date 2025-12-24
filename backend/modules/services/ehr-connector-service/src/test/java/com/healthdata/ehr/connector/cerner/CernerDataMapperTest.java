package com.healthdata.ehr.connector.cerner;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for CernerDataMapper.
 *
 * Tests FHIR R4 resource mapping and Cerner-specific extension handling.
 */
@DisplayName("Cerner Data Mapper Tests")
class CernerDataMapperTest {

    private CernerDataMapper mapper;
    private FhirContext fhirContext;

    private static final String CERNER_EXTENSION_BASE = "http://fhir.cerner.com/extension/";
    private static final String CERNER_CODE_CONSOLE = "http://fhir.cerner.com/code-console";

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        mapper = new CernerDataMapper(fhirContext);
    }

    @Nested
    @DisplayName("Patient Mapping Tests")
    class PatientMappingTests {

        @Test
        @DisplayName("Should return null when patient is null")
        void shouldReturnNullForNullPatient() {
            Patient result = mapper.mapPatient(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map patient with basic demographics")
        void shouldMapPatientWithBasicDemographics() {
            // Given
            Patient patient = new Patient();
            patient.setId("12345");
            patient.addName()
                .setFamily("Smith")
                .addGiven("John");
            patient.setGender(Enumerations.AdministrativeGender.MALE);
            patient.setBirthDateElement(new DateType("1980-05-15"));

            // When
            Patient result = mapper.mapPatient(patient);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("12345");
            assertThat(result.getNameFirstRep().getFamily()).isEqualTo("Smith");
            assertThat(result.getNameFirstRep().getGivenAsSingleString()).isEqualTo("John");
            assertThat(result.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
        }

        @Test
        @DisplayName("Should preserve patient identifiers")
        void shouldPreservePatientIdentifiers() {
            // Given
            Patient patient = new Patient();
            patient.setId("patient-123");
            patient.addIdentifier()
                .setSystem("http://hospital.example/mrn")
                .setValue("MRN-12345");
            patient.addIdentifier()
                .setSystem("http://hl7.org/fhir/sid/us-ssn")
                .setValue("123-45-6789");

            // When
            Patient result = mapper.mapPatient(patient);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdentifier()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Encounter Mapping Tests")
    class EncounterMappingTests {

        @Test
        @DisplayName("Should return null when encounter is null")
        void shouldReturnNullForNullEncounter() {
            Encounter result = mapper.mapEncounter(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map encounter with status and class")
        void shouldMapEncounterWithStatusAndClass() {
            // Given
            Encounter encounter = new Encounter();
            encounter.setId("enc-001");
            encounter.setStatus(Encounter.EncounterStatus.FINISHED);
            encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

            // When
            Encounter result = mapper.mapEncounter(encounter);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("enc-001");
            assertThat(result.getStatus()).isEqualTo(Encounter.EncounterStatus.FINISHED);
            assertThat(result.getClass_().getCode()).isEqualTo("AMB");
        }
    }

    @Nested
    @DisplayName("Observation Mapping Tests")
    class ObservationMappingTests {

        @Test
        @DisplayName("Should return null when observation is null")
        void shouldReturnNullForNullObservation() {
            Observation result = mapper.mapObservation(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map observation with code and value")
        void shouldMapObservationWithCodeAndValue() {
            // Given
            Observation observation = new Observation();
            observation.setId("obs-001");
            observation.setStatus(Observation.ObservationStatus.FINAL);
            observation.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("8480-6")
                .setDisplay("Systolic blood pressure");
            observation.setValue(new Quantity()
                .setValue(120)
                .setUnit("mmHg")
                .setSystem("http://unitsofmeasure.org")
                .setCode("mm[Hg]"));

            // When
            Observation result = mapper.mapObservation(observation);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("obs-001");
            assertThat(result.getStatus()).isEqualTo(Observation.ObservationStatus.FINAL);
            assertThat(result.getCode().getCodingFirstRep().getCode()).isEqualTo("8480-6");
        }
    }

    @Nested
    @DisplayName("Condition Mapping Tests")
    class ConditionMappingTests {

        @Test
        @DisplayName("Should return null when condition is null")
        void shouldReturnNullForNullCondition() {
            Condition result = mapper.mapCondition(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map condition with code and clinical status")
        void shouldMapConditionWithCodeAndStatus() {
            // Given
            Condition condition = new Condition();
            condition.setId("cond-001");
            condition.getClinicalStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");
            condition.getCode()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("38341003")
                .setDisplay("Hypertensive disorder");

            // When
            Condition result = mapper.mapCondition(condition);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("cond-001");
        }
    }

    @Nested
    @DisplayName("MedicationRequest Mapping Tests")
    class MedicationRequestMappingTests {

        @Test
        @DisplayName("Should return null when medication request is null")
        void shouldReturnNullForNullMedicationRequest() {
            MedicationRequest result = mapper.mapMedicationRequest(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map medication request with medication and status")
        void shouldMapMedicationRequestWithMedicationAndStatus() {
            // Given
            MedicationRequest medicationRequest = new MedicationRequest();
            medicationRequest.setId("med-001");
            medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
            medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
            CodeableConcept medication = new CodeableConcept();
            medication.addCoding()
                .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .setCode("197361")
                .setDisplay("Lisinopril 10 MG Oral Tablet");
            medicationRequest.setMedication(medication);

            // When
            MedicationRequest result = mapper.mapMedicationRequest(medicationRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("med-001");
            assertThat(result.getStatus()).isEqualTo(MedicationRequest.MedicationRequestStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Immunization Mapping Tests")
    class ImmunizationMappingTests {

        @Test
        @DisplayName("Should return null when immunization is null")
        void shouldReturnNullForNullImmunization() {
            Immunization result = mapper.mapImmunization(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map immunization with vaccine code")
        void shouldMapImmunizationWithVaccineCode() {
            // Given
            Immunization immunization = new Immunization();
            immunization.setId("imm-001");
            immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
            immunization.getVaccineCode()
                .addCoding()
                .setSystem("http://hl7.org/fhir/sid/cvx")
                .setCode("208")
                .setDisplay("COVID-19, mRNA, LNP-S");

            // When
            Immunization result = mapper.mapImmunization(immunization);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("imm-001");
            assertThat(result.getStatus()).isEqualTo(Immunization.ImmunizationStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("DiagnosticReport Mapping Tests")
    class DiagnosticReportMappingTests {

        @Test
        @DisplayName("Should return null when diagnostic report is null")
        void shouldReturnNullForNullDiagnosticReport() {
            DiagnosticReport result = mapper.mapDiagnosticReport(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map diagnostic report with code and status")
        void shouldMapDiagnosticReportWithCodeAndStatus() {
            // Given
            DiagnosticReport report = new DiagnosticReport();
            report.setId("dr-001");
            report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
            report.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("58410-2")
                .setDisplay("Complete blood count");

            // When
            DiagnosticReport result = mapper.mapDiagnosticReport(report);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdElement().getIdPart()).isEqualTo("dr-001");
            assertThat(result.getStatus()).isEqualTo(DiagnosticReport.DiagnosticReportStatus.FINAL);
        }
    }

    @Nested
    @DisplayName("Cerner Extension Tests")
    class CernerExtensionTests {

        @Test
        @DisplayName("Should return null when resource is null")
        void shouldReturnNullForNullResource() {
            String result = mapper.extractCernerExtension(null, CERNER_EXTENSION_BASE + "test");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when extension URL is null")
        void shouldReturnNullForNullExtensionUrl() {
            Patient patient = new Patient();
            String result = mapper.extractCernerExtension(patient, null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when extension is not present")
        void shouldReturnNullWhenExtensionNotPresent() {
            Patient patient = new Patient();
            patient.setId("test-patient");

            String result = mapper.extractCernerExtension(patient, CERNER_EXTENSION_BASE + "nonexistent");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should extract string value from Cerner extension")
        void shouldExtractStringValueFromCernerExtension() {
            // Given
            Patient patient = new Patient();
            patient.setId("test-patient");
            String extensionUrl = CERNER_EXTENSION_BASE + "custom-field";
            patient.addExtension()
                .setUrl(extensionUrl)
                .setValue(new StringType("custom-value"));

            // When
            String result = mapper.extractCernerExtension(patient, extensionUrl);

            // Then
            assertThat(result).isEqualTo("custom-value");
        }

        @Test
        @DisplayName("Should return null for non-string extension value")
        void shouldReturnNullForNonStringExtensionValue() {
            // Given
            Patient patient = new Patient();
            patient.setId("test-patient");
            String extensionUrl = CERNER_EXTENSION_BASE + "boolean-field";
            patient.addExtension()
                .setUrl(extensionUrl)
                .setValue(new BooleanType(true));

            // When
            String result = mapper.extractCernerExtension(patient, extensionUrl);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should identify Cerner extension URLs")
        void shouldIdentifyCernerExtensionUrls() {
            assertThat(mapper.isCernerExtension(CERNER_EXTENSION_BASE + "custom")).isTrue();
            assertThat(mapper.isCernerExtension("http://hl7.org/fhir/StructureDefinition/patient-birthPlace")).isFalse();
            assertThat(mapper.isCernerExtension(null)).isFalse();
        }

        @Test
        @DisplayName("Should identify Cerner Code Console codings")
        void shouldIdentifyCernerCodeConsoleCodings() {
            // Given
            Coding cernerCoding = new Coding()
                .setSystem(CERNER_CODE_CONSOLE)
                .setCode("12345");

            Coding standardCoding = new Coding()
                .setSystem("http://loinc.org")
                .setCode("8480-6");

            // Then
            assertThat(mapper.isCernerCodeConsole(cernerCoding)).isTrue();
            assertThat(mapper.isCernerCodeConsole(standardCoding)).isFalse();
            assertThat(mapper.isCernerCodeConsole(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("CodeableConcept Normalization Tests")
    class CodeableConceptNormalizationTests {

        @Test
        @DisplayName("Should return null when concept is null")
        void shouldReturnNullForNullConcept() {
            CodeableConcept result = mapper.normalizeCoding(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should pass through codeable concept")
        void shouldPassThroughCodeableConcept() {
            // Given
            CodeableConcept concept = new CodeableConcept();
            concept.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("38341003")
                .setDisplay("Hypertensive disorder");
            concept.setText("High blood pressure");

            // When
            CodeableConcept result = mapper.normalizeCoding(concept);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCodingFirstRep().getCode()).isEqualTo("38341003");
            assertThat(result.getText()).isEqualTo("High blood pressure");
        }
    }
}
