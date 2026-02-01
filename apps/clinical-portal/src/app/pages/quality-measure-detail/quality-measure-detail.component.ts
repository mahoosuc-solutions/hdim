import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { LoggerService } from '../../services/logger.service';

/**
 * Quality Measure Model
 */
interface QualityMeasure {
  id: string;
  code: string;
  name: string;
  description: string;
  category: string;
  measureType: string;
  steward: string;
  starRating: number;
  status: 'Active' | 'Draft' | 'Retired';
  lastEvaluated: string | null;
  denominator?: number;
  numerator?: number;
  rate?: number;
  benchmark?: number;
  careGaps?: number;
  // Additional details for detail view
  purpose?: string;
  improvementNotation?: string;
  riskAdjustment?: string;
  rationale?: string;
  clinicalRecommendation?: string;
  definitions?: string;
}

/**
 * Patient Evaluation Result
 */
interface PatientResult {
  patientId: string;
  patientName: string;
  mrn: string;
  compliant: boolean;
  evaluationDate: string;
  denominatorInclusion: boolean;
  numeratorInclusion: boolean;
  exclusionReason?: string;
  details?: string;
}

/**
 * Care Gap for Measure
 */
interface CareGap {
  gapId: string;
  patientId: string;
  patientName: string;
  mrn: string;
  gapType: string;
  gapDescription: string;
  daysOverdue: number;
  urgency: 'high' | 'medium' | 'low';
  dueDate: string;
}

/**
 * Historical Trend Data Point
 */
interface TrendDataPoint {
  date: string;
  rate: number;
  numerator: number;
  denominator: number;
}

/**
 * Quality Measure Detail Component
 *
 * Displays detailed information about a quality measure including:
 * - Overview (description, category, steward, star rating)
 * - Evaluation Results (patient-level results table)
 * - Care Gaps (linked care gaps for this measure)
 * - Historical Trends (rate over time chart)
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - PHI displayed with proper context
 *
 * Accessibility:
 * - ARIA labels on all sections
 * - Keyboard navigation support
 * - Tab-based navigation for content sections
 *
 * Sprint 1 - Issue #242: Quality Measure Viewer
 */
@Component({
  selector: 'app-quality-measure-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatChipsModule,
    MatTableModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatDividerModule,
    MatBadgeModule,
    LoadingButtonComponent,
    LoadingOverlayComponent,
  ],
  templateUrl: './quality-measure-detail.component.html',
  styleUrls: ['./quality-measure-detail.component.scss'],
})
export class QualityMeasureDetailComponent implements OnInit {
  measureId: string | null = null;
  measure: QualityMeasure | null = null;
  loading = true;
  error: string | null = null;

  // Evaluation results
  patientResults: PatientResult[] = [];
  resultsColumns: string[] = ['patientName', 'mrn', 'compliant', 'evaluationDate', 'details', 'actions'];

  // Care gaps
  careGaps: CareGap[] = [];
  careGapsColumns: string[] = ['patientName', 'mrn', 'gapType', 'gapDescription', 'daysOverdue', 'urgency', 'actions'];

  // Historical trends
  historicalTrends: TrendDataPoint[] = [];

  // Button loading states
  backButtonLoading = false;


  // HEDIS Measures Database (matches quality-measures.component.ts)
  private hedisMeasures: QualityMeasure[] = [
    {
      id: 'BCS',
      code: 'BCS',
      name: 'Breast Cancer Screening',
      description: 'Women aged 50-74 who had a mammogram to screen for breast cancer in the past 27 months.',
      category: 'screening',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 5,
      status: 'Active',
      lastEvaluated: '2025-12-15',
      denominator: 1243,
      numerator: 921,
      rate: 74.1,
      benchmark: 74.2,
      careGaps: 322,
      purpose: 'To identify the percentage of women who have received appropriate breast cancer screening.',
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Breast cancer is one of the most common types of cancer among women. Screening can detect breast cancer at an early stage when it is easier to treat.',
      clinicalRecommendation: 'USPSTF recommends biennial screening mammography for women aged 50-74 years.',
      definitions: 'Mammogram: Imaging study to screen for breast cancer using low-dose x-rays.'
    },
    {
      id: 'COL',
      code: 'COL',
      name: 'Colorectal Cancer Screening',
      description: 'Adults aged 45-75 who had appropriate screening for colorectal cancer.',
      category: 'screening',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 5,
      status: 'Active',
      lastEvaluated: '2025-12-15',
      denominator: 2156,
      numerator: 1563,
      rate: 72.5,
      benchmark: 72.5,
      careGaps: 593,
      purpose: 'To identify the percentage of members who had appropriate screening for colorectal cancer.',
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Colorectal cancer is the third most common cancer and the second leading cause of cancer deaths. Early detection through screening can prevent most colorectal cancer deaths.',
      clinicalRecommendation: 'USPSTF recommends screening for colorectal cancer starting at age 45 years and continuing until age 75 years.',
      definitions: 'Appropriate screening includes colonoscopy, FIT, or other approved methods.'
    },
    {
      id: 'CBP',
      code: 'CBP',
      name: 'Controlling High Blood Pressure',
      description: 'Adults aged 18-85 with a diagnosis of hypertension whose blood pressure was adequately controlled.',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 4,
      status: 'Active',
      lastEvaluated: '2025-12-15',
      denominator: 1876,
      numerator: 1281,
      rate: 68.3,
      benchmark: 68.3,
      careGaps: 595,
      purpose: 'To identify the percentage of members with hypertension whose blood pressure was adequately controlled during the measurement year.',
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Controlling blood pressure prevents heart attacks, strokes, and kidney disease.',
      clinicalRecommendation: 'Target blood pressure <140/90 mmHg for most adults with hypertension.',
      definitions: 'Adequate control: Most recent BP reading <140/90 mmHg during the measurement year.'
    },
    {
      id: 'CDC',
      code: 'CDC',
      name: 'Comprehensive Diabetes Care - HbA1c Control',
      description: 'Adults aged 18-75 with diabetes whose HbA1c was at the controlled level (<8.0%).',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 5,
      status: 'Active',
      lastEvaluated: '2025-12-15',
      denominator: 987,
      numerator: 579,
      rate: 58.7,
      benchmark: 58.7,
      careGaps: 408,
      purpose: 'To identify the percentage of members with diabetes whose HbA1c was adequately controlled.',
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Maintaining good glycemic control prevents or delays complications of diabetes.',
      clinicalRecommendation: 'Target HbA1c <8.0% for most adults with diabetes.',
      definitions: 'HbA1c: Hemoglobin A1c test measuring average blood glucose over past 2-3 months.'
    },
    {
      id: 'EED',
      code: 'EED',
      name: 'Eye Exam for Patients with Diabetes',
      description: 'Adults aged 18-75 with diabetes who had a retinal eye exam.',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 4,
      status: 'Active',
      lastEvaluated: '2025-12-15',
      denominator: 987,
      numerator: 662,
      rate: 67.1,
      benchmark: 67.1,
      careGaps: 325,
      purpose: 'To identify the percentage of members with diabetes who had a retinal eye exam.',
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Diabetic retinopathy is a leading cause of blindness. Early detection and treatment can prevent vision loss.',
      clinicalRecommendation: 'Annual dilated eye examination or retinal photography for all patients with diabetes.',
      definitions: 'Retinal exam: Dilated eye exam or retinal photography by an eye care professional.'
    },
    {
      id: 'SPC',
      code: 'SPC',
      name: 'Statin Therapy for Patients with Cardiovascular Disease',
      description: 'Adults aged 21-75 with cardiovascular disease who were prescribed statin therapy.',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 3,
      status: 'Active',
      lastEvaluated: '2025-12-15',
      denominator: 1452,
      numerator: 1196,
      rate: 82.4,
      benchmark: 82.4,
      careGaps: 256,
      purpose: 'To identify the percentage of members with cardiovascular disease who were prescribed statin therapy.',
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Statin therapy reduces the risk of heart attack and stroke in patients with cardiovascular disease.',
      clinicalRecommendation: 'High-intensity statin therapy for adults aged 21-75 with cardiovascular disease.',
      definitions: 'Cardiovascular disease: History of MI, coronary revascularization, or atherosclerotic cardiovascular disease.'
    }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private logger: LoggerService
  ) {
  }

  ngOnInit(): void {
    this.measureId = this.route.snapshot.paramMap.get('id');

    if (!this.measureId) {
      this.error = 'No measure ID provided';
      this.loading = false;
      return;
    }

    this.loadMeasureData();
  }

  /**
   * Load measure data from local database
   * In production, this would call the backend API
   */
  private loadMeasureData(): void {
    this.loading = true;
    this.logger.info('Loading quality measure details', this.measureId);

    // Simulate API call delay
    setTimeout(() => {
      // Find measure by ID or code
      this.measure = this.hedisMeasures.find(
        m => m.id === this.measureId || m.code === this.measureId
      ) || null;

      if (!this.measure) {
        this.error = `Quality measure "${this.measureId}" not found`;
        this.loading = false;
        return;
      }

      // Load patient results
      this.loadPatientResults();

      // Load care gaps
      this.loadCareGaps();

      // Load historical trends
      this.loadHistoricalTrends();

      this.loading = false;
    }, 500);
  }

  /**
   * Load patient evaluation results
   */
  private loadPatientResults(): void {
    if (!this.measure) return;

    // Generate sample patient results
    const sampleResults: PatientResult[] = [
      {
        patientId: '1',
        patientName: 'Smith, Sarah',
        mrn: 'MRN001',
        compliant: true,
        evaluationDate: '2025-12-15',
        denominatorInclusion: true,
        numeratorInclusion: true,
        details: 'Mammogram performed 2025-10-15'
      },
      {
        patientId: '2',
        patientName: 'Martinez, Elena',
        mrn: 'MRN002',
        compliant: false,
        evaluationDate: '2025-12-15',
        denominatorInclusion: true,
        numeratorInclusion: false,
        details: 'No screening on record'
      },
      {
        patientId: '3',
        patientName: 'Johnson, Robert',
        mrn: 'MRN003',
        compliant: false,
        evaluationDate: '2025-12-15',
        denominatorInclusion: true,
        numeratorInclusion: false,
        details: 'Last screening 11 years ago'
      }
    ];

    this.patientResults = sampleResults;
  }

  /**
   * Load care gaps for this measure
   */
  private loadCareGaps(): void {
    if (!this.measure) return;

    // Generate sample care gaps
    const sampleGaps: CareGap[] = [
      {
        gapId: 'gap-1',
        patientId: '2',
        patientName: 'Martinez, Elena',
        mrn: 'MRN002',
        gapType: 'screening',
        gapDescription: 'Mammogram needed - No screening on record',
        daysOverdue: 180,
        urgency: 'high',
        dueDate: '2025-06-01'
      },
      {
        gapId: 'gap-2',
        patientId: '3',
        patientName: 'Johnson, Robert',
        mrn: 'MRN003',
        gapType: 'screening',
        gapDescription: 'Colonoscopy overdue - Last screening 11 years ago',
        daysOverdue: 365,
        urgency: 'high',
        dueDate: '2024-12-01'
      }
    ];

    this.careGaps = sampleGaps;
  }

  /**
   * Load historical trend data
   */
  private loadHistoricalTrends(): void {
    if (!this.measure) return;

    // Generate sample trend data (6 months)
    const trends: TrendDataPoint[] = [];
    const currentRate = this.measure.rate || 70;

    for (let i = 5; i >= 0; i--) {
      const date = new Date();
      date.setMonth(date.getMonth() - i);

      trends.push({
        date: date.toISOString().split('T')[0],
        rate: currentRate + (Math.random() * 4 - 2), // +/- 2% variance
        numerator: Math.floor((this.measure.numerator || 900) * (0.95 + Math.random() * 0.1)),
        denominator: this.measure.denominator || 1200
      });
    }

    this.historicalTrends = trends;
  }

  /**
   * Navigate back to quality measures list
   */
  goBack(): void {
    this.backButtonLoading = true;
    this.router.navigate(['/quality-measures']);
  }

  /**
   * Navigate to patient detail with context
   */
  viewPatientDetail(patientId: string): void {
    this.logger.info('Navigating to patient detail', patientId);
    this.router.navigate(['/patients', patientId], {
      queryParams: {
        tab: 'results',
        source: 'quality-measure-detail'
      }
    });
  }

  /**
   * Navigate to care gaps dashboard filtered by this measure
   */
  viewCareGaps(): void {
    this.logger.info('Navigating to care gaps filtered by measure', this.measure?.code);
    this.router.navigate(['/care-gaps'], {
      queryParams: {
        measure: this.measure?.code,
        source: 'quality-measure-detail'
      }
    });
  }

  /**
   * Get star rating display
   */
  getStarRatingArray(): number[] {
    return Array(this.measure?.starRating || 0).fill(0);
  }

  /**
   * Get status color
   */
  getStatusColor(): string {
    return this.measure?.status === 'Active' ? 'primary' : 'warn';
  }

  /**
   * Get compliance status color
   */
  getComplianceColor(compliant: boolean): string {
    return compliant ? 'primary' : 'warn';
  }

  /**
   * Get urgency badge class
   */
  getUrgencyClass(urgency: string): string {
    return `urgency-badge urgency-${urgency}`;
  }

  /**
   * Calculate rate percentage
   */
  getRatePercentage(): number {
    if (!this.measure || !this.measure.rate) return 0;
    return Math.round(this.measure.rate * 10) / 10;
  }

  /**
   * Get gap to benchmark
   */
  getGapToBenchmark(): number {
    if (!this.measure || !this.measure.rate || !this.measure.benchmark) return 0;
    return Math.round((this.measure.benchmark - this.measure.rate) * 10) / 10;
  }
}
