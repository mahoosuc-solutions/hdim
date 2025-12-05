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
