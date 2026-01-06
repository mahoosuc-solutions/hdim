package com.healthdata.patient.dto;

import com.healthdata.patient.entity.ProviderPanelAssignmentEntity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for provider panel assignment operations.
 * Issue #135: Create Provider Panel Assignment API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPanelRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @Builder.Default
    private ProviderPanelAssignmentEntity.AssignmentType assignmentType =
            ProviderPanelAssignmentEntity.AssignmentType.PRIMARY;

    private String notes;

    /**
     * Bulk assignment request with multiple patient IDs
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkAssignment {

        @NotNull(message = "Patient IDs are required")
        private List<UUID> patientIds;

        @Builder.Default
        private ProviderPanelAssignmentEntity.AssignmentType assignmentType =
                ProviderPanelAssignmentEntity.AssignmentType.PRIMARY;
    }
}
