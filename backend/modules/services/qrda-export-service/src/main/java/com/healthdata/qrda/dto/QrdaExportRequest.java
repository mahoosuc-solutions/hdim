package com.healthdata.qrda.dto;

import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for initiating a QRDA export job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrdaExportRequest {

    @NotNull(message = "Job type is required")
    private QrdaJobType jobType;

    @NotEmpty(message = "At least one measure ID is required")
    private List<String> measureIds;

    /**
     * Patient IDs to include. Required for QRDA_I, optional for QRDA_III.
     * If null for QRDA_III, all eligible patients will be included.
     */
    private List<UUID> patientIds;

    @NotNull(message = "Performance period start date is required")
    private LocalDate periodStart;

    @NotNull(message = "Performance period end date is required")
    private LocalDate periodEnd;

    /**
     * Whether to validate the generated documents using Schematron.
     * Defaults to true for production, can be disabled for testing.
     */
    @Builder.Default
    private boolean validateDocuments = true;

    /**
     * Whether to include supplemental data elements (race, ethnicity, sex, payer).
     * Required for most CMS reporting programs.
     */
    @Builder.Default
    private boolean includeSupplementalData = true;
}
