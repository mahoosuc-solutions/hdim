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
}
