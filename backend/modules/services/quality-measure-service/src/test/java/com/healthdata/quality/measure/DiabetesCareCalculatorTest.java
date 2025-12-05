package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for Diabetes Care (CDC) measure calculator.
 * Converted from JavaScript Jest tests in diabetes-care.test.js
 */
@DisplayName("HEDIS Measure: Comprehensive Diabetes Care (CDC)")
class DiabetesCareCalculatorTest {

    private DiabetesCareCalculator calculator;
    private PatientData mockPatientData;

    @BeforeEach
    void setUp() {
        calculator = new DiabetesCareCalculator();

        // Create mock patient
        Patient patient = new Patient();
        patient.setId("test-patient-1");
        patient.setBirthDate(Date.from(
            LocalDate.of(1960, 1, 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        ));
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);

        mockPatientData = PatientData.builder()
            .patient(patient)
            .build();
    }

    @Test
    @DisplayName("Should identify eligible population with diabetes diagnosis")
    void shouldIdentifyEligiblePopulation() {
        // Add Type 2 diabetes diagnosis
        Condition diabetesCondition = new Condition();
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode("44054006");
        coding.setDisplay("Type 2 diabetes mellitus");
        code.addCoding(coding);
        diabetesCondition.setCode(code);

        CodeableConcept clinicalStatus = new CodeableConcept();
        Coding statusCoding = new Coding();
        statusCoding.setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical");
        statusCoding.setCode("active");
        clinicalStatus.addCoding(statusCoding);
        diabetesCondition.setClinicalStatus(clinicalStatus);

        mockPatientData.getConditions().add(diabetesCondition);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        assertTrue(result.isEligible(), "Patient should be eligible");
        assertTrue(result.isDenominatorMembership(), "Patient should be in denominator");
        assertEquals("CDC", result.getMeasureId());
        assertEquals("Comprehensive Diabetes Care", result.getMeasureName());
    }

    @Test
    @DisplayName("Should calculate HbA1c control < 8%")
    void shouldCalculateHbA1cControl() {
        // Add diabetes diagnosis
        addDiabetesDiagnosis(mockPatientData);

        // Add HbA1c observation with good control
        Observation hba1c = new Observation();
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://loinc.org");
        coding.setCode("4548-4");
        coding.setDisplay("Hemoglobin A1c");
        code.addCoding(coding);
        hba1c.setCode(code);

        Quantity value = new Quantity();
        value.setValue(7.2);
        value.setUnit("%");
        hba1c.setValue(value);

        DateTimeType effectiveDateTime = new DateTimeType(new Date());
        hba1c.setEffective(effectiveDateTime);

        mockPatientData.getObservations().add(hba1c);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        assertTrue(result.isEligible());
        SubMeasureResult control8 = result.getSubMeasures().get("HbA1c < 8%");
        assertNotNull(control8, "HbA1c < 8% sub-measure should exist");
        assertTrue(control8.isNumeratorMembership(), "Patient should have HbA1c < 8%");
        assertEquals("7.2%", control8.getValue());
    }

    @Test
    @DisplayName("Should detect poor HbA1c control > 9%")
    void shouldDetectPoorHbA1cControl() {
        // Add diabetes diagnosis
        addDiabetesDiagnosis(mockPatientData);

        // Add HbA1c observation with poor control
        Observation hba1c = createHbA1cObservation(9.5);
        mockPatientData.getObservations().add(hba1c);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        SubMeasureResult poorControl = result.getSubMeasures().get("HbA1c > 9%");
        assertNotNull(poorControl);
        assertTrue(poorControl.isNumeratorMembership(), "Patient should have HbA1c > 9%");

        // Should have high-priority recommendations
        assertTrue(result.getRecommendations().stream()
            .anyMatch(r -> "high".equals(r.getPriority()) &&
                          r.getAction().contains("intensive medication")));
    }

    @Test
    @DisplayName("Should detect care gaps when tests are missing")
    void shouldDetectCareGaps() {
        // Add diabetes but no recent tests
        addDiabetesDiagnosis(mockPatientData);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        assertEquals(4, result.getCareGaps().size(),
            "Should have 4 care gaps: HbA1c, eye exam, nephropathy, BP");

        // Verify HbA1c gap
        CareGap hba1cGap = result.getCareGaps().stream()
            .filter(g -> "missing-hba1c-test".equals(g.getType()))
            .findFirst()
            .orElse(null);

        assertNotNull(hba1cGap, "Should have missing HbA1c test gap");
        assertEquals("high", hba1cGap.getSeverity());
        assertTrue(hba1cGap.getAction().contains("Schedule HbA1c test"));
    }

    @Test
    @DisplayName("Should exclude patients in hospice care")
    void shouldExcludeHospicePatients() {
        // Add diabetes diagnosis
        addDiabetesDiagnosis(mockPatientData);

        // Add hospice encounter
        Encounter hospiceEncounter = new Encounter();
        CodeableConcept type = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode("305336008");
        coding.setDisplay("Hospice care");
        type.addCoding(coding);
        hospiceEncounter.addType(type);

        mockPatientData.getEncounters().add(hospiceEncounter);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        assertFalse(result.isEligible(), "Patient should not be eligible");
        assertTrue(result.isDenominatorExclusion(), "Patient should be excluded");
        assertEquals("Patient in hospice care", result.getExclusionReason());
    }

    @Test
    @DisplayName("Should exclude patients with gestational diabetes only")
    void shouldExcludeGestationalDiabetesOnly() {
        // Add only gestational diabetes
        Condition gestationalDiabetes = new Condition();
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode("11687002");
        coding.setDisplay("Gestational diabetes mellitus");
        code.addCoding(coding);
        gestationalDiabetes.setCode(code);

        mockPatientData.getConditions().add(gestationalDiabetes);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        assertFalse(result.isEligible());
        assertEquals("Gestational diabetes only", result.getExclusionReason());
    }

    @Test
    @DisplayName("Should calculate eye exam compliance")
    void shouldCalculateEyeExamCompliance() {
        // Add diabetes diagnosis
        addDiabetesDiagnosis(mockPatientData);

        // Add diabetic retinopathy screening procedure
        Procedure eyeExam = new Procedure();
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode("252779009");
        coding.setDisplay("Diabetic retinopathy screening");
        code.addCoding(coding);
        eyeExam.setCode(code);

        DateTimeType performedDateTime = new DateTimeType(new Date());
        eyeExam.setPerformed(performedDateTime);

        mockPatientData.getProcedures().add(eyeExam);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        SubMeasureResult eyeExamResult = result.getSubMeasures().get("Eye Exam");
        assertNotNull(eyeExamResult);
        assertTrue(eyeExamResult.isNumeratorMembership(), "Patient should have eye exam");
        assertEquals("procedure", eyeExamResult.getMethod());
    }

    @Test
    @DisplayName("Should calculate blood pressure control")
    void shouldCalculateBPControl() {
        // Add diabetes diagnosis
        addDiabetesDiagnosis(mockPatientData);

        // Add controlled blood pressure reading
        Observation bp = createBPObservation(130, 80);
        mockPatientData.getObservations().add(bp);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        SubMeasureResult bpControl = result.getSubMeasures().get("BP Control < 140/90");
        assertNotNull(bpControl);
        assertTrue(bpControl.isNumeratorMembership(), "BP should be controlled");
        assertEquals("130/80", bpControl.getValue());
    }

    @Test
    @DisplayName("Should detect uncontrolled blood pressure")
    void shouldDetectUncontrolledBP() {
        // Add diabetes diagnosis
        addDiabetesDiagnosis(mockPatientData);

        // Add uncontrolled blood pressure
        Observation bp = createBPObservation(150, 95);
        mockPatientData.getObservations().add(bp);

        // Calculate measure
        MeasureResult result = calculator.calculate(mockPatientData);

        // Assertions
        SubMeasureResult bpControl = result.getSubMeasures().get("BP Control < 140/90");
        assertNotNull(bpControl);
        assertFalse(bpControl.isNumeratorMembership(), "BP should NOT be controlled");

        // Should have care gap for uncontrolled BP
        assertTrue(result.getCareGaps().stream()
            .anyMatch(g -> "uncontrolled-blood-pressure".equals(g.getType())));
    }

    // ========== Helper Methods ==========

    private void addDiabetesDiagnosis(PatientData patientData) {
        Condition diabetes = new Condition();
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode("44054006");
        coding.setDisplay("Type 2 diabetes mellitus");
        code.addCoding(coding);
        diabetes.setCode(code);

        patientData.getConditions().add(diabetes);
    }

    private Observation createHbA1cObservation(double value) {
        Observation obs = new Observation();

        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://loinc.org");
        coding.setCode("4548-4");
        coding.setDisplay("Hemoglobin A1c");
        code.addCoding(coding);
        obs.setCode(code);

        Quantity quantity = new Quantity();
        quantity.setValue(value);
        quantity.setUnit("%");
        obs.setValue(quantity);

        DateTimeType effectiveDateTime = new DateTimeType(new Date());
        obs.setEffective(effectiveDateTime);

        return obs;
    }

    private Observation createBPObservation(double systolic, double diastolic) {
        Observation obs = new Observation();

        // BP Panel code
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://loinc.org");
        coding.setCode("85354-9");
        coding.setDisplay("Blood pressure panel");
        code.addCoding(coding);
        obs.setCode(code);

        // Systolic component
        Observation.ObservationComponentComponent systolicComp = new Observation.ObservationComponentComponent();
        CodeableConcept systolicCode = new CodeableConcept();
        Coding systolicCoding = new Coding();
        systolicCoding.setSystem("http://loinc.org");
        systolicCoding.setCode("8480-6");
        systolicCoding.setDisplay("Systolic blood pressure");
        systolicCode.addCoding(systolicCoding);
        systolicComp.setCode(systolicCode);

        Quantity systolicValue = new Quantity();
        systolicValue.setValue(systolic);
        systolicValue.setUnit("mmHg");
        systolicComp.setValue(systolicValue);

        obs.addComponent(systolicComp);

        // Diastolic component
        Observation.ObservationComponentComponent diastolicComp = new Observation.ObservationComponentComponent();
        CodeableConcept diastolicCode = new CodeableConcept();
        Coding diastolicCoding = new Coding();
        diastolicCoding.setSystem("http://loinc.org");
        diastolicCoding.setCode("8462-4");
        diastolicCoding.setDisplay("Diastolic blood pressure");
        diastolicCode.addCoding(diastolicCoding);
        diastolicComp.setCode(diastolicCode);

        Quantity diastolicValue = new Quantity();
        diastolicValue.setValue(diastolic);
        diastolicValue.setUnit("mmHg");
        diastolicComp.setValue(diastolicValue);

        obs.addComponent(diastolicComp);

        DateTimeType effectiveDateTime = new DateTimeType(new Date());
        obs.setEffective(effectiveDateTime);

        return obs;
    }
}
