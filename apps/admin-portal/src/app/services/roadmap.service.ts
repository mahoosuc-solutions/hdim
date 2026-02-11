import { Injectable, signal, computed, inject } from '@angular/core';
import { LoggerService, ContextualLogger } from './logger.service';
import {
  RoadmapMilestone,
  RoadmapStats,
  RoadmapFilter,
  MilestoneStatus,
  MilestoneCategory,
  Quarter,
  CategoryStats,
  QuarterStats,
} from '../models/roadmap.model';

/**
 * Roadmap Service
 *
 * Manages product roadmap data using localStorage with future API extensibility.
 * Uses Angular 17+ signals for reactive state management.
 *
 * Features:
 * - CRUD operations for milestones
 * - Real-time statistics via computed signals
 * - Filtering by category, status, quarter, and search term
 * - localStorage persistence with automatic seeding
 * - Future-ready API integration pattern
 *
 * @example
 * ```typescript
 * // In a component
 * export class RoadmapComponent {
 *   private roadmapService = inject(RoadmapService);
 *
 *   milestones = this.roadmapService.milestones;
 *   stats = this.roadmapService.stats;
 *
 *   updateStatus(id: string, status: MilestoneStatus) {
 *     this.roadmapService.updateMilestoneStatus(id, status);
 *   }
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class RoadmapService {
  private readonly STORAGE_KEY = 'hdim_roadmap_milestones';
  private readonly logger: ContextualLogger;

  // ==========================================
  // State Signals
  // ==========================================

  private _milestones = signal<RoadmapMilestone[]>([]);
  private _loading = signal<boolean>(false);
  private _error = signal<string | null>(null);

  // ==========================================
  // Public Readonly Signals
  // ==========================================

  readonly milestones = this._milestones.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  // ==========================================
  // Computed Signals
  // ==========================================

  /**
   * Computed statistics for the entire roadmap
   */
  readonly stats = computed<RoadmapStats>(() => this.calculateStats());

  /**
   * Milestones grouped by quarter
   */
  readonly milestonesByQuarter = computed(() => {
    const milestones = this._milestones();
    return {
      'Q1 2026': milestones.filter((m) => m.quarter === 'Q1 2026'),
      'Q2 2026': milestones.filter((m) => m.quarter === 'Q2 2026'),
      'Q3 2026': milestones.filter((m) => m.quarter === 'Q3 2026'),
      'Q4 2026': milestones.filter((m) => m.quarter === 'Q4 2026'),
    };
  });

  /**
   * Milestones grouped by category
   */
  readonly milestonesByCategory = computed(() => {
    const milestones = this._milestones();
    return {
      gtm: milestones.filter((m) => m.category === 'gtm'),
      product: milestones.filter((m) => m.category === 'product'),
      infrastructure: milestones.filter((m) => m.category === 'infrastructure'),
      compliance: milestones.filter((m) => m.category === 'compliance'),
    };
  });

  /**
   * Milestones grouped by status
   */
  readonly milestonesByStatus = computed(() => {
    const milestones = this._milestones();
    return {
      planned: milestones.filter((m) => m.status === 'planned'),
      in_progress: milestones.filter((m) => m.status === 'in_progress'),
      completed: milestones.filter((m) => m.status === 'completed'),
      blocked: milestones.filter((m) => m.status === 'blocked'),
    };
  });

  constructor() {
    const loggerService = inject(LoggerService);
    this.logger = loggerService.withContext('RoadmapService');
    this.loadFromStorage();
  }

  // ==========================================
  // Storage Operations
  // ==========================================

  /**
   * Load milestones from localStorage, seeding if empty
   */
  private loadFromStorage(): void {
    this._loading.set(true);
    this._error.set(null);

    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const milestones = JSON.parse(stored) as RoadmapMilestone[];
        this._milestones.set(milestones);
        this.logger.debug('Loaded milestones from storage', { count: milestones.length });
      } else {
        this.logger.info('No stored milestones found, seeding initial data');
        this.seedInitialData();
      }
    } catch (error) {
      this.logger.error('Failed to load milestones from storage', error);
      this._error.set('Failed to load roadmap data');
      this.seedInitialData();
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Save milestones to localStorage
   */
  private saveToStorage(): void {
    try {
      const milestones = this._milestones();
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(milestones));
      this.logger.debug('Saved milestones to storage', { count: milestones.length });
    } catch (error) {
      this.logger.error('Failed to save milestones to storage', error);
      this._error.set('Failed to save roadmap data');
    }
  }

  /**
   * Seed initial milestone data for Q1-Q4 2026
   */
  private seedInitialData(): void {
    const initialMilestones: RoadmapMilestone[] = [
      // ==========================================
      // Q1 2026 - GTM Focus (Current)
      // ==========================================
      {
        id: 'ms_q1_001',
        name: 'Landing page deployed',
        description: 'Production-ready landing page with investor/customer CTAs',
        status: 'completed',
        category: 'gtm',
        quarter: 'Q1 2026',
        startDate: '2026-01-01',
        targetDate: '2026-01-15',
        completedDate: '2026-01-14',
        completionPercent: 100,
        owner: 'Engineering',
        tags: ['website', 'marketing'],
      },
      {
        id: 'ms_q1_002',
        name: 'Sales automation backend',
        description: 'Full CRM backend with leads, opportunities, and pipeline management',
        status: 'completed',
        category: 'product',
        quarter: 'Q1 2026',
        startDate: '2026-01-01',
        targetDate: '2026-01-31',
        completedDate: '2026-01-28',
        completionPercent: 100,
        owner: 'Engineering',
        tags: ['backend', 'crm'],
      },
      {
        id: 'ms_q1_003',
        name: 'Investor dashboard backend',
        description: 'Task management, contact tracking, and activity logging for investor outreach',
        status: 'completed',
        category: 'product',
        quarter: 'Q1 2026',
        startDate: '2026-01-15',
        targetDate: '2026-02-01',
        completedDate: '2026-02-02',
        completionPercent: 100,
        owner: 'Engineering',
        tags: ['backend', 'investor-relations'],
      },
      {
        id: 'ms_q1_004',
        name: 'Investor outreach launch',
        description: 'Begin systematic outreach to healthcare VCs and angels',
        status: 'in_progress',
        category: 'gtm',
        quarter: 'Q1 2026',
        startDate: '2026-02-01',
        targetDate: '2026-02-28',
        completionPercent: 30,
        owner: 'Founder',
        tags: ['fundraising', 'outreach'],
        metrics: [
          { label: 'VCs Contacted', value: '15', target: '50' },
          { label: 'Meetings Scheduled', value: '2', target: '10' },
        ],
      },
      {
        id: 'ms_q1_005',
        name: 'Customer outreach launch',
        description: 'Begin pilot customer acquisition targeting quality leaders at health systems',
        status: 'in_progress',
        category: 'gtm',
        quarter: 'Q1 2026',
        startDate: '2026-02-01',
        targetDate: '2026-03-15',
        completionPercent: 20,
        owner: 'Founder',
        tags: ['sales', 'pilots'],
        metrics: [
          { label: 'Health Systems Contacted', value: '8', target: '50' },
          { label: 'Discovery Calls', value: '1', target: '15' },
        ],
      },
      {
        id: 'ms_q1_006',
        name: 'First investor meeting',
        description: 'Complete first formal pitch meeting with a healthcare VC',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q1 2026',
        startDate: '2026-02-15',
        targetDate: '2026-03-31',
        completionPercent: 0,
        owner: 'Founder',
        dependencies: ['ms_q1_004'],
        tags: ['fundraising', 'milestone'],
      },
      {
        id: 'ms_q1_007',
        name: 'First customer pilot',
        description: 'Sign first pilot agreement with a health system or ACO',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q1 2026',
        startDate: '2026-02-15',
        targetDate: '2026-03-31',
        completionPercent: 0,
        owner: 'Founder',
        dependencies: ['ms_q1_005'],
        tags: ['sales', 'milestone'],
      },

      // ==========================================
      // Q2 2026 - Traction
      // ==========================================
      {
        id: 'ms_q2_001',
        name: 'Seed funding close',
        description: 'Close $1-2M seed round from healthcare-focused investors',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q2 2026',
        startDate: '2026-04-01',
        targetDate: '2026-06-30',
        completionPercent: 0,
        owner: 'Founder',
        dependencies: ['ms_q1_006'],
        tags: ['fundraising', 'milestone'],
        metrics: [
          { label: 'Target Raise', value: '$0', target: '$1.5M' },
          { label: 'Term Sheets', value: '0', target: '2+' },
        ],
      },
      {
        id: 'ms_q2_002',
        name: '3 pilot customers',
        description: 'Onboard 3 pilot customers for HEDIS measure evaluation',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q2 2026',
        startDate: '2026-04-01',
        targetDate: '2026-06-30',
        completionPercent: 0,
        owner: 'Sales',
        dependencies: ['ms_q1_007'],
        tags: ['sales', 'milestone'],
        metrics: [
          { label: 'Active Pilots', value: '0', target: '3' },
          { label: 'Members Under Management', value: '0', target: '50K' },
        ],
      },
      {
        id: 'ms_q2_003',
        name: 'SOC 2 Type 1 certification',
        description: 'Complete SOC 2 Type 1 audit for security compliance',
        status: 'planned',
        category: 'compliance',
        quarter: 'Q2 2026',
        startDate: '2026-04-01',
        targetDate: '2026-06-15',
        completionPercent: 0,
        owner: 'Engineering',
        tags: ['security', 'compliance'],
      },
      {
        id: 'ms_q2_004',
        name: 'HITRUST assessment start',
        description: 'Begin HITRUST CSF assessment process',
        status: 'planned',
        category: 'compliance',
        quarter: 'Q2 2026',
        startDate: '2026-05-01',
        targetDate: '2026-06-30',
        completionPercent: 0,
        owner: 'Engineering',
        dependencies: ['ms_q2_003'],
        tags: ['security', 'compliance', 'hipaa'],
      },

      // ==========================================
      // Q3 2026 - Scale
      // ==========================================
      {
        id: 'ms_q3_001',
        name: 'First paying customer',
        description: 'Convert pilot to paid contract with annual commitment',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q3 2026',
        startDate: '2026-07-01',
        targetDate: '2026-08-31',
        completionPercent: 0,
        owner: 'Sales',
        dependencies: ['ms_q2_002'],
        tags: ['revenue', 'milestone'],
        metrics: [
          { label: 'ARR', value: '$0', target: '$120K' },
        ],
      },
      {
        id: 'ms_q3_002',
        name: 'Team hire (2-3 people)',
        description: 'Hire first employees: senior engineer + sales/customer success',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q3 2026',
        startDate: '2026-07-01',
        targetDate: '2026-09-30',
        completionPercent: 0,
        owner: 'Founder',
        dependencies: ['ms_q2_001'],
        tags: ['hiring', 'team'],
        metrics: [
          { label: 'Headcount', value: '1', target: '3-4' },
        ],
      },
      {
        id: 'ms_q3_003',
        name: 'HITRUST certification',
        description: 'Complete HITRUST CSF certification',
        status: 'planned',
        category: 'compliance',
        quarter: 'Q3 2026',
        startDate: '2026-07-01',
        targetDate: '2026-09-30',
        completionPercent: 0,
        owner: 'Engineering',
        dependencies: ['ms_q2_004'],
        tags: ['security', 'compliance', 'hipaa'],
      },
      {
        id: 'ms_q3_004',
        name: 'AWS marketplace listing',
        description: 'List HDIM on AWS Marketplace for enterprise procurement',
        status: 'planned',
        category: 'infrastructure',
        quarter: 'Q3 2026',
        startDate: '2026-08-01',
        targetDate: '2026-09-30',
        completionPercent: 0,
        owner: 'Engineering',
        tags: ['distribution', 'cloud'],
      },

      // ==========================================
      // Q4 2026 - Growth
      // ==========================================
      {
        id: 'ms_q4_001',
        name: 'Series A preparation',
        description: 'Prepare materials and begin conversations for Series A ($5-10M)',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q4 2026',
        startDate: '2026-10-01',
        targetDate: '2026-12-31',
        completionPercent: 0,
        owner: 'Founder',
        dependencies: ['ms_q3_001'],
        tags: ['fundraising'],
        metrics: [
          { label: 'Target Raise', value: '$0', target: '$7.5M' },
        ],
      },
      {
        id: 'ms_q4_002',
        name: '10+ customers',
        description: 'Reach 10 paying customers with $500K+ ARR',
        status: 'planned',
        category: 'gtm',
        quarter: 'Q4 2026',
        startDate: '2026-10-01',
        targetDate: '2026-12-31',
        completionPercent: 0,
        owner: 'Sales',
        dependencies: ['ms_q3_001'],
        tags: ['sales', 'milestone'],
        metrics: [
          { label: 'Customers', value: '0', target: '10+' },
          { label: 'ARR', value: '$0', target: '$500K+' },
        ],
      },
      {
        id: 'ms_q4_003',
        name: 'Multi-region deployment',
        description: 'Deploy to additional AWS regions for data residency and latency',
        status: 'planned',
        category: 'infrastructure',
        quarter: 'Q4 2026',
        startDate: '2026-10-01',
        targetDate: '2026-12-31',
        completionPercent: 0,
        owner: 'Engineering',
        dependencies: ['ms_q3_004'],
        tags: ['infrastructure', 'scale'],
      },
    ];

    this._milestones.set(initialMilestones);
    this.saveToStorage();
    this.logger.info('Seeded initial roadmap data', { count: initialMilestones.length });
  }

  // ==========================================
  // CRUD Operations
  // ==========================================

  /**
   * Get a single milestone by ID
   */
  getMilestone(id: string): RoadmapMilestone | undefined {
    return this._milestones().find((m) => m.id === id);
  }

  /**
   * Create a new milestone
   */
  createMilestone(milestone: Omit<RoadmapMilestone, 'id'>): RoadmapMilestone {
    const newMilestone: RoadmapMilestone = {
      ...milestone,
      id: `ms_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    };

    this._milestones.update((milestones) => [...milestones, newMilestone]);
    this.saveToStorage();

    this.logger.info('Created milestone', { id: newMilestone.id, name: newMilestone.name });
    return newMilestone;
  }

  /**
   * Update an existing milestone
   */
  updateMilestone(id: string, updates: Partial<RoadmapMilestone>): RoadmapMilestone | null {
    let updated: RoadmapMilestone | null = null;

    this._milestones.update((milestones) =>
      milestones.map((m) => {
        if (m.id === id) {
          updated = { ...m, ...updates };
          return updated;
        }
        return m;
      })
    );

    if (updated) {
      this.saveToStorage();
      this.logger.info('Updated milestone', { id, updates: Object.keys(updates) });
    } else {
      this.logger.warn('Milestone not found for update', { id });
    }

    return updated;
  }

  /**
   * Delete a milestone
   */
  deleteMilestone(id: string): boolean {
    const before = this._milestones().length;
    this._milestones.update((milestones) => milestones.filter((m) => m.id !== id));
    const after = this._milestones().length;

    if (before !== after) {
      this.saveToStorage();
      this.logger.info('Deleted milestone', { id });
      return true;
    }

    this.logger.warn('Milestone not found for deletion', { id });
    return false;
  }

  // ==========================================
  // Status Updates
  // ==========================================

  /**
   * Update milestone status with automatic completion handling
   */
  updateMilestoneStatus(id: string, status: MilestoneStatus, completionPercent?: number): void {
    const milestone = this.getMilestone(id);
    if (!milestone) {
      this.logger.warn('Milestone not found for status update', { id });
      return;
    }

    const updates: Partial<RoadmapMilestone> = { status };

    // Handle completion percent
    if (completionPercent !== undefined) {
      updates.completionPercent = completionPercent;
    } else if (status === 'completed') {
      updates.completionPercent = 100;
      updates.completedDate = new Date().toISOString().split('T')[0];
    } else if (status === 'planned') {
      updates.completionPercent = 0;
      updates.completedDate = undefined;
    }

    this.updateMilestone(id, updates);
    this.logger.info('Updated milestone status', { id, status, completionPercent: updates.completionPercent });
  }

  /**
   * Update milestone progress percentage
   */
  updateMilestoneProgress(id: string, percent: number): void {
    const updates: Partial<RoadmapMilestone> = {
      completionPercent: Math.max(0, Math.min(100, percent)),
    };

    // Auto-update status based on progress
    if (percent >= 100) {
      updates.status = 'completed';
      updates.completedDate = new Date().toISOString().split('T')[0];
    } else if (percent > 0) {
      updates.status = 'in_progress';
    }

    this.updateMilestone(id, updates);
  }

  // ==========================================
  // Bulk Operations
  // ==========================================

  /**
   * Get milestones by quarter
   */
  getMilestonesByQuarter(quarter: Quarter): RoadmapMilestone[] {
    return this._milestones().filter((m) => m.quarter === quarter);
  }

  /**
   * Get milestones by category
   */
  getMilestonesByCategory(category: MilestoneCategory): RoadmapMilestone[] {
    return this._milestones().filter((m) => m.category === category);
  }

  /**
   * Get milestones by status
   */
  getMilestonesByStatus(status: MilestoneStatus): RoadmapMilestone[] {
    return this._milestones().filter((m) => m.status === status);
  }

  // ==========================================
  // Filtering
  // ==========================================

  /**
   * Filter milestones based on multiple criteria
   */
  filterMilestones(filter: RoadmapFilter): RoadmapMilestone[] {
    let result = this._milestones();

    const { categories, statuses, quarters, searchTerm } = filter;

    if (categories?.length) {
      result = result.filter((m) => categories.includes(m.category));
    }

    if (statuses?.length) {
      result = result.filter((m) => statuses.includes(m.status));
    }

    if (quarters?.length) {
      result = result.filter((m) => quarters.includes(m.quarter));
    }

    if (searchTerm?.trim()) {
      const term = searchTerm.toLowerCase().trim();
      result = result.filter(
        (m) =>
          m.name.toLowerCase().includes(term) ||
          m.description.toLowerCase().includes(term) ||
          m.tags?.some((t) => t.toLowerCase().includes(term))
      );
    }

    return result;
  }

  // ==========================================
  // Statistics Calculation
  // ==========================================

  /**
   * Calculate comprehensive roadmap statistics
   */
  private calculateStats(): RoadmapStats {
    const milestones = this._milestones();
    const total = milestones.length;

    if (total === 0) {
      return {
        totalMilestones: 0,
        completedMilestones: 0,
        inProgressMilestones: 0,
        plannedMilestones: 0,
        blockedMilestones: 0,
        overallProgress: 0,
        byCategory: [],
        byQuarter: [],
      };
    }

    const completed = milestones.filter((m) => m.status === 'completed').length;
    const inProgress = milestones.filter((m) => m.status === 'in_progress').length;
    const planned = milestones.filter((m) => m.status === 'planned').length;
    const blocked = milestones.filter((m) => m.status === 'blocked').length;

    // Calculate overall progress as average completion percent
    const overallProgress = Math.round(
      milestones.reduce((sum, m) => sum + m.completionPercent, 0) / total
    );

    // Calculate by category
    const categories: MilestoneCategory[] = ['gtm', 'product', 'infrastructure', 'compliance'];
    const byCategory: CategoryStats[] = categories.map((category) => {
      const categoryMilestones = milestones.filter((m) => m.category === category);
      const categoryTotal = categoryMilestones.length;
      const categoryCompleted = categoryMilestones.filter((m) => m.status === 'completed').length;
      const categoryProgress = categoryTotal > 0
        ? Math.round(categoryMilestones.reduce((sum, m) => sum + m.completionPercent, 0) / categoryTotal)
        : 0;

      return {
        category,
        total: categoryTotal,
        completed: categoryCompleted,
        progress: categoryProgress,
      };
    });

    // Calculate by quarter
    const quarters: Quarter[] = ['Q1 2026', 'Q2 2026', 'Q3 2026', 'Q4 2026'];
    const byQuarter: QuarterStats[] = quarters.map((quarter) => {
      const quarterMilestones = milestones.filter((m) => m.quarter === quarter);
      const quarterTotal = quarterMilestones.length;
      const quarterCompleted = quarterMilestones.filter((m) => m.status === 'completed').length;
      const quarterProgress = quarterTotal > 0
        ? Math.round(quarterMilestones.reduce((sum, m) => sum + m.completionPercent, 0) / quarterTotal)
        : 0;

      return {
        quarter,
        total: quarterTotal,
        completed: quarterCompleted,
        progress: quarterProgress,
      };
    });

    return {
      totalMilestones: total,
      completedMilestones: completed,
      inProgressMilestones: inProgress,
      plannedMilestones: planned,
      blockedMilestones: blocked,
      overallProgress,
      byCategory,
      byQuarter,
    };
  }

  // ==========================================
  // Data Management
  // ==========================================

  /**
   * Export all roadmap data as JSON
   */
  exportData(): string {
    return JSON.stringify(
      {
        milestones: this._milestones(),
        stats: this.calculateStats(),
        exportDate: new Date().toISOString(),
      },
      null,
      2
    );
  }

  /**
   * Import roadmap data from JSON
   */
  importData(jsonData: string): boolean {
    try {
      const data = JSON.parse(jsonData);
      if (Array.isArray(data.milestones)) {
        this._milestones.set(data.milestones);
        this.saveToStorage();
        this.logger.info('Imported roadmap data', { count: data.milestones.length });
        return true;
      }
      throw new Error('Invalid data format: milestones array required');
    } catch (error) {
      this.logger.error('Failed to import roadmap data', error);
      this._error.set('Failed to import roadmap data');
      return false;
    }
  }

  /**
   * Reset to initial seed data
   */
  resetToInitialData(): void {
    this.logger.info('Resetting roadmap to initial data');
    this.seedInitialData();
  }

  /**
   * Clear all error state
   */
  clearError(): void {
    this._error.set(null);
  }
}
