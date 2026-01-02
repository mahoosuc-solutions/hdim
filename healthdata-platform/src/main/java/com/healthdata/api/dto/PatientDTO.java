package com.healthdata.api.dto;

import com.healthdata.patient.domain.Patient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient Data Transfer Object (DTO)
 * Used for transferring patient data to/from API endpoints
 * Includes patient information with associations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDTO {

    // Core Patient Information
    private String id;
    private String mrn;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String gender;

    // Contact Information
    private Patient.Address address;
    private String phoneNumber;
    private String email;

    // System Fields
    private String tenantId;
    private boolean active;

    // Computed Fields
    private int age;
    private String fullName;

    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
