package com.healthdata.sales.controller;

import com.healthdata.sales.dto.ContactDTO;
import com.healthdata.sales.entity.ContactType;
import com.healthdata.sales.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales/contacts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contacts", description = "Contact management endpoints")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Operation(summary = "List all contacts", description = "Get paginated list of contacts")
    public ResponseEntity<Page<ContactDTO>> findAll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID")
    public ResponseEntity<ContactDTO> findById(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contactService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new contact")
    public ResponseEntity<ContactDTO> create(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ContactDTO dto
    ) {
        ContactDTO created = contactService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing contact")
    public ResponseEntity<ContactDTO> update(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody ContactDTO dto
    ) {
        return ResponseEntity.ok(contactService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        contactService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get contacts by account", description = "Paginated list of contacts for an account")
    public ResponseEntity<Page<ContactDTO>> findByAccount(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID accountId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findByAccount(tenantId, accountId, pageable));
    }

    @GetMapping("/account/{accountId}/all")
    @Operation(summary = "Get all contacts for account", description = "Non-paginated list")
    public ResponseEntity<List<ContactDTO>> findAllByAccount(
        @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(contactService.findAllByAccount(accountId));
    }

    @GetMapping("/account/{accountId}/primary")
    @Operation(summary = "Get primary contact for account")
    public ResponseEntity<ContactDTO> findPrimaryContact(
        @PathVariable UUID accountId
    ) {
        ContactDTO primary = contactService.findPrimaryContact(accountId);
        if (primary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(primary);
    }

    @PatchMapping("/account/{accountId}/primary/{contactId}")
    @Operation(summary = "Set primary contact for account")
    public ResponseEntity<ContactDTO> setPrimaryContact(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID accountId,
        @PathVariable UUID contactId
    ) {
        return ResponseEntity.ok(contactService.setPrimaryContact(tenantId, accountId, contactId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get contacts by type", description = "Filter by DECISION_MAKER, INFLUENCER, etc.")
    public ResponseEntity<Page<ContactDTO>> findByContactType(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable ContactType type,
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findByContactType(tenantId, type, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search contacts", description = "Search by name or email")
    public ResponseEntity<Page<ContactDTO>> search(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestParam String query,
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.search(tenantId, query, pageable));
    }

    @GetMapping("/contactable")
    @Operation(summary = "Get contactable contacts", description = "Contacts without do-not-call/email flags")
    public ResponseEntity<Page<ContactDTO>> findContactableContacts(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findContactableContacts(tenantId, pageable));
    }

    @PostMapping("/{id}/record-activity")
    @Operation(summary = "Record contact activity", description = "Updates last contacted timestamp")
    public ResponseEntity<ContactDTO> recordContactActivity(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contactService.recordContactActivity(tenantId, id));
    }
}
