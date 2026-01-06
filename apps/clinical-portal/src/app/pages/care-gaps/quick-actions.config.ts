import { CareGapAlert } from '../../models/care-gap.model';
import { QuickActionType, QuickActionConfig } from './dialogs/quick-action-dialog.component';

/**
 * Quick Actions Configuration per Gap Type
 *
 * Based on Issue #141 requirements:
 * | Gap Type | Quick Actions |
 * |----------|--------------|
 * | Screening (e.g., A1c) | Order Lab, Schedule Visit, Send Reminder |
 * | Medication Review | Review Meds, Refill Request, Schedule Visit |
 * | Preventive Care | Order Test, Schedule Procedure, Send Education |
 * | Behavioral Health | Schedule Screening, Refer to Specialist |
 */

const SCREENING_ACTIONS: QuickActionConfig[] = [
  {
    type: 'ORDER_LAB',
    label: 'Order Lab',
    icon: 'biotech',
    color: '#2196f3',
    description: 'Order lab test to close screening gap',
  },
  {
    type: 'SCHEDULE_VISIT',
    label: 'Schedule Visit',
    icon: 'event',
    color: '#4caf50',
    description: 'Schedule office or telehealth visit',
  },
  {
    type: 'SEND_REMINDER',
    label: 'Send Reminder',
    icon: 'notifications',
    color: '#ff9800',
    description: 'Send SMS, email, or letter reminder',
  },
];

const LAB_ACTIONS: QuickActionConfig[] = [
  {
    type: 'ORDER_LAB',
    label: 'Order Lab',
    icon: 'biotech',
    color: '#2196f3',
    description: 'Order lab test',
  },
  {
    type: 'SEND_REMINDER',
    label: 'Remind Patient',
    icon: 'notifications',
    color: '#ff9800',
    description: 'Send reminder about pending lab',
  },
  {
    type: 'SCHEDULE_VISIT',
    label: 'Schedule Visit',
    icon: 'event',
    color: '#4caf50',
    description: 'Schedule visit for lab draw',
  },
];

const MEDICATION_ACTIONS: QuickActionConfig[] = [
  {
    type: 'REVIEW_MEDS',
    label: 'Review Meds',
    icon: 'medication',
    color: '#9c27b0',
    description: 'Complete medication review',
  },
  {
    type: 'REFILL_REQUEST',
    label: 'Refill Request',
    icon: 'replay',
    color: '#00bcd4',
    description: 'Request medication refill',
  },
  {
    type: 'SCHEDULE_VISIT',
    label: 'Schedule Visit',
    icon: 'event',
    color: '#4caf50',
    description: 'Schedule medication management visit',
  },
];

const FOLLOWUP_ACTIONS: QuickActionConfig[] = [
  {
    type: 'SCHEDULE_VISIT',
    label: 'Schedule Visit',
    icon: 'event',
    color: '#4caf50',
    description: 'Schedule follow-up appointment',
  },
  {
    type: 'SEND_REMINDER',
    label: 'Send Reminder',
    icon: 'notifications',
    color: '#ff9800',
    description: 'Send follow-up reminder',
  },
  {
    type: 'REFER_SPECIALIST',
    label: 'Refer',
    icon: 'send',
    color: '#795548',
    description: 'Refer to specialist',
  },
];

const ASSESSMENT_ACTIONS: QuickActionConfig[] = [
  {
    type: 'SCHEDULE_SCREENING',
    label: 'Schedule Screening',
    icon: 'health_and_safety',
    color: '#4caf50',
    description: 'Schedule behavioral health screening',
  },
  {
    type: 'REFER_SPECIALIST',
    label: 'Refer Specialist',
    icon: 'send',
    color: '#795548',
    description: 'Refer to behavioral health specialist',
  },
  {
    type: 'SEND_EDUCATION',
    label: 'Send Education',
    icon: 'school',
    color: '#00bcd4',
    description: 'Send educational materials',
  },
];

/**
 * Get quick actions for a care gap based on its type
 */
export function getQuickActionsForGap(gap: CareGapAlert): QuickActionConfig[] {
  const measureName = gap.measureName.toLowerCase();

  // Special handling based on measure
  if (measureName.includes('breast') || measureName.includes('bcs')) {
    return [
      {
        type: 'SCHEDULE_PROCEDURE',
        label: 'Schedule Mammogram',
        icon: 'medical_services',
        color: '#e91e63',
        description: 'Schedule mammogram appointment',
      },
      {
        type: 'SEND_REMINDER',
        label: 'Send Reminder',
        icon: 'notifications',
        color: '#ff9800',
        description: 'Send screening reminder',
      },
    ];
  }

  if (measureName.includes('colorectal') || measureName.includes('col')) {
    return [
      {
        type: 'SCHEDULE_PROCEDURE',
        label: 'Schedule Colonoscopy',
        icon: 'medical_services',
        color: '#e91e63',
        description: 'Schedule colonoscopy',
      },
      {
        type: 'ORDER_LAB',
        label: 'Order FIT Test',
        icon: 'biotech',
        color: '#2196f3',
        description: 'Order FIT test as alternative',
      },
      {
        type: 'SEND_REMINDER',
        label: 'Send Reminder',
        icon: 'notifications',
        color: '#ff9800',
        description: 'Send screening reminder',
      },
    ];
  }

  if (measureName.includes('diabetes') || measureName.includes('cdc')) {
    const actions: QuickActionConfig[] = [];

    if (gap.gapDescription.toLowerCase().includes('a1c') || gap.gapDescription.toLowerCase().includes('hba1c')) {
      actions.push({
        type: 'ORDER_LAB',
        label: 'Order HbA1c',
        icon: 'biotech',
        color: '#2196f3',
        description: 'Order HbA1c lab test',
      });
    }

    if (gap.gapDescription.toLowerCase().includes('eye') || gap.gapDescription.toLowerCase().includes('retinal')) {
      actions.push({
        type: 'SCHEDULE_PROCEDURE',
        label: 'Schedule Eye Exam',
        icon: 'visibility',
        color: '#e91e63',
        description: 'Schedule diabetic eye exam',
      });
    }

    if (gap.gapDescription.toLowerCase().includes('foot')) {
      actions.push({
        type: 'SCHEDULE_VISIT',
        label: 'Schedule Foot Exam',
        icon: 'event',
        color: '#4caf50',
        description: 'Schedule diabetic foot exam',
      });
    }

    if (gap.gapDescription.toLowerCase().includes('nephropathy') || gap.gapDescription.toLowerCase().includes('urine')) {
      actions.push({
        type: 'ORDER_LAB',
        label: 'Order Urine Test',
        icon: 'biotech',
        color: '#2196f3',
        description: 'Order urine microalbumin test',
      });
    }

    // Add common actions if not enough specific ones
    if (actions.length < 2) {
      actions.push({
        type: 'SCHEDULE_VISIT',
        label: 'Schedule Visit',
        icon: 'event',
        color: '#4caf50',
        description: 'Schedule diabetes care visit',
      });
    }

    actions.push({
      type: 'SEND_REMINDER',
      label: 'Send Reminder',
      icon: 'notifications',
      color: '#ff9800',
      description: 'Send care reminder',
    });

    return actions.slice(0, 3);
  }

  if (measureName.includes('blood pressure') || measureName.includes('cbp')) {
    return [
      {
        type: 'SCHEDULE_VISIT',
        label: 'BP Follow-up',
        icon: 'event',
        color: '#4caf50',
        description: 'Schedule BP follow-up visit',
      },
      {
        type: 'REVIEW_MEDS',
        label: 'Review Meds',
        icon: 'medication',
        color: '#9c27b0',
        description: 'Review antihypertensive medications',
      },
      {
        type: 'SEND_REMINDER',
        label: 'Send Reminder',
        icon: 'notifications',
        color: '#ff9800',
        description: 'Send BP monitoring reminder',
      },
    ];
  }

  if (measureName.includes('statin') || measureName.includes('spc')) {
    return [
      {
        type: 'REVIEW_MEDS',
        label: 'Prescribe Statin',
        icon: 'medication',
        color: '#9c27b0',
        description: 'Review and prescribe statin therapy',
      },
      {
        type: 'ORDER_LAB',
        label: 'Order Lipid Panel',
        icon: 'biotech',
        color: '#2196f3',
        description: 'Order lipid panel before starting statin',
      },
      {
        type: 'SCHEDULE_VISIT',
        label: 'Schedule Visit',
        icon: 'event',
        color: '#4caf50',
        description: 'Schedule medication discussion visit',
      },
    ];
  }

  // Default based on gap type
  switch (gap.gapType) {
    case 'screening':
      return SCREENING_ACTIONS;
    case 'lab':
      return LAB_ACTIONS;
    case 'medication':
      return MEDICATION_ACTIONS;
    case 'followup':
      return FOLLOWUP_ACTIONS;
    case 'assessment':
      return ASSESSMENT_ACTIONS;
    default:
      return SCREENING_ACTIONS;
  }
}

/**
 * Get primary (first) quick action for a gap - shown as main button
 */
export function getPrimaryQuickAction(gap: CareGapAlert): QuickActionConfig {
  const actions = getQuickActionsForGap(gap);
  return actions[0];
}

/**
 * Get secondary quick actions for a gap - shown in dropdown
 */
export function getSecondaryQuickActions(gap: CareGapAlert): QuickActionConfig[] {
  const actions = getQuickActionsForGap(gap);
  return actions.slice(1);
}

/**
 * Closure time metrics - tracks how long gaps take to close based on intervention type
 */
export interface ClosureMetrics {
  interventionType: QuickActionType;
  averageClosureTime: number; // days
  successRate: number; // percentage
  sampleSize: number;
}

/**
 * Mock closure metrics data - in production, this would come from analytics
 */
export const CLOSURE_METRICS: ClosureMetrics[] = [
  { interventionType: 'ORDER_LAB', averageClosureTime: 7, successRate: 85, sampleSize: 1250 },
  { interventionType: 'SCHEDULE_VISIT', averageClosureTime: 14, successRate: 78, sampleSize: 890 },
  { interventionType: 'SEND_REMINDER', averageClosureTime: 21, successRate: 45, sampleSize: 2100 },
  { interventionType: 'SCHEDULE_PROCEDURE', averageClosureTime: 21, successRate: 72, sampleSize: 450 },
  { interventionType: 'REVIEW_MEDS', averageClosureTime: 3, successRate: 92, sampleSize: 320 },
  { interventionType: 'REFER_SPECIALIST', averageClosureTime: 28, successRate: 65, sampleSize: 180 },
];

/**
 * Get closure metrics for an intervention type
 */
export function getClosureMetrics(actionType: QuickActionType): ClosureMetrics | undefined {
  return CLOSURE_METRICS.find(m => m.interventionType === actionType);
}
