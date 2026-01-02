package com.healthdata.sales.dto;

import com.healthdata.sales.entity.AccountStage;
import com.healthdata.sales.entity.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

    private UUID id;
    private UUID tenantId;

    @NotBlank(message = "Name is required")
    private String name;

    private OrganizationType organizationType;
    private String website;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private Integer patientCount;
    private Integer ehrCount;
    private String ehrSystems;
    private AccountStage stage;
    private Long annualRevenue;
    private Integer employeeCount;
    private String industry;
    private String description;
    private String zohoAccountId;
    private UUID ownerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
