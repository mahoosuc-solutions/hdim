import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';

/**
 * Report Template Definition
 */
export interface ReportTemplate {
  id: string;
  name: string;
  description: string;
  category: ReportTemplateCategory;
  type: 'PATIENT' | 'POPULATION' | 'COMPARATIVE';
  measures: string[];
  icon: string;
  tags: string[];
  isBuiltIn: boolean;
  isFavorite: boolean;
  lastUsed?: Date;
  usageCount: number;
  configuration: ReportTemplateConfig;
}

/**
 * Report Template Configuration
 */
export interface ReportTemplateConfig {
  includeCharts: boolean;
  includeDetails: boolean;
  includeTrends: boolean;
  includeCareGaps: boolean;
  includeRecommendations: boolean;
  groupBy: 'measure' | 'category' | 'patient';
  sortBy: 'compliance' | 'alphabetical' | 'category';
  dateRange?: 'ytd' | 'last-year' | 'custom';
  complianceThreshold?: number;
}

/**
 * Report Template Category
 */
export type ReportTemplateCategory =
  | 'CMS'
  | 'HEDIS'
  | 'PREVENTIVE'
  | 'CHRONIC_DISEASE'
  | 'BEHAVIORAL_HEALTH'
  | 'MEDICATION'
  | 'CUSTOM';

/**
 * Report Templates Service
 *
 * Manages pre-built and custom report templates for CMS/HEDIS reporting.
 *
 * Features:
 * - Pre-built templates for common CMS/HEDIS reports
 * - Custom template creation and management
 * - Template favorites and usage tracking
 * - Template configuration for report customization
 */
@Injectable({
  providedIn: 'root',
})
export class ReportTemplatesService {
  private readonly STORAGE_KEY = 'hdim_report_templates';
  private readonly FAVORITES_KEY = 'hdim_template_favorites';

  private templatesSubject = new BehaviorSubject<ReportTemplate[]>([]);
  readonly templates$ = this.templatesSubject.asObservable();

  // Built-in CMS/HEDIS templates
  private readonly builtInTemplates: ReportTemplate[] = [
    // CMS Templates
    {
      id: 'cms-star-ratings',
      name: 'CMS Star Ratings Report',
      description: 'Comprehensive report covering all CMS Star Rating quality measures for Medicare Advantage plans',
      category: 'CMS',
      type: 'POPULATION',
      measures: [
        'C01-BCS', 'C02-COL', 'C03-CDC-HBA1C', 'C04-CDC-EYE',
        'C05-COA', 'C06-OMW', 'C07-SPC', 'C08-PBH', 'C09-FMC',
        'C10-CBP', 'C11-MRP', 'C12-MTM'
      ],
      icon: 'star',
      tags: ['CMS', 'Star Ratings', 'Medicare', 'Quality'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'category',
        sortBy: 'compliance',
        dateRange: 'ytd',
        complianceThreshold: 80,
      },
    },
    {
      id: 'cms-core-measures',
      name: 'CMS Core Quality Measures',
      description: 'Report on CMS core quality measures required for value-based care programs',
      category: 'CMS',
      type: 'POPULATION',
      measures: [
        'ACO-01', 'ACO-02', 'ACO-03', 'ACO-04', 'ACO-05',
        'ACO-06', 'ACO-07', 'ACO-08', 'ACO-09', 'ACO-10'
      ],
      icon: 'verified',
      tags: ['CMS', 'ACO', 'Value-Based Care', 'Core Measures'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: false,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },

    // HEDIS Templates
    {
      id: 'hedis-preventive',
      name: 'HEDIS Preventive Care',
      description: 'All HEDIS preventive care measures including screenings and immunizations',
      category: 'HEDIS',
      type: 'POPULATION',
      measures: [
        'BCS', 'CCS', 'COL', 'AWC', 'W34', 'WCV',
        'IMA', 'CIS', 'LSC', 'FVA', 'PPC'
      ],
      icon: 'health_and_safety',
      tags: ['HEDIS', 'Preventive', 'Screenings', 'Immunizations'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },
    {
      id: 'hedis-diabetes',
      name: 'HEDIS Diabetes Care (CDC)',
      description: 'Comprehensive Diabetes Care measures including HbA1c, eye exams, and kidney health',
      category: 'HEDIS',
      type: 'POPULATION',
      measures: [
        'CDC-HBA1C-TEST', 'CDC-HBA1C-CONTROL-8', 'CDC-HBA1C-CONTROL-9',
        'CDC-EYE-EXAM', 'CDC-BP-CONTROL', 'CDC-KIDNEY-ANNUAL'
      ],
      icon: 'bloodtype',
      tags: ['HEDIS', 'Diabetes', 'CDC', 'Chronic Disease'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
        complianceThreshold: 75,
      },
    },
    {
      id: 'hedis-cardiovascular',
      name: 'HEDIS Cardiovascular Health',
      description: 'Cardiovascular health measures including blood pressure control and statin therapy',
      category: 'HEDIS',
      type: 'POPULATION',
      measures: [
        'CBP', 'SPC-ALL', 'SPC-DM', 'SPC-CVD',
        'PBH-AMI', 'PBH-CABG', 'PBH-PCI'
      ],
      icon: 'favorite',
      tags: ['HEDIS', 'Cardiovascular', 'Blood Pressure', 'Statin'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },
    {
      id: 'hedis-behavioral',
      name: 'HEDIS Behavioral Health',
      description: 'Behavioral health measures including depression screening, follow-up care, and medication management',
      category: 'BEHAVIORAL_HEALTH',
      type: 'POPULATION',
      measures: [
        'FUH-7', 'FUH-30', 'ADD-INIT', 'ADD-CONT',
        'AMM-ACUTE', 'AMM-CONT', 'DSF', 'FUM-7', 'FUM-30'
      ],
      icon: 'psychology',
      tags: ['HEDIS', 'Behavioral Health', 'Mental Health', 'Depression'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },
    {
      id: 'hedis-medication',
      name: 'HEDIS Medication Management',
      description: 'Medication adherence and management measures across therapeutic categories',
      category: 'MEDICATION',
      type: 'POPULATION',
      measures: [
        'MRP', 'SPD', 'PDC-RAS', 'PDC-DM', 'PDC-STATIN',
        'MTM-CMR', 'POD', 'DAE'
      ],
      icon: 'medication',
      tags: ['HEDIS', 'Medication', 'Adherence', 'PDC'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'category',
        sortBy: 'compliance',
        dateRange: 'ytd',
        complianceThreshold: 80,
      },
    },

    // Preventive Care Templates
    {
      id: 'cancer-screenings',
      name: 'Cancer Screenings Report',
      description: 'All cancer screening measures including breast, cervical, and colorectal cancer',
      category: 'PREVENTIVE',
      type: 'POPULATION',
      measures: ['BCS', 'CCS', 'COL'],
      icon: 'biotech',
      tags: ['Cancer', 'Screenings', 'Preventive', 'Women\'s Health'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },
    {
      id: 'immunizations',
      name: 'Immunizations Report',
      description: 'Child and adult immunization measures including flu, pneumonia, and childhood vaccines',
      category: 'PREVENTIVE',
      type: 'POPULATION',
      measures: ['IMA', 'CIS', 'FVA', 'PNU'],
      icon: 'vaccines',
      tags: ['Immunizations', 'Vaccines', 'Preventive', 'Pediatric'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'measure',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },

    // Chronic Disease Templates
    {
      id: 'chronic-disease-management',
      name: 'Chronic Disease Management',
      description: 'Comprehensive report covering all chronic disease management measures',
      category: 'CHRONIC_DISEASE',
      type: 'POPULATION',
      measures: [
        'CDC-HBA1C-TEST', 'CDC-HBA1C-CONTROL-8', 'CBP',
        'SPC-ALL', 'KED', 'OMW', 'SPR'
      ],
      icon: 'medical_services',
      tags: ['Chronic Disease', 'Diabetes', 'Hypertension', 'Osteoporosis'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'category',
        sortBy: 'compliance',
        dateRange: 'ytd',
      },
    },

    // Patient-Level Templates
    {
      id: 'patient-quality-summary',
      name: 'Patient Quality Summary',
      description: 'Individual patient quality measure summary with all applicable measures',
      category: 'HEDIS',
      type: 'PATIENT',
      measures: [], // All applicable measures
      icon: 'person',
      tags: ['Patient', 'Individual', 'Summary', 'All Measures'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: true,
        includeDetails: true,
        includeTrends: true,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'category',
        sortBy: 'compliance',
      },
    },
    {
      id: 'patient-care-gaps',
      name: 'Patient Care Gaps Report',
      description: 'Focus on open care gaps for an individual patient with recommended actions',
      category: 'HEDIS',
      type: 'PATIENT',
      measures: [], // Measures with open gaps
      icon: 'warning',
      tags: ['Patient', 'Care Gaps', 'Action Items', 'Priority'],
      isBuiltIn: true,
      isFavorite: false,
      usageCount: 0,
      configuration: {
        includeCharts: false,
        includeDetails: true,
        includeTrends: false,
        includeCareGaps: true,
        includeRecommendations: true,
        groupBy: 'category',
        sortBy: 'compliance',
      },
    },
  ];

  constructor() {
    this.loadTemplates();
  }

  /**
   * Get all templates (built-in + custom)
   */
  getTemplates(): ReportTemplate[] {
    return this.templatesSubject.getValue();
  }

  /**
   * Get templates by category
   */
  getTemplatesByCategory(category: ReportTemplateCategory): ReportTemplate[] {
    return this.getTemplates().filter(t => t.category === category);
  }

  /**
   * Get templates by type
   */
  getTemplatesByType(type: 'PATIENT' | 'POPULATION' | 'COMPARATIVE'): ReportTemplate[] {
    return this.getTemplates().filter(t => t.type === type);
  }

  /**
   * Get favorite templates
   */
  getFavoriteTemplates(): ReportTemplate[] {
    return this.getTemplates().filter(t => t.isFavorite);
  }

  /**
   * Get recently used templates
   */
  getRecentTemplates(limit = 5): ReportTemplate[] {
    return [...this.getTemplates()]
      .filter(t => t.lastUsed)
      .sort((a, b) => {
        const aTime = a.lastUsed?.getTime() || 0;
        const bTime = b.lastUsed?.getTime() || 0;
        return bTime - aTime;
      })
      .slice(0, limit);
  }

  /**
   * Get most used templates
   */
  getMostUsedTemplates(limit = 5): ReportTemplate[] {
    return [...this.getTemplates()]
      .filter(t => t.usageCount > 0)
      .sort((a, b) => b.usageCount - a.usageCount)
      .slice(0, limit);
  }

  /**
   * Get a template by ID
   */
  getTemplate(id: string): ReportTemplate | undefined {
    return this.getTemplates().find(t => t.id === id);
  }

  /**
   * Toggle template favorite status
   */
  toggleFavorite(templateId: string): void {
    const templates = this.getTemplates();
    const template = templates.find(t => t.id === templateId);
    if (template) {
      template.isFavorite = !template.isFavorite;
      this.templatesSubject.next([...templates]);
      this.saveFavorites();
    }
  }

  /**
   * Record template usage
   */
  recordUsage(templateId: string): void {
    const templates = this.getTemplates();
    const template = templates.find(t => t.id === templateId);
    if (template) {
      template.usageCount++;
      template.lastUsed = new Date();
      this.templatesSubject.next([...templates]);
      this.saveUsageStats();
    }
  }

  /**
   * Create a custom template
   */
  createCustomTemplate(template: Omit<ReportTemplate, 'id' | 'isBuiltIn' | 'usageCount'>): ReportTemplate {
    const newTemplate: ReportTemplate = {
      ...template,
      id: `custom-${Date.now()}`,
      isBuiltIn: false,
      usageCount: 0,
    };

    const templates = [...this.getTemplates(), newTemplate];
    this.templatesSubject.next(templates);
    this.saveCustomTemplates();

    return newTemplate;
  }

  /**
   * Update a custom template
   */
  updateCustomTemplate(templateId: string, updates: Partial<ReportTemplate>): boolean {
    const templates = this.getTemplates();
    const index = templates.findIndex(t => t.id === templateId);

    if (index === -1 || templates[index].isBuiltIn) {
      return false;
    }

    templates[index] = { ...templates[index], ...updates };
    this.templatesSubject.next([...templates]);
    this.saveCustomTemplates();
    return true;
  }

  /**
   * Delete a custom template
   */
  deleteCustomTemplate(templateId: string): boolean {
    const templates = this.getTemplates();
    const template = templates.find(t => t.id === templateId);

    if (!template || template.isBuiltIn) {
      return false;
    }

    const filtered = templates.filter(t => t.id !== templateId);
    this.templatesSubject.next(filtered);
    this.saveCustomTemplates();
    return true;
  }

  /**
   * Search templates by name or tags
   */
  searchTemplates(query: string): ReportTemplate[] {
    const queryLower = query.toLowerCase();
    return this.getTemplates().filter(t =>
      t.name.toLowerCase().includes(queryLower) ||
      t.description.toLowerCase().includes(queryLower) ||
      t.tags.some(tag => tag.toLowerCase().includes(queryLower))
    );
  }

  /**
   * Get available categories
   */
  getCategories(): { value: ReportTemplateCategory; label: string; icon: string }[] {
    return [
      { value: 'CMS', label: 'CMS Reports', icon: 'verified' },
      { value: 'HEDIS', label: 'HEDIS Measures', icon: 'assessment' },
      { value: 'PREVENTIVE', label: 'Preventive Care', icon: 'health_and_safety' },
      { value: 'CHRONIC_DISEASE', label: 'Chronic Disease', icon: 'medical_services' },
      { value: 'BEHAVIORAL_HEALTH', label: 'Behavioral Health', icon: 'psychology' },
      { value: 'MEDICATION', label: 'Medication', icon: 'medication' },
      { value: 'CUSTOM', label: 'Custom Templates', icon: 'edit' },
    ];
  }

  // Private methods

  private loadTemplates(): void {
    // Start with built-in templates
    const templates = [...this.builtInTemplates];

    // Load favorites
    const favorites = this.loadFavorites();
    templates.forEach(t => {
      if (favorites.includes(t.id)) {
        t.isFavorite = true;
      }
    });

    // Load usage stats
    const usageStats = this.loadUsageStats();
    templates.forEach(t => {
      const stats = usageStats[t.id];
      if (stats) {
        t.usageCount = stats.count;
        t.lastUsed = stats.lastUsed ? new Date(stats.lastUsed) : undefined;
      }
    });

    // Load custom templates
    const customTemplates = this.loadCustomTemplates();
    templates.push(...customTemplates);

    this.templatesSubject.next(templates);
  }

  private loadFavorites(): string[] {
    try {
      const stored = localStorage.getItem(this.FAVORITES_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private saveFavorites(): void {
    const favorites = this.getTemplates()
      .filter(t => t.isFavorite)
      .map(t => t.id);
    localStorage.setItem(this.FAVORITES_KEY, JSON.stringify(favorites));
  }

  private loadUsageStats(): Record<string, { count: number; lastUsed?: string }> {
    try {
      const stored = localStorage.getItem(`${this.STORAGE_KEY}_usage`);
      return stored ? JSON.parse(stored) : {};
    } catch {
      return {};
    }
  }

  private saveUsageStats(): void {
    const stats: Record<string, { count: number; lastUsed?: string }> = {};
    this.getTemplates().forEach(t => {
      if (t.usageCount > 0) {
        stats[t.id] = {
          count: t.usageCount,
          lastUsed: t.lastUsed?.toISOString(),
        };
      }
    });
    localStorage.setItem(`${this.STORAGE_KEY}_usage`, JSON.stringify(stats));
  }

  private loadCustomTemplates(): ReportTemplate[] {
    try {
      const stored = localStorage.getItem(`${this.STORAGE_KEY}_custom`);
      if (!stored) return [];

      const parsed = JSON.parse(stored) as ReportTemplate[];
      return parsed.map(t => ({
        ...t,
        lastUsed: t.lastUsed ? new Date(t.lastUsed) : undefined,
      }));
    } catch {
      return [];
    }
  }

  private saveCustomTemplates(): void {
    const customTemplates = this.getTemplates().filter(t => !t.isBuiltIn);
    localStorage.setItem(`${this.STORAGE_KEY}_custom`, JSON.stringify(customTemplates));
  }
}
