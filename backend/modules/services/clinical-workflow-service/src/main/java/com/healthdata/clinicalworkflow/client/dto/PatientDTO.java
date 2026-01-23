package com.healthdata.clinicalworkflow.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Patient Data Transfer Object
 *
 * Minimal patient information for vital signs alerts and clinical workflows.
 * Contains only essential fields needed for patient identification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {

    /**
     * Patient unique identifier
     */
    private UUID id;

    /**
     * Patient first name
     */
    private String firstName;

    /**
     * Patient last name
     */
    private String lastName;

    /**
     * Medical record number (MRN)
     */
    private String mrn;

    /**
     * Get formatted patient name
     *
     * @return Full name in "LastName, FirstName" format
     */
    public String getFormattedName() {
        if (firstName == null && lastName == null) {
            return "Unknown Patient";
        }
        if (lastName == null) {
            return firstName;
        }
        if (firstName == null) {
            return lastName;
        }
        return lastName + ", " + firstName;
    }
}
