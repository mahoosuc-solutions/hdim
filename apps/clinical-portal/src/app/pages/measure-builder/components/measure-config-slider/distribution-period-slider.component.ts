import {
  Component,
  OnInit,
  OnDestroy,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  NO_ERRORS_SCHEMA
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SliderConfig, DistributionSliderConfig, PeriodSelectorConfig } from '../../models/measure-builder.model';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

/**
 * DistributionPeriodSliderComponent handles both distribution and period selection
 * for measure algorithm configuration.
 *
 * Features:
 * - Distribution Sliders: Weight allocation across measure components
 * - Period Selectors: Measurement period definition
 * - Visual weight distribution bar
 * - Preset period options
 * - Custom period support
 * - Validation (weights sum to 100%)
 * - CQL integration
 * - Accessibility support
 */
@Component({
  selector: 'app-distribution-period-slider',
  standalone: true,
  schemas: [NO_ERRORS_SCHEMA],
  imports: [CommonModule, FormsModule],
  templateUrl: './distribution-period-slider.component.html',
  styleUrl: './distribution-period-slider.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DistributionPeriodSliderComponent implements OnInit, OnDestroy {
  @Input() config: SliderConfig | null = null;
  @Output() valueChanged = new EventEmitter<any>();

  // Track distribution validity
  isDistributionValid = true;
  validationMessage = '';

  // Performance optimization: debounce rapid weight changes
  private readonly weightChangeSubject = new Subject<{ index: number; event: Event }>();
  private readonly destroy$ = new Subject<void>();

  constructor(private changeDetectorRef: ChangeDetectorRef) {}

  ngOnInit(): void {
    // Set up debounced weight change handler for performance
    this.weightChangeSubject
      .pipe(
        debounceTime(50),
        distinctUntilChanged((prev, curr) =>
          prev.index === curr.index &&
          (curr.event.target as HTMLInputElement).value ===
          (prev.event.target as HTMLInputElement).value
        )
      )
      .subscribe(({ index, event }) => {
        this.handleDebouncedWeightChange(index, event);
      });

    if (this.isDistributionSlider()) {
      this.validateDistribution();
    }
  }

  ngOnDestroy(): void {
    this.weightChangeSubject.complete();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Check if current config is a distribution slider
   */
  isDistributionSlider(): boolean {
    return this.config?.type === 'distribution';
  }

  /**
   * Check if current config is a period selector
   */
  isPeriodSelector(): boolean {
    return this.config?.type === 'period';
  }

  /**
   * Get distribution slider config (type guard)
   */
  getDistributionConfig(): DistributionSliderConfig {
    return this.config as DistributionSliderConfig;
  }

  /**
   * Get period selector config (type guard)
   */
  getPeriodConfig(): PeriodSelectorConfig {
    return this.config as PeriodSelectorConfig;
  }

  /**
   * Handle component weight change (debounced)
   */
  onComponentWeightChange(index: number, event: Event): void {
    // Emit to debounced subject for performance optimization
    this.weightChangeSubject.next({ index, event });
  }

  /**
   * Handle debounced weight change
   */
  private handleDebouncedWeightChange(index: number, event: Event): void {
    const target = event.target as HTMLInputElement;
    const newWeight = parseInt(target.value, 10);

    if (this.isDistributionSlider()) {
      const config = this.getDistributionConfig();
      config.components[index].weight = Math.max(0, Math.min(100, newWeight));

      // Rebalance other weights if total doesn't equal 100
      const total = config.components.reduce((sum, comp) => sum + comp.weight, 0);
      if (Math.abs(total - 100) > 0.01) {
        this.rebalanceDistribution(config);
      }

      this.validateDistribution();
      this.emitChange();
      this.changeDetectorRef.markForCheck();
    }
  }

  /**
   * Rebalance distribution weights to sum to 100
   */
  private rebalanceDistribution(config: DistributionSliderConfig): void {
    const total = config.components.reduce((sum, comp) => sum + comp.weight, 0);
    if (total === 0) return;

    // Scale all weights proportionally to sum to 100
    config.components.forEach(comp => {
      comp.weight = Math.round((comp.weight / total) * 100 * 100) / 100;
    });
  }

  /**
   * Handle period preset selection
   */
  selectPeriod(periodType: string): void {
    if (!this.isPeriodSelector()) return;

    const config = this.getPeriodConfig();
    config.periodType = periodType;

    // Update dates based on period type
    this.updatePeriodDates(config, periodType);
    this.emitChange();
  }

  /**
   * Update period dates based on selected period type
   */
  private updatePeriodDates(config: PeriodSelectorConfig, periodType: string): void {
    const today = new Date();
    const currentYear = today.getFullYear();

    switch (periodType) {
      case 'calendar_year':
        config.startDate = `${currentYear}-01-01`;
        config.endDate = `${currentYear}-12-31`;
        break;
      case 'rolling_year':
        const lastYear = new Date(today);
        lastYear.setFullYear(currentYear - 1);
        config.startDate = lastYear.toISOString().split('T')[0];
        config.endDate = today.toISOString().split('T')[0];
        break;
      case 'fiscal_year':
        // October to September
        config.startDate = `${currentYear - 1}-10-01`;
        config.endDate = `${currentYear}-09-30`;
        break;
      case 'quarter':
        const quarter = Math.floor(today.getMonth() / 3);
        const quarterStart = quarter * 3;
        const quarterEnd = Math.min(quarterStart + 2, 11);
        config.startDate = `${currentYear}-${String(quarterStart + 1).padStart(2, '0')}-01`;
        // Last day of quarter month
        const lastDay = new Date(currentYear, quarterEnd + 1, 0).getDate();
        config.endDate = `${currentYear}-${String(quarterEnd + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
        break;
    }
  }

  /**
   * Validate distribution weights sum to 100
   */
  private validateDistribution(): void {
    if (!this.isDistributionSlider()) return;

    const config = this.getDistributionConfig();
    const totalWeight = config.components.reduce((sum, comp) => sum + comp.weight, 0);

    // Allow small floating-point errors (tolerance: 0.01%)
    this.isDistributionValid = Math.abs(totalWeight - 100) < 0.01;
    this.validationMessage = `Total: ${totalWeight}% (must be 100%)`;
  }

  /**
   * Check if distribution is valid
   */
  isValidDistribution(): boolean {
    if (!this.isDistributionSlider()) return true;

    const config = this.getDistributionConfig();
    const totalWeight = config.components.reduce((sum, comp) => sum + comp.weight, 0);
    // Allow small floating-point errors
    return Math.abs(totalWeight - 100) < 0.01;
  }

  /**
   * Get validation message
   */
  getValidationMessage(): string {
    return this.validationMessage;
  }

  /**
   * Get total weight for display
   */
  getTotalWeight(): number {
    if (!this.isDistributionSlider()) return 0;

    const config = this.getDistributionConfig();
    return config.components.reduce((sum, comp) => sum + comp.weight, 0);
  }

  /**
   * Get weight of specific component
   */
  getComponentWeight(index: number): number {
    if (!this.isDistributionSlider()) return 0;

    const config = this.getDistributionConfig();
    return config.components[index]?.weight || 0;
  }

  /**
   * Get distribution bar segments
   */
  getDistributionSegments(): Array<{ width: number; color: string; label: string }> {
    if (!this.isDistributionSlider()) return [];

    const config = this.getDistributionConfig();
    return config.components.map(comp => ({
      width: comp.weight,
      color: comp.color,
      label: comp.label
    }));
  }

  /**
   * Check if period is currently selected
   */
  isPeriodSelected(periodType: string): boolean {
    if (!this.isPeriodSelector()) return false;

    const config = this.getPeriodConfig();
    return config.periodType === periodType;
  }

  /**
   * Get available period presets
   */
  getAvailablePresets(): Array<{ id: string; label: string }> {
    if (!this.isPeriodSelector()) return [];

    const config = this.getPeriodConfig();
    return config.presetPeriods.map(p => ({
      id: p.id,
      label: p.label
    }));
  }

  /**
   * Generate CQL for the slider configuration
   */
  generateCQL(): string {
    if (this.isDistributionSlider()) {
      return this.generateDistributionCQL();
    } else if (this.isPeriodSelector()) {
      return this.generatePeriodCQL();
    }
    return '';
  }

  /**
   * Generate CQL for distribution slider
   */
  private generateDistributionCQL(): string {
    const config = this.getDistributionConfig();
    const weightClauses = config.components
      .map(comp => `${comp.label}: ${(comp.weight / 100).toFixed(2)}`)
      .join(', ');

    return `measure components: { ${weightClauses} }`;
  }

  /**
   * Generate CQL for period selector
   */
  private generatePeriodCQL(): string {
    const config = this.getPeriodConfig();
    return `measurement period from ${config.startDate} to ${config.endDate}`;
  }

  /**
   * Emit value change event
   */
  private emitChange(): void {
    const config = this.config;

    if (this.isDistributionSlider()) {
      const distConfig = config as DistributionSliderConfig;
      this.valueChanged.emit({
        id: config?.id,
        type: 'distribution',
        components: distConfig.components,
        totalWeight: this.getTotalWeight(),
        isValid: this.isValidDistribution()
      });
    } else if (this.isPeriodSelector()) {
      const periodConfig = config as PeriodSelectorConfig;
      this.valueChanged.emit({
        id: config?.id,
        type: 'period',
        periodType: periodConfig.periodType,
        startDate: periodConfig.startDate,
        endDate: periodConfig.endDate
      });
    }
  }
}
