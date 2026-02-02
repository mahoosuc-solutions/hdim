package com.healthdata.investor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a contact (investor, partner, advisor) in the investor dashboard.
 * Tracks contact information, status, and relationship details.
 */
@Entity
@Table(name = "investor_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "organization", length = 255)
    private String organization;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "linkedin_url", length = 500)
    private String linkedInUrl;

    @Column(name = "linkedin_profile_id", length = 100)
    private String linkedInProfileId;  // LinkedIn member ID for API integration

    @Column(name = "category", nullable = false, length = 50)
    private String category;  // VC, Angel, Strategic, Advisor, Partner

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "identified";  // identified, contacted, engaged, meeting_scheduled, follow_up, declined, invested

    @Column(name = "tier", nullable = false, length = 10)
    @Builder.Default
    private String tier = "B";  // A, B, C (priority tier)

    @Column(name = "investment_thesis", columnDefinition = "TEXT")
    private String investmentThesis;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_contacted")
    private Instant lastContacted;

    @Column(name = "next_follow_up")
    private Instant nextFollowUp;

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OutreachActivity> activities = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasLinkedInProfile() {
        return linkedInProfileId != null && !linkedInProfileId.isBlank();
    }

    public int getActivityCount() {
        return activities != null ? activities.size() : 0;
    }
}
