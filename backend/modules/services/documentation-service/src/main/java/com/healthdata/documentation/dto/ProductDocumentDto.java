package com.healthdata.documentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentDto {

    private String id;

    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 100, message = "Title must be between 10 and 100 characters")
    private String title;

    @NotBlank(message = "Portal type is required")
    private String portalType;

    @NotBlank(message = "Path is required")
    private String path;

    @NotBlank(message = "Category is required")
    private String category;

    private String subcategory;

    @NotNull
    @Size(min = 3, max = 10, message = "Must have between 3 and 10 tags")
    private String[] tags;

    private String[] relatedDocuments;

    @NotBlank(message = "Summary is required")
    private String summary;

    private Integer estimatedReadTime;

    private String difficulty;

    private LocalDate lastUpdated;

    private String[] targetAudience;

    private String accessLevel;

    private String owner;

    private String reviewCycle;

    private LocalDate nextReviewDate;

    private String status;

    private String version;

    private LocalDate lastReviewed;

    private String[] seoKeywords;

    private Map<String, Object> externalLinks;

    private Boolean hasVideo;

    private String videoUrl;

    private Integer wordCount;

    private LocalDate createdDate;

    private Integer viewCount;

    private BigDecimal avgRating;

    private Integer feedbackCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<DocumentVersionDto> versions;
}
