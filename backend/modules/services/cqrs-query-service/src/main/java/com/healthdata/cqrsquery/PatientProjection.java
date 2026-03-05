package com.healthdata.cqrsquery;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Patient projection for CQRS read model.
 * Denormalized view optimized for query performance.
 */
public class PatientProjection {
    private final String patientId;
    private final String firstName;
    private final String lastName;
    private LocalDate dateOfBirth;
    private final Set<String> conditions = new HashSet<>();
    private final Set<String> medications = new HashSet<>();
    private String enrollmentStatus = "UNKNOWN";

    public PatientProjection(String patientId, String firstName, String lastName) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void addCondition(String condition) { conditions.add(condition); }
    public void addMedication(String medication) { medications.add(medication); }
    public void setEnrollmentStatus(String status) { this.enrollmentStatus = status; }
    public void setDateOfBirth(LocalDate dob) { this.dateOfBirth = dob; }

    public String getPatientId() { return patientId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Set<String> getConditions() { return conditions; }
    public Set<String> getMedications() { return medications; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
}
