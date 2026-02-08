/**
 * CX Portal Data Models
 */

// Lead model
export interface Lead {
  id: string;
  first_name: string;
  last_name: string;
  email: string;
  phone: string;
  company: string;
  title: string;
  tier: 'A' | 'B' | 'C' | 'U';
  linkedin_url: string;
  city: string;
  state: string;
  source: LeadSource;
  status: LeadStatus;
  org_type: string;
  patient_count: number | null;
  contract_types: string[];
  deal_size: number | null;
  close_probability: number;
  score: number;
  notes: string;
  tags: string[];
  created_at: string;
  updated_at: string;
  hdim_id?: string;
}

export type LeadStatus =
  | 'new'
  | 'contacted'
  | 'engaged'
  | 'qualified'
  | 'demo_scheduled'
  | 'demo_completed'
  | 'proposal_sent'
  | 'negotiation'
  | 'closed_won'
  | 'closed_lost'
  | 'nurture';

export type LeadSource =
  | 'cold_outreach'
  | 'warm_intro'
  | 'inbound'
  | 'conference'
  | 'referral'
  | 'content'
  | 'partner'
  | 'other';

// Investor model
export interface Investor {
  id: string;
  name: string;
  title: string;
  organization: string;
  email: string;
  phone: string;
  linkedin_url: string;
  tier: 'A' | 'B' | 'C';
  investor_type: InvestorType;
  status: InvestorStatus;
  focus_areas: string[];
  typical_check_size: string;
  portfolio_companies: string[];
  warm_intro_source: string;
  notes: string;
  tags: string[];
  created_at: string;
  updated_at: string;
  hdim_id?: string;
}

export type InvestorType = 'vc' | 'pe' | 'angel' | 'strategic' | 'family_office';

export type InvestorStatus =
  | 'identified'
  | 'researching'
  | 'outreach'
  | 'engaged'
  | 'passed'
  | 'committed';

// Customer model
export interface Customer {
  id: string;
  name: string;
  organization_type: string;
  primary_contact_name: string;
  primary_contact_email: string;
  primary_contact_phone: string;
  billing_contact_email: string;
  address: string;
  city: string;
  state: string;
  patient_count: number | null;
  contract_types: string[];
  deployment_type: 'shared' | 'dedicated' | 'private_cloud';
  integration_method: 'csv' | 'fhir' | 'n8n' | 'custom';
  status: CustomerStatus;
  contract_value: number | null;
  contract_start_date: string | null;
  contract_end_date: string | null;
  health_score: number | null;
  deployment_milestones: DeploymentMilestone[];
  notes: string;
  tags: string[];
  created_at: string;
  updated_at: string;
}

export type CustomerStatus = 'prospect' | 'onboarding' | 'active' | 'churned';

export interface DeploymentMilestone {
  id: string;
  name: string;
  description: string;
  status: 'pending' | 'in_progress' | 'completed' | 'blocked';
  completed_at: string | null;
  notes: string;
}

// Activity model
export interface Activity {
  id: string;
  activity_type: ActivityType;
  status: 'pending' | 'completed' | 'failed' | 'cancelled';
  direction: 'outbound' | 'inbound' | 'internal';
  lead_id: string | null;
  investor_id: string | null;
  customer_id: string | null;
  contact_id: string | null;
  subject: string;
  body: string;
  notes: string;
  email_from: string;
  email_to: string;
  meeting_type: string;
  meeting_location: string;
  meeting_duration_minutes: number;
  sequence_id: string | null;
  sequence_step: number;
  opened: boolean;
  opened_at: string | null;
  clicked: boolean;
  clicked_at: string | null;
  replied: boolean;
  replied_at: string | null;
  agent_id: string | null;
  automated: boolean;
  tags: string[];
  created_at: string;
  updated_at: string;
}

export type ActivityType =
  | 'email_sent'
  | 'email_received'
  | 'email_opened'
  | 'email_clicked'
  | 'email_bounced'
  | 'call_outbound'
  | 'call_inbound'
  | 'call_voicemail'
  | 'meeting_scheduled'
  | 'meeting_completed'
  | 'meeting_no_show'
  | 'linkedin_sent'
  | 'linkedin_accepted'
  | 'linkedin_message'
  | 'demo_scheduled'
  | 'demo_completed'
  | 'proposal_sent'
  | 'contract_sent'
  | 'contract_signed'
  | 'note'
  | 'task'
  | 'other';

// Bead model
export interface Bead {
  id: string;
  beads_id: string | null;
  title: string;
  description: string;
  bead_type: BeadType;
  status: BeadStatus;
  priority: 0 | 1 | 2 | 3 | 4;
  assignee: string | null;
  reporter: string | null;
  convoy_id: string | null;
  lead_id: string | null;
  investor_id: string | null;
  customer_id: string | null;
  formula_id: string | null;
  formula_step: number | null;
  blocks: string[];
  blocked_by: string[];
  tags: string[];
  estimated_hours: number | null;
  actual_hours: number | null;
  due_date: string | null;
  agent_id: string | null;
  agent_notes: string;
  created_at: string;
  updated_at: string;
  started_at: string | null;
  completed_at: string | null;
  is_open: boolean;
  is_blocked: boolean;
  is_done: boolean;
  is_ready: boolean;
  is_overdue: boolean;
}

export type BeadType =
  | 'investor_outreach'
  | 'customer_outreach'
  | 'deployment'
  | 'support_ticket'
  | 'task'
  | 'research'
  | 'follow_up';

export type BeadStatus = 'open' | 'in_progress' | 'blocked' | 'done' | 'closed';

// Dashboard models
export interface DashboardData {
  timestamp: string;
  leads: LeadStats;
  investors: InvestorStats;
  customers: CustomerStats;
  activities: ActivityStats;
  beads: BeadStats;
  recent_feed: Activity[];
  ready_beads: Bead[];
}

export interface LeadStats {
  total: number;
  new: number;
  qualified: number;
  demo_scheduled: number;
  closed_won: number;
  pipeline_value: number;
  hot_leads: number;
}

export interface InvestorStats {
  total: number;
  engaged: number;
  committed: number;
  outreach: number;
  by_tier: { A: number; B: number; C: number };
}

export interface CustomerStats {
  total: number;
  active: number;
  onboarding: number;
  total_contract_value: number;
  total_patients: number;
}

export interface ActivityStats {
  total_this_week: number;
  emails_sent: number;
  meetings: number;
  calls: number;
}

export interface BeadStats {
  total: number;
  open: number;
  in_progress: number;
  blocked: number;
  done_this_week: number;
}

export interface PipelineView {
  pipeline: Record<string, PipelineItem[]>;
  stage_totals: Record<string, { count: number; value?: number }>;
}

export interface PipelineItem {
  id: string;
  name: string;
  company?: string;
  organization?: string;
  tier: string;
  score?: number;
  deal_size?: number;
  close_probability?: number;
  investor_type?: string;
}

// Approval workflow models (Human-in-the-Loop)
export type ActionType =
  | 'email'
  | 'linkedin_message'
  | 'linkedin_connection'
  | 'calendar_invite'
  | 'social_post'
  | 'document_share'
  | 'external_api';

export type TargetType = 'investor' | 'customer' | 'partner' | 'network' | 'media';

export type Urgency = 'urgent' | 'normal' | 'low';

export type ActionStatus = 'pending' | 'approved' | 'rejected' | 'edited' | 'expired';

export interface PendingAction {
  id: string;
  action_type: ActionType;
  target_type: TargetType;
  target_id: string;
  target_name: string;
  lead_name?: string; // Optional: populated for customer/lead actions
  subject: string | null;
  content: string;
  metadata: Record<string, any>;
  urgency: Urgency;
  status: ActionStatus;
  scheduled_for?: string; // Optional: for calendar/meeting actions
  created_by: string;
  created_at: string;
  reviewed_by: string | null;
  reviewed_at: string | null;
  decision_notes: string | null;
  executed: boolean;
  executed_at: string | null;
  execution_result: Record<string, any> | null;
  bead_id: string | null;
  formula_step: string | null;
}

export interface ApprovalDecision {
  action_id: string;
  decision: 'approve' | 'reject' | 'edit';
  decision_notes?: string;
  notes?: string; // Alias for decision_notes
  edited_subject?: string;
  edited_content?: string;
}

export interface ApprovalStats {
  pending_count: number;
  urgent_count: number;
  approved_today: number;
  rejected_today: number;
  avg_approval_time_hours: number;
  oldest_pending_hours: number | null;
}
