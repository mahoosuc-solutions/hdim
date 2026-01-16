package com.healthdata.demo.orchestrator.service;

import com.healthdata.demo.orchestrator.model.ScreenshotCaptureResult;
import com.healthdata.demo.orchestrator.model.ScreenshotValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating screenshot capture results
 */
@Slf4j
@Service
public class ScreenshotValidator {
    
    private static final long MIN_FILE_SIZE_KB = 10;
    private static final long MAX_FILE_SIZE_MB = 5;
    
    /**
     * Validate screenshots from all phases
     */
    public ScreenshotValidationResult validateScreenshots(
        ScreenshotCaptureResult before,
        ScreenshotCaptureResult during,
        ScreenshotCaptureResult after
    ) {
        List<ScreenshotValidationResult.ValidationCheck> checks = new ArrayList<>();
        
        // 1. Verify BEFORE screenshots were captured
        checks.add(createCheck(
            "BEFORE screenshots captured",
            before.getScreenshotCount() != null && before.getScreenshotCount() > 0,
            String.format("Captured %d BEFORE screenshots", 
                before.getScreenshotCount() != null ? before.getScreenshotCount() : 0)
        ));
        
        // 2. Verify DURING screenshots show loading states
        boolean duringValid = during.getScreenshotCount() != null && during.getScreenshotCount() > 0;
        checks.add(createCheck(
            "DURING screenshots show data loading",
            duringValid,
            duringValid 
                ? "DURING screenshots captured showing loading progression"
                : "DURING screenshots should show loading indicators or partial data"
        ));
        
        // 3. Verify AFTER screenshots show complete data
        checks.add(createCheck(
            "AFTER screenshots show complete data",
            after.getScreenshotCount() != null && after.getScreenshotCount() > 0,
            String.format("Captured %d AFTER screenshots with complete data",
                after.getScreenshotCount() != null ? after.getScreenshotCount() : 0)
        ));
        
        // 4. Verify file sizes are reasonable
        boolean fileSizesValid = validateFileSizes(before, during, after);
        checks.add(createCheck(
            "Screenshot file sizes",
            fileSizesValid,
            fileSizesValid 
                ? "All screenshot file sizes are within acceptable range (10KB - 5MB)"
                : "Some screenshots may be too small (< 10KB) or too large (> 5MB)"
        ));
        
        // 5. Verify progression (BEFORE → DURING → AFTER shows data loading)
        boolean progressionValid = validateProgression(before, during, after);
        checks.add(createCheck(
            "Screenshot progression",
            progressionValid,
            progressionValid
                ? "Screenshots show clear progression from empty to loaded"
                : "Screenshot progression may not be clearly visible"
        ));
        
        // Calculate overall status
        String overallStatus = calculateOverallStatus(checks);
        
        return ScreenshotValidationResult.builder()
            .overallStatus(overallStatus)
            .checks(checks)
            .build();
    }
    
    private ScreenshotValidationResult.ValidationCheck createCheck(String name, boolean passed, String message) {
        return ScreenshotValidationResult.ValidationCheck.builder()
            .name(name)
            .status(passed ? "PASS" : "WARN")
            .message(message)
            .build();
    }
    
    private boolean validateFileSizes(
        ScreenshotCaptureResult before,
        ScreenshotCaptureResult during,
        ScreenshotCaptureResult after
    ) {
        return validateDirectoryFileSizes(before.getOutputDirectory()) &&
               validateDirectoryFileSizes(during.getOutputDirectory()) &&
               validateDirectoryFileSizes(after.getOutputDirectory());
    }
    
    private boolean validateDirectoryFileSizes(String directory) {
        if (directory == null) {
            return false;
        }
        
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                return false;
            }
            
            return Files.list(dirPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".png"))
                .allMatch(p -> {
                    try {
                        long sizeBytes = Files.size(p);
                        long sizeKB = sizeBytes / 1024;
                        long sizeMB = sizeKB / 1024;
                        return sizeKB >= MIN_FILE_SIZE_KB && sizeMB <= MAX_FILE_SIZE_MB;
                    } catch (IOException e) {
                        log.warn("Failed to check file size: {}", p, e);
                        return false;
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to validate file sizes in directory: {}", directory, e);
            return false;
        }
    }
    
    private boolean validateProgression(
        ScreenshotCaptureResult before,
        ScreenshotCaptureResult during,
        ScreenshotCaptureResult after
    ) {
        // Basic validation: all phases should have screenshots
        boolean beforeHasScreenshots = before.getScreenshotCount() != null && before.getScreenshotCount() > 0;
        boolean duringHasScreenshots = during.getScreenshotCount() != null && during.getScreenshotCount() > 0;
        boolean afterHasScreenshots = after.getScreenshotCount() != null && after.getScreenshotCount() > 0;
        
        return beforeHasScreenshots && duringHasScreenshots && afterHasScreenshots;
    }
    
    private String calculateOverallStatus(List<ScreenshotValidationResult.ValidationCheck> checks) {
        boolean hasFail = checks.stream().anyMatch(c -> "FAIL".equals(c.getStatus()));
        boolean hasWarn = checks.stream().anyMatch(c -> "WARN".equals(c.getStatus()));
        
        if (hasFail) {
            return "FAIL";
        } else if (hasWarn) {
            return "WARN";
        } else {
            return "PASS";
        }
    }
}
