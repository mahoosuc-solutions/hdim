import {
  Component,
  OnInit,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  NO_ERRORS_SCHEMA
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SliderConfig, RangeSliderConfig, ThresholdSliderConfig } from '../../models/measure-builder.model';

/**
 * RangeThresholdSliderComponent handles both range and threshold sliders
 * for measure algorithm configuration.
 *
 * Features:
 * - Range Sliders: Dual-value inputs (age, BMI, etc.)
 * - Threshold Sliders: Single-value with warning/critical zones
 * - Grid-aligned positioning (20px grid)
 * - Warning/Critical indicators
 * - Preset values
 * - CQL integration
 * - Accessibility support (ARIA labels)
 */
@Component({
  selector: 'app-range-threshold-slider',
  standalone: true,
  schemas: [NO_ERRORS_SCHEMA],
  imports: [CommonModule, FormsModule],
  templateUrl: './range-threshold-slider.component.html',
  styleUrl: './range-threshold-slider.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RangeThresholdSliderComponent implements OnInit {
  @Input() config: SliderConfig | null = null;
  @Output() valueChanged = new EventEmitter<any>();

  // Threshold presets
  private readonly presets = {
    'hba1c_control': { value: 7.0, label: 'HbA1c Control (≤7%)' },
    'hba1c_target': { value: 8.0, label: 'HbA1c Target (≤8%)' },
    'bmi_normal': { value: 24.9, label: 'Normal BMI (18.5-24.9)' },
    'bmi_overweight': { value: 29.9, label: 'Overweight (25-29.9)' },
    'bp_systolic_control': { value: 130, label: 'BP Control (<130)' },
    'cholesterol_target': { value: 200, label: 'Cholesterol Target (<200)' }
  };

  // Track which preset is active
  activePreset: string | null = null;

  ngOnInit(): void {
    if (this.isThresholdSlider()) {
      this.determineActivePreset();
    }
  }

  /**
   * Check if current config is a range slider
   */
  isRangeSlider(): boolean {
    return this.config?.type === 'range';
  }

  /**
   * Check if current config is a threshold slider
   */
  isThresholdSlider(): boolean {
    return this.config?.type === 'threshold';
  }

  /**
   * Get range slider config (type guard)
   */
  getRangeConfig(): RangeSliderConfig {
    return this.config as RangeSliderConfig;
  }

  /**
   * Get threshold slider config (type guard)
   */
  getThresholdConfig(): ThresholdSliderConfig {
    return this.config as ThresholdSliderConfig;
  }

  /**
   * Handle range slider min value change
   */
  onRangeMinChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const newMin = Math.max(parseInt(target.value, 10), 0);
    const config = this.getRangeConfig();

    // Validate constraints: min must be >= minValue, <= currentMax
    const constrainedMin = Math.max(
      Math.min(newMin, config.currentMax),
      config.minValue
    );

    if (constrainedMin !== config.currentMin) {
      config.currentMin = constrainedMin;
      this.emitChange();
    }
  }

  /**
   * Handle range slider max value change
   */
  onRangeMaxChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const newMax = parseInt(target.value, 10);
    const config = this.getRangeConfig();

    // Validate constraints: max must be <= maxValue, >= currentMin
    const constrainedMax = Math.min(
      Math.max(newMax, config.currentMin),
      config.maxValue
    );

    if (constrainedMax !== config.currentMax) {
      config.currentMax = constrainedMax;
      this.emitChange();
    }
  }

  /**
   * Handle threshold slider value change
   */
  onThresholdChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const newValue = parseFloat(target.value);
    const config = this.getThresholdConfig();

    // Validate threshold is within bounds [minValue, maxValue]
    const constrainedValue = Math.max(
      Math.min(newValue, config.maxValue),
      config.minValue
    );

    if (constrainedValue !== config.currentValue) {
      config.currentValue = constrainedValue;
      this.determineActivePreset();
      this.emitChange();
    }
  }

  /**
   * Apply preset value
   */
  applyPreset(presetKey: string): void {
    if (!this.isThresholdSlider()) return;

    const preset = this.presets[presetKey as keyof typeof this.presets];
    if (preset) {
      const config = this.getThresholdConfig();
      config.currentValue = preset.value;
      this.activePreset = presetKey;
      this.emitChange();
    }
  }

  /**
   * Determine which preset matches current value
   */
  private determineActivePreset(): void {
    if (!this.isThresholdSlider()) return;

    const config = this.getThresholdConfig();
    const currentValue = config.currentValue;

    for (const [key, preset] of Object.entries(this.presets)) {
      if (Math.abs(currentValue - preset.value) < 0.1) {
        this.activePreset = key;
        return;
      }
    }

    this.activePreset = null;
  }

  /**
   * Check if value is in warning zone
   */
  isInWarningZone(): boolean {
    if (!this.isThresholdSlider()) return false;

    const config = this.getThresholdConfig();
    return (config.currentValue > config.warningThreshold &&
            config.currentValue <= config.criticalThreshold);
  }

  /**
   * Check if value is in critical zone
   */
  isInCriticalZone(): boolean {
    if (!this.isThresholdSlider()) return false;

    const config = this.getThresholdConfig();
    return config.currentValue > config.criticalThreshold;
  }

  /**
   * Get warning indicator position percentage
   */
  getWarningIndicatorPosition(): number {
    if (!this.isThresholdSlider()) return 0;

    const config = this.getThresholdConfig();
    const range = config.maxValue - config.minValue;
    const position = ((config.warningThreshold - config.minValue) / range) * 100;
    return position;
  }

  /**
   * Get critical indicator position percentage
   */
  getCriticalIndicatorPosition(): number {
    if (!this.isThresholdSlider()) return 0;

    const config = this.getThresholdConfig();
    const range = config.maxValue - config.minValue;
    const position = ((config.criticalThreshold - config.minValue) / range) * 100;
    return position;
  }

  /**
   * Get track fill percentage for range slider
   */
  getTrackFillPercentage(): { left: string; width: string } {
    if (!this.isRangeSlider()) return { left: '0%', width: '0%' };

    const config = this.getRangeConfig();
    const range = config.maxValue - config.minValue;
    const minPercent = ((config.currentMin - config.minValue) / range) * 100;
    const maxPercent = ((config.currentMax - config.minValue) / range) * 100;
    const width = maxPercent - minPercent;

    return {
      left: `${minPercent}%`,
      width: `${width}%`
    };
  }

  /**
   * Generate CQL for the slider configuration
   */
  generateCQL(): string {
    if (this.isRangeSlider()) {
      return this.generateRangeCQL();
    } else if (this.isThresholdSlider()) {
      return this.generateThresholdCQL();
    }
    return '';
  }

  /**
   * Generate CQL for range slider
   */
  private generateRangeCQL(): string {
    const config = this.getRangeConfig();
    const fieldName = config.id.replace(/-/g, '_');

    return `${fieldName} >= ${config.currentMin} and ${fieldName} <= ${config.currentMax}`;
  }

  /**
   * Generate CQL for threshold slider
   */
  private generateThresholdCQL(): string {
    const config = this.getThresholdConfig();
    const fieldName = config.id.replace(/-/g, '_');

    return `${fieldName} <= ${config.currentValue} ${config.unit}`;
  }

  /**
   * Emit value change event
   */
  private emitChange(): void {
    const config = this.config;

    if (this.isRangeSlider()) {
      const rangeConfig = config as RangeSliderConfig;
      this.valueChanged.emit({
        id: config?.id,
        currentMin: rangeConfig.currentMin,
        currentMax: rangeConfig.currentMax,
        type: 'range'
      });
    } else if (this.isThresholdSlider()) {
      const thresholdConfig = config as ThresholdSliderConfig;
      this.valueChanged.emit({
        id: config?.id,
        currentValue: thresholdConfig.currentValue,
        type: 'threshold'
      });
    }
  }

  /**
   * Get available presets for threshold slider
   */
  getAvailablePresets(): Array<[string, { value: number; label: string }]> {
    return Object.entries(this.presets);
  }

  /**
   * Check if preset is active
   */
  isPresetActive(presetKey: string): boolean {
    return this.activePreset === presetKey;
  }
}
