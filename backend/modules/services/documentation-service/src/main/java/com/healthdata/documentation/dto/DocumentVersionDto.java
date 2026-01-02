package com.healthdata.documentation.dto;

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
public class DocumentVersionDto {

    private UUID id;

    private String documentId;

    private String versionNumber;

    private String content;

    private String changeSummary;

    private String changedBy;

    private Boolean isMajorVersion;

    private Boolean isPublished;

    private LocalDateTime createdAt;
}
