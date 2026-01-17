package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing paginated vitals history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vital signs history with pagination")
public class VitalsHistoryResponse {

    @Schema(description = "List of vital sign records")
    private List<VitalSignsResponse> vitals;

    @Schema(description = "Total number of records", example = "50")
    private Long totalRecords;

    @Schema(description = "Current page number", example = "0")
    private Integer currentPage;

    @Schema(description = "Page size", example = "20")
    private Integer pageSize;

    @Schema(description = "Total number of pages", example = "3")
    private Integer totalPages;
}
