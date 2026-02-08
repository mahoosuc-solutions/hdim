/**
 * Measure Builder Data Models
 * Defines the structure for visual algorithm building and configuration
 */

/**
 * Represents a population block in the measure algorithm
 * Each block represents a step in the quality measure population criteria
 */
export interface PopulationBlock {
  id: string;
  label: string;
  name?: string; // Alternate property for label
  description?: string;
  condition: string; // CQL condition
  color: string;
  position: {
    x: number;
    y: number;
  };
  type: 'initial' | 'denominator' | 'numerator' | 'exclusion' | 'exception';
  metadata?: Record<string, any>;
  // Visual rendering properties
  x?: number;
  y?: number;
  width?: number;
  height?: number;
}

/**
 * Represents a connection between population blocks
 */
export interface BlockConnection {
  id: string;
  sourceBlockId: string;
  targetBlockId: string;
  // Alternate property names for compatibility
  fromBlockId?: string;
  toBlockId?: string;
  label?: string;
  connectionType: 'inclusion' | 'exclusion' | 'exception';
}

/**
 * Component weight for composite measures
 */
export interface ComponentWeight {
  componentId: string;
  componentName: string;
  weight: number; // 0-100
  color: string;
  label?: string; // Alternate property for componentName
}

/**
 * Complete measure algorithm structure
 */
export interface MeasureAlgorithm {
  initialPopulation: PopulationBlock;
  denominator: PopulationBlock;
  numerator: PopulationBlock;
  exclusions?: PopulationBlock[];
  exceptions?: PopulationBlock[];
  connections?: BlockConnection[];
  compositeWeights?: ComponentWeight[];
  // Collection of all blocks for visual builder
  blocks?: PopulationBlock[];
}

/**
 * Preset value for quick configuration
 */
export interface SliderPreset {
  label: string;
  value: number | number[] | Record<string, number>;
  description?: string;
}

/**
 * Range Slider Configuration
 */
export interface RangeSliderConfig extends BaseSliderConfig {
  type: 'range-dual' | 'range-single' | 'range';
  min: number;
  max: number;
  step: number;
  value: number | number[];
  unit?: string;
  // Alternate property names for compatibility
  minValue?: number;
  maxValue?: number;
  currentMin?: number;
  currentMax?: number;
}

/**
 * Threshold Slider Configuration
 */
export interface ThresholdSliderConfig extends BaseSliderConfig {
  type: 'threshold';
  min: number;
  max: number;
  step: number;
  value: number;
  currentValue?: number; // Alternate property for current value
  unit?: string;
  // Alternate property names for min/max
  minValue?: number;
  maxValue?: number;
  // Threshold properties
  warningThreshold?: number;
  criticalThreshold?: number;
  warning?: {
    value: number;
    message: string;
  };
  critical?: {
    value: number;
    message: string;
  };
  markers?: number[];
}

/**
 * Distribution Slider Configuration (for composite measures)
 */
export interface DistributionSliderConfig extends BaseSliderConfig {
  type: 'distribution';
  components: ComponentWeight[];
  totalRequired?: number; // Default 100
}

/**
 * Period Selector Configuration
 */
export interface PeriodSelectorConfig extends BaseSliderConfig {
  type: 'period-selector' | 'period';
  options: PeriodOption[];
  value: number | string;
  allowCustom?: boolean;
  customMax?: number;
  customPeriodDays?: number;
  customPeriodMonths?: number;
  // Date range properties
  startDate?: string | Date;
  endDate?: string | Date;
  periodType?: string;
  presetPeriods?: PeriodOption[];
}

export interface PeriodOption {
  id?: string;
  label: string;
  value: number | string;
  duration?: number; // in days
}

/**
 * Base configuration for all slider types
 */
export interface BaseSliderConfig {
  id: string;
  label: string;
  name?: string; // Alternate property for label
  type: string;
  category: SliderCategory;
  description?: string;
  presets?: SliderPreset[];
  cqlGenerator: (value: any) => string; // Function to generate CQL condition
  validationFn?: (value: any) => { valid: boolean; message?: string };
}

/**
 * Slider categories for organization
 */
export type SliderCategory =
  | 'demographics'
  | 'clinical-thresholds'
  | 'timing'
  | 'prevalence'
  | 'composite-weights'
  | 'custom';

/**
 * Union type for all slider configurations
 */
export type SliderConfig =
  | RangeSliderConfig
  | ThresholdSliderConfig
  | DistributionSliderConfig
  | PeriodSelectorConfig;

/**
 * Measure Builder State
 */
export interface MeasureBuilderState {
  measureId?: string;
  name: string;
  description?: string;
  category: string;
  algorithm: MeasureAlgorithm;
  sliderConfigurations: SliderConfig[];
  currentCql: string;
  isDirty: boolean;
  lastSavedAt?: Date;
}

/**
 * History entry for undo/redo
 */
export interface HistoryEntry {
  id: string;
  timestamp: Date;
  state: MeasureBuilderState;
  description: string;
}

/**
 * Undo/Redo Stack
 */
export interface UndoRedoStack {
  history: HistoryEntry[];
  currentIndex: number;
  maxSize: number;
}

/**
 * Measure Creation Request (for saving to backend)
 */
export interface MeasureCreationPayload {
  name: string;
  description?: string;
  category: string;
  algorithm: MeasureAlgorithm;
  cqlText: string;
  sliderConfiguration: SliderConfig[];
  valueSets?: Record<string, any>;
}
