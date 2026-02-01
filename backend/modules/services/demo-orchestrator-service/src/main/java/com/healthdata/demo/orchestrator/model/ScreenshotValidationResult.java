package com.healthdata.demo.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotValidationResult {
    private String overallStatus;
    private List<ValidationCheck> checks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationCheck {
        private String name;
        private String status;
        private String message;
    }
}
