package com.healthdata.sales.controller;

import com.healthdata.sales.dto.AccountDTO;
import com.healthdata.sales.entity.AccountStage;
import com.healthdata.sales.entity.OrganizationType;
import com.healthdata.sales.service.AccountService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/sales/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts", description = "Account/Organization management endpoints")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "List all accounts", description = "Get paginated list of accounts")
    public ResponseEntity<Page<AccountDTO>> findAll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountDTO> findById(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(accountService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<AccountDTO> create(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody AccountDTO dto
    ) {
        AccountDTO created = accountService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing account")
    public ResponseEntity<AccountDTO> update(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody AccountDTO dto
    ) {
        return ResponseEntity.ok(accountService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an account")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        accountService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stage/{stage}")
    @Operation(summary = "Get accounts by stage")
    public ResponseEntity<Page<AccountDTO>> findByStage(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable AccountStage stage,
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.findByStage(tenantId, stage, pageable));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get accounts by organization type")
    public ResponseEntity<Page<AccountDTO>> findByOrganizationType(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable OrganizationType type,
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.findByOrganizationType(tenantId, type, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search accounts by name")
    public ResponseEntity<Page<AccountDTO>> search(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestParam String query,
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.search(tenantId, query, pageable));
    }
}
