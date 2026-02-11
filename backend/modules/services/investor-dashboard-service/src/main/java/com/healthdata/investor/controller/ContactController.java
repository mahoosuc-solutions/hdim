package com.healthdata.investor.controller;

import com.healthdata.investor.dto.ContactDTO;
import com.healthdata.investor.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing investor contacts.
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Contact management endpoints")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Operation(summary = "Get all contacts")
    public ResponseEntity<List<ContactDTO>> getAllContacts() {
        return ResponseEntity.ok(contactService.getAllContacts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID with recent activities")
    public ResponseEntity<ContactDTO> getContact(@PathVariable UUID id) {
        return ResponseEntity.ok(contactService.getContact(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get contacts by category")
    public ResponseEntity<List<ContactDTO>> getContactsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(contactService.getContactsByCategory(category));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get contacts by status")
    public ResponseEntity<List<ContactDTO>> getContactsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(contactService.getContactsByStatus(status));
    }

    @GetMapping("/tier/{tier}")
    @Operation(summary = "Get contacts by tier")
    public ResponseEntity<List<ContactDTO>> getContactsByTier(@PathVariable String tier) {
        return ResponseEntity.ok(contactService.getContactsByTier(tier));
    }

    @GetMapping("/search")
    @Operation(summary = "Search contacts by name or organization")
    public ResponseEntity<List<ContactDTO>> searchContacts(@RequestParam String query) {
        return ResponseEntity.ok(contactService.searchContacts(query));
    }

    @GetMapping("/follow-up")
    @Operation(summary = "Get contacts needing follow-up")
    public ResponseEntity<List<ContactDTO>> getContactsNeedingFollowUp() {
        return ResponseEntity.ok(contactService.getContactsNeedingFollowUp());
    }

    @PostMapping
    @Operation(summary = "Create a new contact")
    public ResponseEntity<ContactDTO> createContact(@Valid @RequestBody ContactDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.createContact(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a contact")
    public ResponseEntity<ContactDTO> updateContact(
            @PathVariable UUID id,
            @RequestBody ContactDTO.UpdateRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update contact status")
    public ResponseEntity<ContactDTO> updateContactStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        ContactDTO.UpdateRequest request = ContactDTO.UpdateRequest.builder().status(status).build();
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
