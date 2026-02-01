# Nurse Dashboard Phase 3 - UI Workflows Specification

**Session Date**: January 16, 2026 | **Status**: 🎯 Phase 3 Planning | **Progress**: Design & TDD Planning

## Overview

Phase 3 implements complete user workflows for the RN Dashboard using **Test-Driven Development (TDD)**. Each workflow follows Angular Material design patterns and integrates with the Phase 2 services.

## Workflow Components (5 Total)

### 1. Patient Outreach Workflow

**Component**: `patient-outreach-workflow.component.ts`
**Purpose**: Manage patient contact, document interactions, and schedule follow-ups

**User Journey**:
```
View Outreach Task
    ↓
Select Contact Method (Call/Email/Letter)
    ↓
Log Interaction (Duration, Notes, Outcome)
    ↓
Update Patient Status
    ↓
Schedule Follow-up
    ↓
Save & Close
```

**Methods to Implement** (TDD - Tests First):
- `initiateOutreach()` - Start workflow
- `logContactAttempt()` - Record call/email/letter
- `updateOutcomeType()` - Document result (successful, busy, disconnected, etc.)
- `scheduleFollowUp()` - Plan next contact
- `saveOutreachLog()` - Persist to service
- `closeWorkflow()` - Complete task

**Data Model**:
```typescript
export interface OutreachWorkflow {
  outreachLogId: string;
  patientId: string;
  contactMethod: 'CALL' | 'EMAIL' | 'LETTER';
  contactDuration?: number; // minutes
  outcomeType: OutcomeType; // enum
  notes: string;
  followUpDate?: Date;
  followUpReason?: string;
  completedAt: Date;
}
```

### 2. Medication Reconciliation Workflow

**Component**: `medication-reconciliation-workflow.component.ts`
**Purpose**: Verify patient medication list accuracy and identify discrepancies

**User Journey**:
```
Review Current Medications
    ↓
Compare with Patient Report
    ↓
Identify Discrepancies
    ↓
Mark Duplicates/Discontinued
    ↓
Add Missing Medications
    ↓
Check Drug Interactions
    ↓
Approve/Update List
    ↓
Complete Reconciliation
```

**Methods** (TDD):
- `loadCurrentMedications()` - Display active medications
- `compareWithPatientReport()` - Show differences
- `identifyDiscrepancies()` - Flag issues
- `markDuplicateTherapy()` - Tag redundant meds
- `addMissingMedication()` - New medication form
- `checkDrugInteractions()` - Validate combinations
- `completeMedicationReconciliation()` - Finalize
- `generateReconciliationReport()` - Audit trail

**Data Model**:
```typescript
export interface MedicationReconciliationWorkflow {
  reconciliationId: string;
  patientId: string;
  systemMedications: MedicationOrder[];
  patientReportedMedications: MedicationReport[];
  discrepancies: Discrepancy[];
  drugInteractions: DrugInteraction[];
  approvedList: MedicationOrder[];
  status: ReconciliationStatus;
  completedAt: Date;
  completedBy: string;
}
```

### 3. Patient Education Workflow

**Component**: `patient-education-workflow.component.ts`
**Purpose**: Provide targeted patient education and assess understanding

**User Journey**:
```
Select Education Topic
    ↓
Present Materials (Video/Handout/Interactive)
    ↓
Assess Understanding (Quiz/Demonstration)
    ↓
Document Learning Barriers
    ↓
Schedule Follow-up Session
    ↓
Generate Education Record
```

**Methods** (TDD):
- `selectEducationTopic()` - Choose topic
- `loadEducationMaterials()` - Get resources
- `presentMaterial()` - Display content
- `assessUnderstanding()` - Quiz/demo
- `recordLearningBarriers()` - Note obstacles
- `scheduleFollowUp()` - Plan next session
- `generateEducationRecord()` - Save documentation
- `trackEducationProgress()` - Metrics

**Data Model**:
```typescript
export interface PatientEducationWorkflow {
  educationSessionId: string;
  patientId: string;
  topic: EducationTopic;
  materialType: 'VIDEO' | 'HANDOUT' | 'INTERACTIVE';
  understandingLevel: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';
  assessmentScore?: number;
  learningBarriers: EducationBarriers;
  scheduledFollowUp?: Date;
  completedAt: Date;
  notes: string;
}
```

### 4. Referral Coordination Workflow

**Component**: `referral-coordination-workflow.component.ts`
**Purpose**: Manage specialist referrals and track coordination status

**User Journey**:
```
Review Pending Referral
    ↓
Select Specialist/Service
    ↓
Verify Insurance Coverage
    ↓
Send Referral
    ↓
Track Appointment Status
    ↓
Follow-up Post-Visit
    ↓
Close Referral
```

**Methods** (TDD):
- `loadPendingReferrals()` - Display awaiting action
- `selectSpecialist()` - Choose provider
- `verifyInsuranceCoverage()` - Check authorization
- `sendReferral()` - Submit request
- `trackAppointmentStatus()` - Monitor scheduling
- `documentPostVisitFollowUp()` - Record results
- `closeReferral()` - Complete coordination
- `generateReferralSummary()` - Report

**Data Model**:
```typescript
export interface ReferralCoordinationWorkflow {
  referralId: string;
  patientId: string;
  referralType: ReferralType; // Specialty
  specialist?: Practitioner;
  insuranceVerified: boolean;
  priorAuthRequired: boolean;
  priorAuthStatus?: 'APPROVED' | 'PENDING' | 'DENIED';
  appointmentScheduled?: Date;
  appointmentNotes?: string;
  postVisitNotes?: string;
  status: ReferralStatus;
  completedAt?: Date;
}
```

### 5. Care Plan Management Workflow

**Component**: `care-plan-workflow.component.ts`
**Purpose**: Create, update, and review comprehensive care plans

**User Journey**:
```
View/Create Care Plan
    ↓
Add Patient Problems
    ↓
Define Goals
    ↓
Plan Interventions
    ↓
Assign Care Team
    ↓
Schedule Reviews
    ↓
Track Progress
    ↓
Close/Transition Care
```

**Methods** (TDD):
- `initializeCarePlan()` - Start/load
- `addProblem()` - New issue
- `defineGoals()` - Set targets
- `planInterventions()` - Actions
- `assignTeamMembers()` - Responsibilities
- `scheduleReview()` - Next review date
- `trackProgress()` - Update status
- `closeCarePlan()` - Complete
- `transitionCare()` - Hand-off

**Data Model**:
```typescript
export interface CarePlanWorkflow {
  carePlanId: string;
  patientId: string;
  template: CarePlanTemplate;
  problems: CarePlanProblem[];
  goals: CarePlanGoal[];
  interventions: CarePlanIntervention[];
  teamMembers: CarePlanTeamMember[];
  reviews: CarePlanReview[];
  nextReviewDate: Date;
  status: CarePlanStatus;
  createdAt: Date;
  lastUpdatedAt: Date;
  closedAt?: Date;
}
```

## Implementation Architecture

### Folder Structure

```
apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/workflows/
├── patient-outreach/
│   ├── patient-outreach-workflow.component.ts
│   ├── patient-outreach-workflow.component.html
│   ├── patient-outreach-workflow.component.scss
│   ├── patient-outreach-workflow.component.spec.ts
│   └── index.ts
├── medication-reconciliation/
│   ├── medication-reconciliation-workflow.component.ts
│   ├── medication-reconciliation-workflow.component.html
│   ├── medication-reconciliation-workflow.component.scss
│   ├── medication-reconciliation-workflow.component.spec.ts
│   └── index.ts
├── patient-education/
│   ├── patient-education-workflow.component.ts
│   ├── patient-education-workflow.component.html
│   ├── patient-education-workflow.component.scss
│   ├── patient-education-workflow.component.spec.ts
│   └── index.ts
├── referral-coordination/
│   ├── referral-coordination-workflow.component.ts
│   ├── referral-coordination-workflow.component.html
│   ├── referral-coordination-workflow.component.scss
│   ├── referral-coordination-workflow.component.spec.ts
│   └── index.ts
├── care-plan/
│   ├── care-plan-workflow.component.ts
│   ├── care-plan-workflow.component.html
│   ├── care-plan-workflow.component.scss
│   ├── care-plan-workflow.component.spec.ts
│   └── index.ts
└── workflows.module.ts
```

### Component Pattern (All 5 Workflows)

Each workflow component follows this pattern:

```typescript
@Component({
  selector: 'app-{workflow}-workflow',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatFormFieldModule, ...],
  templateUrl: './{workflow}-workflow.component.html',
  styleUrls: ['./{workflow}-workflow.component.scss']
})
export class {Workflow}WorkflowComponent implements OnInit, OnDestroy {
  @Input() taskId: string;
  @Input() patientId: string;
  @Output() workflowComplete = new EventEmitter<WorkflowResult>();

  loading = false;
  form: FormGroup;
  currentStep = 0;
  totalSteps = 5;

  private destroy$ = new Subject<void>();
  private log: ContextualLogger;

  constructor(
    private formBuilder: FormBuilder,
    private service: SpecificService,
    private toastService: ToastService,
    private logger: LoggerService
  ) {
    this.log = logger.withContext('{Workflow}WorkflowComponent');
  }

  ngOnInit(): void {
    this.initializeWorkflow();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Workflow-specific methods
}
```

### Testing Pattern (TDD)

Each workflow component gets comprehensive unit tests:

```typescript
describe('{Workflow}WorkflowComponent', () => {
  let component: {Workflow}WorkflowComponent;
  let fixture: ComponentFixture<{Workflow}WorkflowComponent>;
  let service: jasmine.SpyObj<SpecificService>;

  beforeEach(async () => {
    // Setup
  });

  describe('initialization', () => {
    it('should load workflow data', (done) => {
      // Test
    });
  });

  describe('workflow steps', () => {
    it('should advance to next step', () => {
      // Test
    });
  });

  describe('form validation', () => {
    it('should validate required fields', () => {
      // Test
    });
  });

  describe('submission', () => {
    it('should save workflow result', (done) => {
      // Test
    });
  });
});
```

## Material Design Integration

### Components Used Per Workflow

**Common Across All**:
- MatStepperModule - Multi-step workflow
- MatFormFieldModule - Input validation
- MatButtonModule - Actions
- MatIconModule - Visual indicators
- MatDialogModule - Confirmations
- MatProgressSpinnerModule - Loading

**Specific to Workflows**:
- Medication: MatTableModule (drug interaction table)
- Education: MatExpansionModule (education materials)
- Referral: MatSelectModule (specialist selection)
- CarePlan: MatTreeModule (hierarchical goals/interventions)

## Integration with RN Dashboard

Each workflow is launched from RN Dashboard via dialog:

```typescript
// In rn-dashboard.component.ts
addressCareGap(gap: CareGapTask): void {
  switch (gap.category) {
    case 'coordination':
      this.dialogService.open(ReferralCoordinationWorkflowComponent, {
        data: { taskId: gap.id, patientId: gap.patientId }
      });
      break;
    case 'medication':
      this.dialogService.open(MedicationReconciliationWorkflowComponent, {
        data: { taskId: gap.id, patientId: gap.patientId }
      });
      break;
    case 'education':
      this.dialogService.open(PatientEducationWorkflowComponent, {
        data: { taskId: gap.id, patientId: gap.patientId }
      });
      break;
  }
}
```

## TDD Implementation Sequence

### Phase 3A: Patient Outreach Workflow (2 hours)
1. Write comprehensive test suite (15+ tests)
2. Implement component class
3. Implement component template
4. Test all interaction paths

### Phase 3B: Medication Reconciliation Workflow (2.5 hours)
1. Write test suite (18+ tests)
2. Implement component with form validation
3. Implement drug interaction table
4. Integration with medication service

### Phase 3C: Patient Education Workflow (2 hours)
1. Write test suite (15+ tests)
2. Implement material presentation logic
3. Implement assessment/quiz
4. Track learning barriers

### Phase 3D: Referral Coordination Workflow (2.5 hours)
1. Write test suite (18+ tests)
2. Implement specialist selection
3. Implement insurance verification
4. Track appointment status

### Phase 3E: Care Plan Workflow (3 hours)
1. Write test suite (20+ tests)
2. Implement hierarchical model (problems → goals → interventions)
3. Implement team member assignment
4. Implement progress tracking

### Phase 3F: Integration & Validation (2 hours)
1. Wire all workflows into RN Dashboard
2. Test dialog launching
3. Test workflow completion callbacks
4. End-to-end validation

## Success Criteria

✅ **Code**:
- 5 workflow components (1,500+ LOC each)
- 80+ unit tests (15-20 per component)
- 100% TypeScript type safety
- Material Design compliance

✅ **User Experience**:
- Multi-step guided workflows
- Form validation and error handling
- Progress indicators
- Completion confirmations

✅ **Integration**:
- Services correctly called
- State properly managed
- Errors handled gracefully
- Data saved to backend

✅ **Testing**:
- Unit test coverage ≥ 80%
- Integration with services verified
- UI interactions validated
- Form validation working

## Estimated Timeline

- Phase 3A: 2 hours (Patient Outreach)
- Phase 3B: 2.5 hours (Medication Reconciliation)
- Phase 3C: 2 hours (Patient Education)
- Phase 3D: 2.5 hours (Referral Coordination)
- Phase 3E: 3 hours (Care Plan)
- Phase 3F: 2 hours (Integration & Validation)

**Total Phase 3: ~14-15 hours**

## Next Steps

1. Create Patient Outreach workflow (Phase 3A)
2. Create Medication Reconciliation workflow (Phase 3B)
3. Create Patient Education workflow (Phase 3C)
4. Create Referral Coordination workflow (Phase 3D)
5. Create Care Plan workflow (Phase 3E)
6. Integration and E2E testing (Phase 3F)

---

_Document Status_: 🎯 Ready for TDD Implementation
_Last Updated_: January 16, 2026
_Methodology_: Test-Driven Development (TDD) Swarm
