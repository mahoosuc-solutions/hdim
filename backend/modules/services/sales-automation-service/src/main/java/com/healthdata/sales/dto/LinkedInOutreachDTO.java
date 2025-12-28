package com.healthdata.sales.dto;

import com.healthdata.sales.entity.LinkedInOutreach.OutreachStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInOutreachDTO {

    private UUID id;
    private UUID tenantId;
    private UUID leadId;
    private UUID contactId;

    private String linkedinProfileUrl;
    private String targetName;
    private String targetTitle;
    private String targetCompany;

    private OutreachType outreachType;
    private OutreachStatus status;

    private String messageContent;
    private String connectionNote;
    private String campaignName;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;

    private Boolean connectionAccepted;
    private LocalDateTime acceptedAt;

    private Boolean replied;
    private LocalDateTime repliedAt;

    private LocalDateTime createdAt;
}
