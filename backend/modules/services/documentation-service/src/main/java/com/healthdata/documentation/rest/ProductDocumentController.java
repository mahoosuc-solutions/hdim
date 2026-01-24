package com.healthdata.documentation.rest;

import com.healthdata.documentation.dto.DocumentFeedbackDto;
import com.healthdata.documentation.dto.DocumentVersionDto;
import com.healthdata.documentation.dto.ProductDocumentDto;
import com.healthdata.documentation.service.ProductDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/product")
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentController {

    private final ProductDocumentService documentService;

    @GetMapping
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<List<ProductDocumentDto>> getDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(documentService.getDocuments(tenantId));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<Page<ProductDocumentDto>> getDocumentsPaginated(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.getDocumentsPaginated(tenantId, pageable));
    }

    @GetMapping("/published")
    public ResponseEntity<Page<ProductDocumentDto>> getPublishedDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.getPublishedDocuments(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<ProductDocumentDto> getDocument(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return documentService.getDocument(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<Page<ProductDocumentDto>> searchDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.searchDocuments(tenantId, query, pageable));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<List<String>> getCategories(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(documentService.getCategories(tenantId));
    }

    @PostMapping
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<ProductDocumentDto> createDocument(
            @Valid @RequestBody ProductDocumentDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductDocumentDto created = documentService.createDocument(dto, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<ProductDocumentDto> updateDocument(
            @PathVariable String id,
            @Valid @RequestBody ProductDocumentDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        return documentService.updateDocument(id, dto, tenantId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<ProductDocumentDto> publishDocument(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        return documentService.publishDocument(id, tenantId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<ProductDocumentDto> archiveDocument(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return documentService.archiveDocument(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (documentService.deleteDocument(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<List<DocumentVersionDto>> getVersions(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(documentService.getVersions(id, tenantId));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<DocumentVersionDto> createVersion(
            @PathVariable String id,
            @Valid @RequestBody DocumentVersionDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        DocumentVersionDto created = documentService.createVersion(id, dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<DocumentFeedbackDto> submitFeedback(
            @PathVariable String id,
            @Valid @RequestBody DocumentFeedbackDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        DocumentFeedbackDto created = documentService.submitFeedback(id, dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/feedback")
    @PreAuthorize("hasPermission('CONFIG_READ')")
    public ResponseEntity<Page<DocumentFeedbackDto>> getFeedback(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.getFeedback(id, tenantId, pageable));
    }
}
