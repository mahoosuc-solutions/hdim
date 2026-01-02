# Patient Health Service Refactoring Strategy

## Current Status (December 2025)

### File Size Analysis
- **Main Service**: `patient-health.service.ts` - 6547 lines
- **Target Size**: < 100 KB (~300-400 lines)
- **Current Status**: Monolithic service not yet refactored

### Infrastructure Status
- ✅ **Sub-services exist** and are fully functional:
  - `PhysicalHealthService` (286 lines)
  - `MentalHealthService` (388 lines)
  - `SDOHService` (424 lines)
  - `RiskStratificationService` (530 lines)
  - `HealthScoringService` (310 lines)

- ✅ **Facade pattern** implemented in `PatientHealthFacade`
  - Aggregates all sub-services
  - Provides convenient overview methods
  - Well-structured and maintainable

- ⚠️ **Components still use monolithic service**
  - No imports updated to use sub-services
  - Backward compatibility maintained through old service

## Refactoring Challenges Identified

### 1. Method Coverage Gap
Some methods exist in the monolithic service but haven't been implemented in sub-services:

**Missing from sub-services:**
- `getHospitalizationPrediction()` - Not in RiskStratificationService
- `getCareGaps()` - Not in RiskStratificationService
- `updateCareGapStatus()` - Not in RiskStratificationService
- `getCareGapMetrics()` - Not in RiskStratificationService
- `recalculateOverallRisk()` - Not in RiskStratificationService
- `getQualityMeasurePerformance()` - Not in HealthScoringService
- `getReferralMetrics()` - Not in SDOHService (partially)

### 2. Type Signature Mismatches
- `getHealthScoreHistory()` returns `Observable<HealthScoreHistory[]>` but should be single object or array?
- Multiple return type inconsistencies between services

### 3. Caching Strategy
- Monolithic service has centralized caching (Maps for vitalSigns, labResults, healthScore, etc.)
- Sub-services have independent caching
- Needs coordination to avoid duplication

## Recommended Refactoring Approach

### Phase 4A: Expand Sub-Services (Current)
**Goal**: Ensure all methods from monolithic service are available in sub-services

**Tasks**:
1. **RiskStratificationService** - Add missing care gap methods:
   ```typescript
   getCareGaps(patientId: string, status?: CareGapStatus, priority?: string): Observable<CareGap[]>
   updateCareGapStatus(update: CareGapStatusUpdate): Observable<CareGapStatusUpdate>
   getCareGapMetrics(patientId: string): Observable<CareGapMetrics>
   getHospitalizationPrediction(patientId: string): Observable<HospitalizationPrediction>
   recalculateOverallRisk(patientId: string): Observable<RiskStratification>
   ```

2. **HealthScoringService** - Add quality measure method:
   ```typescript
   getQualityMeasurePerformance(patientId: string): Observable<QualityMeasurePerformance>
   ```

3. **SDOHService** - Verify/add referral metrics:
   ```typescript
   getReferralMetrics(patientId: string): Observable<any>
   ```

### Phase 4B: Convert MonolithicService to Facade (Next)
**Goal**: Reduce main file size to < 400 lines

**Approach**:
```typescript
@Injectable({ providedIn: 'root' })
export class PatientHealthService {
  constructor(
    private physicalHealth: PhysicalHealthService,
    private mentalHealth: MentalHealthService,
    private sdoh: SDOHService,
    private riskStratification: RiskStratificationService,
    private healthScoring: HealthScoringService,
    private logger: LoggerService
  ) {}

  // Delegate all methods to appropriate sub-services
  getPhysicalHealthSummary(patientId: string): Observable<PhysicalHealthSummary> {
    return this.physicalHealth.getPhysicalHealthSummary(patientId);
  }

  // ... all other methods delegate similarly
}
```

**Benefits**:
- Maintains backward compatibility (existing imports work)
- Reduces main file from 6547 → ~350 lines
- Clear separation of concerns
- Easier to test and maintain

### Phase 4C: Migrate Components (Optional)
**Goal**: Update components to use sub-services directly

**Approach**:
Gradually migrate component imports:
```typescript
// Old way (will work, but deprecated)
import { PatientHealthService } from './services/patient-health.service';

// New way (recommended for new code)
import { PhysicalHealthService } from './services/patient-health/physical-health.service';
import { MentalHealthService } from './services/patient-health/mental-health.service';
```

## Implementation Plan

### Current Work (Phase 4 - Current)
1. ✅ Identified refactoring strategy
2. ✅ Documented challenges and gaps
3. ⏳ **Next**: Expand sub-services to cover all methods

### Future Work (Phase 4B)
1. Add missing methods to sub-services
2. Fix type signature mismatches
3. Consolidate caching strategy
4. Create refactored MonolithicService facade
5. Test all existing functionality

### Future Work (Phase 4C)  
1. Gradually update components to use sub-services
2. Deprecate monolithic service gradually
3. Update documentation
4. Remove monolithic service once all components migrated

## Testing Strategy

### Before Refactoring
- Build must succeed: `npx nx build clinical-portal`
- All unit tests must pass
- E2E tests must pass: `npx nx e2e clinical-portal-e2e`

### During Refactoring
- Maintain backward compatibility
- No changes to public method signatures
- All return types must remain identical
- Caching behavior must be preserved

### After Refactoring
- Same test suite must pass
- No performance degradation
- Bundle size should decrease
- DevTools inspection should show cleaner service architecture

## Progress Tracking

### Completed
- ✅ Phase 1: Add clinical portal to docker-compose
- ✅ Phase 2: Verify gateway-trust authentication
- ✅ Phase 3: Fix hardcoded URLs
- ✅ Phase 4: Identify refactoring strategy

### In Progress
- ⏳ Phase 4A: Expand sub-services
- ⏳ Phase 4B: Create facade
- ⏳ Phase 4C: Migrate components

### Remaining Phases
- ⏳ Phase 5: Test HMAC enforcement
- ⏳ Phase 6: Documentation and handoff

## Success Criteria

### Minimum (Phase 4 Current)
- [✅] Strategy documented
- [✅] Challenges identified
- [✅] Clear next steps defined
- [✅] Architecture reviewed

### Expanded (Phase 4A-B)
- [ ] All sub-services have complete method coverage
- [ ] MonolithicService reduced to < 400 lines
- [ ] All tests pass
- [ ] No functionality broken

### Complete (Phase 4C)
- [ ] All components use sub-services directly
- [ ] Monolithic service deprecated
- [ ] Documentation updated
- [ ] Clean architecture achieved

## Key Files Reference

### Services to Expand
- `apps/clinical-portal/src/app/services/patient-health/risk-stratification.service.ts`
- `apps/clinical-portal/src/app/services/patient-health/health-scoring.service.ts`
- `apps/clinical-portal/src/app/services/patient-health/sdoh.service.ts`

### Service to Refactor
- `apps/clinical-portal/src/app/services/patient-health.service.ts`

### Reference Facade
- `apps/clinical-portal/src/app/services/patient-health/patient-health.facade.ts`

### Components to Update (Later)
- `apps/clinical-portal/src/app/pages/patient-health-overview/patient-health-overview.component.ts`
- Other components importing PatientHealthService

## Notes

- This refactoring is complex due to the large codebase (6547 lines)
- Sub-services infrastructure is solid and well-designed
- Facade pattern already proven effective
- Incremental approach minimizes risk
- Backward compatibility maintained throughout
- Clear migration path for components

## Next Steps

1. Review this document with team
2. Begin Phase 4A: Expand sub-services with missing methods
3. Create unit tests for new sub-service methods
4. Begin Phase 4B: Convert main service to facade
5. Test thoroughly before deploying to demo environment
