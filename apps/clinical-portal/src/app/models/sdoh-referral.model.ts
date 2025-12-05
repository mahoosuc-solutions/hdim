/**
 * SDOH Referral Workflow Models
 *
 * Models for the SDOH (Social Determinants of Health) Referral workflow
 * supporting both internal and external referrals.
 */

import {
  SDOHCategory,
  SDOHNeedWithDetails,
  SDOHScreeningResult,
  CommunityResource,
} from './patient-health.model';

/**
 * Referral Type - Internal vs External
 */
export type ReferralType = 'internal' | 'external';

/**
 * Internal Referral Destination Types
 */
export type InternalReferralDestination =
  | 'care-coordinator'
  | 'social-worker'
  | 'behavioral-health'
  | 'case-manager'
  | 'community-health-worker'
  | 'patient-navigator';

/**
 * External Referral Source
 */
export type ExternalReferralSource =
  | 'community-resource'
  | '211'
  | 'findhelp'
  | 'manual';

/**
 * Referral Urgency Level
 */
export type ReferralUrgency = 'routine' | 'soon' | 'urgent' | 'emergent';

/**
 * Patient Consent Status
 */
export type ConsentStatus =
  | 'obtained-verbal'
  | 'obtained-written'
  | 'obtained-electronic'
  | 'declined'
  | 'pending';

/**
 * Referral Status
 */
export type ReferralStatus =
  | 'draft'
  | 'pending'
  | 'sent'
  | 'accepted'
  | 'in-progress'
  | 'completed'
  | 'cancelled'
  | 'declined';

/**
 * Workflow Step Enumeration
 */
export enum ReferralWorkflowStep {
  SELECT_NEEDS = 0,
  CHOOSE_DESTINATION = 1,
  ENTER_DETAILS = 2,
  REVIEW_CONFIRM = 3,
  TRACK_OUTCOME = 4,
}

/**
 * Staff Member for Internal Referrals
 */
export interface StaffMember {
  id: string;
  name: string;
  role: InternalReferralDestination;
  email?: string;
  phone?: string;
  department?: string;
  availableCapacity?: number; // percentage 0-100
}

/**
 * Contact Information
 */
export interface ContactInfo {
  phone?: string;
  email?: string;
  address?: string;
  website?: string;
  fax?: string;
}

/**
 * Referral Destination (union of internal/external)
 */
export interface ReferralDestination {
  type: ReferralType;

  // Internal destination
  internalType?: InternalReferralDestination;
  assignedStaff?: StaffMember;

  // External destination
  externalSource?: ExternalReferralSource;
  communityResource?: CommunityResource;
  externalOrganization?: string;
  externalContactInfo?: ContactInfo;
}

/**
 * Referral Details Form Data
 */
export interface ReferralDetailsForm {
  urgency: ReferralUrgency;
  consentStatus: ConsentStatus;
  consentDate?: Date;
  consentObtainedBy?: string;

  // Clinical context
  clinicalNotes: string;
  relevantHistory?: string;
  currentInterventions?: string[];

  // Communication preferences
  preferredContactMethod: 'phone' | 'email' | 'fax' | 'portal';
  patientLanguagePreference?: string;
  interpreterNeeded?: boolean;

  // Follow-up
  followUpDays: number;
  notifyOnStatusChange: boolean;
  additionalRecipients?: string[];
}

/**
 * Referral Workflow State
 */
export interface ReferralWorkflowState {
  step: ReferralWorkflowStep;
  selectedNeeds: SDOHNeedWithDetails[];
  referralType: ReferralType | null;
  destination: ReferralDestination | null;
  details: ReferralDetailsForm | null;
  isDirty: boolean;
  isValid: boolean;
}

/**
 * Complete Referral Request (for submission)
 */
export interface SDOHReferralRequest {
  patientId: string;
  patientName?: string;
  needs: SDOHNeedWithDetails[];
  destination: ReferralDestination;
  details: ReferralDetailsForm;
  screeningId?: string;
  createdBy: string;
  createdAt: Date;
}

/**
 * SDOH Referral Detail (full referral record)
 */
export interface SDOHReferralDetail {
  id: string;
  patientId: string;
  patientName?: string;

  // SDOH Needs
  needs: SDOHNeedWithDetails[];
  screeningId?: string;

  // Destination
  destination: ReferralDestination;

  // Details
  urgency: ReferralUrgency;
  consentStatus: ConsentStatus;
  consentDate?: Date;
  clinicalNotes?: string;

  // Status tracking
  status: ReferralStatus;
  statusHistory: ReferralStatusUpdate[];

  // Dates
  createdAt: Date;
  sentAt?: Date;
  acceptedAt?: Date;
  completedAt?: Date;
  cancelledAt?: Date;

  // Metadata
  createdBy: string;
  lastUpdatedBy?: string;
  lastUpdatedAt?: Date;

  // Outcome
  outcome?: ReferralOutcomeDocumentation;
}

/**
 * Referral Status Update
 */
export interface ReferralStatusUpdate {
  previousStatus: ReferralStatus;
  newStatus: ReferralStatus;
  notes?: string;
  updatedBy: string;
  updatedAt: Date;
}

/**
 * Referral Outcome Documentation
 */
export interface ReferralOutcomeDocumentation {
  referralId: string;
  status:
    | 'successful'
    | 'partial'
    | 'unsuccessful'
    | 'patient-declined'
    | 'lost-to-followup';
  servicesReceived: string[];
  barriersEncountered?: string[];
  patientFeedback?: string;
  needResolved: boolean;
  requiresFollowUp: boolean;
  followUpPlan?: string;
  documentedBy: string;
  documentedAt: Date;
}

/**
 * External Integration Search Result (211, findhelp.org)
 */
export interface ExternalResourceSearchResult {
  source: ExternalReferralSource;
  id: string;
  name: string;
  description: string;
  categories: SDOHCategory[];
  contact: ContactInfo;
  services: string[];
  eligibility?: string;
  distance?: number;
  rating?: number;
  lastVerified?: Date;
}

/**
 * Dialog Data for SDOH Referral Dialog
 */
export interface SDOHReferralDialogData {
  patientId: string;
  patientName: string;
  screeningResult?: SDOHScreeningResult;
  preselectedNeeds?: SDOHNeedWithDetails[];
  existingReferral?: SDOHReferralDetail; // For editing/tracking
  mode: 'create' | 'edit' | 'track';
}

/**
 * Dialog Result
 */
export interface SDOHReferralDialogResult {
  action: 'submitted' | 'saved-draft' | 'cancelled';
  referral?: SDOHReferralDetail;
  outcome?: ReferralOutcomeDocumentation;
}

/**
 * Referral Search Criteria
 */
export interface ReferralSearchCriteria {
  status?: ReferralStatus[];
  urgency?: ReferralUrgency[];
  dateRange?: { start: Date; end: Date };
  destinationType?: ReferralType;
}

// ============================================
// Constants
// ============================================

/**
 * Internal destination display names
 */
export const INTERNAL_DESTINATION_NAMES: Record<
  InternalReferralDestination,
  string
> = {
  'care-coordinator': 'Care Coordinator',
  'social-worker': 'Social Worker',
  'behavioral-health': 'Behavioral Health Specialist',
  'case-manager': 'Case Manager',
  'community-health-worker': 'Community Health Worker',
  'patient-navigator': 'Patient Navigator',
};

/**
 * Internal destination icons
 */
export const INTERNAL_DESTINATION_ICONS: Record<
  InternalReferralDestination,
  string
> = {
  'care-coordinator': 'supervisor_account',
  'social-worker': 'support_agent',
  'behavioral-health': 'psychology',
  'case-manager': 'assignment_ind',
  'community-health-worker': 'volunteer_activism',
  'patient-navigator': 'explore',
};

/**
 * Urgency display configuration
 */
export const REFERRAL_URGENCY_CONFIG: Record<
  ReferralUrgency,
  { label: string; icon: string; color: string; description: string }
> = {
  emergent: {
    label: 'Emergent',
    icon: 'error',
    color: '#f44336',
    description: 'Requires immediate attention within 24 hours',
  },
  urgent: {
    label: 'Urgent',
    icon: 'warning',
    color: '#ff9800',
    description: 'Should be addressed within 1 week',
  },
  soon: {
    label: 'Soon',
    icon: 'schedule',
    color: '#ffc107',
    description: 'Should be addressed within 2-4 weeks',
  },
  routine: {
    label: 'Routine',
    icon: 'check_circle',
    color: '#4caf50',
    description: 'Can be addressed at next opportunity',
  },
};

/**
 * Status display configuration
 */
export const REFERRAL_STATUS_CONFIG: Record<
  ReferralStatus,
  { label: string; icon: string; color: string }
> = {
  draft: { label: 'Draft', icon: 'edit', color: '#9e9e9e' },
  pending: { label: 'Pending', icon: 'hourglass_empty', color: '#ff9800' },
  sent: { label: 'Sent', icon: 'send', color: '#2196f3' },
  accepted: { label: 'Accepted', icon: 'thumb_up', color: '#4caf50' },
  'in-progress': { label: 'In Progress', icon: 'autorenew', color: '#2196f3' },
  completed: { label: 'Completed', icon: 'check_circle', color: '#4caf50' },
  cancelled: { label: 'Cancelled', icon: 'cancel', color: '#9e9e9e' },
  declined: { label: 'Declined', icon: 'thumb_down', color: '#f44336' },
};

/**
 * Consent status display configuration
 */
export const CONSENT_STATUS_CONFIG: Record<
  ConsentStatus,
  { label: string; icon: string }
> = {
  'obtained-verbal': { label: 'Verbal Consent Obtained', icon: 'record_voice_over' },
  'obtained-written': { label: 'Written Consent on File', icon: 'description' },
  'obtained-electronic': { label: 'Electronic Consent', icon: 'verified' },
  declined: { label: 'Patient Declined', icon: 'do_not_disturb' },
  pending: { label: 'Consent Pending', icon: 'pending' },
};

// ============================================
// Helper Functions
// ============================================

/**
 * Get initial workflow state
 */
export function getInitialWorkflowState(): ReferralWorkflowState {
  return {
    step: ReferralWorkflowStep.SELECT_NEEDS,
    selectedNeeds: [],
    referralType: null,
    destination: null,
    details: null,
    isDirty: false,
    isValid: false,
  };
}

/**
 * Get default referral details
 */
export function getDefaultReferralDetails(): ReferralDetailsForm {
  return {
    urgency: 'routine',
    consentStatus: 'pending',
    clinicalNotes: '',
    preferredContactMethod: 'phone',
    followUpDays: 14,
    notifyOnStatusChange: true,
  };
}

/**
 * Check if consent is obtained
 */
export function isConsentObtained(status: ConsentStatus): boolean {
  return (
    status === 'obtained-verbal' ||
    status === 'obtained-written' ||
    status === 'obtained-electronic'
  );
}

/**
 * Get destination display name
 */
export function getDestinationDisplayName(
  destination: ReferralDestination
): string {
  if (destination.type === 'internal') {
    if (destination.assignedStaff) {
      return destination.assignedStaff.name;
    }
    return destination.internalType
      ? INTERNAL_DESTINATION_NAMES[destination.internalType]
      : 'Internal Referral';
  } else {
    if (destination.communityResource) {
      return destination.communityResource.name;
    }
    return destination.externalOrganization || 'External Referral';
  }
}

/**
 * Validate referral request
 */
export function validateReferralRequest(
  request: Partial<SDOHReferralRequest>
): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!request.patientId) {
    errors.push('Patient ID is required');
  }

  if (!request.needs || request.needs.length === 0) {
    errors.push('At least one SDOH need must be selected');
  }

  if (!request.destination) {
    errors.push('Referral destination is required');
  }

  if (!request.details) {
    errors.push('Referral details are required');
  } else {
    if (!request.details.urgency) {
      errors.push('Urgency level is required');
    }
    if (!request.details.consentStatus) {
      errors.push('Consent status is required');
    }
    if (!request.details.clinicalNotes || request.details.clinicalNotes.length < 10) {
      errors.push('Clinical notes must be at least 10 characters');
    }
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}
