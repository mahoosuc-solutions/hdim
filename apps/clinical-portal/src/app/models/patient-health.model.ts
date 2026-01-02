/**
 * Patient Health Overview Models
 *
 * Comprehensive models for tracking patient physical, mental, and social health
 */

import { CodeableConcept } from './patient.model';

/**
 * Risk Stratification Levels
 */
export type RiskLevel = 'low' | 'moderate' | 'high' | 'critical';

/**
 * Health Status Categories
 */
export type HealthStatus = 'excellent' | 'good' | 'fair' | 'poor' | 'unknown';

/**
 * Mental Health Assessment Types
 */
export type MentalHealthAssessmentType =
  | 'PHQ-9'      // Depression
  | 'GAD-7'      // Anxiety
  | 'PHQ-2'      // Brief depression screening
  | 'PSC-17'     // Pediatric symptom checklist
  | 'AUDIT-C'    // Alcohol use
  | 'DAST-10'    // Drug abuse
  | 'PCL-5'      // PTSD
  | 'MDQ'        // Bipolar
  | 'CAGE-AID';  // Substance abuse

/**
 * Social Determinants of Health Categories
 */
export type SDOHCategory =
  | 'food-insecurity'
  | 'housing-instability'
  | 'transportation'
  | 'utility-assistance'
  | 'interpersonal-safety'
  | 'education'
  | 'employment'
  | 'social-isolation'
  | 'financial-strain'
  | 'food'
  | 'housing'
  | 'financial'
  | 'social'
  | 'safety';

/**
 * SDOH Questionnaire Types
 */
export type SDOHQuestionnaireType = 'PRAPARE' | 'AHC-HRSN' | 'custom';

/**
 * SDOH Severity Levels
 */
export type SDOHSeverity = 'none' | 'mild' | 'moderate' | 'severe';

/**
 * SDOH Screening Result from FHIR QuestionnaireResponse
 */
export interface SDOHScreeningResult {
  screeningDate: Date;
  questionnaireType: SDOHQuestionnaireType;
  questionnaireName?: string;  // Feature 3.4: Name of questionnaire (e.g., "PRAPARE", "AHC-HRSN")
  needs: SDOHNeedWithDetails[];
  overallRisk: 'low' | 'moderate' | 'high';
  zCodes?: string[];  // Feature 3.4: ICD-10 Z-codes extracted from needs
}

/**
 * SDOH Need with detailed context (from QuestionnaireResponse)
 */
export interface SDOHNeedWithDetails {
  category: SDOHCategory;
  severity: SDOHSeverity;
  zCode: string;
  questionText: string;
  response: string;
}

/**
 * Comprehensive Patient Health Overview
 */
export interface PatientHealthOverview {
  patientId: string;
  lastUpdated: Date;

  // Overall Health Score
  overallHealthScore: HealthScore;

  // Physical Health
  physicalHealth: PhysicalHealthSummary;

  // Mental Health
  mentalHealth: MentalHealthSummary;

  // Social Determinants of Health
  socialDeterminants: SDOHSummary;

  // Risk Stratification
  riskStratification: RiskStratification;

  // Care Gaps & Recommendations
  careGaps: CareGap[];
  recommendations: CareRecommendation[]; // Feature 5.2 format

  // Quality Measures Performance
  qualityMeasures: QualityMeasurePerformance;
}

/**
 * Overall Health Score (0-100)
 */
export interface HealthScore {
  patientId: string;
  overallScore: number; // 0-100 (renamed from 'score' for backend compatibility)
  score?: number; // 0-100 (deprecated, use overallScore)
  status: HealthStatus;
  trend: 'improving' | 'stable' | 'declining' | 'unknown';
  components: {
    physical: number;      // 30% weight
    mental: number;        // 25% weight
    social: number;        // 15% weight
    preventive: number;    // 15% weight
    chronicDisease: number; // 15% weight
  };
  calculatedAt: Date;
  lastCalculated?: Date; // deprecated, use calculatedAt
}

/**
 * Health Score History entry from backend
 */
export interface HealthScoreHistory {
  score: number;
  calculatedAt: Date;
  trigger: string;
}

/**
 * Health Score History Point (for trending)
 */
export interface HealthScoreHistoryPoint {
  date: Date;
  score: number;
  status: HealthStatus;
  components: {
    physical: number;
    mental: number;
    social: number;
    preventive: number;
  };
}

/**
 * Health Score Trend Analysis
 */
export interface HealthScoreTrend {
  direction: 'improving' | 'stable' | 'declining';
  percentChange: number;
  pointsChange: number;
}

/**
 * Physical Health Summary
 */
export interface PhysicalHealthSummary {
  status: HealthStatus;

  // Vital Signs (most recent)
  vitals: {
    bloodPressure?: VitalSign<string>;  // e.g., "120/80"
    heartRate?: VitalSign<number>;
    temperature?: VitalSign<number>;
    weight?: VitalSign<number>;
    height?: VitalSign<number>;
    bmi?: VitalSign<number>;
    oxygenSaturation?: VitalSign<number>;
    respiratoryRate?: VitalSign<number>;
  };

  // Lab Results (recent significant values)
  labs: LabResult[];

  // Active Chronic Conditions
  chronicConditions: ChronicCondition[];

  // Medication Adherence
  medicationAdherence: {
    overallRate: number; // 0-100
    status: 'excellent' | 'good' | 'poor' | 'unknown';
    problematicMedications: string[];
    totalMedications: number; // Total active medications count
  };

  // Functional Status
  functionalStatus: FunctionalStatus;
}

/**
 * Vital Sign with Trend
 */
export interface VitalSign<T> {
  name?: string;
  value: T;
  unit: string;
  date: Date;
  status: 'normal' | 'abnormal' | 'critical';
  trend?: 'improving' | 'stable' | 'declining';
  referenceRange?: {
    low: number;
    high: number;
  };
}

/**
 * Lab Result
 */
export interface LabResult {
  id?: string;
  name?: string;
  code: string;
  loincCode?: string;
  value: number | string;
  unit?: string;
  date: Date;
  status: 'normal' | 'abnormal' | 'critical';
  referenceRange?: {
    low?: number;
    high?: number;
    text?: string;
  };
  interpretation?: LabInterpretation;
  trend?: 'improving' | 'stable' | 'worsening';
}

/**
 * Lab Interpretation
 */
export interface LabInterpretation {
  code: string;
  display: string;
  severity: string;
  description?: string;
}

/**
 * Lab Panel (e.g., CBC, BMP, Lipid Panel)
 */
export interface LabPanel {
  panelCode: string;
  panelName: string;
  date: Date;
  status: 'normal' | 'abnormal' | 'critical';
  results: LabResult[];
}

/**
 * Lab Trend Analysis
 */
export interface LabTrendAnalysis {
  loincCode: string;
  testName: string;
  trend: 'improving' | 'stable' | 'worsening';
  percentChange: number;
  dataPoints: LabResult[];
  recommendation?: string;
}

/**
 * Chronic Condition
 */
export interface ChronicCondition {
  id?: string;
  name?: string;
  code: string;
  system?: string;
  display?: string;
  severity: 'mild' | 'moderate' | 'severe';
  onsetDate?: Date;
  status?: 'active' | 'inactive' | 'resolved' | 'remission' | 'recurrence' | 'relapse';
  controlled?: boolean;
  isControlled?: boolean;
  lastReview?: Date;
  lastAssessment?: Date;
  riskLevel?: RiskLevel;
  complications?: string[];
  clinicalStatus?: string;
}

/**
 * Functional Status
 */
export interface FunctionalStatus {
  adlScore: number;     // Activities of Daily Living (0-6)
  iadlScore: number;    // Instrumental ADL (0-8)
  mobilityScore: number; // 0-100
  painLevel: number;    // 0-10
  fatigueLevel: number; // 0-10
}

/**
 * Mental Health Summary
 */
export interface MentalHealthSummary {
  status: HealthStatus;
  riskLevel: RiskLevel;

  // Screening Assessments
  assessments: MentalHealthAssessment[];

  // Feature 3.3: Specific scores for quick access
  depressionScore?: {
    score: number;
    severity: string;
    date: Date;
  };
  anxietyScore?: {
    score: number;
    severity: string;
    date: Date;
  };

  // Feature 3.3: Mental health conditions (filtered from FHIR)
  conditions?: MentalHealthCondition[];

  // Feature 3.3: Psychiatric medications (from MedicationStatement)
  medications?: Medication[];

  // Feature 3.3: Assessment history for trending
  assessmentHistory?: Assessment[];

  // Diagnosed Mental Health Conditions
  diagnoses: MentalHealthDiagnosis[];

  // Substance Use
  substanceUse: SubstanceUseAssessment;

  // Suicide Risk
  suicideRisk: RiskAssessment;

  // Social Support
  socialSupport: {
    level: 'strong' | 'moderate' | 'weak' | 'unknown';
    hasCaregiver: boolean;
    livesAlone: boolean;
    socialIsolation: boolean;
  };

  // Treatment Engagement
  treatmentEngagement: {
    inTherapy: boolean;
    therapyAdherence?: number; // 0-100
    medicationCompliance?: number; // 0-100
    lastPsychVisit?: Date;
  };
}

/**
 * Mental Health Assessment Result
 */
export interface MentalHealthAssessment {
  type: MentalHealthAssessmentType;
  name: string;
  score: number;
  maxScore: number;
  severity: 'none' | 'minimal' | 'mild' | 'moderate' | 'moderately-severe' | 'severe';
  date: Date;
  trend?: 'improving' | 'stable' | 'declining';
  interpretation: string;

  // Screening-specific details
  positiveScreen: boolean;
  thresholdScore: number;
  requiresFollowup: boolean;
}

/**
 * Mental Health Diagnosis
 */
export interface MentalHealthDiagnosis {
  code: CodeableConcept;
  display: string;
  category: 'mood' | 'anxiety' | 'psychotic' | 'substance' | 'trauma' | 'other';
  severity: 'mild' | 'moderate' | 'severe';
  onsetDate?: Date;
  inRemission: boolean;
  lastReview?: Date;
}

/**
 * Substance Use Assessment
 */
export interface SubstanceUseAssessment {
  hasSubstanceUse: boolean;
  substances: SubstanceUse[];
  overallRisk: RiskLevel;
}

/**
 * Substance Use
 */
export interface SubstanceUse {
  substance: 'alcohol' | 'tobacco' | 'cannabis' | 'opioids' | 'stimulants' | 'other';
  frequency: 'daily' | 'weekly' | 'monthly' | 'occasional' | 'former';
  severity: 'mild' | 'moderate' | 'severe';
  inTreatment: boolean;
}

/**
 * Risk Assessment (e.g., Suicide, Violence)
 */
export interface RiskAssessment {
  level: RiskLevel;
  factors: RiskFactor[];
  protectiveFactors: string[];
  lastAssessed?: Date;
  requiresIntervention: boolean;
}

/**
 * Risk Factor
 */
export interface RiskFactor {
  factor: string;
  severity: 'low' | 'moderate' | 'high';
  modifiable: boolean;
}

/**
 * Social Determinants of Health Summary
 */
export interface SDOHSummary {
  overallRisk: RiskLevel;
  screeningDate?: Date;

  // SDOH Needs
  needs: SDOHNeed[];

  // Referrals & Resources
  activeReferrals: SDOHReferral[];

  // Z-codes (ICD-10 social determinant codes)
  zCodes: string[];
}

/**
 * SDOH Need
 */
export interface SDOHNeed {
  category: SDOHCategory;
  description: string;
  severity: SDOHSeverity;
  identified: Date;
  addressed: boolean;
  interventions?: string[];
}

/**
 * SDOH Referral
 */
export interface SDOHReferral {
  organization: string;
  category: SDOHCategory;
  status: 'pending' | 'active' | 'completed' | 'cancelled';
  referralDate: Date;
  followupDate?: Date;
}

/**
 * Risk Stratification
 */
export interface RiskStratification {
  overallRisk: RiskLevel;

  // Risk Scores
  scores: {
    clinicalComplexity: number;    // 0-100
    socialComplexity: number;      // 0-100
    mentalHealthRisk: number;      // 0-100
    utilizationRisk: number;       // 0-100
    costRisk: number;              // 0-100
  };

  // Predictive Analytics
  predictions: {
    hospitalizationRisk30Day: number;   // 0-100
    hospitalizationRisk90Day: number;   // 0-100
    edVisitRisk30Day: number;           // 0-100
    readmissionRisk: number;            // 0-100
  };

  // Risk Categories
  categories: {
    diabetes: RiskLevel;
    cardiovascular: RiskLevel;
    respiratory: RiskLevel;
    mentalHealth: RiskLevel;
    fallRisk: RiskLevel;
  };
}

/**
 * Care Gap
 */
export interface CareGap {
  id: string;
  category: 'preventive' | 'chronic-disease' | 'mental-health' | 'medication' | 'screening';
  title: string;
  description: string;
  priority: 'low' | 'medium' | 'high' | 'urgent';
  dueDate?: Date;
  overdueDays?: number;

  // Quality Measure Association
  measureId?: string;
  measureName?: string;

  // Actions to Close Gap
  recommendedActions: string[];

  // Barriers
  barriers?: string[];
}

/**
 * Care Recommendation Summary (Legacy)
 * @deprecated Use CareRecommendation from Feature 5.2 instead
 */
export interface CareRecommendationSummary {
  id: string;
  category: 'preventive' | 'treatment' | 'referral' | 'education' | 'lifestyle';
  title: string;
  description: string;
  priority: 'low' | 'medium' | 'high';

  // Evidence & Guidelines
  evidence: string;
  guidelineSource?: string;

  // Patient-Specific Rationale
  rationale: string;

  // Expected Benefit
  expectedBenefit?: string;

  // Resources
  resources?: CareResource[];
}

/**
 * Care Resource
 */
export interface CareResource {
  type: 'article' | 'video' | 'tool' | 'referral' | 'class';
  title: string;
  url?: string;
  description?: string;
}

/**
 * Quality Measure Performance Summary
 */
export interface QualityMeasurePerformance {
  overallCompliance: number; // 0-100
  totalMeasures: number;
  metMeasures: number;

  // By Category
  byCategory: {
    preventive: MeasureCategoryPerformance;
    chronicDisease: MeasureCategoryPerformance;
    mentalHealth: MeasureCategoryPerformance;
    medication: MeasureCategoryPerformance;
  };

  // Recent Results
  recentResults: QualityMeasureSummary[];
}

/**
 * Measure Category Performance
 */
export interface MeasureCategoryPerformance {
  compliance: number; // 0-100
  total: number;
  met: number;
}

/**
 * Quality Measure Summary
 */
export interface QualityMeasureSummary {
  measureId: string;
  measureName: string;
  category: string;
  compliant: boolean;
  score?: number;
  evaluationDate: Date;
  dueDate?: Date;
}

/**
 * Health Trend Data Point
 */
export interface HealthTrendDataPoint {
  date: Date;
  value: number;
  label?: string;
}

/**
 * Health Metric Trend
 */
export interface HealthMetricTrend {
  metric: string;
  unit: string;
  dataPoints: HealthTrendDataPoint[];
  trend: 'improving' | 'stable' | 'declining';
  currentValue: number;
  targetValue?: number;
  referenceRange?: {
    low: number;
    high: number;
  };
}

/**
 * Vital Sign History Point for trending over time
 */
export interface VitalSignHistoryPoint {
  date: Date;
  value: number;
  unit: string;
  interpretation?: string;
}

/**
 * Assessment History Entry for mental health assessments
 */
export interface AssessmentHistoryEntry {
  id: string;
  type: MentalHealthAssessmentType;
  date: Date;
  score: number;
  interpretation: string;
  provider?: string;
}

/**
 * Mental Health Trend Analysis
 */
export interface MentalHealthTrend {
  direction: 'improving' | 'stable' | 'declining';
  percentageChange: number;
  periodMonths: number;
  dataPoints: number;
}

/**
 * Assessment History for backend API responses
 */
export interface AssessmentHistory {
  assessmentId: string;
  type: 'PHQ-9' | 'GAD-7' | 'PHQ-2';
  score: number;
  severity: string;
  assessedAt: Date | string;
  provider?: string;
}

/**
 * Mental Health Diagnosis from FHIR (simplified for backend integration)
 */
export interface MentalHealthDiagnosisFhir {
  code: string;
  display: string;
  severity?: string;
  onsetDate?: Date;
  clinicalStatus: string;
}

/**
 * Medication (simplified for mental health summary)
 */
export interface Medication {
  code: string;
  display: string;
  status: 'active' | 'on-hold' | 'cancelled' | 'completed' | 'entered-in-error' | 'stopped' | 'draft' | 'unknown';
  dosage?: string;
  startDate?: Date;
}

/**
 * Mental Health Condition (simplified from diagnosis)
 */
export interface MentalHealthCondition {
  code: string;
  display: string;
  category: 'mood' | 'anxiety' | 'psychotic' | 'substance' | 'trauma' | 'other';
  severity?: 'mild' | 'moderate' | 'severe';
  onsetDate?: Date;
  status: string;
}

/**
 * Assessment (simplified for assessment history)
 */
export interface Assessment {
  id: string;
  type: MentalHealthAssessmentType;
  date: Date;
  score: number;
  severity: string;
}

/**
 * Social Determinants of Health (SDOH) - Feature 3.4
 * Comprehensive SDOH data parsed from FHIR QuestionnaireResponse and Observations
 */
export interface SocialDeterminants {
  patientId: string;
  screeningDate: Date;
  questionnaireType: SDOHQuestionnaireType;

  // Housing
  housingStatus: {
    stable: boolean;
    details: string;
  };

  // Food Security
  foodSecurity: {
    secure: boolean;
    details: string;
  };

  // Transportation
  transportation: {
    adequate: boolean;
    details: string;
  };

  // Employment
  employment: {
    status: string;
    details: string;
  };

  // Social Support
  socialSupport: {
    level: string;
    details: string;
  };

  // Risk Factors from FHIR Observations (SDOH category)
  riskFactors: SDOHRiskFactor[];

  // Service/Resource Referrals from ServiceRequest
  activeReferrals: ServiceReferral[];
}

/**
 * SDOH Risk Factor from FHIR Observation
 */
export interface SDOHRiskFactor {
  category: SDOHCategory;
  code: string;
  display: string;
  value: string;
  severity: SDOHSeverity;
  date: Date;
  zCode?: string;
}

/**
 * Service Referral from FHIR ServiceRequest
 */
export interface ServiceReferral {
  id: string;
  category: SDOHCategory;
  service: string;
  organization: string;
  status: 'draft' | 'active' | 'on-hold' | 'completed' | 'cancelled';
  priority: 'routine' | 'urgent' | 'asap' | 'stat';
  authoredDate: Date;
  occurrenceDate?: Date;
  note?: string;
}

/**
 * Multi-factor Risk Score (Feature 4.1)
 * Comprehensive risk assessment incorporating clinical, social, and mental health factors
 */
export interface MultiFactorRiskScore {
  patientId: string;
  overallScore: number; // Normalized score 0-100
  overallRisk: RiskLevel;
  calculatedAt: Date;

  // Component Scores (0-100)
  components: {
    clinicalComplexity: number;    // From conditions and medications
    sdohRisk: number;              // From social determinants
    mentalHealthRisk: number;      // From mental health assessments
  };

  // Factor Weights (clinical evidence-based)
  weights: {
    clinicalComplexity: number;    // Default: 0.40 (40%)
    sdohRisk: number;              // Default: 0.30 (30%)
    mentalHealthRisk: number;      // Default: 0.30 (30%)
  };

  // Detailed breakdown
  details: {
    conditionCount: number;
    uncontrolledConditionCount: number;
    medicationCount: number;
    comorbidityScore: number;
    sdohNeedCount: number;
    severeSdohNeedCount: number;
    mentalHealthAssessmentCount: number;
    highRiskMentalHealthConditions: number;
  };
}

/**
 * Category Risk Assessment - Feature 4.3
 * Condition-specific risk calculations for clinical categories
 */
export interface CategoryRiskAssessment {
  category: 'diabetes' | 'cardiovascular' | 'mental-health' | 'respiratory';
  riskLevel: 'low' | 'moderate' | 'high' | 'critical';
  score: number;
  factors: string[];
  recommendations: string[];
  lastAssessed: Date;
}

/**
 * Risk Trend Data - Feature 4.4
 * Historical risk score tracking and trend analysis
 */
export interface RiskTrendData {
  patientId: string;
  metric: string;
  trend: 'improving' | 'stable' | 'declining';
  dataPoints: { date: Date; value: number; label?: string }[];
  percentChange: number;
  startDate: Date;
  endDate: Date;
}

/**
 * Hospitalization Prediction - Feature 4.2
 * Predictive analytics for patient hospitalization risk
 */
export interface HospitalizationPrediction {
  patientId: string;
  probability30Day: number; // 0-100
  probability90Day: number; // 0-100
  confidence: {
    low: number;
    high: number;
  };
  factors: {
    name: string;
    weight: number;
    description: string;
  }[];
  calculatedAt: Date;
}

/**
 * Care Recommendation - Feature 5.2
 * Clinical decision support recommendation with evidence-based guidance
 */
export interface CareRecommendation {
  id: string;
  patientId: string;
  title: string;
  description: string;
  urgency: 'routine' | 'soon' | 'urgent' | 'emergent';
  category: 'preventive' | 'chronic' | 'acute' | 'lifestyle' | 'treatment' | 'referral' | 'education';
  evidenceSource: string;
  clinicalGuideline?: string;
  actionItems: string[];
  status: 'pending' | 'accepted' | 'declined' | 'completed';
  createdDate: Date;
  dueDate?: Date;
  completedDate?: Date;
  outcome?: RecommendationOutcome;

  // Legacy compatibility fields (for existing templates)
  priority?: 'low' | 'medium' | 'high';
  rationale?: string;
  evidence?: string;
  guidelineSource?: string;
  expectedBenefit?: string;
  resources?: CareResource[];
}

/**
 * Recommendation Outcome - Feature 5.2
 * Tracks the result of following a care recommendation
 */
export interface RecommendationOutcome {
  result: 'improved' | 'stable' | 'declined' | 'unknown';
  notes?: string;
  measuredDate: Date;
}

/**
 * Recommendation Filter - Feature 5.2
 * Filter criteria for retrieving care recommendations
 */
export interface RecommendationFilter {
  status?: string[];
  urgency?: string[];
  category?: string[];
}

/**
 * Community Resource - Feature 5.3
 * Community-based resources for SDOH referrals
 */
export interface CommunityResource {
  id: string;
  name: string;
  category: SDOHCategory;
  description: string;
  address: string;
  phone: string;
  website?: string;
  servicesOffered: string[];
  eligibilityRequirements?: string[];
  operatingHours?: string;
  acceptsReferrals: boolean;
}

/**
 * SDOH Referral Detail - Feature 5.3
 * Detailed referral to community resources for SDOH needs with full workflow tracking
 */
export interface SDOHReferralDetail {
  referralId: string;
  patientId: string;
  category: SDOHCategory;
  need: SDOHNeedWithDetails;
  resource: CommunityResource;
  status: 'draft' | 'sent' | 'accepted' | 'in-progress' | 'completed' | 'cancelled';
  priority: 'low' | 'medium' | 'high' | 'urgent';
  createdDate: Date;
  sentDate?: Date;
  acceptedDate?: Date;
  completedDate?: Date;
  cancelledDate?: Date;
  cancelReason?: string;
  outcome?: ReferralOutcome;
  notes?: string[];
}

/**
 * Referral Outcome - Feature 5.3
 * Outcome of an SDOH referral
 */
export interface ReferralOutcome {
  result: 'successful' | 'partial' | 'unsuccessful' | 'unknown';
  servicesReceived: string[];
  followUpNeeded: boolean;
  patientSatisfaction?: number; // 1-5 scale
  notes?: string;
  assessedDate: Date;
}

/**
 * Referral Search Criteria - Feature 5.3
 * Criteria for searching referral history
 */
export interface ReferralSearchCriteria {
  category?: SDOHCategory;
  status?: string[];
  dateRange?: { start: Date; end: Date };
}

/**
 * Care Gap Status - Feature 5.1
 * Status values for care gap lifecycle
 */
export type CareGapStatus = 'open' | 'closed' | 'excluded';

/**
 * Care Gap Status Update - Feature 5.1
 * Request to update care gap status
 */
export interface CareGapStatusUpdate {
  gapId: string;
  status: CareGapStatus;
  reason?: string;
  notes?: string;
  updatedBy: string;
  updatedDate?: Date;
}

/**
 * Care Gap Metrics - Feature 5.1
 * Analytics for care gap closure performance
 */
export interface CareGapMetrics {
  patientId: string;
  totalGaps: number;
  openGaps: number;
  closedGaps: number;
  excludedGaps: number;
  closureRate: number; // percentage
  averageTimeToClosureDays: number;
  categoryCounts: {
    preventive: number;
    'chronic-disease': number;
    'mental-health': number;
    medication: number;
    screening: number;
  };
  priorityCounts: {
    low: number;
    medium: number;
    high: number;
    urgent: number;
  };
}
