package com.healthdata.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Patient Demographics Entity
 *
 * Stores enhanced patient demographics and contact details.
 */
@Entity
@Table(name = "patient_demographics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDemographicsEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "fhir_patient_id", nullable = false, length = 64)
    private String fhirPatientId;

    @Column(name = "mrn", length = 50)
    private String mrn;

    @Column(name = "ssn_encrypted", length = 256)
    private String ssnEncrypted;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "middle_name", length = 128)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "race", length = 50)
    private String race;

    @Column(name = "ethnicity", length = 50)
    private String ethnicity;

    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "deceased", nullable = false)
    @Builder.Default
    private Boolean deceased = false;

    @Column(name = "deceased_date")
    private LocalDate deceasedDate;

    @Column(name = "pcp_id", length = 64)
    private String pcpId;

    @Column(name = "pcp_name", length = 255)
    private String pcpName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
