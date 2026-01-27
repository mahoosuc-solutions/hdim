import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { LoggerService } from '../../../services/logger.service';
import { CommonModule } from '@angular/common';
import { LoggerService } from '../../../services/logger.service';
import { FormsModule } from '@angular/forms';
import { LoggerService } from '../../../services/logger.service';
import { MatCardModule } from '@angular/material/card';
import { LoggerService } from '../../../services/logger.service';
import { MatButtonModule } from '@angular/material/button';
import { LoggerService } from '../../../services/logger.service';
import { MatIconModule } from '@angular/material/icon';
import { LoggerService } from '../../../services/logger.service';
import { MatChipsModule } from '@angular/material/chips';
import { LoggerService } from '../../../services/logger.service';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoggerService } from '../../../services/logger.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { LoggerService } from '../../../services/logger.service';
import { MatInputModule } from '@angular/material/input';
import { LoggerService } from '../../../services/logger.service';
import { MatSelectModule } from '@angular/material/select';
import { LoggerService } from '../../../services/logger.service';
import { MatSliderModule } from '@angular/material/slider';
import { LoggerService } from '../../../services/logger.service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoggerService } from '../../../services/logger.service';
import * as THREE from 'three';
import { LoggerService } from '../../../services/logger.service';
import { Subject, takeUntil, forkJoin, of, throwError } from 'rxjs';
import { LoggerService } from '../../../services/logger.service';
import { catchError } from 'rxjs/operators';
import { LoggerService } from '../../../services/logger.service';

import { ThreeSceneService } from '../core/three-scene.service';
import { LoggerService } from '../../../services/logger.service';
import { DataTransformService } from '../data/data-transform.service';
import { LoggerService } from '../../../services/logger.service';
import { PatientService } from '../../services/patient.service';
import { LoggerService } from '../../../services/logger.service';
import { EvaluationService } from '../../services/evaluation.service';
import { LoggerService } from '../../../services/logger.service';
import { MeasureService } from '../../services/measure.service';
import { LoggerService } from '../../../services/logger.service';
import { QualityConstellationScene, ConstellationFilters, PatientPoint, ConstellationStats } from '../scenes/quality-constellation.scene';
import { LoggerService } from '../../../services/logger.service';
import { QualityMeasureResult, MeasureCategory } from '../../models/quality-result.model';
import { LoggerService } from '../../../services/logger.service';
import { PatientSummary } from '../../models/patient.model';
import { LoggerService } from '../../../services/logger.service';
import { ErrorValidationService } from '../../services/error-validation.service';
import { LoggerService } from '../../../services/logger.service';
import { COMPLIANCE_CONFIG } from '../../config/compliance.config';
import { LoggerService } from '../../../services/logger.service';
import { ErrorCode, ErrorSeverity } from '../../models/error.model';
import { LoggerService } from '../../../services/logger.service';

/**
 * Quality Constellation Component
 *
 * 3D visualization of patient quality metrics showing 10,000+ patients as a constellation.
 * Uses GPU instancing for high performance rendering.
 *
 * Features:
 * - Interactive 3D scatter plot with 10K+ points
 * - Color-coded by compliance status or measure category
 * - Hover tooltips showing patient details
 * - Click to focus/zoom on individual patients
 * - Filter by measure category, compliance status, date range
 * - Search for specific patients
 * - Performance optimized with frustum culling and LOD
 */
@Component({
  selector: 'app-quality-constellation',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './quality-constellation.component.html',
  styleUrl: './quality-constellation.component.scss',
})
export class QualityConstellationComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('sceneContainer', { static: true }) sceneContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('tooltip', { static: true }) tooltipElement!: ElementRef<HTMLDivElement>;

  // Component state
  private destroy$ = new Subject<void>();
  private constellation?: QualityConstellationScene;
  private animationCallback?: (delta: number, elapsed: number) => void;

  // Data
  qualityResults: QualityMeasureResult[] = [];
  patients: PatientSummary[] = [];
  stats?: ConstellationStats;

  // UI state
  isLoading = true;
  loadingMessage = 'Loading patient data...';
  colorScheme: 'compliance' | 'category' = 'compliance';

  // Filters
  filters: ConstellationFilters = {
    measureCategory: 'ALL',
    complianceStatus: 'ALL',
    minComplianceRate: 0,
    maxComplianceRate: 100,
    searchTerm: '',
  };

  // Tooltip state
  tooltipVisible = false;
  tooltipX = 0;
  tooltipY = 0;
  tooltipPatient?: PatientPoint;

  // Filter options
  measureCategories: (MeasureCategory | 'ALL')[] = ['ALL', 'HEDIS', 'CMS', 'CUSTOM'];
  complianceStatuses: ('ALL' | 'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE')[] = [
    'ALL', 'COMPLIANT', 'NON_COMPLIANT', 'NOT_ELIGIBLE'
  ];

  // Mouse interaction
  private mouse = new THREE.Vector2();
  private hoveredPatient?: PatientPoint;

  constructor(
    private loggerService: LoggerService,
    private sceneService: ThreeSceneService,
    private transformService: DataTransformService,
    private patientService: PatientService,
    private evaluationService: EvaluationService,
    private measureService: MeasureService,
    private errorValidationService: ErrorValidationService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    // Initialize scene will be called after data loads
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.animationCallback) {
      this.sceneService.unregisterAnimationCallback(this.animationCallback);
    }

    this.constellation?.dispose();
  }

  /**
   * Load patient and quality data
   */
  private loadData(): void {
    this.isLoading = true;
    this.loadingMessage = 'Loading patient data...';

    // Load patients and quality results in parallel
    // Uses actual API calls with fallback to mock data for demo scenarios
    forkJoin({
      patients: this.patientService.getPatientsSummary(),
      qualityResults: this.evaluationService.getAllResults(0, 10000).pipe(
        catchError((error) => {
          this.logger.warn('Quality results API unavailable:', error.message);
          
          // Check if fallbacks are disabled
          if (COMPLIANCE_CONFIG.disableFallbacks && 
              !this.errorValidationService.isFallbackAllowed('QualityConstellation')) {
            // Track error for compliance
            this.errorValidationService.trackError(error, {
              service: 'QualityConstellation',
              operation: 'loadData',
              errorCode: ErrorCode.DATA_LOADING_ERROR,
              severity: ErrorSeverity.WARNING,
            });
            // Throw error instead of fallback
            return throwError(() => error);
          }
          
          // Return empty array only if allowed
          return of([] as QualityMeasureResult[]);
        })
      ),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ patients, qualityResults }) => {
          this.patients = patients;
          this.loadingMessage = 'Processing quality results...';

          // Use API results if available
          if (qualityResults && qualityResults.length > 0) {
            this.qualityResults = qualityResults;
            this.logger.info(`Loaded ${qualityResults.length} quality results from API for ${patients.length} patients`);
          } else {
            // Check if fallbacks are disabled
            if (COMPLIANCE_CONFIG.disableFallbacks && 
                !this.errorValidationService.isFallbackAllowed('QualityConstellation')) {
              // Track error for compliance
              this.errorValidationService.trackError(
                new Error('No quality results returned from API'),
                {
                  service: 'QualityConstellation',
                  operation: 'loadData',
                  errorCode: ErrorCode.DATA_LOADING_ERROR,
                  severity: ErrorSeverity.WARNING,
                }
              );
              // Don't generate mock data, leave empty
              this.qualityResults = [];
            } else {
              // Generate mock quality results for demo when API is unavailable (only if allowed)
              this.generateMockQualityResults(patients);
            }
          }

          // Initialize scene
          this.initScene();
        },
        error: (error) => {
          this.logger.error('Error loading data:', { error });
          this.loadingMessage = 'Error loading data. Please try again.';
        }
      });
  }

  /**
   * Generate mock quality results for demo/fallback scenarios
   * Used when the quality results API is unavailable or returns empty data
   */
  private generateMockQualityResults(patients: PatientSummary[]): void {
    this.qualityResults = [];
    const measureIds = ['CDC-A1C9', 'CDC-BP', 'CBP', 'COL', 'BCS'];
    const categories: MeasureCategory[] = ['HEDIS', 'CMS', 'CUSTOM'];

    patients.forEach(patient => {
      // Each patient has 3-8 quality measures
      const measureCount = 3 + Math.floor(Math.random() * 6);

      for (let i = 0; i < measureCount; i++) {
        const measureId = measureIds[i % measureIds.length];
        const category = categories[Math.floor(Math.random() * categories.length)];
        const denominatorEligible = Math.random() > 0.1; // 90% eligible
        const numeratorCompliant = denominatorEligible && Math.random() > 0.3; // 70% compliant if eligible
        const complianceRate = numeratorCompliant ? 100 : 0;

        this.qualityResults.push({
          id: `${patient.id}-${measureId}-${i}`,
          tenantId: 'acme-health',
          patientId: patient.id!,
          measureId,
          measureName: `${category} ${measureId}`,
          measureCategory: category,
          measureYear: 2024,
          numeratorCompliant,
          denominatorEligible,
          complianceRate,
          score: numeratorCompliant ? 80 + Math.random() * 20 : Math.random() * 60,
          calculationDate: new Date().toISOString(),
          cqlLibrary: `${category}-${measureId}`,
          createdAt: new Date().toISOString(),
          createdBy: 'system',
          version: 1,
        });
      }
    });

    this.logger.info(`Generated ${this.qualityResults.length} mock quality results for ${patients.length} patients`);
  }

  /**
   * Initialize Three.js scene
   */
  private initScene(): void {
    this.loadingMessage = 'Initializing 3D visualization...';

    this.sceneService.initScene(this.sceneContainer.nativeElement, {
      enableStats: true,
      enableOrbitControls: true,
      enableGrid: true,
      enableAxes: false,
      backgroundColor: 0x0a0e1a,
      cameraPosition: new THREE.Vector3(0, 50, 150),
    });

    // Create constellation scene
    const scene = this.sceneService.getScene();
    this.constellation = new QualityConstellationScene(scene, this.transformService);
    this.constellation.initialize(this.qualityResults, this.patients);

    // Get initial stats
    this.updateStats();

    // Register animation callback
    this.animationCallback = (delta: number, elapsed: number) => {
      this.constellation?.update(delta, elapsed);

      // Update LOD
      const camera = this.sceneService.getCamera();
      this.constellation?.updateLOD(camera);
    };
    this.sceneService.registerAnimationCallback(this.animationCallback);

    // Add mouse move listener for hover
    const canvas = this.sceneService.getRenderer().domElement;
    canvas.addEventListener('mousemove', this.onMouseMove.bind(this));
    canvas.addEventListener('click', this.onMouseClick.bind(this));

    // Start animation
    this.sceneService.startAnimation();

    this.isLoading = false;
  }

  /**
   * Handle mouse move for hover tooltips
   */
  private onMouseMove(event: MouseEvent): void {
    if (!this.constellation) return;

    const canvas = this.sceneService.getRenderer().domElement;
    const rect = canvas.getBoundingClientRect();

    // Calculate mouse position in normalized device coordinates
    this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    // Raycast to find hovered patient
    const camera = this.sceneService.getCamera();
    const patient = this.constellation.raycast(this.mouse, camera);

    if (patient) {
      this.tooltipPatient = patient;
      this.tooltipVisible = true;
      this.tooltipX = event.clientX;
      this.tooltipY = event.clientY;
      this.hoveredPatient = patient;
      this.constellation.highlightPatient(patient.patientId);
    } else {
      this.tooltipVisible = false;
      this.hoveredPatient = undefined;
      this.constellation.highlightPatient(undefined);
    }
  }

  /**
   * Handle mouse click to focus on patient
   */
  private onMouseClick(event: MouseEvent): void {
    if (this.hoveredPatient) {
      this.focusOnPatient(this.hoveredPatient.patientId);
    }
  }

  /**
   * Focus camera on a specific patient
   */
  focusOnPatient(patientId: string): void {
    if (!this.constellation) return;

    this.constellation.focusOnPatient(patientId, this.sceneService.getCamera());

    const focusPosition = this.constellation.getFocusedPosition();
    if (focusPosition) {
      // Animate camera to position
      const camera = this.sceneService.getCamera();
      const controls = this.sceneService.getControls();

      if (controls) {
        // Set target
        controls.target.copy(focusPosition);

        // Move camera to a good viewing position
        const offset = new THREE.Vector3(20, 10, 20);
        camera.position.copy(focusPosition).add(offset);

        controls.update();
      }
    }
  }

  /**
   * Apply filters
   */
  applyFilters(): void {
    if (!this.constellation) return;

    this.constellation.applyFilters(this.filters);
    this.updateStats();
  }

  /**
   * Reset filters
   */
  resetFilters(): void {
    this.filters = {
      measureCategory: 'ALL',
      complianceStatus: 'ALL',
      minComplianceRate: 0,
      maxComplianceRate: 100,
      searchTerm: '',
    };
    this.applyFilters();
  }

  /**
   * Change color scheme
   */
  changeColorScheme(): void {
    if (!this.constellation) return;
    this.constellation.setColorScheme(this.colorScheme);
  }

  /**
   * Update statistics
   */
  private updateStats(): void {
    if (!this.constellation) return;
    this.stats = this.constellation.getStats();
  }

  /**
   * Reset camera view
   */
  resetCamera(): void {
    const camera = this.sceneService.getCamera();
    const controls = this.sceneService.getControls();

    camera.position.set(0, 50, 150);
    if (controls) {
      controls.target.set(0, 0, 0);
      controls.update();
    }
  }

  /**
   * Export visualization as image
   */
  exportImage(): void {
    const renderer = this.sceneService.getRenderer();
    const canvas = renderer.domElement;

    // Create download link
    canvas.toBlob((blob) => {
      if (blob) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `quality-constellation-${new Date().toISOString()}.png`;
        link.click();
        URL.revokeObjectURL(url);
      }
    });
  }

  /**
   * Get compliance status color
   */
  getComplianceColor(rate: number): string {
    if (rate >= 80) return '#4caf50';
    if (rate >= 60) return '#ff9800';
    return '#f44336';
  }

  /**
   * Format percentage
   */
  formatPercent(value: number): string {
    return `${value.toFixed(1)}%`;
  }
}
