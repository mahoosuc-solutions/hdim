package com.healthdata.documentation.rest;

import com.healthdata.documentation.dto.ClinicalDocumentDto;
import com.healthdata.documentation.dto.DocumentAttachmentDto;
import com.healthdata.documentation.service.ClinicalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents/clinical")
@RequiredArgsConstructor
@Slf4j
public class ClinicalDocumentController {

    private final ClinicalDocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<List<ClinicalDocumentDto>> getDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(documentService.getDocuments(tenantId));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<Page<ClinicalDocumentDto>> getDocumentsPaginated(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.getDocumentsPaginated(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<ClinicalDocumentDto> getDocument(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return documentService.getDocument(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<List<ClinicalDocumentDto>> getPatientDocuments(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(documentService.getPatientDocuments(tenantId, patientId));
    }

    @GetMapping("/patient/{patientId}/paginated")
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<Page<ClinicalDocumentDto>> getPatientDocumentsPaginated(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.getPatientDocumentsPaginated(tenantId, patientId, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<Page<ClinicalDocumentDto>> searchDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(documentService.searchDocuments(tenantId, query, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLINICIAN', 'ADMIN')")
    public ResponseEntity<ClinicalDocumentDto> createDocument(
            @Valid @RequestBody ClinicalDocumentDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        ClinicalDocumentDto created = documentService.createDocument(dto, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'ADMIN')")
    public ResponseEntity<ClinicalDocumentDto> updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicalDocumentDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return documentService.updateDocument(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (documentService.deleteDocument(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'ADMIN')")
    public ResponseEntity<DocumentAttachmentDto> addAttachment(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentAttachmentDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        DocumentAttachmentDto created = documentService.addAttachment(id, dto, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('USER', 'CLINICIAN', 'ADMIN')")
    public ResponseEntity<DocumentAttachmentDto> getAttachment(
            @PathVariable UUID attachmentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return documentService.getAttachment(attachmentId, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'ADMIN')")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID attachmentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (documentService.deleteAttachment(attachmentId, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
