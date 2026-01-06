import { QualityMeasureResult } from '../../models/quality-result.model';

/**
 * Severity levels for result highlighting
 * Based on Issue #145 requirements
 */
export type ResultSeverity = 'critical' | 'high' | 'moderate' | 'normal';

/**
 * Trend direction for result comparison
 */
export type TrendDirection = 'improving' | 'worsening' | 'stable';

/**
 * Enhanced result with severity, trend, and context
 */
export interface EnhancedResult extends QualityMeasureResult {
  severity: ResultSeverity;
  trend?: TrendDirection;
  trendIcon?: string;
  previousValue?: number;
  previousDate?: string;
  patientContext?: PatientContext;
}

/**
 * Patient context for inline display
 */
export interface PatientContext {
  age: number;
  gender: string;
  conditions: string[];
  medications: string[];
  riskLevel: 'low' | 'moderate' | 'high';
}

/**
 * Quick action types for results
 */
export type ResultActionType = 'CONTACT_PATIENT' | 'ORDER_FOLLOWUP' | 'REFER_SPECIALIST' | 'SCHEDULE_VISIT' | 'SIGN_RESULT';

/**
 * Quick action configuration
 */
export interface ResultActionConfig {
  type: ResultActionType;
  label: string;
  icon: string;
  color: string;
  description: string;
}

/**
 * Panic values configuration - results requiring immediate attention
 * Based on clinical thresholds
 */
const PANIC_VALUES: Record<string, { low?: number; high?: number }> = {
  // Potassium (K+)
  'POTASSIUM': { low: 2.5, high: 6.5 },
  // Glucose
  'GLUCOSE': { low: 40, high: 500 },
  // HbA1c
  'HBA1C': { high: 12.0 },
  // Blood Pressure Systolic
  'BP_SYSTOLIC': { high: 180 },
  // Blood Pressure Diastolic
  'BP_DIASTOLIC': { high: 120 },
  // Creatinine
  'CREATININE': { high: 10.0 },
  // Hemoglobin
  'HEMOGLOBIN': { low: 7.0, high: 20.0 },
};

/**
 * Calculate severity level for a result
 */
export function calculateSeverity(result: QualityMeasureResult): ResultSeverity {
  // Non-compliant results with low compliance rate are high severity
  if (!result.numeratorCompliant && result.denominatorEligible) {
    if (result.complianceRate < 25) {
      return 'critical';
    }
    if (result.complianceRate < 50) {
      return 'high';
    }
    if (result.complianceRate < 75) {
      return 'moderate';
    }
  }

  // Check for specific measure thresholds
  const measureLower = result.measureName.toLowerCase();

  // Diabetes measures with poor control
  if (measureLower.includes('diabetes') || measureLower.includes('hba1c') || measureLower.includes('a1c')) {
    if (!result.numeratorCompliant) {
      return 'high';
    }
  }

  // Blood pressure measures
  if (measureLower.includes('blood pressure') || measureLower.includes('hypertension')) {
    if (!result.numeratorCompliant) {
      return 'high';
    }
  }

  // Cancer screening overdue
  if (measureLower.includes('breast cancer') || measureLower.includes('colorectal') ||
      measureLower.includes('cervical')) {
    if (!result.numeratorCompliant) {
      return 'moderate';
    }
  }

  // Default: compliant is normal, non-compliant is moderate
  return result.numeratorCompliant ? 'normal' : 'moderate';
}

/**
 * Calculate trend between current and previous values
 * Based on Issue #145 trend calculation algorithm
 */
export function calculateTrend(
  current: number,
  previous: number,
  normalRange: [number, number]
): TrendDirection {
  const normalMid = (normalRange[0] + normalRange[1]) / 2;
  const rangeSize = normalRange[1] - normalRange[0];
  const threshold = 0.1 * rangeSize;

  const currentFromNormal = Math.abs(current - normalMid);
  const previousFromNormal = Math.abs(previous - normalMid);

  if (currentFromNormal < previousFromNormal - threshold) {
    return 'improving';
  }
  if (currentFromNormal > previousFromNormal + threshold) {
    return 'worsening';
  }
  return 'stable';
}

/**
 * Get trend icon based on direction
 */
export function getTrendIcon(trend: TrendDirection): string {
  switch (trend) {
    case 'improving':
      return 'trending_up';
    case 'worsening':
      return 'trending_down';
    case 'stable':
      return 'trending_flat';
  }
}

/**
 * Get trend CSS class
 */
export function getTrendClass(trend: TrendDirection): string {
  switch (trend) {
    case 'improving':
      return 'trend-improving';
    case 'worsening':
      return 'trend-worsening';
    case 'stable':
      return 'trend-stable';
  }
}

/**
 * Get severity CSS class
 */
export function getSeverityClass(severity: ResultSeverity): string {
  return `severity-${severity}`;
}

/**
 * Get severity badge class
 */
export function getSeverityBadgeClass(severity: ResultSeverity): string {
  switch (severity) {
    case 'critical':
      return 'severity-badge-critical';
    case 'high':
      return 'severity-badge-high';
    case 'moderate':
      return 'severity-badge-moderate';
    case 'normal':
      return 'severity-badge-normal';
  }
}

/**
 * Get severity display text
 */
export function getSeverityText(severity: ResultSeverity): string {
  switch (severity) {
    case 'critical':
      return 'Critical';
    case 'high':
      return 'High';
    case 'moderate':
      return 'Moderate';
    case 'normal':
      return 'Normal';
  }
}

/**
 * Quick actions for non-compliant results
 */
const NON_COMPLIANT_ACTIONS: ResultActionConfig[] = [
  {
    type: 'CONTACT_PATIENT',
    label: 'Contact Patient',
    icon: 'phone',
    color: '#1976d2',
    description: 'Reach out to patient about result',
  },
  {
    type: 'ORDER_FOLLOWUP',
    label: 'Order Follow-up',
    icon: 'add_task',
    color: '#4caf50',
    description: 'Order follow-up test or visit',
  },
  {
    type: 'REFER_SPECIALIST',
    label: 'Refer',
    icon: 'send',
    color: '#ff9800',
    description: 'Refer to specialist',
  },
];

/**
 * Quick actions for compliant results
 */
const COMPLIANT_ACTIONS: ResultActionConfig[] = [
  {
    type: 'SIGN_RESULT',
    label: 'Sign Result',
    icon: 'check_circle',
    color: '#4caf50',
    description: 'Digitally sign and acknowledge result',
  },
  {
    type: 'SCHEDULE_VISIT',
    label: 'Schedule Follow-up',
    icon: 'event',
    color: '#1976d2',
    description: 'Schedule routine follow-up',
  },
];

/**
 * Critical severity quick actions
 */
const CRITICAL_ACTIONS: ResultActionConfig[] = [
  {
    type: 'CONTACT_PATIENT',
    label: 'Contact Now',
    icon: 'phone_callback',
    color: '#d32f2f',
    description: 'Urgent: Contact patient immediately',
  },
  {
    type: 'ORDER_FOLLOWUP',
    label: 'Urgent Order',
    icon: 'priority_high',
    color: '#d32f2f',
    description: 'Order urgent follow-up',
  },
  {
    type: 'REFER_SPECIALIST',
    label: 'Urgent Referral',
    icon: 'local_hospital',
    color: '#d32f2f',
    description: 'Urgent specialist referral',
  },
];

/**
 * Get quick actions for a result based on its status
 */
export function getQuickActionsForResult(result: EnhancedResult): ResultActionConfig[] {
  if (result.severity === 'critical') {
    return CRITICAL_ACTIONS;
  }

  if (result.numeratorCompliant) {
    return COMPLIANT_ACTIONS;
  }

  return NON_COMPLIANT_ACTIONS;
}

/**
 * Get primary action for result
 */
export function getPrimaryResultAction(result: EnhancedResult): ResultActionConfig {
  const actions = getQuickActionsForResult(result);
  return actions[0];
}

/**
 * Get secondary actions for result
 */
export function getSecondaryResultActions(result: EnhancedResult): ResultActionConfig[] {
  const actions = getQuickActionsForResult(result);
  return actions.slice(1);
}

/**
 * Mock patient context generator - in production this would come from patient service
 */
export function generateMockPatientContext(patientId: string): PatientContext {
  // Generate pseudo-random but consistent values based on patient ID
  const hash = patientId.split('').reduce((a, b) => {
    a = ((a << 5) - a) + b.charCodeAt(0);
    return a & a;
  }, 0);

  const ages = [35, 42, 55, 62, 68, 71, 45, 58];
  const genders = ['Male', 'Female'];
  const conditionSets = [
    ['Type 2 Diabetes', 'Hypertension'],
    ['Hypertension', 'Hyperlipidemia'],
    ['Type 2 Diabetes'],
    ['Asthma', 'Obesity'],
    ['Hypertension', 'Type 2 Diabetes', 'CKD Stage 3'],
    ['Heart Failure', 'Hypertension'],
    ['COPD'],
    ['Obesity', 'Pre-diabetes'],
  ];
  const medicationSets = [
    ['Metformin 1000mg', 'Lisinopril 20mg'],
    ['Lisinopril 10mg', 'Atorvastatin 40mg'],
    ['Metformin 500mg'],
    ['Albuterol inhaler'],
    ['Metformin 1000mg', 'Lisinopril 20mg', 'Amlodipine 5mg'],
    ['Lisinopril 40mg', 'Carvedilol 12.5mg'],
    ['Tiotropium inhaler'],
    [],
  ];
  const riskLevels: Array<'low' | 'moderate' | 'high'> = ['low', 'moderate', 'high'];

  const idx = Math.abs(hash) % 8;

  return {
    age: ages[idx],
    gender: genders[Math.abs(hash) % 2],
    conditions: conditionSets[idx],
    medications: medicationSets[idx],
    riskLevel: riskLevels[Math.abs(hash) % 3],
  };
}

/**
 * Generate mock previous result for comparison
 */
export function generateMockPreviousValue(currentRate: number): { value: number; date: string } {
  // Generate a pseudo-random previous value based on current
  const variance = (Math.random() - 0.5) * 30; // +/- 15%
  const previousValue = Math.max(0, Math.min(100, currentRate + variance));

  // Generate a date 30-90 days ago
  const daysAgo = 30 + Math.floor(Math.random() * 60);
  const previousDate = new Date();
  previousDate.setDate(previousDate.getDate() - daysAgo);

  return {
    value: Math.round(previousValue * 10) / 10,
    date: previousDate.toISOString(),
  };
}

/**
 * Enhance a result with severity, trend, and context
 */
export function enhanceResult(result: QualityMeasureResult): EnhancedResult {
  const severity = calculateSeverity(result);
  const patientContext = generateMockPatientContext(result.patientId);

  // Generate mock previous value for trend
  const previous = generateMockPreviousValue(result.complianceRate);
  const trend = calculateTrend(
    result.complianceRate,
    previous.value,
    [60, 100] // Normal compliance range
  );

  return {
    ...result,
    severity,
    trend,
    trendIcon: getTrendIcon(trend),
    previousValue: previous.value,
    previousDate: previous.date,
    patientContext,
  };
}

/**
 * Enhance array of results
 */
export function enhanceResults(results: QualityMeasureResult[]): EnhancedResult[] {
  return results.map(enhanceResult);
}

/**
 * Sort results by severity (critical first)
 */
export function sortBySeverity(results: EnhancedResult[]): EnhancedResult[] {
  const severityOrder: Record<ResultSeverity, number> = {
    critical: 0,
    high: 1,
    moderate: 2,
    normal: 3,
  };

  return [...results].sort((a, b) => severityOrder[a.severity] - severityOrder[b.severity]);
}

/**
 * Filter results by severity
 */
export function filterBySeverity(
  results: EnhancedResult[],
  severities: ResultSeverity[]
): EnhancedResult[] {
  return results.filter(r => severities.includes(r.severity));
}

/**
 * Get summary counts by severity
 */
export function getSeverityCounts(results: EnhancedResult[]): Record<ResultSeverity, number> {
  return results.reduce(
    (acc, result) => {
      acc[result.severity]++;
      return acc;
    },
    { critical: 0, high: 0, moderate: 0, normal: 0 }
  );
}
