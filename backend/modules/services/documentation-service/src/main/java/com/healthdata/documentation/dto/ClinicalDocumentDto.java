package com.healthdata.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDocumentDto {

    private UUID id;

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Document type is required")
    private String documentType;

    private String documentTypeCode;

    private String documentTypeSystem;

    private String status;

    private String title;

    private String description;

    private String authorReference;

    private String authorName;

    private String custodianReference;

    private LocalDateTime documentDate;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private String encounterReference;

    private String facilityReference;

    private Map<String, Object> fhirResource;

    private Map<String, Object> categoryCodes;

    private Map<String, Object> securityLabels;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<DocumentAttachmentDto> attachments;

    private CdaDocumentDto cdaDocument;
}
