import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { map, debounceTime } from 'rxjs/operators';

/**
 * TEAM 5: Integration Service
 *
 * Coordinates data flow between all measure builder components (Teams 1-4)
 * Manages measure state, CQL generation, and export functionality
 */

export interface PopulationBlock {
  id: string;
  type: 'initial' | 'denominator' | 'numerator' | 'exclusion' | 'exception';
  label: string;
  x: number;
  y: number;
  cql: string;
}

export interface BlockConnection {
  from: string;
  to: string;
}

export interface Algorithm {
  id: string;
  name?: string;
  blocks: PopulationBlock[];
  connections: BlockConnection[];
}

export interface RangeSliderConfig {
  id: string;
  type: 'range';
  name: string;
  field: string;
  currentMin: number;
  currentMax: number;
  min: number;
  max: number;
  unit?: string;
}

export interface ThresholdSliderConfig {
  id: string;
  type: 'threshold';
  name: string;
  field: string;
  currentValue: number;
  warningThreshold: number;
  criticalThreshold: number;
  min: number;
  max: number;
}

export interface DistributionComponent {
  id: string;
  label: string;
  color: string;
  weight: number;
}

export interface DistributionSliderConfig {
  id: string;
  type: 'distribution';
  name: string;
  components: DistributionComponent[];
}

export interface PeriodSliderConfig {
  id: string;
  type: 'period';
  name: string;
  periodType: 'calendar_year' | 'rolling_year' | 'fiscal_year' | 'quarter' | 'custom';
  startDate: string;
  endDate: string;
  presetPeriods?: Array<{ id: string; label: string }>;
  allowCustom?: boolean;
  customPeriodDays?: number;
  customPeriodMonths?: number;
}

export type SliderConfig = RangeSliderConfig | ThresholdSliderConfig | DistributionSliderConfig | PeriodSliderConfig;

export interface Measure {
  id: string;
  name: string;
  description: string;
  algorithm: Algorithm;
  sliders: SliderConfig[];
  cql?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface CQLSegment {
  category: 'algorithm' | 'range' | 'threshold' | 'distribution' | 'period';
  label: string;
  cql: string;
}

@Injectable({
  providedIn: 'root'
})
export class MeasureBuilderIntegrationService {
  // State subjects
  private algorithmSubject = new BehaviorSubject<Algorithm>({
    id: 'default',
    blocks: [],
    connections: []
  });

  private slidersSubject = new BehaviorSubject<SliderConfig[]>([]);
  private measureSubject = new BehaviorSubject<Measure | null>(null);
  private cqlSegmentsSubject = new BehaviorSubject<CQLSegment[]>([]);

  // Public observables
  public algorithm$ = this.algorithmSubject.asObservable();
  public sliders$ = this.slidersSubject.asObservable();
  public measure$ = this.measureSubject.asObservable();
  public cqlSegments$ = this.cqlSegmentsSubject.asObservable();

  // Combined state for complete measure
  public completeMeasure$ = combineLatest([
    this.algorithmSubject,
    this.slidersSubject,
    this.measureSubject
  ]).pipe(
    debounceTime(100),
    map(([algorithm, sliders, measure]) => ({
      algorithm,
      sliders,
      measure
    }))
  );

  constructor() {}

  // ========== Algorithm Management ==========

  /**
   * Add a new block to the algorithm
   */
  addBlock(block: PopulationBlock): void {
    const current = this.algorithmSubject.value;
    const updated = {
      ...current,
      blocks: [...current.blocks, block]
    };
    this.algorithmSubject.next(updated);
    this.regenerateCQL();
  }

  /**
   * Remove a block from the algorithm
   */
  removeBlock(blockId: string): void {
    const current = this.algorithmSubject.value;
    const updated = {
      ...current,
      blocks: current.blocks.filter(b => b.id !== blockId),
      connections: current.connections.filter(
        c => c.from !== blockId && c.to !== blockId
      )
    };
    this.algorithmSubject.next(updated);
    this.regenerateCQL();
  }

  /**
   * Update block position or properties
   */
  updateBlock(blockId: string, updates: Partial<PopulationBlock>): void {
    const current = this.algorithmSubject.value;
    const updated = {
      ...current,
      blocks: current.blocks.map(b =>
        b.id === blockId ? { ...b, ...updates } : b
      )
    };
    this.algorithmSubject.next(updated);
    this.regenerateCQL();
  }

  /**
   * Add connection between two blocks
   */
  addConnection(from: string, to: string): void {
    const current = this.algorithmSubject.value;
    const exists = current.connections.some(
      c => c.from === from && c.to === to
    );

    if (!exists) {
      const updated = {
        ...current,
        connections: [...current.connections, { from, to }]
      };
      this.algorithmSubject.next(updated);
    }
  }

  /**
   * Remove connection between blocks
   */
  removeConnection(from: string, to: string): void {
    const current = this.algorithmSubject.value;
    const updated = {
      ...current,
      connections: current.connections.filter(
        c => !(c.from === from && c.to === to)
      )
    };
    this.algorithmSubject.next(updated);
  }

  /**
   * Get all blocks in algorithm
   */
  getBlocks(): PopulationBlock[] {
    return this.algorithmSubject.value.blocks;
  }

  /**
   * Get specific block by ID
   */
  getBlock(blockId: string): PopulationBlock | undefined {
    return this.algorithmSubject.value.blocks.find(b => b.id === blockId);
  }

  // ========== Slider Management ==========

  /**
   * Add slider configuration
   */
  addSlider(config: SliderConfig): void {
    const current = this.slidersSubject.value;
    const updated = [...current, config];
    this.slidersSubject.next(updated);
    this.regenerateCQL();
  }

  /**
   * Remove slider configuration
   */
  removeSlider(sliderId: string): void {
    const current = this.slidersSubject.value;
    const updated = current.filter(s => s.id !== sliderId);
    this.slidersSubject.next(updated);
    this.regenerateCQL();
  }

  /**
   * Update slider configuration
   */
  updateSlider(sliderId: string, updates: Partial<SliderConfig>): void {
    const current = this.slidersSubject.value;
    const updated = current.map(s =>
      s.id === sliderId ? { ...s, ...updates } as SliderConfig : s
    );
    this.slidersSubject.next(updated);
    this.regenerateCQL();
  }

  /**
   * Get all sliders
   */
  getSliders(): SliderConfig[] {
    return this.slidersSubject.value;
  }

  /**
   * Get sliders by type
   */
  getSlidersByType(type: 'range' | 'threshold' | 'distribution' | 'period'): SliderConfig[] {
    return this.slidersSubject.value.filter(s => s.type === type);
  }

  // ========== CQL Generation ==========

  /**
   * Regenerate complete CQL from current state
   */
  private regenerateCQL(): void {
    const segments: CQLSegment[] = [];
    const algorithm = this.algorithmSubject.value;
    const sliders = this.slidersSubject.value;

    // Algorithm segments
    algorithm.blocks.forEach(block => {
      let label = block.label;
      if (block.type === 'initial') {
        label = 'Initial Population';
      } else if (block.type === 'denominator') {
        label = 'Denominator';
      } else if (block.type === 'numerator') {
        label = 'Numerator';
      }

      segments.push({
        category: 'algorithm',
        label,
        cql: `define "${label}":\n  ${block.cql}`
      });
    });

    // Range slider segments
    sliders.filter(s => s.type === 'range').forEach(slider => {
      const config = slider as RangeSliderConfig;
      const cql = `${config.field} >= ${config.currentMin} and ${config.field} <= ${config.currentMax}`;
      segments.push({
        category: 'range',
        label: config.name,
        cql: `define "${config.name}":\n  ${cql}`
      });
    });

    // Threshold slider segments
    sliders.filter(s => s.type === 'threshold').forEach(slider => {
      const config = slider as ThresholdSliderConfig;
      const cql = `${config.field} ${config.currentValue > config.warningThreshold ? '>' : '<='} ${config.currentValue}`;
      segments.push({
        category: 'threshold',
        label: config.name,
        cql: `define "${config.name}":\n  ${cql}`
      });
    });

    // Distribution slider segments
    sliders.filter(s => s.type === 'distribution').forEach(slider => {
      const config = slider as DistributionSliderConfig;
      const components = config.components
        .map(c => `${c.label}: ${(c.weight / 100).toFixed(2)}`)
        .join(', ');
      segments.push({
        category: 'distribution',
        label: config.name,
        cql: `measure components: { ${components} }`
      });
    });

    // Period slider segments
    sliders.filter(s => s.type === 'period').forEach(slider => {
      const config = slider as PeriodSliderConfig;
      segments.push({
        category: 'period',
        label: config.name,
        cql: `measurement period from ${config.startDate} to ${config.endDate}`
      });
    });

    this.cqlSegmentsSubject.next(segments);
  }

  /**
   * Get complete generated CQL
   */
  getGeneratedCQL(): string {
    const segments = this.cqlSegmentsSubject.value;
    return segments.map(s => s.cql).join('\n\n');
  }

  /**
   * Get CQL segments (for detailed inspection)
   */
  getCQLSegments(): CQLSegment[] {
    return this.cqlSegmentsSubject.value;
  }

  // ========== Validation ==========

  /**
   * Validate complete measure configuration
   */
  validateMeasure(): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];
    const algorithm = this.algorithmSubject.value;
    const sliders = this.slidersSubject.value;

    // Check algorithm has at least one block
    if (algorithm.blocks.length === 0) {
      errors.push('Measure must have at least one algorithm block');
    }

    // Check algorithm has initial population
    const hasInitial = algorithm.blocks.some(b => b.type === 'initial');
    if (!hasInitial && algorithm.blocks.length > 0) {
      errors.push('Measure must have an Initial Population block');
    }

    // Validate sliders
    sliders.forEach(slider => {
      if (slider.type === 'range') {
        const config = slider as RangeSliderConfig;
        if (config.currentMin > config.currentMax) {
          errors.push(`Range slider "${config.name}": min must be <= max`);
        }
      } else if (slider.type === 'distribution') {
        const config = slider as DistributionSliderConfig;
        const totalWeight = config.components.reduce((sum, c) => sum + c.weight, 0);
        if (totalWeight !== 100) {
          errors.push(`Distribution slider "${config.name}": weights must sum to 100% (current: ${totalWeight}%)`);
        }
      }
    });

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  /**
   * Check if measure is ready for export
   */
  isReadyForExport(): boolean {
    const validation = this.validateMeasure();
    return validation.isValid;
  }

  // ========== Measure Persistence ==========

  /**
   * Load measure into builder
   */
  loadMeasure(measure: Measure): void {
    this.algorithmSubject.next(measure.algorithm);
    this.slidersSubject.next(measure.sliders);
    this.measureSubject.next(measure);
    this.regenerateCQL();
  }

  /**
   * Get current measure state
   */
  getCurrentMeasure(): Measure | null {
    const algorithm = this.algorithmSubject.value;
    const sliders = this.slidersSubject.value;
    const measure = this.measureSubject.value;

    if (measure) {
      return {
        ...measure,
        algorithm,
        sliders,
        cql: this.getGeneratedCQL(),
        updatedAt: new Date()
      };
    }

    return null;
  }

  /**
   * Save measure (state preserved in service)
   */
  saveMeasure(measure: Measure): void {
    this.measureSubject.next({
      ...measure,
      updatedAt: new Date()
    });
  }

  /**
   * Export measure as JSON
   */
  exportMeasureJSON(): string {
    const current = this.getCurrentMeasure();
    if (!current) {
      throw new Error('No measure loaded');
    }

    return JSON.stringify(current, null, 2);
  }

  /**
   * Export measure as CQL file content
   */
  exportMeasureCQL(): string {
    return this.getGeneratedCQL();
  }

  /**
   * Export measure as structured export object
   */
  exportMeasure(): {
    measure: Measure;
    cql: string;
    validation: { isValid: boolean; errors: string[] };
  } {
    const current = this.getCurrentMeasure();
    if (!current) {
      throw new Error('No measure loaded');
    }

    return {
      measure: current,
      cql: this.getGeneratedCQL(),
      validation: this.validateMeasure()
    };
  }

  // ========== State Reset ==========

  /**
   * Clear all builder state
   */
  clearAll(): void {
    this.algorithmSubject.next({
      id: 'default',
      blocks: [],
      connections: []
    });
    this.slidersSubject.next([]);
    this.measureSubject.next(null);
    this.cqlSegmentsSubject.next([]);
  }

  /**
   * Reset algorithm only (keep sliders)
   */
  resetAlgorithm(): void {
    this.algorithmSubject.next({
      id: 'default',
      blocks: [],
      connections: []
    });
    this.regenerateCQL();
  }

  /**
   * Reset sliders only (keep algorithm)
   */
  resetSliders(): void {
    this.slidersSubject.next([]);
    this.regenerateCQL();
  }
}
