package com.healthdata.cdr.converter;

import com.healthdata.cdr.dto.CdaDocument;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CdaToFhirConverter.
 * Tests conversion of CDA/C-CDA documents to FHIR R4 resources.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CDA to FHIR Converter Tests")
class CdaToFhirConverterTest {

    @InjectMocks
    private CdaToFhirConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CdaToFhirConverter();
    }

    @Nested
    @DisplayName("Patient Section Conversion")
    class PatientSectionTests {

        @Test
        @DisplayName("Should convert patient demographics to Patient resource")
        void convertToFhir_withPatientData_createsPatientResource() {
            CdaDocument document = createCdaDocumentWithPatient();

            Bundle bundle = converter.convertToFhir(document);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
            
            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
            assertThat(patient.get().getName()).isNotEmpty();
            assertThat(patient.get().getName().get(0).getFamily()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("Should set patient identifiers from CDA")
        void convertToFhir_withPatientIdentifiers_setsIdentifiers() {
            CdaDocument document = createCdaDocumentWithPatient();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
            assertThat(patient.get().getIdentifier()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Problems Section Conversion")
    class ProblemsSectionTests {

        @Test
        @DisplayName("Should convert problems to Condition resources")
        void convertToFhir_withProblems_createsConditionResources() {
            CdaDocument document = createCdaDocumentWithProblems();

            Bundle bundle = converter.convertToFhir(document);

            List<Condition> conditions = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Condition)
                .map(e -> (Condition) e.getResource())
                .toList();
            assertThat(conditions).isNotEmpty();
            assertThat(conditions.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should set condition code from CDA problem")
        void convertToFhir_withProblemCode_setsCode() {
            CdaDocument document = createCdaDocumentWithProblems();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Condition> condition = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Condition)
                .map(e -> (Condition) e.getResource())
                .findFirst();
            assertThat(condition).isPresent();
            assertThat(condition.get().getCode()).isNotNull();
            assertThat(condition.get().getCode().getCoding()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Medications Section Conversion")
    class MedicationsSectionTests {

        @Test
        @DisplayName("Should convert medications to MedicationRequest resources")
        void convertToFhir_withMedications_createsMedicationRequestResources() {
            CdaDocument document = createCdaDocumentWithMedications();

            Bundle bundle = converter.convertToFhir(document);

            List<MedicationRequest> medications = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof MedicationRequest)
                .map(e -> (MedicationRequest) e.getResource())
                .toList();
            assertThat(medications).isNotEmpty();
        }

        @Test
        @DisplayName("Should set medication code from CDA")
        void convertToFhir_withMedicationCode_setsCode() {
            CdaDocument document = createCdaDocumentWithMedications();

            Bundle bundle = converter.convertToFhir(document);

            Optional<MedicationRequest> medication = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof MedicationRequest)
                .map(e -> (MedicationRequest) e.getResource())
                .findFirst();
            assertThat(medication).isPresent();
            assertThat(medication.get().getMedication()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Allergies Section Conversion")
    class AllergiesSectionTests {

        @Test
        @DisplayName("Should convert allergies to AllergyIntolerance resources")
        void convertToFhir_withAllergies_createsAllergyIntoleranceResources() {
            CdaDocument document = createCdaDocumentWithAllergies();

            Bundle bundle = converter.convertToFhir(document);

            List<AllergyIntolerance> allergies = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof AllergyIntolerance)
                .map(e -> (AllergyIntolerance) e.getResource())
                .toList();
            assertThat(allergies).isNotEmpty();
        }

        @Test
        @DisplayName("Should set allergen code from CDA")
        void convertToFhir_withAllergenCode_setsCode() {
            CdaDocument document = createCdaDocumentWithAllergies();

            Bundle bundle = converter.convertToFhir(document);

            Optional<AllergyIntolerance> allergy = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof AllergyIntolerance)
                .map(e -> (AllergyIntolerance) e.getResource())
                .findFirst();
            assertThat(allergy).isPresent();
            assertThat(allergy.get().getCode()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Immunizations Section Conversion")
    class ImmunizationsSectionTests {

        @Test
        @DisplayName("Should convert immunizations to Immunization resources")
        void convertToFhir_withImmunizations_createsImmunizationResources() {
            CdaDocument document = createCdaDocumentWithImmunizations();

            Bundle bundle = converter.convertToFhir(document);

            List<Immunization> immunizations = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Immunization)
                .map(e -> (Immunization) e.getResource())
                .toList();
            assertThat(immunizations).isNotEmpty();
        }

        @Test
        @DisplayName("Should set vaccine code from CDA")
        void convertToFhir_withVaccineCode_setsCode() {
            CdaDocument document = createCdaDocumentWithImmunizations();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Immunization> immunization = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Immunization)
                .map(e -> (Immunization) e.getResource())
                .findFirst();
            assertThat(immunization).isPresent();
            assertThat(immunization.get().getVaccineCode()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Procedures Section Conversion")
    class ProceduresSectionTests {

        @Test
        @DisplayName("Should convert procedures to Procedure resources")
        void convertToFhir_withProcedures_createsProcedureResources() {
            CdaDocument document = createCdaDocumentWithProcedures();

            Bundle bundle = converter.convertToFhir(document);

            List<Procedure> procedures = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Procedure)
                .map(e -> (Procedure) e.getResource())
                .toList();
            assertThat(procedures).isNotEmpty();
        }

        @Test
        @DisplayName("Should set procedure code from CDA")
        void convertToFhir_withProcedureCode_setsCode() {
            CdaDocument document = createCdaDocumentWithProcedures();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Procedure> procedure = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Procedure)
                .map(e -> (Procedure) e.getResource())
                .findFirst();
            assertThat(procedure).isPresent();
            assertThat(procedure.get().getCode()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Results Section Conversion")
    class ResultsSectionTests {

        @Test
        @DisplayName("Should convert lab results to Observation resources")
        void convertToFhir_withResults_createsObservationResources() {
            CdaDocument document = createCdaDocumentWithResults();

            Bundle bundle = converter.convertToFhir(document);

            List<Observation> observations = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .toList();
            assertThat(observations).isNotEmpty();
        }

        @Test
        @DisplayName("Should set observation code from CDA result")
        void convertToFhir_withResultCode_setsCode() {
            CdaDocument document = createCdaDocumentWithResults();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Observation> observation = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .findFirst();
            assertThat(observation).isPresent();
            assertThat(observation.get().getCode()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Vital Signs Section Conversion")
    class VitalSignsSectionTests {

        @Test
        @DisplayName("Should convert vital signs to Observation resources")
        void convertToFhir_withVitalSigns_createsObservationResources() {
            CdaDocument document = createCdaDocumentWithVitalSigns();

            Bundle bundle = converter.convertToFhir(document);

            List<Observation> observations = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .toList();
            assertThat(observations).isNotEmpty();
        }

        @Test
        @DisplayName("Should set vital signs category to vital-signs")
        void convertToFhir_withVitalSigns_setsVitalSignsCategory() {
            CdaDocument document = createCdaDocumentWithVitalSigns();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Observation> observation = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .findFirst();
            assertThat(observation).isPresent();
            assertThat(observation.get().getCategory()).isNotEmpty();
            assertThat(observation.get().getCategory().get(0).getCoding().get(0).getCode())
                .isEqualTo("vital-signs");
        }
    }

    @Nested
    @DisplayName("Encounters Section Conversion")
    class EncountersSectionTests {

        @Test
        @DisplayName("Should convert encounters to Encounter resources")
        void convertToFhir_withEncounters_createsEncounterResources() {
            CdaDocument document = createCdaDocumentWithEncounters();

            Bundle bundle = converter.convertToFhir(document);

            List<Encounter> encounters = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .toList();
            assertThat(encounters).isNotEmpty();
        }

        @Test
        @DisplayName("Should set encounter status from CDA")
        void convertToFhir_withEncounterStatus_setsStatus() {
            CdaDocument document = createCdaDocumentWithEncounters();

            Bundle bundle = converter.convertToFhir(document);

            Optional<Encounter> encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst();
            assertThat(encounter).isPresent();
            assertThat(encounter.get().getStatus()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Empty/Null Data Handling")
    class EmptyDataTests {

        @Test
        @DisplayName("Should return empty bundle for null document")
        void convertToFhir_withNullDocument_returnsEmptyBundle() {
            Bundle bundle = converter.convertToFhir(null);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isEmpty();
        }

        @Test
        @DisplayName("Should handle document with no sections")
        void convertToFhir_withEmptyDocument_returnsEmptyBundle() {
            CdaDocument document = CdaDocument.builder()
                .documentType("CCD")
                .status("PARSED")
                .build();

            Bundle bundle = converter.convertToFhir(document);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isEmpty();
        }
    }

    // Helper methods to create test CDA documents

    private CdaDocument createCdaDocumentWithPatient() {
        Map<String, Object> patient = new HashMap<>();
        patient.put("gender", "M");
        patient.put("birthTime", "19800115");

        Map<String, Object> name = new HashMap<>();
        name.put("family", "Smith");
        name.put("given", List.of("John"));
        patient.put("name", name);

        List<Map<String, String>> identifiers = new ArrayList<>();
        Map<String, String> identifier = new HashMap<>();
        identifier.put("root", "2.16.840.1.113883.4.1");
        identifier.put("extension", "12345");
        identifiers.add(identifier);
        patient.put("identifiers", identifiers);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .build();
    }

    private CdaDocument createCdaDocumentWithProblems() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> problems = new ArrayList<>();
        Map<String, Object> problem = new HashMap<>();
        Map<String, String> code = new HashMap<>();
        code.put("code", "E11.9");
        code.put("displayName", "Type 2 diabetes mellitus without complications");
        problem.put("code", code);
        problem.put("codeSystemUri", "http://hl7.org/fhir/sid/icd-10-cm");
        problem.put("clinicalStatus", "active");
        problem.put("onsetDate", "20200101");
        problems.add(problem);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .problems(problems)
            .build();
    }

    private CdaDocument createCdaDocumentWithMedications() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> medications = new ArrayList<>();
        Map<String, Object> medication = new HashMap<>();
        Map<String, String> drugCode = new HashMap<>();
        drugCode.put("code", "314076");
        drugCode.put("displayName", "Lisinopril 10 MG Oral Tablet");
        medication.put("drugCode", drugCode);
        medication.put("codeSystemUri", "http://www.nlm.nih.gov/research/umls/rxnorm");
        medication.put("status", "active");
        medications.add(medication);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .medications(medications)
            .build();
    }

    private CdaDocument createCdaDocumentWithAllergies() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> allergies = new ArrayList<>();
        Map<String, Object> allergy = new HashMap<>();
        Map<String, String> allergen = new HashMap<>();
        allergen.put("code", "7980");
        allergen.put("displayName", "Penicillin");
        allergy.put("allergen", allergen);
        allergy.put("allergenCodeSystemUri", "http://www.nlm.nih.gov/research/umls/rxnorm");
        allergy.put("clinicalStatus", "active");
        allergy.put("criticality", "high");
        allergies.add(allergy);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .allergies(allergies)
            .build();
    }

    private CdaDocument createCdaDocumentWithImmunizations() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> immunizations = new ArrayList<>();
        Map<String, Object> immunization = new HashMap<>();
        Map<String, String> vaccineCode = new HashMap<>();
        vaccineCode.put("code", "03");
        vaccineCode.put("displayName", "MMR");
        immunization.put("vaccineCode", vaccineCode);
        immunization.put("vaccineCodeSystemUri", "http://hl7.org/fhir/sid/cvx");
        immunization.put("status", "completed");
        immunization.put("administrationDate", "20230115");
        immunizations.add(immunization);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .immunizations(immunizations)
            .build();
    }

    private CdaDocument createCdaDocumentWithProcedures() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> procedures = new ArrayList<>();
        Map<String, Object> procedure = new HashMap<>();
        Map<String, String> code = new HashMap<>();
        code.put("code", "27447");
        code.put("displayName", "Arthroscopy, knee, surgical");
        procedure.put("code", code);
        procedure.put("codeSystemUri", "http://www.ama-assn.org/go/cpt");
        procedure.put("status", "completed");
        procedure.put("performedDateTime", "20230115");
        procedures.add(procedure);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .procedures(procedures)
            .build();
    }

    private CdaDocument createCdaDocumentWithResults() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        Map<String, String> code = new HashMap<>();
        code.put("code", "2345-7");
        code.put("displayName", "Glucose");
        result.put("code", code);
        result.put("codeSystemUri", "http://loinc.org");
        result.put("status", "completed");
        result.put("valueType", "Quantity");
        Map<String, String> valueQuantity = new HashMap<>();
        valueQuantity.put("value", "95");
        valueQuantity.put("unit", "mg/dL");
        result.put("valueQuantity", valueQuantity);
        result.put("effectiveDateTime", "20240115");
        results.add(result);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .results(results)
            .build();
    }

    private CdaDocument createCdaDocumentWithVitalSigns() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> vitalSigns = new ArrayList<>();
        Map<String, Object> vitalSign = new HashMap<>();
        Map<String, String> code = new HashMap<>();
        code.put("code", "8480-6");
        code.put("displayName", "Systolic blood pressure");
        vitalSign.put("code", code);
        vitalSign.put("codeSystemUri", "http://loinc.org");
        vitalSign.put("status", "completed");
        vitalSign.put("valueType", "Quantity");
        Map<String, String> valueQuantity = new HashMap<>();
        valueQuantity.put("value", "120");
        valueQuantity.put("unit", "mmHg");
        vitalSign.put("valueQuantity", valueQuantity);
        vitalSign.put("effectiveDateTime", "20240115");
        vitalSigns.add(vitalSign);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .vitalSigns(vitalSigns)
            .build();
    }

    private CdaDocument createCdaDocumentWithEncounters() {
        Map<String, Object> patient = createBasicPatient();
        
        List<Map<String, Object>> encounters = new ArrayList<>();
        Map<String, Object> encounter = new HashMap<>();
        encounter.put("status", "finished");
        encounter.put("class", "AMB");
        Map<String, String> type = new HashMap<>();
        type.put("code", "99213");
        type.put("displayName", "Office visit");
        encounter.put("type", type);
        encounter.put("periodStart", "20240115");
        encounter.put("periodEnd", "20240115");
        encounters.add(encounter);

        return CdaDocument.builder()
            .documentType("CCD")
            .status("PARSED")
            .patient(patient)
            .encounters(encounters)
            .build();
    }

    private Map<String, Object> createBasicPatient() {
        Map<String, Object> patient = new HashMap<>();
        patient.put("gender", "M");
        patient.put("birthTime", "19800115");
        
        Map<String, Object> name = new HashMap<>();
        name.put("family", "Smith");
        name.put("given", List.of("John"));
        patient.put("name", name);
        
        return patient;
    }
}
