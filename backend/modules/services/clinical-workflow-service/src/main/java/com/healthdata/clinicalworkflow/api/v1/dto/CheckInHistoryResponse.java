package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing paginated check-in history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Check-in history with pagination")
public class CheckInHistoryResponse {

    @Schema(description = "List of check-in records")
    private List<CheckInResponse> checkIns;

    @Schema(description = "Total number of records", example = "150")
    private Long totalRecords;

    @Schema(description = "Current page number", example = "0")
    private Integer currentPage;

    @Schema(description = "Page size", example = "20")
    private Integer pageSize;

    @Schema(description = "Total number of pages", example = "8")
    private Integer totalPages;
}
