package com.healthdata.ehr.connector.epic;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EpicDataMapperTest {

    private EpicDataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EpicDataMapper();
    }

    @Test
    void testMapPatient_BasicPatient() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("epic-patient-123");
        patient.addName()
                .setFamily("Smith")
                .addGiven("John")
                .addGiven("Michael");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(new Date());

        // Act
        Map<String, Object> result = mapper.mapPatient(patient);

        // Assert
        assertNotNull(result);
        assertEquals("epic-patient-123", result.get("id"));
        assertEquals("Smith", result.get("familyName"));
        assertEquals("John Michael", result.get("givenNames"));
        assertEquals("MALE", result.get("gender"));
        assertNotNull(result.get("birthDate"));
    }

    @Test
    void testMapPatient_WithEpicExtensions() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("epic-patient-456");

        // Add Epic-specific extension for legal sex
        Extension legalSexExtension = new Extension(
                "http://open.epic.com/fhir/extensions/legal-sex",
                new CodeType("M")
        );
        patient.addExtension(legalSexExtension);

        // Add Epic-specific extension for patient class
        Extension patientClassExtension = new Extension(
                "http://open.epic.com/fhir/extensions/patient-class",
                new StringType("Inpatient")
        );
        patient.addExtension(patientClassExtension);

        // Act
        Map<String, Object> result = mapper.mapPatient(patient);

        // Assert
        assertNotNull(result);
        assertEquals("M", result.get("epicLegalSex"));
        assertEquals("Inpatient", result.get("epicPatientClass"));
    }

    @Test
    void testMapPatient_WithMRN() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("epic-patient-789");

        Identifier mrn = new Identifier();
        mrn.setSystem("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.0");
        mrn.setValue("E12345");
        mrn.setType(new CodeableConcept().addCoding(
                new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "MR", "Medical Record Number")
        ));
        patient.addIdentifier(mrn);

        // Act
        Map<String, Object> result = mapper.mapPatient(patient);

        // Assert
        assertNotNull(result);
        assertEquals("E12345", result.get("mrn"));
    }

    @Test
    void testMapEncounter_BasicEncounter() {
        // Arrange
        Encounter encounter = new Encounter();
        encounter.setId("epic-encounter-123");
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setClass_(new Coding(
                "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                "IMP",
                "inpatient encounter"
        ));

        // Act
        Map<String, Object> result = mapper.mapEncounter(encounter);

        // Assert
        assertNotNull(result);
        assertEquals("epic-encounter-123", result.get("id"));
        assertEquals("FINISHED", result.get("status"));
        assertEquals("IMP", result.get("encounterClass"));
    }

    @Test
    void testMapEncounter_WithEpicDepartment() {
        // Arrange
        Encounter encounter = new Encounter();
        encounter.setId("epic-encounter-456");

        // Add Epic department extension
        Extension deptExtension = new Extension(
                "http://open.epic.com/fhir/extensions/encounter-department",
                new StringType("Emergency Department")
        );
        encounter.addExtension(deptExtension);

        // Act
        Map<String, Object> result = mapper.mapEncounter(encounter);

        // Assert
        assertNotNull(result);
        assertEquals("Emergency Department", result.get("epicDepartment"));
    }

    @Test
    void testMapEncounter_WithLocation() {
        // Arrange
        Encounter encounter = new Encounter();
        encounter.setId("epic-encounter-789");

        Encounter.EncounterLocationComponent location = new Encounter.EncounterLocationComponent();
        location.setLocation(new Reference("Location/epic-loc-123").setDisplay("Room 301"));
        encounter.addLocation(location);

        // Act
        Map<String, Object> result = mapper.mapEncounter(encounter);

        // Assert
        assertNotNull(result);
        assertEquals("Room 301", result.get("locationDisplay"));
    }

    @Test
    void testMapObservation_VitalSign() {
        // Arrange
        Observation observation = new Observation();
        observation.setId("epic-obs-123");
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
                .addCoding(new Coding("http://loinc.org", "8867-4", "Heart rate"))
        );
        observation.setValue(new Quantity(72).setUnit("beats/min").setSystem("http://unitsofmeasure.org").setCode("/min"));

        // Act
        Map<String, Object> result = mapper.mapObservation(observation);

        // Assert
        assertNotNull(result);
        assertEquals("epic-obs-123", result.get("id"));
        assertEquals("FINAL", result.get("status"));
        assertEquals("8867-4", result.get("loincCode"));
        assertEquals("Heart rate", result.get("display"));
        assertEquals("72 beats/min", result.get("valueString"));
    }

    @Test
    void testMapObservation_LabResult() {
        // Arrange
        Observation observation = new Observation();
        observation.setId("epic-lab-456");
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
                .addCoding(new Coding("http://loinc.org", "2339-0", "Glucose"))
        );
        observation.setValue(new Quantity(95).setUnit("mg/dL").setSystem("http://unitsofmeasure.org").setCode("mg/dL"));

        // Reference range
        Observation.ObservationReferenceRangeComponent range = new Observation.ObservationReferenceRangeComponent();
        range.setLow(new Quantity(70));
        range.setHigh(new Quantity(100));
        observation.addReferenceRange(range);

        // Act
        Map<String, Object> result = mapper.mapObservation(observation);

        // Assert
        assertNotNull(result);
        assertEquals("95 mg/dL", result.get("valueString"));
        assertEquals("70-100", result.get("referenceRange"));
    }

    @Test
    void testMapObservation_WithEpicExtensions() {
        // Arrange
        Observation observation = new Observation();
        observation.setId("epic-obs-789");

        // Epic ordering provider extension
        Extension providerExtension = new Extension(
                "http://open.epic.com/fhir/extensions/ordering-provider",
                new StringType("Dr. Jane Doe")
        );
        observation.addExtension(providerExtension);

        // Act
        Map<String, Object> result = mapper.mapObservation(observation);

        // Assert
        assertNotNull(result);
        assertEquals("Dr. Jane Doe", result.get("epicOrderingProvider"));
    }

    @Test
    void testMapCondition_ActiveCondition() {
        // Arrange
        Condition condition = new Condition();
        condition.setId("epic-cond-123");
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding(
                        "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        "active",
                        "Active"
                ))
        );
        condition.setCode(new CodeableConcept()
                .addCoding(new Coding("http://snomed.info/sct", "38341003", "Hypertension"))
        );

        // Act
        Map<String, Object> result = mapper.mapCondition(condition);

        // Assert
        assertNotNull(result);
        assertEquals("epic-cond-123", result.get("id"));
        assertEquals("active", result.get("clinicalStatus"));
        assertEquals("38341003", result.get("snomedCode"));
        assertEquals("Hypertension", result.get("display"));
    }

    @Test
    void testMapCondition_WithOnsetDate() {
        // Arrange
        Condition condition = new Condition();
        condition.setId("epic-cond-456");
        Date onsetDate = new Date();
        condition.setOnset(new DateTimeType(onsetDate));

        // Act
        Map<String, Object> result = mapper.mapCondition(condition);

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("onsetDate"));
    }

    @Test
    void testExtractExtensions_MultipleEpicExtensions() {
        // Arrange
        Patient patient = new Patient();
        patient.addExtension(new Extension(
                "http://open.epic.com/fhir/extensions/legal-sex",
                new CodeType("F")
        ));
        patient.addExtension(new Extension(
                "http://open.epic.com/fhir/extensions/patient-class",
                new StringType("Outpatient")
        ));
        patient.addExtension(new Extension(
                "http://open.epic.com/fhir/extensions/mychart-status",
                new BooleanType(true)
        ));

        // Act
        Map<String, Object> result = mapper.extractExtensions(patient);

        // Assert
        assertNotNull(result);
        assertEquals("F", result.get("legal-sex"));
        assertEquals("Outpatient", result.get("patient-class"));
        assertEquals(true, result.get("mychart-status"));
    }

    @Test
    void testExtractExtensions_NoExtensions() {
        // Arrange
        Patient patient = new Patient();

        // Act
        Map<String, Object> result = mapper.extractExtensions(patient);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMapPatient_WithMultipleIdentifiers() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("epic-patient-999");

        patient.addIdentifier(new Identifier()
                .setSystem("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.0")
                .setValue("E99999")
                .setType(new CodeableConcept().addCoding(
                        new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "MR", "Medical Record Number")
                ))
        );

        patient.addIdentifier(new Identifier()
                .setSystem("http://hl7.org/fhir/sid/us-ssn")
                .setValue("123-45-6789")
        );

        // Act
        Map<String, Object> result = mapper.mapPatient(patient);

        // Assert
        assertNotNull(result);
        assertEquals("E99999", result.get("mrn"));
        assertNotNull(result.get("identifiers"));
    }

    @Test
    void testMapObservation_WithInterpretation() {
        // Arrange
        Observation observation = new Observation();
        observation.setId("epic-obs-interp");
        observation.addInterpretation(new CodeableConcept()
                .addCoding(new Coding(
                        "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
                        "H",
                        "High"
                ))
        );

        // Act
        Map<String, Object> result = mapper.mapObservation(observation);

        // Assert
        assertNotNull(result);
        assertEquals("H", result.get("interpretation"));
    }

    @Test
    void testMapCondition_WithEpicProblemListExtension() {
        // Arrange
        Condition condition = new Condition();
        condition.setId("epic-cond-prob");

        Extension problemListExtension = new Extension(
                "http://open.epic.com/fhir/extensions/problem-list-status",
                new StringType("Active Problem")
        );
        condition.addExtension(problemListExtension);

        // Act
        Map<String, Object> result = mapper.mapCondition(condition);

        // Assert
        assertNotNull(result);
        assertEquals("Active Problem", result.get("epicProblemListStatus"));
    }
}
