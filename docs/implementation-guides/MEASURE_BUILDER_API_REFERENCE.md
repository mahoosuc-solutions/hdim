# Measure Builder API Reference

Quick reference guide for all services, components, and models available in the enhanced measure builder.

---

## Services

### AlgorithmBuilderService

**Location:** `services/algorithm-builder.service.ts`

#### Properties
```typescript
algorithm$: Observable<MeasureAlgorithm | null>    // Current algorithm observable
state$: Observable<MeasureBuilderState | null>     // Current state observable
```

#### Methods

**Initialization**
```typescript
initializeAlgorithm(): MeasureAlgorithm
// Creates default algorithm with initial, denominator, and numerator blocks
```

**Block Management**
```typescript
addExclusionBlock(label?: string): PopulationBlock
// Add exclusion criteria block

addExceptionBlock(label?: string): PopulationBlock
// Add exception criteria block

removeBlock(blockId: string): void
// Remove any block by ID

updateBlockPosition(blockId: string, x: number, y: number): void
// Update block canvas position (for drag-and-drop)

updateBlockCondition(blockId: string, condition: string): void
// Update block's CQL condition

duplicateBlock(blockId: string, offsetX?: number, offsetY?: number): PopulationBlock | null
// Clone block with new ID and offset position
```

**Connections**
```typescript
addConnection(sourceBlockId: string, targetBlockId: string, connectionType?: string): BlockConnection
// Create connection between blocks

removeConnection(connectionId: string): void
// Remove connection by ID
```

**History Management**
```typescript
undo(): boolean
// Undo last change, returns success boolean

redo(): boolean
// Redo last undone change

canUndo(): boolean
// Check if undo is available

canRedo(): boolean
// Check if redo is available
```

**State Management**
```typescript
getCurrentAlgorithm(): MeasureAlgorithm | null
// Get current algorithm state

getCurrentState(): MeasureBuilderState | null
// Get complete builder state

updateState(state: Partial<MeasureBuilderState>): void
// Update builder state

reset(): void
// Clear all state and history
```

**Validation**
```typescript
validateAlgorithm(algorithm: MeasureAlgorithm): { valid: boolean; errors: string[] }
// Validate algorithm structure
// Returns: { valid: boolean, errors: string[] }
```

---

### MeasureCqlGeneratorService

**Location:** `services/measure-cql-generator.service.ts`

#### Complete CQL Generation
```typescript
generateCompleteCql(
  algorithm: MeasureAlgorithm,
  sliderConfigs: SliderConfig[],
  measureName: string
): string
// Generates full CQL document from algorithm and slider values
// Returns: Complete CQL code as string
```

#### CQL Generation by Type
```typescript
generateAgeRangeCql(value: number | number[]): string
// generate: define "Age Range": AgeInYears() >= X AND AgeInYears() <= Y

generateThresholdCql(label: string, value: number, unit?: string): string
// generate: define "HbA1c Control": MostRecentObservation("2345-7") < X

generatePeriodCql(periodDays: number): string
// generate: define "Measurement Period": Interval[Today() - X days, Today()]

generateObservationRequiredCql(
  observationType: string,
  periodDays: number,
  resultRangeMin?: number,
  resultRangeMax?: number
): string
// Generate observation requirement with optional result range

generateMedicationCql(medicationCode: string, periodDays: number): string
// Generate medication presence check

generateEncounterCql(
  encounterType: string,
  minCount?: number,
  periodDays?: number
): string
// Generate encounter requirement with minimum count

generateCompositeWeightsCql(components: Array<{ name: string; weight: number }>): string
// Generate composite score calculation
```

#### Validation & Formatting
```typescript
validateCql(cql: string): { valid: boolean; errors: string[] }
// Basic CQL syntax validation
// Checks: library, using, balanced braces/parens

formatCql(cql: string): string
// Format CQL with proper indentation

extractFunctionCalls(cql: string): string[]
// Extract all function calls from CQL
// Returns: Array of unique function names

generateDocumentation(
  measureName: string,
  description?: string,
  populationCriteria?: Record<string, string>
): string
// Generate JSDoc-style CQL comments
```

---

## Models & Types

### MeasureAlgorithm
```typescript
interface MeasureAlgorithm {
  initialPopulation: PopulationBlock;      // Broadest population
  denominator: PopulationBlock;            // Who gets measured
  numerator: PopulationBlock;              // Who meets criteria
  exclusions?: PopulationBlock[];          // People to exclude
  exceptions?: PopulationBlock[];          // Medical exceptions
  connections?: BlockConnection[];         // Block relationships
  compositeWeights?: ComponentWeight[];    // For composite measures
}
```

### PopulationBlock
```typescript
interface PopulationBlock {
  id: string;                              // Unique identifier
  label: string;                           // Display name
  description?: string;
  condition: string;                       // CQL condition
  color: string;                           // Hex color (#1976d2)
  position: { x: number; y: number };      // Canvas position
  type: 'initial' | 'denominator' | 'numerator' | 'exclusion' | 'exception';
  metadata?: Record<string, any>;
}
```

### SliderConfig (Union Type)
```typescript
type SliderConfig =
  | RangeSliderConfig
  | ThresholdSliderConfig
  | DistributionSliderConfig
  | PeriodSelectorConfig;
```

### RangeSliderConfig
```typescript
interface RangeSliderConfig {
  id: string;
  label: string;                          // e.g., "Patient Age Range"
  type: 'range-dual' | 'range-single';
  category: SliderCategory;
  description?: string;
  min: number;
  max: number;
  step: number;
  value: number | number[];               // Current value(s)
  unit?: string;                          // e.g., "years"
  presets?: SliderPreset[];               // Quick-select options
  cqlGenerator: (value: any) => string;   // Function to generate CQL
  validationFn?: (value: any) => { valid: boolean; message?: string };
}
```

### ThresholdSliderConfig
```typescript
interface ThresholdSliderConfig {
  id: string;
  label: string;                          // e.g., "HbA1c Target"
  type: 'threshold';
  category: SliderCategory;
  min: number;
  max: number;
  step: number;
  value: number;
  unit?: string;                          // e.g., "%"
  warning?: { value: number; message: string };
  critical?: { value: number; message: string };
  markers?: number[];                     // Tick marks on slider
  cqlGenerator: (value: any) => string;
}
```

### SliderPreset
```typescript
interface SliderPreset {
  label: string;                          // e.g., "Standard"
  value: number | number[] | Record<string, number>;
  description?: string;
}
```

### MeasureBuilderState
```typescript
interface MeasureBuilderState {
  measureId?: string;                     // Existing measure ID (if editing)
  name: string;                           // Measure name
  description?: string;
  category: string;                       // CUSTOM, HEDIS, CMS, etc.
  algorithm: MeasureAlgorithm;            // Measure algorithm
  sliderConfigurations: SliderConfig[];   // Active sliders
  currentCql: string;                     // Generated CQL
  isDirty: boolean;                       // Unsaved changes
  lastSavedAt?: Date;
}
```

---

## Components

### MeasureBuilderEditorComponent

**Selector:** `app-measure-builder-editor`
**Location:** `editor/measure-builder-editor.component.ts`

#### Inputs
None (manages own state)

#### Outputs
None (emits via services)

#### Key Methods
```typescript
saveMeasure(): Promise<void>
// Save/create measure

publishMeasure(): Promise<void>
// Publish measure to production

testMeasure(): Promise<void>
// Run measure against sample patients

undo(): void
// Undo last change

redo(): void
// Redo last undone change

toggleFullScreen(): void
// Toggle full-screen mode

exportMeasure(): void
// Export as JSON

copyCqlToClipboard(): void
// Copy CQL code
```

#### Usage
```typescript
// In routing:
{
  path: 'measure-builder/editor',
  loadComponent: () => import('./editor/measure-builder-editor.component')
    .then(m => m.MeasureBuilderEditorComponent)
}

// In template:
<app-measure-builder-editor></app-measure-builder-editor>
```

---

### MeasurePreviewPanelComponent

**Selector:** `app-measure-preview-panel`
**Location:** `components/measure-preview-panel/measure-preview-panel.component.ts`

#### Inputs
```typescript
@Input() state: MeasureBuilderState | null
// Current measure state
```

#### Outputs
```typescript
@Output() stateChanged = new EventEmitter<Partial<MeasureBuilderState>>()
// Emitted when metadata form changes
```

#### Key Methods
```typescript
nextStep(): void
// Move to next builder step

previousStep(): void
// Move to previous step

goToStep(stepNumber: number): void
// Jump to specific step (1-5)

getCompletionPercentage(): number
// Returns 0-100 completion %

canPublish(): boolean
// Check if measure ready to publish

getStepStatus(stepNumber: number): 'completed' | 'current' | 'pending'
// Get status for specific step
```

#### Usage
```typescript
<app-measure-preview-panel
  [state]="state"
  (stateChanged)="onStateChange($event)">
</app-measure-preview-panel>
```

---

## Integration Examples

### Example 1: Create and Save a Measure
```typescript
// In component
constructor(
  private algorithmService: AlgorithmBuilderService,
  private customMeasureService: CustomMeasureService,
  private toastService: ToastService
) {}

async createMeasure() {
  // Initialize algorithm
  const algorithm = this.algorithmService.initializeAlgorithm();

  // Update conditions
  this.algorithmService.updateBlockCondition(
    algorithm.denominator.id,
    'exists([Patient] P where P.birthDate > @1990-01-01)'
  );

  // Get current algorithm
  const current = this.algorithmService.getCurrentAlgorithm();

  // Generate CQL
  const cql = this.cqlGeneratorService.generateCompleteCql(
    current!,
    [],
    'My Measure'
  );

  // Save
  const saved = await this.customMeasureService.createDraft({
    name: 'My Measure',
    cqlText: cql
  }).toPromise();

  this.toastService.success('Measure created!');
}
```

### Example 2: Use Undo/Redo
```typescript
// Undo changes
if (this.algorithmService.canUndo()) {
  this.algorithmService.undo();
}

// Redo changes
if (this.algorithmService.canRedo()) {
  this.algorithmService.redo();
}
```

### Example 3: Create Slider Configurations
```typescript
const sliders: SliderConfig[] = [
  {
    id: 'age-range',
    label: 'Patient Age',
    type: 'range-dual',
    category: 'demographics',
    min: 0,
    max: 120,
    step: 1,
    value: [40, 65],
    unit: 'years',
    cqlGenerator: (value) =>
      `AgeInYears() >= ${value[0]} AND AgeInYears() <= ${value[1]}`
  } as RangeSliderConfig,

  {
    id: 'hba1c-target',
    label: 'HbA1c Control Target',
    type: 'threshold',
    category: 'clinical-thresholds',
    min: 0,
    max: 10,
    step: 0.1,
    value: 7.0,
    unit: '%',
    warning: { value: 8.5, message: 'Above standard' },
    cqlGenerator: (value) =>
      `MostRecentHbA1c() < ${value}`
  } as ThresholdSliderConfig
];
```

---

## Keyboard Shortcuts Reference

| Shortcut | Action |
|----------|--------|
| `Ctrl+Z` / `Cmd+Z` | Undo |
| `Ctrl+Y` / `Cmd+Y` | Redo |
| `Ctrl+S` / `Cmd+S` | Save |
| `?` | Show keyboard help |

---

## Common Workflows

### Workflow 1: Create New Measure
```
1. Navigate to /measure-builder/editor
2. Fill in metadata (name, description, category)
3. Use sliders to configure demographics
4. Watch CQL update in preview
5. Add exclusions/exceptions if needed
6. Click Test to run against patients
7. Click Publish when ready
```

### Workflow 2: Edit Existing Measure
```
1. Navigate to existing measure
2. Modify metadata in left panel
3. Adjust algorithm blocks in center
4. Update slider values on right
5. Review CQL preview
6. Save changes with Ctrl+S
7. Test and publish when ready
```

### Workflow 3: Export & Reuse
```
1. Complete measure creation
2. Click Export button
3. Save JSON file locally
4. Share with team
5. Others can import and modify
```

---

## Error Handling

### Common Errors & Solutions

**"Cannot save: Denominator condition is required"**
- Solution: Ensure denominator block has a condition

**"Cannot save: Missing measure data"**
- Solution: Fill in measure name and metadata

**"CQL has warnings: Unbalanced parentheses"**
- Solution: Review CQL code in preview panel

---

## Performance Tips

1. **Debounce Slider Changes** - Changes are debounced 300ms before CQL generation
2. **Use Memoization** - CQL generator caches results
3. **Lazy Load Components** - All components are standalone for route-based lazy loading
4. **Virtual Scrolling** - For lists > 50 items, consider virtual scrolling

---

## Testing Utilities

### Unit Test Template
```typescript
describe('AlgorithmBuilderService', () => {
  let service: AlgorithmBuilderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AlgorithmBuilderService);
  });

  it('should initialize algorithm', () => {
    const algo = service.initializeAlgorithm();
    expect(algo.denominator).toBeDefined();
  });
});
```

---

## References

- **Angular Documentation:** https://angular.io
- **Material Design:** https://material.angular.io
- **CQL Specification:** https://cql.hl7.org
- **FHIR R4:** https://www.hl7.org/fhir/r4/

---

_Last Updated: January 17, 2026_
_API Version: 1.0_
_Framework: Angular 17+_
