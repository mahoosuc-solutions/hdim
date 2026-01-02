/**
 * Sales Automation TypeScript types
 * Mirrors backend DTOs from sales-automation-service
 */

// ==================== Enums ====================

export enum LeadSource {
  WEBSITE = 'WEBSITE',
  ROI_CALCULATOR = 'ROI_CALCULATOR',
  REFERRAL = 'REFERRAL',
  CONFERENCE = 'CONFERENCE',
  COLD_OUTREACH = 'COLD_OUTREACH',
  PARTNER = 'PARTNER',
  SOCIAL_MEDIA = 'SOCIAL_MEDIA',
  OTHER = 'OTHER'
}

export enum LeadStatus {
  NEW = 'NEW',
  CONTACTED = 'CONTACTED',
  QUALIFIED = 'QUALIFIED',
  UNQUALIFIED = 'UNQUALIFIED',
  CONVERTED = 'CONVERTED',
  LOST = 'LOST'
}

export enum AccountType {
  ACO = 'ACO',
  HEALTH_SYSTEM = 'HEALTH_SYSTEM',
  PAYER = 'PAYER',
  HIE = 'HIE',
  FQHC = 'FQHC',
  CLINIC = 'CLINIC',
  OTHER = 'OTHER'
}

export enum AccountStage {
  PROSPECT = 'PROSPECT',
  QUALIFIED = 'QUALIFIED',
  DEMO = 'DEMO',
  PROPOSAL = 'PROPOSAL',
  NEGOTIATION = 'NEGOTIATION',
  CLOSED_WON = 'CLOSED_WON',
  CLOSED_LOST = 'CLOSED_LOST'
}

export enum ContactType {
  DECISION_MAKER = 'DECISION_MAKER',
  INFLUENCER = 'INFLUENCER',
  USER = 'USER',
  CHAMPION = 'CHAMPION',
  BLOCKER = 'BLOCKER',
  OTHER = 'OTHER'
}

export enum OpportunityStage {
  DISCOVERY = 'DISCOVERY',
  DEMO = 'DEMO',
  PROPOSAL = 'PROPOSAL',
  NEGOTIATION = 'NEGOTIATION',
  CLOSED_WON = 'CLOSED_WON',
  CLOSED_LOST = 'CLOSED_LOST'
}

export enum ActivityType {
  CALL = 'CALL',
  EMAIL = 'EMAIL',
  MEETING = 'MEETING',
  NOTE = 'NOTE',
  TASK = 'TASK'
}

export enum EnrollmentStatus {
  ACTIVE = 'ACTIVE',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
  UNSUBSCRIBED = 'UNSUBSCRIBED',
  BOUNCED = 'BOUNCED',
  CONVERTED = 'CONVERTED',
  CANCELLED = 'CANCELLED'
}

export enum SequenceType {
  NURTURE = 'NURTURE',
  FOLLOW_UP = 'FOLLOW_UP',
  ONBOARDING = 'ONBOARDING',
  RE_ENGAGEMENT = 'RE_ENGAGEMENT',
  PROMOTION = 'PROMOTION',
  EVENT = 'EVENT',
  TRIAL = 'TRIAL',
  WELCOME = 'WELCOME',
  CUSTOM = 'CUSTOM'
}

// ==================== Core Entities ====================

export interface Lead {
  id: string;
  tenantId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  company?: string;
  title?: string;
  source: LeadSource;
  status: LeadStatus;
  score: number;
  zohoLeadId?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Contact {
  id: string;
  tenantId: string;
  accountId?: string;
  accountName?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  title?: string;
  department?: string;
  type: ContactType;
  zohoContactId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Account {
  id: string;
  tenantId: string;
  name: string;
  type: AccountType;
  industry?: string;
  website?: string;
  phone?: string;
  patientCount?: number;
  ehrCount?: number;
  state?: string;
  city?: string;
  stage: AccountStage;
  zohoAccountId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Opportunity {
  id: string;
  tenantId: string;
  accountId: string;
  accountName?: string;
  primaryContactId?: string;
  primaryContactName?: string;
  name: string;
  amount: number;
  stage: OpportunityStage;
  expectedCloseDate: string;
  probability: number;
  lostReason?: string;
  zohoOpportunityId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Activity {
  id: string;
  tenantId: string;
  leadId?: string;
  contactId?: string;
  opportunityId?: string;
  accountId?: string;
  type: ActivityType;
  subject: string;
  description?: string;
  scheduledAt?: string;
  completedAt?: string;
  assignedToUserId?: string;
  completed: boolean;
  createdAt: string;
  updatedAt: string;
}

// ==================== Pipeline DTOs ====================

export interface KanbanCard {
  opportunityId: string;
  opportunityName: string;
  accountId: string;
  accountName: string;
  contactName?: string;
  amount: number;
  probability: number;
  expectedCloseDate: string;
  daysInStage: number;
  activityCount: number;
  lastActivityDate?: string;
  isAtRisk: boolean;
  riskReason?: string;
}

export interface KanbanColumn {
  stage: OpportunityStage;
  stageLabel: string;
  cards: KanbanCard[];
  totalValue: number;
  count: number;
}

export interface PipelineSummary {
  totalValue: number;
  weightedValue: number;
  opportunityCount: number;
  avgDealSize: number;
  avgProbability: number;
}

export interface PipelineKanbanDTO {
  columns: KanbanColumn[];
  summary: PipelineSummary;
}

export interface ForecastPeriod {
  label: string;
  startDate: string;
  endDate: string;
  totalValue: number;
  weightedValue: number;
  opportunityCount: number;
  closedWonValue: number;
  closedWonCount: number;
}

export interface PipelineForecastDTO {
  currentQuarter: ForecastPeriod;
  nextQuarter: ForecastPeriod;
  pipelineHealth: 'HEALTHY' | 'AT_RISK' | 'CRITICAL';
  avgSalesCycle: number;
  winRate: number;
}

export interface StageTransitionRequest {
  newStage: OpportunityStage;
  lostReason?: string;
  notes?: string;
  createFollowUp?: boolean;
  followUpDays?: number;
}

// ==================== Email Sequences ====================

export interface EmailSequenceStep {
  id?: string;
  stepOrder: number;
  delayDays?: number;
  delayHours?: number;
  subject: string;
  bodyHtml: string;
  bodyText?: string;
  skipWeekends?: boolean;
  sendTimePreference?: string;
  active: boolean;
}

export interface EmailSequence {
  id?: string;
  tenantId: string;
  name: string;
  description?: string;
  type: SequenceType;
  active: boolean;
  fromEmail?: string;
  fromName?: string;
  replyToEmail?: string;
  trackOpens: boolean;
  trackClicks: boolean;
  includeUnsubscribeLink: boolean;
  steps: EmailSequenceStep[];
  createdAt?: string;
  updatedAt?: string;
}

export interface SequenceEnrollment {
  id: string;
  tenantId: string;
  sequenceId: string;
  sequenceName?: string;
  leadId?: string;
  contactId?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  currentStep: number;
  status: EnrollmentStatus;
  nextEmailAt?: string;
  lastEmailSentAt?: string;
  emailsSent: number;
  emailsOpened: number;
  emailsClicked: number;
  emailsBounced: number;
  createdAt: string;
  updatedAt: string;
}

export interface SequenceAnalytics {
  sequenceId: string;
  totalEnrollments: number;
  activeEnrollments: number;
  completedEnrollments: number;
  totalEmailsSent: number;
  emailsOpened: number;
  emailsClicked: number;
  emailsBounced: number;
  openRate: number;
  clickRate: number;
  bounceRate: number;
}

// ==================== Dashboard ====================

export interface SalesDashboardDTO {
  totalLeads: number;
  newLeadsThisMonth: number;
  qualifiedLeads: number;
  conversionRate: number;

  totalOpportunities: number;
  pipelineValue: number;
  weightedPipelineValue: number;
  avgDealSize: number;

  closedWonThisMonth: number;
  closedWonValue: number;
  closedLostThisMonth: number;
  winRate: number;

  avgSalesCycleInDays: number;
  activitiesThisWeek: number;
  overdueActivities: number;

  leadsBySource: Record<string, number>;
  opportunitiesByStage: Record<string, number>;
  revenueByMonth: Record<string, number>;
}

// ==================== API Response Types ====================

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface SalesFilters {
  status?: string;
  source?: string;
  stage?: string;
  assignedTo?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
}

// ==================== Lead Capture ====================

export interface LeadCaptureRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  company?: string;
  title?: string;
  source: LeadSource;
  notes?: string;
  roiCalculatorData?: Record<string, unknown>;
}

export interface LeadCaptureResponse {
  leadId: string;
  message: string;
  score: number;
}
