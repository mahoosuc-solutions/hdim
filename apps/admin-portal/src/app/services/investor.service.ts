import { Injectable, signal, computed } from '@angular/core';
import {
  InvestorTask,
  Contact,
  OutreachActivity,
  InvestorDocument,
  InvestorDashboardStats,
} from '../models/investor.model';

/**
 * Investor Launch Management Service
 *
 * Manages investor outreach tasks, contacts, and activities.
 * Uses Angular signals for reactive state management.
 *
 * Note: This is a client-side service with localStorage persistence.
 * For production, this would connect to a backend API.
 */
@Injectable({
  providedIn: 'root',
})
export class InvestorService {
  private readonly STORAGE_KEY = 'hdim_investor_data';

  // State signals
  private _tasks = signal<InvestorTask[]>(this.loadInitialTasks());
  private _contacts = signal<Contact[]>(this.loadContacts());
  private _activities = signal<OutreachActivity[]>(this.loadActivities());
  private _documents = signal<InvestorDocument[]>(this.loadDocuments());

  // Public read-only signals
  readonly tasks = this._tasks.asReadonly();
  readonly contacts = this._contacts.asReadonly();
  readonly activities = this._activities.asReadonly();
  readonly documents = this._documents.asReadonly();

  // Computed stats
  readonly stats = computed<InvestorDashboardStats>(() => {
    const tasks = this._tasks();
    const contacts = this._contacts();
    const activities = this._activities();

    return {
      tasksCompleted: tasks.filter((t) => t.status === 'completed').length,
      tasksTotal: tasks.length,
      tasksInProgress: tasks.filter((t) => t.status === 'in_progress').length,
      tasksPending: tasks.filter((t) => t.status === 'pending').length,
      contactsTotal: contacts.length,
      contactsReached: contacts.filter((c) => c.status !== 'not_contacted').length,
      meetingsScheduled: contacts.filter((c) => c.status === 'meeting_scheduled').length,
      linkedInRequestsSent: activities.filter((a) => a.type === 'linkedin_request').length,
      linkedInConnectionsAccepted: contacts.filter((c) => c.status === 'connected' || c.status === 'in_conversation').length,
      emailsSent: activities.filter((a) => a.type === 'email').length,
      responsesReceived: activities.filter((a) => a.status === 'responded').length,
      warmLeads: contacts.filter((c) => c.status === 'warm_lead').length,
    };
  });

  // Computed task groups
  readonly tasksByWeek = computed(() => {
    const tasks = this._tasks();
    return {
      week1: tasks.filter((t) => t.week === 1),
      week2: tasks.filter((t) => t.week === 2),
    };
  });

  readonly tasksByStatus = computed(() => {
    const tasks = this._tasks();
    return {
      pending: tasks.filter((t) => t.status === 'pending'),
      inProgress: tasks.filter((t) => t.status === 'in_progress'),
      completed: tasks.filter((t) => t.status === 'completed'),
      blocked: tasks.filter((t) => t.status === 'blocked'),
    };
  });

  // Contact groups
  readonly contactsByCategory = computed(() => {
    const contacts = this._contacts();
    return {
      qualityLeaders: contacts.filter((c) => c.category === 'quality_leader'),
      investors: contacts.filter((c) => c.category === 'investor'),
      angels: contacts.filter((c) => c.category === 'angel'),
      partners: contacts.filter((c) => c.category === 'partner'),
      customers: contacts.filter((c) => c.category === 'customer'),
    };
  });

  // Task operations
  updateTaskStatus(taskId: string, status: InvestorTask['status']): void {
    this._tasks.update((tasks) =>
      tasks.map((t) =>
        t.id === taskId
          ? {
              ...t,
              status,
              completedDate: status === 'completed' ? new Date().toISOString().split('T')[0] : t.completedDate,
            }
          : t
      )
    );
    this.persistData();
  }

  updateTask(taskId: string, updates: Partial<InvestorTask>): void {
    this._tasks.update((tasks) =>
      tasks.map((t) => (t.id === taskId ? { ...t, ...updates } : t))
    );
    this.persistData();
  }

  addTask(task: Omit<InvestorTask, 'id'>): void {
    const newTask: InvestorTask = {
      ...task,
      id: `task_${Date.now()}`,
    };
    this._tasks.update((tasks) => [...tasks, newTask]);
    this.persistData();
  }

  // Contact operations
  addContact(contact: Omit<Contact, 'id'>): void {
    const newContact: Contact = {
      ...contact,
      id: `contact_${Date.now()}`,
    };
    this._contacts.update((contacts) => [...contacts, newContact]);
    this.persistData();
  }

  updateContactStatus(contactId: string, status: Contact['status']): void {
    this._contacts.update((contacts) =>
      contacts.map((c) =>
        c.id === contactId
          ? { ...c, status, lastContactDate: new Date().toISOString().split('T')[0] }
          : c
      )
    );
    this.persistData();
  }

  updateContact(contactId: string, updates: Partial<Contact>): void {
    this._contacts.update((contacts) =>
      contacts.map((c) => (c.id === contactId ? { ...c, ...updates } : c))
    );
    this.persistData();
  }

  // Activity operations
  logActivity(activity: Omit<OutreachActivity, 'id'>): void {
    const newActivity: OutreachActivity = {
      ...activity,
      id: `activity_${Date.now()}`,
    };
    this._activities.update((activities) => [newActivity, ...activities]);
    this.persistData();
  }

  updateActivityStatus(activityId: string, status: OutreachActivity['status']): void {
    this._activities.update((activities) =>
      activities.map((a) => (a.id === activityId ? { ...a, status } : a))
    );
    this.persistData();
  }

  // Persistence
  private persistData(): void {
    const data = {
      tasks: this._tasks(),
      contacts: this._contacts(),
      activities: this._activities(),
      documents: this._documents(),
    };
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(data));
  }

  private loadFromStorage(): { tasks?: InvestorTask[]; contacts?: Contact[]; activities?: OutreachActivity[]; documents?: InvestorDocument[] } | null {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  private loadContacts(): Contact[] {
    const stored = this.loadFromStorage();
    return stored?.contacts || [];
  }

  private loadActivities(): OutreachActivity[] {
    const stored = this.loadFromStorage();
    return stored?.activities || [];
  }

  private loadDocuments(): InvestorDocument[] {
    // Static document list based on created files
    return [
      {
        id: 'doc_1',
        name: 'Financial Model (3-Year)',
        category: 'financial',
        filePath: 'docs/investor/financial-model.md',
        description: 'ARR projections $480K → $7.2M, SaaS metrics, cash flow',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_2',
        name: 'Outreach Templates',
        category: 'outreach',
        filePath: 'docs/investor/outreach-templates.md',
        description: 'LinkedIn, email, warm intro, discovery call templates',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_3',
        name: 'Customer Target List (50)',
        category: 'target_list',
        filePath: 'docs/investor/customer-target-list.md',
        description: '50 health systems and ACOs in 3 tiers',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_4',
        name: 'Investor Target List (50)',
        category: 'target_list',
        filePath: 'docs/investor/investor-target-list.md',
        description: '50 healthcare VCs with intro paths',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_5',
        name: 'Cap Table Template',
        category: 'financial',
        filePath: 'docs/investor/cap-table.md',
        description: 'Pre/post Series A ownership modeling',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_6',
        name: 'LinkedIn Posts (4)',
        category: 'content',
        filePath: 'docs/investor/linkedin-posts.md',
        description: '4 thought leadership articles ready to post',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_7',
        name: 'Quality Leader Profiles (100)',
        category: 'target_list',
        filePath: 'docs/investor/quality-leader-profiles.md',
        description: '100 LinkedIn contacts at 25 target organizations',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_8',
        name: 'Angel Outreach List (15)',
        category: 'target_list',
        filePath: 'docs/investor/angel-outreach-list.md',
        description: '15 healthcare angels with personalized angles',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_9',
        name: 'Monthly Update Template',
        category: 'outreach',
        filePath: 'docs/investor/monthly-update-template.md',
        description: 'Investor email update format',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_10',
        name: 'Healthcare Conferences',
        category: 'application',
        filePath: 'docs/investor/healthcare-conferences.md',
        description: 'HIMSS 2026, AMGA, regional events',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_11',
        name: 'Accelerator Applications',
        category: 'application',
        filePath: 'docs/investor/accelerator-applications.md',
        description: 'YC, Techstars, Rock Health application guide',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_12',
        name: 'Task Tracker',
        category: 'outreach',
        filePath: 'docs/investor/task-tracker.md',
        description: 'Master task list with status',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_13',
        name: 'Healthtech Partnership Outreach',
        category: 'outreach',
        filePath: 'docs/investor/healthtech-partnership-outreach.md',
        description: 'Verato, Healthwise, Rhapsody partnership materials',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
      {
        id: 'doc_14',
        name: 'Vercel Deployment Alignment',
        category: 'outreach',
        filePath: 'docs/VERCEL_DEPLOYMENT_ALIGNMENT.md',
        description: 'All 4 Vercel projects documented',
        createdDate: '2026-02-02',
        lastUpdated: '2026-02-02',
      },
    ];
  }

  private loadInitialTasks(): InvestorTask[] {
    const stored = this.loadFromStorage();
    if (stored?.tasks?.length) {
      return stored.tasks;
    }

    // Initial tasks from task-tracker.md
    return [
      // Week 1 - Completed
      { id: '1', taskNumber: 1, subject: 'Create 3-year financial model', description: 'ARR projections, SaaS metrics, cash flow', status: 'completed', category: 'investor', week: 1, deliverable: 'docs/investor/financial-model.md' },
      { id: '2', taskNumber: 2, subject: 'Verify demo stack runs smoothly', description: 'Demo verification, nginx fix', status: 'completed', category: 'content', week: 1 },
      { id: '3', taskNumber: 3, subject: 'Draft 4 LinkedIn posts', description: 'Thought leadership content', status: 'completed', category: 'content', week: 1, deliverable: 'docs/investor/linkedin-posts.md' },
      { id: '4', taskNumber: 4, subject: 'Request 5 warm intros', description: 'Use outreach-templates.md', status: 'in_progress', category: 'manual', week: 1, blockedBy: ['5'] },
      { id: '5', taskNumber: 5, subject: 'Search 2nd-degree VC connections', description: 'LinkedIn search for mutual connections', status: 'in_progress', category: 'manual', week: 1 },
      { id: '6', taskNumber: 6, subject: 'Build investor spreadsheet (50 VCs)', description: 'Healthcare VCs with intro paths', status: 'completed', category: 'investor', week: 1, deliverable: 'docs/investor/investor-target-list.md' },
      { id: '7', taskNumber: 7, subject: 'Send first 20 LinkedIn requests', description: 'Quality leaders #1-20', status: 'in_progress', category: 'manual', week: 1 },
      { id: '8', taskNumber: 8, subject: 'Draft customer outreach templates', description: 'Cold/warm email templates', status: 'completed', category: 'customer', week: 1, deliverable: 'docs/investor/outreach-templates.md' },
      { id: '9', taskNumber: 9, subject: 'Find 100 quality leader profiles', description: 'LinkedIn contacts at target orgs', status: 'completed', category: 'customer', week: 1, deliverable: 'docs/investor/quality-leader-profiles.md' },
      { id: '10', taskNumber: 10, subject: 'Build customer target list (50)', description: 'Health systems and ACOs', status: 'completed', category: 'customer', week: 1, deliverable: 'docs/investor/customer-target-list.md' },
      { id: '11', taskNumber: 11, subject: 'Clean up cap table document', description: 'Ownership tracking template', status: 'completed', category: 'investor', week: 1, deliverable: 'docs/investor/cap-table.md' },

      // Week 2 - Completed (Claude)
      { id: '12', taskNumber: 12, subject: 'Create angel outreach list', description: '15 angels with personalized angles', status: 'completed', category: 'investor', week: 2, deliverable: 'docs/investor/angel-outreach-list.md' },
      { id: '13', taskNumber: 13, subject: 'Create monthly investor update template', description: 'Email template with metrics', status: 'completed', category: 'investor', week: 2, deliverable: 'docs/investor/monthly-update-template.md' },
      { id: '14', taskNumber: 14, subject: 'Research healthcare conferences', description: 'Events in next 60 days', status: 'completed', category: 'application', week: 2, deliverable: 'docs/investor/healthcare-conferences.md' },
      { id: '15', taskNumber: 15, subject: 'Create accelerator application list', description: 'YC, Techstars, Rock Health', status: 'completed', category: 'application', week: 2, deliverable: 'docs/investor/accelerator-applications.md' },

      // Week 2 - Pending (Manual)
      { id: '16', taskNumber: 16, subject: 'Identify healthcare conferences to attend', description: 'Register for HIMSS', status: 'pending', category: 'manual', week: 2 },
      { id: '17', taskNumber: 17, subject: 'Start Y Combinator S26 application', description: 'Begin at ycombinator.com/apply', status: 'pending', category: 'application', week: 2 },
      { id: '18', taskNumber: 18, subject: 'Request 5 more warm intros to VCs', description: 'Target Tier 1 VCs', status: 'pending', category: 'manual', week: 2, blockedBy: ['5'] },
      { id: '19', taskNumber: 19, subject: 'Send 5 advice request emails to angels', description: 'First 5 from angel list', status: 'pending', category: 'manual', week: 2 },
      { id: '20', taskNumber: 20, subject: 'Publish first LinkedIn post', description: 'Post #1 from linkedin-posts.md', status: 'pending', category: 'content', week: 2 },
      { id: '21', taskNumber: 21, subject: 'Book 5+ discovery calls', description: 'Convert conversations to calls', status: 'pending', category: 'manual', week: 2, blockedBy: ['22'] },
      { id: '22', taskNumber: 22, subject: 'Follow up on Week 1 connections', description: 'Message accepted connections', status: 'pending', category: 'manual', week: 2, blockedBy: ['7'] },
      { id: '23', taskNumber: 23, subject: 'Send 30 more LinkedIn outreach messages', description: 'Quality leaders #21-50', status: 'pending', category: 'manual', week: 2 },
    ];
  }

  // Import contacts from quality-leader-profiles.md format
  importQualityLeaders(data: string): void {
    // Parse markdown table format and add contacts
    // This would parse the markdown and create Contact objects
    console.log('Import quality leaders:', data.length, 'characters');
  }

  // Export for backup
  exportData(): string {
    return JSON.stringify(
      {
        tasks: this._tasks(),
        contacts: this._contacts(),
        activities: this._activities(),
        exportDate: new Date().toISOString(),
      },
      null,
      2
    );
  }
}
