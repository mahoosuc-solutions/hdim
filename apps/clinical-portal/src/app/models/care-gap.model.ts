/**
 * Care Gap Alert Model
 * Represents an urgent patient care gap that requires attention
 */
export interface CareGapAlert {
  gapId?: string;
  patientId: string;
  patientName: string;
  mrn: string;
  gapType: 'screening' | 'medication' | 'followup' | 'lab' | 'assessment';
  gapDescription: string;
  daysOverdue: number;
  urgency: 'high' | 'medium' | 'low';
  measureName: string;
  dueDate?: string;
  lastContactDate?: string;
}

/**
 * Care Gap Summary
 * Aggregated view of care gaps by type
 */
export interface CareGapSummary {
  totalGaps: number;
  highUrgencyCount: number;
  mediumUrgencyCount: number;
  lowUrgencyCount: number;
  byType: {
    screening: number;
    medication: number;
    followup: number;
    lab: number;
    assessment: number;
  };
  topAlerts: CareGapAlert[];
}

/**
 * Helper function to get icon for care gap type
 */
export function getCareGapIcon(gapType: CareGapAlert['gapType']): string {
  const iconMap = {
    screening: 'health_and_safety',
    medication: 'medication',
    followup: 'event_repeat',
    lab: 'biotech',
    assessment: 'psychology',
  };
  return iconMap[gapType] || 'warning';
}

/**
 * Helper function to get color for urgency level
 */
export function getUrgencyColor(urgency: CareGapAlert['urgency']): 'warn' | 'accent' | 'primary' {
  const colorMap = {
    high: 'warn' as const,
    medium: 'accent' as const,
    low: 'primary' as const,
  };
  return colorMap[urgency] || 'primary';
}

/**
 * Helper function to format days overdue
 */
export function formatDaysOverdue(days: number): string {
  if (days === 0) return 'Due today';
  if (days === 1) return '1 day overdue';
  if (days < 0) return `Due in ${Math.abs(days)} days`;
  if (days < 30) return `${days} days overdue`;
  if (days < 365) {
    const months = Math.floor(days / 30);
    return `${months} month${months > 1 ? 's' : ''} overdue`;
  }
  const years = Math.floor(days / 365);
  return `${years} year${years > 1 ? 's' : ''} overdue`;
}
