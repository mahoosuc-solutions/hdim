/**
 * Scheduled Evaluation Models
 *
 * Enables recurring batch evaluations with configurable schedules.
 * Schedules are stored locally and can be synced to backend when API is available.
 */

export type ScheduleFrequency = 'daily' | 'weekly' | 'monthly' | 'quarterly';
export type ScheduleStatus = 'active' | 'paused' | 'disabled';
export type DayOfWeek = 'monday' | 'tuesday' | 'wednesday' | 'thursday' | 'friday' | 'saturday' | 'sunday';

/**
 * Scheduled Evaluation definition
 */
export interface ScheduledEvaluation {
  id: string;
  name: string;
  description?: string;
  status: ScheduleStatus;

  // Schedule configuration
  frequency: ScheduleFrequency;
  timeOfDay: string; // HH:mm format (24-hour)
  dayOfWeek?: DayOfWeek; // For weekly schedules
  dayOfMonth?: number; // For monthly/quarterly (1-28)
  timezone: string;

  // Evaluation configuration
  measureIds: string[];
  patientFilter: PatientFilter;
  evaluationOptions: EvaluationOptions;

  // Execution tracking
  lastRun?: ScheduleExecution;
  nextRun?: string; // ISO timestamp
  consecutiveFailures: number;

  // Metadata
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  tenantId: string;

  // Notification settings
  notifications: NotificationSettings;
}

/**
 * Patient filter for scheduled evaluations
 */
export interface PatientFilter {
  type: 'all' | 'custom' | 'condition' | 'measure-eligible';
  patientIds?: string[];
  conditions?: string[]; // ICD-10 codes
  measureEligibility?: string[]; // Only patients eligible for these measures
  ageRange?: {
    min?: number;
    max?: number;
  };
  riskLevel?: ('low' | 'medium' | 'high' | 'critical')[];
}

/**
 * Evaluation options for scheduled runs
 */
export interface EvaluationOptions {
  parallelExecution: boolean;
  maxPatientsPerBatch: number;
  continueOnError: boolean;
  generateReport: boolean;
  exportFormat?: 'none' | 'csv' | 'excel' | 'qrda-i' | 'qrda-iii';
}

/**
 * Notification settings for scheduled evaluations
 */
export interface NotificationSettings {
  onSuccess: boolean;
  onFailure: boolean;
  onPartialSuccess: boolean;
  recipients: string[]; // Email addresses or user IDs
  includeReport: boolean;
}

/**
 * Execution record for a scheduled evaluation run
 */
export interface ScheduleExecution {
  id: string;
  scheduleId: string;
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';
  startedAt: string;
  completedAt?: string;
  durationMs?: number;

  // Results
  totalPatients: number;
  processedPatients: number;
  successCount: number;
  failureCount: number;

  // Details
  measureResults: MeasureExecutionSummary[];
  errors?: string[];
  reportUrl?: string;

  // Trigger info
  triggeredBy: 'schedule' | 'manual';
  triggerUser?: string;
}

/**
 * Summary of measure execution within a scheduled run
 */
export interface MeasureExecutionSummary {
  measureId: string;
  measureName: string;
  patientCount: number;
  complianceRate: number;
  avgExecutionMs: number;
}

/**
 * Create a new scheduled evaluation with defaults
 */
export function createScheduledEvaluation(
  name: string,
  measureIds: string[],
  frequency: ScheduleFrequency,
  tenantId: string,
  createdBy: string
): ScheduledEvaluation {
  const now = new Date().toISOString();

  return {
    id: generateScheduleId(),
    name,
    status: 'active',
    frequency,
    timeOfDay: '02:00', // Default to 2 AM
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    measureIds,
    patientFilter: {
      type: 'all',
    },
    evaluationOptions: {
      parallelExecution: true,
      maxPatientsPerBatch: 100,
      continueOnError: true,
      generateReport: true,
      exportFormat: 'none',
    },
    consecutiveFailures: 0,
    createdAt: now,
    createdBy,
    updatedAt: now,
    tenantId,
    notifications: {
      onSuccess: false,
      onFailure: true,
      onPartialSuccess: true,
      recipients: [],
      includeReport: true,
    },
  };
}

/**
 * Generate a unique schedule ID
 */
function generateScheduleId(): string {
  return `sched-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Calculate the next run time for a schedule
 */
export function calculateNextRun(schedule: ScheduledEvaluation): Date {
  const now = new Date();
  const [hours, minutes] = schedule.timeOfDay.split(':').map(Number);

  const nextRun = new Date();
  nextRun.setHours(hours, minutes, 0, 0);

  // If the time has already passed today, start from tomorrow
  if (nextRun <= now) {
    nextRun.setDate(nextRun.getDate() + 1);
  }

  switch (schedule.frequency) {
    case 'daily':
      // Already set to next occurrence
      break;

    case 'weekly':
      const dayMap: Record<DayOfWeek, number> = {
        sunday: 0,
        monday: 1,
        tuesday: 2,
        wednesday: 3,
        thursday: 4,
        friday: 5,
        saturday: 6,
      };
      const targetDay = dayMap[schedule.dayOfWeek || 'monday'];
      while (nextRun.getDay() !== targetDay) {
        nextRun.setDate(nextRun.getDate() + 1);
      }
      break;

    case 'monthly':
      const targetDayOfMonth = schedule.dayOfMonth || 1;
      nextRun.setDate(targetDayOfMonth);
      if (nextRun <= now) {
        nextRun.setMonth(nextRun.getMonth() + 1);
      }
      break;

    case 'quarterly':
      const quarterMonths = [0, 3, 6, 9]; // Jan, Apr, Jul, Oct
      const currentMonth = now.getMonth();
      const nextQuarterMonth = quarterMonths.find((m) => m > currentMonth) ?? quarterMonths[0];
      if (nextQuarterMonth <= currentMonth) {
        nextRun.setFullYear(nextRun.getFullYear() + 1);
      }
      nextRun.setMonth(nextQuarterMonth);
      nextRun.setDate(schedule.dayOfMonth || 1);
      break;
  }

  return nextRun;
}

/**
 * Get human-readable schedule description
 */
export function getScheduleDescription(schedule: ScheduledEvaluation): string {
  const time = formatTime(schedule.timeOfDay);

  switch (schedule.frequency) {
    case 'daily':
      return `Daily at ${time}`;
    case 'weekly':
      const day = schedule.dayOfWeek
        ? schedule.dayOfWeek.charAt(0).toUpperCase() + schedule.dayOfWeek.slice(1)
        : 'Monday';
      return `Every ${day} at ${time}`;
    case 'monthly':
      return `Monthly on day ${schedule.dayOfMonth || 1} at ${time}`;
    case 'quarterly':
      return `Quarterly on day ${schedule.dayOfMonth || 1} at ${time}`;
    default:
      return 'Custom schedule';
  }
}

/**
 * Format time for display
 */
function formatTime(time24: string): string {
  const [hours, minutes] = time24.split(':').map(Number);
  const period = hours >= 12 ? 'PM' : 'AM';
  const hours12 = hours % 12 || 12;
  return `${hours12}:${minutes.toString().padStart(2, '0')} ${period}`;
}
