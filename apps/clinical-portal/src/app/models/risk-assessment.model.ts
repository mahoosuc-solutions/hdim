/**
 * Risk Assessment Models
 *
 * Models for risk stratification including risk factors, predicted outcomes,
 * and population statistics.
 */

/**
 * Risk Factor
 * Represents a factor contributing to patient risk
 */
export interface RiskFactor {
  /**
   * Name of the risk factor (e.g., "Hypertension", "Smoking")
   */
  factor: string;

  /**
   * Risk category (e.g., "CARDIOVASCULAR", "DIABETES")
   */
  category: string;

  /**
   * Numerical weight/contribution to overall risk (0-100)
   */
  weight: number;

  /**
   * Severity level (e.g., "mild", "moderate", "severe")
   */
  severity: string;

  /**
   * Clinical evidence supporting this risk factor
   */
  evidence: string;
}

/**
 * Predicted Outcome
 * Represents a predicted health outcome based on risk assessment
 */
export interface PredictedOutcome {
  /**
   * Description of the predicted outcome
   */
  outcome: string;

  /**
   * Probability of occurrence (0.0 - 1.0)
   */
  probability: number;

  /**
   * Timeframe for the prediction (e.g., "5 years", "10 years")
   */
  timeframe: string;
}

/**
 * Risk Assessment
 * Complete risk assessment for a patient
 */
export interface RiskAssessment {
  /**
   * Unique identifier for this assessment
   */
  id: string;

  /**
   * Patient FHIR ID
   */
  patientId: string;

  /**
   * Risk category (CARDIOVASCULAR, DIABETES, RESPIRATORY, MENTAL_HEALTH)
   */
  riskCategory: string;

  /**
   * Overall risk score (0-100)
   */
  riskScore: number;

  /**
   * Risk level classification (low, moderate, high, very-high)
   */
  riskLevel: string;

  /**
   * Array of risk factors contributing to this assessment
   */
  riskFactors: RiskFactor[];

  /**
   * Array of predicted outcomes
   */
  predictedOutcomes: PredictedOutcome[];

  /**
   * Recommended interventions based on risk assessment
   */
  recommendations: string[];

  /**
   * Date when the assessment was calculated
   */
  assessmentDate: Date;

  /**
   * Date when the record was created
   */
  createdAt: Date;
}

/**
 * Population Stats
 * Population-level risk statistics
 */
export interface PopulationStats {
  /**
   * Total number of patients in the population
   */
  totalPatients: number;

  /**
   * Distribution of patients across risk levels
   */
  riskLevelDistribution: {
    low: number;
    moderate: number;
    high: number;
    'very-high': number;
  };
}

/**
 * Risk Level Type
 * Union type for risk levels
 */
export type RiskLevel = 'low' | 'moderate' | 'high' | 'very-high';

/**
 * Risk Category Type
 * Union type for risk categories
 */
export type RiskCategory =
  | 'CARDIOVASCULAR'
  | 'DIABETES'
  | 'RESPIRATORY'
  | 'MENTAL_HEALTH'
  | 'RENAL'
  | 'ONCOLOGY';

/**
 * HCC-based Risk Assessment Response
 * Matches the backend PatientRiskAssessmentResponse from patient-service
 *
 * This provides comprehensive risk assessment combining:
 * - CMS-HCC RAF scores (V24, V28, and blended)
 * - Risk level classification
 * - Care gap integration
 * - Documentation gap opportunities
 */
export interface HccRiskAssessment {
  /**
   * Patient identifier (UUID)
   */
  patientId: string;

  /**
   * Blended RAF score (weighted combination of V24 and V28)
   */
  rafScoreBlended: number | null;

  /**
   * RAF score under CMS-HCC V24 model
   */
  rafScoreV24: number | null;

  /**
   * RAF score under CMS-HCC V28 model
   */
  rafScoreV28: number | null;

  /**
   * Risk level classification
   */
  riskLevel: 'LOW' | 'MODERATE' | 'HIGH' | 'VERY_HIGH';

  /**
   * Numerical risk score (0-100) for UI display
   */
  riskScore: number;

  /**
   * Total count of captured HCC codes
   */
  hccCount: number;

  /**
   * Top HCC codes by RAF impact (up to 5)
   */
  topHccs: string[];

  /**
   * Chronic conditions derived from HCC codes
   */
  chronicConditions: string[];

  /**
   * Count of open care gaps
   */
  openCareGaps: number;

  /**
   * Count of high-priority care gaps
   */
  highPriorityCareGaps: number;

  /**
   * Count of overdue care gaps
   */
  overdueCareGaps: number;

  /**
   * Potential RAF uplift if documentation gaps are addressed
   */
  potentialRafUplift: number | null;

  /**
   * Count of documentation gaps that could improve RAF score
   */
  documentationGapCount: number | null;

  /**
   * Count of prior-year HCCs needing recapture
   */
  recaptureOpportunities: number | null;

  /**
   * Timestamp when risk assessment was calculated
   */
  calculatedAt: string;

  /**
   * Profile year for HCC calculations
   */
  profileYear: number;

  /**
   * Data availability indicators
   */
  dataAvailability: {
    hccDataAvailable: boolean;
    careGapDataAvailable: boolean;
    documentationGapDataAvailable: boolean;
  };
}
