import { Component, OnInit, OnDestroy, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { EvaluationDataFlowService } from '../../services/evaluation-data-flow.service';

/**
 * Data Flow Step Interface
 */
export interface DataFlowStep {
  stepNumber: number;
  stepName: string;
  stepType: 'DATA_FETCH' | 'EXPRESSION_EVAL' | 'LOGIC_DECISION' | 'CQL_EXECUTION' | 'DATA_TRANSFORM' | 'CACHE_LOOKUP' | 'KAFKA_PUBLISH';
  timestamp: string;
  resourcesAccessed?: string[];
  inputData?: string;
  outputData?: string;
  decision?: string;
  reasoning?: string;
  durationMs?: number;
}

/**
 * Evaluation Data Flow Visualization Component
 * 
 * Displays real-time visualization of data processing during FHIR evaluation:
 * - FHIR data retrieval steps
 * - Kafka message publishing
 * - CQL evaluation steps
 */
@Component({
  selector: 'app-evaluation-data-flow',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MatButtonModule,
    MatExpansionModule,
    MatTooltipModule,
  ],
  templateUrl: './evaluation-data-flow.component.html',
  styleUrl: './evaluation-data-flow.component.scss',
})
export class EvaluationDataFlowComponent implements OnInit, OnDestroy, OnChanges {
  @Input() evaluationId?: string;
  @Input() patientId?: string;
  @Input() measureId?: string;

  private destroy$ = new Subject<void>();

  // Data flow steps
  steps: DataFlowStep[] = [];
  
  // Grouped steps by type
  fhirSteps: DataFlowStep[] = [];
  kafkaSteps: DataFlowStep[] = [];
  cqlSteps: DataFlowStep[] = [];

  // Status
  isProcessing = false;
  currentStep?: DataFlowStep;
  progress = 0;

  // Statistics
  totalDuration = 0;
  fhirResourceCount = 0;
  kafkaMessagesCount = 0;
  cqlExpressionsEvaluated = 0;

  constructor(private dataFlowService: EvaluationDataFlowService) {}

  ngOnInit(): void {
    // Component initialized - ready to receive data flow steps
    if (this.evaluationId) {
      this.connectWebSocket();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['evaluationId'] && this.evaluationId && !changes['evaluationId'].firstChange) {
      this.connectWebSocket();
    }
  }

  /**
   * Connect to WebSocket for real-time data flow steps
   */
  private connectWebSocket(): void {
    if (!this.evaluationId) {
      return;
    }

    this.reset();
    this.startProcessing();

    this.dataFlowService.connect(this.evaluationId).pipe(
      takeUntil(this.destroy$)
    ).subscribe((step) => {
      this.addStep(step);
    });
  }

  ngOnDestroy(): void {
    this.dataFlowService.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Add a new data flow step
   */
  addStep(step: DataFlowStep): void {
    this.steps.push(step);
    this.updateGroupedSteps();
    this.updateStatistics();
    this.currentStep = step;
    this.updateProgress();
  }

  /**
   * Update grouped steps
   */
  private updateGroupedSteps(): void {
    this.fhirSteps = this.steps.filter(s => 
      s.stepType === 'DATA_FETCH' && 
      s.resourcesAccessed && 
      s.resourcesAccessed.length > 0
    );
    
    this.kafkaSteps = this.steps.filter(s => 
      s.stepType === 'KAFKA_PUBLISH' || 
      s.stepName.toLowerCase().includes('kafka') ||
      s.stepName.toLowerCase().includes('publish')
    );
    
    this.cqlSteps = this.steps.filter(s => 
      s.stepType === 'CQL_EXECUTION' || 
      s.stepType === 'EXPRESSION_EVAL' ||
      s.stepType === 'LOGIC_DECISION'
    );
  }

  /**
   * Update statistics
   */
  private updateStatistics(): void {
    // Total duration
    this.totalDuration = this.steps
      .filter(s => s.durationMs)
      .reduce((sum, s) => sum + (s.durationMs || 0), 0);

    // FHIR resource count
    this.fhirResourceCount = this.fhirSteps
      .flatMap(s => s.resourcesAccessed || [])
      .filter((v, i, arr) => arr.indexOf(v) === i).length;

    // Kafka messages count
    this.kafkaMessagesCount = this.kafkaSteps.length;

    // CQL expressions evaluated
    this.cqlExpressionsEvaluated = this.cqlSteps.length;
  }

  /**
   * Update progress
   */
  private updateProgress(): void {
    if (this.steps.length === 0) {
      this.progress = 0;
      return;
    }

    // Estimate progress based on step types
    const totalExpectedSteps = 10; // Estimated total steps
    this.progress = Math.min(100, (this.steps.length / totalExpectedSteps) * 100);
  }

  /**
   * Get step type icon
   */
  getStepTypeIcon(stepType: string): string {
    switch (stepType) {
      case 'DATA_FETCH':
        return 'cloud_download';
      case 'EXPRESSION_EVAL':
      case 'CQL_EXECUTION':
        return 'code';
      case 'LOGIC_DECISION':
        return 'psychology';
      case 'DATA_TRANSFORM':
        return 'transform';
      case 'CACHE_LOOKUP':
        return 'cached';
      case 'KAFKA_PUBLISH':
        return 'send';
      default:
        return 'play_circle';
    }
  }

  /**
   * Get step type color
   */
  getStepTypeColor(stepType: string): string {
    switch (stepType) {
      case 'DATA_FETCH':
        return 'primary';
      case 'EXPRESSION_EVAL':
      case 'CQL_EXECUTION':
        return 'accent';
      case 'LOGIC_DECISION':
        return 'warn';
      case 'DATA_TRANSFORM':
        return '';
      case 'CACHE_LOOKUP':
        return 'primary';
      case 'KAFKA_PUBLISH':
        return 'accent';
      default:
        return '';
    }
  }

  /**
   * Format duration
   */
  formatDuration(ms?: number): string {
    if (!ms) return 'N/A';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }

  /**
   * Check if step is FHIR-related
   */
  isFhirStep(step: DataFlowStep): boolean {
    return step.stepType === 'DATA_FETCH' && 
           !!step.resourcesAccessed && 
           step.resourcesAccessed.length > 0;
  }

  /**
   * Check if step is Kafka-related
   */
  isKafkaStep(step: DataFlowStep): boolean {
    return step.stepType === 'KAFKA_PUBLISH' || 
           step.stepName.toLowerCase().includes('kafka') ||
           step.stepName.toLowerCase().includes('publish');
  }

  /**
   * Check if step is CQL-related
   */
  isCqlStep(step: DataFlowStep): boolean {
    return step.stepType === 'CQL_EXECUTION' || 
           step.stepType === 'EXPRESSION_EVAL' ||
           step.stepType === 'LOGIC_DECISION';
  }

  /**
   * Reset component state
   */
  reset(): void {
    this.steps = [];
    this.fhirSteps = [];
    this.kafkaSteps = [];
    this.cqlSteps = [];
    this.currentStep = undefined;
    this.progress = 0;
    this.totalDuration = 0;
    this.fhirResourceCount = 0;
    this.kafkaMessagesCount = 0;
    this.cqlExpressionsEvaluated = 0;
    this.isProcessing = false;
  }

  /**
   * Start processing
   */
  startProcessing(): void {
    this.isProcessing = true;
    this.reset();
  }

  /**
   * Stop processing
   */
  stopProcessing(): void {
    this.isProcessing = false;
  }
}
