/**
 * Risk Stratification Model Configuration
 * Issue #147: Add Risk Stratification View
 *
 * Defines risk levels, calculation factors, and visualization settings
 * for patient risk stratification in primary care workflows.
 */

export type RiskLevel = 'critical' | 'high' | 'moderate' | 'low';

export interface RiskLevelConfig {
  level: RiskLevel;
  label: string;
  description: string;
  minScore: number;
  maxScore: number;
  color: string;
  bgColor: string;
  icon: string;
  actionRequired: string;
}

export interface RiskFactor {
  id: string;
  name: string;
  weight: number;
  dataSource: string;
  description: string;
  icon: string;
}

export interface PatientRiskProfile {
  patientId: string;
  patientName: string;
  mrn: string;
  dateOfBirth: string;
  age: number;
  gender: string;
  overallRiskScore: number;
  riskLevel: RiskLevel;
  riskFactors: RiskFactorScore[];
  primaryConditions: string[];
  openCareGaps: number;
  lastVisit: string;
  nextScheduledVisit?: string;
  recentEdVisits: number;
  hccScore?: number;
  sdohRiskFactors?: string[];
  trending: 'improving' | 'stable' | 'worsening';
}

export interface RiskFactorScore {
  factorId: string;
  factorName: string;
  rawScore: number;
  weightedScore: number;
  maxScore: number;
  percentContribution: number;
  details?: string;
}

export interface RiskGroupSummary {
  level: RiskLevel;
  count: number;
  percentage: number;
  avgScore: number;
  topConditions: string[];
  topRiskFactors: string[];
}

/**
 * Risk Level Configurations
 */
export const RISK_LEVELS: RiskLevelConfig[] = [
  {
    level: 'critical',
    label: 'Critical Risk',
    description: 'Immediate attention needed - complex care management required',
    minScore: 85,
    maxScore: 100,
    color: '#c62828',
    bgColor: '#ffebee',
    icon: 'warning',
    actionRequired: 'Same-day outreach'
  },
  {
    level: 'high',
    label: 'High Risk',
    description: 'Weekly monitoring - proactive intervention recommended',
    minScore: 70,
    maxScore: 84,
    color: '#ef6c00',
    bgColor: '#fff3e0',
    icon: 'priority_high',
    actionRequired: 'Weekly review'
  },
  {
    level: 'moderate',
    label: 'Moderate Risk',
    description: 'Monthly review - preventive care focus',
    minScore: 50,
    maxScore: 69,
    color: '#f9a825',
    bgColor: '#fffde7',
    icon: 'schedule',
    actionRequired: 'Monthly check-in'
  },
  {
    level: 'low',
    label: 'Low Risk',
    description: 'Routine care - wellness and prevention',
    minScore: 0,
    maxScore: 49,
    color: '#2e7d32',
    bgColor: '#e8f5e9',
    icon: 'check_circle',
    actionRequired: 'Routine care'
  }
];

/**
 * Risk Calculation Factors (Primary Care)
 * Weights sum to 100%
 */
export const RISK_FACTORS: RiskFactor[] = [
  {
    id: 'hcc',
    name: 'HCC Risk Score',
    weight: 25,
    dataSource: 'hcc-service',
    description: 'CMS Hierarchical Condition Category risk score',
    icon: 'analytics'
  },
  {
    id: 'care_gaps',
    name: 'Open Care Gaps',
    weight: 20,
    dataSource: 'care-gap-service',
    description: 'Overdue preventive care and quality measures',
    icon: 'assignment_late'
  },
  {
    id: 'ed_visits',
    name: 'Recent ED Visits',
    weight: 15,
    dataSource: 'fhir-service (Encounter)',
    description: 'Emergency department utilization in past 12 months',
    icon: 'local_hospital'
  },
  {
    id: 'med_adherence',
    name: 'Medication Non-Adherence',
    weight: 15,
    dataSource: 'fhir-service (MedicationRequest)',
    description: 'Proportion of days covered (PDC) below threshold',
    icon: 'medication'
  },
  {
    id: 'chronic_conditions',
    name: 'Chronic Conditions',
    weight: 15,
    dataSource: 'fhir-service (Condition)',
    description: 'Number and severity of chronic conditions',
    icon: 'healing'
  },
  {
    id: 'sdoh',
    name: 'SDOH Factors',
    weight: 10,
    dataSource: 'sdoh-service',
    description: 'Social determinants of health risk indicators',
    icon: 'home'
  }
];

/**
 * Chronic condition categories for filtering
 */
export const CONDITION_CATEGORIES = [
  { id: 'diabetes', name: 'Diabetes', icd10Prefix: 'E11' },
  { id: 'chf', name: 'Heart Failure', icd10Prefix: 'I50' },
  { id: 'copd', name: 'COPD', icd10Prefix: 'J44' },
  { id: 'ckd', name: 'Chronic Kidney Disease', icd10Prefix: 'N18' },
  { id: 'hypertension', name: 'Hypertension', icd10Prefix: 'I10' },
  { id: 'depression', name: 'Depression', icd10Prefix: 'F32' },
  { id: 'anxiety', name: 'Anxiety', icd10Prefix: 'F41' },
  { id: 'obesity', name: 'Obesity', icd10Prefix: 'E66' }
];

/**
 * Helper Functions
 */

export function getRiskLevel(score: number): RiskLevelConfig {
  return RISK_LEVELS.find(r => score >= r.minScore && score <= r.maxScore) || RISK_LEVELS[3];
}

export function getRiskLevelByName(level: RiskLevel): RiskLevelConfig {
  return RISK_LEVELS.find(r => r.level === level) || RISK_LEVELS[3];
}

export function calculateRiskScore(factors: RiskFactorScore[]): number {
  return Math.round(factors.reduce((sum, f) => sum + f.weightedScore, 0));
}

export function getRiskTrendIcon(trending: 'improving' | 'stable' | 'worsening'): string {
  switch (trending) {
    case 'improving': return 'trending_down';
    case 'worsening': return 'trending_up';
    default: return 'trending_flat';
  }
}

export function getRiskTrendClass(trending: 'improving' | 'stable' | 'worsening'): string {
  switch (trending) {
    case 'improving': return 'trend-improving';
    case 'worsening': return 'trend-worsening';
    default: return 'trend-stable';
  }
}

/**
 * Generate mock patient risk profiles for demo
 */
export function generateMockPatients(count: number = 50): PatientRiskProfile[] {
  const firstNames = ['Maria', 'Robert', 'Angela', 'James', 'Patricia', 'Michael', 'Linda', 'David', 'Susan', 'William'];
  const lastNames = ['Garcia', 'Chen', 'Williams', 'Thompson', 'Davis', 'Johnson', 'Brown', 'Miller', 'Wilson', 'Anderson'];
  const conditions = ['Type 2 Diabetes', 'Heart Failure', 'COPD', 'CKD Stage 3', 'Hypertension', 'Depression', 'Anxiety', 'Obesity'];
  const sdohFactors = ['Food Insecurity', 'Transportation Barriers', 'Housing Instability', 'Social Isolation', 'Financial Strain'];

  const patients: PatientRiskProfile[] = [];

  for (let i = 0; i < count; i++) {
    const overallScore = Math.floor(Math.random() * 100);
    const riskLevel = getRiskLevel(overallScore).level;
    const numConditions = riskLevel === 'critical' ? 3 + Math.floor(Math.random() * 3) :
                          riskLevel === 'high' ? 2 + Math.floor(Math.random() * 2) :
                          riskLevel === 'moderate' ? 1 + Math.floor(Math.random() * 2) :
                          Math.floor(Math.random() * 2);

    const patientConditions = shuffleArray([...conditions]).slice(0, numConditions);
    const patientSdoh = riskLevel === 'critical' || riskLevel === 'high' ?
      shuffleArray([...sdohFactors]).slice(0, Math.floor(Math.random() * 3)) : [];

    const age = 40 + Math.floor(Math.random() * 45);
    const baseYear = 2025 - age;

    patients.push({
      patientId: `PAT-${String(i + 1).padStart(6, '0')}`,
      patientName: `${firstNames[i % firstNames.length]} ${lastNames[Math.floor(i / firstNames.length) % lastNames.length]}`,
      mrn: `MRN${String(100000 + i).padStart(7, '0')}`,
      dateOfBirth: `${baseYear}-${String(1 + Math.floor(Math.random() * 12)).padStart(2, '0')}-${String(1 + Math.floor(Math.random() * 28)).padStart(2, '0')}`,
      age,
      gender: Math.random() > 0.5 ? 'Male' : 'Female',
      overallRiskScore: overallScore,
      riskLevel,
      riskFactors: generateRiskFactors(overallScore),
      primaryConditions: patientConditions,
      openCareGaps: riskLevel === 'critical' ? 3 + Math.floor(Math.random() * 3) :
                    riskLevel === 'high' ? 2 + Math.floor(Math.random() * 2) :
                    riskLevel === 'moderate' ? 1 + Math.floor(Math.random() * 2) :
                    Math.floor(Math.random() * 2),
      lastVisit: generatePastDate(90),
      nextScheduledVisit: Math.random() > 0.3 ? generateFutureDate(60) : undefined,
      recentEdVisits: riskLevel === 'critical' ? 1 + Math.floor(Math.random() * 3) :
                      riskLevel === 'high' ? Math.floor(Math.random() * 2) : 0,
      hccScore: 0.5 + (Math.random() * 2.5 * (overallScore / 100)),
      sdohRiskFactors: patientSdoh,
      trending: Math.random() > 0.6 ? 'stable' : Math.random() > 0.5 ? 'improving' : 'worsening'
    });
  }

  return patients.sort((a, b) => b.overallRiskScore - a.overallRiskScore);
}

function generateRiskFactors(targetScore: number): RiskFactorScore[] {
  const factors: RiskFactorScore[] = [];
  let remainingScore = targetScore;

  for (let i = 0; i < RISK_FACTORS.length - 1; i++) {
    const factor = RISK_FACTORS[i];
    const maxContribution = factor.weight;
    const contribution = Math.min(remainingScore, Math.floor(Math.random() * maxContribution * 1.2));
    remainingScore -= contribution;

    factors.push({
      factorId: factor.id,
      factorName: factor.name,
      rawScore: Math.round((contribution / maxContribution) * 100),
      weightedScore: contribution,
      maxScore: maxContribution,
      percentContribution: Math.round((contribution / Math.max(targetScore, 1)) * 100)
    });
  }

  // Last factor gets remaining score
  const lastFactor = RISK_FACTORS[RISK_FACTORS.length - 1];
  factors.push({
    factorId: lastFactor.id,
    factorName: lastFactor.name,
    rawScore: Math.round((remainingScore / lastFactor.weight) * 100),
    weightedScore: Math.max(0, remainingScore),
    maxScore: lastFactor.weight,
    percentContribution: Math.round((remainingScore / Math.max(targetScore, 1)) * 100)
  });

  return factors;
}

function shuffleArray<T>(array: T[]): T[] {
  const shuffled = [...array];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
}

function generatePastDate(maxDaysAgo: number): string {
  const date = new Date();
  date.setDate(date.getDate() - Math.floor(Math.random() * maxDaysAgo));
  return date.toISOString().split('T')[0];
}

function generateFutureDate(maxDaysAhead: number): string {
  const date = new Date();
  date.setDate(date.getDate() + Math.floor(Math.random() * maxDaysAhead));
  return date.toISOString().split('T')[0];
}
