package com.healthdata.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaDocumentDto {

    private UUID id;

    private UUID clinicalDocumentId;

    private String cdaType;

    private String templateId;

    private String rawXml;

    private Map<String, Object> parsedData;

    private String renderedHtml;

    private String validationStatus;

    private Map<String, Object> validationErrors;

    private String documentId;

    private String setId;

    private Integer versionNumber;

    private LocalDateTime effectiveTime;

    private LocalDateTime createdAt;
}
