package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Community Resource model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityResource {
    private String resourceId;
    private String organizationName;
    private ResourceCategory category;
    private String description;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNumber;
    private String email;
    private String website;
    private List<String> servicesProvided;
    private String eligibilityCriteria;
    private String hoursOfOperation;
    private Double latitude;
    private Double longitude;
    private boolean acceptsWalkIns;
    private boolean requiresReferral;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
