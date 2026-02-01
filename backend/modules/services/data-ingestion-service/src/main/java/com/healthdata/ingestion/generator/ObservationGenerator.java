package com.healthdata.ingestion.generator;

import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generates synthetic observation data (vital signs, lab results) for demo patients.
 *
 * Observations include:
 * - Vital Signs: Blood pressure, heart rate, weight, height, BMI
 * - Lab Results: A1C (diabetics), Creatinine (CKD), Lipid panel
 *
 * Values are generated based on patient conditions to be clinically realistic.
 */
@Component
public class ObservationGenerator {

    private final Random random = new Random();

    /**
     * Generate vital signs and lab observations for a patient.
     *
     * @param patient The patient resource
     * @param conditions List of condition ICD-10 codes
     * @param riskCategory Patient risk category (affects values)
     * @param bundle Bundle to add observation resources to
     */
    public void generateObservations(Patient patient, List<String> conditions,
                                      String riskCategory, Bundle bundle) {
        String patientRef = "Patient/" + patient.getId();

        // Always generate vital signs
        generateVitalSigns(patientRef, conditions, riskCategory, bundle);

        // Generate condition-specific labs
        for (String conditionCode : conditions) {
            generateConditionSpecificLabs(patientRef, conditionCode, riskCategory, bundle);
        }
    }

    /**
     * Generate vital sign observations.
     */
    private void generateVitalSigns(String patientRef, List<String> conditions,
                                     String riskCategory, Bundle bundle) {
        Date observationDate = getRecentDate(30);

        // Blood Pressure
        int[] bp = generateBloodPressure(conditions, riskCategory);
        bundle.addEntry().setResource(createBloodPressureObservation(patientRef, bp[0], bp[1], observationDate));

        // Heart Rate
        int heartRate = generateHeartRate(conditions);
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "8867-4", "Heart rate", heartRate, "/min", observationDate
        ));

        // Weight (kg)
        double weight = generateWeight();
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "29463-7", "Body Weight", weight, "kg", observationDate
        ));

        // Height (cm)
        double height = generateHeight();
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "8302-2", "Body Height", height, "cm", observationDate
        ));

        // BMI (calculated)
        double heightMeters = height / 100.0;
        double bmi = weight / (heightMeters * heightMeters);
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "39156-5", "Body Mass Index", round(bmi, 1), "kg/m2", observationDate
        ));
    }

    /**
     * Generate condition-specific lab values.
     */
    private void generateConditionSpecificLabs(String patientRef, String conditionCode,
                                                String riskCategory, Bundle bundle) {
        Date labDate = getRecentDate(90);

        switch (conditionCode) {
            case "E11.9": // Diabetes - A1C
                double a1c = generateA1C(riskCategory);
                bundle.addEntry().setResource(createSimpleObservation(
                    patientRef, "4548-4", "Hemoglobin A1c", a1c, "%", labDate
                ));

                // Fasting glucose
                double glucose = generateFastingGlucose(a1c);
                bundle.addEntry().setResource(createSimpleObservation(
                    patientRef, "1558-6", "Fasting Glucose", glucose, "mg/dL", labDate
                ));
                break;

            case "N18.3": // CKD - Creatinine, eGFR
                double creatinine = generateCreatinine(riskCategory);
                bundle.addEntry().setResource(createSimpleObservation(
                    patientRef, "2160-0", "Creatinine", creatinine, "mg/dL", labDate
                ));

                // eGFR (estimated based on creatinine)
                double egfr = estimateGFR(creatinine);
                bundle.addEntry().setResource(createSimpleObservation(
                    patientRef, "33914-3", "eGFR", round(egfr, 0), "mL/min/1.73m2", labDate
                ));
                break;

            case "I10": // Hypertension - Lipid panel
            case "I50.9": // CHF
                generateLipidPanel(patientRef, riskCategory, bundle, labDate);
                break;
        }
    }

    /**
     * Generate lipid panel observations.
     */
    private void generateLipidPanel(String patientRef, String riskCategory,
                                     Bundle bundle, Date labDate) {
        // Total Cholesterol
        int totalCholesterol = generateCholesterol(riskCategory);
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "2093-3", "Total Cholesterol", totalCholesterol, "mg/dL", labDate
        ));

        // LDL
        int ldl = (int) (totalCholesterol * (0.5 + random.nextDouble() * 0.2));
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "2089-1", "LDL Cholesterol", ldl, "mg/dL", labDate
        ));

        // HDL
        int hdl = 30 + random.nextInt(40);
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "2085-9", "HDL Cholesterol", hdl, "mg/dL", labDate
        ));

        // Triglycerides
        int triglycerides = 100 + random.nextInt(200);
        bundle.addEntry().setResource(createSimpleObservation(
            patientRef, "2571-8", "Triglycerides", triglycerides, "mg/dL", labDate
        ));
    }

    /**
     * Create a blood pressure observation (component-based).
     */
    private Observation createBloodPressureObservation(String patientRef, int systolic,
                                                        int diastolic, Date date) {
        Observation obs = new Observation();
        obs.setId(UUID.randomUUID().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        // Category: vital-signs
        CodeableConcept category = new CodeableConcept();
        category.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode("vital-signs")
            .setDisplay("Vital Signs");
        obs.addCategory(category);

        // Code: Blood pressure
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
            .setSystem("http://loinc.org")
            .setCode("85354-9")
            .setDisplay("Blood pressure panel");
        obs.setCode(code);

        obs.setSubject(new Reference(patientRef));
        obs.setEffective(new DateTimeType(date));

        // Systolic component
        Observation.ObservationComponentComponent systolicComp = obs.addComponent();
        CodeableConcept systolicCode = new CodeableConcept();
        systolicCode.addCoding()
            .setSystem("http://loinc.org")
            .setCode("8480-6")
            .setDisplay("Systolic blood pressure");
        systolicComp.setCode(systolicCode);
        Quantity systolicValue = new Quantity();
        systolicValue.setValue(systolic);
        systolicValue.setUnit("mmHg");
        systolicValue.setSystem("http://unitsofmeasure.org");
        systolicValue.setCode("mm[Hg]");
        systolicComp.setValue(systolicValue);

        // Diastolic component
        Observation.ObservationComponentComponent diastolicComp = obs.addComponent();
        CodeableConcept diastolicCode = new CodeableConcept();
        diastolicCode.addCoding()
            .setSystem("http://loinc.org")
            .setCode("8462-4")
            .setDisplay("Diastolic blood pressure");
        diastolicComp.setCode(diastolicCode);
        Quantity diastolicValue = new Quantity();
        diastolicValue.setValue(diastolic);
        diastolicValue.setUnit("mmHg");
        diastolicValue.setSystem("http://unitsofmeasure.org");
        diastolicValue.setCode("mm[Hg]");
        diastolicComp.setValue(diastolicValue);

        return obs;
    }

    /**
     * Create a simple numeric observation.
     */
    private Observation createSimpleObservation(String patientRef, String loincCode,
                                                 String display, double value,
                                                 String unit, Date date) {
        Observation obs = new Observation();
        obs.setId(UUID.randomUUID().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        // Category
        CodeableConcept category = new CodeableConcept();
        String categoryCode = loincCode.startsWith("8") ? "vital-signs" : "laboratory";
        category.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode(categoryCode);
        obs.addCategory(category);

        // Code
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
            .setSystem("http://loinc.org")
            .setCode(loincCode)
            .setDisplay(display);
        obs.setCode(code);

        obs.setSubject(new Reference(patientRef));
        obs.setEffective(new DateTimeType(date));

        // Value
        Quantity quantity = new Quantity();
        quantity.setValue(value);
        quantity.setUnit(unit);
        obs.setValue(quantity);

        return obs;
    }

    // Value generation methods

    private int[] generateBloodPressure(List<String> conditions, String riskCategory) {
        int baseSystolic = 120;
        int baseDiastolic = 80;

        // Adjust for hypertension
        if (conditions.contains("I10")) {
            baseSystolic += 20 + random.nextInt(30);
            baseDiastolic += 10 + random.nextInt(20);
        }

        // Adjust for risk
        if ("HIGH".equals(riskCategory)) {
            baseSystolic += 10;
            baseDiastolic += 5;
        }

        // Add some variance
        int systolic = baseSystolic + random.nextInt(20) - 10;
        int diastolic = baseDiastolic + random.nextInt(10) - 5;

        return new int[]{
            Math.max(90, Math.min(200, systolic)),
            Math.max(60, Math.min(120, diastolic))
        };
    }

    private int generateHeartRate(List<String> conditions) {
        int baseRate = 72;
        if (conditions.contains("I50.9")) { // CHF
            baseRate += 10;
        }
        return baseRate + random.nextInt(20) - 10;
    }

    private double generateWeight() {
        // Weight in kg: 50-130 kg range
        return 60 + random.nextDouble() * 60;
    }

    private double generateHeight() {
        // Height in cm: 150-190 cm range
        return 155 + random.nextDouble() * 35;
    }

    private double generateA1C(String riskCategory) {
        // A1C ranges:
        // Normal: < 5.7%
        // Prediabetes: 5.7-6.4%
        // Diabetes: >= 6.5%
        // Poor control: > 9%

        double baseA1C = switch (riskCategory) {
            case "LOW" -> 6.5 + random.nextDouble() * 1.5; // 6.5-8.0
            case "MODERATE" -> 7.0 + random.nextDouble() * 2.0; // 7.0-9.0
            case "HIGH" -> 8.0 + random.nextDouble() * 2.5; // 8.0-10.5
            default -> 7.0 + random.nextDouble() * 1.5;
        };

        return round(baseA1C, 1);
    }

    private double generateFastingGlucose(double a1c) {
        // Rough correlation: glucose = (a1c - 2) * 30
        double baseGlucose = (a1c - 2) * 30;
        return round(baseGlucose + random.nextDouble() * 30 - 15, 0);
    }

    private double generateCreatinine(String riskCategory) {
        // CKD Stage 3: eGFR 30-59, creatinine typically 1.5-3.0
        double baseCreatinine = switch (riskCategory) {
            case "LOW" -> 1.2 + random.nextDouble() * 0.5;
            case "MODERATE" -> 1.5 + random.nextDouble() * 0.8;
            case "HIGH" -> 2.0 + random.nextDouble() * 1.0;
            default -> 1.5 + random.nextDouble() * 0.5;
        };

        return round(baseCreatinine, 2);
    }

    private double estimateGFR(double creatinine) {
        // Simplified eGFR estimation
        // Real formula is more complex (CKD-EPI or MDRD)
        return 120 / creatinine;
    }

    private int generateCholesterol(String riskCategory) {
        int baseCholesterol = switch (riskCategory) {
            case "LOW" -> 180 + random.nextInt(40);
            case "MODERATE" -> 200 + random.nextInt(50);
            case "HIGH" -> 220 + random.nextInt(60);
            default -> 190 + random.nextInt(40);
        };
        return baseCholesterol;
    }

    private Date getRecentDate(int maxDaysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(maxDaysAgo));
        return cal.getTime();
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
