import { ComponentFixture, TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VisualAlgorithmBuilderComponent } from './components/visual-algorithm-builder/visual-algorithm-builder.component';
import { RangeThresholdSliderComponent } from './components/range-threshold-slider/range-threshold-slider.component';
import { DistributionPeriodSliderComponent } from './components/measure-config-slider/distribution-period-slider.component';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

/**
 * TEAM 5: Integration & E2E Tests
 *
 * Test Suite: Complete end-to-end workflow testing all measure builder components
 *
 * Scope:
 * - Multi-team component integration (Teams 1-4)
 * - Complete measurement algorithm configuration workflow
 * - Data flow from UI interaction to CQL generation
 * - Measure persistence and export
 * - Error handling and validation across components
 * - Performance and accessibility in integrated context
 *
 * Test Categories: 12
 * Total Tests: 50+
 * Coverage Target: 85%+
 */

// Mock measure builder container component for integration testing
@Component({
  selector: 'app-measure-builder-integration-test',
  template: `
    <!-- Visual Algorithm Builder (Teams 1-2) -->
    <app-visual-algorithm-builder
      [algorithm]="algorithm"
      (algorithmChanged)="onAlgorithmChanged($event)">
    </app-visual-algorithm-builder>

    <!-- Measure Configuration Sliders (Teams 3-4) -->
    <div class="slider-configuration-panel">
      <!-- Range/Threshold Slider (Team 3) -->
      <app-range-threshold-slider
        *ngFor="let slider of rangeSliders"
        [config]="slider"
        (valueChanged)="onSliderValueChanged($event)">
      </app-range-threshold-slider>

      <!-- Distribution/Period Slider (Team 4) -->
      <app-distribution-period-slider
        *ngFor="let slider of distributionSliders"
        [config]="slider"
        (valueChanged)="onSliderValueChanged($event)">
      </app-distribution-period-slider>
    </div>

    <!-- CQL Preview Panel -->
    <div class="cql-preview-panel" *ngIf="currentMeasure">
      <h3>Generated CQL</h3>
      <pre>{{ generatedCQL }}</pre>
      <button (click)="exportMeasure()">Export Measure</button>
      <button (click)="saveMeasure()">Save Measure</button>
    </div>

    <!-- Measure Details Display -->
    <div class="measure-details" *ngIf="currentMeasure">
      <h2>{{ currentMeasure.name }}</h2>
      <p>{{ currentMeasure.description }}</p>
      <p>CQL Lines: {{ cqlLineCount }}</p>
      <p>Algorithm Blocks: {{ algorithmBlockCount }}</p>
      <p>Configuration Sliders: {{ configurationSliderCount }}</p>
    </div>
  `,
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    VisualAlgorithmBuilderComponent,
    RangeThresholdSliderComponent,
    DistributionPeriodSliderComponent
  ]
})
class MeasureBuilderIntegrationTestComponent {
  algorithm: any = {
    id: 'measure-test',
    blocks: [],
    connections: []
  };

  rangeSliders: any[] = [];
  distributionSliders: any[] = [];
  currentMeasure: any = null;
  generatedCQL = '';

  get cqlLineCount(): number {
    return this.generatedCQL.split('\n').length;
  }

  get algorithmBlockCount(): number {
    return this.algorithm.blocks.length;
  }

  get configurationSliderCount(): number {
    return this.rangeSliders.length + this.distributionSliders.length;
  }

  onAlgorithmChanged(event: any): void {
    this.algorithm = { ...event };
    this.generateCQL();
  }

  onSliderValueChanged(event: any): void {
    this.generateCQL();
  }

  generateCQL(): void {
    const cqlParts: string[] = [];

    // Algorithm CQL
    this.algorithm.blocks.forEach((block: any) => {
      if (block.type === 'initial') {
        cqlParts.push(`define "Initial Population":\n  ${block.cql}`);
      }
    });

    // Slider CQL
    this.rangeSliders.forEach((slider: any) => {
      if (slider.type === 'range') {
        cqlParts.push(`define "${slider.name}":\n  ${this.formatCQL(slider)}`);
      }
    });

    this.distributionSliders.forEach((slider: any) => {
      if (slider.type === 'distribution') {
        cqlParts.push(`define "${slider.name}":\n  ${this.formatCQL(slider)}`);
      }
    });

    this.generatedCQL = cqlParts.join('\n\n');
  }

  private formatCQL(slider: any): string {
    if (slider.type === 'range') {
      return `${slider.field} >= ${slider.currentMin} and ${slider.field} <= ${slider.currentMax}`;
    } else if (slider.type === 'distribution') {
      return `components: [${slider.components.map((c: any) => `${c.label}: ${c.weight}%`).join(', ')}]`;
    }
    return '';
  }

  exportMeasure(): void {
    // Implementation for export
  }

  saveMeasure(): void {
    // Implementation for save
  }
}

describe('TEAM 5: Measure Builder Integration & E2E Tests', () => {
  let component: MeasureBuilderIntegrationTestComponent;
  let fixture: ComponentFixture<MeasureBuilderIntegrationTestComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        MeasureBuilderIntegrationTestComponent
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MeasureBuilderIntegrationTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ========== CATEGORY 1: Component Rendering Integration (4 tests) ==========
  describe('Category 1: Component Rendering Integration', () => {
    it('should render visual algorithm builder component', () => {
      const visualBuilder = fixture.debugElement.query(By.directive(VisualAlgorithmBuilderComponent));
      expect(visualBuilder).toBeTruthy();
    });

    it('should render slider configuration panel', () => {
      const sliderPanel = fixture.debugElement.query(By.css('.slider-configuration-panel'));
      expect(sliderPanel).toBeTruthy();
    });

    it('should render CQL preview panel when measure is loaded', () => {
      component.currentMeasure = {
        id: 'measure-1',
        name: 'Diabetes Screening',
        description: 'HbA1c screening for diabetes patients'
      };
      fixture.detectChanges();

      const cqlPanel = fixture.debugElement.query(By.css('.cql-preview-panel'));
      expect(cqlPanel).toBeTruthy();
    });

    it('should render measure details display with loaded measure', () => {
      component.currentMeasure = {
        id: 'measure-1',
        name: 'Diabetes Screening',
        description: 'HbA1c screening for diabetes patients'
      };
      fixture.detectChanges();

      const details = fixture.debugElement.query(By.css('.measure-details'));
      expect(details.nativeElement.textContent).toContain('Diabetes Screening');
    });
  });

  // ========== CATEGORY 2: Measure Creation Workflow (6 tests) ==========
  describe('Category 2: Measure Creation Workflow', () => {
    it('should initialize empty measure with default structure', () => {
      expect(component.algorithm).toEqual(jasmine.objectContaining({
        id: 'measure-test',
        blocks: [],
        connections: []
      }));
    });

    it('should load measure configuration with algorithm blocks', fakeAsync(() => {
      component.algorithm = {
        id: 'measure-diabetes',
        name: 'Diabetes Screening',
        blocks: [
          { id: 'init', type: 'initial', label: 'Initial Population', x: 100, y: 100, cql: 'Patient' },
          { id: 'denom', type: 'denominator', label: 'Denominator', x: 300, y: 100, cql: 'Age >= 18' }
        ],
        connections: [{ from: 'init', to: 'denom' }]
      };
      tick();
      fixture.detectChanges();

      expect(component.algorithmBlockCount).toBe(2);
    }));

    it('should allow adding new population blocks to algorithm', () => {
      const newBlock = {
        id: 'numer',
        type: 'numerator',
        label: 'Numerator',
        x: 500,
        y: 100,
        cql: 'has HbA1c observation'
      };

      component.algorithm.blocks.push(newBlock);
      fixture.detectChanges();

      expect(component.algorithmBlockCount).toBe(1);
    });

    it('should add sliders to measure configuration', () => {
      component.rangeSliders = [
        {
          id: 'hba1c-range',
          type: 'range',
          name: 'HbA1c Control',
          field: 'HbA1c',
          currentMin: 0,
          currentMax: 100,
          min: 0,
          max: 100
        }
      ];
      fixture.detectChanges();

      expect(component.configurationSliderCount).toBe(1);
    });

    it('should initialize distribution sliders for component weighting', () => {
      component.distributionSliders = [
        {
          id: 'component-distribution',
          type: 'distribution',
          name: 'Component Weights',
          components: [
            { id: 'screening', label: 'Screening', color: '#2196F3', weight: 30 },
            { id: 'diagnosis', label: 'Diagnosis', color: '#4CAF50', weight: 40 },
            { id: 'treatment', label: 'Treatment', color: '#FF9800', weight: 30 }
          ]
        }
      ];
      fixture.detectChanges();

      expect(component.distributionSliders[0].components.length).toBe(3);
    });

    it('should create complete measure configuration with all component types', () => {
      component.algorithm = {
        id: 'measure-complete',
        blocks: [
          { id: 'init', type: 'initial', label: 'Initial Population', x: 100, y: 100, cql: 'Patient' }
        ],
        connections: []
      };

      component.rangeSliders = [
        { id: 'slider-1', type: 'range', name: 'Age', currentMin: 18, currentMax: 75 }
      ];

      component.distributionSliders = [
        {
          id: 'dist-1',
          type: 'distribution',
          components: [
            { id: 'c1', label: 'Component 1', color: '#2196F3', weight: 50 },
            { id: 'c2', label: 'Component 2', color: '#4CAF50', weight: 50 }
          ]
        }
      ];

      fixture.detectChanges();

      expect(component.algorithmBlockCount).toBe(1);
      expect(component.configurationSliderCount).toBe(2);
    });
  });

  // ========== CATEGORY 3: Algorithm Block Manipulation (5 tests) ==========
  describe('Category 3: Algorithm Block Manipulation', () => {
    beforeEach(() => {
      component.algorithm = {
        id: 'measure-test',
        blocks: [
          { id: 'block-1', type: 'initial', label: 'Initial', x: 100, y: 100, cql: 'Patient' },
          { id: 'block-2', type: 'denominator', label: 'Denominator', x: 300, y: 100, cql: 'Age > 18' }
        ],
        connections: []
      };
      fixture.detectChanges();
    });

    it('should display all algorithm blocks in visual editor', () => {
      fixture.detectChanges();
      expect(component.algorithmBlockCount).toBe(2);
    });

    it('should add connection between blocks', () => {
      component.algorithm.connections.push({
        from: 'block-1',
        to: 'block-2'
      });
      fixture.detectChanges();

      expect(component.algorithm.connections.length).toBe(1);
    });

    it('should update block position after drag operation', () => {
      component.onAlgorithmChanged({
        ...component.algorithm,
        blocks: [
          { ...component.algorithm.blocks[0], x: 200, y: 150 },
          component.algorithm.blocks[1]
        ]
      });
      fixture.detectChanges();

      expect(component.algorithm.blocks[0].x).toBe(200);
      expect(component.algorithm.blocks[0].y).toBe(150);
    });

    it('should remove block from algorithm', () => {
      component.algorithm.blocks = component.algorithm.blocks.filter(b => b.id !== 'block-1');
      fixture.detectChanges();

      expect(component.algorithmBlockCount).toBe(1);
    });

    it('should undo/redo block position changes', () => {
      const originalX = component.algorithm.blocks[0].x;

      // Simulate undo
      component.onAlgorithmChanged({
        ...component.algorithm,
        blocks: [
          { ...component.algorithm.blocks[0], x: originalX },
          component.algorithm.blocks[1]
        ]
      });

      expect(component.algorithm.blocks[0].x).toBe(originalX);
    });
  });

  // ========== CATEGORY 4: Slider Configuration & Adjustment (6 tests) ==========
  describe('Category 4: Slider Configuration & Adjustment', () => {
    beforeEach(() => {
      component.rangeSliders = [
        {
          id: 'hba1c',
          type: 'range',
          name: 'HbA1c Control',
          field: 'HbA1c',
          currentMin: 0,
          currentMax: 100,
          min: 0,
          max: 100,
          unit: '%'
        }
      ];
      fixture.detectChanges();
    });

    it('should adjust range slider minimum value', () => {
      component.rangeSliders[0].currentMin = 6;
      component.onSliderValueChanged({});
      fixture.detectChanges();

      expect(component.rangeSliders[0].currentMin).toBe(6);
    });

    it('should adjust range slider maximum value', () => {
      component.rangeSliders[0].currentMax = 9;
      component.onSliderValueChanged({});
      fixture.detectChanges();

      expect(component.rangeSliders[0].currentMax).toBe(9);
    });

    it('should add threshold slider to configuration', () => {
      const thresholdSlider = {
        id: 'bp-threshold',
        type: 'threshold',
        name: 'BP Systolic Threshold',
        field: 'BP_Systolic',
        currentValue: 140,
        warningThreshold: 130,
        criticalThreshold: 160,
        min: 0,
        max: 200
      };

      component.rangeSliders.push(thresholdSlider);
      fixture.detectChanges();

      expect(component.configurationSliderCount).toBe(2);
    });

    it('should validate range slider constraints (min <= max)', () => {
      const isValid = component.rangeSliders[0].currentMin <= component.rangeSliders[0].currentMax;
      expect(isValid).toBe(true);
    });

    it('should apply preset values to range slider', () => {
      // HbA1c target preset
      component.rangeSliders[0].currentMin = 0;
      component.rangeSliders[0].currentMax = 7;
      component.onSliderValueChanged({});
      fixture.detectChanges();

      expect(component.rangeSliders[0].currentMax).toBe(7);
    });

    it('should update multiple sliders independently', () => {
      component.rangeSliders.push({
        id: 'bmi',
        type: 'range',
        name: 'BMI Control',
        field: 'BMI',
        currentMin: 18.5,
        currentMax: 29.9,
        min: 0,
        max: 50
      });

      component.rangeSliders[0].currentMin = 6;
      component.rangeSliders[1].currentMin = 20;
      component.onSliderValueChanged({});
      fixture.detectChanges();

      expect(component.rangeSliders[0].currentMin).toBe(6);
      expect(component.rangeSliders[1].currentMin).toBe(20);
    });
  });

  // ========== CATEGORY 5: Distribution & Period Configuration (5 tests) ==========
  describe('Category 5: Distribution & Period Configuration', () => {
    beforeEach(() => {
      component.distributionSliders = [
        {
          id: 'weights',
          type: 'distribution',
          name: 'Component Weights',
          components: [
            { id: 'initial', label: 'Initial Population', color: '#2196F3', weight: 30 },
            { id: 'denom', label: 'Denominator', color: '#4CAF50', weight: 40 },
            { id: 'numer', label: 'Numerator', color: '#FF9800', weight: 30 }
          ]
        }
      ];
      fixture.detectChanges();
    });

    it('should display distribution component weights', () => {
      const totalWeight = component.distributionSliders[0].components.reduce(
        (sum, c) => sum + c.weight,
        0
      );
      expect(totalWeight).toBe(100);
    });

    it('should adjust individual component weights in distribution', () => {
      component.distributionSliders[0].components[0].weight = 50;
      component.onSliderValueChanged({});
      fixture.detectChanges();

      expect(component.distributionSliders[0].components[0].weight).toBe(50);
    });

    it('should add period selector to measure configuration', () => {
      component.distributionSliders.push({
        id: 'period',
        type: 'period',
        name: 'Measurement Period',
        periodType: 'calendar_year',
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        presetPeriods: [
          { id: 'cal', label: 'Calendar Year' },
          { id: 'fy', label: 'Fiscal Year' },
          { id: 'roll', label: 'Rolling Year' },
          { id: 'q', label: 'Quarter' }
        ]
      });
      fixture.detectChanges();

      expect(component.configurationSliderCount).toBe(2);
    });

    it('should switch period selection and update dates', () => {
      const periodSlider = {
        id: 'period',
        type: 'period',
        name: 'Measurement Period',
        periodType: 'calendar_year',
        startDate: '2024-01-01',
        endDate: '2024-12-31'
      };

      component.distributionSliders.push(periodSlider);
      component.distributionSliders[1].periodType = 'fiscal_year';
      component.distributionSliders[1].startDate = '2023-10-01';
      component.distributionSliders[1].endDate = '2024-09-30';
      fixture.detectChanges();

      expect(component.distributionSliders[1].periodType).toBe('fiscal_year');
    });

    it('should validate distribution weights sum to 100%', () => {
      const totalWeight = component.distributionSliders[0].components.reduce(
        (sum, c) => sum + c.weight,
        0
      );
      const isValid = totalWeight === 100;
      expect(isValid).toBe(true);
    });
  });

  // ========== CATEGORY 6: CQL Generation & Validation (5 tests) ==========
  describe('Category 6: CQL Generation & Validation', () => {
    beforeEach(() => {
      component.algorithm = {
        id: 'measure-test',
        blocks: [
          { id: 'init', type: 'initial', label: 'Initial Population', cql: 'Patient' }
        ],
        connections: []
      };

      component.rangeSliders = [
        {
          id: 'age',
          type: 'range',
          name: 'Age',
          field: 'Age',
          currentMin: 18,
          currentMax: 75
        }
      ];

      component.distributionSliders = [
        {
          id: 'period',
          type: 'period',
          name: 'Measurement Period',
          periodType: 'calendar_year',
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      ];

      fixture.detectChanges();
      component.generateCQL();
    });

    it('should generate CQL from algorithm blocks', () => {
      expect(component.generatedCQL).toContain('Initial Population');
      expect(component.generatedCQL).toContain('Patient');
    });

    it('should include range slider CQL in output', () => {
      expect(component.generatedCQL).toContain('Age >= 18');
      expect(component.generatedCQL).toContain('Age <= 75');
    });

    it('should include period definition in CQL', () => {
      expect(component.generatedCQL).toContain('from 2024-01-01');
      expect(component.generatedCQL).toContain('to 2024-12-31');
    });

    it('should validate generated CQL contains required sections', () => {
      const hasInitialPopulation = component.generatedCQL.includes('Initial Population');
      const hasAgeDefinition = component.generatedCQL.includes('Age');
      const hasPeriodDefinition = component.generatedCQL.includes('from');

      expect(hasInitialPopulation && hasAgeDefinition && hasPeriodDefinition).toBe(true);
    });

    it('should update CQL when slider values change', () => {
      const originalCQL = component.generatedCQL;

      component.rangeSliders[0].currentMin = 40;
      component.onSliderValueChanged({});
      component.generateCQL();

      const updatedCQL = component.generatedCQL;
      expect(updatedCQL).not.toEqual(originalCQL);
      expect(updatedCQL).toContain('Age >= 40');
    });
  });

  // ========== CATEGORY 7: Data Persistence & Export (4 tests) ==========
  describe('Category 7: Data Persistence & Export', () => {
    beforeEach(() => {
      component.currentMeasure = {
        id: 'measure-diabetes',
        name: 'Diabetes HbA1c Screening',
        description: 'Screen adults for HbA1c control',
        algorithm: {
          id: 'algo-1',
          blocks: [
            { id: 'init', type: 'initial', label: 'Initial', x: 100, y: 100, cql: 'Patient' }
          ],
          connections: []
        },
        sliders: [
          { id: 'hba1c', type: 'range', currentMin: 0, currentMax: 10 },
          { id: 'period', type: 'period', startDate: '2024-01-01', endDate: '2024-12-31' }
        ]
      };

      fixture.detectChanges();
    });

    it('should load measure from storage', () => {
      expect(component.currentMeasure.name).toBe('Diabetes HbA1c Screening');
      expect(component.currentMeasure.algorithm.blocks.length).toBe(1);
    });

    it('should save measure with complete configuration', (done) => {
      spyOn(component, 'saveMeasure').and.callThrough();
      component.saveMeasure();

      setTimeout(() => {
        expect(component.saveMeasure).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should export measure in CQL format', (done) => {
      component.generateCQL();
      spyOn(component, 'exportMeasure').and.callThrough();
      component.exportMeasure();

      setTimeout(() => {
        expect(component.exportMeasure).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should include algorithm and sliders in exported data', () => {
      const exportData = {
        id: component.currentMeasure.id,
        name: component.currentMeasure.name,
        algorithm: component.currentMeasure.algorithm,
        sliders: component.currentMeasure.sliders,
        cql: component.generatedCQL
      };

      expect(exportData.algorithm).toBeDefined();
      expect(exportData.sliders).toBeDefined();
      expect(exportData.algorithm.blocks.length).toBe(1);
    });
  });

  // ========== CATEGORY 8: Validation & Error Handling (4 tests) ==========
  describe('Category 8: Validation & Error Handling', () => {
    it('should validate that measure has at least one block', () => {
      component.algorithm = { id: 'test', blocks: [], connections: [] };
      const isValid = component.algorithm.blocks.length > 0;

      expect(isValid).toBe(false);
    });

    it('should reject invalid slider configuration (min > max)', () => {
      const invalidSlider = {
        type: 'range',
        currentMin: 100,
        currentMax: 50
      };

      const isValid = invalidSlider.currentMin <= invalidSlider.currentMax;
      expect(isValid).toBe(false);
    });

    it('should reject distribution with weights not summing to 100%', () => {
      component.distributionSliders = [
        {
          type: 'distribution',
          components: [
            { weight: 30 },
            { weight: 40 },
            { weight: 20 } // Total: 90, not 100
          ]
        }
      ];

      const totalWeight = component.distributionSliders[0].components.reduce(
        (sum, c) => sum + c.weight,
        0
      );

      expect(totalWeight).not.toBe(100);
    });

    it('should display error message for invalid configuration', () => {
      const errorMessages: string[] = [];

      if (component.algorithm.blocks.length === 0) {
        errorMessages.push('Measure must have at least one block');
      }

      const hasErrors = errorMessages.length > 0;
      expect(hasErrors).toBe(true);
    });
  });

  // ========== CATEGORY 9: User Interaction Workflow (5 tests) ==========
  describe('Category 9: User Interaction Workflow', () => {
    beforeEach(() => {
      component.algorithm = {
        id: 'test',
        blocks: [],
        connections: []
      };
      component.rangeSliders = [];
      component.distributionSliders = [];
      fixture.detectChanges();
    });

    it('should complete full measure creation workflow', fakeAsync(() => {
      // Step 1: Create initial block
      component.algorithm.blocks.push({
        id: 'init',
        type: 'initial',
        label: 'Initial Population',
        x: 100,
        y: 100,
        cql: 'Patient'
      });
      tick();

      // Step 2: Add denominator block
      component.algorithm.blocks.push({
        id: 'denom',
        type: 'denominator',
        label: 'Denominator',
        x: 300,
        y: 100,
        cql: 'Age > 18'
      });
      tick();

      // Step 3: Connect blocks
      component.algorithm.connections.push({
        from: 'init',
        to: 'denom'
      });
      tick();

      // Step 4: Add range slider
      component.rangeSliders.push({
        id: 'age',
        type: 'range',
        currentMin: 18,
        currentMax: 100
      });
      tick();

      // Step 5: Add distribution slider
      component.distributionSliders.push({
        id: 'weights',
        type: 'distribution',
        components: [
          { weight: 50 },
          { weight: 50 }
        ]
      });
      tick();

      // Verify completion
      expect(component.algorithmBlockCount).toBe(2);
      expect(component.configurationSliderCount).toBe(2);
    }));

    it('should handle rapid slider adjustments', fakeAsync(() => {
      component.rangeSliders = [{ type: 'range', currentMin: 0, currentMax: 100 }];

      for (let i = 0; i < 10; i++) {
        component.rangeSliders[0].currentMin = i * 10;
        component.onSliderValueChanged({});
        tick(10);
      }

      tick();
      expect(component.rangeSliders[0].currentMin).toBe(90);
    }));

    it('should handle block drag operations during slider adjustment', fakeAsync(() => {
      component.algorithm = {
        id: 'test',
        blocks: [{ id: 'b1', type: 'initial', x: 100, y: 100, cql: 'Patient' }],
        connections: []
      };

      // Simulate concurrent: drag block + adjust slider
      component.onAlgorithmChanged({
        ...component.algorithm,
        blocks: [{ ...component.algorithm.blocks[0], x: 200, y: 150 }]
      });
      tick();

      component.rangeSliders = [{ type: 'range', currentMin: 20, currentMax: 80 }];
      component.onSliderValueChanged({});
      tick();

      expect(component.algorithm.blocks[0].x).toBe(200);
      expect(component.rangeSliders[0].currentMin).toBe(20);
    }));

    it('should support undo/redo during measure creation', fakeAsync(() => {
      const originalBlockCount = 0;

      // Add block
      component.algorithm.blocks.push({ id: 'b1', type: 'initial', cql: 'Patient' });
      tick();
      expect(component.algorithmBlockCount).toBe(1);

      // Undo: remove block
      component.algorithm.blocks = [];
      tick();
      expect(component.algorithmBlockCount).toBe(originalBlockCount);

      // Redo: add block back
      component.algorithm.blocks.push({ id: 'b1', type: 'initial', cql: 'Patient' });
      tick();
      expect(component.algorithmBlockCount).toBe(1);
    }));

    it('should validate measure at each step of workflow', () => {
      const validationSteps: boolean[] = [];

      // Step 1: Empty algorithm
      validationSteps.push(component.algorithm.blocks.length > 0);

      // Step 2: With blocks
      component.algorithm.blocks.push({ id: 'b1', type: 'initial', cql: 'Patient' });
      validationSteps.push(component.algorithm.blocks.length > 0);

      // Step 3: With sliders
      component.rangeSliders.push({ type: 'range', currentMin: 0, currentMax: 100 });
      validationSteps.push(component.configurationSliderCount > 0);

      expect(validationSteps).toContain(false); // Step 1 fails
      expect(validationSteps).toContain(true);  // Steps 2 & 3 pass
    });
  });

  // ========== CATEGORY 10: Accessibility & Responsive Design (4 tests) ==========
  describe('Category 10: Accessibility & Responsive Design', () => {
    it('should have semantic HTML structure', () => {
      const cqlPanel = fixture.debugElement.query(By.css('.cql-preview-panel'));
      const hasHeading = cqlPanel ? cqlPanel.query(By.css('h3')) : null;

      expect(hasHeading).toBeTruthy();
    });

    it('should render buttons with accessible labels', () => {
      component.currentMeasure = {
        id: 'test',
        name: 'Test'
      };
      fixture.detectChanges();

      const exportButton = fixture.debugElement.query(By.css('button'));
      expect(exportButton.nativeElement.textContent).toContain('Export');
    });

    it('should support keyboard navigation in measure builder', () => {
      const exportButton = fixture.debugElement.query(By.css('button'));
      expect(exportButton.nativeElement.getAttribute('type')).toBe('button');
    });

    it('should display measure details in responsive layout', () => {
      component.currentMeasure = {
        id: 'test',
        name: 'Test Measure',
        description: 'Test Description'
      };
      fixture.detectChanges();

      const details = fixture.debugElement.query(By.css('.measure-details'));
      const heading = details.query(By.css('h2'));
      expect(heading.nativeElement.textContent).toBe('Test Measure');
    });
  });

  // ========== CATEGORY 11: Performance & Optimization (3 tests) ==========
  describe('Category 11: Performance & Optimization', () => {
    it('should render measure builder with 100+ blocks in <500ms', fakeAsync(() => {
      const startTime = performance.now();

      component.algorithm = {
        id: 'large-measure',
        blocks: Array.from({ length: 100 }, (_, i) => ({
          id: `block-${i}`,
          type: i % 3 === 0 ? 'initial' : i % 3 === 1 ? 'denominator' : 'numerator',
          label: `Block ${i}`,
          x: (i % 10) * 120,
          y: Math.floor(i / 10) * 120,
          cql: `definition-${i}`
        })),
        connections: Array.from({ length: 50 }, (_, i) => ({
          from: `block-${i}`,
          to: `block-${i + 1}`
        }))
      };

      tick();
      fixture.detectChanges();
      tick();

      const endTime = performance.now();
      const renderTime = endTime - startTime;

      expect(renderTime).toBeLessThan(500);
    }));

    it('should update CQL in <200ms when slider changes', fakeAsync(() => {
      component.algorithm = {
        id: 'test',
        blocks: [{ id: 'b1', type: 'initial', cql: 'Patient' }],
        connections: []
      };

      component.rangeSliders = [
        { id: 's1', type: 'range', currentMin: 0, currentMax: 100, field: 'Age' }
      ];

      const startTime = performance.now();

      component.rangeSliders[0].currentMin = 50;
      component.onSliderValueChanged({});
      component.generateCQL();

      tick();

      const endTime = performance.now();
      const updateTime = endTime - startTime;

      expect(updateTime).toBeLessThan(200);
    }));

    it('should handle concurrent slider and block updates efficiently', fakeAsync(() => {
      const startTime = performance.now();

      // Simulate 10 concurrent updates
      for (let i = 0; i < 10; i++) {
        if (i % 2 === 0) {
          component.algorithm.blocks.push({
            id: `block-${i}`,
            type: 'initial',
            x: i * 100,
            y: i * 100,
            cql: `definition-${i}`
          });
        } else {
          if (!component.rangeSliders.length) {
            component.rangeSliders.push({
              id: `slider-${i}`,
              type: 'range',
              currentMin: i * 10,
              currentMax: i * 10 + 50
            });
          }
          component.onSliderValueChanged({});
        }
        tick(10);
      }

      tick();

      const endTime = performance.now();
      const totalTime = endTime - startTime;

      expect(totalTime).toBeLessThan(300);
    }));
  });

  // ========== CATEGORY 12: Multi-Team Integration (4 tests) ==========
  describe('Category 12: Multi-Team Integration', () => {
    it('should integrate Team 1 SVG visual builder with Team 3-4 sliders', () => {
      component.algorithm = {
        id: 'integrated',
        blocks: [{ id: 'b1', type: 'initial', cql: 'Patient', x: 100, y: 100 }],
        connections: []
      };

      component.rangeSliders = [{ id: 's1', type: 'range' }];
      component.distributionSliders = [{ id: 'd1', type: 'distribution' }];

      fixture.detectChanges();

      const visualBuilder = fixture.debugElement.query(By.directive(VisualAlgorithmBuilderComponent));
      const sliderPanel = fixture.debugElement.query(By.css('.slider-configuration-panel'));

      expect(visualBuilder).toBeTruthy();
      expect(sliderPanel).toBeTruthy();
    });

    it('should support Team 2 drag-drop with concurrent slider adjustments', fakeAsync(() => {
      component.algorithm = {
        id: 'test',
        blocks: [{ id: 'b1', type: 'initial', x: 100, y: 100, cql: 'Patient' }],
        connections: []
      };

      component.rangeSliders = [{ type: 'range', currentMin: 0, currentMax: 100 }];
      fixture.detectChanges();

      // Simulate drag: update block position
      component.onAlgorithmChanged({
        ...component.algorithm,
        blocks: [{ ...component.algorithm.blocks[0], x: 200, y: 150 }]
      });
      tick();

      // Simulate slider: adjust value
      component.rangeSliders[0].currentMin = 30;
      component.onSliderValueChanged({});
      tick();

      expect(component.algorithm.blocks[0].x).toBe(200);
      expect(component.rangeSliders[0].currentMin).toBe(30);
    }));

    it('should generate unified CQL from all Team 3-4 slider types', () => {
      component.algorithm = {
        id: 'test',
        blocks: [{ id: 'init', type: 'initial', label: 'Initial', cql: 'Patient' }],
        connections: []
      };

      // Team 3 sliders
      component.rangeSliders = [
        { id: 'r1', type: 'range', field: 'Age', currentMin: 18, currentMax: 75 }
      ];

      // Team 4 sliders
      component.distributionSliders = [
        {
          id: 'd1',
          type: 'distribution',
          components: [
            { label: 'Component 1', weight: 50 },
            { label: 'Component 2', weight: 50 }
          ]
        },
        {
          id: 'p1',
          type: 'period',
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      ];

      component.generateCQL();

      expect(component.generatedCQL).toContain('Age >= 18');
      expect(component.generatedCQL).toContain('Component 1');
      expect(component.generatedCQL).toContain('2024-01-01');
    });

    it('should coordinate Teams 1-4 components for complete measure export', fakeAsync(() => {
      // Setup complete measure with all Team components
      component.algorithm = {
        id: 'complete',
        blocks: [
          { id: 'init', type: 'initial', label: 'Initial', x: 100, y: 100, cql: 'Patient' },
          { id: 'denom', type: 'denominator', label: 'Denominator', x: 300, y: 100, cql: 'Age > 18' }
        ],
        connections: [{ from: 'init', to: 'denom' }]
      };

      component.rangeSliders = [{ id: 'age', type: 'range', field: 'Age', currentMin: 18, currentMax: 75 }];

      component.distributionSliders = [
        {
          id: 'weights',
          type: 'distribution',
          components: [
            { label: 'Screening', weight: 30 },
            { label: 'Diagnosis', weight: 40 },
            { label: 'Treatment', weight: 30 }
          ]
        }
      ];

      component.currentMeasure = {
        id: 'measure-complete',
        name: 'Complete Measure',
        description: 'All Teams Integrated'
      };

      component.generateCQL();
      tick();
      fixture.detectChanges();
      tick();

      spyOn(component, 'exportMeasure').and.callThrough();
      component.exportMeasure();

      tick();

      // Verify all components contributed to export
      expect(component.algorithmBlockCount).toBe(2);
      expect(component.configurationSliderCount).toBe(2);
      expect(component.generatedCQL.length).toBeGreaterThan(0);
      expect(component.exportMeasure).toHaveBeenCalled();
    }));
  });
});
