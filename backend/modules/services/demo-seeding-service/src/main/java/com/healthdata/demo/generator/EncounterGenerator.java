package com.healthdata.demo.generator;

import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generates synthetic encounter history for demo patients.
 *
 * Encounter patterns are based on patient risk:
 * - Low risk: 1-2 office visits per year
 * - Moderate risk: 2-4 office visits, occasional urgent care
 * - High risk: 4-6 office visits, possible ER/hospitalization
 *
 * Encounter types include:
 * - Ambulatory (office visits)
 * - Emergency (ER visits)
 * - Inpatient (hospitalizations)
 */
@Component
public class EncounterGenerator {

    private final Random random = new Random();

    /**
     * Generate encounter history for a patient.
     *
     * @param patient The patient resource
     * @param riskCategory Patient risk category
     * @param bundle Bundle to add encounter resources to
     */
    public void generateEncounters(Patient patient, String riskCategory, Bundle bundle) {
        String patientRef = "Patient/" + patient.getId();

        // Determine number of encounters based on risk
        int encounterCount = determineEncounterCount(riskCategory);

        // Generate encounters over the past 2 years
        for (int i = 0; i < encounterCount; i++) {
            EncounterType type = selectEncounterType(riskCategory, i);
            Date encounterDate = generateEncounterDate(i, encounterCount);

            Encounter encounter = createEncounter(patientRef, type, encounterDate, riskCategory);
            bundle.addEntry()
                .setFullUrl("Encounter/" + encounter.getId())
                .setResource(encounter);
        }
    }

    /**
     * Determine number of encounters based on risk category.
     */
    private int determineEncounterCount(String riskCategory) {
        return switch (riskCategory) {
            case "LOW" -> 1 + random.nextInt(2); // 1-2
            case "MODERATE" -> 2 + random.nextInt(3); // 2-4
            case "HIGH" -> 4 + random.nextInt(4); // 4-7
            default -> 2 + random.nextInt(2);
        };
    }

    /**
     * Select encounter type based on risk and encounter index.
     */
    private EncounterType selectEncounterType(String riskCategory, int index) {
        // First encounter is usually office visit
        if (index == 0) {
            return EncounterType.AMBULATORY;
        }

        // High risk patients may have ER/inpatient encounters
        if ("HIGH".equals(riskCategory)) {
            double rand = random.nextDouble();
            if (rand < 0.15) { // 15% chance of hospitalization
                return EncounterType.INPATIENT;
            } else if (rand < 0.35) { // 20% chance of ER visit
                return EncounterType.EMERGENCY;
            }
        } else if ("MODERATE".equals(riskCategory)) {
            if (random.nextDouble() < 0.10) { // 10% chance of ER
                return EncounterType.EMERGENCY;
            }
        }

        // Default to office visit
        return EncounterType.AMBULATORY;
    }

    /**
     * Generate encounter date within the past 2 years.
     */
    private Date generateEncounterDate(int index, int totalEncounters) {
        Calendar cal = Calendar.getInstance();

        // Spread encounters over past 2 years (730 days)
        int daysRange = 730;
        int interval = daysRange / totalEncounters;
        int daysAgo = (totalEncounters - index - 1) * interval + random.nextInt(interval / 2);

        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return cal.getTime();
    }

    /**
     * Create a FHIR Encounter resource.
     */
    private Encounter createEncounter(String patientRef, EncounterType type,
                                       Date date, String riskCategory) {
        Encounter encounter = new Encounter();
        encounter.setId(UUID.randomUUID().toString());
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        // Class (ambulatory, emergency, inpatient)
        Coding classCoding = new Coding();
        classCoding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
        classCoding.setCode(type.getActCode());
        classCoding.setDisplay(type.getDisplayName());
        encounter.setClass_(classCoding);

        // Type
        CodeableConcept encounterType = new CodeableConcept();
        encounterType.addCoding()
            .setSystem("http://snomed.info/sct")
            .setCode(type.getSnomedCode())
            .setDisplay(type.getTypeDisplay());
        encounter.addType(encounterType);

        // Subject
        encounter.setSubject(new Reference(patientRef));

        // Period
        Period period = new Period();
        period.setStart(date);

        // Calculate end time based on encounter type
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(date);
        switch (type) {
            case AMBULATORY:
                endCal.add(Calendar.MINUTE, 30 + random.nextInt(30)); // 30-60 min
                break;
            case EMERGENCY:
                endCal.add(Calendar.HOUR, 2 + random.nextInt(4)); // 2-6 hours
                break;
            case INPATIENT:
                endCal.add(Calendar.DAY_OF_YEAR, 2 + random.nextInt(5)); // 2-7 days
                break;
        }
        period.setEnd(endCal.getTime());
        encounter.setPeriod(period);

        // Service provider (placeholder)
        encounter.setServiceProvider(new Reference("Organization/demo-org"));

        // Reason code (based on encounter type)
        CodeableConcept reason = new CodeableConcept();
        EncounterReason selectedReason = selectReasonForEncounter(type, riskCategory);
        reason.addCoding()
            .setSystem("http://snomed.info/sct")
            .setCode(selectedReason.snomedCode)
            .setDisplay(selectedReason.display);
        encounter.addReasonCode(reason);

        return encounter;
    }

    /**
     * Select a reason for the encounter based on type and risk.
     */
    private EncounterReason selectReasonForEncounter(EncounterType type, String riskCategory) {
        List<EncounterReason> reasons = switch (type) {
            case AMBULATORY -> Arrays.asList(
                new EncounterReason("185349003", "Routine health maintenance"),
                new EncounterReason("78843000", "Chronic disease management"),
                new EncounterReason("185317003", "Follow-up encounter"),
                new EncounterReason("308335008", "Medical review")
            );
            case EMERGENCY -> Arrays.asList(
                new EncounterReason("267036007", "Shortness of breath"),
                new EncounterReason("29857009", "Chest pain"),
                new EncounterReason("25064002", "Headache"),
                new EncounterReason("422587007", "Acute illness")
            );
            case INPATIENT -> Arrays.asList(
                new EncounterReason("233604007", "Exacerbation of condition"),
                new EncounterReason("84114007", "Heart failure exacerbation"),
                new EncounterReason("195951007", "COPD exacerbation"),
                new EncounterReason("68566005", "Urinary tract infection")
            );
        };

        return reasons.get(random.nextInt(reasons.size()));
    }

    /**
     * Encounter types.
     */
    public enum EncounterType {
        AMBULATORY("AMB", "ambulatory", "390906007", "Office visit"),
        EMERGENCY("EMER", "emergency", "4525004", "Emergency department visit"),
        INPATIENT("IMP", "inpatient encounter", "32485007", "Hospital admission");

        private final String actCode;
        private final String displayName;
        private final String snomedCode;
        private final String typeDisplay;

        EncounterType(String actCode, String displayName, String snomedCode, String typeDisplay) {
            this.actCode = actCode;
            this.displayName = displayName;
            this.snomedCode = snomedCode;
            this.typeDisplay = typeDisplay;
        }

        public String getActCode() { return actCode; }
        public String getDisplayName() { return displayName; }
        public String getSnomedCode() { return snomedCode; }
        public String getTypeDisplay() { return typeDisplay; }
    }

    /**
     * Encounter reason helper.
     */
    private record EncounterReason(String snomedCode, String display) {}
}
