import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';

export interface DemoScenario {
  id: string;
  name: string;
  description: string;
  duration: number;
  valueProp: string;
}

export interface DemoStatus {
  isInitialized: boolean;
  activeScenario: DemoScenario | null;
  patientCount: number;
  careGapCount: number;
  lastReset: string | null;
}

export interface DemoTooltip {
  id: string;
  selector: string;
  content: string;
  position: 'top' | 'bottom' | 'left' | 'right';
}

/**
 * Demo Mode Service
 *
 * Manages demo mode state for the clinical portal.
 * Demo mode is activated via URL parameter: ?demo=true
 *
 * Features:
 * - Toggle demo mode on/off
 * - Load demo scenarios
 * - Show demo-specific tooltips
 * - Recording timer
 * - Scenario information display
 */
@Injectable({
  providedIn: 'root',
})
export class DemoModeService {
  private readonly DEMO_API_URL = environment.apiUrl + '/demo-seeding';
  private readonly DEMO_MODE_KEY = 'healthdata-demo-mode';

  // Signals for reactive state
  public readonly isDemoMode = signal<boolean>(false);
  public readonly activeScenario = signal<DemoScenario | null>(null);
  public readonly status = signal<DemoStatus | null>(null);
  public readonly isLoading = signal<boolean>(false);
  public readonly error = signal<string | null>(null);

  // Recording state
  public readonly isRecording = signal<boolean>(false);
  public readonly recordingStartTime = signal<number | null>(null);
  public readonly recordingDuration = signal<number>(0);
  private recordingInterval: ReturnType<typeof setInterval> | null = null;

  // Tooltips
  public readonly tooltips = signal<DemoTooltip[]>([]);
  public readonly showTooltips = signal<boolean>(true);

  // Computed values
  public readonly scenarioName = computed(() => this.activeScenario()?.name ?? 'No scenario loaded');
  public readonly formattedRecordingTime = computed(() => {
    const seconds = this.recordingDuration();
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Check URL for demo parameter on init
    this.checkUrlForDemoMode();
  }

  /**
   * Check URL query parameter for demo mode activation
   */
  private checkUrlForDemoMode(): void {
    // Parse URL directly since route might not be ready
    const urlParams = new URLSearchParams(window.location.search);
    const demoParam = urlParams.get('demo');
    if (demoParam === 'true') {
      this.enableDemoMode();
    } else {
      // Check localStorage for persisted demo mode
      const saved = localStorage.getItem(this.DEMO_MODE_KEY);
      if (saved === 'true') {
        this.enableDemoMode();
      }
    }
  }

  /**
   * Enable demo mode
   */
  public enableDemoMode(): void {
    this.isDemoMode.set(true);
    localStorage.setItem(this.DEMO_MODE_KEY, 'true');
    this.loadStatus();
    this.loadTooltips();
    console.log('[Demo Mode] Enabled');
  }

  /**
   * Disable demo mode
   */
  public disableDemoMode(): void {
    this.isDemoMode.set(false);
    localStorage.removeItem(this.DEMO_MODE_KEY);
    this.stopRecording();
    this.tooltips.set([]);
    console.log('[Demo Mode] Disabled');

    // Remove demo parameter from URL
    const url = new URL(window.location.href);
    url.searchParams.delete('demo');
    window.history.replaceState({}, '', url.toString());
  }

  /**
   * Toggle demo mode
   */
  public toggleDemoMode(): void {
    if (this.isDemoMode()) {
      this.disableDemoMode();
    } else {
      this.enableDemoMode();
    }
  }

  /**
   * Load demo status from backend
   */
  public async loadStatus(): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);

    try {
      const status = await this.http
        .get<DemoStatus>(`${this.DEMO_API_URL}/api/v1/demo/status`)
        .toPromise();
      this.status.set(status || null);
      if (status?.activeScenario) {
        this.activeScenario.set(status.activeScenario);
      }
    } catch (err: unknown) {
      console.warn('[Demo Mode] Could not load status:', err);
      // Demo mode can still work without backend connection
      this.error.set('Demo backend not available');
    } finally {
      this.isLoading.set(false);
    }
  }

  /**
   * Load available scenarios
   */
  public async loadScenarios(): Promise<DemoScenario[]> {
    try {
      const scenarios = await this.http
        .get<DemoScenario[]>(`${this.DEMO_API_URL}/api/v1/demo/scenarios`)
        .toPromise();
      return scenarios || [];
    } catch (err) {
      console.error('[Demo Mode] Could not load scenarios:', err);
      return [];
    }
  }

  /**
   * Load a specific scenario
   */
  public async loadScenario(scenarioId: string): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);

    try {
      const result = await this.http
        .post<{ scenario: DemoScenario }>(`${this.DEMO_API_URL}/api/v1/demo/scenarios/${scenarioId}/load`, {})
        .toPromise();
      if (result?.scenario) {
        this.activeScenario.set(result.scenario);
      }
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load scenario';
      this.error.set(message);
      throw err;
    } finally {
      this.isLoading.set(false);
    }
  }

  /**
   * Reset demo data
   */
  public async resetDemo(): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);

    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/reset`, {})
        .toPromise();
      this.activeScenario.set(null);
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to reset demo';
      this.error.set(message);
      throw err;
    } finally {
      this.isLoading.set(false);
    }
  }

  /**
   * Start recording timer
   */
  public startRecording(): void {
    if (this.isRecording()) return;

    this.isRecording.set(true);
    this.recordingStartTime.set(Date.now());
    this.recordingDuration.set(0);

    this.recordingInterval = setInterval(() => {
      const start = this.recordingStartTime();
      if (start) {
        this.recordingDuration.set(Math.floor((Date.now() - start) / 1000));
      }
    }, 1000);

    console.log('[Demo Mode] Recording started');
  }

  /**
   * Stop recording timer
   */
  public stopRecording(): void {
    if (!this.isRecording()) return;

    this.isRecording.set(false);
    if (this.recordingInterval) {
      clearInterval(this.recordingInterval);
      this.recordingInterval = null;
    }

    const duration = this.recordingDuration();
    console.log(`[Demo Mode] Recording stopped. Duration: ${this.formattedRecordingTime()}`);
  }

  /**
   * Reset recording timer
   */
  public resetRecording(): void {
    this.stopRecording();
    this.recordingDuration.set(0);
    this.recordingStartTime.set(null);
  }

  /**
   * Load demo tooltips for current page
   */
  private loadTooltips(): void {
    // Default tooltips for common elements
    const defaultTooltips: DemoTooltip[] = [
      {
        id: 'patient-count',
        selector: '[data-demo-tooltip="patient-count"]',
        content: 'Total patients in the evaluation population',
        position: 'bottom',
      },
      {
        id: 'care-gaps',
        selector: '[data-demo-tooltip="care-gaps"]',
        content: 'Open care gaps requiring intervention',
        position: 'bottom',
      },
      {
        id: 'hedis-rate',
        selector: '[data-demo-tooltip="hedis-rate"]',
        content: 'HEDIS compliance rate for this measure',
        position: 'bottom',
      },
      {
        id: 'evaluation-time',
        selector: '[data-demo-tooltip="evaluation-time"]',
        content: 'Time to evaluate all patients',
        position: 'bottom',
      },
    ];
    this.tooltips.set(defaultTooltips);
  }

  /**
   * Toggle tooltip visibility
   */
  public toggleTooltips(): void {
    this.showTooltips.set(!this.showTooltips());
  }

  /**
   * Create a snapshot for demo recording
   */
  public async createSnapshot(name: string): Promise<void> {
    this.isLoading.set(true);
    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/snapshots`, {
          name,
          description: `Demo recording snapshot: ${name}`,
        })
        .toPromise();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create snapshot';
      this.error.set(message);
      throw err;
    } finally {
      this.isLoading.set(false);
    }
  }

  /**
   * Restore from a snapshot
   */
  public async restoreSnapshot(snapshotId: string): Promise<void> {
    this.isLoading.set(true);
    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/snapshots/${snapshotId}/restore`, {})
        .toPromise();
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to restore snapshot';
      this.error.set(message);
      throw err;
    } finally {
      this.isLoading.set(false);
    }
  }
}
