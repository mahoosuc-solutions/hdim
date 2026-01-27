import { Injectable } from '@angular/core';
import { LoggerService } from './logger.service';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { LoggerService } from './logger.service';
import { delay, map } from 'rxjs/operators';
import { LoggerService } from './logger.service';

/**
 * Report Section Types - Available sections for custom reports
 */
export type ReportSectionType =
  | 'executive-summary'
  | 'quality-measures'
  | 'care-gaps'
  | 'patient-risk'
  | 'custom-metrics'
  | 'trend-analysis'
  | 'provider-comparison'
  | 'patient-demographics';

/**
 * Report Section Definition
 */
export interface ReportSection {
  id: string;
  type: ReportSectionType;
  title: string;
  description: string;
  icon: string;
  enabled: boolean;
  order: number;
  configuration: ReportSectionConfig;
}

/**
 * Section-specific configuration
 */
export interface ReportSectionConfig {
  // Quality Measures section
  selectedMeasures?: string[];
  showCompliance?: boolean;
  showTrends?: boolean;
  chartType?: 'bar' | 'line' | 'pie' | 'radar';

  // Care Gaps section
  gapTypes?: string[];
  showUrgency?: boolean;
  groupByPatient?: boolean;

  // Patient Risk section
  riskLevels?: ('high' | 'medium' | 'low')[];
  showInterventions?: boolean;

  // Custom Metrics section
  metrics?: CustomMetric[];

  // Trend Analysis section
  trendPeriod?: 'monthly' | 'quarterly' | 'yearly';
  showProjections?: boolean;

  // Provider Comparison section
  providerIds?: string[];
  showBenchmarks?: boolean;

  // Patient Demographics section
  showAgeDistribution?: boolean;
  showConditionBreakdown?: boolean;
}

/**
 * Custom metric definition
 */
export interface CustomMetric {
  id: string;
  name: string;
  formula: string;
  displayFormat: 'percentage' | 'number' | 'currency';
  target?: number;
}

/**
 * Date range configuration
 */
export interface DateRangeConfig {
  type: 'ytd' | 'last-month' | 'last-quarter' | 'last-year' | 'custom';
  startDate?: Date;
  endDate?: Date;
  comparisonPeriod?: 'previous-period' | 'same-period-last-year' | 'none';
}

/**
 * Patient filter configuration
 */
export interface PatientFilterConfig {
  ageRange?: { min: number; max: number };
  riskLevels?: ('high' | 'medium' | 'low')[];
  conditions?: string[];
  providers?: string[];
  attributedOnly?: boolean;
}

/**
 * Export format options
 */
export type ExportFormat = 'pdf' | 'excel' | 'png' | 'csv';

/**
 * Schedule configuration for automated reports
 */
export interface ScheduleConfig {
  enabled: boolean;
  frequency: 'daily' | 'weekly' | 'monthly';
  dayOfWeek?: number; // 0-6 for weekly
  dayOfMonth?: number; // 1-31 for monthly
  time: string; // HH:mm format
  recipients: string[];
  format: ExportFormat;
}

/**
 * Complete custom report definition
 */
export interface CustomReport {
  id: string;
  name: string;
  description: string;
  createdAt: Date;
  updatedAt: Date;
  createdBy: string;
  isShared: boolean;
  sections: ReportSection[];
  dateRange: DateRangeConfig;
  filters: PatientFilterConfig;
  schedule?: ScheduleConfig;
  tags: string[];
}

/**
 * Generated report result
 */
export interface GeneratedReport {
  id: string;
  reportId: string;
  reportName: string;
  generatedAt: Date;
  dateRange: { start: Date; end: Date };
  sections: GeneratedReportSection[];
  summary: ReportSummary;
}

export interface GeneratedReportSection {
  type: ReportSectionType;
  title: string;
  data: unknown;
  charts?: ChartData[];
}

export interface ChartData {
  type: 'bar' | 'line' | 'pie' | 'radar';
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string[];
  }[];
}

export interface ReportSummary {
  totalPatients: number;
  averageCompliance: number;
  openGaps: number;
  closedGaps: number;
  improvementFromPrevious: number;
  highlights: string[];
}

/**
 * Available section templates
 */
const SECTION_TEMPLATES: Omit<ReportSection, 'id' | 'order'>[] = [
  {
    type: 'executive-summary',
    title: 'Executive Summary',
    description: 'Auto-generated overview with key metrics, highlights, and recommendations',
    icon: 'summarize',
    enabled: true,
    configuration: {},
  },
  {
    type: 'quality-measures',
    title: 'Quality Measure Performance',
    description: 'Compliance rates, trends, and breakdowns for selected quality measures',
    icon: 'analytics',
    enabled: true,
    configuration: {
      showCompliance: true,
      showTrends: true,
      chartType: 'bar',
    },
  },
  {
    type: 'care-gaps',
    title: 'Care Gap Analysis',
    description: 'Open care gaps by type, urgency, and patient attribution',
    icon: 'healing',
    enabled: true,
    configuration: {
      showUrgency: true,
      groupByPatient: false,
    },
  },
  {
    type: 'patient-risk',
    title: 'Patient Risk Stratification',
    description: 'Risk level distribution and intervention recommendations',
    icon: 'warning',
    enabled: false,
    configuration: {
      riskLevels: ['high', 'medium', 'low'],
      showInterventions: true,
    },
  },
  {
    type: 'trend-analysis',
    title: 'Trend Analysis',
    description: 'Historical performance trends and future projections',
    icon: 'trending_up',
    enabled: false,
    configuration: {
      trendPeriod: 'monthly',
      showProjections: false,
    },
  },
  {
    type: 'provider-comparison',
    title: 'Provider Comparison',
    description: 'Side-by-side comparison of provider performance metrics',
    icon: 'compare',
    enabled: false,
    configuration: {
      showBenchmarks: true,
    },
  },
  {
    type: 'patient-demographics',
    title: 'Patient Demographics',
    description: 'Age distribution, condition breakdown, and population insights',
    icon: 'groups',
    enabled: false,
    configuration: {
      showAgeDistribution: true,
      showConditionBreakdown: true,
    },
  },
  {
    type: 'custom-metrics',
    title: 'Custom Metrics',
    description: 'User-defined metrics and calculations',
    icon: 'calculate',
    enabled: false,
    configuration: {
      metrics: [],
    },
  },
];

/**
 * Report Builder Service
 *
 * Manages custom report creation, configuration, and generation.
 *
 * Features:
 * - Drag-and-drop section management
 * - Custom report template saving
 * - Report scheduling
 * - Export to multiple formats
 * - Report history and trend analysis
 *
 * Issue #158: Add Custom Report Builder
 */
@Injectable({
  providedIn: 'root',
})
export class ReportBuilderService {
  private readonly logger: any;
  private readonly REPORTS_STORAGE_KEY = 'hdim_custom_reports';
  private readonly HISTORY_STORAGE_KEY = 'hdim_report_history';

  private reportsSubject = new BehaviorSubject<CustomReport[]>([]);
  readonly reports$ = this.reportsSubject.asObservable();

  private historySubject = new BehaviorSubject<GeneratedReport[]>([]);
  readonly history$ = this.historySubject.asObservable();

  constructor(
    private loggerService: LoggerService,) {
    this.logger = this.loggerService.withContext(\'ReportBuilderService');
    this.loadFromStorage();
  }

  /**
   * Get available section templates for building reports
   */
  getSectionTemplates(): Omit<ReportSection, 'id' | 'order'>[] {
    return [...SECTION_TEMPLATES];
  }

  /**
   * Create a new custom report with default sections
   */
  createReport(name: string, description = ''): CustomReport {
    const defaultSections = SECTION_TEMPLATES.filter(s =>
      ['executive-summary', 'quality-measures', 'care-gaps'].includes(s.type)
    ).map((section, index) => ({
      ...section,
      id: this.generateId(),
      order: index,
    }));

    const report: CustomReport = {
      id: this.generateId(),
      name,
      description,
      createdAt: new Date(),
      updatedAt: new Date(),
      createdBy: 'current-user', // Would get from auth service
      isShared: false,
      sections: defaultSections,
      dateRange: {
        type: 'ytd',
        comparisonPeriod: 'same-period-last-year',
      },
      filters: {},
      tags: [],
    };

    const reports = this.reportsSubject.value;
    reports.push(report);
    this.reportsSubject.next(reports);
    this.saveToStorage();

    return report;
  }

  /**
   * Get a report by ID
   */
  getReport(id: string): CustomReport | undefined {
    return this.reportsSubject.value.find(r => r.id === id);
  }

  /**
   * Update an existing report
   */
  updateReport(report: CustomReport): void {
    const reports = this.reportsSubject.value;
    const index = reports.findIndex(r => r.id === report.id);

    if (index >= 0) {
      report.updatedAt = new Date();
      reports[index] = report;
      this.reportsSubject.next(reports);
      this.saveToStorage();
    }
  }

  /**
   * Delete a report
   */
  deleteReport(id: string): void {
    const reports = this.reportsSubject.value.filter(r => r.id !== id);
    this.reportsSubject.next(reports);
    this.saveToStorage();
  }

  /**
   * Duplicate a report
   */
  duplicateReport(id: string): CustomReport | undefined {
    const original = this.getReport(id);
    if (!original) return undefined;

    const duplicate: CustomReport = {
      ...original,
      id: this.generateId(),
      name: `${original.name} (Copy)`,
      createdAt: new Date(),
      updatedAt: new Date(),
      isShared: false,
      sections: original.sections.map(s => ({
        ...s,
        id: this.generateId(),
      })),
    };

    const reports = this.reportsSubject.value;
    reports.push(duplicate);
    this.reportsSubject.next(reports);
    this.saveToStorage();

    return duplicate;
  }

  /**
   * Add a section to a report
   */
  addSection(reportId: string, sectionType: ReportSectionType): void {
    const report = this.getReport(reportId);
    if (!report) return;

    const template = SECTION_TEMPLATES.find(s => s.type === sectionType);
    if (!template) return;

    const newSection: ReportSection = {
      ...template,
      id: this.generateId(),
      order: report.sections.length,
      enabled: true,
    };

    report.sections.push(newSection);
    this.updateReport(report);
  }

  /**
   * Remove a section from a report
   */
  removeSection(reportId: string, sectionId: string): void {
    const report = this.getReport(reportId);
    if (!report) return;

    report.sections = report.sections.filter(s => s.id !== sectionId);
    // Reorder remaining sections
    report.sections.forEach((s, i) => (s.order = i));
    this.updateReport(report);
  }

  /**
   * Reorder sections (for drag-and-drop)
   */
  reorderSections(reportId: string, sectionIds: string[]): void {
    const report = this.getReport(reportId);
    if (!report) return;

    const sectionMap = new Map(report.sections.map(s => [s.id, s]));
    report.sections = sectionIds
      .map((id, index) => {
        const section = sectionMap.get(id);
        if (section) section.order = index;
        return section;
      })
      .filter((s): s is ReportSection => !!s);

    this.updateReport(report);
  }

  /**
   * Update a section's configuration
   */
  updateSectionConfig(
    reportId: string,
    sectionId: string,
    config: Partial<ReportSectionConfig>
  ): void {
    const report = this.getReport(reportId);
    if (!report) return;

    const section = report.sections.find(s => s.id === sectionId);
    if (section) {
      section.configuration = { ...section.configuration, ...config };
      this.updateReport(report);
    }
  }

  /**
   * Toggle section enabled state
   */
  toggleSection(reportId: string, sectionId: string): void {
    const report = this.getReport(reportId);
    if (!report) return;

    const section = report.sections.find(s => s.id === sectionId);
    if (section) {
      section.enabled = !section.enabled;
      this.updateReport(report);
    }
  }

  /**
   * Update report date range
   */
  updateDateRange(reportId: string, dateRange: DateRangeConfig): void {
    const report = this.getReport(reportId);
    if (!report) return;

    report.dateRange = dateRange;
    this.updateReport(report);
  }

  /**
   * Update report filters
   */
  updateFilters(reportId: string, filters: PatientFilterConfig): void {
    const report = this.getReport(reportId);
    if (!report) return;

    report.filters = filters;
    this.updateReport(report);
  }

  /**
   * Update report schedule
   */
  updateSchedule(reportId: string, schedule: ScheduleConfig | undefined): void {
    const report = this.getReport(reportId);
    if (!report) return;

    report.schedule = schedule;
    this.updateReport(report);
  }

  /**
   * Share or unshare a report
   */
  toggleShared(reportId: string): void {
    const report = this.getReport(reportId);
    if (!report) return;

    report.isShared = !report.isShared;
    this.updateReport(report);
  }

  /**
   * Generate a report (simulated)
   */
  generateReport(reportId: string): Observable<GeneratedReport> {
    const report = this.getReport(reportId);
    if (!report) {
      return of(null as unknown as GeneratedReport);
    }

    // Simulate report generation
    return of(this.createMockGeneratedReport(report)).pipe(
      delay(1500), // Simulate processing time
      map(generated => {
        // Add to history
        const history = this.historySubject.value;
        history.unshift(generated);
        // Keep only last 50 reports
        if (history.length > 50) history.pop();
        this.historySubject.next(history);
        this.saveHistoryToStorage();
        return generated;
      })
    );
  }

  /**
   * Get report generation history
   */
  getReportHistory(reportId?: string): GeneratedReport[] {
    const history = this.historySubject.value;
    if (reportId) {
      return history.filter(h => h.reportId === reportId);
    }
    return history;
  }

  /**
   * Export report to specified format
   */
  exportReport(generatedReport: GeneratedReport, format: ExportFormat): Observable<Blob> {
    // In real implementation, would call backend service
    return of(new Blob(['Report data'], { type: this.getMimeType(format) })).pipe(
      delay(500)
    );
  }

  /**
   * Get shared reports (for team access)
   */
  getSharedReports(): Observable<CustomReport[]> {
    return this.reports$.pipe(map(reports => reports.filter(r => r.isShared)));
  }

  // Private helpers

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private loadFromStorage(): void {
    try {
      const reportsJson = localStorage.getItem(this.REPORTS_STORAGE_KEY);
      if (reportsJson) {
        const reports = JSON.parse(reportsJson);
        this.reportsSubject.next(reports);
      }

      const historyJson = localStorage.getItem(this.HISTORY_STORAGE_KEY);
      if (historyJson) {
        const history = JSON.parse(historyJson);
        this.historySubject.next(history);
      }
    } catch (e) {
      this.logger.error('Error loading reports from storage:', e);
    }
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(
        this.REPORTS_STORAGE_KEY,
        JSON.stringify(this.reportsSubject.value)
      );
    } catch (e) {
      this.logger.error('Error saving reports to storage:', e);
    }
  }

  private saveHistoryToStorage(): void {
    try {
      localStorage.setItem(
        this.HISTORY_STORAGE_KEY,
        JSON.stringify(this.historySubject.value)
      );
    } catch (e) {
      this.logger.error('Error saving history to storage:', e);
    }
  }

  private getMimeType(format: ExportFormat): string {
    switch (format) {
      case 'pdf':
        return 'application/pdf';
      case 'excel':
        return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      case 'csv':
        return 'text/csv';
      case 'png':
        return 'image/png';
      default:
        return 'application/octet-stream';
    }
  }

  private createMockGeneratedReport(report: CustomReport): GeneratedReport {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);

    return {
      id: this.generateId(),
      reportId: report.id,
      reportName: report.name,
      generatedAt: now,
      dateRange: {
        start: report.dateRange.startDate || startOfYear,
        end: report.dateRange.endDate || now,
      },
      sections: report.sections
        .filter(s => s.enabled)
        .map(s => this.generateSectionData(s)),
      summary: {
        totalPatients: 1247,
        averageCompliance: 78.3,
        openGaps: 342,
        closedGaps: 89,
        improvementFromPrevious: 5.2,
        highlights: [
          'Colorectal screening compliance improved by 8.2% this quarter',
          'Diabetes HbA1c control at 82.1%, above target',
          '45 high-priority care gaps closed this month',
          'Breast cancer screening approaching 90% compliance',
        ],
      },
    };
  }

  private generateSectionData(section: ReportSection): GeneratedReportSection {
    return {
      type: section.type,
      title: section.title,
      data: this.getMockDataForSection(section.type),
      charts: section.configuration.chartType
        ? [this.getMockChartData(section.type, section.configuration.chartType)]
        : undefined,
    };
  }

  private getMockDataForSection(type: ReportSectionType): unknown {
    switch (type) {
      case 'executive-summary':
        return {
          overallCompliance: 78.3,
          trend: 'improving',
          topMeasures: ['BCS', 'CDC-HBA1C', 'COL'],
          recommendations: [
            'Focus on colorectal screening outreach',
            'Review diabetic eye exam scheduling',
          ],
        };
      case 'quality-measures':
        return [
          { measure: 'BCS', compliance: 89.2, target: 85, trend: 2.1 },
          { measure: 'CDC-HBA1C', compliance: 82.1, target: 80, trend: 3.5 },
          { measure: 'COL', compliance: 71.5, target: 75, trend: 8.2 },
          { measure: 'CCS', compliance: 68.3, target: 70, trend: 1.2 },
        ];
      case 'care-gaps':
        return {
          total: 342,
          byType: {
            preventive: 156,
            chronic: 112,
            medication: 74,
          },
          byUrgency: {
            high: 89,
            medium: 167,
            low: 86,
          },
        };
      case 'patient-risk':
        return {
          distribution: { high: 234, medium: 567, low: 446 },
          trends: [
            { month: 'Jan', high: 245, medium: 560, low: 440 },
            { month: 'Feb', high: 240, medium: 562, low: 443 },
            { month: 'Mar', high: 234, medium: 567, low: 446 },
          ],
        };
      default:
        return {};
    }
  }

  private getMockChartData(
    type: ReportSectionType,
    chartType: 'bar' | 'line' | 'pie' | 'radar'
  ): ChartData {
    return {
      type: chartType,
      labels: ['BCS', 'CDC-HBA1C', 'COL', 'CCS', 'CBP'],
      datasets: [
        {
          label: 'Current',
          data: [89.2, 82.1, 71.5, 68.3, 76.8],
          backgroundColor: ['#4caf50', '#4caf50', '#ff9800', '#ff9800', '#4caf50'],
        },
        {
          label: 'Previous',
          data: [87.1, 78.6, 63.3, 67.1, 74.2],
          backgroundColor: ['#9e9e9e', '#9e9e9e', '#9e9e9e', '#9e9e9e', '#9e9e9e'],
        },
      ],
    };
  }
}
