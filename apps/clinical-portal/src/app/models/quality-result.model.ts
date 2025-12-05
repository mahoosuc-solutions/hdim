/**
 * Quality Measure Result DTO - Matches backend QualityMeasureResultDTO
 */
export interface QualityMeasureResult {
  id: string; // UUID
  tenantId: string;
  patientId: string; // UUID
  measureId: string; // e.g., "CDC-A1C9"
  measureName: string;
  measureCategory: MeasureCategory;
  measureYear: number;
  numeratorCompliant: boolean; // Patient meets criteria
  denominatorEligible: boolean; // Patient is eligible
  complianceRate: number; // 0.0 to 100.0
  score: number;
  calculationDate: string; // ISO date string
  cqlLibrary?: string;
  createdAt: string; // ISO date string
  createdBy: string;
  version: number;
}

export type MeasureCategory = 'HEDIS' | 'CMS' | 'CUSTOM';

/**
 * Quality Score Summary
 */
export interface QualityScore {
  totalMeasures: number;
  compliantMeasures: number;
  scorePercentage: number; // 0.0 to 100.0
}

/**
 * Patient Quality Report
 */
export interface QualityReport {
  patientId: string;
  patientName?: string;
  reportDate: string;
  overallScore: QualityScore;
  measureResults: QualityMeasureResult[];
  gapsInCare: GapInCare[];
}

/**
 * Gap in Care (measure not met)
 */
export interface GapInCare {
  measureId: string;
  measureName: string;
  description: string;
  recommendation: string;
}

/**
 * Population Quality Report
 */
export interface PopulationQualityReport {
  year: number;
  totalPatients: number;
  reportDate: string;
  overallCompliance: number; // Percentage
  measureSummaries: MeasureSummary[];
}

/**
 * Measure Summary for population report
 */
export interface MeasureSummary {
  measureId: string;
  measureName: string;
  totalEligible: number;
  totalCompliant: number;
  complianceRate: number; // Percentage
}

/**
 * Calculate Measure Request
 */
export interface CalculateMeasureRequest {
  patient: string; // Patient ID
  measure: string; // Measure ID or library name
  createdBy?: string; // Defaults to "system"
}

/**
 * Report Type
 */
export type ReportType = 'PATIENT' | 'POPULATION' | 'CARE_GAP';

/**
 * Report Status
 */
export type ReportStatus = 'GENERATING' | 'COMPLETED' | 'FAILED';

/**
 * Saved Report Entity - Matches backend SavedReportEntity
 */
export interface SavedReport {
  id: string; // UUID
  tenantId: string;
  reportType: ReportType;
  reportName: string;
  patientId?: string; // UUID (for PATIENT reports)
  year?: number; // For POPULATION reports
  reportData: string; // JSON string (QualityReport or PopulationQualityReport)
  createdBy: string;
  createdAt: string; // ISO date string
  status: ReportStatus;
}

/**
 * Save Patient Report Request
 */
export interface SavePatientReportRequest {
  patient: string; // Patient ID (UUID)
  name: string; // Report name
  createdBy?: string; // Defaults to "system"
}

/**
 * Save Population Report Request
 */
export interface SavePopulationReportRequest {
  year: number;
  name: string; // Report name
  createdBy?: string; // Defaults to "system"
}

/**
 * Export Format
 */
export type ExportFormat = 'csv' | 'excel';
