package com.healthdata.documentation.config;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OCR Configuration
 * Configures Tesseract OCR engine for document text extraction
 */
@Configuration
@EnableAsync
@Slf4j
public class OcrConfiguration {

    /**
     * Tesseract OCR instance
     * Configured for English language with LSTM neural network model
     */
    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();

        // Set tessdata path (directory containing trained language data)
        // Common locations:
        // - /usr/share/tesseract-ocr/5/tessdata (Ubuntu/Debian)
        // - /usr/local/share/tessdata (macOS Homebrew)
        // - /opt/homebrew/share/tessdata (macOS Apple Silicon)
        // - /usr/share/tessdata (Alpine Linux - Docker)
        String tessdataPath = findTessdataPath();
        if (tessdataPath != null) {
            tesseract.setDatapath(tessdataPath);
            log.info("Tesseract tessdata path: {}", tessdataPath);
        } else {
            log.warn("Tesseract tessdata not found in standard locations. OCR may fail.");
            log.warn("Install tesseract-ocr: apt-get install tesseract-ocr tesseract-ocr-eng");
        }

        // Set language to English (eng)
        tesseract.setLanguage("eng");

        // Set OCR Engine Mode (OEM)
        // 0 = Legacy engine only
        // 1 = Neural nets LSTM engine only (recommended)
        // 2 = Legacy + LSTM engines
        // 3 = Default (based on what is available)
        tesseract.setOcrEngineMode(1); // LSTM only for better accuracy

        // Set Page Segmentation Mode (PSM)
        // 3 = Fully automatic page segmentation (default)
        // 6 = Assume a single uniform block of text
        tesseract.setPageSegMode(3);

        log.info("Tesseract OCR configured: language=eng, oem=1 (LSTM), psm=3 (auto)");

        return tesseract;
    }

    /**
     * Find tessdata directory in common locations
     * Returns first existing path, or null if not found
     */
    private String findTessdataPath() {
        String[] possiblePaths = {
            "/usr/share/tessdata",                      // Alpine Linux (Docker)
            "/usr/share/tesseract-ocr/5/tessdata",     // Ubuntu/Debian
            "/usr/share/tesseract-ocr/4.00/tessdata",  // Older Ubuntu
            "/usr/local/share/tessdata",               // macOS Homebrew Intel
            "/opt/homebrew/share/tessdata",            // macOS Homebrew Apple Silicon
            System.getenv("TESSDATA_PREFIX")           // Environment variable override
        };

        for (String pathStr : possiblePaths) {
            if (pathStr == null) continue;
            Path path = Paths.get(pathStr);
            if (Files.exists(path) && Files.isDirectory(path)) {
                // Verify eng.traineddata exists
                Path engData = path.resolve("eng.traineddata");
                if (Files.exists(engData)) {
                    return pathStr;
                }
            }
        }

        return null;
    }
}
