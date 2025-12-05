package com.healthdata.ehr.connector.cerner;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CernerDataMapperTest {

    private CernerDataMapper mapper;
    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        mapper = new CernerDataMapper(fhirContext);
    }

    @Test
    void testMapPatient_WithBasicFields() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("12345");
        patient.addName()
                .setFamily("Doe")
                .addGiven("John");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(new Date());

        // Act
        Patient mappedPatient = mapper.mapPatient(patient);

        // Assert
        assertNotNull(mappedPatient);
        assertEquals("12345", mappedPatient.getIdElement().getIdPart());
        assertEquals("Doe", mappedPatient.getNameFirstRep().getFamily());
        assertEquals("John", mappedPatient.getNameFirstRep().getGivenAsSingleString());
    }

    @Test
    void testMapPatient_WithCernerExtensions() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("12345");
        patient.addExtension()
                .setUrl("http://fhir.cerner.com/extension/patient-friendly-name")
                .setValue(new StringType("Johnny"));

        // Act
        Patient mappedPatient = mapper.mapPatient(patient);

        // Assert
        assertNotNull(mappedPatient);
        List<Extension> extensions = mappedPatient.getExtensionsByUrl(
                "http://fhir.cerner.com/extension/patient-friendly-name");
        assertEquals(1, extensions.size());
    }

    @Test
    void testMapPatient_WithMultipleIdentifiers() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("12345");
        patient.addIdentifier()
                .setSystem("urn:oid:2.16.840.1.113883.3.13.6")
                .setValue("MRN123");
        patient.addIdentifier()
                .setSystem("http://hl7.org/fhir/sid/us-ssn")
                .setValue("123-45-6789");

        // Act
        Patient mappedPatient = mapper.mapPatient(patient);

        // Assert
        assertEquals(2, mappedPatient.getIdentifier().size());
    }

    @Test
    void testMapEncounter_WithBasicFields() {
        // Arrange
        Encounter encounter = new Encounter();
        encounter.setId("ENC-123");
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB"));

        // Act
        Encounter mappedEncounter = mapper.mapEncounter(encounter);

        // Assert
        assertNotNull(mappedEncounter);
        assertEquals("ENC-123", mappedEncounter.getIdElement().getIdPart());
        assertEquals(Encounter.EncounterStatus.FINISHED, mappedEncounter.getStatus());
    }

    @Test
    void testMapEncounter_WithCernerPowerChartContext() {
        // Arrange
        Encounter encounter = new Encounter();
        encounter.setId("ENC-123");
        encounter.addExtension()
                .setUrl("http://fhir.cerner.com/extension/powerchart-context")
                .setValue(new StringType("INPATIENT"));

        // Act
        Encounter mappedEncounter = mapper.mapEncounter(encounter);

        // Assert
        assertNotNull(mappedEncounter);
        assertTrue(mappedEncounter.hasExtension("http://fhir.cerner.com/extension/powerchart-context"));
    }

    @Test
    void testMapObservation_WithLabResults() {
        // Arrange
        Observation observation = new Observation();
        observation.setId("OBS-123");
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("2093-3")
                .setDisplay("Cholesterol");
        observation.setValue(new Quantity()
                .setValue(200)
                .setUnit("mg/dL"));

        // Act
        Observation mappedObservation = mapper.mapObservation(observation);

        // Assert
        assertNotNull(mappedObservation);
        assertEquals("OBS-123", mappedObservation.getIdElement().getIdPart());
        assertEquals(Observation.ObservationStatus.FINAL, mappedObservation.getStatus());
        assertTrue(mappedObservation.hasValue());
    }

    @Test
    void testMapObservation_WithCernerCodeConsoleMapping() {
        // Arrange
        Observation observation = new Observation();
        observation.setId("OBS-123");
        observation.getCode()
                .addCoding()
                .setSystem("http://fhir.cerner.com/code-console")
                .setCode("CERNER-123")
                .setDisplay("Custom Lab");

        // Act
        Observation mappedObservation = mapper.mapObservation(observation);

        // Assert
        assertNotNull(mappedObservation);
        assertTrue(mappedObservation.getCode().hasCoding());
        assertEquals("http://fhir.cerner.com/code-console", 
                mappedObservation.getCode().getCodingFirstRep().getSystem());
    }

    @Test
    void testMapCondition_WithBasicFields() {
        // Arrange
        Condition condition = new Condition();
        condition.setId("COND-123");
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")));
        condition.getCode()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("44054006")
                .setDisplay("Diabetes");

        // Act
        Condition mappedCondition = mapper.mapCondition(condition);

        // Assert
        assertNotNull(mappedCondition);
        assertEquals("COND-123", mappedCondition.getIdElement().getIdPart());
        assertTrue(mappedCondition.hasClinicalStatus());
    }

    @Test
    void testMapMedicationRequest_WithBasicFields() {
        // Arrange
        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId("MED-123");
        medRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        medRequest.setMedication(new CodeableConcept()
                .addCoding()
                .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .setCode("197361")
                .setDisplay("Metformin"));

        // Act
        MedicationRequest mappedRequest = mapper.mapMedicationRequest(medRequest);

        // Assert
        assertNotNull(mappedRequest);
        assertEquals("MED-123", mappedRequest.getIdElement().getIdPart());
        assertEquals(MedicationRequest.MedicationRequestStatus.ACTIVE, mappedRequest.getStatus());
    }

    @Test
    void testMapImmunization_WithBasicFields() {
        // Arrange
        Immunization immunization = new Immunization();
        immunization.setId("IMM-123");
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        immunization.getVaccineCode()
                .addCoding()
                .setSystem("http://hl7.org/fhir/sid/cvx")
                .setCode("03")
                .setDisplay("MMR");

        // Act
        Immunization mappedImmunization = mapper.mapImmunization(immunization);

        // Assert
        assertNotNull(mappedImmunization);
        assertEquals("IMM-123", mappedImmunization.getIdElement().getIdPart());
        assertEquals(Immunization.ImmunizationStatus.COMPLETED, mappedImmunization.getStatus());
    }

    @Test
    void testMapDiagnosticReport_WithBasicFields() {
        // Arrange
        DiagnosticReport report = new DiagnosticReport();
        report.setId("DIAG-123");
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        report.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("58410-2")
                .setDisplay("CBC panel");

        // Act
        DiagnosticReport mappedReport = mapper.mapDiagnosticReport(report);

        // Assert
        assertNotNull(mappedReport);
        assertEquals("DIAG-123", mappedReport.getIdElement().getIdPart());
        assertEquals(DiagnosticReport.DiagnosticReportStatus.FINAL, mappedReport.getStatus());
    }

    @Test
    void testExtractCernerExtension_ReturnsValue() {
        // Arrange
        Patient patient = new Patient();
        patient.addExtension()
                .setUrl("http://fhir.cerner.com/extension/custom")
                .setValue(new StringType("test-value"));

        // Act
        String value = mapper.extractCernerExtension(patient, "http://fhir.cerner.com/extension/custom");

        // Assert
        assertEquals("test-value", value);
    }

    @Test
    void testExtractCernerExtension_ReturnsNullWhenNotFound() {
        // Arrange
        Patient patient = new Patient();

        // Act
        String value = mapper.extractCernerExtension(patient, "http://fhir.cerner.com/extension/nonexistent");

        // Assert
        assertNull(value);
    }

    @Test
    void testNormalizeCoding_MapsCernerCodeToStandard() {
        // Arrange
        CodeableConcept concept = new CodeableConcept()
                .addCoding()
                .setSystem("http://fhir.cerner.com/code-console")
                .setCode("CERNER-LAB-123")
                .setDisplay("Lab Test");

        // Act
        CodeableConcept normalized = mapper.normalizeCoding(concept);

        // Assert
        assertNotNull(normalized);
        assertTrue(normalized.hasCoding());
    }

    @Test
    void testMapPatient_HandlesNullInput() {
        // Act
        Patient result = mapper.mapPatient(null);

        // Assert
        assertNull(result);
    }
}
