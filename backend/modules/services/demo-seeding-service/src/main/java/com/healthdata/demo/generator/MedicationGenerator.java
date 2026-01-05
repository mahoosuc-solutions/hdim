package com.healthdata.demo.generator;

import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generates synthetic medication data for demo patients.
 *
 * Medication assignments are based on conditions:
 * - Diabetes: Metformin, Glipizide, Insulin
 * - Hypertension: Lisinopril, Amlodipine, Metoprolol
 * - CHF: Furosemide, Carvedilol, Spironolactone
 * - COPD: Albuterol, Tiotropium, Fluticasone
 * - High cholesterol/Diabetes 40+: Atorvastatin
 */
@Component
public class MedicationGenerator {

    private final Random random = new Random();

    /**
     * Generate medications based on patient conditions.
     *
     * @param patient The patient resource
     * @param conditions List of condition codes (ICD-10)
     * @param bundle Bundle to add medication resources to
     */
    public void generateMedications(Patient patient, List<String> conditions, Bundle bundle) {
        String patientRef = "Patient/" + patient.getId();

        for (String conditionCode : conditions) {
            List<MedicationTemplate> meds = getMedicationsForCondition(conditionCode);
            for (MedicationTemplate med : meds) {
                if (random.nextDouble() < med.getPrescriptionRate()) {
                    MedicationRequest medRequest = createMedicationRequest(patientRef, med);
                    bundle.addEntry()
                        .setFullUrl("MedicationRequest/" + medRequest.getId())
                        .setResource(medRequest);
                }
            }
        }
    }

    /**
     * Create a FHIR MedicationRequest resource.
     */
    private MedicationRequest createMedicationRequest(String patientRef, MedicationTemplate template) {
        MedicationRequest request = new MedicationRequest();
        request.setId(UUID.randomUUID().toString());
        request.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        request.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);

        // Medication code
        CodeableConcept medicationCode = new CodeableConcept();
        medicationCode.addCoding()
            .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
            .setCode(template.getRxNormCode())
            .setDisplay(template.getDisplayName());
        request.setMedication(medicationCode);

        // Subject reference
        request.setSubject(new Reference(patientRef));

        // Authored on (randomly within last 2 years)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(730));
        request.setAuthoredOn(cal.getTime());

        // Dosage
        Dosage dosage = new Dosage();
        dosage.setText(template.getDosageText());

        // Timing
        Timing timing = new Timing();
        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
        repeat.setFrequency(template.getFrequencyPerDay());
        repeat.setPeriod(1);
        repeat.setPeriodUnit(Timing.UnitsOfTime.D);
        timing.setRepeat(repeat);
        dosage.setTiming(timing);

        // Dose
        Dosage.DosageDoseAndRateComponent doseAndRate = new Dosage.DosageDoseAndRateComponent();
        SimpleQuantity doseQuantity = new SimpleQuantity();
        doseQuantity.setValue(template.getDoseValue());
        doseQuantity.setUnit(template.getDoseUnit());
        doseAndRate.setDose(doseQuantity);
        dosage.addDoseAndRate(doseAndRate);

        request.addDosageInstruction(dosage);

        // Dispense request
        MedicationRequest.MedicationRequestDispenseRequestComponent dispense =
            new MedicationRequest.MedicationRequestDispenseRequestComponent();
        dispense.setNumberOfRepeatsAllowed(11); // 12 month supply
        SimpleQuantity quantity = new SimpleQuantity();
        quantity.setValue(30);
        quantity.setUnit("tablets");
        dispense.setQuantity(quantity);
        request.setDispenseRequest(dispense);

        return request;
    }

    /**
     * Get appropriate medications for a condition.
     */
    private List<MedicationTemplate> getMedicationsForCondition(String icd10Code) {
        List<MedicationTemplate> medications = new ArrayList<>();

        switch (icd10Code) {
            case "E11.9": // Type 2 Diabetes
                medications.add(MedicationTemplates.METFORMIN);
                medications.add(MedicationTemplates.GLIPIZIDE);
                medications.add(MedicationTemplates.ATORVASTATIN); // Statin for diabetics
                break;

            case "I10": // Hypertension
                medications.add(MedicationTemplates.LISINOPRIL);
                medications.add(MedicationTemplates.AMLODIPINE);
                medications.add(MedicationTemplates.METOPROLOL);
                break;

            case "I50.9": // CHF
                medications.add(MedicationTemplates.FUROSEMIDE);
                medications.add(MedicationTemplates.CARVEDILOL);
                medications.add(MedicationTemplates.SPIRONOLACTONE);
                medications.add(MedicationTemplates.LISINOPRIL);
                break;

            case "J44.9": // COPD
                medications.add(MedicationTemplates.ALBUTEROL);
                medications.add(MedicationTemplates.TIOTROPIUM);
                medications.add(MedicationTemplates.FLUTICASONE);
                break;

            case "N18.3": // CKD Stage 3
                // Avoid NSAIDs, careful with dosing
                medications.add(MedicationTemplates.CALCIUM_CARBONATE);
                break;
        }

        return medications;
    }

    /**
     * Medication template with prescribing details.
     */
    public static class MedicationTemplate {
        private final String rxNormCode;
        private final String displayName;
        private final String dosageText;
        private final double doseValue;
        private final String doseUnit;
        private final int frequencyPerDay;
        private final double prescriptionRate; // Probability of being prescribed

        public MedicationTemplate(String rxNormCode, String displayName, String dosageText,
                                   double doseValue, String doseUnit, int frequencyPerDay,
                                   double prescriptionRate) {
            this.rxNormCode = rxNormCode;
            this.displayName = displayName;
            this.dosageText = dosageText;
            this.doseValue = doseValue;
            this.doseUnit = doseUnit;
            this.frequencyPerDay = frequencyPerDay;
            this.prescriptionRate = prescriptionRate;
        }

        public String getRxNormCode() { return rxNormCode; }
        public String getDisplayName() { return displayName; }
        public String getDosageText() { return dosageText; }
        public double getDoseValue() { return doseValue; }
        public String getDoseUnit() { return doseUnit; }
        public int getFrequencyPerDay() { return frequencyPerDay; }
        public double getPrescriptionRate() { return prescriptionRate; }
    }

    /**
     * Pre-defined medication templates.
     */
    public static class MedicationTemplates {
        // Diabetes medications
        public static final MedicationTemplate METFORMIN = new MedicationTemplate(
            "860974", "Metformin 500 MG", "Take 1 tablet by mouth twice daily with meals",
            500, "mg", 2, 0.9
        );
        public static final MedicationTemplate GLIPIZIDE = new MedicationTemplate(
            "310539", "Glipizide 5 MG", "Take 1 tablet by mouth daily before breakfast",
            5, "mg", 1, 0.4
        );

        // Hypertension medications
        public static final MedicationTemplate LISINOPRIL = new MedicationTemplate(
            "314076", "Lisinopril 10 MG", "Take 1 tablet by mouth once daily",
            10, "mg", 1, 0.8
        );
        public static final MedicationTemplate AMLODIPINE = new MedicationTemplate(
            "197361", "Amlodipine 5 MG", "Take 1 tablet by mouth once daily",
            5, "mg", 1, 0.5
        );
        public static final MedicationTemplate METOPROLOL = new MedicationTemplate(
            "866924", "Metoprolol Succinate 50 MG", "Take 1 tablet by mouth once daily",
            50, "mg", 1, 0.4
        );

        // CHF medications
        public static final MedicationTemplate FUROSEMIDE = new MedicationTemplate(
            "310429", "Furosemide 40 MG", "Take 1 tablet by mouth once daily in the morning",
            40, "mg", 1, 0.85
        );
        public static final MedicationTemplate CARVEDILOL = new MedicationTemplate(
            "200031", "Carvedilol 6.25 MG", "Take 1 tablet by mouth twice daily with food",
            6.25, "mg", 2, 0.7
        );
        public static final MedicationTemplate SPIRONOLACTONE = new MedicationTemplate(
            "313096", "Spironolactone 25 MG", "Take 1 tablet by mouth once daily",
            25, "mg", 1, 0.6
        );

        // COPD medications
        public static final MedicationTemplate ALBUTEROL = new MedicationTemplate(
            "746763", "Albuterol 90 MCG/Actuation Inhaler",
            "Inhale 2 puffs by mouth every 4-6 hours as needed for shortness of breath",
            90, "mcg", 4, 0.95
        );
        public static final MedicationTemplate TIOTROPIUM = new MedicationTemplate(
            "748794", "Tiotropium 18 MCG Capsule",
            "Inhale 1 capsule by mouth once daily using HandiHaler device",
            18, "mcg", 1, 0.7
        );
        public static final MedicationTemplate FLUTICASONE = new MedicationTemplate(
            "895994", "Fluticasone 250 MCG/Actuation Inhaler",
            "Inhale 2 puffs by mouth twice daily",
            250, "mcg", 2, 0.5
        );

        // Statin
        public static final MedicationTemplate ATORVASTATIN = new MedicationTemplate(
            "617311", "Atorvastatin 20 MG", "Take 1 tablet by mouth once daily at bedtime",
            20, "mg", 1, 0.75
        );

        // CKD
        public static final MedicationTemplate CALCIUM_CARBONATE = new MedicationTemplate(
            "318076", "Calcium Carbonate 500 MG",
            "Take 1 tablet by mouth with meals three times daily",
            500, "mg", 3, 0.6
        );
    }
}
