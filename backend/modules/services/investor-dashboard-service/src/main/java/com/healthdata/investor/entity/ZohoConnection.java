package com.healthdata.investor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing Zoho ONE OAuth connection for a user.
 * Stores access/refresh tokens and connection metadata.
 *
 * Zoho ONE provides access to 45+ applications with single OAuth:
 * - Zoho CRM (contact management)
 * - Zoho Campaigns (email marketing)
 * - Zoho Bookings (meeting scheduler)
 * - Zoho Analytics (BI/reporting)
 * - Zoho Social (social media management)
 * - Plus 40+ more apps
 */
@Entity
@Table(name = "zoho_connections", indexes = {
    @Index(name = "idx_zoho_user_id", columnList = "user_id"),
    @Index(name = "idx_zoho_tenant_id", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZohoConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to InvestorUser who owns this connection
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private InvestorUser user;

    /**
     * Tenant ID for multi-tenancy isolation
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Zoho OAuth access token
     * TODO: Encrypt in production using @Convert(converter = SensitiveDataConverter.class)
     */
    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;

    /**
     * Zoho OAuth refresh token (for token renewal)
     */
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    /**
     * Zoho API domain (varies by data center)
     * Examples: zoho.com, zoho.eu, zoho.in, zoho.com.au
     */
    @Column(name = "api_domain", length = 50)
    private String apiDomain;

    /**
     * Zoho organization ID (ZGID)
     */
    @Column(name = "organization_id", length = 100)
    private String organizationId;

    /**
     * Zoho user email
     */
    @Column(name = "zoho_email", length = 255)
    private String zohoEmail;

    /**
     * Zoho user display name
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Connection status
     */
    @Column(name = "connected", nullable = false)
    @Builder.Default
    private boolean connected = false;

    /**
     * OAuth scope granted
     * Example: "ZohoCRM.modules.ALL,ZohoCampaigns.campaign.ALL,ZohoBookings.appointment.ALL"
     */
    @Column(name = "scope", length = 1000)
    private String scope;

    /**
     * Access token expiration timestamp
     */
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    /**
     * Last successful sync with Zoho
     */
    @Column(name = "last_sync")
    private Instant lastSync;

    /**
     * Sync error message (if any)
     */
    @Column(name = "sync_error", length = 1000)
    private String syncError;

    /**
     * Timestamp when connection was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when connection was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Check if access token is expired
     */
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && Instant.now().isAfter(tokenExpiresAt);
    }

    /**
     * Check if token can be refreshed
     */
    public boolean canRefresh() {
        return refreshToken != null && !refreshToken.isEmpty() && !refreshToken.equals("REVOKED");
    }

    /**
     * Check if connection needs token refresh (expires within 5 minutes)
     */
    public boolean needsRefresh() {
        if (tokenExpiresAt == null) {
            return false;
        }
        Instant fiveMinutesFromNow = Instant.now().plusSeconds(300);
        return tokenExpiresAt.isBefore(fiveMinutesFromNow);
    }
}
