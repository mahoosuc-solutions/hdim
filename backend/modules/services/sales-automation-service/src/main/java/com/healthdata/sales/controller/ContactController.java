package com.healthdata.sales.controller;

import com.healthdata.sales.dto.ContactDTO;
import com.healthdata.sales.entity.ContactType;
import com.healthdata.sales.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(
    name = "Contacts",
    description = """
        APIs for managing contacts within healthcare organizations.

        Contacts are individuals associated with accounts (healthcare organizations):
        - Decision makers, influencers, and champions
        - Primary contacts for accounts
        - LinkedIn profiles and communication preferences
        - Do-not-contact flags for compliance

        Contact types:
        - DECISION_MAKER: Final purchasing authority
        - INFLUENCER: Influences decisions but doesn't sign
        - CHAMPION: Internal advocate for your solution
        - USER: End user of the product
        - OTHER: Other contact roles

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Operation(
        summary = "List all contacts",
        description = "Retrieves a paginated list of all contacts for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contacts"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ContactDTO>> findAll(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get contact by ID",
        description = "Retrieves detailed information about a specific contact."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contact"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<ContactDTO> findById(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contactService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(
        summary = "Create a new contact",
        description = """
            Creates a new contact associated with an account.

            Required fields: firstName, lastName, email, accountId
            Optional: phone, title, contactType, linkedInUrl, doNotCall, doNotEmail
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contact created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "409", description = "Contact with this email already exists")
    })
    public ResponseEntity<ContactDTO> create(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact details", required = true)
        @Valid @RequestBody ContactDTO dto
    ) {
        ContactDTO created = contactService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing contact",
        description = "Updates an existing contact with new information."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contact updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "404", description = "Contact not found"),
        @ApiResponse(responseCode = "409", description = "Contact with this email already exists")
    })
    public ResponseEntity<ContactDTO> update(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Updated contact details", required = true)
        @Valid @RequestBody ContactDTO dto
    ) {
        return ResponseEntity.ok(contactService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a contact",
        description = "Deletes a contact. This will also remove the contact from associated opportunities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contact deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID id
    ) {
        contactService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/account/{accountId}")
    @Operation(
        summary = "Get contacts by account",
        description = "Retrieves a paginated list of contacts for a specific account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account contacts"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<Page<ContactDTO>> findByAccount(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findByAccount(tenantId, accountId, pageable));
    }

    @GetMapping("/account/{accountId}/all")
    @Operation(
        summary = "Get all contacts for account",
        description = "Retrieves all contacts for an account without pagination. Use for dropdowns and small lists."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all account contacts")
    })
    public ResponseEntity<List<ContactDTO>> findAllByAccount(
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(contactService.findAllByAccount(accountId));
    }

    @GetMapping("/account/{accountId}/primary")
    @Operation(
        summary = "Get primary contact for account",
        description = "Retrieves the designated primary contact for an account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved primary contact"),
        @ApiResponse(responseCode = "404", description = "No primary contact set for this account")
    })
    public ResponseEntity<ContactDTO> findPrimaryContact(
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId
    ) {
        ContactDTO primary = contactService.findPrimaryContact(accountId);
        if (primary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(primary);
    }

    @PatchMapping("/account/{accountId}/primary/{contactId}")
    @Operation(
        summary = "Set primary contact for account",
        description = "Designates a contact as the primary contact for an account. Only one primary contact per account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Primary contact set successfully"),
        @ApiResponse(responseCode = "400", description = "Contact does not belong to this account"),
        @ApiResponse(responseCode = "404", description = "Account or contact not found")
    })
    public ResponseEntity<ContactDTO> setPrimaryContact(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId,
        @Parameter(description = "Contact ID to set as primary", required = true)
        @PathVariable UUID contactId
    ) {
        return ResponseEntity.ok(contactService.setPrimaryContact(tenantId, accountId, contactId));
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get contacts by type",
        description = "Retrieves contacts filtered by their role type."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contacts by type"),
        @ApiResponse(responseCode = "400", description = "Invalid contact type")
    })
    public ResponseEntity<Page<ContactDTO>> findByContactType(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact type", required = true,
            schema = @Schema(allowableValues = {"DECISION_MAKER", "INFLUENCER", "CHAMPION", "USER", "OTHER"}))
        @PathVariable ContactType type,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findByContactType(tenantId, type, pageable));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search contacts",
        description = "Searches for contacts by name or email using partial matching (case-insensitive)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results")
    })
    public ResponseEntity<Page<ContactDTO>> search(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Search query (name or email)", required = true, example = "john")
        @RequestParam String query,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.search(tenantId, query, pageable));
    }

    @GetMapping("/contactable")
    @Operation(
        summary = "Get contactable contacts",
        description = "Retrieves contacts that can be contacted (no doNotCall or doNotEmail flags set)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contactable contacts")
    })
    public ResponseEntity<Page<ContactDTO>> findContactableContacts(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.findContactableContacts(tenantId, pageable));
    }

    @PostMapping("/{id}/record-activity")
    @Operation(
        summary = "Record contact activity",
        description = "Updates the lastContactedAt timestamp for a contact. Called automatically when activities are logged."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity recorded successfully"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<ContactDTO> recordContactActivity(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contactService.recordContactActivity(tenantId, id));
    }
}
