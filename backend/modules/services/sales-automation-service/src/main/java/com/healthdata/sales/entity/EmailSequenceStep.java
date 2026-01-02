package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Individual step in an email sequence
 */
@Entity
@Table(name = "email_sequence_steps", indexes = {
    @Index(name = "idx_sequence_steps_sequence", columnList = "sequence_id"),
    @Index(name = "idx_sequence_steps_order", columnList = "step_order")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSequenceStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_id", nullable = false)
    private EmailSequence sequence;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "delay_days", nullable = false)
    @Builder.Default
    private Integer delayDays = 0;

    @Column(name = "delay_hours")
    @Builder.Default
    private Integer delayHours = 0;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "preview_text", length = 255)
    private String previewText;

    @Column(name = "body_html", columnDefinition = "TEXT", nullable = false)
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "template_id", length = 100)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false)
    @Builder.Default
    private StepType stepType = StepType.EMAIL;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "send_time_preference", length = 50)
    private String sendTimePreference;  // e.g., "09:00", "MORNING", "AFTERNOON"

    @Column(name = "skip_weekends")
    @Builder.Default
    private Boolean skipWeekends = true;

    @Column(name = "condition_field", length = 100)
    private String conditionField;

    @Column(name = "condition_operator", length = 50)
    private String conditionOperator;

    @Column(name = "condition_value", length = 255)
    private String conditionValue;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum StepType {
        EMAIL,          // Send email
        WAIT,           // Just wait (for complex timing)
        TASK,           // Create a task
        CONDITION       // Conditional branch (future)
    }

    /**
     * Get total delay in hours from sequence start to this step
     */
    public int getTotalDelayHours() {
        int hours = (delayDays != null ? delayDays : 0) * 24;
        hours += (delayHours != null ? delayHours : 0);
        return hours;
    }
}
