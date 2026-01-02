package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Email sequence definition for automated email campaigns
 */
@Entity
@Table(name = "email_sequences", indexes = {
    @Index(name = "idx_email_sequences_tenant", columnList = "tenant_id"),
    @Index(name = "idx_email_sequences_type", columnList = "sequence_type"),
    @Index(name = "idx_email_sequences_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "sequence_type", nullable = false)
    private SequenceType sequenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "from_name", length = 100)
    private String fromName;

    @Column(name = "from_email", length = 255)
    private String fromEmail;

    @Column(name = "reply_to_email", length = 255)
    private String replyToEmail;

    @Column(name = "unsubscribe_link")
    @Builder.Default
    private Boolean includeUnsubscribeLink = true;

    @Column(name = "track_opens")
    @Builder.Default
    private Boolean trackOpens = true;

    @Column(name = "track_clicks")
    @Builder.Default
    private Boolean trackClicks = true;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @OneToMany(mappedBy = "sequence", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<EmailSequenceStep> steps = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addStep(EmailSequenceStep step) {
        steps.add(step);
        step.setSequence(this);
    }

    public void removeStep(EmailSequenceStep step) {
        steps.remove(step);
        step.setSequence(null);
    }

    public int getStepCount() {
        return steps.size();
    }

    public int getTotalDurationDays() {
        return steps.stream()
            .mapToInt(s -> s.getDelayDays() != null ? s.getDelayDays() : 0)
            .sum();
    }
}
