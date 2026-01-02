package com.healthdata.sales.dto;

import com.healthdata.sales.entity.LeadSource;
import com.healthdata.sales.entity.LeadStatus;
import com.healthdata.sales.entity.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDTO {

    private UUID id;
    private UUID tenantId;

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

    private LeadStatus status;
    private OrganizationType organizationType;
    private Integer patientCount;
    private Integer ehrCount;
    private String state;
    private Integer score;
    private String notes;
    private String zohoLeadId;
    private UUID assignedToUserId;
    private LocalDateTime lastContactedAt;
    private LocalDateTime convertedAt;
    private UUID convertedContactId;
    private UUID convertedOpportunityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
