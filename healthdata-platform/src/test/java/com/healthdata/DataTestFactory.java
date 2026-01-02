package com.healthdata;

import com.healthdata.caregap.domain.CareGap;
import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.MedicationRequest;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.patient.domain.Patient;
import com.healthdata.quality.domain.MeasureResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Factory for creating test entities with realistic default values.
 * Provides builder patterns and convenience methods for test data creation.
 *
 * Usage:
 * Patient patient = DataTestFactory.patientBuilder()
 *     .withMrn("MRN-123")
 *     .withFirstName("John")
 *     .withTenantId("tenant1")
 *     .build();
 */
public class DataTestFactory {

    // ========================================================================
    // Patient Factory Methods
    // ========================================================================

    /**
     * Create a patient builder for fluent test data creation
     */
    public static PatientBuilder patientBuilder() {
        return new PatientBuilder();
    }

    /**
     * Create a default patient for quick test setup
     */
    public static Patient createDefaultPatient() {
        return patientBuilder()
            .withMrn("MRN-TEST-" + System.currentTimeMillis())
            .withFirstName("John")
            .withLastName("Doe")
            .withTenantId("tenant1")
            .build();
    }

    /**
     * Create a patient with specific tenant
     */
    public static Patient createPatientForTenant(String tenantId) {
        return patientBuilder()
            .withMrn("MRN-" + UUID.randomUUID().toString().substring(0, 8))
            .withFirstName("Test")
            .withLastName("Patient")
            .withTenantId(tenantId)
            .build();
    }

    /**
     * Patient builder class
     */
    public static class PatientBuilder {
        private String id = UUID.randomUUID().toString();
        private String mrn = "MRN-" + System.currentTimeMillis();
        private String firstName = "John";
        private String lastName = "Doe";
        private String middleName = "Michael";
        private LocalDate dateOfBirth = LocalDate.of(1960, 1, 1);
        private Patient.Gender gender = Patient.Gender.MALE;
        private String phoneNumber = "555-0001";
        private String email = "test@example.com";
        private String tenantId = "tenant1";
        private boolean active = true;
        private Patient.Address address = new Patient.Address("123 Main St", "Springfield", "IL", "62701", "USA");
        private Set<Patient.Identifier> identifiers = new HashSet<>();

        public PatientBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public PatientBuilder withMrn(String mrn) {
            this.mrn = mrn;
            return this;
        }

        public PatientBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public PatientBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public PatientBuilder withMiddleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public PatientBuilder withDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public PatientBuilder withGender(Patient.Gender gender) {
            this.gender = gender;
            return this;
        }

        public PatientBuilder withPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public PatientBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public PatientBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public PatientBuilder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public PatientBuilder withAddress(Patient.Address address) {
            this.address = address;
            return this;
        }

        public PatientBuilder addIdentifier(String system, String value, String type) {
            identifiers.add(new Patient.Identifier(system, value, type));
            return this;
        }

        public Patient build() {
            return Patient.builder()
                .id(id)
                .mrn(mrn)
                .firstName(firstName)
                .lastName(lastName)
                .middleName(middleName)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .phoneNumber(phoneNumber)
                .email(email)
                .tenantId(tenantId)
                .active(active)
                .address(address)
                .identifiers(identifiers)
                .build();
        }
    }

    // ========================================================================
    // Observation Factory Methods
    // ========================================================================

    public static ObservationBuilder observationBuilder() {
        return new ObservationBuilder();
    }

    /**
     * Create a blood pressure observation
     */
    public static Observation createBloodPressureObservation(String patientId, String tenantId,
                                                              double systolic, double diastolic) {
        LocalDateTime now = LocalDateTime.now();
        return Observation.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .code("8480-6")
            .system("http://loinc.org")
            .display("Systolic Blood Pressure")
            .valueQuantity(BigDecimal.valueOf(systolic))
            .valueUnit("mmHg")
            .status("final")
            .effectiveDate(now)
            .category("vital-signs")
            .tenantId(tenantId)
            .build();
    }

    /**
     * Create a glucose observation
     */
    public static Observation createGlucoseObservation(String patientId, String tenantId, double value) {
        LocalDateTime now = LocalDateTime.now();
        return Observation.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .code("2345-7")
            .system("http://loinc.org")
            .display("Fasting Glucose")
            .valueQuantity(BigDecimal.valueOf(value))
            .valueUnit("mg/dL")
            .status("final")
            .effectiveDate(now)
            .category("laboratory")
            .tenantId(tenantId)
            .build();
    }

    /**
     * Observation builder class
     */
    public static class ObservationBuilder {
        private String id = UUID.randomUUID().toString();
        private String patientId = "patient-1";
        private String code = "8480-6"; // Systolic BP
        private String system = "http://loinc.org";
        private String display = "Systolic Blood Pressure";
        private BigDecimal valueQuantity = BigDecimal.valueOf(120.0);
        private String valueUnit = "mmHg";
        private String valueString = null;
        private String status = "final";
        private LocalDateTime effectiveDate = LocalDateTime.now();
        private String category = "vital-signs";
        private String tenantId = "tenant1";

        public ObservationBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public ObservationBuilder withPatientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public ObservationBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ObservationBuilder withDisplay(String display) {
            this.display = display;
            return this;
        }

        public ObservationBuilder withValueQuantity(BigDecimal value) {
            this.valueQuantity = value;
            return this;
        }

        public ObservationBuilder withValueUnit(String unit) {
            this.valueUnit = unit;
            return this;
        }

        public ObservationBuilder withCategory(String category) {
            this.category = category;
            return this;
        }

        public ObservationBuilder withEffectiveDate(LocalDateTime date) {
            this.effectiveDate = date;
            return this;
        }

        public ObservationBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Observation build() {
            return Observation.builder()
                .id(id)
                .patientId(patientId)
                .code(code)
                .system(system)
                .display(display)
                .valueQuantity(valueQuantity)
                .valueUnit(valueUnit)
                .valueString(valueString)
                .status(status)
                .effectiveDate(effectiveDate)
                .category(category)
                .tenantId(tenantId)
                .build();
        }
    }

    // ========================================================================
    // Condition Factory Methods
    // ========================================================================

    public static ConditionBuilder conditionBuilder() {
        return new ConditionBuilder();
    }

    /**
     * Create a Type 2 Diabetes condition
     */
    public static Condition createDiabetesCondition(String patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return Condition.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .code("44054006")
            .display("Type 2 Diabetes Mellitus")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .category("problem-list-item")
            .severity("moderate")
            .onsetDate(now.minusYears(5))
            .recordedDate(now.minusYears(5))
            .tenantId(tenantId)
            .build();
    }

    /**
     * Create a Hypertension condition
     */
    public static Condition createHypertensionCondition(String patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return Condition.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .code("38341003")
            .display("Essential Hypertension")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .category("problem-list-item")
            .severity("moderate")
            .onsetDate(now.minusYears(8))
            .recordedDate(now.minusYears(8))
            .tenantId(tenantId)
            .build();
    }

    /**
     * Condition builder class
     */
    public static class ConditionBuilder {
        private String id = UUID.randomUUID().toString();
        private String patientId = "patient-1";
        private String code = "44054006"; // Type 2 Diabetes
        private String display = "Type 2 Diabetes Mellitus";
        private String clinicalStatus = "active";
        private String verificationStatus = "confirmed";
        private String category = "problem-list-item";
        private String severity = "moderate";
        private LocalDateTime onsetDate = LocalDateTime.now().minusYears(5);
        private LocalDateTime recordedDate = LocalDateTime.now();
        private LocalDateTime abatementDate = null;
        private String tenantId = "tenant1";

        public ConditionBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public ConditionBuilder withPatientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public ConditionBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ConditionBuilder withDisplay(String display) {
            this.display = display;
            return this;
        }

        public ConditionBuilder withClinicalStatus(String status) {
            this.clinicalStatus = status;
            return this;
        }

        public ConditionBuilder withSeverity(String severity) {
            this.severity = severity;
            return this;
        }

        public ConditionBuilder withOnsetDate(LocalDateTime onsetDate) {
            this.onsetDate = onsetDate;
            return this;
        }

        public ConditionBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Condition build() {
            return Condition.builder()
                .id(id)
                .patientId(patientId)
                .code(code)
                .display(display)
                .clinicalStatus(clinicalStatus)
                .verificationStatus(verificationStatus)
                .category(category)
                .severity(severity)
                .onsetDate(onsetDate)
                .recordedDate(recordedDate)
                .abatementDate(abatementDate)
                .tenantId(tenantId)
                .build();
        }
    }

    // ========================================================================
    // MedicationRequest Factory Methods
    // ========================================================================

    public static MedicationRequestBuilder medicationRequestBuilder() {
        return new MedicationRequestBuilder();
    }

    /**
     * Create a Metformin medication request
     */
    public static MedicationRequest createMetforminRequest(String patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return MedicationRequest.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .medicationCode("860649")
            .medicationDisplay("Metformin 500mg Tab")
            .status("active")
            .intent("order")
            .priority("routine")
            .dosageQuantity(500.0)
            .dosageUnit("mg")
            .dosageTiming("BID")
            .dispenseQuantity(180)
            .dispenseUnit("tablets")
            .daysSupply(30)
            .refillsRemaining(10)
            .authoredOn(now)
            .validPeriodStart(now)
            .validPeriodEnd(now.plusMonths(12))
            .prescriberId("provider-001")
            .reasonCode("44054006")
            .reasonDisplay("Type 2 Diabetes")
            .tenantId(tenantId)
            .build();
    }

    /**
     * MedicationRequest builder class
     */
    public static class MedicationRequestBuilder {
        private String id = UUID.randomUUID().toString();
        private String patientId = "patient-1";
        private String medicationCode = "860649"; // Metformin
        private String medicationDisplay = "Metformin 500mg Tab";
        private String status = "active";
        private String intent = "order";
        private String priority = "routine";
        private Double dosageQuantity = 500.0;
        private String dosageUnit = "mg";
        private String dosageTiming = "BID";
        private Integer dispenseQuantity = 180;
        private String dispenseUnit = "tablets";
        private Integer daysSupply = 30;
        private Integer refillsRemaining = 10;
        private LocalDateTime authoredOn = LocalDateTime.now();
        private LocalDateTime validPeriodStart = LocalDateTime.now();
        private LocalDateTime validPeriodEnd = LocalDateTime.now().plusMonths(12);
        private String prescriberId = "provider-001";
        private String reasonCode = "44054006";
        private String reasonDisplay = "Type 2 Diabetes";
        private String tenantId = "tenant1";

        public MedicationRequestBuilder withPatientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public MedicationRequestBuilder withMedicationCode(String code) {
            this.medicationCode = code;
            return this;
        }

        public MedicationRequestBuilder withMedicationDisplay(String display) {
            this.medicationDisplay = display;
            return this;
        }

        public MedicationRequestBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public MedicationRequestBuilder withDosageQuantity(Double quantity) {
            this.dosageQuantity = quantity;
            return this;
        }

        public MedicationRequestBuilder withDaysSupply(Integer days) {
            this.daysSupply = days;
            return this;
        }

        public MedicationRequestBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public MedicationRequest build() {
            return MedicationRequest.builder()
                .id(id)
                .patientId(patientId)
                .medicationCode(medicationCode)
                .medicationDisplay(medicationDisplay)
                .status(status)
                .intent(intent)
                .priority(priority)
                .dosageQuantity(dosageQuantity)
                .dosageUnit(dosageUnit)
                .dosageTiming(dosageTiming)
                .dispenseQuantity(dispenseQuantity)
                .dispenseUnit(dispenseUnit)
                .daysSupply(daysSupply)
                .refillsRemaining(refillsRemaining)
                .authoredOn(authoredOn)
                .validPeriodStart(validPeriodStart)
                .validPeriodEnd(validPeriodEnd)
                .prescriberId(prescriberId)
                .reasonCode(reasonCode)
                .reasonDisplay(reasonDisplay)
                .tenantId(tenantId)
                .build();
        }
    }

    // ========================================================================
    // MeasureResult Factory Methods
    // ========================================================================

    public static MeasureResultBuilder measureResultBuilder() {
        return new MeasureResultBuilder();
    }

    /**
     * Create a compliant measure result
     */
    public static MeasureResult createCompliantResult(String patientId, String measureId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return MeasureResult.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .measureId(measureId)
            .score(85.0)
            .numerator(1)
            .denominator(1)
            .compliant(true)
            .calculationDate(now)
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .tenantId(tenantId)
            .build();
    }

    /**
     * Create a non-compliant measure result
     */
    public static MeasureResult createNonCompliantResult(String patientId, String measureId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return MeasureResult.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .measureId(measureId)
            .score(45.0)
            .numerator(0)
            .denominator(1)
            .compliant(false)
            .calculationDate(now)
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .tenantId(tenantId)
            .build();
    }

    /**
     * MeasureResult builder class
     */
    public static class MeasureResultBuilder {
        private String id = UUID.randomUUID().toString();
        private String patientId = "patient-1";
        private String measureId = "HEDIS-CDC";
        private Double score = 75.0;
        private Integer numerator = 1;
        private Integer denominator = 1;
        private boolean compliant = true;
        private LocalDateTime calculationDate = LocalDateTime.now();
        private LocalDate periodStart = LocalDate.of(2024, 1, 1);
        private LocalDate periodEnd = LocalDate.of(2024, 12, 31);
        private String tenantId = "tenant1";
        private Map<String, String> details = new HashMap<>();

        public MeasureResultBuilder withPatientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public MeasureResultBuilder withMeasureId(String measureId) {
            this.measureId = measureId;
            return this;
        }

        public MeasureResultBuilder withScore(Double score) {
            this.score = score;
            return this;
        }

        public MeasureResultBuilder withNumerator(Integer numerator) {
            this.numerator = numerator;
            return this;
        }

        public MeasureResultBuilder withDenominator(Integer denominator) {
            this.denominator = denominator;
            return this;
        }

        public MeasureResultBuilder withCompliant(boolean compliant) {
            this.compliant = compliant;
            return this;
        }

        public MeasureResultBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public MeasureResult build() {
            return MeasureResult.builder()
                .id(id)
                .patientId(patientId)
                .measureId(measureId)
                .score(score)
                .numerator(numerator)
                .denominator(denominator)
                .compliant(compliant)
                .calculationDate(calculationDate)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .tenantId(tenantId)
                .details(details)
                .build();
        }
    }

    // ========================================================================
    // CareGap Factory Methods
    // ========================================================================

    public static CareGapBuilder careGapBuilder() {
        return new CareGapBuilder();
    }

    /**
     * Create an open high-priority care gap
     */
    public static CareGap createOpenHighPriorityGap(String patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return CareGap.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .gapType("CHRONIC_DISEASE_MONITORING")
            .description("Needs follow-up")
            .priority("HIGH")
            .status("OPEN")
            .measureId("HEDIS-CDC")
            .dueDate(now.plusDays(30))
            .detectedDate(now)
            .providerId("provider-001")
            .careTeamId("team-001")
            .interventionType("OUTREACH")
            .riskScore(80.0)
            .financialImpact(2500.0)
            .tenantId(tenantId)
            .build();
    }

    /**
     * Create a closed care gap
     */
    public static CareGap createClosedGap(String patientId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return CareGap.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .gapType("CHRONIC_DISEASE_MONITORING")
            .description("Gap has been resolved")
            .priority("MEDIUM")
            .status("CLOSED")
            .measureId("HEDIS-HTN")
            .dueDate(now.minusDays(5))
            .detectedDate(now.minusDays(30))
            .closedDate(now.minusDays(2))
            .closureReason("Patient completed intervention")
            .providerId("provider-001")
            .careTeamId("team-001")
            .interventionType("APPOINTMENT")
            .riskScore(45.0)
            .financialImpact(1500.0)
            .tenantId(tenantId)
            .build();
    }

    /**
     * CareGap builder class
     */
    public static class CareGapBuilder {
        private String id = UUID.randomUUID().toString();
        private String patientId = "patient-1";
        private String gapType = "CHRONIC_DISEASE_MONITORING";
        private String description = "Patient requires intervention";
        private String priority = "HIGH";
        private String status = "OPEN";
        private String measureId = "HEDIS-CDC";
        private LocalDateTime dueDate = LocalDateTime.now().plusDays(30);
        private LocalDateTime detectedDate = LocalDateTime.now();
        private LocalDateTime closedDate = null;
        private String closureReason = null;
        private String providerId = "provider-001";
        private String careTeamId = "team-001";
        private String interventionType = "OUTREACH";
        private String interventionNotes = null;
        private Double riskScore = 75.0;
        private Double financialImpact = 2000.0;
        private String tenantId = "tenant1";

        public CareGapBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public CareGapBuilder withPatientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public CareGapBuilder withGapType(String gapType) {
            this.gapType = gapType;
            return this;
        }

        public CareGapBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public CareGapBuilder withPriority(String priority) {
            this.priority = priority;
            return this;
        }

        public CareGapBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public CareGapBuilder withMeasureId(String measureId) {
            this.measureId = measureId;
            return this;
        }

        public CareGapBuilder withDueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public CareGapBuilder withRiskScore(Double riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public CareGapBuilder withFinancialImpact(Double impact) {
            this.financialImpact = impact;
            return this;
        }

        public CareGapBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CareGap build() {
            return CareGap.builder()
                .id(id)
                .patientId(patientId)
                .gapType(gapType)
                .description(description)
                .priority(priority)
                .status(status)
                .measureId(measureId)
                .dueDate(dueDate)
                .detectedDate(detectedDate)
                .closedDate(closedDate)
                .closureReason(closureReason)
                .providerId(providerId)
                .careTeamId(careTeamId)
                .interventionType(interventionType)
                .interventionNotes(interventionNotes)
                .riskScore(riskScore)
                .financialImpact(financialImpact)
                .tenantId(tenantId)
                .build();
        }
    }
}
