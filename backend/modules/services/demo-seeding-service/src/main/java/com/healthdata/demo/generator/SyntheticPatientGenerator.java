package com.healthdata.demo.generator;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.demo.domain.model.SyntheticPatientTemplate;
import net.datafaker.Faker;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final double pediatricPct;
    private final double geriatricPct;
    private final double chronicPct;
    private final double behavioralPct;
    private final double preventivePct;

    public SyntheticPatientGenerator(
            FhirContext fhirContext,
            MedicationGenerator medicationGenerator,
            ObservationGenerator observationGenerator,
            EncounterGenerator encounterGenerator,
            ProcedureGenerator procedureGenerator,
            ObjectMapper objectMapper,
            @Value("${demo.population.pediatric-pct:0.10}") double pediatricPct,
            @Value("${demo.population.geriatric-pct:0.15}") double geriatricPct,
            @Value("${demo.population.chronic-pct:0.30}") double chronicPct,
            @Value("${demo.population.behavioral-pct:0.12}") double behavioralPct,
            @Value("${demo.population.preventive-pct:0.12}") double preventivePct) {
        this.fhirContext = fhirContext;
        this.faker = new Faker();
        this.random = new Random();
        this.medicationGenerator = medicationGenerator;
        this.observationGenerator = observationGenerator;
        this.encounterGenerator = encounterGenerator;
        this.procedureGenerator = procedureGenerator;
        this.objectMapper = objectMapper;
        this.pediatricPct = clampPct(pediatricPct);
        this.geriatricPct = clampPct(geriatricPct);
        this.chronicPct = clampPct(chronicPct);
        this.behavioralPct = clampPct(behavioralPct);
        this.preventivePct = clampPct(preventivePct);
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

        List<PatientGenerationContext> contexts = buildCohortContexts(count);
        for (int i = 0; i < contexts.size(); i++) {
            PatientGenerationContext context = contexts.get(i);
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
     * Build a cohort with configured demographic and clinical distribution.
     */
    private List<PatientGenerationContext> buildCohortContexts(int total) {
        List<PatientGenerationContext> contexts = new ArrayList<>(total);

        int pediatricCount = Math.round((float) (total * pediatricPct));
        int geriatricCount = Math.round((float) (total * geriatricPct));
        int chronicCount = Math.round((float) (total * chronicPct));
        int behavioralCount = Math.round((float) (total * behavioralPct));
        int preventiveCount = Math.round((float) (total * preventivePct));

        int reserved = pediatricCount + geriatricCount + chronicCount + behavioralCount + preventiveCount;
        if (reserved > total) {
            double scale = (double) total / Math.max(1, reserved);
            pediatricCount = (int) Math.floor(pediatricCount * scale);
            geriatricCount = (int) Math.floor(geriatricCount * scale);
            chronicCount = (int) Math.floor(chronicCount * scale);
            behavioralCount = (int) Math.floor(behavioralCount * scale);
            preventiveCount = (int) Math.floor(preventiveCount * scale);
            reserved = pediatricCount + geriatricCount + chronicCount + behavioralCount + preventiveCount;
        }
        int generalCount = Math.max(0, total - reserved);

        addContexts(contexts, CohortCategory.PEDIATRIC, pediatricCount);
        addContexts(contexts, CohortCategory.GERIATRIC, geriatricCount);
        addContexts(contexts, CohortCategory.ADULT_CHRONIC, chronicCount);
        addContexts(contexts, CohortCategory.ADULT_BEHAVIORAL, behavioralCount);
        addContexts(contexts, CohortCategory.ADULT_PREVENTIVE, preventiveCount);
        addContexts(contexts, CohortCategory.ADULT_GENERAL, generalCount);

        Collections.shuffle(contexts, random);
        return contexts;
    }

    private void addContexts(List<PatientGenerationContext> contexts, CohortCategory category, int count) {
        for (int i = 0; i < count; i++) {
            contexts.add(createContextForCategory(category));
        }
    }

    private PatientGenerationContext createContextForCategory(CohortCategory category) {
        PatientGenerationContext context = new PatientGenerationContext();
        context.setCohortCategory(category);

        switch (category) {
            case PEDIATRIC -> {
                context.setAgeRange(AgeRange.PEDIATRIC);
                context.setRiskCategory(RiskCategory.LOW);
                context.setHccScore(generateHccScore(RiskCategory.LOW));
            }
            case GERIATRIC -> {
                context.setAgeRange(AgeRange.SENIOR);
                RiskCategory risk = random.nextDouble() < 0.4 ? RiskCategory.HIGH : RiskCategory.MODERATE;
                context.setRiskCategory(risk);
                context.setHccScore(generateHccScore(risk));
            }
            case ADULT_CHRONIC -> {
                context.setAgeRange(selectAdultAgeRange());
                RiskCategory risk = random.nextDouble() < 0.5 ? RiskCategory.HIGH : RiskCategory.MODERATE;
                context.setRiskCategory(risk);
                context.setHccScore(generateHccScore(risk));
            }
            case ADULT_BEHAVIORAL -> {
                context.setAgeRange(selectAdultAgeRange());
                RiskCategory risk = random.nextDouble() < 0.7 ? RiskCategory.MODERATE : RiskCategory.LOW;
                context.setRiskCategory(risk);
                context.setHccScore(generateHccScore(risk));
            }
            case ADULT_PREVENTIVE -> {
                context.setAgeRange(selectAdultAgeRange());
                context.setRiskCategory(RiskCategory.LOW);
                context.setHccScore(generateHccScore(RiskCategory.LOW));
            }
            case ADULT_GENERAL -> {
                context.setAgeRange(selectAdultAgeRange());
                RiskCategory risk = random.nextDouble() < 0.6 ? RiskCategory.LOW : RiskCategory.MODERATE;
                context.setRiskCategory(risk);
                context.setHccScore(generateHccScore(risk));
            }
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
            case PEDIATRIC -> 0;
            case YOUNG_ADULT -> 18;
            case MIDDLE_AGE -> 45;
            case SENIOR -> 65;
        };
        int maxAge = switch (ageRange) {
            case PEDIATRIC -> 17;
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

        switch (context.getCohortCategory()) {
            case PEDIATRIC -> {
                if (random.nextDouble() < 0.15) {
                    conditions.add(ConditionTemplates.ASTHMA);
                }
            }
            case GERIATRIC -> {
                if (random.nextDouble() < 0.6) {
                    conditions.add(ConditionTemplates.HYPERTENSION);
                }
                if (random.nextDouble() < 0.25) {
                    conditions.add(ConditionTemplates.CKD_STAGE_3);
                }
            }
            case ADULT_CHRONIC -> addChronicConditions(conditions, context.getRiskCategory());
            case ADULT_BEHAVIORAL -> {
                conditions.add(ConditionTemplates.DEPRESSION);
                if (random.nextDouble() < 0.5) {
                    conditions.add(ConditionTemplates.ANXIETY);
                }
            }
            case ADULT_PREVENTIVE -> {
                // No chronic conditions for preventive-only patients.
            }
            case ADULT_GENERAL -> {
                if (random.nextDouble() < 0.2) {
                    conditions.add(ConditionTemplates.HYPERTENSION);
                }
            }
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
        PEDIATRIC, YOUNG_ADULT, MIDDLE_AGE, SENIOR
    }

    public enum CohortCategory {
        PEDIATRIC,
        GERIATRIC,
        ADULT_CHRONIC,
        ADULT_BEHAVIORAL,
        ADULT_PREVENTIVE,
        ADULT_GENERAL
    }

    public static class PatientGenerationContext {
        private RiskCategory riskCategory;
        private AgeRange ageRange;
        private BigDecimal hccScore;
        private CohortCategory cohortCategory;

        public RiskCategory getRiskCategory() { return riskCategory; }
        public void setRiskCategory(RiskCategory riskCategory) { this.riskCategory = riskCategory; }
        public AgeRange getAgeRange() { return ageRange; }
        public void setAgeRange(AgeRange ageRange) { this.ageRange = ageRange; }
        public BigDecimal getHccScore() { return hccScore; }
        public void setHccScore(BigDecimal hccScore) { this.hccScore = hccScore; }
        public CohortCategory getCohortCategory() { return cohortCategory; }
        public void setCohortCategory(CohortCategory cohortCategory) { this.cohortCategory = cohortCategory; }
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
        public static final ConditionTemplate DEPRESSION =
            new ConditionTemplate("F32.9", "Major Depressive Disorder, Single Episode");
        public static final ConditionTemplate ANXIETY =
            new ConditionTemplate("F41.9", "Anxiety Disorder, Unspecified");
        public static final ConditionTemplate ASTHMA =
            new ConditionTemplate("J45.909", "Asthma, Unspecified, Uncomplicated");
    }

    private AgeRange selectAdultAgeRange() {
        return random.nextDouble() < 0.5 ? AgeRange.YOUNG_ADULT : AgeRange.MIDDLE_AGE;
    }

    private BigDecimal generateHccScore(RiskCategory riskCategory) {
        return switch (riskCategory) {
            case LOW -> BigDecimal.valueOf(0.5 + (random.nextDouble() * 0.5));
            case MODERATE -> BigDecimal.valueOf(1.0 + (random.nextDouble() * 1.0));
            case HIGH -> BigDecimal.valueOf(2.0 + (random.nextDouble() * 2.0));
        };
    }

    private void addChronicConditions(List<ConditionTemplate> conditions, RiskCategory riskCategory) {
        switch (riskCategory) {
            case LOW -> {
                if (random.nextDouble() < 0.5) {
                    conditions.add(ConditionTemplates.HYPERTENSION);
                }
            }
            case MODERATE -> {
                conditions.add(ConditionTemplates.HYPERTENSION);
                if (random.nextDouble() < 0.6) {
                    conditions.add(ConditionTemplates.DIABETES_TYPE_2);
                }
            }
            case HIGH -> {
                conditions.add(ConditionTemplates.HYPERTENSION);
                conditions.add(ConditionTemplates.DIABETES_TYPE_2);
                if (random.nextDouble() < 0.6) {
                    conditions.add(ConditionTemplates.CHF);
                }
                if (random.nextDouble() < 0.6) {
                    conditions.add(ConditionTemplates.CKD_STAGE_3);
                }
                if (random.nextDouble() < 0.4) {
                    conditions.add(ConditionTemplates.COPD);
                }
            }
        }
    }

    private double clampPct(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
