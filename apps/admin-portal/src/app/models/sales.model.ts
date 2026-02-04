/**
 * Sales Automation Engine Models
 *
 * Data structures for CRM functionality including leads, accounts,
 * contacts, opportunities, activities, sequences, and LinkedIn outreach.
 *
 * These models align with the sales-automation-service backend DTOs.
 */

// ==========================================
// Enums (matching backend)
// ==========================================

export type LeadStatus = 'OPEN' | 'QUALIFIED' | 'UNQUALIFIED' | 'CONVERTED' | 'LOST';
export type LeadSource = 'WEBSITE' | 'REFERRAL' | 'EVENT' | 'DEMO_REQUEST' | 'ROI_CALCULATOR' | 'PHONE' | 'PARTNER' | 'EMAIL' | 'OTHER';
export type OpportunityStage = 'DISCOVERY' | 'DEMO' | 'PROPOSAL' | 'NEGOTIATION' | 'CONTRACT' | 'CLOSED_WON' | 'CLOSED_LOST';
export type ActivityType = 'CALL' | 'EMAIL' | 'MEETING' | 'DEMO' | 'TASK' | 'FOLLOW_UP' | 'NOTE' | 'OTHER';
export type ActivityStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type SequenceStatus = 'ACTIVE' | 'INACTIVE' | 'DRAFT';
export type EnrollmentStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'UNSUBSCRIBED' | 'BOUNCED';
export type LinkedInCampaignStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED';
export type OutreachStatus = 'PENDING' | 'SENT' | 'ACCEPTED' | 'DECLINED' | 'NO_RESPONSE';

// ==========================================
// Lead Management
// ==========================================

export interface Lead {
  id: string;
  tenantId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  company: string;
  title?: string;
  source: LeadSource;
  status: LeadStatus;
  score: number;
  linkedInUrl?: string;
  website?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  notes?: string;
  assignedTo?: string;
  convertedAccountId?: string;
  convertedContactId?: string;
  convertedOpportunityId?: string;
  convertedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LeadCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  company: string;
  title?: string;
  source: LeadSource;
  linkedInUrl?: string;
  website?: string;
  notes?: string;
}

export interface LeadConversionResult {
  leadId: string;
  accountId: string;
  contactId: string;
  opportunityId?: string;
  convertedAt: string;
}

// ==========================================
// Account Management
// ==========================================

export interface Account {
  id: string;
  tenantId: string;
  name: string;
  industry?: string;
  website?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  employeeCount?: number;
  annualRevenue?: number;
  type: 'PROSPECT' | 'CUSTOMER' | 'PARTNER' | 'COMPETITOR' | 'OTHER';
  description?: string;
  ownerId?: string;
  parentAccountId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccountCreateRequest {
  name: string;
  industry?: string;
  website?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  employeeCount?: number;
  annualRevenue?: number;
  type: Account['type'];
  description?: string;
}

// ==========================================
// Contact Management
// ==========================================

export interface SalesContact {
  id: string;
  tenantId: string;
  accountId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  mobile?: string;
  title?: string;
  department?: string;
  linkedInUrl?: string;
  isPrimary: boolean;
  doNotCall: boolean;
  doNotEmail: boolean;
  notes?: string;
  ownerId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SalesContactCreateRequest {
  accountId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  mobile?: string;
  title?: string;
  department?: string;
  linkedInUrl?: string;
  isPrimary?: boolean;
  notes?: string;
}

// ==========================================
// Opportunity Management
// ==========================================

export interface Opportunity {
  id: string;
  tenantId: string;
  accountId: string;
  name: string;
  stage: OpportunityStage;
  amount: number;
  probability: number;
  closeDate: string;
  description?: string;
  nextStep?: string;
  leadSource?: LeadSource;
  campaignId?: string;
  primaryContactId?: string;
  ownerId?: string;
  lostReason?: string;
  wonReason?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OpportunityCreateRequest {
  accountId: string;
  name: string;
  stage?: OpportunityStage;
  amount: number;
  closeDate: string;
  description?: string;
  nextStep?: string;
  leadSource?: LeadSource;
  primaryContactId?: string;
}

export interface OpportunityStageUpdate {
  opportunityId: string;
  newStage: OpportunityStage;
  notes?: string;
}

// ==========================================
// Activity Management
// ==========================================

export interface Activity {
  id: string;
  tenantId: string;
  type: ActivityType;
  subject: string;
  description?: string;
  status: ActivityStatus;
  dueDate?: string;
  completedAt?: string;
  duration?: number; // in minutes
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  relatedToType?: 'LEAD' | 'ACCOUNT' | 'CONTACT' | 'OPPORTUNITY';
  relatedToId?: string;
  assignedTo?: string;
  outcome?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ActivityCreateRequest {
  type: ActivityType;
  subject: string;
  description?: string;
  dueDate?: string;
  duration?: number;
  priority?: Activity['priority'];
  relatedToType?: Activity['relatedToType'];
  relatedToId?: string;
}

export interface ActivityLogRequest {
  type: ActivityType;
  subject: string;
  description?: string;
  relatedToType: Activity['relatedToType'];
  relatedToId: string;
  outcome?: string;
  duration?: number;
}

// ==========================================
// Email Sequences
// ==========================================

export interface EmailSequence {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  status: SequenceStatus;
  steps: SequenceStep[];
  enrollmentCount: number;
  completedCount: number;
  replyRate: number;
  createdAt: string;
  updatedAt: string;
}

export interface SequenceStep {
  id: string;
  sequenceId: string;
  stepNumber: number;
  type: 'EMAIL' | 'TASK' | 'LINKEDIN' | 'WAIT';
  subject?: string;
  content?: string;
  templateId?: string;
  delayDays: number;
  delayHours: number;
}

export interface SequenceCreateRequest {
  name: string;
  description?: string;
  steps: Omit<SequenceStep, 'id' | 'sequenceId'>[];
}

export interface SequenceEnrollment {
  id: string;
  sequenceId: string;
  leadId?: string;
  contactId?: string;
  status: EnrollmentStatus;
  currentStep: number;
  enrolledAt: string;
  completedAt?: string;
  lastActivityAt?: string;
}

export interface SequenceAnalytics {
  sequenceId: string;
  totalEnrolled: number;
  activeEnrollments: number;
  completedEnrollments: number;
  emailsSent: number;
  emailsOpened: number;
  emailsClicked: number;
  replies: number;
  unsubscribes: number;
  bounces: number;
  openRate: number;
  clickRate: number;
  replyRate: number;
}

// ==========================================
// LinkedIn Outreach
// ==========================================

export interface LinkedInCampaign {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  status: LinkedInCampaignStatus;
  targetCriteria?: string;
  dailyLimit: number;
  totalSent: number;
  totalAccepted: number;
  acceptanceRate: number;
  createdAt: string;
  updatedAt: string;
}

export interface LinkedInOutreach {
  id: string;
  tenantId: string;
  campaignId?: string;
  leadId?: string;
  contactId?: string;
  type: 'CONNECTION' | 'INMAIL' | 'MESSAGE';
  status: OutreachStatus;
  message?: string;
  scheduledAt?: string;
  sentAt?: string;
  respondedAt?: string;
  linkedInProfileUrl: string;
  createdAt: string;
  updatedAt: string;
}

export interface LinkedInConnectionRequest {
  leadId?: string;
  contactId?: string;
  linkedInProfileUrl: string;
  message?: string;
  campaignId?: string;
}

export interface LinkedInInMailRequest {
  leadId?: string;
  contactId?: string;
  linkedInProfileUrl: string;
  subject: string;
  message: string;
  campaignId?: string;
}

export interface LinkedInAnalytics {
  totalConnections: number;
  pendingConnections: number;
  acceptedConnections: number;
  totalInMails: number;
  inMailResponses: number;
  connectionRate: number;
  responseRate: number;
  campaignPerformance: CampaignPerformance[];
}

export interface CampaignPerformance {
  campaignId: string;
  campaignName: string;
  sent: number;
  accepted: number;
  rate: number;
}

// ==========================================
// Pipeline & Kanban
// ==========================================

export interface PipelineKanban {
  stages: KanbanStage[];
  totalValue: number;
  weightedValue: number;
}

export interface KanbanStage {
  stage: OpportunityStage;
  opportunities: Opportunity[];
  count: number;
  value: number;
}

export interface PipelineMetrics {
  totalPipeline: number;
  weightedPipeline: number;
  averageDealSize: number;
  winRate: number;
  averageSalesCycle: number; // in days
  stageConversion: StageConversion[];
}

export interface StageConversion {
  fromStage: OpportunityStage;
  toStage: OpportunityStage;
  rate: number;
  averageDays: number;
}

export interface PipelineForecast {
  period: string;
  committed: number;
  bestCase: number;
  pipeline: number;
  closed: number;
}

// ==========================================
// Dashboard
// ==========================================

export interface SalesDashboard {
  leadMetrics: LeadMetrics;
  pipelineMetrics: PipelineSummary;
  activityMetrics: ActivityMetrics;
  recentLeads: Lead[];
  recentOpportunities: Opportunity[];
  recentActivities: Activity[];
  upcomingActivities: Activity[];
}

export interface LeadMetrics {
  totalLeads: number;
  openLeads: number;
  qualifiedLeads: number;
  convertedLeads: number;
  conversionRate: number;
  leadsBySource: { source: LeadSource; count: number }[];
  averageScore: number;
}

export interface PipelineSummary {
  totalValue: number;
  weightedValue: number;
  openOpportunities: number;
  closingThisMonth: number;
  closingThisQuarter: number;
  atRiskDeals: number;
}

export interface ActivityMetrics {
  totalActivities: number;
  completedToday: number;
  scheduledToday: number;
  overdueCount: number;
  callsThisWeek: number;
  emailsThisWeek: number;
  meetingsThisWeek: number;
}

// ==========================================
// Search & Filter
// ==========================================

export interface LeadFilter {
  status?: LeadStatus[];
  source?: LeadSource[];
  minScore?: number;
  maxScore?: number;
  assignedTo?: string;
  createdAfter?: string;
  createdBefore?: string;
}

export interface OpportunityFilter {
  stage?: OpportunityStage[];
  minAmount?: number;
  maxAmount?: number;
  closeDateAfter?: string;
  closeDateBefore?: string;
  ownerId?: string;
  accountId?: string;
}

export interface ActivityFilter {
  type?: ActivityType[];
  status?: ActivityStatus[];
  priority?: Activity['priority'][];
  dueDateAfter?: string;
  dueDateBefore?: string;
  assignedTo?: string;
}

// ==========================================
// Pagination
// ==========================================

export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
