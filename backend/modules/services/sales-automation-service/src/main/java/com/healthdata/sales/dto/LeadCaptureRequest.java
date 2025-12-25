package com.healthdata.sales.dto;

import com.healthdata.sales.entity.LeadSource;
import com.healthdata.sales.entity.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for public lead capture endpoints
 * Used by website forms, ROI calculator, and demo requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadCaptureRequest {

    private String firstName;
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String company;
    private String title;
    private String website;

    @NotNull(message = "Source is required")
    private LeadSource source;

    private OrganizationType organizationType;
    private Integer patientCount;
    private Integer ehrCount;
    private String state;
    private String notes;

    // ROI Calculator specific fields
    private Double estimatedSavings;
    private String currentEhrSystem;
    private Integer numberOfProviders;

    // UTM tracking
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
}
