package com.healthdata.documentation.service;

import com.healthdata.documentation.dto.DocumentFeedbackDto;
import com.healthdata.documentation.dto.DocumentVersionDto;
import com.healthdata.documentation.dto.ProductDocumentDto;
import com.healthdata.documentation.persistence.DocumentFeedbackEntity;
import com.healthdata.documentation.persistence.DocumentMetadataEntity;
import com.healthdata.documentation.persistence.DocumentVersionEntity;
import com.healthdata.documentation.repository.DocumentFeedbackRepository;
import com.healthdata.documentation.repository.DocumentMetadataRepository;
import com.healthdata.documentation.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentService {

    private final DocumentMetadataRepository metadataRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentFeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public List<ProductDocumentDto> getDocuments(String tenantId) {
        return metadataRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductDocumentDto> getDocumentsPaginated(String tenantId, Pageable pageable) {
        return metadataRepository.findByTenantId(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDocumentDto> getPublishedDocuments(String tenantId, Pageable pageable) {
        return metadataRepository.findPublishedDocuments(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductDocumentDto> getDocument(String id, String tenantId) {
        metadataRepository.incrementViewCount(id);
        return metadataRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDocumentDto> searchDocuments(String tenantId, String query, Pageable pageable) {
        return metadataRepository.searchDocuments(tenantId, query, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories(String tenantId) {
        return metadataRepository.findDistinctCategories(tenantId);
    }

    @Transactional
    public ProductDocumentDto createDocument(ProductDocumentDto dto, String tenantId) {
        String id = dto.getId() != null ? dto.getId() : generateDocumentId(dto.getTitle());

        DocumentMetadataEntity entity = DocumentMetadataEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .title(dto.getTitle())
                .portalType(dto.getPortalType())
                .path(dto.getPath())
                .category(dto.getCategory())
                .subcategory(dto.getSubcategory())
                .tags(dto.getTags())
                .relatedDocuments(dto.getRelatedDocuments() != null ? dto.getRelatedDocuments() : new String[]{})
                .summary(dto.getSummary())
                .estimatedReadTime(dto.getEstimatedReadTime())
                .difficulty(dto.getDifficulty())
                .lastUpdated(LocalDate.now())
                .targetAudience(dto.getTargetAudience())
                .accessLevel(dto.getAccessLevel())
                .owner(dto.getOwner())
                .reviewCycle(dto.getReviewCycle())
                .nextReviewDate(dto.getNextReviewDate())
                .status("draft")
                .version("1.0")
                .lastReviewed(LocalDate.now())
                .seoKeywords(dto.getSeoKeywords())
                .externalLinks(dto.getExternalLinks())
                .hasVideo(dto.getHasVideo() != null ? dto.getHasVideo() : false)
                .videoUrl(dto.getVideoUrl())
                .wordCount(dto.getWordCount())
                .build();

        entity = metadataRepository.save(entity);
        log.info("Created product document {} for tenant {}", entity.getId(), tenantId);
        return toDto(entity);
    }

    @Transactional
    public Optional<ProductDocumentDto> updateDocument(String id, ProductDocumentDto dto, String tenantId, String userId) {
        return metadataRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    String oldVersion = entity.getVersion();

                    entity.setTitle(dto.getTitle());
                    entity.setPortalType(dto.getPortalType());
                    entity.setPath(dto.getPath());
                    entity.setCategory(dto.getCategory());
                    entity.setSubcategory(dto.getSubcategory());
                    entity.setTags(dto.getTags());
                    entity.setRelatedDocuments(dto.getRelatedDocuments());
                    entity.setSummary(dto.getSummary());
                    entity.setEstimatedReadTime(dto.getEstimatedReadTime());
                    entity.setDifficulty(dto.getDifficulty());
                    entity.setLastUpdated(LocalDate.now());
                    entity.setTargetAudience(dto.getTargetAudience());
                    entity.setAccessLevel(dto.getAccessLevel());
                    entity.setOwner(dto.getOwner());
                    entity.setReviewCycle(dto.getReviewCycle());
                    entity.setNextReviewDate(dto.getNextReviewDate());
                    entity.setSeoKeywords(dto.getSeoKeywords());
                    entity.setExternalLinks(dto.getExternalLinks());
                    entity.setHasVideo(dto.getHasVideo());
                    entity.setVideoUrl(dto.getVideoUrl());
                    entity.setWordCount(dto.getWordCount());

                    if (dto.getVersion() != null && !dto.getVersion().equals(oldVersion)) {
                        entity.setVersion(dto.getVersion());
                    }

                    entity = metadataRepository.save(entity);
                    log.info("Updated product document {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public Optional<ProductDocumentDto> publishDocument(String id, String tenantId, String userId) {
        return metadataRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setStatus("published");
                    entity = metadataRepository.save(entity);
                    log.info("Published product document {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public Optional<ProductDocumentDto> archiveDocument(String id, String tenantId) {
        return metadataRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setStatus("archived");
                    entity = metadataRepository.save(entity);
                    log.info("Archived product document {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public boolean deleteDocument(String id, String tenantId) {
        if (metadataRepository.existsByIdAndTenantId(id, tenantId)) {
            metadataRepository.deleteById(id);
            log.info("Deleted product document {}", id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionDto> getVersions(String documentId, String tenantId) {
        return versionRepository.findVersionsOrderByCreatedAtDesc(documentId, tenantId)
                .stream()
                .map(this::toVersionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentVersionDto createVersion(String documentId, DocumentVersionDto dto, String tenantId, String userId) {
        DocumentVersionEntity entity = DocumentVersionEntity.builder()
                .documentId(documentId)
                .tenantId(tenantId)
                .versionNumber(dto.getVersionNumber())
                .content(dto.getContent())
                .changeSummary(dto.getChangeSummary())
                .changedBy(userId)
                .isMajorVersion(dto.getIsMajorVersion() != null ? dto.getIsMajorVersion() : false)
                .isPublished(false)
                .build();

        entity = versionRepository.save(entity);
        log.info("Created version {} for document {}", dto.getVersionNumber(), documentId);
        return toVersionDto(entity);
    }

    @Transactional
    public DocumentFeedbackDto submitFeedback(String documentId, DocumentFeedbackDto dto, String tenantId, String userId) {
        Optional<DocumentFeedbackEntity> existingFeedback = feedbackRepository
                .findByDocumentIdAndUserId(documentId, userId, tenantId);

        DocumentFeedbackEntity entity;
        if (existingFeedback.isPresent()) {
            entity = existingFeedback.get();
            entity.setRating(dto.getRating());
            entity.setComment(dto.getComment());
            entity.setHelpful(dto.getHelpful());
            entity.setFeedbackType(dto.getFeedbackType());
        } else {
            entity = DocumentFeedbackEntity.builder()
                    .documentId(documentId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .rating(dto.getRating())
                    .comment(dto.getComment())
                    .helpful(dto.getHelpful())
                    .feedbackType(dto.getFeedbackType() != null ? dto.getFeedbackType() : "GENERAL")
                    .status("PENDING")
                    .build();
        }

        entity = feedbackRepository.save(entity);
        updateDocumentRating(documentId);
        log.info("Submitted feedback for document {} by user {}", documentId, userId);
        return toFeedbackDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<DocumentFeedbackDto> getFeedback(String documentId, String tenantId, Pageable pageable) {
        return feedbackRepository.findByDocumentIdAndTenantId(documentId, tenantId, pageable)
                .map(this::toFeedbackDto);
    }

    private void updateDocumentRating(String documentId) {
        Double avgRating = feedbackRepository.calculateAverageRating(documentId);
        long feedbackCount = feedbackRepository.countByDocumentId(documentId);

        metadataRepository.findById(documentId).ifPresent(entity -> {
            entity.setAvgRating(avgRating != null ? BigDecimal.valueOf(avgRating) : null);
            entity.setFeedbackCount((int) feedbackCount);
            metadataRepository.save(entity);
        });
    }

    private String generateDocumentId(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private ProductDocumentDto toDto(DocumentMetadataEntity entity) {
        return ProductDocumentDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .portalType(entity.getPortalType())
                .path(entity.getPath())
                .category(entity.getCategory())
                .subcategory(entity.getSubcategory())
                .tags(entity.getTags())
                .relatedDocuments(entity.getRelatedDocuments())
                .summary(entity.getSummary())
                .estimatedReadTime(entity.getEstimatedReadTime())
                .difficulty(entity.getDifficulty())
                .lastUpdated(entity.getLastUpdated())
                .targetAudience(entity.getTargetAudience())
                .accessLevel(entity.getAccessLevel())
                .owner(entity.getOwner())
                .reviewCycle(entity.getReviewCycle())
                .nextReviewDate(entity.getNextReviewDate())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .lastReviewed(entity.getLastReviewed())
                .seoKeywords(entity.getSeoKeywords())
                .externalLinks(entity.getExternalLinks())
                .hasVideo(entity.getHasVideo())
                .videoUrl(entity.getVideoUrl())
                .wordCount(entity.getWordCount())
                .createdDate(entity.getCreatedDate())
                .viewCount(entity.getViewCount())
                .avgRating(entity.getAvgRating())
                .feedbackCount(entity.getFeedbackCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DocumentVersionDto toVersionDto(DocumentVersionEntity entity) {
        return DocumentVersionDto.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .versionNumber(entity.getVersionNumber())
                .content(entity.getContent())
                .changeSummary(entity.getChangeSummary())
                .changedBy(entity.getChangedBy())
                .isMajorVersion(entity.getIsMajorVersion())
                .isPublished(entity.getIsPublished())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DocumentFeedbackDto toFeedbackDto(DocumentFeedbackEntity entity) {
        return DocumentFeedbackDto.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .userId(entity.getUserId())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .helpful(entity.getHelpful())
                .feedbackType(entity.getFeedbackType())
                .status(entity.getStatus())
                .adminResponse(entity.getAdminResponse())
                .respondedBy(entity.getRespondedBy())
                .respondedAt(entity.getRespondedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
