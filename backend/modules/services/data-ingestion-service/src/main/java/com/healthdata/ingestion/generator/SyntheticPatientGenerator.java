package com.healthdata.ingestion.generator;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ingestion.domain.SyntheticPatientTemplate;
import net.datafaker.Faker;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generates synthetic FHIR R4 compliant patient data for demo purposes.
 *
 * Features:
 * - Realistic names, ages, demographics
 * - Clinically plausible conditions and medications
 * - HCC risk score distribution
 * - Care gap generation for quality measures
 * - HIPAA-compliant (no real PHI)
 *
 * Patient Distribution:
 * - 60% low-risk (HCC < 1.0)
 * - 30% moderate-risk (HCC 1.0-2.0)
 * - 10% high-risk (HCC > 2.0)
 *
 * Condition Prevalence:
 * - Diabetes: 12%
 * - Hypertension: 30%
 * - CHF: 3%
 * - COPD: 6%
 * - CKD: 8%
 */
@Component
public class SyntheticPatientGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SyntheticPatientGenerator.class);

    private final FhirContext fhirContext;
    private final Faker faker;
    private final Random random;
    private final MedicationGenerator medicationGenerator;
    private final ObservationGenerator observationGenerator;
    private final EncounterGenerator encounterGenerator;
    private final ProcedureGenerator procedureGenerator;
    private final ObjectMapper objectMapper;

    public SyntheticPatientGenerator(
            FhirContext fhirContext,
            MedicationGenerator medicationGenerator,
            ObservationGenerator observationGenerator,
            EncounterGenerator encounterGenerator,
            ProcedureGenerator procedureGenerator,
            ObjectMapper objectMapper) {
        this.fhirContext = fhirContext;
        this.faker = new Faker();
        this.random = new Random();
        this.medicationGenerator = medicationGenerator;
        this.observationGenerator = observationGenerator;
        this.encounterGenerator = encounterGenerator;
        this.procedureGenerator = procedureGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate a cohort of synthetic patients with realistic distribution.
     *
     * @param count Number of patients to generate
     * @param tenantId Tenant context
     * @return Bundle containing all generated FHIR resources
     */
    public Bundle generateCohort(int count, String tenantId) {
        logger.info("Generating {} synthetic patients for tenant: {}", count, tenantId);

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());

        for (int i = 0; i < count; i++) {
            PatientGenerationContext context = determinePatientContext(i, count);
            Patient patient = generatePatient(context, tenantId);

            bundle.addEntry()
                .setFullUrl("Patient/" + patient.getId())
                .setResource(patient);

            // Generate associated resources
            generateConditions(patient, context, bundle);
            generateMedications(patient, context, bundle);
            generateObservations(patient, context, bundle);
            generateEncounters(patient, context, bundle);
            generateProcedures(patient, context, bundle);

            if ((i + 1) % 100 == 0) {
                logger.info("Generated {} / {} patients", i + 1, count);
            }
        }

        logger.info("Completed generation of {} patients", count);
        return bundle;
    }

    /**
     * Generate from a pre-defined patient template (persona).
     *
     * @param template Patient template/persona
     * @param tenantId Tenant context
     * @return Bundle with patient and related resources
     */
    public Bundle generateFromTemplate(SyntheticPatientTemplate template, String tenantId) {
        logger.info("Generating patient from template: {}", template.getPersonaName());

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = createPatientFromTemplate(template, tenantId);
        bundle.addEntry().setFullUrl("Patient/" + patient.getId()).setResource(patient);

        // Apply template-specific conditions, meds, etc.
        applyTemplateConditions(patient, template, bundle);
        applyTemplateMedications(patient, template, bundle);
        applyTemplateObservations(patient, template, bundle);
        
        // Generate encounters and procedures based on template
        String riskCategory = template.getRiskCategory().name();
        encounterGenerator.generateEncounters(patient, riskCategory, bundle);
        
        int age = template.getAge();
        String gender = template.getGender().name();
        List<String> conditionCodes = parseConditionCodes(template.getConditions());
        boolean createCareGap = random.nextDouble() < 0.3; // 30% have gaps
        procedureGenerator.generateProcedures(patient, age, gender, conditionCodes, createCareGap, bundle);

        return bundle;
    }

    /**
     * Determine patient generation context based on distribution.
     */
    private PatientGenerationContext determinePatientContext(int index, int total) {
        double percentile = (double) index / total;

        PatientGenerationContext context = new PatientGenerationContext();

        // Risk distribution: 60% low, 30% moderate, 10% high
        if (percentile < 0.60) {
            context.setRiskCategory(RiskCategory.LOW);
            context.setHccScore(BigDecimal.valueOf(0.5 + (random.nextDouble() * 0.5))); // 0.5-1.0
        } else if (percentile < 0.90) {
            context.setRiskCategory(RiskCategory.MODERATE);
            context.setHccScore(BigDecimal.valueOf(1.0 + (random.nextDouble() * 1.0))); // 1.0-2.0
        } else {
            context.setRiskCategory(RiskCategory.HIGH);
            context.setHccScore(BigDecimal.valueOf(2.0 + (random.nextDouble() * 2.0))); // 2.0-4.0
        }

        // Age distribution
        double ageRand = random.nextDouble();
        if (ageRand < 0.25) {
            context.setAgeRange(AgeRange.YOUNG_ADULT); // 18-44
        } else if (ageRand < 0.65) {
            context.setAgeRange(AgeRange.MIDDLE_AGE); // 45-64
        } else {
            context.setAgeRange(AgeRange.SENIOR); // 65+
        }

        return context;
    }

    /**
     * Generate a realistic patient resource.
     */
    private Patient generatePatient(PatientGenerationContext context, String tenantId) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());

        // Name
        boolean isFemale = random.nextBoolean();
        HumanName name = new HumanName();
        name.setFamily(faker.name().lastName());
        name.addGiven(isFemale ? faker.name().femaleFirstName() : faker.name().firstName());
        name.setUse(HumanName.NameUse.OFFICIAL);
        patient.addName(name);

        // Gender
        patient.setGender(isFemale ? Enumerations.AdministrativeGender.FEMALE : Enumerations.AdministrativeGender.MALE);

        // Birth date based on age range
        LocalDate birthDate = generateBirthDate(context.getAgeRange());
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // Address
        Address address = new Address();
        address.addLine(faker.address().streetAddress());
        address.setCity(faker.address().city());
        address.setState(faker.address().stateAbbr());
        address.setPostalCode(faker.address().zipCode());
        address.setCountry("USA");
        patient.addAddress(address);

        // Telecom
        ContactPoint phone = new ContactPoint();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setValue(faker.phoneNumber().phoneNumber());
        patient.addTelecom(phone);

        // Identifier - MRN
        Identifier mrn = new Identifier();
        mrn.setSystem("urn:oid:2.16.840.1.113883.4.1"); // SSN OID
        mrn.setValue(generateMRN());
        patient.addIdentifier(mrn);

        // Extension for tenant context
        Extension tenantExt = new Extension();
        tenantExt.setUrl("http://healthdata.com/fhir/StructureDefinition/tenant-id");
        tenantExt.setValue(new StringType(tenantId));
        patient.addExtension(tenantExt);

        // Extension for HCC risk score
        Extension hccExt = new Extension();
        hccExt.setUrl("http://healthdata.com/fhir/StructureDefinition/hcc-risk-score");
        hccExt.setValue(new DecimalType(context.getHccScore()));
        patient.addExtension(hccExt);

        return patient;
    }

    private LocalDate generateBirthDate(AgeRange ageRange) {
        int minAge = switch (ageRange) {
            case YOUNG_ADULT -> 18;
            case MIDDLE_AGE -> 45;
            case SENIOR -> 65;
        };
        int maxAge = switch (ageRange) {
            case YOUNG_ADULT -> 44;
            case MIDDLE_AGE -> 64;
            case SENIOR -> 85;
        };

        int age = minAge + random.nextInt(maxAge - minAge + 1);
        return LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
    }

    private String generateMRN() {
        return String.format("MRN%07d", random.nextInt(10000000));
    }

    /**
     * Generate conditions based on patient risk category.
     */
    private void generateConditions(Patient patient, PatientGenerationContext context, Bundle bundle) {
        List<ConditionTemplate> conditions = selectConditions(context);

        for (ConditionTemplate template : conditions) {
            Condition condition = new Condition();
            condition.setId(UUID.randomUUID().toString());
            condition.setSubject(new Reference("Patient/" + patient.getId()));

            // Clinical status
            CodeableConcept clinicalStatus = new CodeableConcept();
            clinicalStatus.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active")
                .setDisplay("Active");
            condition.setClinicalStatus(clinicalStatus);

            // Verification status
            CodeableConcept verificationStatus = new CodeableConcept();
            verificationStatus.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed")
                .setDisplay("Confirmed");
            condition.setVerificationStatus(verificationStatus);

            // Condition code
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
                .setCode(template.getIcd10Code())
                .setDisplay(template.getDisplay());
            condition.setCode(code);

            // Onset date (1-5 years ago)
            Date onsetDate = faker.date().past(365 * 5, TimeUnit.DAYS);
            condition.setOnset(new DateTimeType(onsetDate));

            bundle.addEntry()
                .setFullUrl("Condition/" + condition.getId())
                .setResource(condition);
        }
    }

    /**
     * Select conditions based on risk category and prevalence rates.
     */
    private List<ConditionTemplate> selectConditions(PatientGenerationContext context) {
        List<ConditionTemplate> conditions = new ArrayList<>();

        switch (context.getRiskCategory()) {
            case LOW:
                // 0-1 conditions
                if (random.nextDouble() < 0.3) { // 30% have hypertension
                    conditions.add(ConditionTemplates.HYPERTENSION);
                }
                break;

            case MODERATE:
                // 1-2 conditions
                if (random.nextDouble() < 0.6) { // 60% have hypertension
                    conditions.add(ConditionTemplates.HYPERTENSION);
                }
                if (random.nextDouble() < 0.4) { // 40% have diabetes
                    conditions.add(ConditionTemplates.DIABETES_TYPE_2);
                }
                break;

            case HIGH:
                // 3-5 conditions
                conditions.add(ConditionTemplates.HYPERTENSION);
                conditions.add(ConditionTemplates.DIABETES_TYPE_2);

                if (random.nextDouble() < 0.5) {
                    conditions.add(ConditionTemplates.CHF);
                }
                if (random.nextDouble() < 0.5) {
                    conditions.add(ConditionTemplates.CKD_STAGE_3);
                }
                if (random.nextDouble() < 0.3) {
                    conditions.add(ConditionTemplates.COPD);
                }
                break;
        }

        return conditions;
    }

    private void generateMedications(Patient patient, PatientGenerationContext context, Bundle bundle) {
        // Extract condition codes from bundle entries
        List<String> conditionCodes = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Condition)
            .map(e -> {
                Condition condition = (Condition) e.getResource();
                return condition.getCode().getCodingFirstRep().getCode();
            })
            .toList();

        if (!conditionCodes.isEmpty()) {
            medicationGenerator.generateMedications(patient, conditionCodes, bundle);
        }
    }

    private void generateObservations(Patient patient, PatientGenerationContext context, Bundle bundle) {
        // Extract condition codes from bundle entries
        List<String> conditionCodes = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Condition)
            .map(e -> {
                Condition condition = (Condition) e.getResource();
                return condition.getCode().getCodingFirstRep().getCode();
            })
            .toList();

        // Convert risk category to string
        String riskCategory = context.getRiskCategory().name();

        observationGenerator.generateObservations(patient, conditionCodes, riskCategory, bundle);
    }

    private void generateEncounters(Patient patient, PatientGenerationContext context, Bundle bundle) {
        // Convert risk category to string
        String riskCategory = context.getRiskCategory().name();
        encounterGenerator.generateEncounters(patient, riskCategory, bundle);
    }

    private void generateProcedures(Patient patient, PatientGenerationContext context, Bundle bundle) {
        // Calculate patient age
        LocalDate birthDate = patient.getBirthDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        int age = LocalDate.now().getYear() - birthDate.getYear();
        if (LocalDate.now().getDayOfYear() < birthDate.getDayOfYear()) {
            age--;
        }

        // Get gender
        String gender = patient.getGender() != null 
            ? patient.getGender().name() 
            : "UNKNOWN";

        // Extract condition codes from bundle entries
        List<String> conditionCodes = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Condition)
            .map(e -> {
                Condition condition = (Condition) e.getResource();
                return condition.getCode().getCodingFirstRep().getCode();
            })
            .toList();

        // Determine if we should create care gaps (30% of patients have gaps)
        boolean createCareGap = random.nextDouble() < 0.3;

        procedureGenerator.generateProcedures(patient, age, gender, conditionCodes, createCareGap, bundle);
    }

    private Patient createPatientFromTemplate(SyntheticPatientTemplate template, String tenantId) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());

        // Name
        HumanName name = new HumanName();
        name.setFamily(template.getLastName());
        name.addGiven(template.getFirstName());
        name.setUse(HumanName.NameUse.OFFICIAL);
        patient.addName(name);

        // Gender
        patient.setGender(switch (template.getGender()) {
            case MALE -> Enumerations.AdministrativeGender.MALE;
            case FEMALE -> Enumerations.AdministrativeGender.FEMALE;
            default -> Enumerations.AdministrativeGender.UNKNOWN;
        });

        // Birth date (calculate from age)
        LocalDate birthDate = LocalDate.now().minusYears(template.getAge())
            .minusDays(random.nextInt(365));
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // Address
        Address address = new Address();
        address.addLine(faker.address().streetAddress());
        address.setCity(faker.address().city());
        address.setState(faker.address().stateAbbr());
        address.setPostalCode(faker.address().zipCode());
        address.setCountry("USA");
        patient.addAddress(address);

        // Telecom
        ContactPoint phone = new ContactPoint();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setValue(faker.phoneNumber().phoneNumber());
        patient.addTelecom(phone);

        // Identifier - MRN
        Identifier mrn = new Identifier();
        mrn.setSystem("urn:oid:2.16.840.1.113883.4.1");
        mrn.setValue(generateMRN());
        patient.addIdentifier(mrn);

        // Extension for tenant context
        Extension tenantExt = new Extension();
        tenantExt.setUrl("http://healthdata.com/fhir/StructureDefinition/tenant-id");
        tenantExt.setValue(new StringType(tenantId));
        patient.addExtension(tenantExt);

        // Extension for HCC risk score
        Extension hccExt = new Extension();
        hccExt.setUrl("http://healthdata.com/fhir/StructureDefinition/hcc-risk-score");
        hccExt.setValue(new DecimalType(template.getHccScore()));
        patient.addExtension(hccExt);

        return patient;
    }

    private void applyTemplateConditions(Patient patient, SyntheticPatientTemplate template, Bundle bundle) {
        List<String> conditionCodes = parseConditionCodes(template.getConditions());
        
        for (String icd10Code : conditionCodes) {
            Condition condition = new Condition();
            condition.setId(UUID.randomUUID().toString());
            condition.setSubject(new Reference("Patient/" + patient.getId()));

            // Clinical status
            CodeableConcept clinicalStatus = new CodeableConcept();
            clinicalStatus.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active")
                .setDisplay("Active");
            condition.setClinicalStatus(clinicalStatus);

            // Verification status
            CodeableConcept verificationStatus = new CodeableConcept();
            verificationStatus.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed")
                .setDisplay("Confirmed");
            condition.setVerificationStatus(verificationStatus);

            // Condition code
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
                .setCode(icd10Code);
            condition.setCode(code);

            // Onset date (1-5 years ago)
            Date onsetDate = faker.date().past(365 * 5, TimeUnit.DAYS);
            condition.setOnset(new DateTimeType(onsetDate));

            bundle.addEntry()
                .setFullUrl("Condition/" + condition.getId())
                .setResource(condition);
        }
    }

    private void applyTemplateMedications(Patient patient, SyntheticPatientTemplate template, Bundle bundle) {
        List<String> conditionCodes = parseConditionCodes(template.getConditions());
        if (!conditionCodes.isEmpty()) {
            medicationGenerator.generateMedications(patient, conditionCodes, bundle);
        }
    }

    private void applyTemplateObservations(Patient patient, SyntheticPatientTemplate template, Bundle bundle) {
        List<String> conditionCodes = parseConditionCodes(template.getConditions());
        String riskCategory = template.getRiskCategory().name();
        observationGenerator.generateObservations(patient, conditionCodes, riskCategory, bundle);
    }

    /**
     * Parse condition codes from JSON string.
     */
    private List<String> parseConditionCodes(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.trim().isEmpty() || "[]".equals(conditionsJson.trim())) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, String>> conditions = objectMapper.readValue(
                conditionsJson, 
                new TypeReference<List<Map<String, String>>>() {}
            );
            return conditions.stream()
                .map(c -> c.get("code"))
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            logger.warn("Failed to parse condition codes from JSON: {}", conditionsJson, e);
            return Collections.emptyList();
        }
    }

    // Supporting classes

    public enum RiskCategory {
        LOW, MODERATE, HIGH
    }

    public enum AgeRange {
        YOUNG_ADULT, MIDDLE_AGE, SENIOR
    }

    public static class PatientGenerationContext {
        private RiskCategory riskCategory;
        private AgeRange ageRange;
        private BigDecimal hccScore;

        public RiskCategory getRiskCategory() { return riskCategory; }
        public void setRiskCategory(RiskCategory riskCategory) { this.riskCategory = riskCategory; }
        public AgeRange getAgeRange() { return ageRange; }
        public void setAgeRange(AgeRange ageRange) { this.ageRange = ageRange; }
        public BigDecimal getHccScore() { return hccScore; }
        public void setHccScore(BigDecimal hccScore) { this.hccScore = hccScore; }
    }

    public static class ConditionTemplate {
        private final String icd10Code;
        private final String display;

        public ConditionTemplate(String icd10Code, String display) {
            this.icd10Code = icd10Code;
            this.display = display;
        }

        public String getIcd10Code() { return icd10Code; }
        public String getDisplay() { return display; }
    }

    public static class ConditionTemplates {
        public static final ConditionTemplate DIABETES_TYPE_2 =
            new ConditionTemplate("E11.9", "Type 2 Diabetes Mellitus");
        public static final ConditionTemplate HYPERTENSION =
            new ConditionTemplate("I10", "Essential Hypertension");
        public static final ConditionTemplate CHF =
            new ConditionTemplate("I50.9", "Congestive Heart Failure");
        public static final ConditionTemplate COPD =
            new ConditionTemplate("J44.9", "Chronic Obstructive Pulmonary Disease");
        public static final ConditionTemplate CKD_STAGE_3 =
            new ConditionTemplate("N18.3", "Chronic Kidney Disease, Stage 3");
    }

    /**
     * @deprecated Use SyntheticPatientTemplate from domain model instead
     */
    @Deprecated
    public static class PatientTemplate {
        private String personaName;
        private Map<String, Object> attributes;

        public String getPersonaName() { return personaName; }
        public void setPersonaName(String personaName) { this.personaName = personaName; }
        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    }
}
