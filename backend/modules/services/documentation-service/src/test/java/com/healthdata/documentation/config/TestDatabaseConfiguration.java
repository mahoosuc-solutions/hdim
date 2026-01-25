package com.healthdata.documentation.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test-specific configuration.
 *
 * This configuration:
 * 1. Excludes the custom DatabaseAutoConfiguration and allows Spring Boot's
 *    default DataSource auto-configuration to work with Testcontainers
 * 2. Provides a mock Tesseract bean to avoid requiring Tesseract installation
 *    in test environments
 */
@TestConfiguration
public class TestDatabaseConfiguration {

    /**
     * Mock Tesseract bean for testing OCR functionality
     * Returns simulated OCR text without requiring actual Tesseract installation
     */
    @Bean
    @Primary
    public Tesseract tesseract() throws Exception {
        Tesseract mockTesseract = mock(Tesseract.class);

        // Simulate OCR text extraction for both File and BufferedImage inputs
        when(mockTesseract.doOCR(any(java.io.File.class))).thenReturn(
            "This is simulated OCR text extracted from the test document.\n" +
            "Patient: John Doe\n" +
            "Date: 2024-01-15\n" +
            "Lab Results: Normal"
        );

        when(mockTesseract.doOCR(any(java.awt.image.BufferedImage.class))).thenReturn(
            "This is simulated OCR text extracted from the test document.\n" +
            "Patient: John Doe\n" +
            "Date: 2024-01-15\n" +
            "Lab Results: Normal"
        );

        return mockTesseract;
    }
}
