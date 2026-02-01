import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { trigger, state, style, transition, animate } from '@angular/animations';

/**
 * Event Processing Explainer Component
 *
 * Educational component that explains HDIM's event-driven data processing architecture
 * to clinical users in accessible, healthcare-oriented language.
 *
 * Key Features:
 * - Interactive visual flow diagram
 * - Clinical use case examples (patient registration, care gap detection)
 * - HIPAA audit trail explanation
 * - Real-time vs. batch processing comparison
 *
 * Target Audience: Clinical staff, quality officers, healthcare administrators
 */
@Component({
  selector: 'app-event-processing-explainer',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatExpansionModule,
    MatTabsModule,
    MatTooltipModule,
    MatDividerModule,
    MatChipsModule,
  ],
  templateUrl: './event-processing-explainer.component.html',
  styleUrls: ['./event-processing-explainer.component.scss'],
  animations: [
    trigger('eventFlow', [
      state('inactive', style({
        opacity: 0.5,
        transform: 'scale(0.95)'
      })),
      state('active', style({
        opacity: 1,
        transform: 'scale(1)'
      })),
      transition('inactive => active', animate('300ms ease-in')),
      transition('active => inactive', animate('300ms ease-out'))
    ])
  ]
})
export class EventProcessingExplainerComponent {
  /**
   * Clinical workflow examples with event mappings
   */
  clinicalWorkflows = [
    {
      title: 'Quality Measure Evaluation',
      icon: 'analytics',
      description: 'How HDIM evaluates HEDIS quality measures for patient populations',
      steps: [
        {
          step: 1,
          clinical: 'Administrator requests quality measure evaluation (e.g., HbA1c control for diabetic patients)',
          technical: 'EvaluationRequestedEvent emitted with measure ID and cohort criteria',
          icon: 'assignment',
          color: '#4CAF50'
        },
        {
          step: 2,
          clinical: 'System identifies eligible patient cohort (all diabetic patients aged 18-75)',
          technical: 'CohortIdentifiedEvent published with 1,500 patient IDs',
          icon: 'group',
          color: '#2196F3'
        },
        {
          step: 3,
          clinical: 'CQL engine evaluates clinical logic for each patient (HbA1c < 8% in last year?)',
          technical: 'BatchEvaluationStartedEvent → 1,500 PatientEvaluatedEvents emitted in parallel',
          icon: 'code',
          color: '#FF9800'
        },
        {
          step: 4,
          clinical: 'Results aggregated: 1,200 compliant (80%), 300 non-compliant (20%)',
          technical: 'EvaluationCompletedEvent with compliance rate and stratifications',
          icon: 'pie_chart',
          color: '#9C27B0'
        },
        {
          step: 5,
          clinical: 'Care gaps identified for 300 non-compliant patients',
          technical: '300 CareGapIdentifiedEvents published to care gap service',
          icon: 'report_problem',
          color: '#F44336'
        },
        {
          step: 6,
          clinical: 'Provider dashboard updated with real-time compliance scores',
          technical: 'DashboardRefreshedEvent triggers UI update via WebSocket',
          icon: 'dashboard',
          color: '#00BCD4'
        }
      ]
    },
    {
      title: 'Patient Registration',
      icon: 'person_add',
      description: 'When a new patient joins your practice',
      steps: [
        {
          step: 1,
          clinical: 'Front desk registers patient in EHR',
          technical: 'PatientCreatedEvent emitted',
          icon: 'edit_note',
          color: '#4CAF50'
        },
        {
          step: 2,
          clinical: 'Demographics verified and updated',
          technical: 'PatientUpdatedEvent processed',
          icon: 'verified_user',
          color: '#2196F3'
        },
        {
          step: 3,
          clinical: 'Insurance eligibility checked',
          technical: 'CoverageValidatedEvent triggered',
          icon: 'fact_check',
          color: '#FF9800'
        },
        {
          step: 4,
          clinical: 'Quality measures automatically calculated',
          technical: 'EvaluationScheduledEvent dispatched',
          icon: 'analytics',
          color: '#9C27B0'
        }
      ]
    },
    {
      title: 'Care Gap Detection',
      icon: 'medical_information',
      description: 'Identifying patients who need preventive care',
      steps: [
        {
          step: 1,
          clinical: 'Quality measure evaluation runs',
          technical: 'EvaluationCompletedEvent emitted',
          icon: 'assignment_turned_in',
          color: '#4CAF50'
        },
        {
          step: 2,
          clinical: 'Care gap identified (e.g., overdue mammogram)',
          technical: 'CareGapIdentifiedEvent published',
          icon: 'report_problem',
          color: '#FF5722'
        },
        {
          step: 3,
          clinical: 'Patient added to outreach list',
          technical: 'OutreachCampaignUpdatedEvent processed',
          icon: 'campaign',
          color: '#2196F3'
        },
        {
          step: 4,
          clinical: 'Provider notified via dashboard',
          technical: 'NotificationSentEvent recorded',
          icon: 'notifications_active',
          color: '#FF9800'
        }
      ]
    },
    {
      title: 'Quality Report Generation',
      icon: 'assessment',
      description: 'Creating HEDIS quality reports for payers',
      steps: [
        {
          step: 1,
          clinical: 'Administrator requests annual HEDIS report',
          technical: 'ReportGenerationRequestedEvent emitted',
          icon: 'play_arrow',
          color: '#4CAF50'
        },
        {
          step: 2,
          clinical: 'System evaluates 10,000+ patients',
          technical: 'BatchEvaluationCompletedEvent published',
          icon: 'sync',
          color: '#2196F3'
        },
        {
          step: 3,
          clinical: 'Results aggregated by measure',
          technical: 'AggregationCompletedEvent processed',
          icon: 'functions',
          color: '#FF9800'
        },
        {
          step: 4,
          clinical: 'PDF report generated and downloaded',
          technical: 'ReportGeneratedEvent recorded',
          icon: 'download',
          color: '#9C27B0'
        }
      ]
    }
  ];

  /**
   * Architecture benefits for healthcare context
   */
  benefits = [
    {
      title: 'Complete Audit Trail',
      icon: 'history',
      description: 'Every clinical action is recorded with timestamp and user, ensuring HIPAA compliance and enabling full traceability for audits.',
      examples: [
        'Who updated patient demographics and when',
        'Which evaluation triggered a care gap alert',
        'How quality measure scores changed over time'
      ],
      compliance: 'HIPAA § 164.312(b) - Audit Controls'
    },
    {
      title: 'Real-Time Updates',
      icon: 'update',
      description: 'Clinical data changes propagate instantly across the system, keeping all dashboards and reports synchronized.',
      examples: [
        'Care gap dashboard updates when patient visit closes gap',
        'Quality measure scores recalculate immediately',
        'Provider leaderboards refresh in real-time'
      ],
      compliance: null
    },
    {
      title: 'Data Integrity',
      icon: 'shield',
      description: 'Events are immutable - once recorded, they cannot be altered. This prevents data corruption and maintains clinical accuracy.',
      examples: [
        'Original patient data preserved even after updates',
        'Evaluation results cannot be retroactively changed',
        'Care gap closure timestamps are permanent'
      ],
      compliance: 'HIPAA § 164.312(c)(1) - Integrity Controls'
    },
    {
      title: 'System Resilience',
      icon: 'sync_problem',
      description: 'If a service fails, events are queued and processed when it recovers. No data is lost.',
      examples: [
        'Report generation continues after temporary outage',
        'Care gap alerts queue during maintenance',
        'Quality evaluations resume after service restart'
      ],
      compliance: null
    }
  ];

  /**
   * Technical vs. Traditional comparison
   */
  comparisonData = {
    traditional: {
      title: 'Traditional Database',
      icon: 'storage',
      description: 'Stores only current state',
      limitations: [
        'Limited audit history',
        'Difficult to trace changes',
        'Data overwrites previous values',
        'Hard to debug issues'
      ]
    },
    eventDriven: {
      title: 'Event-Driven Architecture',
      icon: 'timeline',
      description: 'Stores complete history of changes',
      advantages: [
        'Full audit trail preserved',
        'Time-travel debugging possible',
        'Data never deleted, only appended',
        'Easy to replay and reconstruct state'
      ]
    }
  };

  /**
   * Quality measures processing deep dive
   */
  qualityMeasuresDeepDive = {
    title: 'How Quality Measures Are Evaluated',
    description: 'Behind the scenes of HEDIS measure evaluation using event-driven processing',
    stages: [
      {
        name: 'Cohort Identification',
        icon: 'filter_list',
        color: '#4CAF50',
        description: 'System identifies which patients qualify for the measure',
        technicalDetails: [
          'CQL initial population criteria evaluated against patient database',
          'Demographic filters (age, gender) applied first for performance',
          'Diagnosis codes (ICD-10) matched against value sets',
          'Enrollment periods validated (continuous 12-month eligibility)'
        ],
        events: [
          'CohortIdentificationStartedEvent',
          'PatientEligibilityEvaluatedEvent (per patient)',
          'CohortIdentifiedEvent (with final patient list)'
        ],
        example: 'For diabetes HbA1c measure: Find all patients aged 18-75 with diabetes diagnosis (E10.*, E11.*) enrolled for 12 consecutive months'
      },
      {
        name: 'Clinical Data Retrieval',
        icon: 'assignment_turned_in',
        color: '#2196F3',
        description: 'Gather all relevant clinical data for each eligible patient',
        technicalDetails: [
          'FHIR resources fetched: Observations (labs), Conditions (diagnoses), Medications, Encounters',
          'Date range filtering based on measurement period (typically past 12 months)',
          'Data standardization (LOINC codes for labs, RxNorm for medications)',
          'Cached results reused when patient data hasn\'t changed'
        ],
        events: [
          'DataRetrievalRequestedEvent (per patient)',
          'FHIRResourceFetchedEvent (per resource type)',
          'DataRetrievalCompletedEvent (with aggregated data)'
        ],
        example: 'Retrieve all HbA1c lab results (LOINC: 4548-4) from past 12 months for 1,500 diabetic patients'
      },
      {
        name: 'CQL Logic Evaluation',
        icon: 'code',
        color: '#FF9800',
        description: 'Execute clinical quality logic for each patient',
        technicalDetails: [
          'CQL expression parsed and compiled into execution plan',
          'Measure-specific logic executed (e.g., "Most recent HbA1c < 8%")',
          'Date-based calculations (e.g., "within 12 months before end of measurement period")',
          'Boolean logic evaluated (AND/OR conditions, exclusions)'
        ],
        events: [
          'CQLEvaluationStartedEvent (per patient)',
          'CQLExpressionEvaluatedEvent (per CQL statement)',
          'PatientEvaluationCompletedEvent (with compliant/non-compliant result)'
        ],
        example: 'For each patient: Check if most recent HbA1c value < 8.0% AND occurred within measurement period'
      },
      {
        name: 'Results Aggregation',
        icon: 'functions',
        color: '#9C27B0',
        description: 'Combine individual patient results into population-level metrics',
        technicalDetails: [
          'Numerator count (patients meeting quality criteria)',
          'Denominator count (all eligible patients)',
          'Compliance rate calculation (numerator / denominator * 100)',
          'Stratifications by demographics (age groups, gender, race/ethnicity)'
        ],
        events: [
          'AggregationStartedEvent',
          'StratificationCalculatedEvent (per demographic group)',
          'MeasureResultCalculatedEvent (final compliance rate)'
        ],
        example: 'Numerator: 1,200 patients with HbA1c < 8% | Denominator: 1,500 eligible patients | Rate: 80% compliance'
      },
      {
        name: 'Care Gap Generation',
        icon: 'report_problem',
        color: '#F44336',
        description: 'Identify patients who need follow-up actions',
        technicalDetails: [
          'Non-compliant patients extracted (denominator - numerator)',
          'Gap reason determined (e.g., "No HbA1c test in past year" vs. "HbA1c > 8%")',
          'Priority scoring (days overdue, clinical risk factors)',
          'Outreach recommendations generated (schedule lab, adjust medication)'
        ],
        events: [
          'CareGapIdentifiedEvent (per non-compliant patient)',
          'CareGapPrioritizedEvent (with urgency score)',
          'OutreachRecommendationGeneratedEvent'
        ],
        example: '300 patients without HbA1c test in past year → Priority 1 (overdue >90 days) vs. Priority 2 (overdue 30-90 days)'
      },
      {
        name: 'Real-Time Dashboard Update',
        icon: 'dashboard',
        color: '#00BCD4',
        description: 'Push results to provider dashboards via WebSocket',
        technicalDetails: [
          'Projection updates (pre-aggregated read models for fast queries)',
          'WebSocket notifications sent to connected clients',
          'Dashboard widgets auto-refresh with new compliance rates',
          'Provider leaderboards updated in real-time'
        ],
        events: [
          'ProjectionUpdatedEvent',
          'DashboardRefreshRequestedEvent',
          'NotificationSentEvent (to connected users)'
        ],
        example: 'Provider dashboard shows updated HbA1c compliance: 80% (up from 78% last month) with green trend indicator'
      }
    ],
    performance: {
      title: 'Performance Characteristics',
      metrics: [
        { label: 'Cohort Identification', value: '~2-5 seconds for 10,000 patients', icon: 'speed' },
        { label: 'Data Retrieval', value: '~10-20 seconds for 1,500 patients', icon: 'cloud_download' },
        { label: 'CQL Evaluation', value: '~30-60 seconds for 1,500 patients (parallel)', icon: 'rocket_launch' },
        { label: 'Results Aggregation', value: '~1-2 seconds', icon: 'functions' },
        { label: 'Total Evaluation Time', value: '~45-90 seconds for full measure', icon: 'timer' }
      ],
      optimizations: [
        'Parallel patient evaluation (100 patients evaluated simultaneously)',
        'Redis caching of FHIR resources (30-second TTL for active evaluations)',
        'Pre-compiled CQL execution plans (compiled once, executed many times)',
        'Projection-based queries (no event replay for read operations)'
      ]
    }
  };

  /**
   * Real-world analogy for non-technical users
   */
  analogy = {
    title: 'Think of it Like a Medical Chart',
    traditional: {
      description: 'Traditional Database = Single Page Summary',
      example: 'Only shows current diagnosis, medications, and vital signs. Previous values are lost when updated.'
    },
    eventDriven: {
      description: 'Event-Driven System = Complete Medical Record',
      example: 'Preserves every visit note, lab result, and medication change with timestamp. You can see the full patient journey.'
    }
  };

  /**
   * Active workflow index for animation
   */
  activeWorkflowIndex = 0;

  /**
   * Active step within workflow
   */
  activeStepIndex = 0;

  /**
   * Animation interval
   */
  private animationInterval: any;

  ngOnInit(): void {
    this.startAnimation();
  }

  ngOnDestroy(): void {
    this.stopAnimation();
  }

  /**
   * Start the workflow animation
   */
  startAnimation(): void {
    this.animationInterval = setInterval(() => {
      const currentWorkflow = this.clinicalWorkflows[this.activeWorkflowIndex];
      this.activeStepIndex++;

      if (this.activeStepIndex >= currentWorkflow.steps.length) {
        this.activeStepIndex = 0;
        this.activeWorkflowIndex = (this.activeWorkflowIndex + 1) % this.clinicalWorkflows.length;
      }
    }, 3000); // 3 seconds per step
  }

  /**
   * Stop the workflow animation
   */
  stopAnimation(): void {
    if (this.animationInterval) {
      clearInterval(this.animationInterval);
    }
  }

  /**
   * Select a specific workflow
   */
  selectWorkflow(index: number): void {
    this.stopAnimation();
    this.activeWorkflowIndex = index;
    this.activeStepIndex = 0;
    this.startAnimation();
  }

  /**
   * Check if step is active
   */
  isStepActive(workflowIndex: number, stepIndex: number): boolean {
    return workflowIndex === this.activeWorkflowIndex && stepIndex === this.activeStepIndex;
  }

  /**
   * Get step animation state
   */
  getStepState(workflowIndex: number, stepIndex: number): string {
    return this.isStepActive(workflowIndex, stepIndex) ? 'active' : 'inactive';
  }
}
