package com.healthdata.demo.orchestrator.service;

import com.healthdata.demo.orchestrator.model.ScreenshotCaptureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class ScreenshotCaptureService {
    private static final String SCREENSHOT_SCRIPT_PATH = "scripts/capture-screenshots.js";
    private static final String BASE_OUTPUT_DIR = "docs/screenshots/scenarios";
    
    public ScreenshotCaptureResult captureBefore(String scenarioId, String userType) {
        return executeScreenshotCapture(scenarioId, userType, "BEFORE");
    }
    
    public ScreenshotCaptureResult captureDuring(String scenarioId, String userType) {
        return executeScreenshotCapture(scenarioId, userType, "DURING");
    }
    
    public ScreenshotCaptureResult captureAfter(String scenarioId, String userType) {
        return executeScreenshotCapture(scenarioId, userType, "AFTER");
    }
    
    private ScreenshotCaptureResult executeScreenshotCapture(String scenarioId, String userType, String phase) {
        String outputDir = String.format("%s/%s/%s", BASE_OUTPUT_DIR, scenarioId, phase.toLowerCase());
        log.info("Capturing {} screenshots for scenario: {}, userType: {}", phase, scenarioId, userType);
        
        try {
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder("node", SCREENSHOT_SCRIPT_PATH,
                "--scenario", scenarioId, "--user-type", userType, "--phase", phase, "--output-dir", outputDir);
            processBuilder.directory(new File("."));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                int screenshotCount = countScreenshots(outputDir);
                List<String> screenshotFiles = listScreenshotFiles(outputDir);
                log.info("Successfully captured {} {} screenshots for scenario: {}", screenshotCount, phase, scenarioId);
                
                return ScreenshotCaptureResult.builder()
                    .phase(phase).scenarioId(scenarioId).userType(userType).outputDirectory(outputDir)
                    .screenshotCount(screenshotCount).screenshotFiles(screenshotFiles).status("SUCCESS").build();
            } else {
                String errorMsg = String.format("Screenshot capture script exited with code: %d", exitCode);
                log.error(errorMsg);
                return ScreenshotCaptureResult.builder()
                    .phase(phase).scenarioId(scenarioId).userType(userType).status("FAILED").errorMessage(errorMsg).build();
            }
        } catch (Exception e) {
            String errorMsg = String.format("Failed to capture screenshots: %s", e.getMessage());
            log.error(errorMsg, e);
            return ScreenshotCaptureResult.builder()
                .phase(phase).scenarioId(scenarioId).userType(userType).status("FAILED").errorMessage(errorMsg).build();
        }
    }
    
    private int countScreenshots(String directory) {
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) return 0;
            try (Stream<Path> paths = Files.list(dirPath)) {
                return (int) paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".png")).count();
            }
        } catch (IOException e) {
            log.warn("Failed to count screenshots in directory: {}", directory, e);
            return 0;
        }
    }
    
    private List<String> listScreenshotFiles(String directory) {
        List<String> files = new ArrayList<>();
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) return files;
            try (Stream<Path> paths = Files.list(dirPath)) {
                paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".png"))
                    .forEach(p -> files.add(p.getFileName().toString()));
            }
        } catch (IOException e) {
            log.warn("Failed to list screenshot files in directory: {}", directory, e);
        }
        return files;
    }
}
