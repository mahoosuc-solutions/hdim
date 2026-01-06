import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

/**
 * Help Content for a specific page
 */
export interface HelpContent {
  pageId: string;
  title: string;
  overview: string;
  features: HelpFeature[];
  tutorials: HelpTutorial[];
  faq: HelpFAQ[];
  tips: string[];
}

/**
 * Feature help information
 */
export interface HelpFeature {
  name: string;
  description: string;
  tip: string;
  icon: string;
}

/**
 * Tutorial link
 */
export interface HelpTutorial {
  title: string;
  description: string;
  url: string;
  duration: string;
  type: 'video' | 'article' | 'guide';
}

/**
 * FAQ item
 */
export interface HelpFAQ {
  question: string;
  answer: string;
  tags: string[];
}

/**
 * What's New item
 */
export interface WhatsNewItem {
  id: string;
  title: string;
  description: string;
  date: Date;
  type: 'feature' | 'improvement' | 'fix';
  link?: string;
  dismissed: boolean;
}

/**
 * Help Service
 *
 * Provides contextual help content throughout the Clinical Portal.
 *
 * Features:
 * - Context-aware help based on current page
 * - Feature tooltips and descriptions
 * - Tutorial links (video, article, guide)
 * - FAQ content
 * - What's New announcements
 * - Guided tours for first-time users
 */
@Injectable({
  providedIn: 'root',
})
export class HelpService {
  private readonly DISMISSED_KEY = 'hdim_dismissed_whats_new';
  private readonly SEEN_TOURS_KEY = 'hdim_seen_tours';

  private helpContent: Map<string, HelpContent> = new Map();
  private whatsNewItems: WhatsNewItem[] = [];

  private currentHelpSubject = new BehaviorSubject<HelpContent | null>(null);
  readonly currentHelp$ = this.currentHelpSubject.asObservable();

  private whatsNewSubject = new BehaviorSubject<WhatsNewItem[]>([]);
  readonly whatsNew$ = this.whatsNewSubject.asObservable();

  private showHelpPanelSubject = new BehaviorSubject<boolean>(false);
  readonly showHelpPanel$ = this.showHelpPanelSubject.asObservable();

  constructor(private router: Router) {
    this.initializeHelpContent();
    this.initializeWhatsNew();
    this.loadDismissedItems();
    this.setupRouteListener();
  }

  /**
   * Initialize help content for all pages
   */
  private initializeHelpContent(): void {
    // Dashboard Help
    this.helpContent.set('dashboard', {
      pageId: 'dashboard',
      title: 'Provider Dashboard',
      overview: 'Your central hub for patient care management. View key metrics, care gaps, and patient priorities at a glance.',
      features: [
        {
          name: 'Quality Metrics',
          description: 'Real-time quality measure performance across your patient panel',
          tip: 'Click on any metric card to drill down into detailed patient lists',
          icon: 'assessment',
        },
        {
          name: 'Care Gap Priorities',
          description: 'High-priority care gaps requiring immediate attention',
          tip: 'Care gaps are sorted by clinical urgency and patient risk score',
          icon: 'warning',
        },
        {
          name: 'Patient Panel',
          description: 'Quick access to your assigned patients',
          tip: 'Use filters to find patients by risk level or care gap status',
          icon: 'people',
        },
        {
          name: 'Role-Based Views',
          description: 'Dashboard adapts to show relevant information for your role',
          tip: 'Administrators see organization-wide metrics, providers see their panel',
          icon: 'supervisor_account',
        },
      ],
      tutorials: [
        {
          title: 'Getting Started with the Dashboard',
          description: 'Learn the basics of navigating and using the provider dashboard',
          url: '/knowledge-base/article/dashboard-basics',
          duration: '5 min',
          type: 'guide',
        },
        {
          title: 'Managing Care Gaps',
          description: 'How to identify, prioritize, and close care gaps',
          url: '/knowledge-base/article/care-gaps',
          duration: '8 min',
          type: 'guide',
        },
      ],
      faq: [
        {
          question: 'How often is the data refreshed?',
          answer: 'Dashboard data is refreshed every 5 minutes automatically. You can also manually refresh using the refresh button or Ctrl+R.',
          tags: ['refresh', 'data', 'real-time'],
        },
        {
          question: 'Why are some patients marked as high risk?',
          answer: 'Risk scores are calculated based on clinical factors including age, conditions, medications, and care gap status. Higher scores indicate patients who may benefit from more proactive care.',
          tags: ['risk', 'patients', 'scoring'],
        },
      ],
      tips: [
        'Use keyboard shortcut Alt+D to quickly return to the dashboard from anywhere',
        'Click on patient names to view their detailed care profile',
        'The colored indicators show measure compliance status at a glance',
      ],
    });

    // Patients Help
    this.helpContent.set('patients', {
      pageId: 'patients',
      title: 'Patient Management',
      overview: 'Search, view, and manage patient records. Access comprehensive patient profiles and care history.',
      features: [
        {
          name: 'Patient Search',
          description: 'Find patients by name, MRN, or demographic criteria',
          tip: 'Use Ctrl+F to focus the search box instantly',
          icon: 'search',
        },
        {
          name: 'Patient Profile',
          description: 'Comprehensive view of patient demographics, conditions, and care gaps',
          tip: 'Click any row to expand the patient details panel',
          icon: 'person',
        },
        {
          name: 'Care Gap Status',
          description: 'Visual indicators showing open care gaps for each patient',
          tip: 'Red badges indicate high-priority gaps requiring action',
          icon: 'warning',
        },
        {
          name: 'Bulk Actions',
          description: 'Select multiple patients for batch operations',
          tip: 'Use checkboxes to select patients, then choose a bulk action',
          icon: 'checklist',
        },
      ],
      tutorials: [
        {
          title: 'Searching for Patients',
          description: 'Advanced search techniques for finding patients',
          url: '/knowledge-base/article/patient-search',
          duration: '3 min',
          type: 'article',
        },
        {
          title: 'Understanding Patient Risk Scores',
          description: 'How risk stratification helps prioritize care',
          url: '/knowledge-base/article/risk-scores',
          duration: '6 min',
          type: 'guide',
        },
      ],
      faq: [
        {
          question: 'How do I add a new patient?',
          answer: 'Patients are typically synchronized from your EHR system. If you need to add a patient manually, contact your system administrator.',
          tags: ['add', 'new', 'patient', 'create'],
        },
        {
          question: 'What do the colored status indicators mean?',
          answer: 'Green = compliant/no action needed, Yellow = approaching due date, Red = overdue/non-compliant, Gray = not applicable.',
          tags: ['colors', 'status', 'indicators'],
        },
      ],
      tips: [
        'Sort by risk score to prioritize high-risk patients',
        'Use the filter sidebar to narrow down patient lists',
        'Export patient lists to CSV for external analysis',
      ],
    });

    // Evaluations Help
    this.helpContent.set('evaluations', {
      pageId: 'evaluations',
      title: 'Quality Measure Evaluations',
      overview: 'Run quality measure calculations against patient data. View individual or population-level measure compliance.',
      features: [
        {
          name: 'Measure Selection',
          description: 'Choose from HEDIS, CMS, or custom quality measures',
          tip: 'Use the category filter to find specific measure types',
          icon: 'checklist',
        },
        {
          name: 'Patient vs Population',
          description: 'Run evaluations for individual patients or entire populations',
          tip: 'Population evaluations may take longer but provide aggregate statistics',
          icon: 'groups',
        },
        {
          name: 'Real-Time Results',
          description: 'View evaluation results as they are calculated',
          tip: 'Large evaluations run in the background - you can navigate away and return',
          icon: 'speed',
        },
        {
          name: 'Local Calculation',
          description: 'Calculate measures directly in the browser for quick results',
          tip: 'Local mode is faster but may not include all clinical data',
          icon: 'computer',
        },
      ],
      tutorials: [
        {
          title: 'Running Your First Evaluation',
          description: 'Step-by-step guide to running quality measure evaluations',
          url: '/knowledge-base/article/first-evaluation',
          duration: '5 min',
          type: 'guide',
        },
        {
          title: 'Understanding Measure Results',
          description: 'How to interpret evaluation results and compliance rates',
          url: '/knowledge-base/article/measure-results',
          duration: '7 min',
          type: 'article',
        },
      ],
      faq: [
        {
          question: 'Why does the evaluation take so long?',
          answer: 'Evaluation time depends on the number of patients and complexity of the measure. Population evaluations process hundreds of patients and may take several minutes.',
          tags: ['slow', 'time', 'performance'],
        },
        {
          question: "What's the difference between local and server evaluation?",
          answer: 'Local evaluation runs in your browser using cached data for speed. Server evaluation uses the full clinical data store for accuracy.',
          tags: ['local', 'server', 'difference'],
        },
      ],
      tips: [
        'Start with a single patient to verify measure logic before running population evaluations',
        'Save frequently used evaluation parameters as presets',
        'Check the "Live Monitor" in Visualizations to track long-running evaluations',
      ],
    });

    // Reports Help
    this.helpContent.set('reports', {
      pageId: 'reports',
      title: 'Quality Reports',
      overview: 'Generate, view, and export comprehensive quality measure reports for patients and populations.',
      features: [
        {
          name: 'Report Templates',
          description: 'Pre-built templates for CMS, HEDIS, and custom reports',
          tip: 'Star your favorite templates for quick access',
          icon: 'description',
        },
        {
          name: 'Custom Reports',
          description: 'Build your own reports with specific measures and configurations',
          tip: 'Use the Report Builder for full customization',
          icon: 'build',
        },
        {
          name: 'Export Options',
          description: 'Export reports to PDF, CSV, or Excel formats',
          tip: 'CSV exports include raw data for further analysis',
          icon: 'download',
        },
        {
          name: 'QRDA Export',
          description: 'Generate QRDA I and III files for CMS submission',
          tip: 'QRDA exports follow current CMS specifications',
          icon: 'upload_file',
        },
      ],
      tutorials: [
        {
          title: 'Creating Custom Reports',
          description: 'How to build tailored reports for your organization',
          url: '/knowledge-base/article/custom-reports',
          duration: '10 min',
          type: 'guide',
        },
        {
          title: 'QRDA Export Guide',
          description: 'Generating compliant QRDA files for CMS submission',
          url: '/knowledge-base/article/qrda-export',
          duration: '8 min',
          type: 'article',
        },
      ],
      faq: [
        {
          question: 'How do I submit reports to CMS?',
          answer: 'Use the QRDA Export feature to generate compliant files, then upload them through the CMS submission portal.',
          tags: ['cms', 'submission', 'qrda'],
        },
        {
          question: 'Can I schedule recurring reports?',
          answer: 'Yes, use the "Schedule" option when saving a report to set up automatic generation.',
          tags: ['schedule', 'recurring', 'automatic'],
        },
      ],
      tips: [
        'Use Ctrl+N to quickly start a new custom report',
        'Comparative reports show trends over time',
        'Check QRDA validation results before submitting to CMS',
      ],
    });

    // Insights Help
    this.helpContent.set('insights', {
      pageId: 'insights',
      title: 'Population Health Insights',
      overview: 'AI-powered analytics revealing patterns, trends, and opportunities across your patient population.',
      features: [
        {
          name: 'Active Insights',
          description: 'AI-generated observations about your patient population',
          tip: 'Insights are prioritized by potential impact on quality scores',
          icon: 'psychology',
        },
        {
          name: 'Predicted Care Gaps',
          description: 'Machine learning predictions of patients likely to develop care gaps',
          tip: 'Act on predictions early to prevent gaps from occurring',
          icon: 'trending_up',
        },
        {
          name: 'Quality Scores',
          description: 'Real-time measure compliance with benchmarks and targets',
          tip: 'Click on measures to see improvement recommendations',
          icon: 'score',
        },
        {
          name: 'Risk Stratification',
          description: 'Patient population breakdown by risk tier',
          tip: 'High-risk patients require more intensive care management',
          icon: 'signal_cellular_alt',
        },
      ],
      tutorials: [
        {
          title: 'Using AI Insights',
          description: 'How to interpret and act on AI-generated insights',
          url: '/knowledge-base/article/ai-insights',
          duration: '6 min',
          type: 'guide',
        },
        {
          title: 'Predictive Analytics',
          description: 'Understanding care gap predictions',
          url: '/knowledge-base/article/predictions',
          duration: '8 min',
          type: 'article',
        },
      ],
      faq: [
        {
          question: 'How accurate are the predictions?',
          answer: 'Prediction accuracy varies by measure type but typically ranges from 75-90%. Confidence scores are shown for each prediction.',
          tags: ['accuracy', 'predictions', 'confidence'],
        },
        {
          question: 'How often are insights updated?',
          answer: 'Insights are recalculated nightly based on the latest patient data.',
          tags: ['update', 'refresh', 'frequency'],
        },
      ],
      tips: [
        'Dismiss insights you have already addressed to focus on new opportunities',
        'High-impact insights may affect multiple quality measures',
        'Export insights to share with your care team',
      ],
    });

    // Report Builder Help
    this.helpContent.set('report-builder', {
      pageId: 'report-builder',
      title: 'Custom Report Builder',
      overview: 'Create personalized quality measure reports with drag-and-drop measure selection and rich configuration options.',
      features: [
        {
          name: 'Measure Selection',
          description: 'Drag and drop measures to build your custom report',
          tip: 'Use the search to quickly find specific measures',
          icon: 'checklist',
        },
        {
          name: 'Report Sections',
          description: 'Choose which sections to include in your report',
          tip: 'Disable sections you do not need to reduce report size',
          icon: 'view_module',
        },
        {
          name: 'Configuration Options',
          description: 'Set grouping, sorting, and threshold preferences',
          tip: 'Compliance threshold highlights underperforming measures',
          icon: 'tune',
        },
        {
          name: 'Templates',
          description: 'Save your configuration as a reusable template',
          tip: 'Add templates to favorites for quick access',
          icon: 'bookmark',
        },
      ],
      tutorials: [
        {
          title: 'Building Your First Custom Report',
          description: 'Step-by-step guide to creating custom reports',
          url: '/knowledge-base/article/report-builder-guide',
          duration: '10 min',
          type: 'guide',
        },
      ],
      faq: [
        {
          question: 'Can I edit a built-in template?',
          answer: 'Built-in templates cannot be modified, but you can create a custom template based on one.',
          tags: ['edit', 'template', 'built-in'],
        },
        {
          question: 'How many measures can I include?',
          answer: 'There is no hard limit, but reports with more than 20 measures may take longer to generate.',
          tags: ['measures', 'limit', 'performance'],
        },
      ],
      tips: [
        'Use categories to organize measures logically',
        'Preview your report before saving the template',
        'Enable "Generate Immediately" to see results right away',
      ],
    });

    // Default help for unknown pages
    this.helpContent.set('default', {
      pageId: 'default',
      title: 'Clinical Portal Help',
      overview: 'Welcome to the Clinical Portal. Use the navigation menu to access different features.',
      features: [
        {
          name: 'Navigation',
          description: 'Use the sidebar to navigate between sections',
          tip: 'Press ? to see all keyboard shortcuts',
          icon: 'menu',
        },
        {
          name: 'Help System',
          description: 'Click the help icon on any page for contextual assistance',
          tip: 'Help content is tailored to each page',
          icon: 'help',
        },
      ],
      tutorials: [
        {
          title: 'Clinical Portal Overview',
          description: 'Introduction to the Clinical Portal features',
          url: '/knowledge-base/article/overview',
          duration: '5 min',
          type: 'guide',
        },
      ],
      faq: [
        {
          question: 'Where can I get additional help?',
          answer: 'Visit the Knowledge Base for articles and guides, or contact support.',
          tags: ['help', 'support', 'contact'],
        },
      ],
      tips: [
        'Press ? anywhere to see keyboard shortcuts',
        'Your preferences are saved automatically',
      ],
    });
  }

  /**
   * Initialize What's New items
   */
  private initializeWhatsNew(): void {
    this.whatsNewItems = [
      {
        id: 'population-insights-2026-01',
        title: 'Population Health Insights',
        description: 'New AI-powered insights dashboard with predictive care gap detection and risk stratification visualization.',
        date: new Date('2026-01-06'),
        type: 'feature',
        link: '/insights',
        dismissed: false,
      },
      {
        id: 'custom-report-builder-2026-01',
        title: 'Custom Report Builder',
        description: 'Create personalized quality reports with drag-and-drop measure selection and rich configuration options.',
        date: new Date('2026-01-06'),
        type: 'feature',
        link: '/report-builder',
        dismissed: false,
      },
      {
        id: 'keyboard-shortcuts-2026-01',
        title: 'Keyboard Shortcuts',
        description: 'Navigate faster with keyboard shortcuts. Press ? anywhere to see all available shortcuts.',
        date: new Date('2026-01-06'),
        type: 'feature',
        dismissed: false,
      },
      {
        id: 'measure-builder-perf-2026-01',
        title: 'Measure Builder Performance',
        description: 'Improved measure builder with bulk operations and performance dashboard.',
        date: new Date('2026-01-05'),
        type: 'improvement',
        link: '/measure-builder',
        dismissed: false,
      },
    ];

    this.emitWhatsNew();
  }

  /**
   * Setup route change listener
   */
  private setupRouteListener(): void {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        const pageId = this.getPageIdFromUrl(event.urlAfterRedirects);
        const help = this.helpContent.get(pageId) || this.helpContent.get('default') || null;
        this.currentHelpSubject.next(help);
      });
  }

  /**
   * Extract page ID from URL
   */
  private getPageIdFromUrl(url: string): string {
    const path = url.split('?')[0].split('/').filter(Boolean);
    if (path.length === 0) return 'dashboard';
    return path[0].replace(/-/g, '-');
  }

  /**
   * Get help for current page
   */
  getCurrentHelp(): HelpContent | null {
    return this.currentHelpSubject.getValue();
  }

  /**
   * Get help for specific page
   */
  getHelpForPage(pageId: string): HelpContent | null {
    return this.helpContent.get(pageId) || this.helpContent.get('default') || null;
  }

  /**
   * Get What's New items (undismissed)
   */
  getWhatsNew(): WhatsNewItem[] {
    return this.whatsNewItems.filter(item => !item.dismissed);
  }

  /**
   * Dismiss What's New item
   */
  dismissWhatsNewItem(id: string): void {
    const item = this.whatsNewItems.find(i => i.id === id);
    if (item) {
      item.dismissed = true;
      this.saveDismissedItems();
      this.emitWhatsNew();
    }
  }

  /**
   * Dismiss all What's New items
   */
  dismissAllWhatsNew(): void {
    this.whatsNewItems.forEach(item => item.dismissed = true);
    this.saveDismissedItems();
    this.emitWhatsNew();
  }

  /**
   * Toggle help panel visibility
   */
  toggleHelpPanel(): void {
    this.showHelpPanelSubject.next(!this.showHelpPanelSubject.getValue());
  }

  /**
   * Show help panel
   */
  showHelpPanel(): void {
    this.showHelpPanelSubject.next(true);
  }

  /**
   * Hide help panel
   */
  hideHelpPanel(): void {
    this.showHelpPanelSubject.next(false);
  }

  /**
   * Check if user has seen a guided tour
   */
  hasSeenTour(tourId: string): boolean {
    const seen = this.getSeenTours();
    return seen.includes(tourId);
  }

  /**
   * Mark tour as seen
   */
  markTourAsSeen(tourId: string): void {
    const seen = this.getSeenTours();
    if (!seen.includes(tourId)) {
      seen.push(tourId);
      localStorage.setItem(this.SEEN_TOURS_KEY, JSON.stringify(seen));
    }
  }

  /**
   * Reset tour history (show tours again)
   */
  resetTours(): void {
    localStorage.removeItem(this.SEEN_TOURS_KEY);
  }

  /**
   * Search help content
   */
  searchHelp(query: string): { pageId: string; matches: string[] }[] {
    const results: { pageId: string; matches: string[] }[] = [];
    const queryLower = query.toLowerCase();

    for (const [pageId, content] of this.helpContent.entries()) {
      const matches: string[] = [];

      if (content.title.toLowerCase().includes(queryLower)) {
        matches.push(`Title: ${content.title}`);
      }
      if (content.overview.toLowerCase().includes(queryLower)) {
        matches.push(`Overview: ${content.overview.substring(0, 100)}...`);
      }

      content.features.forEach(f => {
        if (f.name.toLowerCase().includes(queryLower) || f.description.toLowerCase().includes(queryLower)) {
          matches.push(`Feature: ${f.name}`);
        }
      });

      content.faq.forEach(f => {
        if (f.question.toLowerCase().includes(queryLower) || f.answer.toLowerCase().includes(queryLower)) {
          matches.push(`FAQ: ${f.question}`);
        }
      });

      if (matches.length > 0) {
        results.push({ pageId, matches });
      }
    }

    return results;
  }

  // Private helpers

  private emitWhatsNew(): void {
    this.whatsNewSubject.next([...this.whatsNewItems]);
  }

  private loadDismissedItems(): void {
    try {
      const stored = localStorage.getItem(this.DISMISSED_KEY);
      if (stored) {
        const dismissed: string[] = JSON.parse(stored);
        this.whatsNewItems.forEach(item => {
          if (dismissed.includes(item.id)) {
            item.dismissed = true;
          }
        });
        this.emitWhatsNew();
      }
    } catch {
      // Ignore parse errors
    }
  }

  private saveDismissedItems(): void {
    const dismissed = this.whatsNewItems
      .filter(item => item.dismissed)
      .map(item => item.id);
    localStorage.setItem(this.DISMISSED_KEY, JSON.stringify(dismissed));
  }

  private getSeenTours(): string[] {
    try {
      const stored = localStorage.getItem(this.SEEN_TOURS_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }
}
