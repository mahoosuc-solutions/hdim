import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { LoggerService } from '../../services/logger.service';
import { API_CONFIG } from '../../config/api.config';
import { environment } from '../../../environments/environment';

export interface DemoScenario {
  id: string;
  name: string;
  displayName: string;
  description: string;
  patientCount: number;
  tenantId: string;
  estimatedLoadTimeSeconds: number | null;
}

export interface DemoStatus {
  ready: boolean;
  scenarioCount: number;
  templateCount: number;
  currentSessionId: string | null;
  currentScenario: string | null;
  sessionStatus: string | null;
}

export interface LoadScenarioResponse {
  scenarioName: string;
  sessionId: string | null;
  patientCount: number;
  careGapCount: number;
  loadTimeMs: number;
  success: boolean;
  errorMessage?: string | null;
}

export interface DemoProgress {
  sessionId: string;
  scenarioName: string;
  tenantId: string;
  stage: string;
  progressPercent: number;
  patientsGenerated?: number | null;
  patientsPersisted?: number | null;
  careGapsCreated?: number | null;
  measuresSeeded?: number | null;
  message?: string | null;
  updatedAt?: string | null;
  cancelRequested?: boolean;
}

export interface DemoTooltip {
  id: string;
  selector: string;
  content: string;
  position: 'top' | 'bottom' | 'left' | 'right';
}

export interface DemoStoryboardStep {
  id: string;
  title: string;
  route?: string;
  narration?: string;
  popups?: DemoTooltip[];
  highlightSelectors?: string[];
}

export interface DemoStoryboard {
  version?: string;
  title?: string;
  steps: DemoStoryboardStep[];
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
  private readonly DEMO_API_URL = this.resolveApiBaseUrl() + '/demo';
  private readonly DEMO_MODE_KEY = 'healthdata-demo-mode';
  private readonly STORYBOARD_URL = '/demo/storyboard.json';

  // Signals for reactive state
  public readonly isDemoMode = signal<boolean>(false);
  public readonly activeScenario = signal<DemoScenario | null>(null);
  public readonly status = signal<DemoStatus | null>(null);
  public readonly scenarios = signal<DemoScenario[]>([]);
  public readonly lastLoadResult = signal<LoadScenarioResponse | null>(null);
  public readonly progress = signal<DemoProgress | null>(null);
  public readonly isLoading = signal<boolean>(false);
  public readonly error = signal<string | null>(null);
  public readonly storyboardSteps = signal<DemoStoryboardStep[]>([]);
  public readonly activeStoryboardStep = signal<DemoStoryboardStep | null>(null);
  public readonly storyboardEnabled = signal<boolean>(false);
  public readonly storyboardConnected = signal<boolean>(false);
  public readonly storyboardError = signal<string | null>(null);

  // Backend availability tracking - prevents repeated failed API calls
  private demoBackendAvailable: boolean | null = null;
  private backendCheckInProgress = false;
  private progressInterval: ReturnType<typeof setInterval> | null = null;
  private storyboardSocket: WebSocket | null = null;

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
  });  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private logger: LoggerService
  ) {
    // Check URL for demo parameter on init
    this.checkUrlForDemoMode();
    this.checkUrlForStoryboard();
  }

  private resolveApiBaseUrl(): string {
    if (API_CONFIG.USE_API_GATEWAY && API_CONFIG.API_GATEWAY_URL) {
      return API_CONFIG.API_GATEWAY_URL;
    }
    const browserOrigin = typeof window !== 'undefined' ? window.location.origin : '';
    if (environment.apiConfig.useApiGateway) {
      return environment.apiConfig.apiGatewayUrl || browserOrigin;
    }
    return browserOrigin;
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

  private checkUrlForStoryboard(): void {
    const urlParams = new URLSearchParams(window.location.search);
    const storyboardParam = urlParams.get('storyboard');
    if (storyboardParam === 'true' || storyboardParam === '1') {
      this.storyboardEnabled.set(true);
      this.loadStoryboard();
      const wsUrl = urlParams.get('storyboardWs');
      if (wsUrl) {
        this.connectStoryboardSocket(wsUrl);
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
    if (this.storyboardEnabled()) {
      this.loadStoryboard();
    }
    this.logger.info('[Demo Mode] Enabled');
  }

  /**
   * Disable demo mode
   */
  public disableDemoMode(): void {
    this.isDemoMode.set(false);
    localStorage.removeItem(this.DEMO_MODE_KEY);
    this.stopRecording();
    this.tooltips.set([]);
    this.activeStoryboardStep.set(null);
    this.storyboardConnected.set(false);
    this.storyboardError.set(null);
    this.disconnectStoryboardSocket();
    this.logger.info('[Demo Mode] Disabled');

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

  public async loadStoryboard(): Promise<void> {
    try {
      const storyboard = await this.http.get<DemoStoryboard>(this.STORYBOARD_URL).toPromise();
      const steps = storyboard?.steps ?? [];
      this.storyboardSteps.set(steps);
      if (steps.length && !this.activeStoryboardStep()) {
        this.applyStoryboardStep(steps[0]);
      }
      this.storyboardError.set(null);
    } catch (err) {
      this.storyboardError.set('Storyboard unavailable');
      this.storyboardSteps.set([]);
    }
  }

  public setStoryboardStep(stepId: string): void {
    const step = this.storyboardSteps().find((item) => item.id === stepId) ?? null;
    this.applyStoryboardStep(step);
  }

  public setStoryboardStepIndex(stepIndex: number): void {
    const steps = this.storyboardSteps();
    if (!steps.length) {
      return;
    }
    const step = steps[Math.max(0, Math.min(stepIndex, steps.length - 1))] ?? null;
    this.applyStoryboardStep(step);
  }

  private applyStoryboardStep(step: DemoStoryboardStep | null): void {
    this.activeStoryboardStep.set(step);
    if (step?.popups?.length) {
      this.tooltips.set(step.popups);
      this.showTooltips.set(true);
    } else {
      this.tooltips.set([]);
    }
  }

  private connectStoryboardSocket(wsUrl: string): void {
    if (this.storyboardSocket) {
      return;
    }
    try {
      this.storyboardSocket = new WebSocket(wsUrl);
      this.storyboardSocket.onopen = () => {
        this.storyboardConnected.set(true);
        this.storyboardError.set(null);
        const payload = {
          type: 'storyboard.ready',
          steps: this.storyboardSteps().map((step) => step.id),
        };
        this.storyboardSocket?.send(JSON.stringify(payload));
      };
      this.storyboardSocket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          if (message.type === 'storyboard.step' && message.stepId) {
            this.setStoryboardStep(message.stepId);
          } else if (message.type === 'storyboard.step' && typeof message.stepIndex === 'number') {
            this.setStoryboardStepIndex(message.stepIndex);
          }
        } catch (err) {
          this.logger.warn('[Demo Mode] Failed to parse storyboard message', err);
        }
      };
      this.storyboardSocket.onerror = () => {
        this.storyboardConnected.set(false);
        this.storyboardError.set('Storyboard socket error');
      };
      this.storyboardSocket.onclose = () => {
        this.storyboardConnected.set(false);
      };
    } catch (err) {
      this.storyboardError.set('Storyboard socket unavailable');
    }
  }

  private disconnectStoryboardSocket(): void {
    if (this.storyboardSocket) {
      this.storyboardSocket.close();
      this.storyboardSocket = null;
    }
  }

  /**
   * Check if demo backend is available
   * Uses a quick health check to avoid 500 error spam in console
   */
  private async checkBackendAvailability(): Promise<boolean> {
    // Return cached result if already checked
    if (this.demoBackendAvailable !== null) {
      return this.demoBackendAvailable;
    }

    // Prevent concurrent checks
    if (this.backendCheckInProgress) {
      return false;
    }

    this.backendCheckInProgress = true;

    try {
      // Quick check using demo status endpoint to avoid actuator CORS issues
      const response = await this.http
        .get(`${this.DEMO_API_URL}/api/v1/demo/status`, {
          observe: 'response',
        })
        .toPromise();
      this.demoBackendAvailable = response?.status === 200;
    } catch {
      // Backend not available - this is expected when demo service isn't running
      this.demoBackendAvailable = false;
      this.logger.info('[Demo Mode] Demo backend not available - using local-only mode');
    } finally {
      this.backendCheckInProgress = false;
    }

    return this.demoBackendAvailable;
  }

  /**
   * Load demo status from backend
   */
  public async loadStatus(): Promise<void> {
    // Skip if backend not available
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available - recording/tooltips still work');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    try {
      const status = await this.http
        .get<DemoStatus>(`${this.DEMO_API_URL}/api/v1/demo/status`)
        .toPromise();
      this.status.set(status || null);
      if (status?.currentScenario) {
        const scenarioMatch = this.scenarios().find(
          (scenario) => scenario.name === status.currentScenario
        );
        this.activeScenario.set(
          scenarioMatch || {
            id: status.currentSessionId || status.currentScenario,
            name: status.currentScenario,
            displayName: status.currentScenario,
            description: '',
            patientCount: 0,
            tenantId: '',
            estimatedLoadTimeSeconds: null,
          }
        );
      }
    } catch (err: unknown) {
      this.logger.warn('[Demo Mode] Could not load status:', err);
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
    // Skip if backend not available
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.scenarios.set([]);
      return [];
    }

    try {
      const scenarios = await this.http
        .get<DemoScenario[]>(`${this.DEMO_API_URL}/api/v1/demo/scenarios`)
        .toPromise();
      const resolved = scenarios || [];
      this.scenarios.set(resolved);
      return resolved;
    } catch (err) {
      this.logger.error('[Demo Mode] Could not load scenarios:', err);
      this.scenarios.set([]);
      return [];
    }
  }

  /**
   * Load a specific scenario
   */
  public async loadScenario(scenarioId: string): Promise<void> {
    // Requires backend
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

    this.isLoading.set(true);
    this.error.set(null);
    this.lastLoadResult.set(null);
    this.progress.set(null);
    this.startProgressPolling();
    const statusInterval = setInterval(() => {
      this.loadStatus();
    }, 1000);

    try {
      const result = await this.http
        .post<LoadScenarioResponse>(`${this.DEMO_API_URL}/api/v1/demo/scenarios/${encodeURIComponent(scenarioId)}`, {})
        .toPromise();
      if (result) {
        this.lastLoadResult.set(result);
        const scenarioMatch = this.scenarios().find((scenario) => scenario.name === result.scenarioName);
        this.activeScenario.set(
          scenarioMatch || {
            id: result.sessionId || result.scenarioName,
            name: result.scenarioName,
            displayName: result.scenarioName,
            description: '',
            patientCount: result.patientCount,
            tenantId: '',
            estimatedLoadTimeSeconds: null,
          }
        );
      }
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load scenario';
      this.error.set(message);
      throw err;
    } finally {
      clearInterval(statusInterval);
      await this.pollProgress();
      this.stopProgressPolling();
      this.isLoading.set(false);
    }
  }

  /**
   * Reset demo data
   */
  public async resetDemo(): Promise<void> {
    // Requires backend
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

    this.isLoading.set(true);
    this.error.set(null);

    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/reset`, {})
        .toPromise();
      this.activeScenario.set(null);
      this.progress.set(null);
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to reset demo';
      this.error.set(message);
      throw err;
    } finally {
      this.isLoading.set(false);
    }
  }

  public async resetCurrentTenant(): Promise<void> {
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

    this.isLoading.set(true);
    this.error.set(null);

    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/reset/current-tenant`, {})
        .toPromise();
      this.lastLoadResult.set(null);
      this.progress.set(null);
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to reset current tenant';
      this.error.set(message);
      throw err;
    } finally {
      this.isLoading.set(false);
    }
  }

  private startProgressPolling(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
    this.progressInterval = setInterval(() => {
      this.pollProgress();
    }, 1000);
  }

  private stopProgressPolling(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  private async pollProgress(): Promise<void> {
    try {
      const progress = await this.http
        .get<DemoProgress>(`${this.DEMO_API_URL}/api/v1/demo/sessions/current/progress`)
        .toPromise();
      if (progress) {
        this.progress.set(progress);
        if (progress.stage === 'COMPLETE' || progress.stage === 'FAILED' || progress.stage === 'CANCELLED') {
          this.stopProgressPolling();
        }
      }
    } catch {
      // Progress endpoint may not exist or no active session yet.
    }
  }

  public async cancelCurrentLoad(): Promise<void> {
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/sessions/current/cancel`, {})
        .toPromise();
      this.error.set(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to cancel scenario load';
      this.error.set(message);
      throw err;
    }
  }

  public async stopCurrentSession(): Promise<void> {
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

    try {
      await this.http
        .post(`${this.DEMO_API_URL}/api/v1/demo/sessions/current/stop`, {})
        .toPromise();
      this.progress.set(null);
      await this.loadStatus();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to stop demo session';
      this.error.set(message);
      throw err;
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

    this.logger.info('[Demo Mode] Recording started');
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
    this.logger.info(`[Demo Mode] Recording stopped. Duration: ${this.formattedRecordingTime()}`);
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
    // Requires backend
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

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
    // Requires backend
    const isAvailable = await this.checkBackendAvailability();
    if (!isAvailable) {
      this.error.set('Demo backend not available');
      throw new Error('Demo backend not available');
    }

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
