import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, firstValueFrom, forkJoin, map, of } from 'rxjs';
import { API_CONFIG } from '../../config/api.config';
import { LoggerService } from '../../services/logger.service';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';
import { MeasureInfo } from '../../models/cql-library.model';
import { QualityMeasureResult } from '../../models/quality-result.model';
import { PatientSummary } from '../../models/patient.model';

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
}

interface EvaluationResult {
  measureId: string;
  measureCode: string;
  measureName: string;
  evaluationTime: number;
  patientsEvaluated: number;
  denominator: number;
  numerator: number;
  rate: number;
  benchmark: number;
  gapToBenchmark: number;
  careGapsCount: number;
}

@Component({
  selector: 'app-quality-measures',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatBadgeModule,
    MatDividerModule,
    MatAutocompleteModule,
  ],
  templateUrl: './quality-measures.component.html',
  styleUrl: './quality-measures.component.scss',
})
export class QualityMeasuresComponent implements OnInit {
  searchTerm = signal('');
  selectedCategory = signal<string>('all');
  selectedStatus = signal<string>('all');
  selectedMeasure = signal<QualityMeasure | null>(null);

  isEvaluating = signal(false);
  evaluationProgress = signal(0);
  evaluationStatus = signal('');
  evaluationResult = signal<EvaluationResult | null>(null);

  estimatedTimeRemaining = computed(() => Math.max(0, Math.floor((100 - this.evaluationProgress()) / 25)));

  totalPatients = signal(0);
  lastEvaluationDate = signal<string | null>(null);
  evaluationPatientId = signal<string | null>(null);
  patientSearchTerm = signal('');
  patients = signal<PatientSummary[]>([]);
  loadingPatients = signal(false);

  filteredPatients = computed(() => {
    const term = this.patientSearchTerm().trim().toLowerCase();
    const allPatients = this.patients();
    if (!term) {
      return allPatients.slice(0, 10);
    }
    return allPatients
      .filter((patient) =>
        patient.fullName.toLowerCase().includes(term) ||
        (patient.mrn || '').toLowerCase().includes(term)
      )
      .slice(0, 10);
  });

  categories = [
    { value: 'all', label: 'All Categories' },
    { value: 'screening', label: 'Screening' },
    { value: 'chronic', label: 'Chronic Disease' },
    { value: 'prevention', label: 'Prevention' },
    { value: 'behavioral', label: 'Behavioral Health' },
  ];

  measures = signal<QualityMeasure[]>([]);

  filteredMeasures = computed(() => {
    let result = this.measures();

    const search = this.searchTerm().toLowerCase();
    if (search) {
      result = result.filter((m) =>
        m.name.toLowerCase().includes(search) ||
        m.code.toLowerCase().includes(search) ||
        m.description.toLowerCase().includes(search)
      );
    }

    if (this.selectedCategory() !== 'all') {
      result = result.filter((m) => m.category === this.selectedCategory());
    }

    if (this.selectedStatus() !== 'all') {
      result = result.filter((m) => m.status === this.selectedStatus());
    }

    return result;
  });

  displayedColumns = ['code', 'name', 'category', 'benchmark', 'status', 'lastEvaluated', 'actions'];

  isLoading = signal(false);
  loadError = signal<string | null>(null);

  private http = inject(HttpClient);
  private logger = inject(LoggerService).withContext('QualityMeasuresComponent');
  private measureService = inject(MeasureService);
  private evaluationService = inject(EvaluationService);
  private patientService = inject(PatientService);

  constructor(
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadMeasureCatalog();
    this.loadPatients();
    this.loadPatientCountAndContext();
    this.loadDefaultEvaluationPreset();
  }

  private loadMeasureCatalog(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    forkJoin({
      measures: this.measureService.getLocalMeasuresAsInfo().pipe(catchError(() => of([] as MeasureInfo[]))),
      results: this.evaluationService.getAllResults(0, 5000).pipe(catchError(() => of([] as QualityMeasureResult[]))),
    }).subscribe({
      next: ({ measures, results }) => {
        const mapped = measures.map((measure) => this.mapMeasureWithResults(measure, results));
        this.measures.set(mapped);

        const latest = mapped
          .map((m) => m.lastEvaluated)
          .filter((date): date is string => !!date)
          .sort()
          .at(-1) ?? null;

        this.lastEvaluationDate.set(latest);
        this.isLoading.set(false);

        if (mapped.length === 0) {
          this.loadError.set('No quality measures were returned by the quality-measure service.');
        }
      },
      error: (error) => {
        this.logger.error('Failed to load quality measure catalog', error);
        this.loadError.set('Failed to load quality measure data.');
        this.isLoading.set(false);
      },
    });
  }

  private loadPatientCountAndContext(): void {
    const fhirUrl = `${API_CONFIG.FHIR_SERVER_URL}/Patient?_count=1`;

    this.http.get<FhirBundleResponse>(fhirUrl, {
      headers: { 'X-Tenant-ID': API_CONFIG.DEFAULT_TENANT_ID },
    }).subscribe({
      next: (response) => {
        const total = response.total || (response.entry?.length || 0);
        this.totalPatients.set(total);

        const fallbackPatientId = response.entry?.[0]?.resource?.id || null;
        if (!this.evaluationPatientId() && fallbackPatientId) {
          this.evaluationPatientId.set(fallbackPatientId);
        }
      },
      error: (error) => {
        this.logger.error('Failed to load patient count', error);
      },
    });
  }

  private loadDefaultEvaluationPreset(): void {
    this.evaluationService.getDefaultEvaluationPreset().subscribe({
      next: (preset) => {
        if (preset?.patientId) {
          this.evaluationPatientId.set(preset.patientId);
          this.syncPatientSearchFromSelection();
        }
      },
      error: (error) => {
        this.logger.warn('Unable to load default evaluation preset', error);
      },
    });
  }

  private loadPatients(): void {
    this.loadingPatients.set(true);
    this.patientService.getPatientsSummaryCached().pipe(
      catchError((error) => {
        this.logger.warn('Unable to load patient summaries for autocomplete', error);
        return of([] as PatientSummary[]);
      })
    ).subscribe((patients) => {
      this.patients.set(patients);
      this.loadingPatients.set(false);
      this.syncPatientSearchFromSelection();
    });
  }

  onPatientSearchChange(value: string | PatientSummary): void {
    if (typeof value !== 'string') {
      this.selectEvaluationPatient(value);
      return;
    }

    this.patientSearchTerm.set(value);

    const selected = this.selectedEvaluationPatient();
    if (selected && this.getPatientDisplay(selected) !== value) {
      this.evaluationPatientId.set(null);
    }
  }

  selectEvaluationPatient(patient: PatientSummary): void {
    this.evaluationPatientId.set(patient.id);
    this.patientSearchTerm.set(this.getPatientDisplay(patient));
  }

  getPatientDisplay(patient: PatientSummary): string {
    return patient.mrn ? `${patient.fullName} (MRN: ${patient.mrn})` : patient.fullName;
  }

  private selectedEvaluationPatient(): PatientSummary | null {
    const selectedId = this.evaluationPatientId();
    if (!selectedId) return null;
    return this.patients().find((patient) => patient.id === selectedId) || null;
  }

  private syncPatientSearchFromSelection(): void {
    const selected = this.selectedEvaluationPatient();
    if (selected) {
      this.patientSearchTerm.set(this.getPatientDisplay(selected));
    }
  }

  selectMeasure(measure: QualityMeasure): void {
    this.selectedMeasure.set(measure);
    this.evaluationResult.set(null);
  }

  viewMeasureDetail(measure: QualityMeasure): void {
    this.router.navigate(['/quality-measures', measure.code]);
  }

  closeMeasureDetail(): void {
    this.selectedMeasure.set(null);
    this.evaluationResult.set(null);
  }

  async runEvaluation(): Promise<void> {
    const measure = this.selectedMeasure();
    if (!measure) return;

    const patientId = this.evaluationPatientId();
    if (!patientId) {
      this.loadError.set('No patient context available. Set a default evaluation preset or load patient data first.');
      return;
    }

    this.isEvaluating.set(true);
    this.evaluationProgress.set(20);
    this.evaluationStatus.set(`Submitting ${measure.code} evaluation...`);

    const startedAt = performance.now();

    try {
      const localResult = await firstValueFrom(
        this.evaluationService.calculateLocalMeasure(patientId, measure.code)
      );

      this.evaluationProgress.set(85);
      this.evaluationStatus.set('Finalizing quality measure result...');

      const denominator = localResult.denominatorMembership ? 1 : 0;
      const numerator = denominator === 1 && localResult.careGaps.length === 0 ? 1 : 0;
      const rate = denominator === 0 ? 0 : 100 * (numerator / denominator);
      const benchmark = measure.benchmark || 70;
      const careGapsCount = localResult.careGaps.length;
      const evaluationTime = Math.round((performance.now() - startedAt) / 100) / 10;

      const result: EvaluationResult = {
        measureId: measure.id,
        measureCode: measure.code,
        measureName: measure.name,
        evaluationTime,
        patientsEvaluated: 1,
        denominator,
        numerator,
        rate,
        benchmark,
        gapToBenchmark: Math.round((rate - benchmark) * 10) / 10,
        careGapsCount,
      };

      this.evaluationResult.set(result);

      const updatedMeasures = this.measures().map((m) => {
        if (m.id !== measure.id) return m;
        return {
          ...m,
          lastEvaluated: localResult.calculatedAt,
          denominator,
          numerator,
          rate,
          careGaps: careGapsCount,
        };
      });

      this.measures.set(updatedMeasures);
      this.selectedMeasure.set(updatedMeasures.find((m) => m.id === measure.id) || null);
      this.lastEvaluationDate.set(localResult.calculatedAt);
      this.loadError.set(null);
      this.evaluationProgress.set(100);
      this.evaluationStatus.set('Evaluation complete');
    } catch (error) {
      this.logger.error('Failed to run quality measure evaluation', error);
      this.loadError.set('Evaluation failed. Please retry or verify the quality-measure service is available.');
      this.evaluationStatus.set('Evaluation failed');
    } finally {
      this.isEvaluating.set(false);
    }
  }

  viewCareGaps(): void {
    const result = this.evaluationResult();
    if (result) {
      this.router.navigate(['/care-gaps'], {
        queryParams: {
          measureCode: result.measureCode,
          measureName: result.measureName,
        },
      });
    }
  }

  generateOutreach(): void {
    const result = this.evaluationResult();
    if (result) {
      this.router.navigate(['/outreach-campaigns'], {
        queryParams: {
          measureCode: result.measureCode,
          careGapsCount: result.careGapsCount,
        },
      });
    }
  }

  viewCqlLogic(): void {
    const measure = this.selectedMeasure();
    if (measure) {
      this.router.navigate(['/measure-builder'], {
        queryParams: { measureId: measure.id },
      });
    }
  }

  getStarArray(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => (i < rating ? 1 : 0));
  }

  getCategoryLabel(category: string): string {
    const cat = this.categories.find((c) => c.value === category);
    return cat?.label || category;
  }

  formatDate(date: string | null): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private mapMeasureWithResults(measure: MeasureInfo, results: QualityMeasureResult[]): QualityMeasure {
    const matchingResults = results.filter((result) => result.measureId === measure.id);
    const denominator = matchingResults.filter((result) => result.denominatorEligible).length;
    const numerator = matchingResults.filter((result) => result.denominatorEligible && result.numeratorCompliant).length;
    const rate = denominator > 0 ? Math.round((numerator / denominator) * 1000) / 10 : undefined;
    const lastEvaluated = matchingResults
      .map((result) => result.calculationDate || result.createdAt)
      .sort()
      .at(-1) ?? null;

    return {
      id: measure.id,
      code: measure.id,
      name: measure.description || measure.name,
      description: measure.description || `${measure.name} quality measure`,
      category: this.mapCategory(measure.category),
      measureType: `HEDIS ${new Date().getFullYear()}`,
      steward: 'NCQA',
      starRating: STAR_RATING_BY_MEASURE[measure.id] ?? 3,
      status: 'Active',
      lastEvaluated,
      denominator: denominator || undefined,
      numerator: numerator || undefined,
      rate,
      benchmark: BENCHMARK_BY_MEASURE[measure.id] ?? 70,
      careGaps: denominator - numerator,
    };
  }

  private mapCategory(category?: string): string {
    const normalized = (category || '').toUpperCase();
    if (normalized.includes('BEHAVIORAL')) return 'behavioral';
    if (normalized.includes('CHRONIC')) return 'chronic';
    if (normalized.includes('PREVENTIVE') || normalized.includes('SCREEN') || normalized.includes('WOMENS')) return 'screening';
    return 'prevention';
  }
}

interface FhirBundleResponse {
  resourceType: string;
  type: string;
  total?: number;
  entry?: { resource?: { id?: string } }[];
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
