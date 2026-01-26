import { Injectable } from '@angular/core';
import { LoggerService } from './logger.service';
import { QualityMeasureResult } from '../models/quality-result.model';
import { LoggerService } from './logger.service';

export interface ReportOptions {
  title?: string;
  subtitle?: string;
  dateRange?: { from: Date; to: Date };
  includeCharts?: boolean;
  includeDetails?: boolean;
  groupByMeasure?: boolean;
}

export interface ComplianceSummary {
  totalEvaluations: number;
  compliantCount: number;
  nonCompliantCount: number;
  notEligibleCount: number;
  overallComplianceRate: number;
  measureBreakdown: MeasureStats[];
}

export interface MeasureStats {
  measureName: string;
  measureCategory: string;
  evaluations: number;
  compliant: number;
  eligible: number;
  complianceRate: number;
}

/**
 * Service for generating PDF and enhanced export reports.
 *
 * Provides methods to export quality measure results in various formats
 * including styled PDF reports with summary statistics.
 */
@Injectable({
  providedIn: 'root',
})
export class ReportExportService {
  private readonly logger = this.loggerService.withContext('ReportExportService');

  constructor(private readonly loggerService: LoggerService) {}
  /**
   * Generate a PDF report from quality measure results.
   * Uses native browser print-to-PDF functionality for maximum compatibility.
   */
  generatePDFReport(
    results: QualityMeasureResult[],
    options: ReportOptions = {}
  ): void {
    const summary = this.calculateSummary(results);
    const htmlContent = this.generateReportHTML(results, summary, options);

    // Open print dialog with the report content
    this.printToPDF(htmlContent, options.title || 'Quality Measure Report');
  }

  /**
   * Calculate compliance summary statistics from results.
   */
  calculateSummary(results: QualityMeasureResult[]): ComplianceSummary {
    const compliantCount = results.filter((r) => r.numeratorCompliant).length;
    const nonCompliantCount = results.filter(
      (r) => r.denominatorEligible && !r.numeratorCompliant
    ).length;
    const notEligibleCount = results.filter((r) => !r.denominatorEligible).length;

    const eligibleResults = results.filter((r) => r.denominatorEligible);
    const overallComplianceRate =
      eligibleResults.length > 0
        ? (compliantCount / eligibleResults.length) * 100
        : 0;

    // Group by measure for breakdown
    const measureMap = new Map<
      string,
      { name: string; category: string; compliant: number; eligible: number; total: number }
    >();

    results.forEach((result) => {
      const key = result.measureName || 'Unknown';
      if (!measureMap.has(key)) {
        measureMap.set(key, {
          name: result.measureName || 'Unknown',
          category: result.measureCategory || 'HEDIS',
          compliant: 0,
          eligible: 0,
          total: 0,
        });
      }

      const stats = measureMap.get(key)!;
      stats.total++;
      if (result.denominatorEligible) {
        stats.eligible++;
        if (result.numeratorCompliant) {
          stats.compliant++;
        }
      }
    });

    const measureBreakdown: MeasureStats[] = Array.from(measureMap.values())
      .map((stats) => ({
        measureName: stats.name,
        measureCategory: stats.category,
        evaluations: stats.total,
        compliant: stats.compliant,
        eligible: stats.eligible,
        complianceRate: stats.eligible > 0 ? (stats.compliant / stats.eligible) * 100 : 0,
      }))
      .sort((a, b) => b.complianceRate - a.complianceRate);

    return {
      totalEvaluations: results.length,
      compliantCount,
      nonCompliantCount,
      notEligibleCount,
      overallComplianceRate,
      measureBreakdown,
    };
  }

  /**
   * Generate HTML content for the PDF report.
   */
  private generateReportHTML(
    results: QualityMeasureResult[],
    summary: ComplianceSummary,
    options: ReportOptions
  ): string {
    const title = options.title || 'Quality Measure Compliance Report';
    const subtitle = options.subtitle || 'Healthcare Data in Motion Platform';
    const generatedDate = new Date().toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });

    let dateRangeText = '';
    if (options.dateRange) {
      const fromStr = options.dateRange.from.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
      const toStr = options.dateRange.to.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
      dateRangeText = `<p class="date-range">Reporting Period: ${fromStr} - ${toStr}</p>`;
    }

    const measureBreakdownRows = summary.measureBreakdown
      .map(
        (m) => `
        <tr>
          <td>${this.escapeHtml(m.measureName)}</td>
          <td class="center">${m.measureCategory}</td>
          <td class="center">${m.evaluations}</td>
          <td class="center">${m.compliant}</td>
          <td class="center">${m.eligible}</td>
          <td class="center ${this.getComplianceClass(m.complianceRate)}">${m.complianceRate.toFixed(1)}%</td>
        </tr>
      `
      )
      .join('');

    let detailsSection = '';
    if (options.includeDetails !== false) {
      const detailRows = results
        .slice(0, 100) // Limit to first 100 for performance
        .map(
          (r) => `
          <tr>
            <td>${this.formatDate(r.calculationDate)}</td>
            <td>${r.patientId}</td>
            <td>${this.escapeHtml(r.measureName || 'N/A')}</td>
            <td class="center ${this.getStatusClass(r)}">${this.getStatusText(r)}</td>
            <td class="center">${r.complianceRate?.toFixed(1) || 0}%</td>
          </tr>
        `
        )
        .join('');

      const totalShown = Math.min(results.length, 100);
      const moreNote =
        results.length > 100
          ? `<p class="note">Showing ${totalShown} of ${results.length} evaluations</p>`
          : '';

      detailsSection = `
        <div class="section">
          <h2>Evaluation Details</h2>
          ${moreNote}
          <table class="details-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Patient ID</th>
                <th>Measure</th>
                <th>Status</th>
                <th>Compliance</th>
              </tr>
            </thead>
            <tbody>
              ${detailRows}
            </tbody>
          </table>
        </div>
      `;
    }

    return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>${title}</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      font-size: 12px;
      line-height: 1.5;
      color: #333;
      background: white;
      padding: 20px;
    }

    .header {
      text-align: center;
      margin-bottom: 30px;
      padding-bottom: 20px;
      border-bottom: 2px solid #1976d2;
    }

    .header h1 {
      font-size: 24px;
      color: #1976d2;
      margin-bottom: 5px;
    }

    .header .subtitle {
      font-size: 14px;
      color: #666;
    }

    .header .generated-date {
      font-size: 11px;
      color: #999;
      margin-top: 10px;
    }

    .date-range {
      font-size: 12px;
      color: #555;
      margin-top: 5px;
    }

    .summary-grid {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      margin-bottom: 30px;
    }

    .stat-box {
      flex: 1;
      padding: 15px;
      border-radius: 8px;
      text-align: center;
    }

    .stat-box.compliant {
      background-color: #e8f5e9;
      border: 1px solid #4caf50;
    }

    .stat-box.non-compliant {
      background-color: #fff3e0;
      border: 1px solid #ff9800;
    }

    .stat-box.not-eligible {
      background-color: #e3f2fd;
      border: 1px solid #2196f3;
    }

    .stat-box.total {
      background-color: #f3e5f5;
      border: 1px solid #9c27b0;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 5px;
    }

    .stat-label {
      font-size: 11px;
      text-transform: uppercase;
      color: #666;
      letter-spacing: 0.5px;
    }

    .section {
      margin-bottom: 30px;
    }

    .section h2 {
      font-size: 16px;
      color: #333;
      margin-bottom: 15px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e0e0e0;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 11px;
    }

    th, td {
      padding: 10px 8px;
      text-align: left;
      border-bottom: 1px solid #e0e0e0;
    }

    th {
      background-color: #f5f5f5;
      font-weight: 600;
      color: #555;
    }

    tr:nth-child(even) {
      background-color: #fafafa;
    }

    .center {
      text-align: center;
    }

    .high-compliance {
      color: #2e7d32;
      font-weight: 600;
    }

    .medium-compliance {
      color: #ef6c00;
      font-weight: 600;
    }

    .low-compliance {
      color: #c62828;
      font-weight: 600;
    }

    .status-compliant {
      color: #2e7d32;
    }

    .status-non-compliant {
      color: #ef6c00;
    }

    .status-not-eligible {
      color: #757575;
    }

    .note {
      font-size: 11px;
      color: #666;
      font-style: italic;
      margin-bottom: 10px;
    }

    .footer {
      margin-top: 40px;
      padding-top: 20px;
      border-top: 1px solid #e0e0e0;
      text-align: center;
      font-size: 10px;
      color: #999;
    }

    @media print {
      body {
        padding: 0;
      }

      .section {
        page-break-inside: avoid;
      }

      table {
        page-break-inside: auto;
      }

      tr {
        page-break-inside: avoid;
        page-break-after: auto;
      }
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>${this.escapeHtml(title)}</h1>
    <p class="subtitle">${this.escapeHtml(subtitle)}</p>
    ${dateRangeText}
    <p class="generated-date">Generated on ${generatedDate}</p>
  </div>

  <div class="summary-grid">
    <div class="stat-box total">
      <div class="stat-value">${summary.overallComplianceRate.toFixed(1)}%</div>
      <div class="stat-label">Overall Compliance</div>
    </div>
    <div class="stat-box compliant">
      <div class="stat-value">${summary.compliantCount}</div>
      <div class="stat-label">Compliant</div>
    </div>
    <div class="stat-box non-compliant">
      <div class="stat-value">${summary.nonCompliantCount}</div>
      <div class="stat-label">Non-Compliant</div>
    </div>
    <div class="stat-box not-eligible">
      <div class="stat-value">${summary.notEligibleCount}</div>
      <div class="stat-label">Not Eligible</div>
    </div>
  </div>

  <div class="section">
    <h2>Measure Performance Summary</h2>
    <table>
      <thead>
        <tr>
          <th>Measure</th>
          <th class="center">Category</th>
          <th class="center">Evaluations</th>
          <th class="center">Compliant</th>
          <th class="center">Eligible</th>
          <th class="center">Compliance Rate</th>
        </tr>
      </thead>
      <tbody>
        ${measureBreakdownRows}
      </tbody>
    </table>
  </div>

  ${detailsSection}

  <div class="footer">
    <p>Healthcare Data in Motion - Quality Measure Reporting</p>
    <p>This report is generated for internal use and quality improvement purposes.</p>
  </div>
</body>
</html>
    `;
  }

  /**
   * Open print dialog to save as PDF.
   */
  private printToPDF(htmlContent: string, title: string): void {
    const printWindow = window.open('', '_blank', 'width=800,height=600');
    if (!printWindow) {
      this.logger.error('Failed to open print window. Please allow popups.');
      return;
    }

    printWindow.document.write(htmlContent);
    printWindow.document.close();
    printWindow.document.title = title;

    // Wait for content to load, then print
    printWindow.onload = () => {
      setTimeout(() => {
        printWindow.print();
      }, 250);
    };
  }

  /**
   * Generate a downloadable HTML report file.
   */
  downloadHTMLReport(results: QualityMeasureResult[], options: ReportOptions = {}): void {
    const summary = this.calculateSummary(results);
    const htmlContent = this.generateReportHTML(results, summary, options);

    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `quality-report-${new Date().toISOString().split('T')[0]}.html`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Helper to escape HTML entities.
   */
  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Format date for display.
   */
  private formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  /**
   * Get CSS class based on compliance rate.
   */
  private getComplianceClass(rate: number): string {
    if (rate >= 80) return 'high-compliance';
    if (rate >= 50) return 'medium-compliance';
    return 'low-compliance';
  }

  /**
   * Get status text for a result.
   */
  private getStatusText(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) return 'Compliant';
    if (result.denominatorEligible) return 'Non-Compliant';
    return 'Not Eligible';
  }

  /**
   * Get status CSS class for a result.
   */
  private getStatusClass(result: QualityMeasureResult): string {
    if (result.numeratorCompliant) return 'status-compliant';
    if (result.denominatorEligible) return 'status-non-compliant';
    return 'status-not-eligible';
  }
}
