package com.healthdata.documentation.service;

import com.healthdata.documentation.persistence.DocumentAttachmentEntity;
import com.healthdata.documentation.repository.DocumentAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OCR Service for extracting text from documents
 * Supports PDF and image files (PNG, JPG, JPEG, TIFF)
 * Uses Tesseract OCR engine for text extraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private final DocumentAttachmentRepository attachmentRepository;
    private final Tesseract tesseract;

    /**
     * Process document asynchronously for OCR text extraction
     * Updates attachment record with extracted text or error message
     */
    @Async
    @Transactional
    public void processDocumentAsync(UUID attachmentId, String tenantId) {
        log.info("Starting async OCR processing for attachment {}", attachmentId);

        DocumentAttachmentEntity attachment = attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        // Skip if not OCR-supported file type
        if (!attachment.isOcrSupported()) {
            log.warn("Attachment {} has unsupported file type for OCR: {}", attachmentId, attachment.getContentType());
            return;
        }

        // Update status to PROCESSING
        attachment.setOcrStatus("PROCESSING");
        attachmentRepository.save(attachment);

        try {
            String extractedText;

            if (attachment.isPdf()) {
                extractedText = extractTextFromPdf(attachment);
            } else if (attachment.isImage()) {
                extractedText = extractTextFromImage(attachment);
            } else {
                throw new IllegalStateException("Unsupported file type: " + attachment.getContentType());
            }

            // Update attachment with extracted text
            attachment.setOcrText(extractedText);
            attachment.setOcrStatus("COMPLETED");
            attachment.setOcrProcessedAt(LocalDateTime.now());
            attachment.setOcrErrorMessage(null);

            attachmentRepository.save(attachment);
            log.info("OCR processing completed for attachment {}. Extracted {} characters.",
                     attachmentId, extractedText != null ? extractedText.length() : 0);

        } catch (Exception e) {
            log.error("OCR processing failed for attachment {}", attachmentId, e);

            // Update attachment with error status
            attachment.setOcrStatus("FAILED");
            attachment.setOcrProcessedAt(LocalDateTime.now());
            attachment.setOcrErrorMessage(e.getMessage());
            attachment.setOcrText(null);

            attachmentRepository.save(attachment);
        }
    }

    /**
     * Extract text from PDF file
     * First tries native PDF text extraction, falls back to OCR if needed
     */
    private String extractTextFromPdf(DocumentAttachmentEntity attachment) throws IOException, TesseractException {
        Path filePath = Paths.get(attachment.getStoragePath());
        File pdfFile = filePath.toFile();

        log.debug("Extracting text from PDF: {}", pdfFile.getAbsolutePath());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            // Try native text extraction first
            PDFTextStripper stripper = new PDFTextStripper();
            String nativeText = stripper.getText(document);

            // If native extraction yields sufficient text, use it
            if (nativeText != null && nativeText.trim().length() > 50) {
                log.debug("PDF native text extraction successful ({} characters)", nativeText.length());
                return nativeText.trim();
            }

            // Otherwise, fall back to OCR (for scanned PDFs)
            log.debug("PDF has minimal native text, using OCR fallback");
            return extractTextFromPdfViaOcr(document);
        }
    }

    /**
     * Extract text from PDF using OCR (for scanned PDFs)
     * Renders each page as an image and runs Tesseract OCR
     */
    private String extractTextFromPdfViaOcr(PDDocument document) throws IOException, TesseractException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder allText = new StringBuilder();

        int pageCount = document.getNumberOfPages();
        log.debug("Processing {} pages with OCR", pageCount);

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            // Render page to image at 300 DPI for better OCR accuracy
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);

            // Run OCR on page image
            String pageText = tesseract.doOCR(image);
            if (pageText != null && !pageText.trim().isEmpty()) {
                allText.append(pageText).append("\n\n");
            }

            log.debug("OCR processed page {} of {}", pageIndex + 1, pageCount);
        }

        return allText.toString().trim();
    }

    /**
     * Extract text from image file using Tesseract OCR
     */
    private String extractTextFromImage(DocumentAttachmentEntity attachment) throws IOException, TesseractException {
        Path filePath = Paths.get(attachment.getStoragePath());
        File imageFile = filePath.toFile();

        log.debug("Extracting text from image: {}", imageFile.getAbsolutePath());

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("Failed to read image file: " + imageFile.getAbsolutePath());
        }

        String extractedText = tesseract.doOCR(image);
        return extractedText != null ? extractedText.trim() : "";
    }
}
