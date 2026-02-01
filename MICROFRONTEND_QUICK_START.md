# Microfrontend Architecture Quick Start

**Current Status**: Phase 3 Complete ✅ | Phase 4 Ready 🚀

---

## Quick Commands

### Start Local Development

```bash
# 1. Start Docker infrastructure
docker compose -f docker-compose.demo.yml up -d

# 2. Start shell app (host)
npx nx serve shell-app

# 3. In another terminal, start patient MFE (remote)
npx nx serve mfePatients

# 4. Open browser
http://localhost:4200
```

### Build Everything

```bash
# Build all projects
npx nx build shell-app

# Build specific project
npx nx build mfePatients

# Build specific libraries
npx nx build data-access
npx nx build util-auth
npx nx build state
```

### Run Tests

```bash
# Test all
npx nx run-many --target=test

# Test specific project
npx nx test shell-app

# Test specific library
npx nx test data-access
```

---

## Architecture Overview

### What We Have (✅ Completed)

1. **Shell App** (`apps/shell-app`)
   - Module Federation host
   - Authentication UI
   - Central routing
   - Runs on port 4200

2. **Patient MFE** (`apps/mfe-patients`)
   - Module Federation remote
   - Patient list/chart components
   - Routed at `/mfePatients`

3. **Shared Libraries**
   - `@health-platform/shared/data-access` - API services, interceptors, **360 pipeline**
   - `@health-platform/shared/util-auth` - Auth guards, user models
   - `@health-platform/shared/ui-common` - Reusable components
   - `@health-platform/shared/feature-shell` - Shell layout
   - `@health-platform/shared/state` - NgRx auth store

4. **Clinical 360 Pipeline** (NEW)
   - Orchestrates multi-source data loading
   - Patient → Clinical observations → Quality measures → Care gaps → Workflows
   - Coordinates across all MFEs
   - See: `libs/shared/data-access/src/lib/services/clinical-360-pipeline.service.ts`

5. **Inter-MFE Event Bus** (NEW)
   - EventBusService for MFE communication
   - Patient selection, workflow events, measure evaluation
   - Clinically-typed events for 360 pipeline
   - See: `libs/shared/data-access/src/lib/event-bus/event-bus.service.ts`

### What's Next (📋 Phase 4)

**Create 3 New MFEs**:
1. `mfe-quality` - Quality measures, evaluations
2. `mfe-care-gaps` - Care gap management, interventions
3. `mfe-reports` - Analytics, dashboards

**Integrate 360 Pipeline**:
- Patient selection in mfe-patients
- Triggers Clinical360PipelineService
- Quality measures auto-evaluate
- Care gaps display
- All MFEs synchronized via EventBus

---

## Key Files to Understand

### State Management
```
libs/shared/state/src/
├── lib/auth/
│   ├── auth.actions.ts       # Action types
│   ├── auth.reducer.ts       # State shape
│   ├── auth.effects.ts       # Side effects
│   └── auth.selectors.ts     # Selectors
└── state.providers.ts        # NgRx bootstrap
```

### Clinical 360 Pipeline
```
libs/shared/data-access/src/lib/
├── services/
│   └── clinical-360-pipeline.service.ts
│       ├── loadClinical360()     # Load all data
│       ├── refreshSection()      # Partial update
│       ├── getCareReadinessScore()
│       └── Clinical360Data type
└── event-bus/
    └── event-bus.service.ts
        ├── emit()                # Send events
        ├── on()                  # Subscribe to events
        └── ClinicalEventType enum
```

### Shell App Setup
```
apps/shell-app/src/
├── app/
│   ├── app.routes.ts         # Federated routes
│   ├── app.config.ts         # Providers (NgRx, interceptors)
│   ├── pages/
│   │   └── home.page.ts      # Home with auth context
│   └── layout/               # Shell layout component
└── main.ts
```

---

## Common Workflows

### Workflow 1: Patient Selection Flow

1. **User selects patient in mfe-patients**
   ```typescript
   selectPatient(patientId: string) {
     this.eventBus.emit({
       type: ClinicalEventType.PATIENT_SELECTED,
       source: 'mfe-patients',
       data: { patientId, tenantId }
     });
   }
   ```

2. **All MFEs receive event via EventBus**
   ```typescript
   constructor(private eventBus: EventBusService) {
     this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
       .subscribe(event => {
         console.log('Patient selected:', event.data.patientId);
       });
   }
   ```

3. **mfe-quality loads measures for patient**
   ```typescript
   this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
     .subscribe(event => {
       this.qualityService.evaluatePatient(event.data.patientId);
     });
   ```

4. **mfe-care-gaps loads interventions**
   ```typescript
   this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
     .subscribe(event => {
       this.careGapService.loadGaps(event.data.patientId);
     });
   ```

### Workflow 2: 360 Data Pipeline Loading

```typescript
// In any component
constructor(private pipeline: Clinical360PipelineService) {}

loadPatientData(patientId: string) {
  // Load complete clinical picture
  this.pipeline.loadClinical360(patientId, this.tenantId)
    .subscribe(data => {
      console.log('Complete patient data:');
      console.log('- Measures met:', data.qualityMeasures.measuresMet);
      console.log('- Care gaps:', data.careGaps.totalGaps);
      console.log('- Active workflows:', data.activeWorkflows.totalActive);

      // Compute readiness
      this.pipeline.getCareReadinessScore()
        .subscribe(score => console.log('Care readiness:', score));
    });
}
```

### Workflow 3: Partial Data Refresh

```typescript
// Measures just evaluated - refresh that section
this.pipeline.refreshSection(patientId, tenantId, 'qualityMeasures')
  .subscribe(() => {
    console.log('Quality measures updated');
  });

// Event automatically emitted: DATA_PIPELINE_STEP_COMPLETE
```

---

## Authentication Flow

1. **User logs in** (gateway-service validates JWT)
2. **Gateway injects headers**: X-Auth-User-Id, X-Auth-Roles, X-Auth-Tenant-Ids
3. **Shell app loads** and intercepts with tenantInterceptor, authInterceptor
4. **AuthService loads user** from /auth/me
5. **NgRx auth effects** sync user to store
6. **All MFEs access via selectors**:
   ```typescript
   this.store.select(selectSharedAuthUser)
   this.store.select(selectSharedAuthTenantId)
   this.store.select(selectIsAuthenticated)
   ```

---

## Adding a New MFE

### Step 1: Generate Application
```bash
nx generate @nx/angular:application mfe-care-gaps \
  --routing \
  --projectNameAndRootFormat=as-provided \
  --style=scss
```

### Step 2: Add Module Federation
```bash
# In mfe-care-gaps
# Update webpack.config.js to expose routes
```

### Step 3: Add to Shell Routes
```typescript
// apps/shell-app/src/app/app.routes.ts
{
  path: 'mfeCareGaps',
  loadChildren: () =>
    import('mfeCareGaps/Routes').then(m => m!.remoteRoutes)
}
```

### Step 4: Implement with EventBus
```typescript
// In mfe-care-gaps
this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
  .subscribe(event => {
    this.loadCareGaps(event.data.patientId);
  });
```

---

## Testing 360 Pipeline

```typescript
// In component test
it('should load 360 data when patient selected', fakeAsync(() => {
  const mockData: Clinical360Data = { ... };
  service.loadClinical360.and.returnValue(of(mockData));

  component.selectPatient('PATIENT123');

  expect(service.loadClinical360).toHaveBeenCalledWith('PATIENT123', 'tenant1');
  expect(component.careReadiness).toBe(75); // Example score
}));
```

---

## Troubleshooting

### "Module not found: mfePatients"
- Ensure mfePatients is running: `npx nx serve mfePatients`
- Check webpack.config.js exposes routes

### "Property X does not exist on type User"
- User model is in `@health-platform/shared/util-auth`
- Import: `import type { User } from '@health-platform/shared/util-auth'`

### "Cannot find selector selectSharedAuthUser"
- Ensure `provideSharedState()` called in app.config.ts
- Import from `@health-platform/shared/state`

### 360 Pipeline returns null
- Check tenantId is correct: `this.eventBus.currentPatient$.value.tenantId`
- Verify gateway endpoint: `GET /api/v1/clinical-360/:patientId`

---

## Performance Tips

1. **Cache 360 data** (automatic 5-minute TTL)
   - Only reload when patient changes
   - Use `refreshSection()` for updates

2. **Lazy load MFEs**
   - Shell only loads mfe-patients on `/mfePatients` route
   - Other MFEs load on-demand

3. **Unsubscribe from EventBus**
   ```typescript
   private destroy$ = new Subject<void>();

   ngOnInit() {
     this.eventBus.on(ClinicalEventType.PATIENT_SELECTED)
       .pipe(takeUntil(this.destroy$))
       .subscribe(...);
   }

   ngOnDestroy() {
     this.destroy$.next();
     this.destroy$.complete();
   }
   ```

---

## Next Steps for Phase 4

- [ ] Create mfe-quality MFE
- [ ] Create mfe-care-gaps MFE
- [ ] Create mfe-reports MFE
- [ ] Implement 360 pipeline backend endpoint
- [ ] Add clinical workflow routing
- [ ] Test end-to-end patient workflows
- [ ] Update CI/CD for MFE builds
- [ ] Add E2E tests for MFE integration

---

**Last Updated**: January 17, 2026
**Maintainer**: AI Assistant
**See Also**: `MICROFRONTEND_RECOVERY_SUMMARY.md` for detailed status
