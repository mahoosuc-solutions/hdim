import { Component, OnInit, inject } from '@angular/core';
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
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, map, of } from 'rxjs';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { LoggerService } from '../../services/logger.service';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { API_CONFIG } from '../../config/api.config';
import { MeasureInfo } from '../../models/cql-library.model';
import { QualityMeasureResult } from '../../models/quality-result.model';

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
  purpose?: string;
  improvementNotation?: string;
  riskAdjustment?: string;
  rationale?: string;
  clinicalRecommendation?: string;
  definitions?: string;
}

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

interface TrendDataPoint {
  date: string;
  rate: number;
  numerator: number;
  denominator: number;
}

interface CareGapApiResponse {
  content: CareGapApiItem[];
}

interface CareGapApiItem {
  id?: string;
  patientId: string;
  measureId: string;
  gapDescription?: string;
  priority?: string;
  gapStatus?: string;
  dueDate?: string;
}

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

  patientResults: PatientResult[] = [];
  resultsColumns: string[] = ['patientName', 'mrn', 'compliant', 'evaluationDate', 'details', 'actions'];

  careGaps: CareGap[] = [];
  careGapsColumns: string[] = ['patientName', 'mrn', 'gapType', 'gapDescription', 'daysOverdue', 'urgency', 'actions'];

  historicalTrends: TrendDataPoint[] = [];

  backButtonLoading = false;

  private http = inject(HttpClient);
  private measureService = inject(MeasureService);
  private evaluationService = inject(EvaluationService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.measureId = this.route.snapshot.paramMap.get('id');

    if (!this.measureId) {
      this.error = 'No measure ID provided';
      this.loading = false;
      return;
    }

    this.loadMeasureData();
  }

  private loadMeasureData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      measures: this.measureService.getLocalMeasuresAsInfo().pipe(catchError(() => of([] as MeasureInfo[]))),
      results: this.evaluationService.getAllResults(0, 5000).pipe(catchError(() => of([] as QualityMeasureResult[]))),
      careGaps: this.http
        .get<CareGapApiResponse>(`${API_CONFIG.CARE_GAP_URL}/api/v1/care-gaps`, {
          headers: { 'X-Tenant-ID': API_CONFIG.DEFAULT_TENANT_ID },
        })
        .pipe(
          map((resp) => resp.content || []),
          catchError(() => of([] as CareGapApiItem[]))
        ),
    }).subscribe({
      next: ({ measures, results, careGaps }) => {
        const key = (this.measureId || '').toUpperCase();
        const measureInfo = measures.find((m) => m.id.toUpperCase() === key || m.name.toUpperCase() === key);

        if (!measureInfo) {
          this.error = `Quality measure "${this.measureId}" not found`;
          this.loading = false;
          return;
        }

        const matchingResults = results.filter((r) => r.measureId.toUpperCase() === key);
        this.measure = this.buildMeasure(measureInfo, matchingResults);
        this.patientResults = this.buildPatientResults(matchingResults);
        this.careGaps = this.buildCareGaps(careGaps, matchingResults, key);
        this.historicalTrends = this.buildHistoricalTrends(matchingResults);

        this.loading = false;
      },
      error: (error) => {
        this.logger.error('Failed to load quality measure detail', error);
        this.error = 'Failed to load quality measure detail';
        this.loading = false;
      },
    });
  }

  private buildMeasure(measureInfo: MeasureInfo, results: QualityMeasureResult[]): QualityMeasure {
    const denominator = results.filter((r) => r.denominatorEligible).length;
    const numerator = results.filter((r) => r.denominatorEligible && r.numeratorCompliant).length;
    const rate = denominator > 0 ? Math.round((numerator / denominator) * 1000) / 10 : 0;
    const careGaps = denominator - numerator;
    const benchmark = BENCHMARK_BY_MEASURE[measureInfo.id] ?? 70;

    return {
      id: measureInfo.id,
      code: measureInfo.id,
      name: measureInfo.description || measureInfo.name,
      description: measureInfo.description || `${measureInfo.name} quality measure`,
      category: measureInfo.category || 'HEDIS',
      measureType: `HEDIS ${new Date().getFullYear()}`,
      steward: 'NCQA',
      starRating: STAR_RATING_BY_MEASURE[measureInfo.id] ?? 3,
      status: 'Active',
      lastEvaluated: results.map((r) => r.calculationDate || r.createdAt).sort().at(-1) ?? null,
      denominator,
      numerator,
      rate,
      benchmark,
      careGaps,
      purpose: `Evaluate ${measureInfo.id} compliance for attributed patients.`,
      improvementNotation: 'Higher rate indicates better performance',
      riskAdjustment: 'None',
      rationale: 'Backed by quality-measure-service evaluation results.',
      clinicalRecommendation: 'Address care gaps for non-compliant eligible patients.',
      definitions: 'Denominator = eligible patients; Numerator = compliant patients.',
    };
  }

  private buildPatientResults(results: QualityMeasureResult[]): PatientResult[] {
    return results.slice(0, 200).map((result) => ({
      patientId: result.patientId,
      patientName: `Patient ${result.patientId.slice(0, 8)}`,
      mrn: `MRN-${result.patientId.slice(0, 8).toUpperCase()}`,
      compliant: result.denominatorEligible && result.numeratorCompliant,
      evaluationDate: (result.calculationDate || result.createdAt).split('T')[0],
      denominatorInclusion: result.denominatorEligible,
      numeratorInclusion: result.numeratorCompliant,
      details: result.denominatorEligible
        ? (result.numeratorCompliant ? 'Meets numerator criteria' : 'Eligible but not numerator compliant')
        : 'Not denominator eligible',
    }));
  }

  private buildCareGaps(careGapItems: CareGapApiItem[], results: QualityMeasureResult[], measureId: string): CareGap[] {
    const fromCareGapApi = careGapItems
      .filter((gap) => gap.measureId.toUpperCase() === measureId)
      .map((gap) => ({
        gapId: gap.id || `${gap.patientId}-${measureId}`,
        patientId: gap.patientId,
        patientName: `Patient ${gap.patientId.slice(0, 8)}`,
        mrn: `MRN-${gap.patientId.slice(0, 8).toUpperCase()}`,
        gapType: gap.gapStatus || 'open',
        gapDescription: gap.gapDescription || 'Quality measure care gap',
        daysOverdue: this.daysOverdue(gap.dueDate),
        urgency: this.mapUrgency(gap.priority),
        dueDate: gap.dueDate || new Date().toISOString().split('T')[0],
      }));

    if (fromCareGapApi.length > 0) {
      return fromCareGapApi;
    }

    return results
      .filter((r) => r.denominatorEligible && !r.numeratorCompliant)
      .slice(0, 200)
      .map((result) => ({
        gapId: `derived-${result.id}`,
        patientId: result.patientId,
        patientName: `Patient ${result.patientId.slice(0, 8)}`,
        mrn: `MRN-${result.patientId.slice(0, 8).toUpperCase()}`,
        gapType: 'compliance',
        gapDescription: `No compliant ${result.measureName || result.measureId} evidence in numerator`,
        daysOverdue: 30,
        urgency: 'medium' as const,
        dueDate: new Date().toISOString().split('T')[0],
      }));
  }

  private buildHistoricalTrends(results: QualityMeasureResult[]): TrendDataPoint[] {
    const monthly = new Map<string, { denominator: number; numerator: number }>();

    for (const result of results) {
      const key = (result.calculationDate || result.createdAt).slice(0, 7);
      const current = monthly.get(key) || { denominator: 0, numerator: 0 };
      if (result.denominatorEligible) {
        current.denominator += 1;
        if (result.numeratorCompliant) {
          current.numerator += 1;
        }
      }
      monthly.set(key, current);
    }

    return Array.from(monthly.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-6)
      .map(([date, stats]) => ({
        date,
        numerator: stats.numerator,
        denominator: stats.denominator,
        rate: stats.denominator === 0 ? 0 : Math.round((stats.numerator / stats.denominator) * 1000) / 10,
      }));
  }

  private mapUrgency(priority?: string): 'high' | 'medium' | 'low' {
    const normalized = (priority || '').toLowerCase();
    if (normalized === 'high' || normalized === 'critical') return 'high';
    if (normalized === 'low') return 'low';
    return 'medium';
  }

  private daysOverdue(dueDate?: string): number {
    if (!dueDate) return 0;
    const due = new Date(dueDate).getTime();
    const now = Date.now();
    return Math.max(0, Math.floor((now - due) / (1000 * 60 * 60 * 24)));
  }

  goBack(): void {
    this.backButtonLoading = true;
    this.router.navigate(['/quality-measures']);
  }

  viewPatientDetail(patientId: string): void {
    this.logger.info('Navigating to patient detail', patientId);
    this.router.navigate(['/patients', patientId], {
      queryParams: {
        tab: 'results',
        source: 'quality-measure-detail',
      },
    });
  }

  viewCareGaps(): void {
    this.logger.info('Navigating to care gaps filtered by measure', this.measure?.code);
    this.router.navigate(['/care-gaps'], {
      queryParams: {
        measure: this.measure?.code,
        source: 'quality-measure-detail',
      },
    });
  }

  getStarRatingArray(): number[] {
    return Array(this.measure?.starRating || 0).fill(0);
  }

  getStatusColor(): string {
    return this.measure?.status === 'Active' ? 'primary' : 'warn';
  }

  getComplianceColor(compliant: boolean): string {
    return compliant ? 'primary' : 'warn';
  }

  getUrgencyClass(urgency: string): string {
    return `urgency-badge urgency-${urgency}`;
  }

  getRatePercentage(): number {
    if (!this.measure || !this.measure.rate) return 0;
    return Math.round(this.measure.rate * 10) / 10;
  }

  getGapToBenchmark(): number {
    if (!this.measure || !this.measure.rate || !this.measure.benchmark) return 0;
    return Math.round((this.measure.benchmark - this.measure.rate) * 10) / 10;
  }
}

const BENCHMARK_BY_MEASURE: Record<string, number> = {
  BCS: 74.2,
  COL: 72.5,
  CBP: 68.3,
  CDC: 58.7,
  EED: 67.1,
  SPC: 82.4,
};

const STAR_RATING_BY_MEASURE: Record<string, number> = {
  BCS: 5,
  COL: 5,
  CBP: 4,
  CDC: 5,
  EED: 4,
  SPC: 3,
};
