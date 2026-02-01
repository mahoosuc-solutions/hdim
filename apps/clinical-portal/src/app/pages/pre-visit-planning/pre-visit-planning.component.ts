import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { PatientService } from '../../services/patient.service';
import { PatientSummary } from '../../models/patient.model';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { LoggerService } from '../../services/logger.service';

/**
 * Pre-visit patient summary with care gaps and appointment info
 */
export interface PreVisitPatient {
  id: string;
  fullName: string;
  mrn: string;
  dateOfBirth: string;
  age: number;
  appointmentTime: string;
  careGaps: PreVisitCareGap[];
  prepared: boolean;
  lastVisit?: string;
  primaryConditions: string[];
  notes?: string;
}

/**
 * Care gap for pre-visit planning
 */
export interface PreVisitCareGap {
  id: string;
  measureName: string;
  description: string;
  urgency: 'high' | 'medium' | 'low';
  daysOverdue: number;
  recommendedAction: string;
}

@Component({
  selector: 'app-pre-visit-planning',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatNativeDateModule,
    MatExpansionModule,
    MatCheckboxModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatDividerModule,
    LoadingOverlayComponent,
    LoadingButtonComponent,
  ],
  templateUrl: './pre-visit-planning.component.html',
  styleUrl: './pre-visit-planning.component.scss',
})
export class PreVisitPlanningComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Date selection
  selectedDate = new FormControl<Date>(this.getTomorrowDate());
  minDate = new Date();
  maxDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 days ahead

  // State signals
  loading = signal(false);
  error = signal<string | null>(null);
  patients = signal<PreVisitPatient[]>([]);
  expandedPatientId = signal<string | null>(null);

  // Computed values
  totalPatients = computed(() => this.patients().length);
  totalCareGaps = computed(() =>
    this.patients().reduce((sum, p) => sum + p.careGaps.length, 0)
  );
  preparedCount = computed(() =>
    this.patients().filter(p => p.prepared).length
  );
  highUrgencyCount = computed(() =>
    this.patients().reduce((sum, p) =>
      sum + p.careGaps.filter(g => g.urgency === 'high').length, 0)
  );  constructor(
    private patientService: PatientService,
    private router: Router,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadPatientsForDate();

    // Reload when date changes
    this.selectedDate.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadPatientsForDate());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get tomorrow's date (default for pre-visit planning)
   */
  private getTomorrowDate(): Date {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(0, 0, 0, 0);
    return tomorrow;
  }

  /**
   * Load patients scheduled for the selected date
   */
  loadPatientsForDate(): void {
    this.loading.set(true);
    this.error.set(null);

    // For demo purposes, generate mock scheduled patients
    // In production, this would call a scheduling API
    this.patientService.getPatientsSummaryCached()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (patients) => {
          // Generate mock pre-visit data for demo
          const preVisitPatients = this.generatePreVisitData(patients);
          this.patients.set(preVisitPatients);
        },
        error: (err) => {
          this.logger.error('Failed to load patients', err);
          this.error.set('Failed to load scheduled patients. Please try again.');
        }
      });
  }

  /**
   * Generate mock pre-visit data for demo purposes
   * In production, this would come from scheduling + care gap APIs
   */
  private generatePreVisitData(patients: PatientSummary[]): PreVisitPatient[] {
    const appointmentTimes = [
      '9:00 AM', '9:30 AM', '10:00 AM', '10:30 AM', '11:00 AM',
      '1:00 PM', '1:30 PM', '2:00 PM', '2:30 PM', '3:00 PM'
    ];

    // Take up to 8 patients for demo
    return patients.slice(0, 8).map((patient, index) => ({
      id: patient.id,
      fullName: patient.fullName,
      mrn: patient.mrn || `MRN-${index + 100}`,
      dateOfBirth: patient.dateOfBirth || '1970-01-01',
      age: this.calculateAge(patient.dateOfBirth || '1970-01-01'),
      appointmentTime: appointmentTimes[index % appointmentTimes.length],
      prepared: false,
      primaryConditions: this.getMockConditions(index),
      careGaps: this.getMockCareGaps(patient.id, index),
      lastVisit: this.getMockLastVisit(index),
    }));
  }

  private calculateAge(dateOfBirth: string): number {
    const birth = new Date(dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  private getMockConditions(index: number): string[] {
    const conditionSets = [
      ['Type 2 Diabetes', 'Hypertension'],
      ['Hypertension', 'Hyperlipidemia'],
      ['Type 2 Diabetes'],
      ['COPD', 'Hypertension'],
      ['Depression', 'Anxiety'],
      ['Coronary Artery Disease'],
      ['Type 2 Diabetes', 'CKD Stage 3'],
      ['Osteoarthritis', 'Obesity'],
    ];
    return conditionSets[index % conditionSets.length];
  }

  private getMockCareGaps(patientId: string, index: number): PreVisitCareGap[] {
    const gapSets: PreVisitCareGap[][] = [
      [
        { id: `${patientId}-gap1`, measureName: 'Diabetes: HbA1c Control', description: 'HbA1c test overdue', urgency: 'high', daysOverdue: 45, recommendedAction: 'Order HbA1c lab' },
        { id: `${patientId}-gap2`, measureName: 'Diabetes: Eye Exam', description: 'Annual eye exam due', urgency: 'medium', daysOverdue: 30, recommendedAction: 'Refer to ophthalmology' },
      ],
      [
        { id: `${patientId}-gap1`, measureName: 'Controlling Blood Pressure', description: 'BP not at goal', urgency: 'high', daysOverdue: 60, recommendedAction: 'Review BP medications' },
        { id: `${patientId}-gap2`, measureName: 'Statin Therapy', description: 'Statin not prescribed', urgency: 'medium', daysOverdue: 90, recommendedAction: 'Prescribe statin' },
      ],
      [
        { id: `${patientId}-gap1`, measureName: 'Colorectal Screening', description: 'Colonoscopy overdue', urgency: 'high', daysOverdue: 120, recommendedAction: 'Order colonoscopy' },
      ],
      [
        { id: `${patientId}-gap1`, measureName: 'COPD: Spirometry', description: 'Spirometry not performed', urgency: 'medium', daysOverdue: 180, recommendedAction: 'Schedule spirometry' },
        { id: `${patientId}-gap2`, measureName: 'COPD: Rescue Inhaler', description: 'No rescue inhaler prescribed', urgency: 'low', daysOverdue: 30, recommendedAction: 'Review inhaler therapy' },
      ],
      [
        { id: `${patientId}-gap1`, measureName: 'Depression Screening', description: 'PHQ-9 not completed', urgency: 'medium', daysOverdue: 365, recommendedAction: 'Administer PHQ-9' },
      ],
      [
        { id: `${patientId}-gap1`, measureName: 'CAD: Statin Therapy', description: 'High-intensity statin recommended', urgency: 'high', daysOverdue: 30, recommendedAction: 'Optimize statin therapy' },
        { id: `${patientId}-gap2`, measureName: 'CAD: Aspirin', description: 'Aspirin therapy review', urgency: 'low', daysOverdue: 15, recommendedAction: 'Confirm aspirin therapy' },
        { id: `${patientId}-gap3`, measureName: 'CAD: BP Control', description: 'Blood pressure monitoring', urgency: 'medium', daysOverdue: 45, recommendedAction: 'Check BP at visit' },
      ],
      [],  // No gaps
      [
        { id: `${patientId}-gap1`, measureName: 'BMI Assessment', description: 'BMI counseling due', urgency: 'low', daysOverdue: 90, recommendedAction: 'Discuss weight management' },
      ],
    ];
    return gapSets[index % gapSets.length];
  }

  private getMockLastVisit(index: number): string {
    const daysAgo = [30, 45, 60, 90, 120, 180, 15, 365][index % 8];
    const lastVisit = new Date();
    lastVisit.setDate(lastVisit.getDate() - daysAgo);
    return lastVisit.toISOString().split('T')[0];
  }

  /**
   * Toggle patient expansion
   */
  toggleExpand(patientId: string): void {
    if (this.expandedPatientId() === patientId) {
      this.expandedPatientId.set(null);
    } else {
      this.expandedPatientId.set(patientId);
    }
  }

  /**
   * Mark patient as prepared
   */
  togglePrepared(patient: PreVisitPatient, event: Event): void {
    event.stopPropagation();
    const updatedPatients = this.patients().map(p =>
      p.id === patient.id ? { ...p, prepared: !p.prepared } : p
    );
    this.patients.set(updatedPatients);
  }

  /**
   * Navigate to patient detail
   */
  viewPatient(patientId: string): void {
    this.router.navigate(['/patients', patientId]);
  }

  /**
   * Print all summaries
   */
  printAllSummaries(): void {
    window.print();
  }

  /**
   * Export to PDF (placeholder)
   */
  exportPdf(): void {
    // In production, this would generate a PDF
    this.logger.info('Export PDF - would generate PDF with patients', this.patients().length);
    alert('PDF export would be generated here. Feature coming soon!');
  }

  /**
   * Get urgency badge class
   */
  getUrgencyClass(urgency: string): string {
    switch (urgency) {
      case 'high': return 'urgency-high';
      case 'medium': return 'urgency-medium';
      case 'low': return 'urgency-low';
      default: return '';
    }
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  /**
   * Calculate days since last visit
   */
  daysSinceLastVisit(lastVisit?: string): string {
    if (!lastVisit) return 'No prior visit';
    const days = Math.floor((Date.now() - new Date(lastVisit).getTime()) / (1000 * 60 * 60 * 24));
    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    return `${days} days ago`;
  }
}
