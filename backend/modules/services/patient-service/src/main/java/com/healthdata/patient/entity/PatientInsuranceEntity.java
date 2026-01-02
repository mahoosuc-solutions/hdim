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
 * Patient Insurance Entity
 *
 * Stores insurance coverage information per patient.
 */
@Entity
@Table(name = "patient_insurance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientInsuranceEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "coverage_type", nullable = false, length = 50)
    private String coverageType;

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "payer_id", length = 128)
    private String payerId;

    @Column(name = "plan_name", length = 255)
    private String planName;

    @Encrypted(value = "Insurance Member ID", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "member_id", nullable = false, length = 356)
    private String memberId;

    @Encrypted(value = "Insurance Group Number", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "group_number", length = 356)
    private String groupNumber;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

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
