package com.healthdata.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFeedbackDto {

    private UUID id;

    private String documentId;

    private String userId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private String comment;

    private Boolean helpful;

    private String feedbackType;

    private String status;

    private String adminResponse;

    private String respondedBy;

    private LocalDateTime respondedAt;

    private LocalDateTime createdAt;
}
