package com.healthdata.documentation.service;

import com.healthdata.documentation.dto.ClinicalDocumentDto;
import com.healthdata.documentation.dto.DocumentAttachmentDto;
import com.healthdata.documentation.persistence.ClinicalDocumentEntity;
import com.healthdata.documentation.persistence.DocumentAttachmentEntity;
import com.healthdata.documentation.repository.ClinicalDocumentRepository;
import com.healthdata.documentation.repository.DocumentAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalDocumentService {

    private final ClinicalDocumentRepository documentRepository;
    private final DocumentAttachmentRepository attachmentRepository;
    private final OcrService ocrService;

    @Transactional(readOnly = true)
    public List<ClinicalDocumentDto> getDocuments(String tenantId) {
        return documentRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ClinicalDocumentDto> getDocumentsPaginated(String tenantId, Pageable pageable) {
        return documentRepository.findByTenantId(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ClinicalDocumentDto> getDocument(UUID id, String tenantId) {
        return documentRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDtoWithAttachments);
    }

    @Transactional(readOnly = true)
    public List<ClinicalDocumentDto> getPatientDocuments(String tenantId, String patientId) {
        return documentRepository.findByTenantIdAndPatientId(tenantId, patientId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ClinicalDocumentDto> getPatientDocumentsPaginated(String tenantId, String patientId, Pageable pageable) {
        return documentRepository.findByTenantIdAndPatientId(tenantId, patientId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ClinicalDocumentDto> searchDocuments(String tenantId, String query, Pageable pageable) {
        return documentRepository.searchDocuments(tenantId, query, pageable)
                .map(this::toDto);
    }

    @Transactional
    public ClinicalDocumentDto createDocument(ClinicalDocumentDto dto, String tenantId) {
        ClinicalDocumentEntity entity = ClinicalDocumentEntity.builder()
                .tenantId(tenantId)
                .patientId(dto.getPatientId())
                .documentType(dto.getDocumentType())
                .documentTypeCode(dto.getDocumentTypeCode())
                .documentTypeSystem(dto.getDocumentTypeSystem())
                .status(dto.getStatus() != null ? dto.getStatus() : "current")
                .title(dto.getTitle())
                .description(dto.getDescription())
                .authorReference(dto.getAuthorReference())
                .authorName(dto.getAuthorName())
                .custodianReference(dto.getCustodianReference())
                .documentDate(dto.getDocumentDate())
                .periodStart(dto.getPeriodStart())
                .periodEnd(dto.getPeriodEnd())
                .encounterReference(dto.getEncounterReference())
                .facilityReference(dto.getFacilityReference())
                .fhirResource(dto.getFhirResource())
                .categoryCodes(dto.getCategoryCodes())
                .securityLabels(dto.getSecurityLabels())
                .build();

        entity = documentRepository.save(entity);
        log.info("Created clinical document {} for patient {} in tenant {}", entity.getId(), dto.getPatientId(), tenantId);
        return toDto(entity);
    }

    @Transactional
    public Optional<ClinicalDocumentDto> updateDocument(UUID id, ClinicalDocumentDto dto, String tenantId) {
        return documentRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setDocumentType(dto.getDocumentType());
                    entity.setDocumentTypeCode(dto.getDocumentTypeCode());
                    entity.setDocumentTypeSystem(dto.getDocumentTypeSystem());
                    entity.setStatus(dto.getStatus());
                    entity.setTitle(dto.getTitle());
                    entity.setDescription(dto.getDescription());
                    entity.setAuthorReference(dto.getAuthorReference());
                    entity.setAuthorName(dto.getAuthorName());
                    entity.setCustodianReference(dto.getCustodianReference());
                    entity.setDocumentDate(dto.getDocumentDate());
                    entity.setPeriodStart(dto.getPeriodStart());
                    entity.setPeriodEnd(dto.getPeriodEnd());
                    entity.setEncounterReference(dto.getEncounterReference());
                    entity.setFacilityReference(dto.getFacilityReference());
                    entity.setFhirResource(dto.getFhirResource());
                    entity.setCategoryCodes(dto.getCategoryCodes());
                    entity.setSecurityLabels(dto.getSecurityLabels());

                    entity = documentRepository.save(entity);
                    log.info("Updated clinical document {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public boolean deleteDocument(UUID id, String tenantId) {
        if (documentRepository.existsByIdAndTenantId(id, tenantId)) {
            attachmentRepository.deleteByClinicalDocumentIdAndTenantId(id, tenantId);
            documentRepository.deleteById(id);
            log.info("Deleted clinical document {}", id);
            return true;
        }
        return false;
    }

    @Transactional
    public DocumentAttachmentDto addAttachment(UUID documentId, DocumentAttachmentDto dto, String tenantId) {
        if (!documentRepository.existsByIdAndTenantId(documentId, tenantId)) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        DocumentAttachmentEntity entity = DocumentAttachmentEntity.builder()
                .clinicalDocumentId(documentId)
                .tenantId(tenantId)
                .contentType(dto.getContentType())
                .fileName(dto.getFileName())
                .fileSize(dto.getFileSize())
                .storagePath(dto.getStoragePath())
                .storageType(dto.getStorageType())
                .hashValue(dto.getHashValue())
                .language(dto.getLanguage())
                .title(dto.getTitle())
                .creationDate(dto.getCreationDate())
                .build();

        entity = attachmentRepository.save(entity);
        log.info("Added attachment {} to document {}", entity.getId(), documentId);
        return toAttachmentDto(entity);
    }

    @Transactional(readOnly = true)
    public Optional<DocumentAttachmentDto> getAttachment(UUID attachmentId, String tenantId) {
        return attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .map(this::toAttachmentDto);
    }

    @Transactional
    public boolean deleteAttachment(UUID attachmentId, String tenantId) {
        if (attachmentRepository.existsByIdAndTenantId(attachmentId, tenantId)) {
            attachmentRepository.deleteById(attachmentId);
            log.info("Deleted attachment {}", attachmentId);
            return true;
        }
        return false;
    }

    /**
     * Upload a file and attach to clinical document
     * Saves file to local storage and creates attachment record
     * Triggers async OCR processing for supported file types (PDF, images)
     */
    @Transactional
    public DocumentAttachmentDto uploadFile(UUID documentId, MultipartFile file, String title, String tenantId) {
        // Validate document exists
        ClinicalDocumentEntity document = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        // Validate file type (only PDF and images supported for OCR)
        if (!isSupportedFileType(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Supported: PDF, PNG, JPG, JPEG, TIFF");
        }

        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (fileSize > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        try {
            // Generate unique filename
            UUID attachmentId = UUID.randomUUID();
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = attachmentId + fileExtension;

            // Define storage path: /var/lib/healthdata/documents/{tenantId}/{documentId}/{filename}
            Path storageDirectory = Paths.get("/var/lib/healthdata/documents", tenantId, documentId.toString());
            Files.createDirectories(storageDirectory);

            Path storagePath = storageDirectory.resolve(storedFilename);

            // Save file to disk
            Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved file to: {}", storagePath);

            // Calculate SHA-256 hash for integrity verification
            String hashValue = calculateSha256(file.getBytes());

            // Create attachment entity
            DocumentAttachmentEntity entity = DocumentAttachmentEntity.builder()
                    .id(attachmentId)
                    .clinicalDocumentId(documentId)
                    .tenantId(tenantId)
                    .contentType(contentType)
                    .fileName(originalFilename)
                    .fileSize(fileSize)
                    .storagePath(storagePath.toString())
                    .storageType("LOCAL")
                    .hashAlgorithm("SHA-256")
                    .hashValue(hashValue)
                    .title(title != null ? title : originalFilename)
                    .creationDate(LocalDateTime.now())
                    .ocrStatus("PENDING") // Initial OCR status
                    .build();

            entity = attachmentRepository.save(entity);
            log.info("Created attachment {} for document {} (patient: {})", attachmentId, documentId, document.getPatientId());

            // Trigger async OCR processing for supported file types
            if (entity.isOcrSupported()) {
                ocrService.processDocumentAsync(entity.getId(), tenantId);
                log.info("Triggered async OCR processing for attachment {}", attachmentId);
            }

            return toAttachmentDto(entity);

        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger OCR re-processing for an attachment
     */
    @Transactional
    public void triggerOcrReprocessing(UUID attachmentId, String tenantId) {
        DocumentAttachmentEntity attachment = attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        // Reset OCR status to PENDING
        attachment.setOcrStatus("PENDING");
        attachment.setOcrProcessedAt(null);
        attachment.setOcrText(null);
        attachment.setOcrErrorMessage(null);
        attachmentRepository.save(attachment);

        log.info("Triggered OCR reprocessing for attachment {}", attachmentId);

        // Trigger async OCR processing
        if (attachment.isOcrSupported()) {
            ocrService.processDocumentAsync(attachmentId, tenantId);
        }
    }

    /**
     * Full-text search across OCR extracted text
     */
    @Transactional(readOnly = true)
    public Page<DocumentAttachmentDto> searchOcrText(String tenantId, String query, Pageable pageable) {
        log.debug("Searching OCR text for query: {} in tenant: {}", query, tenantId);
        return attachmentRepository.searchOcrText(tenantId, query, pageable)
                .map(this::toAttachmentDto);
    }

    private boolean isSupportedFileType(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("application/pdf") ||
               contentType.startsWith("image/png") ||
               contentType.startsWith("image/jpeg") ||
               contentType.startsWith("image/jpg") ||
               contentType.startsWith("image/tiff");
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

    private ClinicalDocumentDto toDto(ClinicalDocumentEntity entity) {
        return ClinicalDocumentDto.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .documentType(entity.getDocumentType())
                .documentTypeCode(entity.getDocumentTypeCode())
                .documentTypeSystem(entity.getDocumentTypeSystem())
                .status(entity.getStatus())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .authorReference(entity.getAuthorReference())
                .authorName(entity.getAuthorName())
                .custodianReference(entity.getCustodianReference())
                .documentDate(entity.getDocumentDate())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .encounterReference(entity.getEncounterReference())
                .facilityReference(entity.getFacilityReference())
                .fhirResource(entity.getFhirResource())
                .categoryCodes(entity.getCategoryCodes())
                .securityLabels(entity.getSecurityLabels())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ClinicalDocumentDto toDtoWithAttachments(ClinicalDocumentEntity entity) {
        ClinicalDocumentDto dto = toDto(entity);
        List<DocumentAttachmentDto> attachments = attachmentRepository
                .findByClinicalDocumentIdAndTenantId(entity.getId(), entity.getTenantId())
                .stream()
                .map(this::toAttachmentDto)
                .collect(Collectors.toList());
        dto.setAttachments(attachments);
        return dto;
    }

    private DocumentAttachmentDto toAttachmentDto(DocumentAttachmentEntity entity) {
        return DocumentAttachmentDto.builder()
                .id(entity.getId())
                .clinicalDocumentId(entity.getClinicalDocumentId())
                .contentType(entity.getContentType())
                .fileName(entity.getFileName())
                .fileSize(entity.getFileSize())
                .storagePath(entity.getStoragePath())
                .storageType(entity.getStorageType())
                .hashValue(entity.getHashValue())
                .language(entity.getLanguage())
                .title(entity.getTitle())
                .creationDate(entity.getCreationDate())
                .createdAt(entity.getCreatedAt())
                .ocrText(entity.getOcrText())
                .ocrProcessedAt(entity.getOcrProcessedAt())
                .ocrStatus(entity.getOcrStatus())
                .ocrErrorMessage(entity.getOcrErrorMessage())
                .build();
    }
}
