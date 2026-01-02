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
                .build();
    }
}
