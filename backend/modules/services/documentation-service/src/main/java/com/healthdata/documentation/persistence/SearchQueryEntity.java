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
 * Search Query Entity
 *
 * Tracks search analytics for the documentation portal.
 */
@Entity
@Table(name = "search_queries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQueryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "portal_type", length = 10)
    private String portalType;

    @Column(name = "filters_applied", columnDefinition = "JSONB")
    private String filtersApplied;

    @Column(name = "results_count", nullable = false)
    private Integer resultsCount;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "clicked_result_id", length = 100)
    private String clickedResultId;

    @Column(name = "clicked_result_position")
    private Integer clickedResultPosition;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    @PrePersist
    protected void onCreate() {
        if (searchedAt == null) {
            searchedAt = Instant.now();
        }
    }
}
