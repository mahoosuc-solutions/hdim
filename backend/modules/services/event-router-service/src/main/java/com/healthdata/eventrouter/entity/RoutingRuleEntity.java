package com.healthdata.eventrouter.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "event_routing_rules", indexes = {
    @Index(name = "idx_tenant_source_enabled", columnList = "tenant_id, source_topic, enabled"),
    @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
public class RoutingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description")
    private String description;

    @Column(name = "source_topic", nullable = false)
    private String sourceTopic;

    @Column(name = "target_topic", nullable = false)
    private String targetTopic;

    @Column(name = "filter_expression", columnDefinition = "TEXT")
    private String filterExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "transformation_script", columnDefinition = "TEXT")
    private String transformationScript;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

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

    public enum Priority {
        CRITICAL(0),
        HIGH(1),
        MEDIUM(2),
        LOW(3);

        private final int order;

        Priority(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }
}
