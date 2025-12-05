package com.healthdata.test;

import com.healthdata.fhir.entity.Condition;
import com.healthdata.fhir.entity.Observation;
import com.healthdata.fhir.entity.MedicationRequest;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.ObservationRepository;
import com.healthdata.fhir.repository.MedicationRequestRepository;
import com.healthdata.patient.entity.Patient;
import com.healthdata.patient.repository.PatientRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fluent Test Data Builder
 *
 * Provides fluent API for creating test data with realistic clinical scenarios.
 * Supports building complex patient scenarios with observations, conditions, and medications.
 *
 * Usage:
 * <pre>
 * Patient patient = testDataBuilder
 *     .aPatientWithDiabetes()
 *     .age(55)
 *     .withHbA1c(6.8)
 *     .withBloodPressure(130, 80)
 *     .build();
 * </pre>
 *
 * @author TDD Swarm Agent 5B
 */
@Component
public class TestDataBuilder {

    private final PatientRepository patientRepository;
    private final ObservationRepository observationRepository;
    private final ConditionRepository conditionRepository;
    private final MedicationRequestRepository medicationRepository;

    private static final String DEFAULT_TENANT_ID = "test-tenant-1";

    public TestDataBuilder(
            PatientRepository patientRepository,
            ObservationRepository observationRepository,
            ConditionRepository conditionRepository,
            MedicationRequestRepository medicationRepository) {
        this.patientRepository = patientRepository;
        this.observationRepository = observationRepository;
        this.conditionRepository = conditionRepository;
        this.medicationRepository = medicationRepository;
    }

    // ==================== PATIENT BUILDERS ====================

    /**
     * Creates a healthy patient builder
     */
    public PatientBuilder aHealthyPatient() {
        return new PatientBuilder()
                .firstName("Healthy")
                .lastName("Patient")
                .mrn("MRN-HEALTHY-" + UUID.randomUUID())
                .age(50)
                .gender("male")
                .tenantId(DEFAULT_TENANT_ID);
    }

    /**
     * Creates a patient with Type 2 Diabetes
     */
    public PatientBuilder aPatientWithDiabetes() {
        return new PatientBuilder()
                .firstName("Diabetic")
                .lastName("Patient")
                .mrn("MRN-DIABETES-" + UUID.randomUUID())
                .age(60)
                .gender("male")
                .tenantId(DEFAULT_TENANT_ID)
                .withCondition("E11", "Type 2 Diabetes Mellitus");
    }

    /**
     * Creates a patient with Hypertension
     */
    public PatientBuilder aPatientWithHypertension() {
        return new PatientBuilder()
                .firstName("Hypertensive")
                .lastName("Patient")
                .mrn("MRN-HTN-" + UUID.randomUUID())
                .age(55)
                .gender("male")
                .tenantId(DEFAULT_TENANT_ID)
                .withCondition("I10", "Essential Hypertension");
    }

    /**
     * Creates a high-risk patient with multiple chronic conditions
     */
    public PatientBuilder aHighRiskPatient() {
        return new PatientBuilder()
                .firstName("HighRisk")
                .lastName("Patient")
                .mrn("MRN-HIGHRISK-" + UUID.randomUUID())
                .age(75)
                .gender("male")
                .tenantId(DEFAULT_TENANT_ID)
                .withCondition("I10", "Essential Hypertension")
                .withCondition("E11", "Type 2 Diabetes Mellitus")
                .withCondition("I50", "Heart Failure");
    }

    /**
     * Creates an elderly patient
     */
    public PatientBuilder anElderlyPatient() {
        return new PatientBuilder()
                .firstName("Elderly")
                .lastName("Patient")
                .mrn("MRN-ELDERLY-" + UUID.randomUUID())
                .age(80)
                .gender("female")
                .tenantId(DEFAULT_TENANT_ID);
    }

    /**
     * Creates a female patient for screening scenarios
     */
    public PatientBuilder aFemalePatientForScreening() {
        return new PatientBuilder()
                .firstName("Screening")
                .lastName("Patient")
                .mrn("MRN-SCREEN-" + UUID.randomUUID())
                .age(55)
                .gender("female")
                .tenantId(DEFAULT_TENANT_ID);
    }

    // ==================== OBSERVATION BUILDERS ====================

    /**
     * Creates an HbA1c observation builder
     */
    public ObservationBuilder anHbA1cObservation() {
        return new ObservationBuilder()
                .code("4548-4")
                .system("http://loinc.org")
                .display("Hemoglobin A1c")
                .unit("%")
                .effectiveDate(LocalDate.now().minusDays(30));
    }

    /**
     * Creates a blood pressure observation builder
     */
    public ObservationBuilder aBloodPressureObservation() {
        return new ObservationBuilder()
                .code("85354-9") // Blood pressure panel
                .system("http://loinc.org")
                .display("Blood Pressure")
                .effectiveDate(LocalDate.now().minusDays(30));
    }

    /**
     * Creates a cholesterol observation builder
     */
    public ObservationBuilder aCholesterolObservation() {
        return new ObservationBuilder()
                .code("2093-3")
                .system("http://loinc.org")
                .display("Total Cholesterol")
                .unit("mg/dL")
                .effectiveDate(LocalDate.now().minusDays(30));
    }

    // ==================== PATIENT BUILDER CLASS ====================

    public class PatientBuilder {
        private String id;
        private String firstName;
        private String lastName;
        private String mrn;
        private LocalDate dateOfBirth;
        private int age;
        private String gender;
        private String email;
        private String phone;
        private String tenantId;
        private List<ConditionData> conditions = new ArrayList<>();
        private List<ObservationData> observations = new ArrayList<>();
        private List<MedicationData> medications = new ArrayList<>();

        public PatientBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PatientBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public PatientBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public PatientBuilder mrn(String mrn) {
            this.mrn = mrn;
            return this;
        }

        public PatientBuilder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public PatientBuilder age(int age) {
            this.age = age;
            this.dateOfBirth = LocalDate.now().minusYears(age);
            return this;
        }

        public PatientBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public PatientBuilder email(String email) {
            this.email = email;
            return this;
        }

        public PatientBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public PatientBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        // ========== CONDITION BUILDERS ==========

        public PatientBuilder withCondition(String code, String display) {
            conditions.add(new ConditionData(code, "http://hl7.org/fhir/sid/icd-10", display, "active"));
            return this;
        }

        public PatientBuilder withDiabetes() {
            return withCondition("E11", "Type 2 Diabetes Mellitus");
        }

        public PatientBuilder withHypertension() {
            return withCondition("I10", "Essential Hypertension");
        }

        public PatientBuilder withHeartFailure() {
            return withCondition("I50", "Heart Failure");
        }

        public PatientBuilder withCOPD() {
            return withCondition("J44", "Chronic Obstructive Pulmonary Disease");
        }

        // ========== OBSERVATION BUILDERS ==========

        public PatientBuilder withHbA1c(double value) {
            observations.add(new ObservationData(
                    "4548-4", "http://loinc.org", "Hemoglobin A1c",
                    value, "%", LocalDate.now().minusDays(30)));
            return this;
        }

        public PatientBuilder withHbA1c(double value, LocalDate effectiveDate) {
            observations.add(new ObservationData(
                    "4548-4", "http://loinc.org", "Hemoglobin A1c",
                    value, "%", effectiveDate));
            return this;
        }

        public PatientBuilder withBloodPressure(int systolic, int diastolic) {
            observations.add(new ObservationData(
                    "8480-6", "http://loinc.org", "Systolic Blood Pressure",
                    (double) systolic, "mmHg", LocalDate.now().minusDays(30)));
            observations.add(new ObservationData(
                    "8462-4", "http://loinc.org", "Diastolic Blood Pressure",
                    (double) diastolic, "mmHg", LocalDate.now().minusDays(30)));
            return this;
        }

        public PatientBuilder withBloodPressure(int systolic, int diastolic, LocalDate effectiveDate) {
            observations.add(new ObservationData(
                    "8480-6", "http://loinc.org", "Systolic BP",
                    (double) systolic, "mmHg", effectiveDate));
            observations.add(new ObservationData(
                    "8462-4", "http://loinc.org", "Diastolic BP",
                    (double) diastolic, "mmHg", effectiveDate));
            return this;
        }

        public PatientBuilder withCholesterol(double value) {
            observations.add(new ObservationData(
                    "2093-3", "http://loinc.org", "Total Cholesterol",
                    value, "mg/dL", LocalDate.now().minusDays(30)));
            return this;
        }

        public PatientBuilder withBMI(double value) {
            observations.add(new ObservationData(
                    "39156-5", "http://loinc.org", "Body Mass Index",
                    value, "kg/m2", LocalDate.now().minusDays(30)));
            return this;
        }

        public PatientBuilder withMammogram() {
            observations.add(new ObservationData(
                    "24606-6", "http://loinc.org", "Mammogram",
                    null, null, LocalDate.now().minusMonths(18)));
            return this;
        }

        public PatientBuilder withColonoscopy() {
            observations.add(new ObservationData(
                    "73761-0", "http://loinc.org", "Colonoscopy",
                    null, null, LocalDate.now().minusYears(5)));
            return this;
        }

        // ========== MEDICATION BUILDERS ==========

        public PatientBuilder withMedication(String medicationCode, String display) {
            medications.add(new MedicationData(medicationCode, display, "active"));
            return this;
        }

        public PatientBuilder withMetformin() {
            return withMedication("6809", "Metformin");
        }

        public PatientBuilder withLisinopril() {
            return withMedication("29046", "Lisinopril");
        }

        public PatientBuilder withAspirin() {
            return withMedication("1191", "Aspirin");
        }

        // ========== BUILD METHOD ==========

        public Patient build() {
            // Create patient
            Patient patient = Patient.builder()
                    .id(id != null ? id : UUID.randomUUID().toString())
                    .firstName(firstName)
                    .lastName(lastName)
                    .mrn(mrn)
                    .dateOfBirth(dateOfBirth != null ? dateOfBirth : LocalDate.now().minusYears(age))
                    .gender(gender)
                    .email(email)
                    .phone(phone)
                    .tenantId(tenantId)
                    .build();

            patient = patientRepository.save(patient);

            // Create conditions
            for (ConditionData conditionData : conditions) {
                Condition condition = Condition.builder()
                        .id(UUID.randomUUID().toString())
                        .patientId(patient.getId())
                        .code(conditionData.code)
                        .system(conditionData.system)
                        .display(conditionData.display)
                        .clinicalStatus(conditionData.status)
                        .tenantId(tenantId)
                        .build();
                conditionRepository.save(condition);
            }

            // Create observations
            for (ObservationData obsData : observations) {
                Observation obs = Observation.builder()
                        .id(UUID.randomUUID().toString())
                        .patientId(patient.getId())
                        .code(obsData.code)
                        .system(obsData.system)
                        .display(obsData.display)
                        .valueQuantity(obsData.value)
                        .unit(obsData.unit)
                        .effectiveDateTime(obsData.effectiveDate.atStartOfDay())
                        .status("final")
                        .tenantId(tenantId)
                        .build();
                observationRepository.save(obs);
            }

            // Create medications
            for (MedicationData medData : medications) {
                MedicationRequest medReq = MedicationRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .patientId(patient.getId())
                        .medicationCode(medData.code)
                        .medicationDisplay(medData.display)
                        .status(medData.status)
                        .authoredOn(LocalDateTime.now().minusDays(30))
                        .tenantId(tenantId)
                        .build();
                medicationRepository.save(medReq);
            }

            return patient;
        }

        public Patient buildAndSave() {
            return build();
        }
    }

    // ==================== OBSERVATION BUILDER CLASS ====================

    public class ObservationBuilder {
        private String patientId;
        private String code;
        private String system;
        private String display;
        private Double value;
        private String unit;
        private LocalDate effectiveDate;
        private String status = "final";
        private String tenantId = DEFAULT_TENANT_ID;

        public ObservationBuilder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public ObservationBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ObservationBuilder system(String system) {
            this.system = system;
            return this;
        }

        public ObservationBuilder display(String display) {
            this.display = display;
            return this;
        }

        public ObservationBuilder value(Double value) {
            this.value = value;
            return this;
        }

        public ObservationBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public ObservationBuilder effectiveDate(LocalDate effectiveDate) {
            this.effectiveDate = effectiveDate;
            return this;
        }

        public ObservationBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ObservationBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Observation build() {
            Observation obs = Observation.builder()
                    .id(UUID.randomUUID().toString())
                    .patientId(patientId)
                    .code(code)
                    .system(system)
                    .display(display)
                    .valueQuantity(value)
                    .unit(unit)
                    .effectiveDateTime(effectiveDate != null ? effectiveDate.atStartOfDay() : LocalDateTime.now())
                    .status(status)
                    .tenantId(tenantId)
                    .build();

            return observationRepository.save(obs);
        }
    }

    // ==================== DATA CLASSES ====================

    private static class ConditionData {
        String code;
        String system;
        String display;
        String status;

        ConditionData(String code, String system, String display, String status) {
            this.code = code;
            this.system = system;
            this.display = display;
            this.status = status;
        }
    }

    private static class ObservationData {
        String code;
        String system;
        String display;
        Double value;
        String unit;
        LocalDate effectiveDate;

        ObservationData(String code, String system, String display, Double value, String unit, LocalDate effectiveDate) {
            this.code = code;
            this.system = system;
            this.display = display;
            this.value = value;
            this.unit = unit;
            this.effectiveDate = effectiveDate;
        }
    }

    private static class MedicationData {
        String code;
        String display;
        String status;

        MedicationData(String code, String display, String status) {
            this.code = code;
            this.display = display;
            this.status = status;
        }
    }
}
