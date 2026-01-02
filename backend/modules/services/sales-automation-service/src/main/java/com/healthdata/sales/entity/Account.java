package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account entity representing a customer organization
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_tenant", columnList = "tenant_id"),
    @Index(name = "idx_accounts_name", columnList = "name"),
    @Index(name = "idx_accounts_stage", columnList = "stage"),
    @Index(name = "idx_accounts_zoho", columnList = "zoho_account_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type")
    private OrganizationType organizationType;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "patient_count")
    private Integer patientCount;

    @Column(name = "ehr_count")
    private Integer ehrCount;

    @Column(name = "ehr_systems", length = 500)
    private String ehrSystems;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private AccountStage stage;

    @Column(name = "annual_revenue")
    private Long annualRevenue;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "zoho_account_id", length = 100)
    private String zohoAccountId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
