package com.healthdata.documentation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Document View Entity
 *
 * Tracks document views and analytics.
 */
@Entity
@Table(name = "document_views")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = Instant.now();
        }
    }
}
