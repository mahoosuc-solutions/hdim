package com.healthdata.investor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for InvestorTask entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {

    private UUID id;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;

    private String status;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Week is required")
    private Integer week;

    private String deliverable;

    private String owner;

    private LocalDate dueDate;

    private Instant completedAt;

    private String notes;

    private Integer sortOrder;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Request DTO for creating a task.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Subject is required")
        private String subject;

        private String description;

        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Week is required")
        private Integer week;

        private String deliverable;

        private String owner;

        private LocalDate dueDate;

        private String notes;
    }

    /**
     * Request DTO for updating a task.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String subject;
        private String description;
        private String status;
        private String category;
        private Integer week;
        private String deliverable;
        private String owner;
        private LocalDate dueDate;
        private String notes;
    }
}
