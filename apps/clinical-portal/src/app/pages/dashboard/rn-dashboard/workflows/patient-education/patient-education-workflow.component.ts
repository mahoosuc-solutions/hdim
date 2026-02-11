/**
 * Patient Education Workflow Component
 *
 * Manages patient education delivery through a 5-step workflow:
 * 1. Select education topic
 * 2. Choose material type (video/handout/interactive)
 * 3. Present educational material
 * 4. Assess patient understanding via quiz
 * 5. Document learning barriers and schedule follow-up
 */

import { Component, Inject, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';
import { NurseWorkflowService } from '../../../../../services/nurse-workflow/nurse-workflow.service';
import { ToastService } from '../../../../../services/toast.service';
import { LoggerService, ContextualLogger } from '../../../../../services/logger.service';
import { AuthService } from '../../../../../services/auth.service';
import { API_CONFIG } from '../../../../../config/api.config';

export interface EducationTopic {
  id: string;
  name: string;
  category: string;
  materials: EducationMaterial[];
}

export interface EducationMaterial {
  id: string;
  title: string;
  type: 'VIDEO' | 'HANDOUT' | 'INTERACTIVE';
  duration?: number;
  content?: string;
  url?: string;
}

export interface EducationBarriers {
  healthLiteracy?: boolean;
  language?: boolean;
  cognitive?: boolean;
  emotional?: boolean;
  visual?: boolean;
  hearing?: boolean;
}

export interface PatientEducationWorkflowData {
  educationSessionId: string;
  patientId: string;
  patientName: string;
}

export interface PatientEducationResult {
  success: boolean;
  result?: any;
  error?: string;
}

@Component({
  selector: 'app-patient-education-workflow',
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
    MatRadioModule,
    MatExpansionModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTooltipModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './patient-education-workflow.component.html',
  styleUrls: ['./patient-education-workflow.component.scss'],
})
export class PatientEducationWorkflowComponent implements OnInit, OnDestroy {
  @Input() educationSessionId: string;
  @Input() patientId: string;
  @Input() patientName: string;
  @Output() workflowComplete = new EventEmitter<PatientEducationResult>();

  form!: FormGroup;
  loading = false;
  currentStep = 0;
  totalSteps = 5;

  // Education data
  educationTopics: EducationTopic[] = [];
  selectedTopicName = '';
  selectedTopicMaterials: EducationMaterial[] = [];
  assessmentQuestions: any[] = [];
  materialTypes = ['VIDEO', 'HANDOUT', 'INTERACTIVE'];
  learningBarrierOptions = [
    { key: 'healthLiteracy', label: 'Health Literacy', description: 'Difficulty understanding health information' },
    { key: 'language', label: 'Language/Cultural', description: 'Language barriers or cultural differences' },
    { key: 'cognitive', label: 'Cognitive', description: 'Difficulty with learning or memory' },
    { key: 'emotional', label: 'Emotional/Psychological', description: 'Anxiety or other emotional concerns' },
    { key: 'visual', label: 'Visual', description: 'Vision impairment' },
    { key: 'hearing', label: 'Hearing', description: 'Hearing impairment' },
  ];

  // Tracking state
  materialCompleted = false;
  understandingScore = 0;
  understandingLevel = '';
  educationSummary: any = null;

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private nurseWorkflowService: NurseWorkflowService,
    private toastService: ToastService,
    private logger: LoggerService,
    private dialogRef: MatDialogRef<PatientEducationWorkflowComponent>,
    private authService: AuthService,
    @Inject(MAT_DIALOG_DATA) public data: PatientEducationWorkflowData
  ) {    this.educationSessionId = data.educationSessionId;
    this.patientId = data.patientId;
    this.patientName = data.patientName;
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadEducationTopics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize form with all fields
   */
  private initializeForm(): void {
    this.form = this.formBuilder.group(
      {
        // Step 0: Topic Selection
        selectedTopic: ['', Validators.required],

        // Step 1: Material Type
        materialType: [''],

        // Step 2: Material Presentation (tracked internally)

        // Step 3: Assessment
        assessmentAnswers: [[]],
        understandingScore: [0],

        // Step 4: Learning Barriers
        healthLiteracy: [false],
        language: [false],
        cognitive: [false],
        emotional: [false],
        visual: [false],
        hearing: [false],
        barrierNotes: [''],

        // Follow-up
        needsFollowUp: [false],
        followUpDate: [{ value: null, disabled: true }],
        followUpReason: [''],
      },
      {
        validators: [this.followUpValidator.bind(this)],
      }
    );
  }

  /**
   * Custom validator for follow-up scheduling
   */
  private followUpValidator(group: FormGroup): { [key: string]: any } | null {
    const needsFollowUp = group.get('needsFollowUp')?.value;
    const followUpDate = group.get('followUpDate')?.value;

    if (!needsFollowUp) {
      return null;
    }

    if (!followUpDate) {
      return { followUpDateRequired: true };
    }

    return null;
  }

  /**
   * Load available education topics
   */
  private loadEducationTopics(): void {
    this.loading = true;
    this.nurseWorkflowService.setTenantContext(
      this.authService.getTenantId() || API_CONFIG.DEFAULT_TENANT_ID
    );

    this.nurseWorkflowService
      .getEducationTopics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (topics: EducationTopic[]) => {
          this.educationTopics = topics || [];
          this.loading = false;
          this.logger.info(`Loaded ${this.educationTopics.length} education topics`);
        },
        error: (error: unknown) => {
          this.logger.error('Failed to load education topics:', error);
          this.toastService.error('Failed to load education topics');
          this.loading = false;
        },
      });
  }

  /**
   * Handle topic selection
   */
  onTopicSelected(): void {
    const topicId = this.form.get('selectedTopic')?.value;
    const topic = this.educationTopics.find((t) => t.id === topicId);

    if (topic) {
      this.selectedTopicName = topic.name;
      this.selectedTopicMaterials = topic.materials || [];
    }
  }

  /**
   * Check if can proceed to next step
   */
  canProceedToNextStep(): boolean {
    switch (this.currentStep) {
      case 0:
        return !!this.form.get('selectedTopic')?.value;
      case 1:
        return !!this.form.get('materialType')?.value;
      case 2:
        return this.materialCompleted;
      case 3:
        return this.understandingScore >= 70; // 70% minimum score
      case 4:
        if (this.form.get('needsFollowUp')?.value) {
          return !!this.form.get('followUpDate')?.value && !!this.form.get('followUpReason')?.value;
        }
        return true;
      default:
        return false;
    }
  }

  /**
   * Get material duration (for video materials)
   */
  getMaterialDuration(): number {
    const materialType = this.form.get('materialType')?.value;
    const material = this.selectedTopicMaterials.find((m) => m.type === materialType);
    return material?.duration || 0;
  }

  /**
   * Check if should show material content
   */
  shouldShowMaterialContent(): boolean {
    return !!this.form.get('materialType')?.value;
  }

  /**
   * Check if video material
   */
  isVideoMaterial(): boolean {
    return this.form.get('materialType')?.value === 'VIDEO';
  }

  /**
   * Check if handout material
   */
  isHandoutMaterial(): boolean {
    return this.form.get('materialType')?.value === 'HANDOUT';
  }

  /**
   * Check if interactive material
   */
  isInteractiveMaterial(): boolean {
    return this.form.get('materialType')?.value === 'INTERACTIVE';
  }

  /**
   * Mark material as completed
   */
  markMaterialComplete(): void {
    this.materialCompleted = true;
    this.toastService.success('Material marked as completed');
  }

  /**
   * Get description for material type
   */
  getTypeDescription(type: string): string {
    switch (type) {
      case 'VIDEO':
        return 'Watch an educational video with key information';
      case 'HANDOUT':
        return 'Review a printable document with detailed information';
      case 'INTERACTIVE':
        return 'Complete an interactive learning module with exercises';
      default:
        return '';
    }
  }

  /**
   * Get form control by key
   */
  getFormControl(key: string): import('@angular/forms').FormControl {
    return this.form.get(key) as import('@angular/forms').FormControl;
  }

  /**
   * Load assessment questions for understanding check
   */
  private loadAssessmentQuestions(): void {
    this.assessmentQuestions = [
      {
        id: 1,
        question: 'Which key points did you find most important?',
        type: 'OPEN_ENDED',
      },
      {
        id: 2,
        question: 'Can you describe what you learned in your own words?',
        type: 'OPEN_ENDED',
      },
      {
        id: 3,
        question: 'Do you feel confident applying this knowledge?',
        options: ['Very Confident', 'Confident', 'Somewhat Confident', 'Not Confident'],
        type: 'MULTIPLE_CHOICE',
      },
      {
        id: 4,
        question: 'What questions do you still have?',
        type: 'OPEN_ENDED',
      },
    ];
  }

  /**
   * Record assessment answers
   */
  recordAssessmentAnswers(answers: any[]): void {
    this.form.patchValue({ assessmentAnswers: answers });
  }

  /**
   * Calculate understanding score
   */
  calculateUnderstandingScore(): void {
    // Simplified scoring: 0-100 based on answers
    const answers = this.form.get('assessmentAnswers')?.value;
    const baseScore = 60; // Start at 60
    let bonus = 0;

    // Award points for detailed answers
    if (answers && answers.length > 0) {
      answers.forEach((answer: string) => {
        if (answer && answer.length > 50) {
          bonus += 10;
        }
      });
    }

    this.understandingScore = Math.min(100, baseScore + bonus);
    this.understandingLevel = this.getUnderstandingLevel();
    this.form.patchValue({ understandingScore: this.understandingScore });
  }

  /**
   * Get understanding level based on score
   */
  getUnderstandingLevel(): string {
    if (this.understandingScore >= 90) return 'EXCELLENT';
    if (this.understandingScore >= 80) return 'GOOD';
    if (this.understandingScore >= 70) return 'FAIR';
    return 'POOR';
  }

  /**
   * Flag patient for follow-up due to poor understanding
   */
  flagForFollowUp(): void {
    if (this.understandingScore < 70) {
      this.form.patchValue({
        needsFollowUp: true,
        followUpReason: `Patient needs reinforcement - understanding level: ${this.understandingLevel}`,
      });
    }
  }

  /**
   * Get barrier label by key
   */
  getBarrierLabel(key: string): string {
    const option = this.learningBarrierOptions.find((o) => o.key === key);
    return option?.label || key;
  }

  /**
   * Check if key is a valid learning barrier
   */
  isLearningBarrier(key: string): boolean {
    return this.learningBarrierOptions.some((o) => o.key === key);
  }

  /**
   * Get active learning barriers
   */
  getActiveBarriers(): { key: string; label: string }[] {
    const barriers: { key: string; label: string }[] = [];
    for (const option of this.learningBarrierOptions) {
      if (this.form.get(option.key)?.value === true) {
        barriers.push({ key: option.key, label: option.label });
      }
    }
    return barriers;
  }

  /**
   * Get barrier description
   */
  getBarrierDescription(barrier: string): string {
    const option = this.learningBarrierOptions.find((o) => o.key === barrier);
    return option?.description || '';
  }

  /**
   * Get suggested modifications for identified barriers
   */
  getSuggestedModifications(): any[] {
    const suggestions: any[] = [];
    const barriers = this.form.value;

    if (barriers.healthLiteracy) {
      suggestions.push({
        barrier: 'healthLiteracy',
        suggestion: 'Use simpler language, visual aids, and teach-back method',
      });
    }

    if (barriers.language) {
      suggestions.push({
        barrier: 'language',
        suggestion: 'Provide materials in preferred language, use interpreter',
      });
    }

    if (barriers.cognitive) {
      suggestions.push({
        barrier: 'cognitive',
        suggestion: 'Break content into smaller chunks, use repetition',
      });
    }

    if (barriers.visual) {
      suggestions.push({
        barrier: 'visual',
        suggestion: 'Provide audio resources, large print materials',
      });
    }

    if (barriers.hearing) {
      suggestions.push({
        barrier: 'hearing',
        suggestion: 'Use captions, written materials, visual demonstrations',
      });
    }

    return suggestions;
  }

  /**
   * Apply barrier recommendations (auto-schedule follow-up if barriers found)
   */
  applyBarrierRecommendations(): void {
    const barriers = this.form.value;
    const hasBarriers = Object.values(barriers).some((v) => v === true);

    if (hasBarriers && !this.form.get('needsFollowUp')?.value) {
      this.form.patchValue({
        needsFollowUp: true,
        followUpReason: 'Follow-up recommended due to identified learning barriers',
      });
    }
  }

  /**
   * Suggest follow-up interval based on understanding
   */
  suggestFollowUpInterval(): number {
    if (this.understandingScore >= 80) {
      return 7; // 1 week
    } else if (this.understandingScore >= 70) {
      return 3; // 3 days
    } else {
      return 1; // Next day
    }
  }

  /**
   * Generate education session summary
   */
  generateEducationSummary(): void {
    const topicId = this.form.get('selectedTopic')?.value;
    const topic = this.educationTopics.find((t) => t.id === topicId);

    this.educationSummary = {
      sessionId: this.educationSessionId,
      topicId: topicId,
      topicName: topic?.name || 'Unknown',
      materialType: this.form.get('materialType')?.value,
      understandingScore: this.understandingScore,
      understandingLevel: this.understandingLevel,
      learningBarriers: this.form.value,
      needsFollowUp: this.form.get('needsFollowUp')?.value,
      followUpDate: this.form.get('followUpDate')?.value,
      followUpReason: this.form.get('followUpReason')?.value,
      completedAt: new Date(),
    };
  }

  /**
   * Advance to next step
   */
  nextStep(): void {
    if (this.currentStep === 0) {
      this.onTopicSelected();
    } else if (this.currentStep === 3) {
      this.calculateUnderstandingScore();
      this.flagForFollowUp();
    } else if (this.currentStep === 4) {
      this.applyBarrierRecommendations();
      this.generateEducationSummary();
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
   * Complete education workflow and save
   */
  completeEducationWorkflow(): void {
    if (!this.canProceedToNextStep()) {
      this.toastService.error('Please complete all required steps');
      return;
    }

    this.loading = true;

    this.nurseWorkflowService
      .logPatientEducation(this.educationSummary)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: unknown) => {
          this.loading = false;
          this.toastService.success('Patient education recorded successfully');
          this.logger.info('Education workflow completed');

          const workflowResult: PatientEducationResult = {
            success: true,
            result: result,
          };

          this.workflowComplete.emit(workflowResult);
          this.dialogRef.close({ success: true, result });
        },
        error: (error: unknown) => {
          this.loading = false;
          this.logger.error('Failed to save patient education:', error);
          this.toastService.error('Failed to save education record');
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
