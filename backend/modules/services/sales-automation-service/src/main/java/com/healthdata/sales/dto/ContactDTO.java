package com.healthdata.sales.dto;

import com.healthdata.sales.entity.ContactType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {

    private UUID id;
    private UUID tenantId;
    private UUID accountId;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String mobile;
    private String title;
    private String department;
    private ContactType contactType;
    private Boolean isPrimary;
    private Boolean doNotCall;
    private Boolean doNotEmail;
    private String linkedinUrl;
    private String notes;
    private String zohoContactId;
    private UUID ownerUserId;
    private LocalDateTime lastContactedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
