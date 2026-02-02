import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
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
 * Production mode: HTTP API calls to investor-dashboard-service
 * Development mode: Falls back to localStorage if API unavailable
 */
@Injectable({
  providedIn: 'root',
})
export class InvestorService {
  private readonly http = inject(HttpClient);
  private readonly STORAGE_KEY = 'hdim_investor_data';

  // API base URL - uses relative path for Vercel deployment (same origin)
  // Falls back to localhost for local development with separate API server
  private readonly apiBaseUrl = environment.production
    ? '' // Empty string for same-origin /api/* calls on Vercel
    : 'http://localhost:3000'; // Vercel dev server

  // State signals
  private _tasks = signal<InvestorTask[]>([]);
  private _contacts = signal<Contact[]>([]);
  private _activities = signal<OutreachActivity[]>([]);
  private _documents = signal<InvestorDocument[]>(this.getStaticDocuments());
  private _isLoading = signal<boolean>(false);
  private _error = signal<string | null>(null);
  private _useLocalStorage = signal<boolean>(false);

  // Public read-only signals
  readonly tasks = this._tasks.asReadonly();
  readonly contacts = this._contacts.asReadonly();
  readonly activities = this._activities.asReadonly();
  readonly documents = this._documents.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

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
      week3: tasks.filter((t) => t.week === 3),
      week4: tasks.filter((t) => t.week === 4),
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

  constructor() {
    // Load initial data
    this.loadAllData();
  }

  // ==========================================
  // Data Loading
  // ==========================================

  loadAllData(): void {
    this._isLoading.set(true);
    this._error.set(null);

    // Try to load from API first
    this.loadTasks();
    this.loadContacts();
    this.loadActivities();
  }

  private loadTasks(): void {
    this.http.get<InvestorTask[]>(`${this.apiBaseUrl}/api/tasks`).pipe(
      tap((tasks) => {
        this._tasks.set(this.mapApiTasksToModel(tasks));
        this._useLocalStorage.set(false);
      }),
      catchError((error) => {
        console.warn('API unavailable, falling back to localStorage:', error.message);
        this._useLocalStorage.set(true);
        this._tasks.set(this.loadInitialTasks());
        return of([]);
      })
    ).subscribe({
      complete: () => this._isLoading.set(false),
    });
  }

  private loadContacts(): void {
    this.http.get<Contact[]>(`${this.apiBaseUrl}/api/contacts`).pipe(
      tap((contacts) => {
        this._contacts.set(this.mapApiContactsToModel(contacts));
      }),
      catchError(() => {
        this._contacts.set(this.loadContactsFromStorage());
        return of([]);
      })
    ).subscribe();
  }

  private loadActivities(): void {
    this.http.get<OutreachActivity[]>(`${this.apiBaseUrl}/api/activities`).pipe(
      tap((activities) => {
        this._activities.set(this.mapApiActivitiesToModel(activities));
      }),
      catchError(() => {
        this._activities.set(this.loadActivitiesFromStorage());
        return of([]);
      })
    ).subscribe();
  }

  // ==========================================
  // Task Operations
  // ==========================================

  updateTaskStatus(taskId: string, status: InvestorTask['status']): void {
    if (this._useLocalStorage()) {
      this.updateTaskLocal(taskId, { status });
      return;
    }

    this.http.patch<InvestorTask>(`${this.apiBaseUrl}/api/tasks/${taskId}/status`, null, {
      params: { status }
    }).pipe(
      tap((updated) => {
        this._tasks.update((tasks) =>
          tasks.map((t) => (t.id === taskId ? { ...t, status, completedDate: status === 'completed' ? new Date().toISOString().split('T')[0] : t.completedDate } : t))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update task status');
        return throwError(() => error);
      })
    ).subscribe();
  }

  updateTask(taskId: string, updates: Partial<InvestorTask>): void {
    if (this._useLocalStorage()) {
      this.updateTaskLocal(taskId, updates);
      return;
    }

    this.http.put<InvestorTask>(`${this.apiBaseUrl}/api/tasks/${taskId}`, updates).pipe(
      tap((updated) => {
        this._tasks.update((tasks) =>
          tasks.map((t) => (t.id === taskId ? { ...t, ...updates } : t))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update task');
        return throwError(() => error);
      })
    ).subscribe();
  }

  addTask(task: Omit<InvestorTask, 'id'>): void {
    if (this._useLocalStorage()) {
      this.addTaskLocal(task);
      return;
    }

    this.http.post<InvestorTask>(`${this.apiBaseUrl}/api/tasks`, {
      subject: task.subject,
      description: task.description,
      category: task.category,
      week: task.week,
      deliverable: task.deliverable,
      owner: task.assignee,
      dueDate: task.dueDate,
      notes: task.notes,
    }).pipe(
      tap((created) => {
        const newTask = this.mapApiTaskToModel(created);
        this._tasks.update((tasks) => [...tasks, newTask]);
      }),
      catchError((error) => {
        this._error.set('Failed to create task');
        return throwError(() => error);
      })
    ).subscribe();
  }

  deleteTask(taskId: string): void {
    if (this._useLocalStorage()) {
      this._tasks.update((tasks) => tasks.filter((t) => t.id !== taskId));
      this.persistData();
      return;
    }

    this.http.delete(`${this.apiBaseUrl}/api/tasks/${taskId}`).pipe(
      tap(() => {
        this._tasks.update((tasks) => tasks.filter((t) => t.id !== taskId));
      }),
      catchError((error) => {
        this._error.set('Failed to delete task');
        return throwError(() => error);
      })
    ).subscribe();
  }

  // ==========================================
  // Contact Operations
  // ==========================================

  addContact(contact: Omit<Contact, 'id'>): void {
    if (this._useLocalStorage()) {
      this.addContactLocal(contact);
      return;
    }

    this.http.post<Contact>(`${this.apiBaseUrl}/api/contacts`, {
      name: contact.name,
      title: contact.title,
      organization: contact.organization,
      email: contact.email,
      phone: contact.phone,
      linkedInUrl: contact.linkedInUrl,
      category: this.mapCategoryToApi(contact.category),
      tier: contact.tier?.toString() || 'B',
      notes: contact.notes,
    }).pipe(
      tap((created) => {
        const newContact = this.mapApiContactToModel(created);
        this._contacts.update((contacts) => [...contacts, newContact]);
      }),
      catchError((error) => {
        this._error.set('Failed to create contact');
        return throwError(() => error);
      })
    ).subscribe();
  }

  updateContactStatus(contactId: string, status: Contact['status']): void {
    if (this._useLocalStorage()) {
      this._contacts.update((contacts) =>
        contacts.map((c) =>
          c.id === contactId
            ? { ...c, status, lastContactDate: new Date().toISOString().split('T')[0] }
            : c
        )
      );
      this.persistData();
      return;
    }

    this.http.patch<Contact>(`${this.apiBaseUrl}/api/contacts/${contactId}/status`, null, {
      params: { status: this.mapStatusToApi(status) }
    }).pipe(
      tap(() => {
        this._contacts.update((contacts) =>
          contacts.map((c) =>
            c.id === contactId
              ? { ...c, status, lastContactDate: new Date().toISOString().split('T')[0] }
              : c
          )
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update contact status');
        return throwError(() => error);
      })
    ).subscribe();
  }

  updateContact(contactId: string, updates: Partial<Contact>): void {
    if (this._useLocalStorage()) {
      this._contacts.update((contacts) =>
        contacts.map((c) => (c.id === contactId ? { ...c, ...updates } : c))
      );
      this.persistData();
      return;
    }

    this.http.put<Contact>(`${this.apiBaseUrl}/api/contacts/${contactId}`, updates).pipe(
      tap(() => {
        this._contacts.update((contacts) =>
          contacts.map((c) => (c.id === contactId ? { ...c, ...updates } : c))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update contact');
        return throwError(() => error);
      })
    ).subscribe();
  }

  deleteContact(contactId: string): void {
    if (this._useLocalStorage()) {
      this._contacts.update((contacts) => contacts.filter((c) => c.id !== contactId));
      this.persistData();
      return;
    }

    this.http.delete(`${this.apiBaseUrl}/api/contacts/${contactId}`).pipe(
      tap(() => {
        this._contacts.update((contacts) => contacts.filter((c) => c.id !== contactId));
      }),
      catchError((error) => {
        this._error.set('Failed to delete contact');
        return throwError(() => error);
      })
    ).subscribe();
  }

  // ==========================================
  // Activity Operations
  // ==========================================

  logActivity(activity: Omit<OutreachActivity, 'id'>): void {
    if (this._useLocalStorage()) {
      this.logActivityLocal(activity);
      return;
    }

    this.http.post<OutreachActivity>(`${this.apiBaseUrl}/api/activities`, {
      contactId: activity.contactId,
      activityType: this.mapActivityTypeToApi(activity.type),
      subject: activity.subject,
      content: activity.notes,
      activityDate: activity.date,
      notes: activity.notes,
    }).pipe(
      tap((created) => {
        const newActivity = this.mapApiActivityToModel(created);
        this._activities.update((activities) => [newActivity, ...activities]);
      }),
      catchError((error) => {
        this._error.set('Failed to log activity');
        return throwError(() => error);
      })
    ).subscribe();
  }

  updateActivityStatus(activityId: string, status: OutreachActivity['status']): void {
    if (this._useLocalStorage()) {
      this._activities.update((activities) =>
        activities.map((a) => (a.id === activityId ? { ...a, status } : a))
      );
      this.persistData();
      return;
    }

    this.http.put<OutreachActivity>(`${this.apiBaseUrl}/api/activities/${activityId}`, { status }).pipe(
      tap(() => {
        this._activities.update((activities) =>
          activities.map((a) => (a.id === activityId ? { ...a, status } : a))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to update activity status');
        return throwError(() => error);
      })
    ).subscribe();
  }

  // ==========================================
  // Search
  // ==========================================

  searchTasks(query: string): Observable<InvestorTask[]> {
    if (this._useLocalStorage()) {
      const tasks = this._tasks().filter(
        (t) =>
          t.subject.toLowerCase().includes(query.toLowerCase()) ||
          t.description?.toLowerCase().includes(query.toLowerCase())
      );
      return of(tasks);
    }

    return this.http.get<InvestorTask[]>(`${this.apiBaseUrl}/api/tasks/search`, {
      params: { query }
    }).pipe(
      map((tasks) => this.mapApiTasksToModel(tasks)),
      catchError(() => of([]))
    );
  }

  searchContacts(query: string): Observable<Contact[]> {
    if (this._useLocalStorage()) {
      const contacts = this._contacts().filter(
        (c) =>
          c.name.toLowerCase().includes(query.toLowerCase()) ||
          c.organization?.toLowerCase().includes(query.toLowerCase())
      );
      return of(contacts);
    }

    return this.http.get<Contact[]>(`${this.apiBaseUrl}/api/contacts/search`, {
      params: { query }
    }).pipe(
      map((contacts) => this.mapApiContactsToModel(contacts)),
      catchError(() => of([]))
    );
  }

  // ==========================================
  // Export/Import
  // ==========================================

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

  // ==========================================
  // Private Local Storage Methods
  // ==========================================

  private updateTaskLocal(taskId: string, updates: Partial<InvestorTask>): void {
    this._tasks.update((tasks) =>
      tasks.map((t) =>
        t.id === taskId
          ? {
              ...t,
              ...updates,
              completedDate: updates.status === 'completed' ? new Date().toISOString().split('T')[0] : t.completedDate,
            }
          : t
      )
    );
    this.persistData();
  }

  private addTaskLocal(task: Omit<InvestorTask, 'id'>): void {
    const newTask: InvestorTask = {
      ...task,
      id: `task_${Date.now()}`,
    } as InvestorTask;
    this._tasks.update((tasks) => [...tasks, newTask]);
    this.persistData();
  }

  private addContactLocal(contact: Omit<Contact, 'id'>): void {
    const newContact: Contact = {
      ...contact,
      id: `contact_${Date.now()}`,
    };
    this._contacts.update((contacts) => [...contacts, newContact]);
    this.persistData();
  }

  private logActivityLocal(activity: Omit<OutreachActivity, 'id'>): void {
    const newActivity: OutreachActivity = {
      ...activity,
      id: `activity_${Date.now()}`,
    };
    this._activities.update((activities) => [newActivity, ...activities]);
    this.persistData();
  }

  private persistData(): void {
    const data = {
      tasks: this._tasks(),
      contacts: this._contacts(),
      activities: this._activities(),
      documents: this._documents(),
    };
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(data));
  }

  private loadFromStorage(): { tasks?: InvestorTask[]; contacts?: Contact[]; activities?: OutreachActivity[] } | null {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  private loadContactsFromStorage(): Contact[] {
    const stored = this.loadFromStorage();
    return stored?.contacts || [];
  }

  private loadActivitiesFromStorage(): OutreachActivity[] {
    const stored = this.loadFromStorage();
    return stored?.activities || [];
  }

  // ==========================================
  // API Data Mapping
  // ==========================================

  private mapApiTasksToModel(apiTasks: any[]): InvestorTask[] {
    return apiTasks.map((t) => this.mapApiTaskToModel(t));
  }

  private mapApiTaskToModel(t: any): InvestorTask {
    return {
      id: t.id,
      taskNumber: t.sortOrder || 0,
      subject: t.subject,
      description: t.description || '',
      status: t.status as InvestorTask['status'],
      category: this.mapApiCategoryToModel(t.category),
      week: t.week,
      deliverable: t.deliverable,
      dueDate: t.dueDate,
      completedDate: t.completedAt ? new Date(t.completedAt).toISOString().split('T')[0] : undefined,
      assignee: t.owner,
      notes: t.notes,
    };
  }

  private mapApiContactsToModel(apiContacts: any[]): Contact[] {
    return apiContacts.map((c) => this.mapApiContactToModel(c));
  }

  private mapApiContactToModel(c: any): Contact {
    return {
      id: c.id,
      name: c.name,
      title: c.title || '',
      organization: c.organization || '',
      email: c.email,
      phone: c.phone,
      linkedInUrl: c.linkedInUrl,
      category: this.mapApiCategoryToContactModel(c.category),
      tier: this.mapApiTierToModel(c.tier),
      status: this.mapApiStatusToModel(c.status),
      lastContactDate: c.lastContacted ? new Date(c.lastContacted).toISOString().split('T')[0] : undefined,
      nextFollowUpDate: c.nextFollowUp ? new Date(c.nextFollowUp).toISOString().split('T')[0] : undefined,
      notes: c.notes,
    };
  }

  private mapApiActivitiesToModel(apiActivities: any[]): OutreachActivity[] {
    return apiActivities.map((a) => this.mapApiActivityToModel(a));
  }

  private mapApiActivityToModel(a: any): OutreachActivity {
    return {
      id: a.id,
      contactId: a.contactId,
      contactName: a.contactName || '',
      type: this.mapApiActivityTypeToModel(a.activityType),
      status: this.mapApiActivityStatusToModel(a.status),
      date: a.activityDate,
      subject: a.subject,
      notes: a.notes || a.content,
    };
  }

  private mapCategoryToApi(category: Contact['category']): string {
    const mapping: Record<string, string> = {
      quality_leader: 'VC',
      investor: 'VC',
      angel: 'Angel',
      partner: 'Strategic',
      customer: 'Partner',
    };
    return mapping[category] || 'VC';
  }

  private mapApiCategoryToModel(category: string): InvestorTask['category'] {
    const mapping: Record<string, InvestorTask['category']> = {
      Legal: 'investor',
      Financial: 'investor',
      Technical: 'content',
      Marketing: 'customer',
      Governance: 'investor',
      Admin: 'manual',
    };
    return mapping[category] || 'manual';
  }

  private mapApiCategoryToContactModel(category: string): Contact['category'] {
    const mapping: Record<string, Contact['category']> = {
      VC: 'investor',
      Angel: 'angel',
      Strategic: 'partner',
      Advisor: 'partner',
      Partner: 'customer',
    };
    return mapping[category] || 'quality_leader';
  }

  private mapApiTierToModel(tier: string): 1 | 2 | 3 | undefined {
    if (tier === 'A') return 1;
    if (tier === 'B') return 2;
    if (tier === 'C') return 3;
    return 2;
  }

  private mapStatusToApi(status: Contact['status']): string {
    const mapping: Record<string, string> = {
      not_contacted: 'identified',
      connection_sent: 'contacted',
      connected: 'engaged',
      in_conversation: 'engaged',
      meeting_scheduled: 'meeting_scheduled',
      warm_lead: 'follow_up',
      cold: 'declined',
    };
    return mapping[status] || 'identified';
  }

  private mapApiStatusToModel(status: string): Contact['status'] {
    const mapping: Record<string, Contact['status']> = {
      identified: 'not_contacted',
      contacted: 'connection_sent',
      engaged: 'connected',
      meeting_scheduled: 'meeting_scheduled',
      follow_up: 'warm_lead',
      declined: 'cold',
      invested: 'warm_lead',
    };
    return mapping[status] || 'not_contacted';
  }

  private mapActivityTypeToApi(type: OutreachActivity['type']): string {
    const mapping: Record<string, string> = {
      linkedin_request: 'linkedin_connect',
      linkedin_message: 'linkedin_message',
      email: 'email',
      call: 'call',
      meeting: 'meeting',
      follow_up: 'intro_request',
    };
    return mapping[type] || 'email';
  }

  private mapApiActivityTypeToModel(type: string): OutreachActivity['type'] {
    const mapping: Record<string, OutreachActivity['type']> = {
      linkedin_connect: 'linkedin_request',
      linkedin_message: 'linkedin_message',
      email: 'email',
      call: 'call',
      meeting: 'meeting',
      intro_request: 'follow_up',
    };
    return mapping[type] || 'email';
  }

  private mapApiActivityStatusToModel(status: string): OutreachActivity['status'] {
    const mapping: Record<string, OutreachActivity['status']> = {
      pending: 'sent',
      sent: 'sent',
      responded: 'responded',
      completed: 'completed',
      no_response: 'no_response',
    };
    return mapping[status] || 'sent';
  }

  // ==========================================
  // Static Data
  // ==========================================

  private getStaticDocuments(): InvestorDocument[] {
    return [
      { id: 'doc_1', name: 'Financial Model (3-Year)', category: 'financial', filePath: 'docs/investor/financial-model.md', description: 'ARR projections $480K → $7.2M, SaaS metrics, cash flow', createdDate: '2026-02-02', lastUpdated: '2026-02-02' },
      { id: 'doc_2', name: 'Outreach Templates', category: 'outreach', filePath: 'docs/investor/outreach-templates.md', description: 'LinkedIn, email, warm intro, discovery call templates', createdDate: '2026-02-02', lastUpdated: '2026-02-02' },
      { id: 'doc_3', name: 'Customer Target List (50)', category: 'target_list', filePath: 'docs/investor/customer-target-list.md', description: '50 health systems and ACOs in 3 tiers', createdDate: '2026-02-02', lastUpdated: '2026-02-02' },
      { id: 'doc_4', name: 'Investor Target List (50)', category: 'target_list', filePath: 'docs/investor/investor-target-list.md', description: '50 healthcare VCs with intro paths', createdDate: '2026-02-02', lastUpdated: '2026-02-02' },
      { id: 'doc_5', name: 'Cap Table Template', category: 'financial', filePath: 'docs/investor/cap-table.md', description: 'Pre/post Series A ownership modeling', createdDate: '2026-02-02', lastUpdated: '2026-02-02' },
    ];
  }

  private loadInitialTasks(): InvestorTask[] {
    const stored = this.loadFromStorage();
    if (stored?.tasks?.length) {
      return stored.tasks;
    }

    // Return default tasks (same as before)
    return [
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
      { id: '12', taskNumber: 12, subject: 'Create angel outreach list', description: '15 angels with personalized angles', status: 'completed', category: 'investor', week: 2, deliverable: 'docs/investor/angel-outreach-list.md' },
      { id: '13', taskNumber: 13, subject: 'Create monthly investor update template', description: 'Email template with metrics', status: 'completed', category: 'investor', week: 2, deliverable: 'docs/investor/monthly-update-template.md' },
      { id: '14', taskNumber: 14, subject: 'Research healthcare conferences', description: 'Events in next 60 days', status: 'completed', category: 'application', week: 2, deliverable: 'docs/investor/healthcare-conferences.md' },
      { id: '15', taskNumber: 15, subject: 'Create accelerator application list', description: 'YC, Techstars, Rock Health', status: 'completed', category: 'application', week: 2, deliverable: 'docs/investor/accelerator-applications.md' },
      { id: '16', taskNumber: 16, subject: 'Identify healthcare conferences to attend', description: 'Register for HIMSS', status: 'pending', category: 'manual', week: 2 },
      { id: '17', taskNumber: 17, subject: 'Start Y Combinator S26 application', description: 'Begin at ycombinator.com/apply', status: 'pending', category: 'application', week: 2 },
    ];
  }
}
