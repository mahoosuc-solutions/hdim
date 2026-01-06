package com.healthdata.predictive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Request to dismiss an insight with reason tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDismissalRequest {

    @NotNull(message = "Insight ID is required")
    private UUID insightId;

    @NotBlank(message = "Dismissal reason is required")
    private String reason;

    /**
     * Standard dismissal reasons
     */
    public enum DismissalReason {
        ALREADY_ADDRESSED("Already addressed through other means"),
        NOT_APPLICABLE("Not applicable to my practice"),
        PATIENT_DECLINED("Patient declined intervention"),
        WILL_ADDRESS_LATER("Will address at a later time"),
        INCORRECT_DATA("Data appears to be incorrect"),
        OTHER("Other reason");

        private final String description;

        DismissalReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
