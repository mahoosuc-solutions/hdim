# TDD Swarm Implementation Guide
## Test-Driven Development for Clinical MFE Platform

**Status**: Phase 4 Implementation Ready
**Test Scenarios**: 30 comprehensive scenarios (10 per MFE)
**Infrastructure**: TDD Swarm test harness established

---

## Overview

The TDD Swarm approach enables coordinated, test-driven implementation of the three Phase 4 MFEs:
- **mfe-quality** - Quality Measures
- **mfe-care-gaps** - Care Gap Management
- **mfe-reports** - Analytics & Dashboards

Each MFE has **10 comprehensive test scenarios** covering:
1. Core functionality
2. EventBus coordination
3. 360 pipeline integration
4. Error handling
5. Performance
6. Multi-tenant isolation
7. Integration with other MFEs

---

## Test Scenario Breakdown

### mfe-quality (10 scenarios)

| # | Scenario | Type | Focus |
|---|----------|------|-------|
| 1 | Load from 360 Pipeline | Core | Data loading |
| 2 | Filter by Status | UX | User interaction |
| 3 | View Details | UX | Component expansion |
| 4 | Emit MFE Events | Integration | EventBus |
| 5 | Handle Empty State | Error | Graceful fallback |
| 6 | Refresh Section | Integration | Partial update |
| 7 | Error Handling | Error | Exception flow |
| 8 | Multi-Tenant | Security | Data isolation |
| 9 | Performance (100+) | Perf | Large datasets |
| 10 | Integration w/ Care Gaps | Integration | MFE coordination |

### mfe-care-gaps (10 scenarios)

| # | Scenario | Type | Focus |
|---|----------|------|-------|
| 1 | Load from 360 Pipeline | Core | Data loading |
| 2 | Filter by Priority | UX | User interaction |
| 3 | View Details | UX | Component expansion |
| 4 | Close Gap | Core | Business logic |
| 5 | Emit Events | Integration | EventBus |
| 6 | Handle Empty State | Error | Success state |
| 7 | Scheduled vs Overdue | Business | Date logic |
| 8 | Bulk Actions | UX | Multi-select |
| 9 | Link to Workflows | Integration | Workflow MFE |
| 10 | Performance (200+) | Perf | Large datasets |

### mfe-reports (10 scenarios)

| # | Scenario | Type | Focus |
|---|----------|------|-------|
| 1 | Care Readiness Dashboard | Core | Score display |
| 2 | Measure Summary | Analytics | Aggregation |
| 3 | Care Gap Summary | Analytics | Aggregation |
| 4 | Trend Analysis | Analytics | Time series |
| 5 | Population Segmentation | Analytics | Grouping |
| 6 | Export to PDF | UX | Report generation |
| 7 | Date Range Filter | UX | Date selection |
| 8 | Population Comparison | Analytics | Comparative analysis |
| 9 | Drill Down to Patients | Navigation | Deep linking |
| 10 | Real-Time Updates | Integration | EventBus subscription |

---

## How to Run TDD Swarm

### Setup Testing Library
```bash
# Create testing library (already done)
ls libs/shared/testing/src/lib/
# - tdd-harness.ts
# - mfe-quality.test-scenarios.ts
# - mfe-care-gaps.test-scenarios.ts
# - mfe-reports.test-scenarios.ts
```

### Implementation Strategy

For each MFE, follow this pattern:

#### 1. Create MFE Application
```bash
nx generate @nx/angular:application mfe-quality --routing
```

#### 2. Add Module Federation Config
```typescript
// apps/mfe-quality/webpack.config.ts
module.exports = {
  output: {
    uniqueName: "mfeQuality",
    publicPath: "auto",
  },
  experiments: {
    outputModule: true,
  },
  plugins: [
    new ModuleFederationPlugin({
      name: "mfeQuality",
      filename: "remoteEntry.mjs",
      exposes: {
        "./Routes": "apps/mfe-quality/src/app/remote-entry/entry.routes.ts",
      },
      shared: {
        "@angular/core": { singleton: true, strictVersion: false },
        "@angular/common": { singleton: true, strictVersion: false },
        "@health-platform/shared/state": { singleton: true },
        "@health-platform/shared/data-access": { singleton: true },
      },
    }),
  ],
};
```

#### 3. Import Test Scenarios
```typescript
// apps/mfe-quality/src/app/test.spec.ts
import {
  TDDSwarmTestManager,
  QUALITY_MEASURE_SCENARIOS
} from '@health-platform/shared/testing';

describe('mfe-quality - TDD Swarm', () => {
  let testManager: TDDSwarmTestManager;

  beforeEach(() => {
    testManager = new TDDSwarmTestManager();
    QUALITY_MEASURE_SCENARIOS.forEach(scenario => {
      testManager.registerScenario(scenario);
    });
  });

  it('should pass all quality measure scenarios', async () => {
    const results = await testManager.runSwarm();
    testManager.printReport();

    const failed = results.filter(r => !r.passed);
    expect(failed.length).toBe(0);
  });
});
```

#### 4. Implement Components to Pass Tests

Test scenarios guide implementation:

```typescript
// apps/mfe-quality/src/app/quality-measures/quality-measures.component.ts
@Component({
  selector: 'app-quality-measures',
  template: `
    <!-- Load from 360 (Scenario 1) -->
    <div *ngIf="qualityMeasures$ | async as measures; else loading">
      <!-- Filter by status (Scenario 2) -->
      <div class="filters">
        <button *ngFor="let status of statuses"
                (click)="filterByStatus(status)"
                [class.active]="activeFilter === status">
          {{ status }}
        </button>
      </div>

      <!-- Display measures (Scenario 3 - drill in) -->
      <table>
        <tr *ngFor="let measure of filteredMeasures$ | async">
          <td (click)="showDetail(measure)">{{ measure.name }}</td>
          <td>
            <span [ngClass]="'status-' + measure.status">
              {{ measure.status }}
            </span>
          </td>
        </tr>
      </table>

      <!-- Detail panel (Scenario 3) -->
      <div *ngIf="selectedMeasure$ | async as measure">
        <app-measure-detail [measure]="measure"></app-measure-detail>
      </div>
    </div>

    <ng-template #loading>
      <app-loading></app-loading>
    </ng-template>
  `,
})
export class QualityMeasuresComponent implements OnInit {
  qualityMeasures$: Observable<QualityMeasure[]>;
  filteredMeasures$: Observable<QualityMeasure[]>;
  selectedMeasure$: Observable<QualityMeasure | null>;

  activeFilter = 'ALL';

  constructor(
    private pipeline: Clinical360PipelineService,
    private eventBus: EventBusService
  ) {}

  ngOnInit() {
    // Scenario 1: Load from 360 pipeline
    this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
      .subscribe(event => {
        this.loadMeasures(event.data.patientId);
      });
  }

  loadMeasures(patientId: string) {
    this.qualityMeasures$ = this.pipeline.clinical360$.pipe(
      map(data => data?.qualityMeasures.measures || []),
      tap(measures => {
        // Scenario 4: Emit event when loaded
        this.eventBus.emit({
          type: ClinicalEventType.MEASURE_EVALUATION_COMPLETED,
          source: 'mfe-quality',
          data: {
            patientId,
            totalMeasures: measures.length,
            measuresMet: measures.filter(m => m.status === 'MET').length,
          },
        } as any);
      })
    );

    // Scenario 2: Support filtering
    this.filteredMeasures$ = combineLatest([
      this.qualityMeasures$,
      this.activeFilterSubject$,
    ]).pipe(
      map(([measures, filter]) =>
        filter === 'ALL'
          ? measures
          : measures.filter(m => m.status === filter)
      )
    );
  }

  filterByStatus(status: string) {
    this.activeFilter = status;
    this.activeFilterSubject$.next(status);
  }

  showDetail(measure: QualityMeasure) {
    this.selectedMeasure$.next(measure);
  }
}
```

#### 5. Run TDD Swarm for MFE
```bash
# Run all quality measure test scenarios
nx test mfe-quality

# Output:
# 🐝 Starting TDD Swarm with 10 scenarios...
#
# 🧪 Scenario: Quality Measures: Load from 360 Pipeline
#    ✅ PASSED (234ms)
#
# 🧪 Scenario: Quality Measures: Filter by Status
#    ✅ PASSED (156ms)
# ...
#
# ============================================================
# 📊 TDD SWARM TEST REPORT
# ============================================================
# Total Tests:   10
# ✅ Passed:     10
# ❌ Failed:     0
# ⏱️  Duration:   2,156ms
# 📈 Pass Rate:  100%
# ============================================================
```

---

## Test Execution Order

### Sequential Implementation (Recommended)
```
Week 1:
  Day 1-2: mfe-quality (test scenarios + implementation)
  Day 3-4: mfe-care-gaps (test scenarios + implementation)
  Day 5:   mfe-reports (test scenarios + implementation)

Week 2:
  Day 1-2: Integration testing (all MFEs + 360 pipeline)
  Day 3-4: Performance testing (large datasets)
  Day 5:   Final validation + bug fixes
```

### Parallel Implementation (Aggressive)
```
Day 1: Create all 3 MFEs + module federation
Day 2: Implement mfe-quality + test scenarios
Day 3: Implement mfe-care-gaps + test scenarios
Day 4: Implement mfe-reports + test scenarios
Day 5: Integration + performance + final validation
```

---

## Key Testing Utilities

### TDDSwarmTestManager
```typescript
const manager = new TDDSwarmTestManager();

// Register scenario
manager.registerScenario(qualityMeasuresLoadScenario);

// Run all scenarios in parallel (swarm)
const results = await manager.runSwarm();

// Generate report
const report = manager.generateReport();
// { total: 10, passed: 10, failed: 0, passRate: 100 }

// Print formatted report
manager.printReport();
```

### Mock Data
```typescript
import { MOCK_CLINICAL_360_DATA } from '@health-platform/shared/testing';

// Use in tests
const data = MOCK_CLINICAL_360_DATA;
console.log(data.qualityMeasures.measures);     // 2 measures
console.log(data.careGaps.gaps);                // 2 gaps
console.log(data.metadata.dataQuality);         // All complete
```

### Test Setup
```typescript
import { setupMFETestBed, createMockProviders } from '@health-platform/shared/testing';

beforeEach(async () => {
  const mocks = createMockProviders();
  await setupMFETestBed(QualityMeasuresComponent, [
    { provide: Clinical360PipelineService, useValue: mocks.mockClinical360Service },
    { provide: EventBusService, useValue: mocks.mockEventBus },
  ]);
});
```

---

## Integration Testing

### All MFEs Together
```typescript
// Run all 30 scenarios across all MFEs
const allScenarios = [
  ...QUALITY_MEASURE_SCENARIOS,
  ...CARE_GAPS_SCENARIOS,
  ...REPORTS_SCENARIOS,
];

const manager = new TDDSwarmTestManager();
allScenarios.forEach(s => manager.registerScenario(s));
const results = await manager.runSwarm();

// Expected: 30/30 passing (100%)
```

### EventBus Coordination Testing
```typescript
// Scenario 10 for each MFE tests cross-MFE communication
// Example: Quality Measures + Care Gaps coordination

const qualityManager = new TDDSwarmTestManager();
qualityManager.registerScenario(qualityMeasuresIntegrationScenario);

const careGapsManager = new TDDSwarmTestManager();
careGapsManager.registerScenario(careGapsWorkflowScenario);

// Run in parallel, verify EventBus events flow correctly
await Promise.all([
  qualityManager.runSwarm(),
  careGapsManager.runSwarm(),
]);
```

---

## Performance Benchmarks

### Expected Build Times
- mfe-quality build: 15-20 seconds
- mfe-care-gaps build: 15-20 seconds
- mfe-reports build: 15-20 seconds
- Full shell + 3 MFEs: 45-60 seconds

### Expected Test Execution Times
- mfe-quality scenarios: 2-3 seconds
- mfe-care-gaps scenarios: 2-3 seconds
- mfe-reports scenarios: 2-3 seconds
- All 30 scenarios in parallel: 8-10 seconds

### Expected Runtime Performance
- MFE load time: <2 seconds
- Patient selection → all data loaded: <1.5 seconds
- Event emission between MFEs: <100ms
- Report generation: <3 seconds

---

## Success Criteria

### Test Coverage
- [ ] All 30 test scenarios passing
- [ ] 100% pass rate on TDD Swarm
- [ ] Zero critical test failures
- [ ] Integration tests all passing

### Functionality
- [ ] mfe-quality displays quality measures correctly
- [ ] mfe-care-gaps shows care gaps and allows closing
- [ ] mfe-reports displays care readiness dashboard
- [ ] All MFEs coordinate via EventBus

### Performance
- [ ] Build times within benchmarks
- [ ] Runtime performance meets targets
- [ ] No memory leaks detected
- [ ] Large datasets handled smoothly (100-200+ items)

### Integration
- [ ] 360 pipeline data flows correctly
- [ ] EventBus coordinates all MFEs
- [ ] State consistent across MFEs
- [ ] No race conditions

---

## Troubleshooting

### Test Fails: "Cannot find module @health-platform/shared/testing"
**Solution**: Ensure tsconfig.base.json path mapping is added for testing library

### Test Fails: "EventBus not emitting"
**Solution**: Verify EventBusService mock is provided in test setup

### MFE Not Loading: "Module not found: mfeQuality"
**Solution**: Check webpack.config.ts exposes routes correctly

### Performance Degradation: Rendering slow with 200+ items
**Solution**: Implement virtual scrolling or pagination for large lists

---

## References

- **Test Harness**: `libs/shared/testing/src/lib/tdd-harness.ts`
- **Quality Scenarios**: `libs/shared/testing/src/lib/mfe-quality.test-scenarios.ts`
- **Care Gaps Scenarios**: `libs/shared/testing/src/lib/mfe-care-gaps.test-scenarios.ts`
- **Reports Scenarios**: `libs/shared/testing/src/lib/mfe-reports.test-scenarios.ts`
- **360 Pipeline**: `libs/shared/data-access/src/lib/services/clinical-360-pipeline.service.ts`
- **EventBus**: `libs/shared/data-access/src/lib/event-bus/event-bus.service.ts`

---

**Status**: Ready to implement Phase 4 MFEs with comprehensive TDD
**Next Step**: Generate first MFE application
**Expected Completion**: 1-2 weeks

