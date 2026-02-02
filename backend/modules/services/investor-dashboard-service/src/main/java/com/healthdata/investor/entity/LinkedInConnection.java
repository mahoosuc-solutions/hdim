package com.healthdata.investor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity storing LinkedIn OAuth connection details for a user.
 * Enables automated LinkedIn outreach tracking and profile lookups.
 */
@Entity
@Table(name = "linkedin_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedInConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private InvestorUser user;

    @Column(name = "linkedin_member_id", length = 100)
    private String linkedInMemberId;

    @Column(name = "linkedin_profile_url", length = 500)
    private String linkedInProfileUrl;

    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;

    @Column(name = "scope", length = 255)
    private String scope;

    @Column(name = "connected", nullable = false)
    @Builder.Default
    private Boolean connected = true;

    @Column(name = "last_sync")
    private Instant lastSync;

    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(Instant.now());
    }

    public boolean isConnected() {
        return connected != null && connected && !isTokenExpired();
    }

    public boolean canRefresh() {
        return refreshToken != null && !refreshToken.isBlank();
    }
}
