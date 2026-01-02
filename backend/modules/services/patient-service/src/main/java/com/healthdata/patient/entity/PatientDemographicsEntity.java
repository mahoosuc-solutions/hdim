package com.healthdata.patient.entity;

import com.healthdata.security.encryption.Encrypted;
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

    @Encrypted(value = "Medical Record Number", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "mrn", length = 200)
    private String mrn;

    @Encrypted(value = "Social Security Number", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "ssn_encrypted", length = 256)
    private String ssnEncrypted;

    @Encrypted(value = "Patient First Name", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "first_name", nullable = false, length = 356)
    private String firstName;

    @Encrypted(value = "Patient Middle Name", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "middle_name", length = 356)
    private String middleName;

    @Encrypted(value = "Patient Last Name", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "last_name", nullable = false, length = 356)
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

    @Encrypted(value = "Patient Email", category = Encrypted.HipaaCategory.PII)
    @Column(name = "email", length = 610)
    private String email;

    @Encrypted(value = "Patient Phone", category = Encrypted.HipaaCategory.PII)
    @Column(name = "phone", length = 140)
    private String phone;

    @Encrypted(value = "Patient Address Line 1", category = Encrypted.HipaaCategory.PII)
    @Column(name = "address_line1", length = 610)
    private String addressLine1;

    @Encrypted(value = "Patient Address Line 2", category = Encrypted.HipaaCategory.PII)
    @Column(name = "address_line2", length = 610)
    private String addressLine2;

    @Encrypted(value = "Patient City", category = Encrypted.HipaaCategory.PII)
    @Column(name = "city", length = 300)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Encrypted(value = "Patient Zip Code", category = Encrypted.HipaaCategory.PII)
    @Column(name = "zip_code", length = 140)
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
