package com.healthdata.quality.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for all FHIR resources needed to calculate quality measures for a patient.
 * This aggregates data from the FHIR server into a single object for measure calculation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientData {

    /**
     * Patient demographics and identifiers
     */
    private Patient patient;

    /**
     * Patient's medical conditions (diagnoses)
     * Used for: Eligibility checks, exclusion criteria
     */
    @Builder.Default
    private List<Condition> conditions = new ArrayList<>();

    /**
     * Patient's observations (labs, vitals, etc.)
     * Used for: HbA1c values, blood pressure, test results
     */
    @Builder.Default
    private List<Observation> observations = new ArrayList<>();

    /**
     * Patient's procedures (surgeries, screenings, etc.)
     * Used for: Eye exams, colonoscopies, mammographies
     */
    @Builder.Default
    private List<Procedure> procedures = new ArrayList<>();

    /**
     * Patient's encounters (visits, admissions)
     * Used for: Hospice encounters, care setting determination
     */
    @Builder.Default
    private List<Encounter> encounters = new ArrayList<>();

    /**
     * Patient's active and historical medications
     * Used for: Medication adherence, therapy compliance
     */
    @Builder.Default
    private List<MedicationStatement> medicationStatements = new ArrayList<>();

    /**
     * Patient's immunization records
     * Used for: Immunization measures
     */
    @Builder.Default
    private List<Immunization> immunizations = new ArrayList<>();
}
