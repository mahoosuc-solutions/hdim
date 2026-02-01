import { Component, Input, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { LoggerService } from '../../services/logger.service';
import { PatientService } from '../../services/patient.service';
import { CareGapService } from '../../services/care-gap.service';

/**
 * Timeline Event Types
 */
export type TimelineEventType = 'ENCOUNTER' | 'OBSERVATION' | 'MEDICATION' | 'PROCEDURE' | 'CARE_GAP';

/**
 * Timeline Event Interface
 */
export interface TimelineEvent {
  id: string;
  type: TimelineEventType;
  timestamp: Date;
  title: string;
  description: string;
  icon: string;
  color: string;
  details?: any;
  resourceType?: string;
}

/**
 * Patient Timeline Component
 *
 * Displays chronological timeline of patient events including:
 * - Encounters (visits, admissions)
 * - Observations (vitals, labs)
 * - Medications (prescriptions, administrations)
 * - Procedures (surgeries, treatments)
 * - Care Gaps (identified gaps)
 *
 * Features:
 * - Filterable by event type
 * - Date range filtering
 * - Keyword search
 * - Sortable (newest/oldest first)
 * - Expandable event details
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - All data cached for max 5 minutes
 * - PHI filtering in logs
 *
 * Sprint 2 - Issue #238: Patient Timeline View
 */
@Component({
  selector: 'app-patient-timeline',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
  ],
  templateUrl: './patient-timeline.component.html',
  styleUrls: ['./patient-timeline.component.scss'],
})
export class PatientTimelineComponent implements OnInit, OnDestroy {
  @Input() patientId!: string;

  // Signals for reactive state
  events = signal<TimelineEvent[]>([]);
  filteredEvents = signal<TimelineEvent[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);

  // Filter state
  eventTypeFilters = signal({
    ENCOUNTER: true,
    OBSERVATION: true,
    MEDICATION: true,
    PROCEDURE: true,
    CARE_GAP: true,
  });

  searchKeyword = signal('');
  startDate = signal<Date | null>(null);
  endDate = signal<Date | null>(null);
  sortOrder = signal<'newest' | 'oldest'>('newest');

  // Expanded event IDs
  expandedEventIds = new Set<string>();

  private destroy$ = new Subject<void>();
  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(
    private patientService: PatientService,
    private careGapService: CareGapService,
    private logger: LoggerService
  ) {
  }

  ngOnInit(): void {
    if (!this.patientId) {
      this.logger.error('Patient ID is required for timeline');
      this.loadError.set('Patient ID is required');
      this.loading.set(false);
      return;
    }

    this.loadTimelineEvents();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all timeline events for patient
   */
  private loadTimelineEvents(): void {
    this.loading.set(true);
    this.loadError.set(null);

    // TODO: Replace with actual API integration when backend endpoints available
    // For now, using mock data to demonstrate UI functionality
    setTimeout(() => {
      const mockEvents: TimelineEvent[] = [
        // Mock Encounters
        {
          id: 'enc-001',
          type: 'ENCOUNTER',
          timestamp: new Date('2024-01-15T10:30:00'),
          title: 'Annual Wellness Visit',
          description: 'Routine check-up and preventive care screening',
          icon: 'local_hospital',
          color: '#1976d2',
          details: {
            location: 'Primary Care Clinic',
            provider: 'Dr. Sarah Johnson',
            duration: '45 minutes',
          },
        },
        {
          id: 'enc-002',
          type: 'ENCOUNTER',
          timestamp: new Date('2023-11-20T14:00:00'),
          title: 'Follow-up Visit',
          description: 'Follow-up for hypertension management',
          icon: 'local_hospital',
          color: '#1976d2',
          details: {
            location: 'Primary Care Clinic',
            provider: 'Dr. Sarah Johnson',
            duration: '30 minutes',
          },
        },

        // Mock Observations
        {
          id: 'obs-001',
          type: 'OBSERVATION',
          timestamp: new Date('2024-01-15T10:45:00'),
          title: 'Blood Pressure Reading',
          description: '128/82 mmHg - Within normal range',
          icon: 'favorite',
          color: '#388e3c',
          details: {
            systolic: 128,
            diastolic: 82,
            unit: 'mmHg',
            status: 'Normal',
          },
        },
        {
          id: 'obs-002',
          type: 'OBSERVATION',
          timestamp: new Date('2024-01-15T11:00:00'),
          title: 'HbA1c Test',
          description: '6.2% - Good diabetes control',
          icon: 'science',
          color: '#388e3c',
          details: {
            value: 6.2,
            unit: '%',
            referenceRange: '< 7.0%',
            status: 'Good Control',
          },
        },

        // Mock Medications
        {
          id: 'med-001',
          type: 'MEDICATION',
          timestamp: new Date('2024-01-15T11:15:00'),
          title: 'Lisinopril Prescribed',
          description: '10mg once daily for hypertension',
          icon: 'medication',
          color: '#f57c00',
          details: {
            medication: 'Lisinopril',
            dose: '10mg',
            frequency: 'Once daily',
            indication: 'Hypertension',
            prescriber: 'Dr. Sarah Johnson',
          },
        },

        // Mock Procedures
        {
          id: 'proc-001',
          type: 'PROCEDURE',
          timestamp: new Date('2023-12-01T08:00:00'),
          title: 'Colonoscopy',
          description: 'Screening colonoscopy - No abnormalities found',
          icon: 'healing',
          color: '#7b1fa2',
          details: {
            procedure: 'Colonoscopy',
            indication: 'Colorectal cancer screening',
            result: 'Normal - no polyps detected',
            provider: 'Dr. Michael Chen',
          },
        },

        // Mock Care Gaps
        {
          id: 'gap-001',
          type: 'CARE_GAP',
          timestamp: new Date('2024-01-20T00:00:00'),
          title: 'Breast Cancer Screening Due',
          description: 'Mammography screening overdue by 45 days',
          icon: 'warning',
          color: '#d32f2f',
          details: {
            measure: 'BCS-E - Breast Cancer Screening',
            daysOverdue: 45,
            lastScreening: '2022-12-10',
            nextDue: '2024-12-10',
            priority: 'High',
          },
        },
      ];

      // Sort by timestamp (newest first by default)
      const sortedEvents = mockEvents.sort(
        (a, b) => b.timestamp.getTime() - a.timestamp.getTime()
      );

      this.events.set(sortedEvents);
      this.applyFilters();
      this.loading.set(false);

      this.logger.info('Timeline events loaded', {
        patientId: this.patientId,
        eventCount: sortedEvents.length,
      });
    }, 500);
  }

  /**
   * Apply filters to events
   */
  private applyFilters(): void {
    let filtered = this.events();

    // Filter by event type
    const typeFilters = this.eventTypeFilters();
    filtered = filtered.filter((event) => typeFilters[event.type]);

    // Filter by date range
    const start = this.startDate();
    const end = this.endDate();
    if (start) {
      filtered = filtered.filter((event) => event.timestamp >= start);
    }
    if (end) {
      filtered = filtered.filter((event) => event.timestamp <= end);
    }

    // Filter by keyword search
    const keyword = this.searchKeyword().toLowerCase();
    if (keyword) {
      filtered = filtered.filter(
        (event) =>
          event.title.toLowerCase().includes(keyword) ||
          event.description.toLowerCase().includes(keyword)
      );
    }

    // Sort by timestamp
    const order = this.sortOrder();
    filtered = [...filtered].sort((a, b) =>
      order === 'newest'
        ? b.timestamp.getTime() - a.timestamp.getTime()
        : a.timestamp.getTime() - b.timestamp.getTime()
    );

    this.filteredEvents.set(filtered);
  }

  /**
   * Toggle event type filter
   */
  toggleEventType(type: TimelineEventType): void {
    const current = this.eventTypeFilters();
    this.eventTypeFilters.set({
      ...current,
      [type]: !current[type],
    });
    this.applyFilters();
  }

  /**
   * Update search keyword
   */
  onSearchChange(keyword: string): void {
    this.searchKeyword.set(keyword);
    this.applyFilters();
  }

  /**
   * Update date range
   */
  onDateRangeChange(): void {
    this.applyFilters();
  }

  /**
   * Toggle sort order
   */
  toggleSortOrder(): void {
    this.sortOrder.set(this.sortOrder() === 'newest' ? 'oldest' : 'newest');
    this.applyFilters();
  }

  /**
   * Toggle event details expansion
   */
  toggleEventDetails(eventId: string): void {
    if (this.expandedEventIds.has(eventId)) {
      this.expandedEventIds.delete(eventId);
    } else {
      this.expandedEventIds.add(eventId);
    }
  }

  /**
   * Check if event is expanded
   */
  isEventExpanded(eventId: string): boolean {
    return this.expandedEventIds.has(eventId);
  }

  /**
   * Format timestamp for display
   */
  formatTimestamp(timestamp: Date): string {
    return new Intl.DateTimeFormat('en-US', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(timestamp);
  }

  /**
   * Get event type label
   */
  getEventTypeLabel(type: TimelineEventType): string {
    const labels: Record<TimelineEventType, string> = {
      ENCOUNTER: 'Encounters',
      OBSERVATION: 'Observations',
      MEDICATION: 'Medications',
      PROCEDURE: 'Procedures',
      CARE_GAP: 'Care Gaps',
    };
    return labels[type];
  }

  /**
   * Get active filter count
   */
  getActiveFilterCount(): number {
    const typeFilters = this.eventTypeFilters();
    const activeTypes = Object.values(typeFilters).filter((v) => v).length;
    const hasDateRange = this.startDate() || this.endDate();
    const hasKeyword = this.searchKeyword().length > 0;

    return (activeTypes < 5 ? 1 : 0) + (hasDateRange ? 1 : 0) + (hasKeyword ? 1 : 0);
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.eventTypeFilters.set({
      ENCOUNTER: true,
      OBSERVATION: true,
      MEDICATION: true,
      PROCEDURE: true,
      CARE_GAP: true,
    });
    this.searchKeyword.set('');
    this.startDate.set(null);
    this.endDate.set(null);
    this.applyFilters();
  }
}
