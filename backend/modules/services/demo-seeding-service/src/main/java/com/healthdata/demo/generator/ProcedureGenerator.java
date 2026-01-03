package com.healthdata.demo.generator;

import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Generates synthetic procedure data for demo patients.
 *
 * Critical for quality measure demonstrations:
 * - BCS: Mammograms (CPT 77067, 77063)
 * - COL: Colonoscopies (CPT 45378, 45380)
 * - EED: Diabetic eye exams (CPT 92250, 92134)
 * - Immunizations (CVX codes)
 *
 * Procedures are generated to create realistic care gap distributions:
 * - Some patients are compliant (procedure within required timeframe)
 * - Some patients have gaps (procedure overdue)
 * - Some patients have never had the procedure
 */
@Component
public class ProcedureGenerator {

    private final Random random = new Random();

    /**
     * Generate procedures for a patient, considering quality measure compliance.
     *
     * @param patient The patient resource
     * @param age Patient age (for age-appropriate procedures)
     * @param gender Patient gender (for gender-specific procedures)
     * @param conditions List of condition codes
     * @param createCareGap If true, intentionally create a care gap
     * @param bundle Bundle to add procedure resources to
     */
    public void generateProcedures(Patient patient, int age, String gender,
                                    List<String> conditions, boolean createCareGap,
                                    Bundle bundle) {
        String patientRef = "Patient/" + patient.getId();

        // Breast Cancer Screening (BCS) - Women 50-74
        if ("FEMALE".equalsIgnoreCase(gender) && age >= 50 && age <= 74) {
            generateBCSProcedure(patientRef, createCareGap, bundle);
        }

        // Colorectal Cancer Screening (COL) - Adults 50-75
        if (age >= 50 && age <= 75) {
            generateCOLProcedure(patientRef, createCareGap, bundle);
        }

        // Diabetic Eye Exam (EED) - Diabetics
        if (conditions.contains("E11.9")) {
            generateEEDProcedure(patientRef, createCareGap, bundle);
        }

        // Annual wellness visit
        if (random.nextDouble() < 0.7) { // 70% have annual wellness
            generateAnnualWellnessVisit(patientRef, bundle);
        }

        // Flu shot (seasonal)
        if (random.nextDouble() < 0.5) { // 50% vaccination rate
            generateFluVaccination(patientRef, bundle);
        }
    }

    /**
     * Generate Breast Cancer Screening procedure (mammogram).
     */
    private void generateBCSProcedure(String patientRef, boolean createCareGap, Bundle bundle) {
        // BCS requires mammogram within 27 months
        Date procedureDate;

        if (createCareGap) {
            // Create care gap: mammogram more than 27 months ago (28-40 months)
            procedureDate = getDateMonthsAgo(28 + random.nextInt(12));
        } else {
            // Compliant: mammogram within 27 months
            procedureDate = getDateMonthsAgo(random.nextInt(24)); // 0-24 months ago
        }

        Procedure procedure = createProcedure(
            patientRef,
            "77067",
            "Screening mammography, bilateral",
            "http://www.ama-assn.org/go/cpt",
            procedureDate
        );
        bundle.addEntry()
            .setFullUrl("Procedure/" + procedure.getId())
            .setResource(procedure);
    }

    /**
     * Generate Colorectal Cancer Screening procedure (colonoscopy).
     */
    private void generateCOLProcedure(String patientRef, boolean createCareGap, Bundle bundle) {
        // COL requires colonoscopy within 10 years, FIT within 1 year, etc.
        // For demo, we'll use colonoscopy (simplest to demonstrate)

        Date procedureDate;

        if (createCareGap) {
            // Care gap: colonoscopy more than 10 years ago
            procedureDate = getDateYearsAgo(11 + random.nextInt(5));
        } else if (random.nextDouble() < 0.7) {
            // Compliant via colonoscopy
            procedureDate = getDateYearsAgo(random.nextInt(9)); // 0-9 years ago
        } else {
            // Never had procedure (care gap)
            return;
        }

        Procedure procedure = createProcedure(
            patientRef,
            "45378",
            "Colonoscopy, diagnostic",
            "http://www.ama-assn.org/go/cpt",
            procedureDate
        );
        bundle.addEntry()
            .setFullUrl("Procedure/" + procedure.getId())
            .setResource(procedure);
    }

    /**
     * Generate Diabetic Eye Exam procedure.
     */
    private void generateEEDProcedure(String patientRef, boolean createCareGap, Bundle bundle) {
        // EED requires annual dilated eye exam

        Date procedureDate;

        if (createCareGap) {
            // Care gap: exam more than 12 months ago
            procedureDate = getDateMonthsAgo(13 + random.nextInt(12));
        } else {
            // Compliant: exam within 12 months
            procedureDate = getDateMonthsAgo(random.nextInt(11));
        }

        Procedure procedure = createProcedure(
            patientRef,
            "92250",
            "Fundus photography with interpretation",
            "http://www.ama-assn.org/go/cpt",
            procedureDate
        );
        bundle.addEntry()
            .setFullUrl("Procedure/" + procedure.getId())
            .setResource(procedure);
    }

    /**
     * Generate Annual Wellness Visit.
     */
    private void generateAnnualWellnessVisit(String patientRef, Bundle bundle) {
        Date procedureDate = getDateMonthsAgo(random.nextInt(14)); // 0-14 months ago

        Procedure procedure = createProcedure(
            patientRef,
            "G0438",
            "Annual wellness visit, initial",
            "http://www.ama-assn.org/go/cpt",
            procedureDate
        );
        bundle.addEntry()
            .setFullUrl("Procedure/" + procedure.getId())
            .setResource(procedure);
    }

    /**
     * Generate Flu Vaccination.
     */
    private void generateFluVaccination(String patientRef, Bundle bundle) {
        // Flu season: September to March
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);

        // If we're in flu season or just after, vaccine was this season
        // Otherwise, vaccine was last season
        if (currentMonth >= Calendar.SEPTEMBER || currentMonth <= Calendar.MARCH) {
            cal.set(Calendar.MONTH, Calendar.OCTOBER + random.nextInt(3));
        } else {
            cal.add(Calendar.YEAR, -1);
            cal.set(Calendar.MONTH, Calendar.OCTOBER + random.nextInt(3));
        }
        cal.set(Calendar.DAY_OF_MONTH, 1 + random.nextInt(28));

        Immunization immunization = createImmunization(
            patientRef,
            "140",  // CVX code for influenza
            "Influenza, seasonal, injectable",
            cal.getTime()
        );
        bundle.addEntry()
            .setFullUrl("Immunization/" + immunization.getId())
            .setResource(immunization);
    }

    /**
     * Create a FHIR Procedure resource.
     */
    private Procedure createProcedure(String patientRef, String code, String display,
                                       String codeSystem, Date performedDate) {
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID().toString());
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);

        // Subject
        procedure.setSubject(new Reference(patientRef));

        // Code
        CodeableConcept procedureCode = new CodeableConcept();
        procedureCode.addCoding()
            .setSystem(codeSystem)
            .setCode(code)
            .setDisplay(display);
        procedure.setCode(procedureCode);

        // Performed date
        procedure.setPerformed(new DateTimeType(performedDate));

        // Category
        CodeableConcept category = new CodeableConcept();
        category.addCoding()
            .setSystem("http://snomed.info/sct")
            .setCode("103693007")
            .setDisplay("Diagnostic procedure");
        procedure.setCategory(category);

        return procedure;
    }

    /**
     * Create a FHIR Immunization resource.
     */
    private Immunization createImmunization(String patientRef, String cvxCode,
                                             String display, Date occurrenceDate) {
        Immunization immunization = new Immunization();
        immunization.setId(UUID.randomUUID().toString());
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);

        // Patient
        immunization.setPatient(new Reference(patientRef));

        // Vaccine code
        CodeableConcept vaccineCode = new CodeableConcept();
        vaccineCode.addCoding()
            .setSystem("http://hl7.org/fhir/sid/cvx")
            .setCode(cvxCode)
            .setDisplay(display);
        immunization.setVaccineCode(vaccineCode);

        // Occurrence
        immunization.setOccurrence(new DateTimeType(occurrenceDate));

        // Primary source
        immunization.setPrimarySource(true);

        return immunization;
    }

    // Date helper methods

    private Date getDateMonthsAgo(int months) {
        LocalDate date = LocalDate.now().minusMonths(months);
        // Add some day variance
        date = date.minusDays(random.nextInt(28));
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date getDateYearsAgo(int years) {
        LocalDate date = LocalDate.now().minusYears(years);
        // Add some month/day variance
        date = date.minusMonths(random.nextInt(6)).minusDays(random.nextInt(28));
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Calculate if patient has a care gap for a specific measure.
     *
     * @param measureCode The quality measure code (BCS, COL, EED, etc.)
     * @param procedureDate Date of the most recent relevant procedure
     * @return true if there is a care gap
     */
    public boolean hasCareGap(String measureCode, Date procedureDate) {
        if (procedureDate == null) {
            return true; // No procedure = care gap
        }

        LocalDate procedureLocalDate = procedureDate.toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();

        return switch (measureCode) {
            case "BCS" -> procedureLocalDate.plusMonths(27).isBefore(now);
            case "COL" -> procedureLocalDate.plusYears(10).isBefore(now);
            case "EED" -> procedureLocalDate.plusMonths(12).isBefore(now);
            default -> false;
        };
    }
}
