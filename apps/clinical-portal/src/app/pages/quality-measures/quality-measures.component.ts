import { Component, OnInit, OnDestroy, signal, computed, inject, DestroyRef } from '@angular/core';
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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { API_CONFIG } from '../../config/api.config';
import { LoggerService } from '../../services/logger.service';
import { MeasureService } from '../../services/measure.service';
import { EvaluationService } from '../../services/evaluation.service';
import { PatientService } from '../../services/patient.service';

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
export class QualityMeasuresComponent implements OnInit, OnDestroy {
  // Signals for reactive state
  searchTerm = signal('');
  selectedCategory = signal<string>('all');
  selectedStatus = signal<string>('all');
  selectedMeasure = signal<QualityMeasure | null>(null);

  // Evaluation state
  isEvaluating = signal(false);
  evaluationProgress = signal(0);
  evaluationStatus = signal('');
  evaluationResult = signal<EvaluationResult | null>(null);

  // Computed for template (avoid Math in templates)
  estimatedTimeRemaining = computed(() => {
    return Math.max(0, Math.floor((100 - this.evaluationProgress()) / 8));
  });

  // Population stats
  totalPatients = signal(5000);
  lastEvaluationDate = signal<string | null>(null);

  // Categories for filter
  categories = [
    { value: 'all', label: 'All Categories' },
    { value: 'screening', label: 'Screening' },
    { value: 'chronic', label: 'Chronic Disease' },
    { value: 'prevention', label: 'Prevention' },
    { value: 'behavioral', label: 'Behavioral Health' },
  ];

  // Sample HEDIS measures
  measures = signal<QualityMeasure[]>([
    {
      id: '1',
      code: 'BCS',
      name: 'Breast Cancer Screening',
      description: 'Women aged 50-74 who had a mammogram to screen for breast cancer in the past 27 months.',
      category: 'screening',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 5,
      status: 'Active',
      lastEvaluated: null,
      benchmark: 74.2,
    },
    {
      id: '2',
      code: 'COL',
      name: 'Colorectal Cancer Screening',
      description: 'Adults aged 45-75 who had appropriate screening for colorectal cancer.',
      category: 'screening',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 5,
      status: 'Active',
      lastEvaluated: null,
      benchmark: 72.5,
    },
    {
      id: '3',
      code: 'CBP',
      name: 'Controlling High Blood Pressure',
      description: 'Adults aged 18-85 with a diagnosis of hypertension whose blood pressure was adequately controlled.',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 4,
      status: 'Active',
      lastEvaluated: null,
      benchmark: 68.3,
    },
    {
      id: '4',
      code: 'CDC',
      name: 'Comprehensive Diabetes Care - HbA1c Control',
      description: 'Adults aged 18-75 with diabetes whose HbA1c was at the controlled level (<8.0%).',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 5,
      status: 'Active',
      lastEvaluated: null,
      benchmark: 58.7,
    },
    {
      id: '5',
      code: 'EED',
      name: 'Eye Exam for Patients with Diabetes',
      description: 'Adults aged 18-75 with diabetes who had a retinal eye exam.',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 4,
      status: 'Active',
      lastEvaluated: null,
      benchmark: 67.1,
    },
    {
      id: '6',
      code: 'SPC',
      name: 'Statin Therapy for Patients with Cardiovascular Disease',
      description: 'Adults aged 21-75 with cardiovascular disease who were prescribed statin therapy.',
      category: 'chronic',
      measureType: 'HEDIS 2025',
      steward: 'NCQA',
      starRating: 3,
      status: 'Active',
      lastEvaluated: null,
      benchmark: 82.4,
    },
  ]);

  // Computed filtered measures
  filteredMeasures = computed(() => {
    let result = this.measures();

    // Filter by search term
    const search = this.searchTerm().toLowerCase();
    if (search) {
      result = result.filter(m =>
        m.name.toLowerCase().includes(search) ||
        m.code.toLowerCase().includes(search) ||
        m.description.toLowerCase().includes(search)
      );
    }

    // Filter by category
    if (this.selectedCategory() !== 'all') {
      result = result.filter(m => m.category === this.selectedCategory());
    }

    // Filter by status
    if (this.selectedStatus() !== 'all') {
      result = result.filter(m => m.status === this.selectedStatus());
    }

    return result;
  });

  displayedColumns = ['code', 'name', 'category', 'benchmark', 'status', 'lastEvaluated', 'actions'];

  // Patient context for evaluation
  patientSearchTerm = signal('');
  filteredPatients = signal<Array<{ id: string; fullName: string; mrn: string }>>([]);
  patients = signal<Array<{ id: string; fullName: string; mrn: string; status: string }>>([]);
  evaluationPatientId = signal<string | null>(null);

  // Loading state
  isLoading = signal(false);
  loadError = signal<string | null>(null);

  private http = inject(HttpClient);
  private loggerService = inject(LoggerService);
  private measureService = inject(MeasureService);
  private evaluationService = inject(EvaluationService);
  private patientService = inject(PatientService);
  private destroyRef = inject(DestroyRef);
  private logger: ReturnType<LoggerService['withContext']>;
  private presetPollingInterval: ReturnType<typeof setInterval> | null = null;
  private presetPollingTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private router: Router,
    private dialog: MatDialog
  ) {
    this.logger = this.loggerService.withContext('QualityMeasuresComponent');
  }

  ngOnInit(): void {
    this.loadCareGapStatistics();
    this.loadPatientCount();
    this.loadMeasuresFromService();
    this.loadPatientsFromService();
    this.loadDefaultPreset();
  }

  ngOnDestroy(): void {
    if (this.presetPollingInterval) clearInterval(this.presetPollingInterval);
    if (this.presetPollingTimeout) clearTimeout(this.presetPollingTimeout);
  }

  /**
   * Load care gap statistics from the Care Gap API
   * Aggregates care gaps by measure to show counts per measure
   */
  private loadCareGapStatistics(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    const careGapUrl = `${API_CONFIG.CARE_GAP_URL}/api/v1/care-gaps`;

    this.http.get<CareGapApiResponse>(careGapUrl, {
      headers: { 'X-Tenant-ID': API_CONFIG.DEFAULT_TENANT_ID }
    }).subscribe({
      next: (response) => {
        // Aggregate care gaps by measure code
        const careGapCounts = new Map<string, number>();
        const gapList = response.content || [];

        gapList.forEach((gap: CareGapItem) => {
          const measureId = gap.measureId;
          careGapCounts.set(measureId, (careGapCounts.get(measureId) || 0) + 1);
        });

        // Update measures with care gap counts
        const updatedMeasures = this.measures().map(measure => ({
          ...measure,
          careGaps: careGapCounts.get(measure.code) || 0
        }));

        this.measures.set(updatedMeasures);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.logger.error('Failed to load care gap statistics', error);
        this.loadError.set('Failed to load care gap data. Using default measures.');
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Load patient count from FHIR Patient API
   */
  private loadPatientCount(): void {
    const fhirUrl = `${API_CONFIG.FHIR_SERVER_URL}/Patient`;

    this.http.get<FhirBundleResponse>(fhirUrl, {
      headers: { 'X-Tenant-ID': API_CONFIG.DEFAULT_TENANT_ID }
    }).subscribe({
      next: (response) => {
        const total = response.total || (response.entry?.length || 0);
        this.totalPatients.set(total);
      },
      error: (error) => {
        this.logger.error('Failed to load patient count', error);
        // Keep default value
      }
    });
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

    this.isEvaluating.set(true);
    this.evaluationProgress.set(0);
    this.evaluationStatus.set(`Initializing evaluation for ${measure.code}...`);

    // Simulate evaluation progress
    const totalPatients = this.totalPatients();
    const evaluationTime = 12000; // 12 seconds
    const startTime = Date.now();

    const progressInterval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min((elapsed / evaluationTime) * 100, 99);
      const patientsProcessed = Math.floor((progress / 100) * totalPatients);

      this.evaluationProgress.set(progress);
      this.evaluationStatus.set(`Processing ${patientsProcessed.toLocaleString()} / ${totalPatients.toLocaleString()} patients...`);

      if (progress >= 99) {
        clearInterval(progressInterval);
      }
    }, 100);

    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, evaluationTime));

    clearInterval(progressInterval);
    this.evaluationProgress.set(100);
    this.evaluationStatus.set('Evaluation complete!');

    // Generate result based on measure
    const result = this.generateEvaluationResult(measure);
    this.evaluationResult.set(result);

    // Update measure with result
    const updatedMeasures = this.measures().map(m => {
      if (m.id === measure.id) {
        return {
          ...m,
          lastEvaluated: new Date().toISOString(),
          denominator: result.denominator,
          numerator: result.numerator,
          rate: result.rate,
          careGaps: result.careGapsCount,
        };
      }
      return m;
    });
    this.measures.set(updatedMeasures);
    this.selectedMeasure.set(updatedMeasures.find(m => m.id === measure.id) || null);

    this.isEvaluating.set(false);
    this.lastEvaluationDate.set(new Date().toISOString());
  }

  private generateEvaluationResult(measure: QualityMeasure): EvaluationResult {
    // Generate realistic results based on measure type
    const totalPatients = this.totalPatients();
    const eligiblePercent = 0.15 + Math.random() * 0.05; // 15-20% eligible
    const denominator = Math.floor(totalPatients * eligiblePercent);
    const rate = (measure.benchmark || 70) - 2 + Math.random() * 4; // Near benchmark
    const numerator = Math.floor(denominator * (rate / 100));
    const careGapsCount = denominator - numerator;

    return {
      measureId: measure.id,
      measureCode: measure.code,
      measureName: measure.name,
      evaluationTime: 12.4,
      patientsEvaluated: totalPatients,
      denominator,
      numerator,
      rate: Math.round(rate * 10) / 10,
      benchmark: measure.benchmark || 70,
      gapToBenchmark: Math.round((rate - (measure.benchmark || 70)) * 10) / 10,
      careGapsCount,
    };
  }

  viewCareGaps(): void {
    const result = this.evaluationResult();
    if (result) {
      this.router.navigate(['/care-gaps'], {
        queryParams: {
          measureCode: result.measureCode,
          measureName: result.measureName,
        }
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
        }
      });
    }
  }

  viewCqlLogic(): void {
    const measure = this.selectedMeasure();
    if (measure) {
      this.router.navigate(['/measure-builder'], {
        queryParams: { measureId: measure.id }
      });
    }
  }

  getStarArray(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i < rating ? 1 : 0);
  }

  getCategoryLabel(category: string): string {
    const cat = this.categories.find(c => c.value === category);
    return cat?.label || category;
  }

  /**
   * Load measures from MeasureService and merge into measures signal
   */
  private loadMeasuresFromService(): void {
    this.measureService.getLocalMeasuresAsInfo().pipe(
      switchMap((infos) =>
        this.evaluationService.getAllResults(0, 100).pipe(
          catchError(() => of([] as any[])),
          switchMap((results) => {
            const gapsByMeasure = new Map<string, number>();
            results.forEach((r: any) => {
              const mId = r.measureId || r.measureName;
              if (mId && r.denominatorEligible && !r.numeratorCompliant) {
                gapsByMeasure.set(mId, (gapsByMeasure.get(mId) || 0) + 1);
              }
            });
            const mapped: QualityMeasure[] = infos.map(info => {
              const existing = this.measures().find(m => m.code === info.name);
              return {
                id: info.id,
                code: info.name,
                name: info.displayName || info.name,
                description: info.description || '',
                category: info.category || 'HEDIS',
                measureType: existing?.measureType ?? 'HEDIS',
                steward: existing?.steward ?? 'NCQA',
                starRating: existing?.starRating ?? 0,
                benchmark: existing?.benchmark ?? 0,
                status: existing?.status ?? 'Active' as const,
                careGaps: gapsByMeasure.get(info.name) || 0,
                lastEvaluated: existing?.lastEvaluated ?? null,
              };
            });
            this.measures.set(mapped);
            return of(mapped);
          }),
        )
      ),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      error: (error) => {
        this.logger.error('Failed to load measures from service', error);
      }
    });
  }

  /**
   * Load patients from PatientService
   */
  private loadPatientsFromService(): void {
    if (!this.patientService.getPatientsSummaryCached) {
      this.logger.warn('getPatientsSummaryCached not available on PatientService');
      return;
    }
    this.patientService.getPatientsSummaryCached().pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (summaries: any[]) => {
        this.patients.set(summaries);
        this.filteredPatients.set(summaries);
      },
      error: () => {
        // Patients list remains empty — non-critical
      }
    });
  }

  /**
   * Load default evaluation preset
   */
  private loadDefaultPreset(): void {
    if (!this.evaluationService.getDefaultEvaluationPreset) {
      return;
    }
    this.evaluationService.getDefaultEvaluationPreset().pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (preset: any) => {
        if (preset?.patientId) {
          this.evaluationPatientId.set(preset.patientId);
          const patient = this.patients().find(p => p.id === preset.patientId);
          if (patient) {
            this.patientSearchTerm.set(this.getPatientDisplay(patient));
          } else {
            // Poll for patient data with tracked interval for cleanup
            this.presetPollingInterval = setInterval(() => {
              const found = this.patients().find(p => p.id === preset.patientId);
              if (found) {
                this.patientSearchTerm.set(this.getPatientDisplay(found));
                if (this.presetPollingInterval) clearInterval(this.presetPollingInterval);
                this.presetPollingInterval = null;
              }
            }, 200);
            this.presetPollingTimeout = setTimeout(() => {
              if (this.presetPollingInterval) clearInterval(this.presetPollingInterval);
              this.presetPollingInterval = null;
            }, 5000);
          }
        }
      },
      error: () => {
        // No default preset available — non-critical
      }
    });
  }

  /**
   * Display function for patient autocomplete
   */
  getPatientDisplay(patient: any): string {
    if (!patient) return '';
    if (typeof patient === 'string') return patient;
    return patient.mrn ? `${patient.fullName} (MRN: ${patient.mrn})` : patient.fullName;
  }

  /**
   * Handle patient selection from autocomplete
   */
  selectEvaluationPatient(patient: any): void {
    this.evaluationPatientId.set(patient.id);
    this.patientSearchTerm.set(this.getPatientDisplay(patient));
  }

  /**
   * Handle patient search input changes
   */
  onPatientSearchChange(term: string): void {
    this.patientSearchTerm.set(term);
    if (!term) {
      this.filteredPatients.set(this.patients());
      return;
    }
    const lower = term.toLowerCase();
    this.filteredPatients.set(
      this.patients().filter(p =>
        p.fullName.toLowerCase().includes(lower) ||
        p.mrn?.toLowerCase().includes(lower)
      )
    );
  }

  /**
   * Run evaluation for selected measure and patient
   */
  async runEvaluation(): Promise<void> {
    const measure = this.selectedMeasure();
    const patientId = this.evaluationPatientId();
    if (!measure || !patientId) return;

    this.isEvaluating.set(true);
    this.evaluationResult.set(null);

    try {
      const result = await new Promise<any>((resolve, reject) => {
        this.evaluationService.calculateLocalMeasure(patientId, measure.code).subscribe({
          next: (r) => resolve(r),
          error: (e) => reject(e),
        });
      });

      this.evaluationResult.set({
        measureCode: result.measureId || measure.code,
        measureName: result.measureName || measure.name,
        totalPatients: 1,
        patientsEvaluated: 1,
        denominatorCount: result.denominatorMembership ? 1 : 0,
        numeratorCount: (result.eligible && !result.denominatorExclusion) ? (result.subMeasures && Object.keys(result.subMeasures).length > 0 ? 0 : 1) : 0,
        complianceRate: result.eligible ? 100 : 0,
        careGapsCount: result.careGaps?.length || 0,
        executionTimeMs: 0,
        timestamp: result.calculatedAt || new Date().toISOString(),
      });
    } catch (error) {
      this.logger.error('Evaluation failed', error);
    } finally {
      this.isEvaluating.set(false);
    }
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
}

// API Response Types
interface CareGapApiResponse {
  content: CareGapItem[];
  totalElements: number;
  totalPages: number;
}

interface CareGapItem {
  id: string;
  patientId: string;
  measureId: string;
  measureName: string;
  gapStatus: string;
  priority: string;
  gapDescription: string;
}

interface FhirBundleResponse {
  resourceType: string;
  type: string;
  total?: number;
  entry?: { resource: unknown }[];
}
