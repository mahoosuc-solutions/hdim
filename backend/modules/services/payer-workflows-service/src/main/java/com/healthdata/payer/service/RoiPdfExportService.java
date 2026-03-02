package com.healthdata.payer.service;

import com.healthdata.payer.domain.RoiCalculation;
import com.healthdata.payer.repository.RoiCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoiPdfExportService {

    private final RoiCalculationRepository repository;

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat PERCENT = NumberFormat.getPercentInstance(Locale.US);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("MMMM d, yyyy").withZone(ZoneId.of("UTC"));

    static {
        CURRENCY.setMaximumFractionDigits(0);
        PERCENT.setMaximumFractionDigits(0);
    }

    /**
     * Generate a branded PDF report for a saved ROI calculation.
     * Returns empty if the calculation ID is not found.
     */
    public Optional<byte[]> generatePdf(String calculationId) {
        return repository.findById(calculationId).map(this::buildPdf);
    }

    private byte[] buildPdf(RoiCalculation calc) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 720;
                float margin = 60;
                float width = page.getMediaBox().getWidth() - 2 * margin;

                // Title
                y = drawText(cs, fontBold, 20, margin, y, "HDIM ROI Analysis Report");
                y -= 5;

                // Date & contact
                if (calc.getCreatedAt() != null) {
                    y = drawText(cs, fontRegular, 10, margin, y, "Generated: " + DATE_FMT.format(calc.getCreatedAt()));
                }
                if (calc.getContactCompany() != null) {
                    y = drawText(cs, fontRegular, 10, margin, y, "Prepared for: " + calc.getContactCompany());
                }
                if (calc.getContactName() != null) {
                    y = drawText(cs, fontRegular, 10, margin, y, "Contact: " + calc.getContactName());
                }

                y -= 15;

                // Organization Profile section
                y = drawText(cs, fontBold, 14, margin, y, "Organization Profile");
                y -= 3;
                y = drawRow(cs, fontRegular, margin, y, width, "Organization Type", calc.getOrgType().displayName);
                y = drawRow(cs, fontRegular, margin, y, width, "Patient Population", String.format("%,d", calc.getPatientPopulation()));
                y = drawRow(cs, fontRegular, margin, y, width, "Current Quality Score", calc.getCurrentQualityScore() + "%");
                y = drawRow(cs, fontRegular, margin, y, width, "Current Star Rating", calc.getCurrentStarRating() + " stars");
                y = drawRow(cs, fontRegular, margin, y, width, "Manual Reporting Hours/week", String.valueOf(calc.getManualReportingHours()));

                y -= 15;

                // Projected Improvements
                y = drawText(cs, fontBold, 14, margin, y, "Projected Improvements");
                y -= 3;
                y = drawRow(cs, fontRegular, margin, y, width, "Quality Score Improvement", "+" + calc.getQualityImprovement() + " points");
                y = drawRow(cs, fontRegular, margin, y, width, "Projected Quality Score", calc.getProjectedScore() + "%");
                y = drawRow(cs, fontRegular, margin, y, width, "Projected Star Rating", calc.getProjectedStarRating() + " stars");

                y -= 15;

                // Financial Impact
                y = drawText(cs, fontBold, 14, margin, y, "Year 1 Financial Impact");
                y -= 3;
                y = drawRow(cs, fontRegular, margin, y, width, "Quality Bonuses & Shared Savings", formatCurrency(calc.getQualityBonuses()));
                y = drawRow(cs, fontRegular, margin, y, width, "Administrative Savings", formatCurrency(calc.getAdminSavings()));
                y = drawRow(cs, fontRegular, margin, y, width, "Care Gap Closure Value", formatCurrency(calc.getGapClosureValue()));
                y -= 5;
                y = drawRow(cs, fontBold, margin, y, width, "Total Year 1 Value", formatCurrency(calc.getTotalYear1Value()));
                y = drawRow(cs, fontRegular, margin, y, width, "Year 1 Investment", formatCurrency(calc.getYear1Investment()));

                y -= 15;

                // ROI Summary
                y = drawText(cs, fontBold, 14, margin, y, "Return on Investment");
                y -= 3;
                y = drawRow(cs, fontBold, margin, y, width, "Year 1 ROI", calc.getYear1ROI() + "%");
                y = drawRow(cs, fontRegular, margin, y, width, "Payback Period", calc.getPaybackDays() + " days");
                y = drawRow(cs, fontBold, margin, y, width, "3-Year Net Present Value", formatCurrency(calc.getThreeYearNPV()));

                y -= 30;

                // Disclaimer
                drawText(cs, fontRegular, 8, margin, y,
                        "This report is generated by HealthData-in-Motion (HDIM). Projections are based on industry benchmarks.");
                y -= 12;
                drawText(cs, fontRegular, 8, margin, y,
                        "Actual results may vary based on implementation, data quality, and organizational factors.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate ROI PDF", e);
        }
    }

    private float drawText(PDPageContentStream cs, PDType1Font font, float size,
                           float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - size - 4;
    }

    private float drawRow(PDPageContentStream cs, PDType1Font font, float x, float y,
                          float width, String label, String value) throws IOException {
        float fontSize = 11;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x + width - font.getStringWidth(value) / 1000 * fontSize, y);
        cs.showText(value);
        cs.endText();

        return y - fontSize - 4;
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "$0";
        return CURRENCY.format(value.longValue());
    }
}
