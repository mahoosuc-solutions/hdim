# Measure Builder Enhancement - TDD Swarm Execution Guide

**Status:** Ready for Team Execution
**Timeline:** 3-4 weeks (Phases 2-4)
**Teams:** 4 Parallel Swarms
**Methodology:** Test-Driven Development with Git Worktrees

---

## Executive Summary

This document outlines the TDD Swarm execution plan for implementing the remaining phases of the Enhanced Measure Builder using proven team coordination patterns from Phase 1.3 Event Sourcing delivery.

**Deliverables:**
- Phase 2: Visual Algorithm Builder Component (50+ tests, 1,500+ LOC)
- Phase 3: Interactive Slider Components (60+ tests, 1,800+ LOC)
- Phase 4: Integration & Testing Suite (100+ tests, 2,000+ LOC)

---

## Team Structure & Worktrees

### Git Worktree Configuration

```bash
# Main Branch
feature/enhanced-measure-builder

# Team Worktrees (Parallel Development)
├── Worktree 1: measure-builder-visual (Team 1 & 2)
│   └── Branch: feature/visual-algorithm-builder
├── Worktree 2: measure-builder-sliders (Team 3 & 4)
│   └── Branch: feature/interactive-sliders
├── Worktree 3: measure-builder-tests (Integration Team)
│   └── Branch: feature/integration-tests
└── Worktree 4: measure-builder-performance (Optimization Team)
    └── Branch: feature/performance-optimization
```

### Team Assignments

| Team | Focus | Worktree | Lead | Duration |
|------|-------|----------|------|----------|
| **Team 1** | Visual Algorithm Builder (SVG Rendering) | measure-builder-visual | Lead Dev 1 | 6 hrs |
| **Team 2** | Drag-and-Drop & Connections | measure-builder-visual | Lead Dev 2 | 6 hrs |
| **Team 3** | Range & Threshold Sliders | measure-builder-sliders | Lead Dev 3 | 5 hrs |
| **Team 4** | Distribution & Period Sliders | measure-builder-sliders | Lead Dev 4 | 5 hrs |
| **Team 5** | Integration & E2E Tests | measure-builder-tests | QA Lead | 8 hrs |
| **Team 6** | Performance & Optimization | measure-builder-performance | Perf Lead | 6 hrs |

---

## Phase 2: Visual Algorithm Builder (Week 1)

### Team 1: SVG Flowchart Rendering

**Component:** `VisualAlgorithmBuilderComponent`

**Definition of Done:**
- [ ] SVG rendering for population blocks
- [ ] Color-coded blocks (initial, denominator, numerator, exclusion, exception)
- [ ] Connection lines between blocks
- [ ] Block hover effects and tooltips
- [ ] 40+ unit tests
- [ ] Code coverage ≥85%
- [ ] Zero compilation warnings

**Test Specification (Red-Green-Refactor)**

```typescript
// 1. RED PHASE - Write failing tests
describe('VisualAlgorithmBuilderComponent', () => {
  describe('SVG Rendering', () => {
    it('should render initial population block in blue', () => {
      // Arrange
      const algorithm: MeasureAlgorithm = {
        initialPopulation: {
          id: 'block_1',
          label: 'Initial Population',
          type: 'initial',
          color: '#1976d2',
          position: { x: 100, y: 50 },
          condition: 'test'
        }
        // ...
      };
      component.algorithm = algorithm;

      // Act
      fixture.detectChanges();

      // Assert
      const initialBlock = fixture.debugElement.query(
        By.css('[data-test="population-block-initial"]')
      );
      expect(initialBlock).toBeTruthy();
      expect(initialBlock.nativeElement.style.fill).toBe('rgb(25, 118, 210)');
    });

    it('should render denominator block in green', () => {
      // Similar pattern for denominator
    });

    it('should render numerator block in orange', () => {
      // Similar pattern for numerator
    });

    it('should render exclusion blocks in red', () => {
      // Test multiple exclusions
    });

    it('should render exception blocks in purple', () => {
      // Test multiple exceptions
    });
  });

  describe('Connection Lines', () => {
    it('should draw connection from initial to denominator', () => {
      // Verify SVG path element exists
      const connection = fixture.debugElement.query(
        By.css('path[data-source="block_initial"][data-target="block_denom"]')
      );
      expect(connection).toBeTruthy();
    });

    it('should draw exclusion connections as dashed lines', () => {
      // Verify stroke-dasharray is set
    });

    it('should draw exception connections as dotted lines', () => {
      // Verify stroke-dasharray pattern
    });
  });

  describe('Block Positioning', () => {
    it('should position blocks based on algorithm coordinates', () => {
      // Verify transform translate values
    });

    it('should update position when algorithm changes', () => {
      // Arrange
      const newAlgorithm = { ...algorithm };
      newAlgorithm.denominator.position = { x: 200, y: 300 };

      // Act
      component.algorithm = newAlgorithm;
      fixture.detectChanges();

      // Assert - verify new position
    });
  });

  describe('Interactivity', () => {
    it('should show tooltip on block hover', (done) => {
      // Simulate hover
      const block = fixture.debugElement.query(By.css('[data-test="population-block"]'));
      block.triggerEventHandler('mouseenter', null);
      fixture.detectChanges();

      fixture.whenStable().then(() => {
        const tooltip = fixture.debugElement.query(By.css('.block-tooltip'));
        expect(tooltip).toBeTruthy();
        expect(tooltip.nativeElement.textContent).toContain('Initial Population');
        done();
      });
    });

    it('should emit event when block is clicked', () => {
      // Spy on output
      spyOn(component.blockSelected, 'emit');

      const block = fixture.debugElement.query(By.css('[data-test="population-block"]'));
      block.nativeElement.click();

      expect(component.blockSelected.emit).toHaveBeenCalledWith('block_1');
    });

    it('should highlight block on selection', () => {
      // Select block
      component.selectBlock('block_1');
      fixture.detectChanges();

      const block = fixture.debugElement.query(By.css('[data-test="population-block"]'));
      expect(block.nativeElement.style.stroke).toBe('rgb(0, 0, 0)');
      expect(block.nativeElement.style.strokeWidth).toBe('3');
    });
  });
});
```

**Implementation Blueprint (Green Phase)**

```typescript
// 2. GREEN PHASE - Minimum code to pass tests
@Component({
  selector: 'app-visual-algorithm-builder',
  standalone: true,
  imports: [CommonModule, MatTooltipModule, MatButtonModule],
  template: `
    <svg [attr.width]="canvasWidth" [attr.height]="canvasHeight" class="algorithm-canvas">
      <!-- Connection Lines -->
      <g class="connections">
        <svg:path
          *ngFor="let conn of algorithm?.connections"
          [attr.d]="generateConnectionPath(conn)"
          [attr.data-source]="conn.sourceBlockId"
          [attr.data-target]="conn.targetBlockId"
          [ngClass]="'connection-' + conn.connectionType">
        </svg:path>
      </g>

      <!-- Population Blocks -->
      <g class="blocks">
        <!-- Initial Population -->
        <g *ngIf="algorithm?.initialPopulation"
           [attr.data-test]="'population-block-initial'"
           [ngClass]="{ selected: selectedBlockId === algorithm.initialPopulation.id }"
           (click)="selectBlock(algorithm.initialPopulation.id)"
           (mouseenter)="onBlockHover(algorithm.initialPopulation)"
           (mouseleave)="onBlockLeave()">
          <rect
            [attr.x]="algorithm.initialPopulation.position.x"
            [attr.y]="algorithm.initialPopulation.position.y"
            [attr.width]="blockWidth"
            [attr.height]="blockHeight"
            [attr.fill]="algorithm.initialPopulation.color"
            class="population-block">
          </rect>
          <text
            [attr.x]="algorithm.initialPopulation.position.x + blockWidth/2"
            [attr.y]="algorithm.initialPopulation.position.y + blockHeight/2"
            text-anchor="middle"
            dominant-baseline="middle">
            {{ algorithm.initialPopulation.label }}
          </text>
        </g>

        <!-- Denominator Block -->
        <g *ngIf="algorithm?.denominator"
           [attr.data-test]="'population-block-denominator'"
           [ngClass]="{ selected: selectedBlockId === algorithm.denominator.id }"
           (click)="selectBlock(algorithm.denominator.id)">
          <!-- Similar structure -->
        </g>

        <!-- Numerator Block -->
        <g *ngIf="algorithm?.numerator"
           [attr.data-test]="'population-block-numerator'"
           [ngClass]="{ selected: selectedBlockId === algorithm.numerator.id }"
           (click)="selectBlock(algorithm.numerator.id)">
          <!-- Similar structure -->
        </g>

        <!-- Exclusion Blocks -->
        <g *ngFor="let exclusion of algorithm?.exclusions"
           [attr.data-test]="'population-block-exclusion'"
           (click)="selectBlock(exclusion.id)">
          <!-- Similar structure -->
        </g>

        <!-- Exception Blocks -->
        <g *ngFor="let exception of algorithm?.exceptions"
           [attr.data-test]="'population-block-exception'"
           (click)="selectBlock(exception.id)">
          <!-- Similar structure -->
        </g>
      </g>
    </svg>

    <!-- Tooltip -->
    <div *ngIf="hoveredBlock" class="block-tooltip" [@fadeInOut]>
      <strong>{{ hoveredBlock.label }}</strong>
      <p>{{ hoveredBlock.description }}</p>
    </div>
  `,
  styles: [`
    .algorithm-canvas {
      border: 1px solid #e0e0e0;
      background: white;
      width: 100%;
      height: 500px;
    }

    .population-block {
      stroke: #333;
      stroke-width: 2;
      cursor: pointer;
      transition: stroke-width 0.2s;
    }

    .population-block:hover {
      stroke-width: 3;
    }

    .population-block.selected {
      stroke-width: 3;
      stroke: #000;
    }

    .connection-inclusion {
      stroke: #1976d2;
      stroke-width: 2;
      fill: none;
    }

    .connection-exclusion {
      stroke: #d32f2f;
      stroke-width: 2;
      stroke-dasharray: 5,5;
      fill: none;
    }

    .connection-exception {
      stroke: #7b1fa2;
      stroke-width: 2;
      stroke-dasharray: 2,2;
      fill: none;
    }

    .block-tooltip {
      position: absolute;
      background: rgba(0,0,0,0.8);
      color: white;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 12px;
      z-index: 1000;
    }
  `]
})
export class VisualAlgorithmBuilderComponent implements OnInit {
  @Input() algorithm: MeasureAlgorithm | null = null;
  @Output() blockSelected = new EventEmitter<string>();
  @Output() blockConditionChanged = new EventEmitter<{ blockId: string; condition: string }>();

  canvasWidth = 800;
  canvasHeight = 600;
  blockWidth = 150;
  blockHeight = 80;

  selectedBlockId: string | null = null;
  hoveredBlock: PopulationBlock | null = null;

  selectBlock(blockId: string): void {
    this.selectedBlockId = blockId;
    this.blockSelected.emit(blockId);
  }

  onBlockHover(block: PopulationBlock): void {
    this.hoveredBlock = block;
  }

  onBlockLeave(): void {
    this.hoveredBlock = null;
  }

  generateConnectionPath(conn: BlockConnection): string {
    // Generate SVG path for connection line
    const source = this.getBlockPosition(conn.sourceBlockId);
    const target = this.getBlockPosition(conn.targetBlockId);

    if (!source || !target) return '';

    const x1 = source.x + this.blockWidth / 2;
    const y1 = source.y + this.blockHeight;
    const x2 = target.x + this.blockWidth / 2;
    const y2 = target.y;

    return `M ${x1} ${y1} L ${x2} ${y2}`;
  }

  private getBlockPosition(blockId: string): { x: number; y: number } | null {
    if (!this.algorithm) return null;

    if (this.algorithm.initialPopulation.id === blockId) {
      return this.algorithm.initialPopulation.position;
    }
    if (this.algorithm.denominator.id === blockId) {
      return this.algorithm.denominator.position;
    }
    if (this.algorithm.numerator.id === blockId) {
      return this.algorithm.numerator.position;
    }

    const exclusion = this.algorithm.exclusions?.find(e => e.id === blockId);
    if (exclusion) return exclusion.position;

    const exception = this.algorithm.exceptions?.find(e => e.id === blockId);
    if (exception) return exception.position;

    return null;
  }
}
```

**Refactoring Phase (Blue Phase)**

```typescript
// 3. BLUE PHASE - Improve code while keeping tests passing
// Extract: SVG path generation into separate service
// Extract: Block rendering into child component
// Add: Animation framework support
// Optimize: Memoization of path calculations
```

**Acceptance Criteria:**
- ✅ All 40+ tests passing
- ✅ Code coverage ≥85%
- ✅ SVG renders correctly for all block types
- ✅ Hover and selection working
- ✅ Tooltips display correctly

---

### Team 2: Drag-and-Drop & Block Connections

**Component Enhancements:** Extend `VisualAlgorithmBuilderComponent`

**Definition of Done:**
- [ ] Drag-and-drop block repositioning
- [ ] Connection line updates on drag
- [ ] Context menu (edit, duplicate, delete)
- [ ] Connection creation UI
- [ ] Undo/redo integration
- [ ] 35+ unit tests
- [ ] Code coverage ≥85%

**Test Specification**

```typescript
describe('VisualAlgorithmBuilderComponent - Drag & Drop', () => {
  describe('Block Dragging', () => {
    it('should update block position on drag', () => {
      // Arrange
      const initialPosition = { x: 100, y: 50 };
      const draggedPosition = { x: 200, y: 150 };

      // Act
      component.onBlockDragStart('block_1', initialPosition);
      component.onBlockDragMove(draggedPosition);
      component.onBlockDragEnd();

      // Assert
      expect(component.algorithm?.denominator.position).toEqual(draggedPosition);
    });

    it('should update connection lines during drag', () => {
      // Verify paths recalculate
    });

    it('should emit position change event', () => {
      spyOn(component.blockPositionChanged, 'emit');
      component.onBlockDrag('block_1', { x: 200, y: 150 });
      expect(component.blockPositionChanged.emit).toHaveBeenCalled();
    });
  });

  describe('Context Menu', () => {
    it('should show context menu on right-click', () => {
      const block = fixture.debugElement.query(By.css('[data-test="population-block"]'));
      block.nativeElement.dispatchEvent(new MouseEvent('contextmenu', { button: 2 }));
      fixture.detectChanges();

      const menu = fixture.debugElement.query(By.css('.context-menu'));
      expect(menu).toBeTruthy();
    });

    it('should edit block condition on menu click', () => {
      component.editBlock('block_1');
      // Should open editor dialog
    });

    it('should duplicate block', () => {
      spyOn(component.blockDuplicated, 'emit');
      component.duplicateBlock('block_1');
      expect(component.blockDuplicated.emit).toHaveBeenCalled();
    });

    it('should delete block', () => {
      spyOn(component.blockDeleted, 'emit');
      component.deleteBlock('block_1');
      expect(component.blockDeleted.emit).toHaveBeenCalled();
    });
  });

  describe('Connection Creation', () => {
    it('should allow creating connection between blocks', () => {
      component.startConnection('block_initial');
      component.endConnection('block_denominator');

      expect(component.algorithm?.connections).toContain(
        jasmine.objectContaining({
          sourceBlockId: 'block_initial',
          targetBlockId: 'block_denominator'
        })
      );
    });

    it('should display connection preview during creation', () => {
      component.startConnection('block_1');
      component.updateConnectionPreview({ x: 300, y: 200 });

      const preview = fixture.debugElement.query(By.css('.connection-preview'));
      expect(preview).toBeTruthy();
    });

    it('should prevent invalid connections (cycle)', () => {
      // Numerator cannot connect to denominator
      const valid = component.isValidConnection('block_numerator', 'block_denominator');
      expect(valid).toBeFalsy();
    });
  });

  describe('Undo/Redo Integration', () => {
    it('should undo block position change', () => {
      component.onBlockDrag('block_1', { x: 200, y: 150 });
      component.undo();

      expect(component.algorithm?.denominator.position).toEqual(initialPosition);
    });

    it('should redo block deletion', () => {
      component.deleteBlock('block_1');
      component.redo();

      expect(component.algorithm?.exclusions).toContain(
        jasmine.objectContaining({ id: 'block_1' })
      );
    });
  });
});
```

---

## Phase 3: Interactive Slider Components (Week 2)

### Team 3: Range & Threshold Sliders

**Components:**
- `RangeSliderComponent` - Dual-value range slider (age, BMI)
- `ThresholdSliderComponent` - Single-value threshold (HbA1c, BP)

**Test Specification**

```typescript
describe('RangeSliderComponent', () => {
  describe('Value Changes', () => {
    it('should emit range change event', () => {
      spyOn(component.valueChanged, 'emit');

      component.setMinValue(50);
      component.setMaxValue(75);

      expect(component.valueChanged.emit).toHaveBeenCalledWith({
        min: 50,
        max: 75
      });
    });

    it('should swap min/max if min exceeds max', () => {
      component.minValue = 30;
      component.maxValue = 20;

      expect(component.minValue).toBe(20);
      expect(component.maxValue).toBe(30);
    });

    it('should constrain values within min/max bounds', () => {
      component.setMinValue(-10);  // Below minimum
      expect(component.minValue).toBe(component.min);

      component.setMaxValue(130); // Above maximum
      expect(component.maxValue).toBe(component.max);
    });
  });

  describe('Presets', () => {
    it('should apply preset values', () => {
      component.applyPreset('Adults');
      expect(component.minValue).toBe(18);
      expect(component.maxValue).toBe(65);
    });

    it('should render preset buttons', () => {
      const presets = fixture.debugElement.queryAll(By.css('.preset-button'));
      expect(presets.length).toBe(component.presets.length);
    });
  });

  describe('UI Display', () => {
    it('should display current values', () => {
      component.minValue = 40;
      component.maxValue = 65;
      fixture.detectChanges();

      const minDisplay = fixture.debugElement.query(By.css('[data-test="min-value"]'));
      expect(minDisplay.nativeElement.textContent).toContain('40');

      const maxDisplay = fixture.debugElement.query(By.css('[data-test="max-value"]'));
      expect(maxDisplay.nativeElement.textContent).toContain('65');
    });

    it('should display unit label', () => {
      component.unit = 'years';
      fixture.detectChanges();

      expect(fixture.debugElement.nativeElement.textContent).toContain('years');
    });
  });
});

describe('ThresholdSliderComponent', () => {
  describe('Threshold Indicators', () => {
    it('should show warning indicator when value >= warning threshold', () => {
      component.value = 8.5;
      component.warning = { value: 8.5, message: 'Above standard' };
      fixture.detectChanges();

      const warning = fixture.debugElement.query(By.css('.warning-indicator'));
      expect(warning).toBeTruthy();
      expect(warning.nativeElement.textContent).toContain('Above standard');
    });

    it('should show critical indicator when value >= critical threshold', () => {
      component.value = 9.0;
      component.critical = { value: 9.0, message: 'High-risk' };
      fixture.detectChanges();

      const critical = fixture.debugElement.query(By.css('.critical-indicator'));
      expect(critical).toBeTruthy();
    });
  });

  describe('Markers', () => {
    it('should render tick markers at specified values', () => {
      component.markers = [7, 7.5, 8, 8.5, 9];
      fixture.detectChanges();

      const markers = fixture.debugElement.queryAll(By.css('.marker'));
      expect(markers.length).toBe(5);
    });
  });
});
```

---

### Team 4: Distribution & Period Sliders

**Components:**
- `DistributionSliderComponent` - Component weight distribution (0-100%)
- `PeriodSelectorComponent` - Time period selection

**Test Specification**

```typescript
describe('DistributionSliderComponent', () => {
  describe('Weight Distribution', () => {
    it('should enforce total weight equals 100%', () => {
      component.components = [
        { name: 'Screening', weight: 30, color: '#1976d2' },
        { name: 'Follow-up', weight: 40, color: '#388e3c' },
        { name: 'Treatment', weight: 30, color: '#f57c00' }
      ];

      const total = component.components.reduce((sum, c) => sum + c.weight, 0);
      expect(total).toBe(100);
    });

    it('should adjust other weights when one changes', () => {
      component.updateComponentWeight('Screening', 40);

      const total = component.components.reduce((sum, c) => sum + c.weight, 0);
      expect(total).toBe(100);
    });

    it('should emit weight change event', () => {
      spyOn(component.weightsChanged, 'emit');
      component.updateComponentWeight('Screening', 35);

      expect(component.weightsChanged.emit).toHaveBeenCalledWith(
        jasmine.objectContaining({
          Screening: 35
        })
      );
    });
  });

  describe('Visual Representation', () => {
    it('should render proportional bars', () => {
      component.components = [
        { name: 'A', weight: 50, color: 'red' },
        { name: 'B', weight: 50, color: 'blue' }
      ];
      fixture.detectChanges();

      const bars = fixture.debugElement.queryAll(By.css('.weight-bar'));
      expect(bars.length).toBe(2);
      expect(parseInt(bars[0].nativeElement.style.width)).toBe(50);
      expect(parseInt(bars[1].nativeElement.style.width)).toBe(50);
    });
  });
});

describe('PeriodSelectorComponent', () => {
  describe('Period Selection', () => {
    it('should select predefined period', () => {
      component.selectPeriod(365);
      expect(component.selectedValue).toBe(365);
    });

    it('should allow custom period input', () => {
      component.allowCustom = true;
      component.setCustomPeriod(90);

      expect(component.selectedValue).toBe(90);
      expect(component.valueChanged.emit).toHaveBeenCalledWith(90);
    });

    it('should validate custom period within bounds', () => {
      component.customMax = 730;
      component.setCustomPeriod(800); // Exceeds max

      expect(component.selectedValue).toBe(730);
    });
  });

  describe('Display', () => {
    it('should display period in human-readable format', () => {
      component.selectedValue = 365;
      fixture.detectChanges();

      const display = fixture.debugElement.query(By.css('[data-test="period-display"]'));
      expect(display.nativeElement.textContent).toContain('12 months');
    });
  });
});
```

---

## Phase 4: Integration & Testing Suite (Week 3-4)

### Team 5: E2E Tests & Integration

**Test Coverage:**

```typescript
describe('Measure Builder - End-to-End Workflows', () => {
  describe('Create Measure Workflow', () => {
    it('should create measure from scratch and publish', async () => {
      // Navigate to editor
      await page.goto('/measure-builder/editor');

      // Fill metadata
      await page.fill('[data-test="measure-name"]', 'Diabetes HbA1c Control');
      await page.fill('[data-test="measure-description"]', 'Monitor HbA1c levels');

      // Configure algorithm
      await page.click('[data-test="add-exclusion"]');

      // Adjust sliders
      await page.fill('[data-test="age-min"]', '40');
      await page.fill('[data-test="age-max"]', '65');
      await page.fill('[data-test="hba1c-target"]', '7.0');

      // Save
      await page.click('[data-test="save-button"]');
      await page.waitForNavigation();

      // Verify saved
      expect(await page.textContent('[data-test="measure-status"]'))
        .toContain('Saved');
    });
  });

  describe('Algorithm Editing', () => {
    it('should drag blocks and update connections', async () => {
      // Drag denominator block
      await page.drag('[data-test="block-denominator"]', { x: 100, y: 50 });

      // Verify connection line updated
      const path = await page.$('[data-source="block_initial"]');
      expect(path).toBeTruthy();
    });
  });

  describe('CQL Preview', () => {
    it('should update CQL when sliders change', async () => {
      // Change HbA1c target
      await page.fill('[data-test="hba1c-target"]', '8.0');

      // Wait for CQL to update
      await page.waitForSelector('[data-test="cql-output"]');

      const cql = await page.textContent('[data-test="cql-output"]');
      expect(cql).toContain('< 8.0');
    });
  });

  describe('Undo/Redo', () => {
    it('should undo changes with keyboard shortcut', async () => {
      const initialValue = await page.inputValue('[data-test="hba1c-target"]');

      await page.fill('[data-test="hba1c-target"]', '8.5');
      await page.keyboard.press('Control+Z');

      const value = await page.inputValue('[data-test="hba1c-target"]');
      expect(value).toBe(initialValue);
    });
  });
});
```

### Team 6: Performance & Optimization

**Performance Targets:**

```typescript
describe('Measure Builder - Performance', () => {
  describe('Rendering Performance', () => {
    it('should render with 50+ blocks in < 500ms', async () => {
      const start = performance.now();

      component.algorithm = generateLargeAlgorithm(50);
      fixture.detectChanges();
      await fixture.whenStable();

      const duration = performance.now() - start;
      expect(duration).toBeLessThan(500);
    });

    it('should handle rapid slider changes smoothly', async () => {
      const start = performance.now();

      for (let i = 0; i < 100; i++) {
        component.updateSlider('age-min', 40 + i);
        fixture.detectChanges();
      }

      const duration = performance.now() - start;
      expect(duration).toBeLessThan(1000);
    });
  });

  describe('Memory Usage', () => {
    it('should not leak memory on repeated operations', async () => {
      const initialMemory = performance.memory?.usedJSHeapSize;

      for (let i = 0; i < 100; i++) {
        component.addExclusionBlock();
        fixture.detectChanges();
      }

      const finalMemory = performance.memory?.usedJSHeapSize;
      const increase = (finalMemory! - initialMemory!) / initialMemory!;

      expect(increase).toBeLessThan(0.1); // Less than 10% increase
    });
  });

  describe('CQL Generation Performance', () => {
    it('should generate CQL for 20 components in < 100ms', () => {
      const start = performance.now();

      const cql = service.generateCompleteCql(
        generateLargeAlgorithm(20),
        generateSliders(50),
        'Large Measure'
      );

      const duration = performance.now() - start;
      expect(duration).toBeLessThan(100);
      expect(cql.length).toBeGreaterThan(0);
    });
  });
});
```

---

## Worktree Setup Instructions

### Step 1: Create Worktrees

```bash
#!/bin/bash
# From project root

# Create main feature branch
git checkout -b feature/enhanced-measure-builder

# Create Team 1-2 worktree (Visual Algorithm)
git worktree add ../measure-builder-visual feature/visual-algorithm-builder
cd ../measure-builder-visual
git checkout -b feature/visual-algorithm-builder

# Create Team 3-4 worktree (Sliders)
git worktree add ../measure-builder-sliders feature/interactive-sliders
cd ../measure-builder-sliders
git checkout -b feature/interactive-sliders

# Create Team 5 worktree (Integration Tests)
git worktree add ../measure-builder-tests feature/integration-tests
cd ../measure-builder-tests
git checkout -b feature/integration-tests

# Create Team 6 worktree (Performance)
git worktree add ../measure-builder-perf feature/performance-optimization
cd ../measure-builder-perf
git checkout -b feature/performance-optimization
```

### Step 2: Each Team Sets Up Local Development

```bash
# In team's worktree directory
cd ../measure-builder-visual

# Install dependencies (if needed)
npm install

# Run tests in watch mode
npm run test:watch

# Build as you go
npm run build
```

### Step 3: Continuous Integration

```bash
# Each team before committing
./gradlew clean build test

# Check code quality
./gradlew sonarqube

# Verify coverage
./gradlew jacocoTestReport
```

---

## Merging Strategy (Week 4)

### Merge Sequence

```bash
# 1. Merge Team 1-2 work (Visual Builder)
git checkout feature/enhanced-measure-builder
git merge ../measure-builder-visual --no-ff

# 2. Merge Team 3-4 work (Sliders)
git merge ../measure-builder-sliders --no-ff

# 3. Merge Team 5 work (Tests)
git merge ../measure-builder-tests --no-ff

# 4. Merge Team 6 work (Performance)
git merge ../measure-builder-perf --no-ff

# 5. Run full integration tests
./gradlew integrationTest

# 6. Merge to master
git checkout master
git merge feature/enhanced-measure-builder --no-ff
```

---

## Definition of Done - Per Phase

### Phase 2: Visual Algorithm Builder
- [ ] All tests passing (50+ tests)
- [ ] Code coverage ≥85%
- [ ] SVG rendering working for all block types
- [ ] Drag-and-drop functional
- [ ] Context menu implemented
- [ ] Connection management working
- [ ] No compilation warnings
- [ ] Performance < 500ms for 50 blocks
- [ ] API documentation updated
- [ ] Code review approved

### Phase 3: Slider Components
- [ ] All tests passing (60+ tests)
- [ ] Code coverage ≥85%
- [ ] All slider types implemented
- [ ] Real-time CQL updates
- [ ] Preset functionality working
- [ ] Validation rules enforced
- [ ] Performance < 100ms per slider update
- [ ] No memory leaks
- [ ] Documentation complete
- [ ] Code review approved

### Phase 4: Integration & Testing
- [ ] All tests passing (100+ tests)
- [ ] E2E workflows validated
- [ ] Performance targets met
- [ ] Full integration with existing workflow
- [ ] No broken features
- [ ] Deployment ready
- [ ] User documentation complete
- [ ] Security review passed
- [ ] HIPAA compliance verified
- [ ] Ready for production

---

## Daily Standup Template

**Time:** 9:00 AM
**Duration:** 15 minutes
**Attendees:** All team leads + product owner

**Agenda:**
1. **What did we accomplish yesterday?**
   - Team 1: "Completed SVG rendering tests, 35/40 passing"
   - Team 2: "Implemented drag-and-drop, debugging context menu"
   - ...

2. **What are we working on today?**
   - Team 3: "Starting range slider component"
   - ...

3. **Any blockers?**
   - Team X: "Waiting for clarification on slider behavior"
   - ...

4. **Metrics Update**
   - Tests passing: 45/100
   - Code coverage: 72%
   - Blockers: 2

---

## Communication Channels

**Primary:** Slack #measure-builder-swarm
**Secondary:** GitHub Issues (blockers)
**Tertiary:** Weekly sync (Fridays 3 PM)

---

## Success Criteria

✅ **Code Quality**
- 100+ tests passing
- Code coverage ≥85%
- Zero compilation warnings
- SonarQube green

✅ **Functionality**
- All 5 phases implemented
- Visual builder with drag-and-drop
- All slider types working
- CQL generation real-time
- Undo/redo operational

✅ **Performance**
- Rendering: < 500ms for 50 blocks
- Sliders: < 100ms per update
- CQL generation: < 100ms for 20 components
- No memory leaks

✅ **Documentation**
- API documentation complete
- User guide ready
- Code comments for complex logic
- Architecture diagrams updated

✅ **Deployment Ready**
- Security audit passed
- HIPAA compliance verified
- Performance testing complete
- Rollback plan in place

---

## Estimated Timeline

| Week | Phase | Teams | Deliverable |
|------|-------|-------|-------------|
| **1** | Phase 2 (Visual) | 1-2 | Algorithm builder with D&D |
| **2** | Phase 3 (Sliders) | 3-4 | All slider components |
| **3** | Phase 4 (Integration) | 5-6 | E2E tests + Performance |
| **4** | Finalization | All | Merged, tested, deployed |

**Total: 4 weeks (3-4 weeks estimate confirmed)**

---

## Contingency Plans

**If tests are failing (> 20% failure rate):**
1. Extend Phase by 2 days
2. Dedicated debugging session
3. Pair programming on failing tests

**If performance targets missed:**
1. Profiling analysis
2. Code optimization sprint
3. Architecture review if needed

**If blockers appear:**
1. Escalate to tech lead
2. Parallel workaround if available
3. Adjust timeline if necessary

---

_Document Created: January 17, 2026_
_Framework: Angular 17 + NX Monorepo_
_Methodology: TDD Swarm with Git Worktrees_
_Status: Ready for Execution_
