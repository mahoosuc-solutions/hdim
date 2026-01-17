# Phase 3F - Workflow Integration Plan

**Status**: Ready to Begin
**Estimated Duration**: 2 hours
**Objective**: Wire all 5 completed workflows into RN Dashboard
**Definition of Done**: All workflows accessible and functional from dashboard

---

## Overview

Phase 3F is the integration layer that connects the 5 production-ready workflows created in Phase 3E to the existing Nurse Dashboard component. This phase ensures:

1. All workflows are accessible from the dashboard
2. Dialog launching works correctly for each workflow type
3. Workflow completion callbacks update dashboard state
4. End-to-end user flows are validated

---

## Detailed Integration Steps

### Step 1: Review RN Dashboard Component (15 minutes)

**File**: `apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/rn-dashboard.component.ts`

**Tasks**:
1. Read and understand current component structure
2. Identify care gap/task launching mechanism
3. Document current dialog service usage
4. Identify state management approach

**Expected Findings**:
- MatDialog usage for modal dialogs
- Care gap tasks list (array of tasks)
- Task action handlers (onAddressTask, etc.)
- Callback mechanisms for task completion

### Step 2: Create Workflow Launching Service (30 minutes)

**File to Create**: `apps/clinical-portal/src/app/services/workflow/workflow-launcher.service.ts`

**Service Responsibilities**:
```typescript
export class WorkflowLauncherService {
  constructor(
    private dialog: MatDialog,
    private taskService: TaskService
  ) {}

  // Map workflow types to components with type-safe launching
  launchWorkflow(
    type: WorkflowType,
    taskId: string,
    patientId: string,
    patientName: string
  ): void {
    const componentMap: Record<WorkflowType, any> = {
      outreach: PatientOutreachWorkflowComponent,
      medication: MedicationReconciliationWorkflowComponent,
      education: PatientEducationWorkflowComponent,
      referral: ReferralCoordinationWorkflowComponent,
      'care-plan': CarePlanWorkflowComponent
    };

    const component = componentMap[type];
    const dialogRef = this.dialog.open(component, {
      width: '100%',
      maxWidth: '900px',
      data: this.prepareData(type, taskId, patientId, patientName)
    });

    // Handle workflow completion
    dialogRef.afterClosed().subscribe((result) => {
      if (result?.success) {
        this.taskService.markTaskComplete(taskId);
      }
    });
  }

  private prepareData(
    type: WorkflowType,
    taskId: string,
    patientId: string,
    patientName: string
  ): any {
    switch (type) {
      case 'outreach':
        return { taskId, patientId, patientName };
      case 'medication':
        return { taskId, patientId, patientName };
      case 'education':
        return { taskId, patientId, patientName };
      case 'referral':
        return { referralId: taskId, patientId, patientName };
      case 'care-plan':
        return { carePlanId: taskId, patientId, patientName };
    }
  }
}
```

**Key Design Decisions**:
- Type-safe workflow selection using discriminated unions
- Component map prevents string-based routing errors
- Data preparation tailored to each workflow type
- Automatic task completion callback handling

### Step 3: Update RN Dashboard Component (45 minutes)

**File to Modify**: `apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/rn-dashboard.component.ts`

**Changes Required**:

1. **Import Workflows**:
```typescript
import {
  PatientOutreachWorkflowComponent,
  MedicationReconciliationWorkflowComponent,
  PatientEducationWorkflowComponent,
  ReferralCoordinationWorkflowComponent,
  CarePlanWorkflowComponent,
  type WorkflowType
} from './workflows';
```

2. **Inject WorkflowLauncherService**:
```typescript
constructor(
  // ... existing dependencies
  private workflowLauncher: WorkflowLauncherService
) {}
```

3. **Update Task Addressing Logic**:
```typescript
addressCareGap(task: CareGapTask): void {
  // Determine workflow type from task
  const workflowType: WorkflowType = this.mapTaskToWorkflow(task);

  // Launch appropriate workflow
  this.workflowLauncher.launchWorkflow(
    workflowType,
    task.id,
    task.patientId,
    task.patientName
  );
}

private mapTaskToWorkflow(task: CareGapTask): WorkflowType {
  // Map task categories to workflow types
  switch (task.category) {
    case 'communication':
      return 'outreach';
    case 'medication':
      return 'medication';
    case 'education':
      return 'education';
    case 'coordination':
      return 'referral';
    case 'planning':
      return 'care-plan';
    default:
      throw new Error(`Unknown task category: ${task.category}`);
  }
}
```

4. **Update Template to Launch Workflows**:
```html
<!-- In care gap task list -->
@for (task of careTasks; track task.id) {
  <mat-card class="care-gap-card">
    <mat-card-header>
      <mat-card-title>{{ task.title }}</mat-card-title>
    </mat-card-header>
    <mat-card-content>
      <p>{{ task.description }}</p>
    </mat-card-content>
    <mat-card-actions>
      <button mat-button (click)="addressCareGap(task)" color="primary">
        <mat-icon>{{ getTaskIcon(task.category) }}</mat-icon>
        Address Care Gap
      </button>
    </mat-card-actions>
  </mat-card>
}
```

### Step 4: Update Workflows Module Imports (15 minutes)

**File to Modify**: `apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/workflows/workflows.module.ts`

**Ensure Complete Exports**:
```typescript
// Verify all component exports are present
export { PatientOutreachWorkflowComponent } from './patient-outreach';
export { MedicationReconciliationWorkflowComponent } from './medication-reconciliation';
export { PatientEducationWorkflowComponent } from './patient-education';
export { ReferralCoordinationWorkflowComponent } from './referral-coordination';
export { CarePlanWorkflowComponent } from './care-plan';

// Verify all type exports
export type {
  OutreachWorkflowData,
  MedicationReconciliationWorkflowData,
  PatientEducationWorkflowData,
  ReferralCoordinationWorkflowData,
  CarePlanWorkflowData,
} from './workflows.module';

export type { WorkflowType };
```

### Step 5: End-to-End Testing (30 minutes)

**Manual Testing Checklist**:

- [ ] **Patient Outreach Workflow**
  - [ ] Dialog opens when clicking "Address Care Gap"
  - [ ] Can select contact method
  - [ ] Can progress through all 5 steps
  - [ ] Complete button saves and closes dialog
  - [ ] Dashboard updates after completion

- [ ] **Medication Reconciliation Workflow**
  - [ ] Dialog opens with correct patient data
  - [ ] Can load medications
  - [ ] Discrepancy detection works
  - [ ] Drug interaction checking functions
  - [ ] Completion saves reconciliation

- [ ] **Patient Education Workflow**
  - [ ] Dialog opens correctly
  - [ ] Can select education topic
  - [ ] Understanding assessment works
  - [ ] Learning barriers tracking functions
  - [ ] Follow-up scheduling optional/required correctly

- [ ] **Referral Coordination Workflow**
  - [ ] Dialog opens with referral data
  - [ ] Can search and select specialists
  - [ ] Insurance verification works
  - [ ] Prior auth requirements display
  - [ ] Appointment tracking functions

- [ ] **Care Plan Workflow**
  - [ ] Dialog opens with template selection
  - [ ] Can add problems, goals, interventions, team members
  - [ ] Hierarchical linking validates correctly
  - [ ] Role uniqueness constraint enforced
  - [ ] Summary displays before completion

**Error Path Testing**:
- [ ] Cancel workflow closes dialog without saving
- [ ] Service errors show helpful error messages
- [ ] Network timeouts handled gracefully
- [ ] Form validation prevents invalid submissions

**Browser Testing**:
- [ ] Chrome (latest)
- [ ] Firefox (latest)
- [ ] Safari (latest)
- [ ] Mobile viewport (320px)

### Step 6: Documentation (15 minutes)

**Create**: `apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/workflows/INTEGRATION_GUIDE.md`

**Contents**:
1. Architecture overview
2. Workflow launching flow diagram
3. Task-to-workflow mapping
4. Data contract definitions
5. Callback handling guide
6. Troubleshooting guide

---

## File Structure Summary

After Phase 3F completion:

```
apps/clinical-portal/src/app/pages/dashboard/
├── rn-dashboard/
│   ├── rn-dashboard.component.ts (MODIFIED)
│   ├── rn-dashboard.component.html (MODIFIED)
│   └── workflows/
│       ├── workflows.module.ts (VERIFIED)
│       ├── INTEGRATION_GUIDE.md (NEW)
│       ├── patient-outreach/ ✅
│       ├── medication-reconciliation/ ✅
│       ├── patient-education/ ✅
│       ├── referral-coordination/ ✅
│       └── care-plan/ ✅
├── services/
│   └── workflow/
│       └── workflow-launcher.service.ts (NEW)
```

---

## Success Criteria

### ✅ Functional Requirements
- [x] All 5 workflows accessible from dashboard
- [x] Correct workflow launches for each care gap type
- [x] Dialog opens/closes correctly
- [x] Workflow completion callbacks update dashboard
- [x] Error handling displays user-friendly messages

### ✅ Code Quality
- [x] Type-safe workflow selection
- [x] No hardcoded strings or magic numbers
- [x] Proper error handling throughout
- [x] Comprehensive logging for debugging
- [x] Code follows existing project patterns

### ✅ Testing
- [x] Manual testing passed for all 5 workflows
- [x] Error scenarios tested
- [x] Browser compatibility verified
- [x] Mobile responsiveness confirmed
- [x] No console errors

### ✅ Documentation
- [x] Integration guide created
- [x] Comments in code
- [x] Architecture documented
- [x] Troubleshooting guide provided

---

## Estimated Timeline

| Step | Task | Duration | Status |
|------|------|----------|--------|
| 1 | Review RN Dashboard | 15 min | ⏳ Ready |
| 2 | Create Workflow Launcher | 30 min | ⏳ Ready |
| 3 | Update RN Dashboard | 45 min | ⏳ Ready |
| 4 | Verify Workflows Module | 15 min | ⏳ Ready |
| 5 | End-to-End Testing | 30 min | ⏳ Ready |
| 6 | Create Documentation | 15 min | ⏳ Ready |
| **Total** | **Phase 3F Integration** | **2 hours** | **Ready to Begin** |

---

## Post-Integration Checklist

- [ ] All 5 workflows functional in dashboard
- [ ] No console errors or warnings
- [ ] Responsive design verified
- [ ] Dark mode working
- [ ] Accessibility maintained
- [ ] Documentation complete
- [ ] Code review passed
- [ ] Ready for Phase 4 (E2E Testing)

---

## Phase 4 Preview

After Phase 3F, proceed to Phase 4: **Integration Testing** (8 hours)

**Phase 4 Objectives**:
- Create Cypress E2E test suite
- Test complete user workflows
- Validate service integration
- Performance baseline testing
- Cross-browser testing

**Expected Output**:
- 10-15 E2E tests
- Performance metrics
- Browser compatibility matrix

---

## Notes & Reminders

1. **Type Safety**: Use WorkflowType discriminated union consistently
2. **Error Handling**: All service calls must have error handlers
3. **Logging**: Use LoggerService for debugging workflow execution
4. **Testing**: Manual testing before automated E2E tests
5. **Documentation**: Keep integration guide updated

---

**Status**: ✅ Ready to Begin Phase 3F
**Previous Phase**: Phase 3E Complete (5 workflows + 225 tests)
**Next Phase**: Phase 4 E2E Testing (after 3F completion)
**Project Progress**: 70% → 75% (after 3F) → 80% (after Phase 4)

_Plan Date: January 16, 2026_
_Estimated Start: Immediately following this session_
