package com.healthdata.sales.dto;

import com.healthdata.sales.entity.EmailSequenceStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSequenceStepDTO {

    private UUID id;
    private UUID sequenceId;

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    private Integer delayDays;
    private Integer delayHours;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String previewText;

    @NotBlank(message = "Email body is required")
    private String bodyHtml;

    private String bodyText;
    private String templateId;
    private EmailSequenceStep.StepType stepType;
    private Boolean active;
    private String sendTimePreference;
    private Boolean skipWeekends;

    // Conditional logic
    private String conditionField;
    private String conditionOperator;
    private String conditionValue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
