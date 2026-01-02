package com.healthdata.ecr.service;

import com.healthdata.ecr.persistence.ElectronicCaseReportEntity;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.*;
import com.healthdata.ecr.service.EicrGeneratorService.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EicrGeneratorService.
 * Tests eICR FHIR Bundle generation conforming to HL7 eCR IG.
 */
@ExtendWith(MockitoExtension.class)
class EicrGeneratorServiceTest {

    @InjectMocks
    private EicrGeneratorService eicrGeneratorService;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ENCOUNTER_ID = UUID.randomUUID();
    private static final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eicrGeneratorService, "authorOrganization", "Test Health System");
        ReflectionTestUtils.setField(eicrGeneratorService, "custodianOid", "2.16.840.1.113883.3.12345");
    }

    @Nested
    @DisplayName("generateEicr() bundle structure tests")
    class BundleStructureTests {

        @Test
        @DisplayName("Should generate valid FHIR Bundle")
        void generateEicr_shouldCreateValidBundle() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            EncounterData encounterData = createEncounterData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

            // Assert
            assertThat(bundle).isNotNull();
            assertThat(bundle.getId()).isNotNull();
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.DOCUMENT);
            assertThat(bundle.getTimestamp()).isNotNull();
            assertThat(bundle.getEntry()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include Composition as first entry")
        void generateEicr_shouldHaveCompositionFirst() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            assertThat(bundle.getEntry()).isNotEmpty();
            Resource firstResource = bundle.getEntry().get(0).getResource();
            assertThat(firstResource).isInstanceOf(Composition.class);
        }

        @Test
        @DisplayName("Should include all required resources")
        void generateEicr_shouldIncludeRequiredResources() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            EncounterData encounterData = createEncounterData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

            // Assert - Check for required resource types
            boolean hasComposition = false;
            boolean hasPatient = false;
            boolean hasPractitioner = false;
            boolean hasOrganization = false;
            boolean hasEncounter = false;
            boolean hasCondition = false;

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource instanceof Composition) hasComposition = true;
                if (resource instanceof Patient) hasPatient = true;
                if (resource instanceof Practitioner) hasPractitioner = true;
                if (resource instanceof Organization) hasOrganization = true;
                if (resource instanceof Encounter) hasEncounter = true;
                if (resource instanceof Condition) hasCondition = true;
            }

            assertThat(hasComposition).isTrue();
            assertThat(hasPatient).isTrue();
            assertThat(hasPractitioner).isTrue();
            assertThat(hasOrganization).isTrue();
            assertThat(hasEncounter).isTrue();
            assertThat(hasCondition).isTrue();
        }

        @Test
        @DisplayName("Should generate without encounter data")
        void generateEicr_withoutEncounter_shouldSucceed() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isNotEmpty();

            // Should not contain Encounter resource
            boolean hasEncounter = bundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Encounter);
            assertThat(hasEncounter).isFalse();
        }

        @Test
        @DisplayName("Bundle entries should have fullUrl with urn:uuid:")
        void generateEicr_entriesShouldHaveFullUrls() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                assertThat(entry.getFullUrl()).startsWith("urn:uuid:");
            }
        }
    }

    @Nested
    @DisplayName("Composition tests")
    class CompositionTests {

        @Test
        @DisplayName("Should set eICR profile URL")
        void generateEicr_compositionShouldHaveProfile() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);
            Composition composition = (Composition) bundle.getEntry().get(0).getResource();

            // Assert
            assertThat(composition.getMeta().getProfile()).isNotEmpty();
            assertThat(composition.getMeta().getProfile().get(0).getValue())
                .contains("eicr-composition");
        }

        @Test
        @DisplayName("Should set document type code")
        void generateEicr_compositionShouldHaveTypeCode() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);
            Composition composition = (Composition) bundle.getEntry().get(0).getResource();

            // Assert - LOINC code 55751-2 for Public health Case report
            assertThat(composition.getType().getCoding()).isNotEmpty();
            assertThat(composition.getType().getCoding().get(0).getCode()).isEqualTo("55751-2");
        }

        @Test
        @DisplayName("Should include condition name in title")
        void generateEicr_compositionTitleShouldIncludeCondition() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            ecr.setConditionName("COVID-19");
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);
            Composition composition = (Composition) bundle.getEntry().get(0).getResource();

            // Assert
            assertThat(composition.getTitle()).contains("COVID-19");
        }

        @Test
        @DisplayName("Should have Reason for Report section")
        void generateEicr_compositionShouldHaveReasonSection() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);
            Composition composition = (Composition) bundle.getEntry().get(0).getResource();

            // Assert
            assertThat(composition.getSection()).isNotEmpty();
            boolean hasReasonSection = composition.getSection().stream()
                .anyMatch(s -> "Reason for Report".equals(s.getTitle()));
            assertThat(hasReasonSection).isTrue();
        }
    }

    @Nested
    @DisplayName("Patient resource tests")
    class PatientResourceTests {

        @Test
        @DisplayName("Should create patient with demographics")
        void generateEicr_patientShouldHaveDemographics() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = PatientData.builder()
                .patientId(PATIENT_ID)
                .mrn("MRN123456")
                .firstName("John")
                .lastName("Doe")
                .birthDate(LocalDate.of(1980, 5, 15))
                .gender("male")
                .phone("555-1234")
                .build();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Find patient resource
            Patient patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst()
                .orElseThrow();

            // Assert
            assertThat(patient.getName()).isNotEmpty();
            assertThat(patient.getName().get(0).getFamily()).isEqualTo("Doe");
            assertThat(patient.getName().get(0).getGiven().get(0).getValue()).isEqualTo("John");
            assertThat(patient.getBirthDate()).isNotNull();
            assertThat(patient.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
        }

        @Test
        @DisplayName("Should set patient profile URL")
        void generateEicr_patientShouldHaveProfile() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            Patient patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst()
                .orElseThrow();

            // Assert
            assertThat(patient.getMeta().getProfile()).isNotEmpty();
            assertThat(patient.getMeta().getProfile().get(0).getValue())
                .contains("us-ph-patient");
        }

        @Test
        @DisplayName("Should include patient address")
        void generateEicr_patientShouldHaveAddress() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = PatientData.builder()
                .patientId(PATIENT_ID)
                .mrn("MRN123456")
                .firstName("John")
                .lastName("Doe")
                .address(AddressData.builder()
                    .street("123 Main St")
                    .city("Springfield")
                    .state("IL")
                    .zipCode("62701")
                    .build())
                .build();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            Patient patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst()
                .orElseThrow();

            // Assert
            assertThat(patient.getAddress()).isNotEmpty();
            Address address = patient.getAddress().get(0);
            assertThat(address.getCity()).isEqualTo("Springfield");
            assertThat(address.getState()).isEqualTo("IL");
            assertThat(address.getPostalCode()).isEqualTo("62701");
        }
    }

    @Nested
    @DisplayName("Trigger category tests")
    class TriggerCategoryTests {

        @Test
        @DisplayName("Should create Condition for DIAGNOSIS trigger")
        void generateEicr_diagnosisTrigger_shouldCreateCondition() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            boolean hasCondition = bundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Condition);
            assertThat(hasCondition).isTrue();

            Condition condition = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Condition)
                .map(e -> (Condition) e.getResource())
                .findFirst()
                .orElseThrow();

            assertThat(condition.getCode().getCoding().get(0).getCode()).isEqualTo("U07.1");
        }

        @Test
        @DisplayName("Should create Observation for LAB_RESULT trigger")
        void generateEicr_labResultTrigger_shouldCreateObservation() {
            // Arrange
            ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .triggerCode("94500-6")
                .triggerCodeSystem("http://loinc.org")
                .triggerDisplay("SARS-CoV-2 RNA [Presence] in Respiratory specimen")
                .triggerCategory(TriggerCategory.LAB_RESULT)
                .conditionName("COVID-19")
                .status(EcrStatus.PENDING)
                .urgency(EcrUrgency.WITHIN_24_HOURS)
                .build();
            PatientData patientData = createPatientData();
            TriggerData triggerData = TriggerData.builder()
                .authorName("Dr. Lab Tech")
                .labValue("Detected")
                .build();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            boolean hasObservation = bundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Observation);
            assertThat(hasObservation).isTrue();

            Observation observation = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .findFirst()
                .orElseThrow();

            assertThat(observation.getCode().getCoding().get(0).getCode()).isEqualTo("94500-6");
            assertThat(observation.getCategory().get(0).getCoding().get(0).getCode())
                .isEqualTo("laboratory");
        }

        @Test
        @DisplayName("Should create MedicationAdministration for MEDICATION trigger")
        void generateEicr_medicationTrigger_shouldCreateMedicationAdministration() {
            // Arrange
            ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .triggerCode("2058869")
                .triggerCodeSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .triggerDisplay("Tecovirimat")
                .triggerCategory(TriggerCategory.MEDICATION)
                .conditionName("Mpox")
                .status(EcrStatus.PENDING)
                .urgency(EcrUrgency.IMMEDIATE)
                .build();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            boolean hasMedAdmin = bundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof MedicationAdministration);
            assertThat(hasMedAdmin).isTrue();
        }

        @Test
        @DisplayName("Should create Procedure for PROCEDURE trigger")
        void generateEicr_procedureTrigger_shouldCreateProcedure() {
            // Arrange
            ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .triggerCode("713084008")
                .triggerCodeSystem("http://snomed.info/sct")
                .triggerDisplay("Autopsy")
                .triggerCategory(TriggerCategory.PROCEDURE)
                .conditionName("Unknown cause of death")
                .status(EcrStatus.PENDING)
                .urgency(EcrUrgency.WITHIN_72_HOURS)
                .build();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert
            boolean hasProcedure = bundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Procedure);
            assertThat(hasProcedure).isTrue();
        }
    }

    @Nested
    @DisplayName("Encounter resource tests")
    class EncounterResourceTests {

        @Test
        @DisplayName("Should create encounter with class code")
        void generateEicr_encounterShouldHaveClass() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            EncounterData encounterData = EncounterData.builder()
                .encounterId(ENCOUNTER_ID)
                .encounterClass("EMER")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now())
                .facilityName("Test Hospital")
                .build();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

            Encounter encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst()
                .orElseThrow();

            // Assert
            assertThat(encounter.getClass_().getCode()).isEqualTo("EMER");
            assertThat(encounter.getClass_().getDisplay()).isEqualTo("emergency");
        }

        @Test
        @DisplayName("Should set encounter period")
        void generateEicr_encounterShouldHavePeriod() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();
            EncounterData encounterData = EncounterData.builder()
                .encounterId(ENCOUNTER_ID)
                .encounterClass("IMP")
                .startTime(startTime)
                .endTime(endTime)
                .build();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, encounterData, triggerData);

            Encounter encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst()
                .orElseThrow();

            // Assert
            assertThat(encounter.getPeriod()).isNotNull();
            assertThat(encounter.getPeriod().getStart()).isNotNull();
            assertThat(encounter.getPeriod().getEnd()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Organization resource tests")
    class OrganizationResourceTests {

        @Test
        @DisplayName("Should include author organization")
        void generateEicr_shouldHaveAuthorOrganization() {
            // Arrange
            ElectronicCaseReportEntity ecr = createDiagnosisEcr();
            PatientData patientData = createPatientData();
            TriggerData triggerData = createTriggerData();

            // Act
            Bundle bundle = eicrGeneratorService.generateEicr(ecr, patientData, null, triggerData);

            // Assert - Should have at least one organization (author + custodian)
            long orgCount = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Organization)
                .count();
            assertThat(orgCount).isGreaterThanOrEqualTo(2);
        }
    }

    // Helper methods

    private ElectronicCaseReportEntity createDiagnosisEcr() {
        return ElectronicCaseReportEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .encounterId(ENCOUNTER_ID)
            .triggerCode("U07.1")
            .triggerCodeSystem("http://hl7.org/fhir/sid/icd-10-cm")
            .triggerDisplay("COVID-19")
            .triggerCategory(TriggerCategory.DIAGNOSIS)
            .conditionName("COVID-19")
            .status(EcrStatus.PENDING)
            .urgency(EcrUrgency.WITHIN_24_HOURS)
            .build();
    }

    private PatientData createPatientData() {
        return PatientData.builder()
            .patientId(PATIENT_ID)
            .mrn("MRN123456")
            .firstName("Jane")
            .lastName("Smith")
            .birthDate(LocalDate.of(1985, 3, 20))
            .gender("female")
            .phone("555-9876")
            .build();
    }

    private EncounterData createEncounterData() {
        return EncounterData.builder()
            .encounterId(ENCOUNTER_ID)
            .encounterClass("AMB")
            .startTime(LocalDateTime.now().minusHours(1))
            .facilityName("Test Clinic")
            .build();
    }

    private TriggerData createTriggerData() {
        return TriggerData.builder()
            .authorName("Dr. Test Provider")
            .onsetDate(LocalDateTime.now().minusDays(3))
            .build();
    }
}
