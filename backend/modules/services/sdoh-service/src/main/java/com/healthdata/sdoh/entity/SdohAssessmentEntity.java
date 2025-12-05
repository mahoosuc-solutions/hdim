package com.healthdata.sdoh.entity;

import com.healthdata.sdoh.model.SdohAssessment;
import com.healthdata.sdoh.model.SdohCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "sdoh_assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohAssessmentEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String assessmentId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime assessmentDate;

    @Column(nullable = false)
    private String screeningTool;

    @Column(columnDefinition = "TEXT")
    private String responsesJson;

    @Column(columnDefinition = "TEXT")
    private String identifiedNeedsJson;

    @Column(columnDefinition = "TEXT")
    private String identifiedZCodesJson;

    private Double riskScore;

    private String assessedBy;

    @Enumerated(EnumType.STRING)
    private SdohAssessment.AssessmentStatus status;

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
