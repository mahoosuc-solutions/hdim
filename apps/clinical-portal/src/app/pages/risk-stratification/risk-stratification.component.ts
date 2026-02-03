import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, debounceTime, catchError } from 'rxjs/operators';

import {
  RiskLevel,
  RiskLevelConfig,
  PatientRiskProfile,
  RiskGroupSummary,
  RISK_LEVELS,
  RISK_FACTORS,
  CONDITION_CATEGORIES,
  getRiskLevel,
  getRiskLevelByName,
  getRiskTrendIcon,
  getRiskTrendClass
} from './risk-model.config';
import { PatientService } from '../../services/patient.service';
import { CareGapService, CareGapApiItem, CareGapPageResponse } from '../../services/care-gap.service';
import { RiskAssessmentService } from '../../services/risk-assessment.service';
import { Patient } from '../../models/patient.model';
import { HccRiskAssessment } from '../../models/risk-assessment.model';
import { LoggerService } from '../../services/logger.service';

/**
 * Risk Stratification Component
 * Issue #147: Add Risk Stratification View
 *
 * Provides patient risk grouping visualization with actionable insights
 * for primary care providers managing panels of 150+ patients.
 */
@Component({
  selector: 'app-risk-stratification',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatPaginatorModule,
    MatSortModule,
    MatExpansionModule,
    MatProgressBarModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './risk-stratification.component.html',
  styleUrl: './risk-stratification.component.scss'
})
export class RiskStratificationComponent implements OnInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  // Data
  allPatients: PatientRiskProfile[] = [];
  filteredPatients: PatientRiskProfile[] = [];
  dataSource = new MatTableDataSource<PatientRiskProfile>([]);
  riskGroupSummaries: RiskGroupSummary[] = [];

  // UI State
  loading = false;
  selectedRiskLevel: RiskLevel | null = null;
  selectedCondition: string | null = null;
  searchTerm = '';
  expandedPatientId: string | null = null;
  viewMode: 'cards' | 'table' = 'cards';

  // Table configuration
  displayedColumns: string[] = [
    'riskScore',
    'patientName',
    'age',
    'primaryConditions',
    'openCareGaps',
    'lastVisit',
    'trending',
    'actions'
  ];

  // Configuration references for template
  readonly riskLevels = RISK_LEVELS;
  readonly riskFactors = RISK_FACTORS;
  readonly conditionCategories = CONDITION_CATEGORIES;

  // Cache for HCC risk assessments by patient ID
  private hccAssessments = new Map<string, HccRiskAssessment>();

  constructor(
    private router: Router,
    private patientService: PatientService,
    private careGapService: CareGapService,
    private riskAssessmentService: RiskAssessmentService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.setupSearch();
    this.loadPatients();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearch(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm = term;
      this.applyFilters();
    });
  }

  loadPatients(): void {
    this.loading = true;
    forkJoin({
      patients: this.patientService.getPatients(200).pipe(
        catchError(() => of([] as Patient[]))
      ),
      careGaps: this.careGapService.getCareGapsPage({ size: 500 }).pipe(
        catchError(() => of({
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0,
          size: 0
        } as CareGapPageResponse))
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ patients, careGaps }) => {
        const gapsByPatient = this.indexCareGaps(careGaps.content || []);

        // First pass: map patients with basic data
        this.allPatients = patients.map((patient) =>
          this.mapPatientToRiskProfile(patient, gapsByPatient.get(patient.id) || [], null)
        );

        // Then fetch HCC assessments in parallel for all patients
        this.fetchHccAssessments(patients);

        this.calculateRiskGroupSummaries();
        this.applyFilters();
        this.loading = false;
      });
  }

  /**
   * Fetch HCC risk assessments for all patients in parallel.
   * Updates patient risk profiles as assessments arrive.
   */
  private fetchHccAssessments(patients: Patient[]): void {
    // Fetch assessments for each patient (limit to first 50 to avoid overwhelming the API)
    const patientsToFetch = patients.slice(0, 50);

    patientsToFetch.forEach(patient => {
      this.riskAssessmentService.getHccRiskAssessment(patient.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe(assessment => {
          if (assessment && assessment.dataAvailability.hccDataAvailable) {
            this.hccAssessments.set(patient.id, assessment);

            // Update the patient's risk profile with real HCC data
            const patientIndex = this.allPatients.findIndex(p => p.patientId === patient.id);
            if (patientIndex !== -1) {
              this.allPatients[patientIndex] = this.updateRiskProfileWithHcc(
                this.allPatients[patientIndex],
                assessment
              );

              // Recalculate summaries and refresh view
              this.calculateRiskGroupSummaries();
              this.applyFilters();
            }
          }
        });
    });
  }

  /**
   * Update a patient risk profile with real HCC assessment data.
   */
  private updateRiskProfileWithHcc(
    profile: PatientRiskProfile,
    hcc: HccRiskAssessment
  ): PatientRiskProfile {
    // Convert backend risk level to frontend format
    const riskLevelMap: Record<string, RiskLevel> = {
      'LOW': 'low',
      'MODERATE': 'moderate',
      'HIGH': 'high',
      'VERY_HIGH': 'very-high'
    };

    const mappedRiskLevel = riskLevelMap[hcc.riskLevel] || 'low';

    return {
      ...profile,
      overallRiskScore: hcc.riskScore,
      riskLevel: mappedRiskLevel,
      hccScore: hcc.rafScoreBlended ?? 0,
      openCareGaps: hcc.openCareGaps,
      primaryConditions: hcc.chronicConditions.length > 0
        ? hcc.chronicConditions
        : profile.primaryConditions,
      riskFactors: this.buildHccRiskFactors(hcc),
    };
  }

  /**
   * Build risk factors from HCC assessment data.
   */
  private buildHccRiskFactors(hcc: HccRiskAssessment): PatientRiskProfile['riskFactors'] {
    const factors: PatientRiskProfile['riskFactors'] = [];

    // RAF score factor
    if (hcc.rafScoreBlended !== null && hcc.rafScoreBlended > 0) {
      const rafContribution = Math.min(50, Math.round(hcc.rafScoreBlended * 25));
      factors.push({
        factorId: 'raf-score',
        factorName: `RAF Score: ${hcc.rafScoreBlended.toFixed(3)}`,
        rawScore: hcc.rafScoreBlended,
        weightedScore: rafContribution,
        maxScore: 50,
        percentContribution: rafContribution
      });
    }

    // HCC conditions
    if (hcc.hccCount > 0) {
      const hccContribution = Math.min(30, hcc.hccCount * 6);
      factors.push({
        factorId: 'hcc-conditions',
        factorName: `${hcc.hccCount} HCC Conditions`,
        rawScore: hcc.hccCount,
        weightedScore: hccContribution,
        maxScore: 30,
        percentContribution: hccContribution
      });
    }

    // Care gaps
    if (hcc.openCareGaps > 0) {
      const gapContribution = Math.min(20, hcc.openCareGaps * 5);
      factors.push({
        factorId: 'care-gaps',
        factorName: `${hcc.openCareGaps} Open Care Gaps`,
        rawScore: hcc.openCareGaps,
        weightedScore: gapContribution,
        maxScore: 20,
        percentContribution: gapContribution
      });
    }

    // If no significant factors, show low risk
    if (factors.length === 0) {
      factors.push({
        factorId: 'low-risk',
        factorName: 'Low risk profile',
        rawScore: 0,
        weightedScore: 10,
        maxScore: 10,
        percentContribution: 10
      });
    }

    return factors;
  }

  private indexCareGaps(gaps: CareGapApiItem[]): Map<string, CareGapApiItem[]> {
    const map = new Map<string, CareGapApiItem[]>();
    gaps.forEach((gap) => {
      const list = map.get(gap.patientId) || [];
      list.push(gap);
      map.set(gap.patientId, list);
    });
    return map;
  }

  private mapPatientToRiskProfile(
    patient: Patient,
    gaps: CareGapApiItem[],
    hccAssessment: HccRiskAssessment | null
  ): PatientRiskProfile {
    const summary = this.patientService.toPatientSummary(patient);
    const age = summary.age ?? 0;
    const gapCount = gaps.length;

    // Use HCC-based scoring if available, otherwise fall back to calculated score
    const riskScore = hccAssessment?.riskScore ?? this.calculateRiskScore(age, gapCount);
    const riskLevel = hccAssessment
      ? this.mapHccRiskLevel(hccAssessment.riskLevel)
      : getRiskLevel(riskScore).level;

    // Use HCC conditions if available, otherwise derive from care gaps
    const primaryConditions = hccAssessment?.chronicConditions?.length
      ? hccAssessment.chronicConditions
      : (this.mapGapConditions(gaps).length ? this.mapGapConditions(gaps) : ['General Wellness']);

    const riskFactors = hccAssessment
      ? this.buildHccRiskFactors(hccAssessment)
      : this.buildRiskFactors(age, gapCount);

    // Use real RAF score if available
    const hccScore = hccAssessment?.rafScoreBlended ?? Math.round((riskScore / 100) * 3.5 * 100) / 100;

    return {
      patientId: patient.id,
      patientName: summary.fullName,
      mrn: summary.mrn || 'N/A',
      dateOfBirth: patient.birthDate || 'N/A',
      age,
      gender: patient.gender || 'unknown',
      overallRiskScore: riskScore,
      riskLevel,
      riskFactors,
      primaryConditions,
      openCareGaps: hccAssessment?.openCareGaps ?? gapCount,
      lastVisit: this.formatLastUpdated(patient),
      nextScheduledVisit: undefined,
      recentEdVisits: 0,
      hccScore,
      sdohRiskFactors: undefined,
      trending: this.getTrending(hccAssessment?.openCareGaps ?? gapCount)
    };
  }

  /**
   * Map backend risk level to frontend format.
   */
  private mapHccRiskLevel(backendLevel: string): RiskLevel {
    const map: Record<string, RiskLevel> = {
      'LOW': 'low',
      'MODERATE': 'moderate',
      'HIGH': 'high',
      'VERY_HIGH': 'very-high'
    };
    return map[backendLevel] || 'low';
  }

  private calculateRiskScore(age: number, gapCount: number): number {
    const ageScore = Math.min(40, Math.round(age * 0.5));
    const gapScore = Math.min(60, gapCount * 12);
    return Math.min(100, ageScore + gapScore);
  }

  private mapGapConditions(gaps: CareGapApiItem[]): string[] {
    const conditions = new Set<string>();
    gaps.forEach((gap) => {
      switch (gap.measureId) {
        case 'CDC':
        case 'EED':
        case 'KED':
          conditions.add('Diabetes');
          break;
        case 'SPC':
        case 'BPD':
          conditions.add('Cardiovascular');
          break;
        case 'DSF':
          conditions.add('Behavioral Health');
          break;
        case 'BCS':
        case 'CCS':
        case 'COL':
        case 'OSW':
        case 'AWV':
          conditions.add('Preventive Care');
          break;
        default:
          if (gap.measureName) {
            conditions.add(gap.measureName);
          }
      }
    });
    return Array.from(conditions);
  }

  private buildRiskFactors(age: number, gapCount: number): PatientRiskProfile['riskFactors'] {
    const factors = [];
    if (gapCount >= 3) {
      factors.push({
        factorId: 'care-gaps',
        factorName: 'Multiple care gaps',
        rawScore: gapCount,
        weightedScore: Math.min(40, gapCount * 10),
        maxScore: 40,
        percentContribution: 40
      });
    } else if (gapCount > 0) {
      factors.push({
        factorId: 'care-gaps',
        factorName: 'Open care gaps',
        rawScore: gapCount,
        weightedScore: Math.min(25, gapCount * 8),
        maxScore: 25,
        percentContribution: 25
      });
    }
    if (age >= 65) {
      factors.push({
        factorId: 'age',
        factorName: 'Senior age',
        rawScore: age,
        weightedScore: 30,
        maxScore: 30,
        percentContribution: 30
      });
    }
    if (factors.length === 0) {
      factors.push({
        factorId: 'low-risk',
        factorName: 'Low risk profile',
        rawScore: 0,
        weightedScore: 10,
        maxScore: 10,
        percentContribution: 10
      });
    }
    return factors;
  }

  private formatLastUpdated(patient: Patient): string {
    if (!patient.birthDate) return new Date().toISOString().split('T')[0];
    const parsed = new Date(patient.birthDate);
    return isNaN(parsed.getTime()) ? new Date().toISOString().split('T')[0] : parsed.toISOString().split('T')[0];
  }

  private getTrending(gapCount: number): 'improving' | 'stable' | 'worsening' {
    if (gapCount >= 3) return 'worsening';
    if (gapCount === 0) return 'improving';
    return 'stable';
  }

  private calculateRiskGroupSummaries(): void {
    this.riskGroupSummaries = RISK_LEVELS.map(level => {
      const patientsInGroup = this.allPatients.filter(p => p.riskLevel === level.level);
      const conditionCounts = new Map<string, number>();
      const factorCounts = new Map<string, number>();

      patientsInGroup.forEach(p => {
        p.primaryConditions.forEach(c => {
          conditionCounts.set(c, (conditionCounts.get(c) || 0) + 1);
        });
        p.riskFactors.forEach(f => {
          if (f.percentContribution > 15) {
            factorCounts.set(f.factorName, (factorCounts.get(f.factorName) || 0) + 1);
          }
        });
      });

      const topConditions = Array.from(conditionCounts.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([name]) => name);

      const topRiskFactors = Array.from(factorCounts.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([name]) => name);

      return {
        level: level.level,
        count: patientsInGroup.length,
        percentage: Math.round((patientsInGroup.length / this.allPatients.length) * 100),
        avgScore: patientsInGroup.length > 0
          ? Math.round(patientsInGroup.reduce((sum, p) => sum + p.overallRiskScore, 0) / patientsInGroup.length)
          : 0,
        topConditions,
        topRiskFactors
      };
    });
  }

  applyFilters(): void {
    let result = [...this.allPatients];

    // Filter by risk level
    if (this.selectedRiskLevel) {
      result = result.filter(p => p.riskLevel === this.selectedRiskLevel);
    }

    // Filter by condition
    if (this.selectedCondition) {
      result = result.filter(p =>
        p.primaryConditions.some(c =>
          c.toLowerCase().includes(this.selectedCondition!.toLowerCase())
        )
      );
    }

    // Filter by search term
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(p =>
        p.patientName.toLowerCase().includes(term) ||
        p.mrn.toLowerCase().includes(term) ||
        p.primaryConditions.some(c => c.toLowerCase().includes(term))
      );
    }

    this.filteredPatients = result;
    this.dataSource.data = result;

    if (this.paginator) {
      this.paginator.firstPage();
    }
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  selectRiskLevel(level: RiskLevel | null): void {
    this.selectedRiskLevel = this.selectedRiskLevel === level ? null : level;
    this.applyFilters();
  }

  selectCondition(condition: string | null): void {
    this.selectedCondition = this.selectedCondition === condition ? null : condition;
    this.applyFilters();
  }

  clearFilters(): void {
    this.selectedRiskLevel = null;
    this.selectedCondition = null;
    this.searchTerm = '';
    this.applyFilters();
  }

  toggleViewMode(): void {
    this.viewMode = this.viewMode === 'cards' ? 'table' : 'cards';
  }

  togglePatientExpanded(patientId: string): void {
    this.expandedPatientId = this.expandedPatientId === patientId ? null : patientId;
  }

  // Helper methods for template
  getRiskLevelConfig(level: RiskLevel): RiskLevelConfig {
    return getRiskLevelByName(level);
  }

  getRiskLevelForScore(score: number): RiskLevelConfig {
    return getRiskLevel(score);
  }

  getTrendIcon(trending: 'improving' | 'stable' | 'worsening'): string {
    return getRiskTrendIcon(trending);
  }

  getTrendClass(trending: 'improving' | 'stable' | 'worsening'): string {
    return getRiskTrendClass(trending);
  }

  getGroupSummary(level: RiskLevel): RiskGroupSummary | undefined {
    return this.riskGroupSummaries.find(s => s.level === level);
  }

  getFactorContributionWidth(factor: { weightedScore: number; maxScore: number }): number {
    return Math.min(100, (factor.weightedScore / factor.maxScore) * 100);
  }

  // Navigation actions
  viewPatientDetails(patient: PatientRiskProfile): void {
    this.router.navigate(['/patients', patient.patientId]);
  }

  viewCareGaps(patient: PatientRiskProfile): void {
    this.router.navigate(['/care-gaps'], {
      queryParams: { patientId: patient.patientId }
    });
  }

  scheduleVisit(patient: PatientRiskProfile): void {
    // Would integrate with scheduling system
    this.logger.info('Schedule visit for patient', patient.patientName);
  }

  initiateOutreach(patient: PatientRiskProfile): void {
    // Would initiate care coordination outreach
    this.logger.info('Initiate outreach for patient', patient.patientName);
  }

  exportPatientList(): void {
    const data = this.filteredPatients.map(p => ({
      'Patient Name': p.patientName,
      'MRN': p.mrn,
      'Age': p.age,
      'Risk Score': p.overallRiskScore,
      'Risk Level': getRiskLevelByName(p.riskLevel).label,
      'Primary Conditions': p.primaryConditions.join('; '),
      'Open Care Gaps': p.openCareGaps,
      'Last Visit': p.lastVisit,
      'Trend': p.trending
    }));

    const csv = this.convertToCSV(data);
    this.downloadCSV(csv, 'risk-stratification-report.csv');
  }

  private convertToCSV(data: any[]): string {
    if (data.length === 0) return '';

    const headers = Object.keys(data[0]);
    const rows = data.map(row =>
      headers.map(h => {
        const val = row[h];
        return typeof val === 'string' && val.includes(',') ? `"${val}"` : val;
      }).join(',')
    );

    return [headers.join(','), ...rows].join('\n');
  }

  private downloadCSV(content: string, filename: string): void {
    const blob = new Blob([content], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  // Pagination
  onPageChange(event: PageEvent): void {
    // Handle if needed
  }

  // Sorting
  onSortChange(sortState: Sort): void {
    const data = [...this.filteredPatients];

    if (!sortState.active || sortState.direction === '') {
      this.dataSource.data = data;
      return;
    }

    this.dataSource.data = data.sort((a, b) => {
      const isAsc = sortState.direction === 'asc';
      switch (sortState.active) {
        case 'riskScore': return this.compare(a.overallRiskScore, b.overallRiskScore, isAsc);
        case 'patientName': return this.compare(a.patientName, b.patientName, isAsc);
        case 'age': return this.compare(a.age, b.age, isAsc);
        case 'openCareGaps': return this.compare(a.openCareGaps, b.openCareGaps, isAsc);
        case 'lastVisit': return this.compare(a.lastVisit, b.lastVisit, isAsc);
        default: return 0;
      }
    });
  }

  private compare(a: number | string, b: number | string, isAsc: boolean): number {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }
}
