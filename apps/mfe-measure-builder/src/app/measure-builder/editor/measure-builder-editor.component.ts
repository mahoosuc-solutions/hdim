import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  ViewChild,
  ElementRef,
  NO_ERRORS_SCHEMA,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import {
  MeasureBuilderState,
  MeasureAlgorithm,
  SliderConfig,
} from '../models/measure-builder.model';
import { AlgorithmBuilderService } from '../services/algorithm-builder.service';
import { MeasureCqlGeneratorService } from '../services/measure-cql-generator.service';
import { CustomMeasureService } from '../../services/custom-measure.service';
import { ToastService } from '../../services/toast.service';

import { MeasurePreviewPanelComponent } from '../components/measure-preview-panel/measure-preview-panel.component';
import { VisualAlgorithmBuilderComponent } from '../components/visual-algorithm-builder/visual-algorithm-builder.component';
import { MeasureConfigSliderComponent } from '../components/measure-config-slider/measure-config-slider.component';

/**
 * Main measure builder editor component with 3-panel layout
 * Left: Metadata and preview
 * Center: Visual algorithm builder
 * Right: Slider-based configuration
 */
@Component({
  selector: 'app-measure-builder-editor',
  standalone: true,
  schemas: [NO_ERRORS_SCHEMA],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatTabsModule,
    MatTooltipModule,
    MatProgressBarModule,
    MeasurePreviewPanelComponent,
    VisualAlgorithmBuilderComponent,
    MeasureConfigSliderComponent,
  ],
  templateUrl: './measure-builder-editor.component.html',
  styleUrls: ['./measure-builder-editor.component.scss'],
})
export class MeasureBuilderEditorComponent implements OnInit, OnDestroy {
  @ViewChild('mainContainer') mainContainer?: ElementRef;
  @ViewChild('canvasContainer') canvasContainer?: ElementRef;

  private destroy$ = new Subject<void>();

  // State
  state: MeasureBuilderState | null = null;
  algorithm: MeasureAlgorithm | null = null;
  sliderConfigurations: SliderConfig[] = [];
  currentCql = '';

  // UI State
  loading = false;
  saving = false;
  sidebarOpen = true;
  rightPanelOpen = true;
  fullScreenMode = false;
  cqlPreviewHeight = 150;
  isDirty = false;
  canUndo = false;
  canRedo = false;

  // Editor options
  editorReadOnly = false;
  showCqlPreview = true;

  constructor(
    public algorithmBuilderService: AlgorithmBuilderService,
    private cqlGeneratorService: MeasureCqlGeneratorService,
    private customMeasureService: CustomMeasureService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.initializeEditor();
    this.subscribeToAlgorithmChanges();
    this.subscribeToStateChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize editor with new algorithm
   */
  private initializeEditor(): void {
    this.loading = true;
    try {
      const algorithm = this.algorithmBuilderService.initializeAlgorithm();
      this.algorithm = algorithm;

      // Initialize sample slider configurations
      this.initializeSampleSliders();

      // Generate initial CQL
      this.updateCql();

      this.loading = false;
    } catch (error: any) {
      this.toastService.error('Failed to initialize editor: ' + error.message);
      this.loading = false;
    }
  }

  /**
   * Initialize sample slider configurations for demonstration
   */
  private initializeSampleSliders(): void {
    this.sliderConfigurations = [
      {
        id: 'age-range',
        label: 'Patient Age Range',
        type: 'range-dual',
        category: 'demographics',
        description: 'Specify the age range for the measure population',
        min: 0,
        max: 120,
        step: 1,
        unit: 'years',
        value: [40, 65],
        presets: [
          { label: 'Adults', value: [18, 65] },
          { label: 'Seniors', value: [65, 120] },
          { label: 'All Ages', value: [0, 120] },
        ],
        cqlGenerator: (value: any) =>
          this.cqlGeneratorService.generateAgeRangeCql(value),
      } as any,
      {
        id: 'measurement-period',
        label: 'Measurement Period',
        type: 'period-selector',
        category: 'timing',
        description: 'Select the time period for measure evaluation',
        options: [
          { label: '30 days', value: 30 },
          { label: '90 days', value: 90 },
          { label: '6 months', value: 180 },
          { label: '12 months', value: 365 },
        ],
        value: 365,
        allowCustom: true,
        customMax: 730,
        cqlGenerator: (value: any) =>
          this.cqlGeneratorService.generatePeriodCql(value),
      } as any,
    ];
  }

  /**
   * Subscribe to algorithm changes
   */
  private subscribeToAlgorithmChanges(): void {
    this.algorithmBuilderService.algorithm$
      .pipe(takeUntil(this.destroy$))
      .subscribe((algorithm) => {
        if (algorithm) {
          this.algorithm = algorithm;
          this.updateCql();
          this.isDirty = true;
          this.updateUndoRedoState();
        }
      });
  }

  /**
   * Subscribe to state changes
   */
  private subscribeToStateChanges(): void {
    this.algorithmBuilderService.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe((state) => {
        this.state = state;
      });
  }

  /**
   * Generate CQL from current algorithm and sliders
   */
  private updateCql(): void {
    if (this.algorithm) {
      this.currentCql = this.cqlGeneratorService.generateCompleteCql(
        this.algorithm,
        this.sliderConfigurations,
        this.state?.name || 'Untitled Measure'
      );
    }
  }

  /**
   * Handle slider value changes
   */
  onSliderChange(slideConfig: SliderConfig): void {
    this.updateCql();
    this.isDirty = true;
    // Could emit event to update algorithm based on slider
  }

  /**
   * Handle algorithm block changes
   */
  onBlockConditionChange(blockId: string, condition: string): void {
    this.algorithmBuilderService.updateBlockCondition(blockId, condition);
  }

  /**
   * Save measure
   */
  async saveMeasure(): Promise<void> {
    if (!this.state || !this.algorithm) {
      this.toastService.error('Cannot save: Missing measure data');
      return;
    }

    const validation = this.algorithmBuilderService.validateAlgorithm(this.algorithm);
    if (!validation.valid) {
      this.toastService.error('Cannot save: ' + validation.errors.join(', '));
      return;
    }

    this.saving = true;
    try {
      // Validate CQL
      const cqlValidation = this.cqlGeneratorService.validateCql(this.currentCql);
      if (!cqlValidation.valid) {
        this.toastService.warning(
          'CQL has warnings: ' + cqlValidation.errors.join(', ')
        );
      }

      // Save via service
      if (this.state.measureId) {
        // Update existing
        await this.customMeasureService
          .updateCql(this.state.measureId, this.currentCql)
          .toPromise();
        this.toastService.success('Measure updated successfully');
      } else {
        // Create new
        const payload: any = {
          name: this.state.name,
          description: this.state.description,
          category: this.state.category,
          cqlText: this.currentCql,
        };

        const created = await this.customMeasureService
          .createDraft(payload)
          .toPromise();

        if (created) {
          this.state.measureId = created.id;
          this.toastService.success('Measure created successfully');
        }
      }

      this.isDirty = false;
      this.saving = false;
    } catch (error: any) {
      this.toastService.error('Failed to save measure: ' + error.message);
      this.saving = false;
    }
  }

  /**
   * Test measure
   */
  async testMeasure(): Promise<void> {
    if (!this.state?.measureId) {
      this.toastService.error('Please save the measure first');
      return;
    }

    try {
      // Call test endpoint
      // const results = await this.customMeasureService
      //   .testMeasure(this.state.measureId)
      //   .toPromise();
      // this.toastService.success('Measure test completed');
    } catch (error: any) {
      this.toastService.error('Test failed: ' + error.message);
    }
  }

  /**
   * Publish measure
   */
  async publishMeasure(): Promise<void> {
    if (!this.state?.measureId) {
      this.toastService.error('Please save the measure first');
      return;
    }

    try {
      // Call publish endpoint
      // const result = await this.customMeasureService
      //   .publish(this.state.measureId)
      //   .toPromise();
      // this.toastService.success('Measure published successfully');
    } catch (error: any) {
      this.toastService.error('Publish failed: ' + error.message);
    }
  }

  /**
   * Undo last change
   */
  undo(): void {
    this.algorithmBuilderService.undo();
    this.updateUndoRedoState();
  }

  /**
   * Redo last undone change
   */
  redo(): void {
    this.algorithmBuilderService.redo();
    this.updateUndoRedoState();
  }

  /**
   * Update undo/redo button states
   */
  private updateUndoRedoState(): void {
    this.canUndo = this.algorithmBuilderService.canUndo();
    this.canRedo = this.algorithmBuilderService.canRedo();
  }

  /**
   * Toggle full screen mode
   */
  toggleFullScreen(): void {
    this.fullScreenMode = !this.fullScreenMode;
  }

  /**
   * Toggle left sidebar
   */
  toggleLeftSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  /**
   * Toggle right panel
   */
  toggleRightPanel(): void {
    this.rightPanelOpen = !this.rightPanelOpen;
  }

  /**
   * Toggle CQL preview visibility
   */
  toggleCqlPreview(): void {
    this.showCqlPreview = !this.showCqlPreview;
  }

  /**
   * Export measure as JSON
   */
  exportMeasure(): void {
    if (!this.state || !this.algorithm) {
      this.toastService.error('No measure to export');
      return;
    }

    const payload = {
      state: this.state,
      algorithm: this.algorithm,
      cql: this.currentCql,
      exportedAt: new Date().toISOString(),
    };

    const json = JSON.stringify(payload, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${this.state.name || 'measure'}-export.json`;
    link.click();
    window.URL.revokeObjectURL(url);

    this.toastService.success('Measure exported');
  }

  /**
   * Copy CQL to clipboard
   */
  copyCqlToClipboard(): void {
    navigator.clipboard.writeText(this.currentCql).then(() => {
      this.toastService.success('CQL copied to clipboard');
    });
  }

  /**
   * Keyboard shortcuts
   */
  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (event.ctrlKey || event.metaKey) {
      switch (event.key.toLowerCase()) {
        case 'z':
          event.preventDefault();
          this.undo();
          break;
        case 'y':
          event.preventDefault();
          this.redo();
          break;
        case 's':
          event.preventDefault();
          this.saveMeasure();
          break;
      }
    }

    if (event.key === '?') {
      event.preventDefault();
      this.showKeyboardShortcuts();
    }
  }

  /**
   * Show keyboard shortcuts help
   */
  private showKeyboardShortcuts(): void {
    const help = `
Keyboard Shortcuts:
Ctrl+Z (Cmd+Z) - Undo
Ctrl+Y (Cmd+Y) - Redo
Ctrl+S (Cmd+S) - Save
? - Show this help
    `;
    this.toastService.info(help);
  }
}
