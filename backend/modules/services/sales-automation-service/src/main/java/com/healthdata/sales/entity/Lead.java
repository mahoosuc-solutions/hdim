package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lead entity representing a potential customer
 */
@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_tenant", columnList = "tenant_id"),
    @Index(name = "idx_leads_email", columnList = "email"),
    @Index(name = "idx_leads_status", columnList = "status"),
    @Index(name = "idx_leads_source", columnList = "source"),
    @Index(name = "idx_leads_zoho", columnList = "zoho_lead_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "company", length = 255)
    private String company;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "website", length = 255)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type")
    private OrganizationType organizationType;

    @Column(name = "patient_count")
    private Integer patientCount;

    @Column(name = "ehr_count")
    private Integer ehrCount;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "score")
    private Integer score;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "zoho_lead_id", length = 100)
    private String zohoLeadId;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "converted_contact_id")
    private UUID convertedContactId;

    @Column(name = "converted_opportunity_id")
    private UUID convertedOpportunityId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Calculate lead score based on organization type and size
     */
    public void calculateScore() {
        int calculatedScore = 0;

        // Organization type scoring
        if (organizationType != null) {
            switch (organizationType) {
                case ACO -> calculatedScore += 30;
                case HEALTH_SYSTEM -> calculatedScore += 25;
                case PAYER -> calculatedScore += 35;
                case HIE -> calculatedScore += 20;
                case FQHC -> calculatedScore += 15;
            }
        }

        // Patient count scoring
        if (patientCount != null) {
            if (patientCount >= 100000) calculatedScore += 30;
            else if (patientCount >= 50000) calculatedScore += 25;
            else if (patientCount >= 25000) calculatedScore += 20;
            else if (patientCount >= 10000) calculatedScore += 15;
            else if (patientCount >= 5000) calculatedScore += 10;
            else calculatedScore += 5;
        }

        // EHR count scoring (multi-EHR = more value)
        if (ehrCount != null && ehrCount > 1) {
            calculatedScore += Math.min(ehrCount * 5, 20);
        }

        // Source scoring
        if (source != null) {
            switch (source) {
                case DEMO_REQUEST -> calculatedScore += 15;
                case ROI_CALCULATOR -> calculatedScore += 10;
                case REFERRAL -> calculatedScore += 10;
                case CONFERENCE -> calculatedScore += 5;
                case WEBSITE -> calculatedScore += 3;
            }
        }

        this.score = Math.min(calculatedScore, 100);
    }
}
