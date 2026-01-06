/**
 * Issue #24: Provider Dashboard Help System
 * Contextual help panel with feature guides, tutorials, and FAQs
 *
 * Provides:
 * - Context-aware help content based on current page
 * - Feature overview and step-by-step guides
 * - Video tutorial links
 * - FAQ section
 * - Contact support option
 */
import { Component, OnInit, OnDestroy, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import {
  trigger,
  state,
  style,
  transition,
  animate,
} from '@angular/animations';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';

export interface HelpFeature {
  name: string;
  description: string;
  tip: string;
  icon?: string;
}

export interface HelpTutorial {
  title: string;
  url: string;
  duration: string;
  type: 'video' | 'article' | 'guide';
}

export interface HelpFaq {
  question: string;
  answer: string;
}

export interface HelpContent {
  pageId: string;
  title: string;
  overview: string;
  features: HelpFeature[];
  tutorials: HelpTutorial[];
  faq: HelpFaq[];
  shortcuts?: { key: string; action: string }[];
}

// Help content database for all pages
const HELP_CONTENT: Record<string, HelpContent> = {
  dashboard: {
    pageId: 'dashboard',
    title: 'Provider Dashboard',
    overview:
      'Your personalized view of patient care gaps, quality measures, and pending tasks. Use this dashboard to prioritize your daily workflow and track your performance.',
    features: [
      {
        name: 'Care Gap Summary',
        description: 'Shows high-priority care gaps requiring attention',
        tip: 'Click on any care gap to view details and take quick actions',
        icon: 'warning',
      },
      {
        name: 'Quality Measures',
        description: 'Your performance on HEDIS quality measures',
        tip: 'Green indicates you\'re meeting targets, yellow needs attention',
        icon: 'assessment',
      },
      {
        name: 'Pending Results',
        description: 'Lab results and reports awaiting your review',
        tip: 'Use bulk signing for normal results to save time',
        icon: 'pending_actions',
      },
      {
        name: 'Patient Risk Stratification',
        description: 'Patients grouped by risk level',
        tip: 'Focus on high-risk patients first for maximum impact',
        icon: 'priority_high',
      },
    ],
    tutorials: [
      {
        title: 'Getting Started with Provider Dashboard',
        url: '/help/tutorials/dashboard-intro',
        duration: '5 min',
        type: 'video',
      },
      {
        title: 'Closing Care Gaps Efficiently',
        url: '/help/tutorials/care-gaps',
        duration: '8 min',
        type: 'video',
      },
      {
        title: 'Understanding Quality Measures',
        url: '/help/guides/quality-measures',
        duration: '10 min',
        type: 'article',
      },
    ],
    faq: [
      {
        question: 'How often is the dashboard updated?',
        answer:
          'Dashboard data refreshes every 5 minutes automatically. You can also click the refresh button to get the latest data immediately.',
      },
      {
        question: 'Why am I seeing patients not in my panel?',
        answer:
          'Contact your administrator to verify your panel assignment. Some shared patients may appear if you\'re part of a care team.',
      },
      {
        question: 'How is my quality score calculated?',
        answer:
          'Quality scores are based on your patient panel\'s compliance with HEDIS measures. The numerator (compliant patients) divided by denominator (eligible patients) gives your rate.',
      },
    ],
    shortcuts: [
      { key: '?', action: 'Open help panel' },
      { key: 'Ctrl+R', action: 'Refresh dashboard' },
      { key: 'Ctrl+S', action: 'Sign selected result' },
      { key: 'Ctrl+F', action: 'Focus search' },
    ],
  },
  patients: {
    pageId: 'patients',
    title: 'Patient Management',
    overview:
      'Search, view, and manage your patient panel. Access detailed patient information, care gaps, and clinical history.',
    features: [
      {
        name: 'Patient Search',
        description: 'Find patients by name, MRN, or condition',
        tip: 'Use filters to narrow down large patient lists',
        icon: 'search',
      },
      {
        name: 'Patient Details',
        description: 'Complete view of patient demographics and history',
        tip: 'Click the care gap tab to see all open items',
        icon: 'person',
      },
      {
        name: 'Care Timeline',
        description: 'Chronological view of patient encounters',
        tip: 'Hover over timeline items for quick details',
        icon: 'timeline',
      },
    ],
    tutorials: [
      {
        title: 'Finding and Managing Patients',
        url: '/help/tutorials/patient-management',
        duration: '6 min',
        type: 'video',
      },
    ],
    faq: [
      {
        question: 'How do I add a new patient?',
        answer:
          'New patients are typically added through your EHR system. Contact your administrator if you need to manually add a patient.',
      },
      {
        question: 'Can I see patients from other providers?',
        answer:
          'You can only see patients assigned to your panel unless you have administrative access.',
      },
    ],
    shortcuts: [
      { key: 'Ctrl+F', action: 'Focus search' },
      { key: 'Enter', action: 'Open selected patient' },
      { key: 'Escape', action: 'Clear search' },
    ],
  },
  evaluations: {
    pageId: 'evaluations',
    title: 'Quality Measure Evaluations',
    overview:
      'Run quality measure evaluations against your patient population. View compliance rates and identify patients needing intervention.',
    features: [
      {
        name: 'Measure Selection',
        description: 'Choose measures to evaluate',
        tip: 'Select multiple measures for batch evaluation',
        icon: 'checklist',
      },
      {
        name: 'Population Filter',
        description: 'Filter patients for evaluation',
        tip: 'Use date ranges to evaluate specific periods',
        icon: 'filter_list',
      },
      {
        name: 'Results View',
        description: 'Detailed evaluation results',
        tip: 'Export results for reporting or further analysis',
        icon: 'analytics',
      },
    ],
    tutorials: [
      {
        title: 'Running Your First Evaluation',
        url: '/help/tutorials/first-evaluation',
        duration: '7 min',
        type: 'video',
      },
      {
        title: 'Understanding CQL Measures',
        url: '/help/guides/cql-basics',
        duration: '15 min',
        type: 'article',
      },
    ],
    faq: [
      {
        question: 'Why does evaluation take so long?',
        answer:
          'Large patient populations require processing many clinical records. Use population filters to reduce evaluation time.',
      },
      {
        question: 'What if I get different results than expected?',
        answer:
          'Check the measure definition and value sets. Ensure patient data is complete and up-to-date in the FHIR repository.',
      },
    ],
    shortcuts: [
      { key: 'Ctrl+E', action: 'Run evaluation' },
      { key: 'Ctrl+S', action: 'Save results' },
    ],
  },
  insights: {
    pageId: 'insights',
    title: 'Population Health Insights',
    overview:
      'AI-powered insights about your patient population. Identify trends, at-risk patients, and improvement opportunities.',
    features: [
      {
        name: 'AI Insights',
        description: 'Machine learning-generated observations',
        tip: 'Insights are prioritized by potential impact',
        icon: 'psychology',
      },
      {
        name: 'Risk Pyramid',
        description: 'Population stratified by risk level',
        tip: 'Click each tier to see the patient list',
        icon: 'signal_cellular_alt',
      },
      {
        name: 'Trend Analysis',
        description: 'Quality metrics over time',
        tip: 'Look for seasonal patterns in care gaps',
        icon: 'trending_up',
      },
    ],
    tutorials: [
      {
        title: 'Using AI Insights Effectively',
        url: '/help/tutorials/ai-insights',
        duration: '10 min',
        type: 'video',
      },
    ],
    faq: [
      {
        question: 'How accurate are AI predictions?',
        answer:
          'Our AI models have been validated with 85%+ accuracy. Predictions should be used as guidance alongside clinical judgment.',
      },
      {
        question: 'Can I dismiss insights?',
        answer:
          'Yes, click the dismiss button on any insight. You can view dismissed insights from the filter menu.',
      },
    ],
    shortcuts: [
      { key: 'D', action: 'Dismiss selected insight' },
      { key: 'A', action: 'Take action on insight' },
    ],
  },
  reports: {
    pageId: 'reports',
    title: 'Reports & Analytics',
    overview:
      'Generate and customize quality reports. Track performance over time and prepare for audits.',
    features: [
      {
        name: 'Report Builder',
        description: 'Create custom reports',
        tip: 'Save report templates for recurring reports',
        icon: 'build',
      },
      {
        name: 'Export Options',
        description: 'PDF, Excel, and print formats',
        tip: 'Use PDF for formal submissions, Excel for data analysis',
        icon: 'download',
      },
      {
        name: 'Scheduled Reports',
        description: 'Automatic report delivery',
        tip: 'Set up weekly or monthly email delivery',
        icon: 'schedule',
      },
    ],
    tutorials: [
      {
        title: 'Creating Your First Report',
        url: '/help/tutorials/first-report',
        duration: '8 min',
        type: 'video',
      },
    ],
    faq: [
      {
        question: 'Can I share reports with other providers?',
        answer:
          'Yes, use the share feature to send reports to colleagues. They must have access to the same tenant.',
      },
    ],
    shortcuts: [
      { key: 'Ctrl+P', action: 'Print report' },
      { key: 'Ctrl+E', action: 'Export to Excel' },
    ],
  },
  'pre-visit': {
    pageId: 'pre-visit',
    title: 'Pre-Visit Planning',
    overview:
      'Prepare for upcoming patient visits. Review care gaps, recent results, and create visit agendas.',
    features: [
      {
        name: 'Visit Summary',
        description: 'Comprehensive patient overview',
        tip: 'Print summaries for paper-based workflows',
        icon: 'event_note',
      },
      {
        name: 'Care Gap Checklist',
        description: 'Gaps addressable during the visit',
        tip: 'Check off items as you discuss them with the patient',
        icon: 'checklist',
      },
      {
        name: 'Suggested Agenda',
        description: 'AI-generated visit topics',
        tip: 'Customize the agenda based on visit type',
        icon: 'format_list_bulleted',
      },
    ],
    tutorials: [
      {
        title: 'Effective Pre-Visit Planning',
        url: '/help/tutorials/pre-visit',
        duration: '6 min',
        type: 'video',
      },
    ],
    faq: [
      {
        question: 'How far in advance should I plan?',
        answer:
          'We recommend reviewing tomorrow\'s patients at the end of each day. Complex patients may benefit from earlier review.',
      },
    ],
    shortcuts: [
      { key: 'P', action: 'Print summary' },
      { key: 'N', action: 'Next patient' },
      { key: 'B', action: 'Previous patient' },
    ],
  },
  'measure-builder': {
    pageId: 'measure-builder',
    title: 'Custom Measure Builder',
    overview:
      'Create and manage custom quality measures. Use templates or AI assistance to build CQL-based measures.',
    features: [
      {
        name: 'Templates',
        description: 'Pre-built primary care measure templates',
        tip: 'Start with a template and customize to your needs',
        icon: 'content_copy',
      },
      {
        name: 'AI CQL Generation',
        description: 'Describe measures in plain English',
        tip: 'Be specific about criteria for better AI results',
        icon: 'smart_toy',
      },
      {
        name: 'Live Preview',
        description: 'Test measures against sample patients',
        tip: 'Preview helps validate logic before publishing',
        icon: 'preview',
      },
    ],
    tutorials: [
      {
        title: 'Building Custom Measures',
        url: '/help/tutorials/custom-measures',
        duration: '12 min',
        type: 'video',
      },
      {
        title: 'CQL Language Guide',
        url: '/help/guides/cql-reference',
        duration: '20 min',
        type: 'article',
      },
    ],
    faq: [
      {
        question: 'Do I need to know CQL?',
        answer:
          'No, you can use templates or AI generation. However, understanding basic CQL helps when customizing measures.',
      },
      {
        question: 'Can I share custom measures?',
        answer:
          'Yes, measures can be shared within your organization. Contact an administrator for cross-tenant sharing.',
      },
    ],
    shortcuts: [
      { key: 'Ctrl+Enter', action: 'Run preview' },
      { key: 'Ctrl+S', action: 'Save measure' },
    ],
  },
};

// Default help content for unknown pages
const DEFAULT_HELP: HelpContent = {
  pageId: 'default',
  title: 'Clinical Portal Help',
  overview:
    'Welcome to the Clinical Portal help system. Navigate to any page to see context-specific help.',
  features: [
    {
      name: 'Navigation',
      description: 'Use the sidebar to access different sections',
      tip: 'Press ? anywhere to open this help panel',
      icon: 'menu',
    },
    {
      name: 'Search',
      description: 'Search for patients, measures, or features',
      tip: 'Use Ctrl+F for quick access to search',
      icon: 'search',
    },
  ],
  tutorials: [
    {
      title: 'Clinical Portal Overview',
      url: '/help/tutorials/overview',
      duration: '5 min',
      type: 'video',
    },
  ],
  faq: [
    {
      question: 'How do I get started?',
      answer:
        'Start with the Provider Dashboard to see your care gaps and quality measures. Use the sidebar to navigate to other sections.',
    },
    {
      question: 'How do I contact support?',
      answer:
        'Click the "Contact Support" button in this panel or email support@healthdata.com.',
    },
  ],
  shortcuts: [
    { key: '?', action: 'Open help panel' },
    { key: 'Ctrl+F', action: 'Focus search' },
    { key: 'Escape', action: 'Close dialogs' },
  ],
};

@Component({
  selector: 'app-help-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatExpansionModule,
    MatTabsModule,
    MatListModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  template: `
    <div class="help-panel" [class.open]="isOpen" [@slidePanel]="isOpen ? 'open' : 'closed'">
      <!-- Header -->
      <div class="help-header">
        <div class="help-title">
          <mat-icon>help_outline</mat-icon>
          <h2>{{ helpContent.title }}</h2>
        </div>
        <button mat-icon-button (click)="close()" aria-label="Close help panel">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Content -->
      <div class="help-content">
        <!-- Overview -->
        <div class="help-overview">
          <p>{{ helpContent.overview }}</p>
        </div>

        <!-- Tabs -->
        <mat-tab-group animationDuration="200ms">
          <!-- Features Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>lightbulb</mat-icon>
              <span>Features</span>
            </ng-template>
            <div class="tab-content">
              @for (feature of helpContent.features; track feature.name) {
                <div class="feature-card">
                  <div class="feature-icon">
                    <mat-icon>{{ feature.icon || 'star' }}</mat-icon>
                  </div>
                  <div class="feature-info">
                    <h4>{{ feature.name }}</h4>
                    <p class="description">{{ feature.description }}</p>
                    <p class="tip">
                      <mat-icon>tips_and_updates</mat-icon>
                      {{ feature.tip }}
                    </p>
                  </div>
                </div>
              }
            </div>
          </mat-tab>

          <!-- Tutorials Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>play_circle</mat-icon>
              <span>Tutorials</span>
            </ng-template>
            <div class="tab-content">
              @for (tutorial of helpContent.tutorials; track tutorial.title) {
                <a class="tutorial-card" [href]="tutorial.url" target="_blank">
                  <mat-icon class="tutorial-type">
                    @switch (tutorial.type) {
                      @case ('video') { play_circle_filled }
                      @case ('article') { article }
                      @case ('guide') { menu_book }
                    }
                  </mat-icon>
                  <div class="tutorial-info">
                    <h4>{{ tutorial.title }}</h4>
                    <span class="duration">{{ tutorial.duration }}</span>
                  </div>
                  <mat-icon class="open-icon">open_in_new</mat-icon>
                </a>
              }
              @if (helpContent.tutorials.length === 0) {
                <p class="empty-state">No tutorials available for this page.</p>
              }
            </div>
          </mat-tab>

          <!-- FAQ Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>quiz</mat-icon>
              <span>FAQ</span>
            </ng-template>
            <div class="tab-content">
              <mat-accordion>
                @for (faq of helpContent.faq; track faq.question) {
                  <mat-expansion-panel>
                    <mat-expansion-panel-header>
                      <mat-panel-title>
                        {{ faq.question }}
                      </mat-panel-title>
                    </mat-expansion-panel-header>
                    <p>{{ faq.answer }}</p>
                  </mat-expansion-panel>
                }
              </mat-accordion>
              @if (helpContent.faq.length === 0) {
                <p class="empty-state">No FAQs available for this page.</p>
              }
            </div>
          </mat-tab>

          <!-- Shortcuts Tab -->
          @if (helpContent.shortcuts && helpContent.shortcuts.length > 0) {
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon>keyboard</mat-icon>
                <span>Shortcuts</span>
              </ng-template>
              <div class="tab-content">
                <div class="shortcuts-list">
                  @for (shortcut of helpContent.shortcuts; track shortcut.key) {
                    <div class="shortcut-item">
                      <kbd>{{ shortcut.key }}</kbd>
                      <span>{{ shortcut.action }}</span>
                    </div>
                  }
                </div>
              </div>
            </mat-tab>
          }
        </mat-tab-group>
      </div>

      <!-- Footer -->
      <div class="help-footer">
        <button mat-stroked-button (click)="openKnowledgeBase()">
          <mat-icon>menu_book</mat-icon>
          Knowledge Base
        </button>
        <button mat-flat-button color="primary" (click)="contactSupport()">
          <mat-icon>support_agent</mat-icon>
          Contact Support
        </button>
      </div>
    </div>

    <!-- Backdrop -->
    @if (isOpen) {
      <div class="help-backdrop" (click)="close()"></div>
    }
  `,
  styles: [`
    :host {
      display: contents;
    }

    .help-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.3);
      z-index: 999;
    }

    .help-panel {
      position: fixed;
      top: 0;
      right: 0;
      width: 400px;
      max-width: 90vw;
      height: 100vh;
      background: white;
      box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .help-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 20px;
      background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
      color: white;
    }

    .help-title {
      display: flex;
      align-items: center;
      gap: 12px;

      h2 {
        margin: 0;
        font-size: 18px;
        font-weight: 500;
      }

      mat-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
      }
    }

    .help-header button {
      color: white;
    }

    .help-content {
      flex: 1;
      overflow-y: auto;
      padding: 0;
    }

    .help-overview {
      padding: 16px 20px;
      background: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;

      p {
        margin: 0;
        color: rgba(0, 0, 0, 0.7);
        line-height: 1.5;
      }
    }

    .tab-content {
      padding: 16px 20px;
    }

    /* Features */
    .feature-card {
      display: flex;
      gap: 12px;
      padding: 12px;
      margin-bottom: 12px;
      background: #fafafa;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }

    .feature-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);
      color: white;
      flex-shrink: 0;
    }

    .feature-info {
      flex: 1;

      h4 {
        margin: 0 0 4px;
        font-size: 14px;
        font-weight: 600;
      }

      .description {
        margin: 0 0 8px;
        font-size: 13px;
        color: rgba(0, 0, 0, 0.7);
      }

      .tip {
        display: flex;
        align-items: flex-start;
        gap: 6px;
        margin: 0;
        padding: 8px;
        background: #fff8e1;
        border-radius: 4px;
        font-size: 12px;
        color: rgba(0, 0, 0, 0.7);

        mat-icon {
          font-size: 16px;
          width: 16px;
          height: 16px;
          color: #ff9800;
          flex-shrink: 0;
        }
      }
    }

    /* Tutorials */
    .tutorial-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      margin-bottom: 8px;
      background: #fafafa;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
      text-decoration: none;
      color: inherit;
      transition: all 0.2s ease;

      &:hover {
        background: #e3f2fd;
        border-color: #1976d2;
      }

      .tutorial-type {
        color: #1976d2;
        font-size: 32px;
        width: 32px;
        height: 32px;
      }

      .tutorial-info {
        flex: 1;

        h4 {
          margin: 0 0 2px;
          font-size: 14px;
        }

        .duration {
          font-size: 12px;
          color: rgba(0, 0, 0, 0.6);
        }
      }

      .open-icon {
        color: rgba(0, 0, 0, 0.4);
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
    }

    /* FAQ */
    mat-accordion {
      display: block;

      mat-expansion-panel {
        margin-bottom: 8px;
        border-radius: 8px !important;
        box-shadow: none !important;
        border: 1px solid #e0e0e0;

        &.mat-expanded {
          border-color: #1976d2;
        }
      }

      mat-panel-title {
        font-size: 14px;
        font-weight: 500;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: rgba(0, 0, 0, 0.7);
        line-height: 1.5;
      }
    }

    /* Shortcuts */
    .shortcuts-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .shortcut-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 12px;
      background: #fafafa;
      border-radius: 6px;

      kbd {
        display: inline-block;
        padding: 4px 8px;
        background: #e0e0e0;
        border-radius: 4px;
        font-family: monospace;
        font-size: 12px;
        font-weight: 600;
        min-width: 60px;
        text-align: center;
      }

      span {
        font-size: 13px;
        color: rgba(0, 0, 0, 0.8);
      }
    }

    /* Empty state */
    .empty-state {
      text-align: center;
      color: rgba(0, 0, 0, 0.5);
      padding: 24px;
      font-style: italic;
    }

    /* Footer */
    .help-footer {
      display: flex;
      gap: 12px;
      padding: 16px 20px;
      border-top: 1px solid #e0e0e0;
      background: #fafafa;

      button {
        flex: 1;
      }
    }

    /* Tab styling */
    ::ng-deep .mat-mdc-tab-labels {
      padding: 0 8px;
    }

    ::ng-deep .mat-mdc-tab {
      min-width: 80px !important;
      padding: 0 12px !important;
    }

    ::ng-deep .mat-mdc-tab .mdc-tab__content {
      gap: 6px;
    }

    ::ng-deep .mat-mdc-tab mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    /* Responsive */
    @media (max-width: 480px) {
      .help-panel {
        width: 100vw;
        max-width: none;
      }
    }
  `],
  animations: [
    trigger('slidePanel', [
      state('closed', style({ transform: 'translateX(100%)' })),
      state('open', style({ transform: 'translateX(0)' })),
      transition('closed <=> open', animate('250ms ease-in-out')),
    ]),
  ],
})
export class HelpPanelComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly router = inject(Router);

  @Input() isOpen = false;

  helpContent: HelpContent = DEFAULT_HELP;

  ngOnInit(): void {
    // Update help content based on current route
    this.updateHelpContent(this.router.url);

    // Listen for route changes
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event) => {
        this.updateHelpContent((event as NavigationEnd).urlAfterRedirects);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  close(): void {
    this.isOpen = false;
  }

  open(): void {
    this.isOpen = true;
  }

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  openKnowledgeBase(): void {
    window.open('/help/knowledge-base', '_blank');
  }

  contactSupport(): void {
    window.open('mailto:support@healthdata.com?subject=Clinical Portal Support Request', '_blank');
  }

  private updateHelpContent(url: string): void {
    // Extract page ID from URL
    const path = url.split('?')[0].split('/').filter(Boolean)[0] || 'dashboard';

    // Map routes to help content
    const pageId = this.mapRouteToPageId(path);
    this.helpContent = HELP_CONTENT[pageId] || DEFAULT_HELP;
  }

  private mapRouteToPageId(path: string): string {
    const routeMap: Record<string, string> = {
      '': 'dashboard',
      'dashboard': 'dashboard',
      'patients': 'patients',
      'evaluations': 'evaluations',
      'results': 'evaluations',
      'insights': 'insights',
      'reports': 'reports',
      'report-builder': 'reports',
      'pre-visit': 'pre-visit',
      'measure-builder': 'measure-builder',
      'visualization': 'dashboard',
      'agent-builder': 'dashboard',
    };
    return routeMap[path] || path;
  }
}
