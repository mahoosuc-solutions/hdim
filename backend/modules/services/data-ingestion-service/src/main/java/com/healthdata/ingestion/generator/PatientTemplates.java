package com.healthdata.ingestion.generator;

import com.healthdata.demo.domain.model.SyntheticPatientTemplate;
import com.healthdata.demo.domain.model.SyntheticPatientTemplate.Gender;
import com.healthdata.demo.domain.model.SyntheticPatientTemplate.RiskCategory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Pre-defined patient templates (personas) for consistent demo experiences.
 *
 * These personas are used in demo scripts and provide relatable,
 * recognizable examples for sales demonstrations.
 *
 * Personas:
 * 1. Michael Chen - Complex diabetic (HEDIS evaluation focus)
 * 2. Sarah Martinez - Preventive care gap (BCS demo)
 * 3. Emma Johnson - High-risk multi-morbid (Risk stratification demo)
 * 4. Carlos Rodriguez - SDOH barriers (Patient journey demo)
 */
public class PatientTemplates {

    /**
     * Michael Chen - Complex Diabetic Patient
     *
     * Demo Focus: HEDIS evaluation, care gap identification
     * Risk Score: 2.4 (moderate-high)
     *
     * Clinical Profile:
     * - Type 2 Diabetes with rising A1C (7.2% -> 8.1%)
     * - Hypertension (BP 145/92)
     * - Multiple care gaps: CDC-E, EED, SPC
     * - On 5 medications
     * - Trending toward poor control
     */
    public static SyntheticPatientTemplate createMichaelChen() {
        SyntheticPatientTemplate template = new SyntheticPatientTemplate(
            "complex-diabetic",
            "Michael",
            "Chen",
            58,
            Gender.MALE,
            new BigDecimal("2.4"),
            RiskCategory.MODERATE
        );

        template.setDescription(
            "Complex diabetic patient with rising A1C and multiple care gaps. " +
            "Ideal for demonstrating HEDIS evaluation and care gap prioritization."
        );

        template.setConditions(toJson(Arrays.asList(
            condition("E11.9", "Type 2 Diabetes Mellitus"),
            condition("I10", "Essential Hypertension"),
            condition("E78.0", "Hyperlipidemia")
        )));

        template.setMedications(toJson(Arrays.asList(
            medication("860974", "Metformin 500 MG", "Take 1 tablet twice daily"),
            medication("310539", "Glipizide 5 MG", "Take 1 tablet daily"),
            medication("314076", "Lisinopril 20 MG", "Take 1 tablet daily"),
            medication("617311", "Atorvastatin 20 MG", "Take 1 tablet at bedtime"),
            medication("197361", "Amlodipine 5 MG", "Take 1 tablet daily")
        )));

        template.setCareGaps(toJson(Arrays.asList(
            careGap("CDC-E", "Diabetes HbA1c Control", "A1C above 8% target"),
            careGap("EED", "Eye Exam for Diabetics", "No eye exam in 14 months"),
            careGap("SPC", "Statin Therapy", "Statin therapy adherence gap")
        )));

        template.setObservations(toJson(Arrays.asList(
            observation("4548-4", "Hemoglobin A1c", "8.1", "%"),
            observation("85354-9", "Blood pressure", "145/92", "mmHg"),
            observation("2093-3", "Total Cholesterol", "218", "mg/dL"),
            observation("2089-1", "LDL Cholesterol", "142", "mg/dL"),
            observation("29463-7", "Body Weight", "98", "kg"),
            observation("39156-5", "BMI", "31.2", "kg/m2")
        )));

        template.setScenarioNotes(
            "Use for HEDIS evaluation demo. Show how evaluation identifies " +
            "multiple care gaps and prioritizes based on clinical urgency. " +
            "A1C trending up is key talking point for intervention importance."
        );

        return template;
    }

    /**
     * Sarah Martinez - Preventive Care Gap Patient
     *
     * Demo Focus: Breast Cancer Screening (BCS) measure
     * Risk Score: 1.8 (low-moderate)
     *
     * Clinical Profile:
     * - Generally healthy 52-year-old woman
     * - Overdue for mammogram by 8 months
     * - Last preventive visit was 14 months ago
     * - Perfect candidate for outreach
     */
    public static SyntheticPatientTemplate createSarahMartinez() {
        SyntheticPatientTemplate template = new SyntheticPatientTemplate(
            "preventive-gap",
            "Sarah",
            "Martinez",
            52,
            Gender.FEMALE,
            new BigDecimal("1.8"),
            RiskCategory.LOW
        );

        template.setDescription(
            "Otherwise healthy woman overdue for breast cancer screening. " +
            "Ideal for demonstrating BCS measure evaluation and member outreach."
        );

        template.setConditions(toJson(Arrays.asList())); // No chronic conditions

        template.setMedications(toJson(Arrays.asList(
            medication("311989", "Vitamin D 1000 IU", "Take 1 tablet daily")
        )));

        template.setCareGaps(toJson(Arrays.asList(
            careGap("BCS", "Breast Cancer Screening", "Mammogram overdue by 8 months")
        )));

        template.setObservations(toJson(Arrays.asList(
            observation("85354-9", "Blood pressure", "118/76", "mmHg"),
            observation("29463-7", "Body Weight", "68", "kg"),
            observation("39156-5", "BMI", "24.1", "kg/m2"),
            observation("2093-3", "Total Cholesterol", "195", "mg/dL")
        )));

        template.setEncounters(toJson(Arrays.asList(
            encounter("390906007", "Office visit", "14 months ago", "completed"),
            encounter("390906007", "Preventive visit", "6 months ago", "cancelled")
        )));

        template.setScenarioNotes(
            "Featured patient in HEDIS demo script. Show drill-down from " +
            "care gap list to patient detail. Highlight cancelled preventive " +
            "visit as opportunity for re-engagement."
        );

        return template;
    }

    /**
     * Emma Johnson - High-Risk Multi-Morbid Patient
     *
     * Demo Focus: Risk stratification, predictive analytics
     * Risk Score: 3.7 (high)
     *
     * Clinical Profile:
     * - CHF, COPD, CKD Stage 3, Diabetes
     * - 6 active medications
     * - Recent ER visit
     * - 78% predicted admission risk
     */
    public static SyntheticPatientTemplate createEmmaJohnson() {
        SyntheticPatientTemplate template = new SyntheticPatientTemplate(
            "high-risk-multimorbid",
            "Emma",
            "Johnson",
            71,
            Gender.FEMALE,
            new BigDecimal("3.7"),
            RiskCategory.HIGH
        );

        template.setDescription(
            "High-risk patient with multiple chronic conditions. " +
            "Ideal for demonstrating risk stratification and care management prioritization."
        );

        template.setConditions(toJson(Arrays.asList(
            condition("I50.9", "Congestive Heart Failure"),
            condition("J44.9", "Chronic Obstructive Pulmonary Disease"),
            condition("N18.3", "Chronic Kidney Disease, Stage 3"),
            condition("E11.9", "Type 2 Diabetes Mellitus"),
            condition("I10", "Essential Hypertension")
        )));

        template.setMedications(toJson(Arrays.asList(
            medication("310429", "Furosemide 40 MG", "Take 1 tablet daily"),
            medication("200031", "Carvedilol 12.5 MG", "Take 1 tablet twice daily"),
            medication("313096", "Spironolactone 25 MG", "Take 1 tablet daily"),
            medication("746763", "Albuterol Inhaler", "2 puffs every 4-6 hours as needed"),
            medication("860974", "Metformin 500 MG", "Take 1 tablet twice daily"),
            medication("314076", "Lisinopril 10 MG", "Take 1 tablet daily")
        )));

        template.setCareGaps(toJson(Arrays.asList(
            careGap("CDC-E", "Diabetes HbA1c Control", "A1C 8.4% - above target"),
            careGap("COL", "Colorectal Cancer Screening", "Colonoscopy overdue"),
            careGap("CBP", "Blood Pressure Control", "BP not at goal")
        )));

        template.setObservations(toJson(Arrays.asList(
            observation("4548-4", "Hemoglobin A1c", "8.4", "%"),
            observation("85354-9", "Blood pressure", "158/94", "mmHg"),
            observation("2160-0", "Creatinine", "2.1", "mg/dL"),
            observation("33914-3", "eGFR", "38", "mL/min/1.73m2"),
            observation("29463-7", "Body Weight", "82", "kg"),
            observation("6299-2", "BNP", "450", "pg/mL")
        )));

        template.setEncounters(toJson(Arrays.asList(
            encounter("4525004", "Emergency department visit", "3 weeks ago", "completed"),
            encounter("390906007", "Office visit", "2 months ago", "completed"),
            encounter("32485007", "Hospital admission", "4 months ago", "completed")
        )));

        template.setScenarioNotes(
            "Use for risk stratification demo. Show 78% predicted admission " +
            "risk based on HCC score, recent ER visit, and multiple comorbidities. " +
            "Emphasize cost avoidance potential through proactive care management."
        );

        return template;
    }

    /**
     * Carlos Rodriguez - SDOH Barriers Patient
     *
     * Demo Focus: Social determinants, patient journey
     * Risk Score: 2.1 (moderate)
     *
     * Clinical Profile:
     * - Diabetes with medication non-adherence
     * - Food insecurity (Z59.4)
     * - Housing instability (Z59.0)
     * - Transportation barriers
     */
    public static SyntheticPatientTemplate createCarlosRodriguez() {
        SyntheticPatientTemplate template = new SyntheticPatientTemplate(
            "sdoh-barriers",
            "Carlos",
            "Rodriguez",
            45,
            Gender.MALE,
            new BigDecimal("2.1"),
            RiskCategory.MODERATE
        );

        template.setDescription(
            "Patient with social determinants affecting care. " +
            "Ideal for demonstrating SDOH screening and holistic patient journey."
        );

        template.setConditions(toJson(Arrays.asList(
            condition("E11.9", "Type 2 Diabetes Mellitus"),
            condition("Z59.4", "Lack of adequate food"),
            condition("Z59.0", "Homelessness"),
            condition("F32.9", "Major depressive disorder")
        )));

        template.setMedications(toJson(Arrays.asList(
            medication("860974", "Metformin 500 MG", "Take 1 tablet twice daily")
        )));

        template.setCareGaps(toJson(Arrays.asList(
            careGap("CDC-E", "Diabetes HbA1c Control", "A1C 9.2% - poor control"),
            careGap("EED", "Eye Exam for Diabetics", "No eye exam in 18 months")
        )));

        template.setSdohFactors(toJson(Arrays.asList(
            sdohFactor("food-insecurity", "Food Insecurity", "Positive HFSSM screening"),
            sdohFactor("housing-instability", "Housing Instability", "Currently in transitional housing"),
            sdohFactor("transportation", "Transportation Barriers", "No reliable transportation to appointments"),
            sdohFactor("medication-cost", "Medication Cost Barriers", "Unable to afford all prescribed medications")
        )));

        template.setObservations(toJson(Arrays.asList(
            observation("4548-4", "Hemoglobin A1c", "9.2", "%"),
            observation("85354-9", "Blood pressure", "138/88", "mmHg"),
            observation("29463-7", "Body Weight", "78", "kg"),
            observation("39156-5", "BMI", "27.8", "kg/m2"),
            observation("82589-3", "PHQ-9 Score", "14", "{score}")
        )));

        template.setScenarioNotes(
            "Use for patient journey demo. Show SDOH screening results, " +
            "care barriers, and how HDIM identifies holistic intervention " +
            "opportunities beyond just clinical measures."
        );

        return template;
    }

    /**
     * Get all pre-defined patient templates.
     */
    public static List<SyntheticPatientTemplate> getAllTemplates() {
        return Arrays.asList(
            createMichaelChen(),
            createSarahMartinez(),
            createEmmaJohnson(),
            createCarlosRodriguez()
        );
    }

    // JSON helper methods

    private static String toJson(List<?> items) {
        if (items.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(items.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }

    private static String condition(String code, String display) {
        return String.format("{\"code\":\"%s\",\"display\":\"%s\"}", code, display);
    }

    private static String medication(String rxnorm, String name, String dosage) {
        return String.format("{\"rxnorm\":\"%s\",\"name\":\"%s\",\"dosage\":\"%s\"}",
            rxnorm, name, dosage);
    }

    private static String careGap(String measure, String name, String reason) {
        return String.format("{\"measure\":\"%s\",\"name\":\"%s\",\"reason\":\"%s\"}",
            measure, name, reason);
    }

    private static String observation(String loinc, String name, String value, String unit) {
        return String.format("{\"loinc\":\"%s\",\"name\":\"%s\",\"value\":\"%s\",\"unit\":\"%s\"}",
            loinc, name, value, unit);
    }

    private static String encounter(String code, String type, String when, String status) {
        return String.format("{\"code\":\"%s\",\"type\":\"%s\",\"when\":\"%s\",\"status\":\"%s\"}",
            code, type, when, status);
    }

    private static String sdohFactor(String id, String name, String detail) {
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"detail\":\"%s\"}",
            id, name, detail);
    }
}
