package com.healthdata.sdoh.entity;

import com.healthdata.sdoh.model.ResourceCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_resources", schema = "sdoh")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityResourceEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String resourceId;

    @Column(nullable = false)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNumber;
    private String email;
    private String website;

    @Column(columnDefinition = "TEXT")
    private String servicesProvidedJson;

    @Column(columnDefinition = "TEXT")
    private String eligibilityCriteria;

    private String hoursOfOperation;
    private Double latitude;
    private Double longitude;

    @Column(name = "accepts_walk_ins", nullable = false)
    private boolean acceptsWalkIns;

    @Column(name = "requires_referral", nullable = false)
    private boolean requiresReferral;

    private String language;

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
