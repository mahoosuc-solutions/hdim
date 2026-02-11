/**
 * Investor Launch Management Models
 *
 * Data structures for tracking investor outreach, contacts, and tasks.
 */

export interface InvestorTask {
  id: string;
  taskNumber: number;
  subject: string;
  description: string;
  status: 'pending' | 'in_progress' | 'completed' | 'blocked';
  category: 'investor' | 'customer' | 'content' | 'application' | 'manual';
  week: number;
  blockedBy?: string[];
  deliverable?: string;
  dueDate?: string;
  completedDate?: string;
  assignee?: string;
  notes?: string;
}

export interface Contact {
  id: string;
  name: string;
  title: string;
  organization: string;
  linkedInUrl?: string;
  email?: string;
  phone?: string;
  category: 'quality_leader' | 'investor' | 'angel' | 'partner' | 'customer';
  tier?: 1 | 2 | 3;
  status: 'not_contacted' | 'connection_sent' | 'connected' | 'in_conversation' | 'meeting_scheduled' | 'warm_lead' | 'cold';
  lastContactDate?: string;
  nextFollowUpDate?: string;
  notes?: string;
  tags?: string[];
}

export interface OutreachActivity {
  id: string;
  contactId: string;
  contactName: string;
  type: 'linkedin_request' | 'linkedin_message' | 'email' | 'call' | 'meeting' | 'follow_up';
  status: 'sent' | 'responded' | 'no_response' | 'scheduled' | 'completed';
  date: string;
  subject?: string;
  notes?: string;
}

export interface InvestorDocument {
  id: string;
  name: string;
  category: 'financial' | 'outreach' | 'target_list' | 'content' | 'application';
  filePath: string;
  description: string;
  createdDate: string;
  lastUpdated: string;
}

export interface InvestorDashboardStats {
  tasksCompleted: number;
  tasksTotal: number;
  tasksInProgress: number;
  tasksPending: number;
  contactsTotal: number;
  contactsReached: number;
  meetingsScheduled: number;
  linkedInRequestsSent: number;
  linkedInConnectionsAccepted: number;
  emailsSent: number;
  responsesReceived: number;
  warmLeads: number;
}

export interface WeeklyProgress {
  week: number;
  startDate: string;
  endDate: string;
  tasksPlanned: number;
  tasksCompleted: number;
  outreachSent: number;
  responsesReceived: number;
  meetingsBooked: number;
}
