package com.healthdata.cqrsquery;

import java.time.LocalDate;
import java.util.*;

/**
 * In-memory projection store for CQRS read model.
 * Placeholder implementation — production would use Spring Data repositories
 * backed by the event-sourcing module's projection tables.
 */
public class MockProjectionStore {
    private final Map<String, List<PatientProjection>> data = new HashMap<>();

    public void addPatient(String tenantId, String patientId, String firstName, String lastName) {
        data.computeIfAbsent(tenantId, k -> new ArrayList<>())
            .add(new PatientProjection(patientId, firstName, lastName));
    }

    public void addPatientWithDOB(String tenantId, String patientId, String firstName, String lastName, LocalDate dob) {
        PatientProjection p = new PatientProjection(patientId, firstName, lastName);
        p.setDateOfBirth(dob);
        data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
    }

    public void addPatientWithCondition(String tenantId, String patientId, String firstName, String lastName, String condition) {
        PatientProjection p = new PatientProjection(patientId, firstName, lastName);
        p.addCondition(condition);
        data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
    }

    public void addPatientWithMedication(String tenantId, String patientId, String firstName, String lastName, String medication) {
        PatientProjection p = new PatientProjection(patientId, firstName, lastName);
        p.addMedication(medication);
        data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
    }

    public void addPatientWithEnrollment(String tenantId, String patientId, String firstName, String lastName, String status) {
        PatientProjection p = new PatientProjection(patientId, firstName, lastName);
        p.setEnrollmentStatus(status);
        data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
    }

    public void addComplexPatient(String tenantId, String patientId, String firstName, String lastName,
                                  String condition, String medication, String enrollmentStatus) {
        PatientProjection p = new PatientProjection(patientId, firstName, lastName);
        p.addCondition(condition);
        p.addMedication(medication);
        p.setEnrollmentStatus(enrollmentStatus);
        data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
    }

    public List<PatientProjection> getPatients(String tenantId) {
        return data.getOrDefault(tenantId, new ArrayList<>());
    }
}
