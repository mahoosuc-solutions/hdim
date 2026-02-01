/**
 * Care Plan Workflow Component
 *
 * Manages comprehensive care plans with hierarchical problems, goals, interventions,
 * and team coordination through a 6-step workflow.
 */

import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Subject, takeUntil } from 'rxjs';
import { CarePlanService } from '../../../../../services/care-plan/care-plan.service';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService, ContextualLogger } from '../../../../../services/logger.service';

export interface Problem {
  id?: string;
  problemName: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface Goal {
  id?: string;
  relatedProblemId: string;
  goalDescription: string;
  targetDate: Date;
}

export interface Intervention {
  id?: string;
  relatedGoalId: string;
  interventionName: string;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'AS_NEEDED';
}

export interface TeamMember {
  id?: string;
  name: string;
  role: 'PRIMARY_NURSE' | 'PHYSICIAN' | 'CASE_MANAGER' | 'SOCIAL_WORKER' | 'OTHER';
}

export interface CarePlanTemplate {
  id: string;
  name: string;
  category: string;
}

export interface CarePlanWorkflowData {
  carePlanId: string;
  patientId: string;
  patientName: string;
}

export interface CarePlanSummary {
  problems: Problem[];
  goals: Goal[];
  interventions: Intervention[];
  teamMembers: TeamMember[];
  nextReviewDate: Date;
}

export interface CarePlanResult {
  success: boolean;
  result?: any;
  error?: string;
}

@Component({
  selector: 'app-care-plan-workflow',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './care-plan-workflow.component.html',
  styleUrls: ['./care-plan-workflow.component.scss'],
})
export class CarePlanWorkflowComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  loading = false;
  currentStep = 0;
  totalSteps = 6;

  // Data collections
  carePlanTemplates: CarePlanTemplate[] = [];
  problems: Problem[] = [];
  goals: Goal[] = [];
  interventions: Intervention[] = [];
  teamMembers: TeamMember[] = [];

  // Forms for each section
  problemForm!: FormGroup;
  goalForm!: FormGroup;
  interventionForm!: FormGroup;
  teamMemberForm!: FormGroup;

  // Care plan summary
  carePlanSummary: CarePlanSummary | null = null;

  // Table columns
  problemColumns = ['name', 'severity', 'action'];
  goalColumns = ['goal', 'target', 'action'];
  interventionColumns = ['intervention', 'frequency', 'action'];
  teamColumns = ['name', 'role', 'action'];

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private carePlanService: CarePlanService,
    private toastService: ToastService,
    private logger: LoggerService,
    private dialogRef: MatDialogRef<CarePlanWorkflowComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CarePlanWorkflowData
  ) {    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadCarePlanTemplates();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize all forms
   */
  private initializeForm(): void {
    this.form = this.formBuilder.group({
      selectedTemplate: ['', Validators.required],
      nextReviewDate: [''],
    });

    this.problemForm = this.formBuilder.group({
      problemName: ['', Validators.required],
      severity: ['HIGH', Validators.required],
    });

    this.goalForm = this.formBuilder.group({
      relatedProblemId: ['', Validators.required],
      goalDescription: ['', Validators.required],
      targetDate: ['', [Validators.required, this.futureDateValidator.bind(this)]],
    });

    this.interventionForm = this.formBuilder.group({
      relatedGoalId: ['', Validators.required],
      interventionName: ['', Validators.required],
      frequency: ['WEEKLY', Validators.required],
    });

    this.teamMemberForm = this.formBuilder.group({
      teamMemberId: ['', Validators.required],
      teamMemberName: ['', Validators.required],
      role: ['PRIMARY_NURSE', Validators.required],
    });
  }

  /**
   * Validator for future dates
   */
  private futureDateValidator(control: any): { [key: string]: any } | null {
    if (!control.value) return null;

    const selectedDate = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (selectedDate <= today) {
      return { pastDate: true };
    }

    return null;
  }

  /**
   * Load care plan templates
   */
  private loadCarePlanTemplates(): void {
    this.loading = true;

    this.carePlanService.setTenantContext('TENANT001'); // TODO: Get from auth

    this.carePlanService
      .getCarePlanTemplates()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (templates: CarePlanTemplate[]) => {
          this.carePlanTemplates = templates || [];
          this.loading = false;
          this.logger.info(`Loaded ${templates?.length || 0} care plan templates`);
        },
        error: (error: unknown) => {
          this.logger.error('Failed to load care plan templates:', error);
          this.toastService.error('Failed to load care plan templates');
          this.loading = false;
        },
      });
  }

  /**
   * Get selected template details
   */
  getSelectedTemplate(): any | null {
    const selectedId = this.form.get('selectedTemplate')?.value;
    if (!selectedId) {
      return null;
    }
    return this.carePlanTemplates.find((t) => t.id === selectedId) || null;
  }

  /**
   * Check if can proceed to next step
   */
  canProceedToNextStep(): boolean {
    switch (this.currentStep) {
      case 0:
        return !!this.form.get('selectedTemplate')?.value;
      case 1:
        return this.problems.length > 0;
      case 2:
        return this.goals.length > 0;
      case 3:
        return this.interventions.length > 0;
      case 4:
        return this.teamMembers.length > 0;
      case 5:
        return !!this.form.get('nextReviewDate')?.value;
      default:
        return false;
    }
  }

  /**
   * Initialize care plan with template
   */
  initializeCarePlan(): void {
    this.loading = true;
    const templateId = this.form.get('selectedTemplate')?.value;

    const createData = {
      patientId: this.data.patientId,
      templateId: templateId,
    };

    this.carePlanService
      .createCarePlan(createData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.loading = false;
          this.toastService.success('Care plan initialized');
          this.logger.info('Care plan initialized successfully');
        },
        error: (error: unknown) => {
          this.loading = false;
          this.logger.error('Failed to initialize care plan:', error);
          this.toastService.error('Failed to initialize care plan');
        },
      });
  }

  /**
   * Add problem to list
   */
  addProblem(): void {
    if (!this.problemForm.valid) {
      this.toastService.error('Please fill in all required fields');
      return;
    }

    const problem: Problem = {
      id: `PROBLEM_${Date.now()}`,
      ...this.problemForm.value,
    };

    this.problems.push(problem);
    this.problemForm.reset({ severity: 'HIGH' });
    this.toastService.success('Problem added');
  }

  /**
   * Remove problem from list
   */
  removeProblem(index: number): void {
    this.problems.splice(index, 1);
    this.toastService.info('Problem removed');
  }

  /**
   * Add goal to list
   */
  addGoal(): void {
    if (!this.goalForm.valid) {
      this.toastService.error('Please fill in all required fields');
      return;
    }

    const goal: Goal = {
      id: `GOAL_${Date.now()}`,
      ...this.goalForm.value,
    };

    this.goals.push(goal);
    this.goalForm.reset();
    this.toastService.success('Goal added');
  }

  /**
   * Remove goal from list
   */
  removeGoal(index: number): void {
    this.goals.splice(index, 1);
    this.toastService.info('Goal removed');
  }

  /**
   * Add intervention to list
   */
  addIntervention(): void {
    if (!this.interventionForm.valid) {
      this.toastService.error('Please fill in all required fields');
      return;
    }

    const intervention: Intervention = {
      id: `INTERVENTION_${Date.now()}`,
      ...this.interventionForm.value,
    };

    this.interventions.push(intervention);
    this.interventionForm.reset({ frequency: 'WEEKLY' });
    this.toastService.success('Intervention added');
  }

  /**
   * Remove intervention from list
   */
  removeIntervention(index: number): void {
    this.interventions.splice(index, 1);
    this.toastService.info('Intervention removed');
  }

  /**
   * Check if can add team member
   */
  canAddTeamMember(): boolean {
    const role = this.teamMemberForm.get('role')?.value;
    if (role === 'PRIMARY_NURSE') {
      const hasPrimaryNurse = this.teamMembers.some((tm) => tm.role === 'PRIMARY_NURSE');
      return !hasPrimaryNurse;
    }
    return true;
  }

  /**
   * Add team member to list
   */
  addTeamMember(): void {
    if (!this.teamMemberForm.valid || !this.canAddTeamMember()) {
      this.toastService.error('Cannot add team member - check requirements');
      return;
    }

    const teamMember: TeamMember = {
      id: `TEAM_${Date.now()}`,
      name: this.teamMemberForm.get('teamMemberName')?.value,
      role: this.teamMemberForm.get('role')?.value,
    };

    this.teamMembers.push(teamMember);
    this.teamMemberForm.reset({ role: 'PRIMARY_NURSE' });
    this.toastService.success('Team member added');
  }

  /**
   * Remove team member from list
   */
  removeTeamMember(index: number): void {
    this.teamMembers.splice(index, 1);
    this.toastService.info('Team member removed');
  }

  /**
   * Generate care plan summary
   */
  generateCarePlanSummary(): void {
    this.carePlanSummary = {
      problems: [...this.problems],
      goals: [...this.goals],
      interventions: [...this.interventions],
      teamMembers: [...this.teamMembers],
      nextReviewDate: this.form.get('nextReviewDate')?.value,
    };
  }

  /**
   * Advance to next step
   */
  nextStep(): void {
    if (this.currentStep === 0) {
      this.initializeCarePlan();
    } else if (this.currentStep === 5) {
      this.generateCarePlanSummary();
    }

    if (this.currentStep < this.totalSteps - 1 && this.canProceedToNextStep()) {
      this.currentStep++;
    }
  }

  /**
   * Go to previous step
   */
  previousStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /**
   * Complete care plan workflow
   */
  completeCarePlanWorkflow(): void {
    if (!this.canProceedToNextStep()) {
      this.toastService.error('Please complete all required steps');
      return;
    }

    this.loading = true;

    const completionData = {
      carePlanId: this.data.carePlanId,
      problems: this.problems,
      goals: this.goals,
      interventions: this.interventions,
      teamMembers: this.teamMembers,
      nextReviewDate: this.form.get('nextReviewDate')?.value,
      completedAt: new Date(),
    };

    this.carePlanService
      .completeCarePlan(this.data.carePlanId, completionData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.loading = false;
          this.toastService.success('Care plan completed successfully');
          this.logger.info('Care plan workflow completed');

          const workflowResult: CarePlanResult = {
            success: true,
            result: result,
          };

          this.dialogRef.close({ success: true, result });
        },
        error: (error: unknown) => {
          this.loading = false;
          this.logger.error('Failed to complete care plan:', error);
          this.toastService.error('Failed to save care plan');
        },
      });
  }

  /**
   * Cancel workflow
   */
  cancelWorkflow(): void {
    this.dialogRef.close({ success: false });
  }
}
