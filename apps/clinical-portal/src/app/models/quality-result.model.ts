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
 * Local Measure Result - from quality-measure-service /calculate-local endpoint
 * This is the result from local Java measure calculators (no CQL Engine)
 */
export interface LocalMeasureResult {
  measureId: string;
  measureName: string;
  patientId: string;
  eligible: boolean; // Backend returns 'eligible'
  isEligible?: boolean; // Alias for 'eligible' (deprecated)
  denominatorMembership: boolean;
  denominatorExclusion: boolean;
  exclusionReason?: string;
  subMeasures: Record<string, SubMeasureResult>;
  careGaps: LocalCareGap[];
  recommendations: LocalRecommendation[];
  calculatedAt: string;
}

/**
 * Sub-measure result for local calculation
 * Matches backend SubMeasureResult model
 */
export interface SubMeasureResult {
  numeratorMembership: boolean; // Backend field name (is patient in numerator?)
  value?: string;               // Measured value (e.g., "7.5%" for HbA1c)
  date?: string;                // Date when measurement was taken
  method?: string;              // Method of measurement (e.g., "procedure", "observation")
  numericValue?: number;        // Numeric value for calculations
  metadata?: string;            // Additional metadata
  // Note: The sub-measure name is the key in the subMeasures Record, not a field here
}

/**
 * Care gap from local calculation
 * Matches backend CareGap model
 */
export interface LocalCareGap {
  type: string;           // e.g., "missing-hba1c-test"
  description: string;
  severity: string;       // "critical", "high", "medium", "low"
  action?: string;        // Recommended action
  measureComponent?: string; // e.g., "HbA1c Testing"
  // Legacy field aliases for backward compatibility
  gapId?: string;
  priority?: string;
  intervention?: string;
}

/**
 * Recommendation from local calculation
 * Matches backend Recommendation model
 */
export interface LocalRecommendation {
  priority: string;   // "high", "medium", "low" (lowercase from backend)
  action: string;     // Recommended action
  rationale?: string; // Clinical rationale
  category?: string;  // "medication", "referral", "visit", "lifestyle", "screening"
  // Legacy field aliases for backward compatibility
  id?: string;
  title?: string;
  description?: string;
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
