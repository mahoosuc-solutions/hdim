package com.healthdata.ingestion.generator;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit tests for SyntheticPatientGenerator.
 * Tests the generation of synthetic FHIR R4 compliant patient data.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Synthetic Patient Generator Tests")
@Tag("unit")
class SyntheticPatientGeneratorTest {

    @Mock
    private MedicationGenerator medicationGenerator;

    @Mock
    private ObservationGenerator observationGenerator;

    @Mock
    private EncounterGenerator encounterGenerator;

    @Mock
    private ProcedureGenerator procedureGenerator;

    private SyntheticPatientGenerator patientGenerator;

    private static final String TEST_TENANT_ID = "test-tenant-001";

    @BeforeEach
    void setUp() {
        FhirContext fhirContext = FhirContext.forR4();
        ObjectMapper objectMapper = new ObjectMapper();

        patientGenerator = new SyntheticPatientGenerator(
                fhirContext,
                medicationGenerator,
                observationGenerator,
                encounterGenerator,
                procedureGenerator,
                objectMapper
        );
    }

    @Nested
    @DisplayName("Cohort Generation Tests")
    class CohortGenerationTests {

        @Test
        @DisplayName("Should generate requested number of patients")
        void shouldGenerateRequestedNumberOfPatients() {
            // Given
            int patientCount = 10;

            // When
            Bundle bundle = patientGenerator.generateCohort(patientCount, TEST_TENANT_ID);

            // Then
            assertThat(bundle).isNotNull();
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.COLLECTION);

            // Count Patient resources (not conditions, medications, etc.)
            long actualPatientCount = bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .count();
            assertThat(actualPatientCount).isEqualTo(patientCount);
        }

        @Test
        @DisplayName("Should generate bundle with timestamp")
        void shouldGenerateBundleWithTimestamp() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            assertThat(bundle.getTimestamp()).isNotNull();
            assertThat(bundle.getTimestamp()).isBefore(new Date());
        }

        @Test
        @DisplayName("Should generate bundle with correct type")
        void shouldGenerateBundleWithCorrectType() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
        }
    }

    @Nested
    @DisplayName("Patient Resource Tests")
    class PatientResourceTests {

        @Test
        @DisplayName("Should generate patient with unique ID")
        void shouldGeneratePatientWithUniqueId() {
            // When
            Bundle bundle = patientGenerator.generateCohort(2, TEST_TENANT_ID);

            // Then
            List<Patient> patients = bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .map(entry -> (Patient) entry.getResource())
                    .toList();

            assertThat(patients.get(0).getId()).isNotEqualTo(patients.get(1).getId());
        }

        @Test
        @DisplayName("Should generate patient with name")
        void shouldGeneratePatientWithName() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            assertThat(patient.getName()).isNotEmpty();
            HumanName name = patient.getNameFirstRep();
            assertThat(name.getFamily()).isNotBlank();
            assertThat(name.getGiven()).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate patient with gender")
        void shouldGeneratePatientWithGender() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            assertThat(patient.getGender()).isNotNull();
            assertThat(patient.getGender()).isIn(
                    Enumerations.AdministrativeGender.MALE,
                    Enumerations.AdministrativeGender.FEMALE
            );
        }

        @Test
        @DisplayName("Should generate patient with birth date")
        void shouldGeneratePatientWithBirthDate() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            assertThat(patient.getBirthDate()).isNotNull();
            assertThat(patient.getBirthDate()).isBefore(new Date()); // In the past
        }

        @Test
        @DisplayName("Should generate patient with address")
        void shouldGeneratePatientWithAddress() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            assertThat(patient.getAddress()).isNotEmpty();
            Address address = patient.getAddressFirstRep();
            assertThat(address.getCity()).isNotBlank();
            assertThat(address.getState()).isNotBlank();
            assertThat(address.getPostalCode()).isNotBlank();
            assertThat(address.getCountry()).isEqualTo("USA");
        }

        @Test
        @DisplayName("Should generate patient with MRN identifier")
        void shouldGeneratePatientWithMrnIdentifier() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            assertThat(patient.getIdentifier()).isNotEmpty();
            Identifier mrn = patient.getIdentifierFirstRep();
            assertThat(mrn.getValue()).startsWith("MRN");
        }

        @Test
        @DisplayName("Should generate patient with tenant ID extension")
        void shouldGeneratePatientWithTenantIdExtension() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            Optional<Extension> tenantExt = patient.getExtension().stream()
                    .filter(ext -> ext.getUrl().contains("tenant-id"))
                    .findFirst();

            assertThat(tenantExt).isPresent();
            assertThat(((StringType) tenantExt.get().getValue()).getValue()).isEqualTo(TEST_TENANT_ID);
        }

        @Test
        @DisplayName("Should generate patient with HCC risk score extension")
        void shouldGeneratePatientWithHccRiskScoreExtension() {
            // When
            Bundle bundle = patientGenerator.generateCohort(1, TEST_TENANT_ID);

            // Then
            Patient patient = (Patient) bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .findFirst()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .orElseThrow();

            Optional<Extension> hccExt = patient.getExtension().stream()
                    .filter(ext -> ext.getUrl().contains("hcc-risk-score"))
                    .findFirst();

            assertThat(hccExt).isPresent();
            BigDecimal hccScore = ((DecimalType) hccExt.get().getValue()).getValue();
            assertThat(hccScore).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Risk Distribution Tests")
    class RiskDistributionTests {

        @Test
        @DisplayName("Should generate patients with varied HCC risk scores")
        void shouldGeneratePatientsWithVariedHccRiskScores() {
            // Given - Generate enough patients to see distribution
            int patientCount = 100;

            // When
            Bundle bundle = patientGenerator.generateCohort(patientCount, TEST_TENANT_ID);

            // Then
            List<BigDecimal> hccScores = bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .map(entry -> (Patient) entry.getResource())
                    .map(patient -> patient.getExtension().stream()
                            .filter(ext -> ext.getUrl().contains("hcc-risk-score"))
                            .findFirst()
                            .map(ext -> ((DecimalType) ext.getValue()).getValue())
                            .orElse(BigDecimal.ZERO))
                    .toList();

            // Verify distribution - should have low, moderate, and high risk patients
            long lowRisk = hccScores.stream().filter(s -> s.compareTo(BigDecimal.ONE) < 0).count();
            long moderateRisk = hccScores.stream()
                    .filter(s -> s.compareTo(BigDecimal.ONE) >= 0 && s.compareTo(new BigDecimal("2.0")) < 0)
                    .count();
            long highRisk = hccScores.stream().filter(s -> s.compareTo(new BigDecimal("2.0")) >= 0).count();

            assertThat(lowRisk).isGreaterThan(0);
            assertThat(moderateRisk).isGreaterThan(0);
            assertThat(highRisk).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Condition Generation Tests")
    class ConditionGenerationTests {

        @Test
        @DisplayName("Should generate conditions for patients")
        void shouldGenerateConditionsForPatients() {
            // When
            Bundle bundle = patientGenerator.generateCohort(10, TEST_TENANT_ID);

            // Then
            long conditionCount = bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Condition)
                    .count();

            // At least some patients should have conditions
            assertThat(conditionCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should generate conditions with required fields")
        void shouldGenerateConditionsWithRequiredFields() {
            // When
            Bundle bundle = patientGenerator.generateCohort(20, TEST_TENANT_ID);

            // Then
            bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Condition)
                    .map(entry -> (Condition) entry.getResource())
                    .forEach(condition -> {
                        assertThat(condition.getId()).isNotBlank();
                        assertThat(condition.getSubject()).isNotNull();
                        assertThat(condition.getClinicalStatus()).isNotNull();
                        assertThat(condition.getVerificationStatus()).isNotNull();
                        assertThat(condition.getCode()).isNotNull();
                        assertThat(condition.getOnset()).isNotNull();
                    });
        }

        @Test
        @DisplayName("Should generate conditions with valid ICD-10 codes")
        void shouldGenerateConditionsWithValidIcd10Codes() {
            // When
            Bundle bundle = patientGenerator.generateCohort(20, TEST_TENANT_ID);

            // Then
            List<String> validCodes = List.of("E11.9", "I10", "I50.9", "J44.9", "N18.3");

            bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Condition)
                    .map(entry -> (Condition) entry.getResource())
                    .forEach(condition -> {
                        String code = condition.getCode().getCodingFirstRep().getCode();
                        assertThat(validCodes).contains(code);
                    });
        }
    }

    @Nested
    @DisplayName("Age Distribution Tests")
    class AgeDistributionTests {

        @Test
        @DisplayName("Should generate patients across all age ranges")
        void shouldGeneratePatientsAcrossAllAgeRanges() {
            // Given
            int patientCount = 100;

            // When
            Bundle bundle = patientGenerator.generateCohort(patientCount, TEST_TENANT_ID);

            // Then
            LocalDate today = LocalDate.now();
            List<Integer> ages = bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .map(entry -> (Patient) entry.getResource())
                    .map(patient -> {
                        LocalDate birthDate = patient.getBirthDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        return today.getYear() - birthDate.getYear();
                    })
                    .toList();

            // Should have young adults (18-44), middle age (45-64), seniors (65+)
            long youngAdults = ages.stream().filter(age -> age >= 18 && age <= 44).count();
            long middleAge = ages.stream().filter(age -> age >= 45 && age <= 64).count();
            long seniors = ages.stream().filter(age -> age >= 65).count();

            assertThat(youngAdults).isGreaterThan(0);
            assertThat(middleAge).isGreaterThan(0);
            assertThat(seniors).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should generate patients with valid age range")
        void shouldGeneratePatientsWithValidAgeRange() {
            // Given
            int patientCount = 50;

            // When
            Bundle bundle = patientGenerator.generateCohort(patientCount, TEST_TENANT_ID);

            // Then
            LocalDate today = LocalDate.now();
            bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .map(entry -> (Patient) entry.getResource())
                    .forEach(patient -> {
                        LocalDate birthDate = patient.getBirthDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        int age = today.getYear() - birthDate.getYear();

                        assertThat(age).isBetween(18, 85);
                    });
        }
    }
}
