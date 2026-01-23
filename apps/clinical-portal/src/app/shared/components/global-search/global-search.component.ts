/**
 * Global Search Component
 *
 * Provides instant search across patients, measures, and features
 * Activated via Ctrl+K keyboard shortcut or toolbar button
 */

import { Component, OnInit, OnDestroy, HostListener, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PatientService } from '../../../services/patient.service';
import { MeasureService } from '../../../services/measure.service';
import { LoggerService } from '../../../services/logger.service';

export interface SearchResult {
  id: string;
  type: 'patient' | 'measure' | 'page' | 'action';
  title: string;
  subtitle?: string;
  description?: string;
  icon: string;
  route?: string[];
  queryParams?: any;
  action?: () => void;
  metadata?: Record<string, any>;
}

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatListModule,
    MatDividerModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './global-search.component.html',
  styleUrls: ['./global-search.component.scss'],
})
export class GlobalSearchComponent implements OnInit, OnDestroy {
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  searchQuery = '';
  searchResults: SearchResult[] = [];
  recentSearches: SearchResult[] = [];
  popularPages: SearchResult[] = [];
  loading = false;
  selectedIndex = 0;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();
  private get logger() {
    return this.loggerService.withContext('GlobalSearchComponent');
  }

  // Quick access pages
  private readonly QUICK_PAGES: SearchResult[] = [
    {
      id: 'dashboard',
      type: 'page',
      title: 'Dashboard',
      subtitle: 'Performance Overview',
      icon: 'dashboard',
      route: ['/dashboard'],
    },
    {
      id: 'patients',
      type: 'page',
      title: 'Patients',
      subtitle: 'Patient Management',
      icon: 'people',
      route: ['/patients'],
    },
    {
      id: 'evaluations',
      type: 'page',
      title: 'Evaluations',
      subtitle: 'Run Quality Measures',
      icon: 'assessment',
      route: ['/evaluations'],
    },
    {
      id: 'results',
      type: 'page',
      title: 'Results',
      subtitle: 'View Measure Results',
      icon: 'analytics',
      route: ['/results'],
    },
    {
      id: 'reports',
      type: 'page',
      title: 'Reports',
      subtitle: 'Quality Reports',
      icon: 'description',
      route: ['/reports'],
    },
    {
      id: 'measure-builder',
      type: 'page',
      title: 'Measure Builder',
      subtitle: 'Create Custom Measures',
      icon: 'build_circle',
      route: ['/measure-builder'],
    },
  ];

  constructor(
    public dialogRef: MatDialogRef<GlobalSearchComponent>,
    private router: Router,
    private patientService: PatientService,
    private measureService: MeasureService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    // Setup search debouncing
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((query) => {
        this.performSearch(query);
      });

    // Load recent searches from localStorage
    this.loadRecentSearches();

    // Set popular pages
    this.popularPages = this.QUICK_PAGES.slice(0, 4);

    // Focus search input after view init
    setTimeout(() => {
      this.searchInput?.nativeElement.focus();
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Handle keyboard shortcuts
   */
  @HostListener('document:keydown', ['$event'])
  handleKeyboard(event: KeyboardEvent): void {
    switch (event.key) {
      case 'Escape':
        this.close();
        break;
      case 'ArrowDown':
        event.preventDefault();
        this.navigateResults(1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.navigateResults(-1);
        break;
      case 'Enter':
        event.preventDefault();
        this.selectResult(this.selectedIndex);
        break;
    }
  }

  /**
   * Handle search input changes
   */
  onSearchChange(): void {
    if (this.searchQuery.trim()) {
      this.searchSubject.next(this.searchQuery.trim());
    } else {
      this.searchResults = [];
      this.selectedIndex = 0;
    }
  }

  /**
   * Perform search across patients, measures, and pages
   */
  private performSearch(query: string): void {
    this.loading = true;
    const results: SearchResult[] = [];

    // Search pages
    const pageResults = this.searchPages(query);
    results.push(...pageResults);

    // Search patients by name
    this.patientService.searchPatients({ name: query }).subscribe({
      next: (patients) => {
        const patientResults: SearchResult[] = patients.slice(0, 5).map((patient) => ({
          id: patient.id || '',
          type: 'patient' as const,
          title: this.getPatientName(patient),
          subtitle: `MRN: ${this.getPatientMRN(patient) || 'N/A'}`,
          description: `${patient.gender || 'Unknown'} • DOB: ${patient.birthDate || 'Unknown'}`,
          icon: 'person',
          route: ['/patients', patient.id],
          metadata: { patient },
        }));
        results.push(...patientResults);
        this.updateResults(results);
      },
      error: (err) => {
        this.logger.error('Error searching patients', err);
        this.updateResults(results);
      },
    });

    // Search measures
    this.measureService.searchMeasures(query).subscribe({
      next: (measures) => {
        const measureResults: SearchResult[] = measures.slice(0, 3).map((measure) => ({
          id: measure.id || '',
          type: 'measure' as const,
          title: measure.name,
          subtitle: 'Quality Measure',
          description: measure.description || '',
          icon: 'rule',
          route: ['/evaluations'],
          queryParams: { measureId: measure.id },
          metadata: { measure },
        }));
        results.push(...measureResults);
        this.updateResults(results);
      },
      error: (err) => {
        this.logger.error('Error searching measures', err);
        this.updateResults(results);
      },
    });
  }

  /**
   * Get patient name from FHIR Patient resource
   */
  private getPatientName(patient: any): string {
    if (!patient.name || patient.name.length === 0) {
      return 'Unknown Patient';
    }
    const name = patient.name[0];
    const family = name.family || '';
    const given = name.given ? name.given.join(' ') : '';
    return `${family}, ${given}`.trim();
  }

  /**
   * Get patient MRN from FHIR Patient resource
   */
  private getPatientMRN(patient: any): string | null {
    if (!patient.identifier || patient.identifier.length === 0) {
      return null;
    }
    // Find MRN identifier
    const mrnIdentifier = patient.identifier.find((id: any) =>
      id.type?.coding?.some((coding: any) => coding.code === 'MR')
    );
    return mrnIdentifier?.value || patient.identifier[0]?.value || null;
  }

  /**
   * Search through available pages
   */
  private searchPages(query: string): SearchResult[] {
    const lowerQuery = query.toLowerCase();
    return this.QUICK_PAGES.filter(
      (page) =>
        page.title.toLowerCase().includes(lowerQuery) ||
        page.subtitle?.toLowerCase().includes(lowerQuery)
    );
  }

  /**
   * Update search results and reset selection
   */
  private updateResults(results: SearchResult[]): void {
    this.searchResults = results;
    this.selectedIndex = 0;
    this.loading = false;
  }

  /**
   * Navigate through results with arrow keys
   */
  private navigateResults(direction: number): void {
    const maxIndex = this.getVisibleResults().length - 1;
    this.selectedIndex = Math.max(0, Math.min(this.selectedIndex + direction, maxIndex));
  }

  /**
   * Get visible results (search results or quick pages)
   */
  getVisibleResults(): SearchResult[] {
    return this.searchResults.length > 0 ? this.searchResults : this.popularPages;
  }

  /**
   * Select a search result
   */
  selectResult(index: number): void {
    const results = this.getVisibleResults();
    if (index >= 0 && index < results.length) {
      const result = results[index];
      this.navigateToResult(result);
    }
  }

  /**
   * Navigate to selected result
   */
  navigateToResult(result: SearchResult): void {
    // Save to recent searches
    this.saveRecentSearch(result);

    // Execute action or navigate
    if (result.action) {
      result.action();
    } else if (result.route) {
      this.router.navigate(result.route, { queryParams: result.queryParams });
    }

    // Close dialog
    this.close();
  }

  /**
   * Clear search
   */
  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedIndex = 0;
    this.searchInput.nativeElement.focus();
  }

  /**
   * Close dialog
   */
  close(): void {
    this.dialogRef.close();
  }

  /**
   * Load recent searches from localStorage
   */
  private loadRecentSearches(): void {
    try {
      const stored = localStorage.getItem('recentSearches');
      if (stored) {
        this.recentSearches = JSON.parse(stored).slice(0, 5);
      }
    } catch (err) {
      this.logger.error('Error loading recent searches', err);
    }
  }

  /**
   * Save recent search to localStorage
   */
  private saveRecentSearch(result: SearchResult): void {
    try {
      // Remove if already exists
      this.recentSearches = this.recentSearches.filter((r) => r.id !== result.id);

      // Add to beginning
      this.recentSearches.unshift(result);

      // Keep only 10 most recent
      this.recentSearches = this.recentSearches.slice(0, 10);

      // Save to localStorage
      localStorage.setItem('recentSearches', JSON.stringify(this.recentSearches));
    } catch (err) {
      this.logger.error('Error saving recent search', err);
    }
  }

  /**
   * Get result type icon color
   */
  getResultIconColor(type: string): string {
    switch (type) {
      case 'patient': return '#1976d2';
      case 'measure': return '#388e3c';
      case 'page': return '#f57c00';
      case 'action': return '#7b1fa2';
      default: return '#757575';
    }
  }

  /**
   * Get result type label
   */
  getResultTypeLabel(type: string): string {
    switch (type) {
      case 'patient': return 'Patient';
      case 'measure': return 'Measure';
      case 'page': return 'Page';
      case 'action': return 'Action';
      default: return '';
    }
  }
}
