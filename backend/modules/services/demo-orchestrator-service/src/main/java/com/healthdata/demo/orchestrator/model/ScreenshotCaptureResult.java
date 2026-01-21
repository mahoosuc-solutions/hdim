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
public class ScreenshotCaptureResult {
    private String phase;
    private String scenarioId;
    private String userType;
    private String outputDirectory;
    private Integer screenshotCount;
    private List<String> screenshotFiles;
    private String status;
    private String errorMessage;
}
