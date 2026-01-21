package com.healthdata.demo.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotResults {
    private ScreenshotCaptureResult before;
    private ScreenshotCaptureResult during;
    private ScreenshotCaptureResult after;
    private ScreenshotValidationResult validation;
}
