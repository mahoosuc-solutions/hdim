package com.healthdata.sdoh.entity;

import com.healthdata.sdoh.model.SdohCategory;
import com.healthdata.sdoh.model.SdohDiagnosis;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "sdoh_diagnoses", schema = "sdoh")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohDiagnosisEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String diagnosisId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String zCode;

    @Column(nullable = false)
    private String zCodeDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SdohCategory category;

    @Column(columnDefinition = "TEXT")
    private String clinicalNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SdohDiagnosis.DiagnosisStatus status;

    @Column(nullable = false)
    private LocalDateTime diagnosisDate;

    private String diagnosedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
