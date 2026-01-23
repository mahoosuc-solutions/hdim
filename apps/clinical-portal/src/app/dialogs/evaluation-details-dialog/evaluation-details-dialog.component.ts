import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatCardModule } from '@angular/material/card';
import { EvaluationService } from '../../services/evaluation.service';
import { LoggerService } from '../../services/logger.service';

/**
 * Evaluation Details Dialog Data Interface
 */
export interface EvaluationDetailsDialogData {
  evaluationId: string;
  patientName?: string;
  measureName?: string;
}

/**
 * Detailed evaluation information
 */
interface EvaluationDetails {
  id: string;
  measureId: string;
  measureName: string;
  patientId: string;
  patientName: string;
  evaluationDate: string;
  result: string;
  score: number;
  numerator: boolean;
  denominator: boolean;
  exclusion: boolean;
  cqlLibrary?: string;
  expressions?: CqlExpression[];
  fhirResources?: FhirResourceInfo[];
  history?: EvaluationHistoryItem[];
}

interface CqlExpression {
  name: string;
  result: any;
  type: string;
}

interface FhirResourceInfo {
  resourceType: string;
  id: string;
  relevance: string;
  data: any;
}

interface EvaluationHistoryItem {
  date: string;
  result: string;
  score: number;
}

/**
 * Evaluation Details Dialog Component
 *
 * Provides comprehensive view of evaluation results with multiple tabs:
 * - Summary: Key metrics and results
 * - CQL Details: CQL library and expression results
 * - Patient Data: FHIR resources used in evaluation
 * - History: Previous evaluations for same measure
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(EvaluationDetailsDialogComponent, {
 *   data: { evaluationId: 'eval-123', patientName: 'John Doe', measureName: 'CMS125' }
 * });
 * ```
 */
@Component({
  selector: 'app-evaluation-details-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatCardModule,
  ],
  templateUrl: './evaluation-details-dialog.component.html',
  styleUrls: ['./evaluation-details-dialog.component.scss'],
})
export class EvaluationDetailsDialogComponent implements OnInit {
  evaluationDetails = signal<EvaluationDetails | null>(null);
  isLoading = signal(true);
  error = signal<string | null>(null);
  private logger = this.loggerService.withContext('EvaluationDetailsDialogComponent');

  constructor(
    private dialogRef: MatDialogRef<EvaluationDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EvaluationDetailsDialogData,
    private evaluationService: EvaluationService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadEvaluationDetails();
  }

  /**
   * Load evaluation details from service
   */
  loadEvaluationDetails(): void {
    this.isLoading.set(true);
    this.error.set(null);

    // Simulate API call - in production, use actual service
    setTimeout(() => {
      const mockDetails: EvaluationDetails = {
        id: this.data.evaluationId,
        measureId: 'CMS125',
        measureName: this.data.measureName || 'Breast Cancer Screening',
        patientId: 'patient-123',
        patientName: this.data.patientName || 'Jane Smith',
        evaluationDate: new Date().toISOString(),
        result: 'NUMERATOR',
        score: 1.0,
        numerator: true,
        denominator: true,
        exclusion: false,
        cqlLibrary: 'BreastCancerScreening-1.0.0',
        expressions: [
          {
            name: 'Initial Population',
            result: true,
            type: 'Boolean',
          },
          {
            name: 'Denominator',
            result: true,
            type: 'Boolean',
          },
          {
            name: 'Numerator',
            result: true,
            type: 'Boolean',
          },
          {
            name: 'Denominator Exclusion',
            result: false,
            type: 'Boolean',
          },
          {
            name: 'Qualifying Procedures',
            result: [
              {
                resourceType: 'Procedure',
                code: 'Mammography',
                date: '2024-03-15',
              },
            ],
            type: 'List<Procedure>',
          },
        ],
        fhirResources: [
          {
            resourceType: 'Patient',
            id: 'patient-123',
            relevance: 'Subject of evaluation',
            data: {
              name: 'Jane Smith',
              birthDate: '1960-05-15',
              gender: 'female',
            },
          },
          {
            resourceType: 'Procedure',
            id: 'procedure-456',
            relevance: 'Qualifying screening procedure',
            data: {
              code: 'Mammography',
              performedDateTime: '2024-03-15',
              status: 'completed',
            },
          },
          {
            resourceType: 'Condition',
            id: 'condition-789',
            relevance: 'Relevant condition',
            data: {
              code: 'Breast cancer risk',
              recordedDate: '2023-01-10',
            },
          },
        ],
        history: [
          {
            date: '2024-01-15T10:00:00Z',
            result: 'NUMERATOR',
            score: 1.0,
          },
          {
            date: '2023-01-20T14:30:00Z',
            result: 'DENOMINATOR',
            score: 0.0,
          },
          {
            date: '2022-02-10T09:15:00Z',
            result: 'DENOMINATOR',
            score: 0.0,
          },
        ],
      };

      this.evaluationDetails.set(mockDetails);
      this.isLoading.set(false);
    }, 800);
  }

  /**
   * Print evaluation details
   */
  onPrint(): void {
    window.print();
  }

  /**
   * Export to PDF (placeholder)
   */
  onExportPdf(): void {
    this.logger.info('Export to PDF functionality would be implemented here');
    // In production, use a library like jsPDF or pdfmake
  }

  /**
   * Close dialog
   */
  onClose(): void {
    this.dialogRef.close();
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * Format JSON for display
   */
  formatJson(obj: any): string {
    return JSON.stringify(obj, null, 2);
  }

  /**
   * Get result badge color
   */
  getResultColor(result: string): string {
    const colors: Record<string, string> = {
      NUMERATOR: '#4caf50',
      DENOMINATOR: '#ff9800',
      EXCLUSION: '#f44336',
      EXCEPTION: '#9e9e9e',
    };
    return colors[result] || '#2196f3';
  }

  /**
   * Get icon for result
   */
  getResultIcon(result: string): string {
    const icons: Record<string, string> = {
      NUMERATOR: 'check_circle',
      DENOMINATOR: 'pending',
      EXCLUSION: 'cancel',
      EXCEPTION: 'help',
    };
    return icons[result] || 'info';
  }

  /**
   * Get icon for FHIR resource type
   */
  getResourceIcon(resourceType: string): string {
    const icons: Record<string, string> = {
      Patient: 'person',
      Procedure: 'medical_services',
      Condition: 'sick',
      Observation: 'monitor_heart',
      Medication: 'medication',
      Encounter: 'local_hospital',
      AllergyIntolerance: 'warning',
      DiagnosticReport: 'lab_profile',
    };
    return icons[resourceType] || 'description';
  }
}
