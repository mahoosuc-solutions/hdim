package com.healthdata.sdoh.entity;

import com.healthdata.sdoh.model.SdohRiskScore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "sdoh_risk_scores", schema = "sdoh")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohRiskScoreEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String scoreId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Double totalScore;

    @Column(columnDefinition = "TEXT")
    private String categoryScoresJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SdohRiskScore.RiskLevel riskLevel;

    private String assessmentId;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
