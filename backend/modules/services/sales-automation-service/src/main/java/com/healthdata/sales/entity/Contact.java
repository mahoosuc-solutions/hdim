package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contact entity representing an individual at a customer organization
 */
@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contacts_tenant", columnList = "tenant_id"),
    @Index(name = "idx_contacts_account", columnList = "account_id"),
    @Index(name = "idx_contacts_email", columnList = "email"),
    @Index(name = "idx_contacts_zoho", columnList = "zoho_contact_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "mobile", length = 50)
    private String mobile;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "department", length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type")
    private ContactType contactType;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean primary = false;

    @Column(name = "do_not_call", nullable = false)
    @Builder.Default
    private Boolean doNotCall = false;

    @Column(name = "do_not_email", nullable = false)
    @Builder.Default
    private Boolean doNotEmail = false;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "zoho_contact_id", length = 100)
    private String zohoContactId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
