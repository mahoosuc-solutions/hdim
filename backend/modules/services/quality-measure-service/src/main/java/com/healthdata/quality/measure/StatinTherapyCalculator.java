package com.healthdata.quality.measure;

import com.healthdata.quality.model.*;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * HEDIS Statin Therapy for Patients With Cardiovascular Disease (SPC) Measure Calculator
 *
 * Evaluates whether patients with clinical ASCVD (atherosclerotic cardiovascular disease)
 * are receiving statin therapy.
 *
 * HEDIS Specification: SPC-E (2024)
 */
@Component
public class StatinTherapyCalculator implements MeasureCalculator {

    private static final String MEASURE_ID = "SPC";
    private static final String MEASURE_NAME = "Statin Therapy for Patients With Cardiovascular Disease";
    private static final String VERSION = "2024";

    // ICD-10/SNOMED codes for ASCVD (ischemic heart disease, MI, angina, stroke)
    private static final List<String> ASCVD_CODES = Arrays.asList(
        // ICD-10 Coronary artery disease
        "I25.10",  // Atherosclerotic heart disease of native coronary artery
        "I25.110", // Atherosclerotic heart disease of native coronary artery with unstable angina pectoris
        "I25.111", // Atherosclerotic heart disease of native coronary artery with angina pectoris with documented spasm
        "I25.118", // Atherosclerotic heart disease of native coronary artery with other forms of angina pectoris
        "I25.119", // Atherosclerotic heart disease of native coronary artery with unspecified angina pectoris
        "I25.2",   // Old myocardial infarction
        "I25.5",   // Ischemic cardiomyopathy
        "I25.6",   // Silent myocardial ischemia
        "I25.7",   // Atherosclerosis of coronary artery bypass graft(s)
        "I25.8",   // Other forms of chronic ischemic heart disease
        "I25.9",   // Chronic ischemic heart disease, unspecified
        // ICD-10 Stroke/TIA
        "I63",     // Cerebral infarction
        "I63.0",   // Cerebral infarction due to thrombosis
        "I63.1",   // Cerebral infarction due to embolism
        "I63.2",   // Cerebral infarction due to unspecified occlusion
        "I63.3",   // Cerebral infarction due to thrombosis of cerebral arteries
        "I63.4",   // Cerebral infarction due to embolism of cerebral arteries
        "I63.5",   // Cerebral infarction due to unspecified occlusion or stenosis
        "I63.6",   // Cerebral infarction due to cerebral venous thrombosis
        "I63.8",   // Other cerebral infarction
        "I63.9",   // Cerebral infarction, unspecified
        "G45",     // Transient cerebral ischemic attacks
        // ICD-10 Peripheral artery disease
        "I70.2",   // Atherosclerosis of native arteries of extremities
        "I70.0",   // Atherosclerosis of aorta
        // SNOMED codes
        "53741008",  // Coronary arteriosclerosis
        "22298006",  // Myocardial infarction
        "394659003", // Acute coronary syndrome
        "233970002", // Coronary artery bypass graft
        "230690007", // Cerebrovascular accident
        "266257000", // Transient ischemic attack
        "400047006", // Peripheral vascular disease
        "399211009"  // History of MI
    );

    // RxNorm codes for statins
    private static final List<String> STATIN_RXNORM_CODES = Arrays.asList(
        // Atorvastatin
        "617314", "617311", "617318", "617320",
        // Rosuvastatin
        "861634", "861643", "861646", "861648",
        // Simvastatin
        "312961", "312962", "312963", "312964", "312965",
        // Pravastatin
        "308409", "308410", "308411",
        // Lovastatin
        "197904", "197905", "197906",
        // Fluvastatin
        "310405", "310404",
        // Pitavastatin
        "859751", "859753"
    );

    // Generic statin names for display matching
    private static final List<String> STATIN_NAMES = Arrays.asList(
        "atorvastatin", "rosuvastatin", "simvastatin", "pravastatin",
        "lovastatin", "fluvastatin", "pitavastatin", "lipitor",
        "crestor", "zocor", "pravachol", "mevacor", "lescol", "livalo"
    );

    // Exclusion codes
    private static final List<String> EXCLUSION_CODES = Arrays.asList(
        "62014003",  // Adverse reaction to HMG CoA reductase inhibitor
        "293631009", // Statin allergy
        "91936005"   // Allergy to drug
    );

    @Override
    public MeasureResult calculate(PatientData patientData) {
        MeasureResult result = MeasureResult.builder()
            .measureId(MEASURE_ID)
            .measureName(MEASURE_NAME)
            .patientId(patientData.getPatient() != null
                ? parsePatientId(patientData.getPatient().getId())
                : null)
            .build();

        Patient patient = patientData.getPatient();
        if (patient == null) {
            result.setExclusionReason("No patient data");
            return result;
        }

        // Check age eligibility (21-75)
        int age = calculateAge(patient);
        if (age < 21 || age > 75) {
            result.setExclusionReason("Age not in range 21-75 (age: " + age + ")");
            return result;
        }

        // Check for ASCVD diagnosis
        if (!hasASCVDDiagnosis(patientData)) {
            result.setExclusionReason("No ASCVD diagnosis");
            return result;
        }

        // Check for statin allergy/intolerance
        if (hasStatinContraindication(patientData)) {
            result.setExclusionReason("Documented statin allergy or intolerance");
            return result;
        }

        result.setEligible(true);
        result.setDenominatorMembership(true);

        // Check for active statin therapy
        boolean hasStatinTherapy = hasActiveStatinPrescription(patientData);

        // Create sub-measure
        Map<String, SubMeasureResult> subMeasures = new HashMap<>();
        subMeasures.put("Statin Therapy", SubMeasureResult.builder()
            .numeratorMembership(hasStatinTherapy)
            .method(hasStatinTherapy ? "medication" : "none")
            .build());
        result.setSubMeasures(subMeasures);

        if (!hasStatinTherapy) {
            List<CareGap> careGaps = new ArrayList<>();
            careGaps.add(CareGap.builder()
                .type("missing-statin-therapy")
                .description("Patient with ASCVD not on statin therapy")
                .severity("high")
                .action("Initiate statin therapy for secondary prevention")
                .measureComponent("Statin Therapy")
                .build());
            result.setCareGaps(careGaps);

            List<Recommendation> recommendations = new ArrayList<>();
            recommendations.add(Recommendation.builder()
                .priority("high")
                .action("Prescribe high-intensity statin (atorvastatin 40-80mg or rosuvastatin 20-40mg)")
                .rationale("Patient has clinical ASCVD and requires statin therapy for secondary prevention")
                .category("medication")
                .build());
            result.setRecommendations(recommendations);
        }

        return result;
    }

    private boolean hasASCVDDiagnosis(PatientData patientData) {
        if (patientData.getConditions() == null) return false;

        return patientData.getConditions().stream()
            .filter(cond -> cond.getCode() != null && cond.getCode().hasCoding())
            .flatMap(cond -> cond.getCode().getCoding().stream())
            .anyMatch(coding -> ASCVD_CODES.contains(coding.getCode()));
    }

    private boolean hasStatinContraindication(PatientData patientData) {
        // Check conditions for allergies
        if (patientData.getConditions() != null) {
            boolean hasAllergy = patientData.getConditions().stream()
                .filter(cond -> cond.getCode() != null && cond.getCode().hasCoding())
                .flatMap(cond -> cond.getCode().getCoding().stream())
                .anyMatch(coding -> EXCLUSION_CODES.contains(coding.getCode()));
            if (hasAllergy) return true;
        }

        return false;
    }

    private boolean hasActiveStatinPrescription(PatientData patientData) {
        // Check MedicationStatements
        if (patientData.getMedicationStatements() != null) {
            for (MedicationStatement med : patientData.getMedicationStatements()) {
                if (isStatinMedication(med)) {
                    // Check if active
                    if (med.getStatus() == MedicationStatement.MedicationStatementStatus.ACTIVE ||
                        med.getStatus() == MedicationStatement.MedicationStatementStatus.INTENDED) {
                        return true;
                    }
                }
            }
        }

        // Also check MedicationRequests if available (through conditions workaround)
        // The demo seeding creates MedicationRequests, check for those patterns

        return false;
    }

    private boolean isStatinMedication(MedicationStatement med) {
        if (med.getMedicationCodeableConcept() != null) {
            CodeableConcept medCode = med.getMedicationCodeableConcept();

            // Check RxNorm codes
            if (medCode.hasCoding()) {
                for (Coding coding : medCode.getCoding()) {
                    if (STATIN_RXNORM_CODES.contains(coding.getCode())) {
                        return true;
                    }
                }
            }

            // Check display text
            if (medCode.hasText()) {
                String text = medCode.getText().toLowerCase();
                return STATIN_NAMES.stream().anyMatch(text::contains);
            }

            // Check coding display
            if (medCode.hasCoding()) {
                for (Coding coding : medCode.getCoding()) {
                    if (coding.hasDisplay()) {
                        String display = coding.getDisplay().toLowerCase();
                        if (STATIN_NAMES.stream().anyMatch(display::contains)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private int calculateAge(Patient patient) {
        if (patient.getBirthDate() == null) return 0;
        LocalDate birthDate = patient.getBirthDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, LocalDate.now()).getYears();
    }

    private UUID parsePatientId(String id) {
        try {
            if (id != null && id.contains("/")) {
                id = id.substring(id.lastIndexOf("/") + 1);
            }
            return UUID.fromString(id);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getMeasureId() {
        return MEASURE_ID;
    }

    @Override
    public String getMeasureName() {
        return MEASURE_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
