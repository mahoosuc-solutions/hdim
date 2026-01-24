package com.healthdata.documentation.dto;

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
public class DocumentAttachmentDto {

    private UUID id;

    private UUID clinicalDocumentId;

    @NotBlank(message = "Content type is required")
    private String contentType;

    private String fileName;

    private Long fileSize;

    private String storagePath;

    private String storageType;

    private String hashValue;

    private String language;

    private String title;

    private LocalDateTime creationDate;

    private LocalDateTime createdAt;

    private String ocrText; // Extracted text from OCR processing

    private LocalDateTime ocrProcessedAt; // When OCR processing completed

    private String ocrStatus; // PENDING, PROCESSING, COMPLETED, FAILED

    private String ocrErrorMessage; // Error message if OCR failed
}
