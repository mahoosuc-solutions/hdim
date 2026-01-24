package com.healthdata.documentation.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document Attachment Entity
 *
 * Stores binary attachments associated with clinical documents.
 * Supports PDF, images, and other file types.
 */
@Entity
@Table(name = "document_attachments",
       indexes = {
           @Index(name = "idx_attachment_clinical_doc", columnList = "clinical_document_id"),
           @Index(name = "idx_attachment_tenant", columnList = "tenant_id"),
           @Index(name = "idx_attachment_content_type", columnList = "content_type")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAttachmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "clinical_document_id", nullable = false)
    private UUID clinicalDocumentId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType; // MIME type

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path", length = 500)
    private String storagePath; // Path to file in storage system

    @Column(name = "storage_type", length = 50)
    @Builder.Default
    private String storageType = "LOCAL"; // LOCAL, S3, AZURE_BLOB, etc.

    @Column(name = "hash_algorithm", length = 20)
    @Builder.Default
    private String hashAlgorithm = "SHA-256";

    @Column(name = "hash_value", length = 128)
    private String hashValue; // For integrity verification

    @Column(name = "language", length = 10)
    private String language; // e.g., "en-US"

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText; // Extracted text from OCR processing

    @Column(name = "ocr_processed_at")
    private LocalDateTime ocrProcessedAt; // When OCR processing completed

    @Column(name = "ocr_status", length = 50)
    @Builder.Default
    private String ocrStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "ocr_error_message", length = 500)
    private String ocrErrorMessage; // Error message if OCR failed

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isOcrSupported() {
        return isPdf() || isImage();
    }

    public boolean isOcrCompleted() {
        return "COMPLETED".equals(ocrStatus);
    }

    public boolean isOcrFailed() {
        return "FAILED".equals(ocrStatus);
    }
}
