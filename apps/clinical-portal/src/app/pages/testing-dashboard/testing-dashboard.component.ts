import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { DemoModeService, DemoScenario } from '../../demo-mode/services/demo-mode.service';
import { TestingService } from '../../services/testing.service';
import { ErrorValidationService, ErrorSummary } from '../../services/error-validation.service';
import { COMPLIANCE_CONFIG } from '../../config/compliance.config';
import { LoggerService } from '../../services/logger.service';

export interface TestResult {
  success: boolean;
  message: string;
  timestamp: Date;
  serviceName?: string;
  endpoint?: string;
  method?: string;
  responseTime?: number;
}

/**
 * Testing Dashboard Component
 * 
 * Comprehensive testing dashboard for development and automated testing.
 * Provides access to demo scenarios, API testing, data management, and service health checks.
 */
@Component({
  selector: 'app-testing-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatExpansionModule,
    MatDividerModule,
    MatSnackBarModule,
    MatMenuModule,
  ],
  templateUrl: './testing-dashboard.component.html',
  styleUrl: './testing-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TestingDashboardComponent implements OnInit, OnDestroy {
  // Demo scenarios
  scenarios: DemoScenario[] = [];
  loadingScenarios = false;
  loadingScenarioId: string | null = null;

  // Service health
  serviceHealth: Map<string, { status: 'healthy' | 'unhealthy' | 'unknown'; lastChecked: Date | null }> = new Map();
  checkingHealth = false;

  // Test execution status
  testResults: Map<string, TestResult> = new Map();
  private readonly STORAGE_KEY = 'hdim-test-results';

  // Compliance validation
  errorSummary: ErrorSummary | null = null;
  complianceConfig = COMPLIANCE_CONFIG;

  private destroy$ = new Subject<void>();
  private get logger() {
    return this.loggerService.withContext('TestingDashboardComponent');
  }

  constructor(
    private demoModeService: DemoModeService,
    private testingService: TestingService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private errorValidationService: ErrorValidationService,
    private router: Router,
    private loggerService: LoggerService
  ) {
    // Initialize service health map
    this.serviceHealth.set('patient-service', { status: 'unknown', lastChecked: null });
    this.serviceHealth.set('care-gap-service', { status: 'unknown', lastChecked: null });
    this.serviceHealth.set('quality-measure-service', { status: 'unknown', lastChecked: null });
    this.serviceHealth.set('fhir-service', { status: 'unknown', lastChecked: null });
    this.serviceHealth.set('demo-seeding-service', { status: 'unknown', lastChecked: null });
    this.serviceHealth.set('gateway-service', { status: 'unknown', lastChecked: null });
  }

  ngOnInit(): void {
    this.loadTestResultsFromStorage();
    this.loadScenarios();
    this.checkAllServiceHealth();
    this.loadErrorSummary();
  }

  /**
   * Load error summary for compliance validation
   */
  loadErrorSummary(): void {
    this.errorSummary = this.errorValidationService.getErrorSummary();
    this.cdr.markForCheck();
  }

  /**
   * Toggle fallbacks (runtime configuration for testing)
   */
  toggleFallbacks(): void {
    // Note: This modifies the config object directly for runtime testing
    // In production, this should be controlled via environment configuration
    COMPLIANCE_CONFIG.disableFallbacks = !COMPLIANCE_CONFIG.disableFallbacks;
    this.complianceConfig = { ...COMPLIANCE_CONFIG };
    this.showMessage(
      `Fallbacks ${COMPLIANCE_CONFIG.disableFallbacks ? 'disabled' : 'enabled'}`,
      'info'
    );
    this.cdr.markForCheck();
  }

  /**
   * Navigate to compliance dashboard
   */
  navigateToCompliance(): void {
    this.router.navigate(['/compliance']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load available demo scenarios
   */
  async loadScenarios(): Promise<void> {
    this.loadingScenarios = true;
    try {
      this.scenarios = await this.demoModeService.loadScenarios();
    } catch (error) {
      this.logger.error('Failed to load scenarios', error);
      this.showMessage('Failed to load scenarios', 'error');
    } finally {
      this.loadingScenarios = false;
    }
  }

  /**
   * Load a demo scenario
   */
  async loadScenario(scenario: DemoScenario): Promise<void> {
    this.loadingScenarioId = scenario.id;
    try {
      // loadScenario returns a Promise, not an Observable
      await this.demoModeService.loadScenario(scenario.name);
      this.showMessage(`Scenario "${scenario.displayName}" loading started`, 'success');
      this.recordTestResult(`scenario-${scenario.name}`, true, `Scenario "${scenario.displayName}" load initiated`, {
        serviceName: 'demo-seeding-service',
      });
    } catch (error: any) {
      const errorMsg = error?.message || 'Failed to load scenario';
      this.showMessage(errorMsg, 'error');
      this.recordTestResult(`scenario-${scenario.name}`, false, errorMsg);
    } finally {
      this.loadingScenarioId = null;
    }
  }

  /**
   * Check health of all services
   */
  async checkAllServiceHealth(): Promise<void> {
    this.checkingHealth = true;
    try {
      const healthChecks = await firstValueFrom(this.testingService.checkAllServiceHealth());
      healthChecks.forEach((result, serviceName) => {
        this.serviceHealth.set(serviceName, {
          status: result.healthy ? 'healthy' : 'unhealthy',
          lastChecked: new Date(),
        });
      });
    } catch (error) {
      this.logger.error('Failed to check service health', error);
      this.showMessage('Failed to check service health', 'error');
    } finally {
      this.checkingHealth = false;
    }
  }

  /**
   * Check health of a specific service
   */
  async checkServiceHealth(serviceName: string): Promise<void> {
    try {
      const result = await firstValueFrom(this.testingService.checkServiceHealth(serviceName));
      this.serviceHealth.set(serviceName, {
        status: result.healthy ? 'healthy' : 'unhealthy',
        lastChecked: new Date(),
      });
      const message = result.healthy 
        ? `${serviceName} is healthy (${result.responseTime}ms)`
        : `${serviceName} is unhealthy: ${result.error || 'Unknown error'}`;
      this.showMessage(message, result.healthy ? 'success' : 'error');
      this.cdr.markForCheck();
    } catch (error) {
      this.serviceHealth.set(serviceName, {
        status: 'unhealthy',
        lastChecked: new Date(),
      });
      this.showMessage(`Failed to check ${serviceName}`, 'error');
    }
  }

  /**
   * Seed test data
   */
  async seedTestData(): Promise<void> {
    try {
      await firstValueFrom(this.testingService.seedTestData());
      this.showMessage('Test data seeded successfully', 'success');
      this.recordTestResult('seed-data', true, 'Test data seeded');
    } catch (error: any) {
      const errorMsg = error?.message || 'Failed to seed test data';
      this.showMessage(errorMsg, 'error');
      this.recordTestResult('seed-data', false, errorMsg);
    }
  }

  /**
   * Validate test data
   */
  async validateTestData(): Promise<void> {
    try {
      const result = await firstValueFrom(this.testingService.validateTestData());
      if (result.valid) {
        this.showMessage(`Validation passed: ${result.message}`, 'success');
        this.recordTestResult('validate-data', true, result.message);
      } else {
        this.showMessage(`Validation failed: ${result.message}`, 'error');
        this.recordTestResult('validate-data', false, result.message);
      }
    } catch (error: any) {
      const errorMsg = error?.message || 'Failed to validate test data';
      this.showMessage(errorMsg, 'error');
      this.recordTestResult('validate-data', false, errorMsg);
    }
  }

  /**
   * Reset test data
   */
  async resetTestData(): Promise<void> {
    if (!confirm('Are you sure you want to reset all test data? This action cannot be undone.')) {
      return;
    }
    try {
      await firstValueFrom(this.testingService.resetTestData());
      this.showMessage('Test data reset successfully', 'success');
      this.recordTestResult('reset-data', true, 'Test data reset');
    } catch (error: any) {
      const errorMsg = error?.message || 'Failed to reset test data';
      this.showMessage(errorMsg, 'error');
      this.recordTestResult('reset-data', false, errorMsg);
    }
  }

  /**
   * Test an API endpoint
   */
  async testApiEndpoint(service: string, endpoint: string, method = 'GET'): Promise<void> {
    const testId = `api-${service}-${endpoint.replace(/[^a-zA-Z0-9]/g, '-')}`;
    try {
      const result = await firstValueFrom(this.testingService.testApiEndpoint(service, endpoint, method));
      if (result.success) {
        const message = `${service} ${endpoint}: HTTP ${result.status} - ${result.responseSummary}`;
        this.showMessage(`${service} ${endpoint} test passed`, 'success');
        this.recordTestResult(testId, true, message, {
          serviceName: result.serviceName,
          endpoint: result.endpoint,
          method: result.method,
          responseTime: result.responseTime,
        });
      } else {
        const message = `${service} ${endpoint}: ${result.error || 'Test failed'}`;
        this.showMessage(message, 'error');
        this.recordTestResult(testId, false, message, {
          serviceName: result.serviceName,
          endpoint: result.endpoint,
          method: result.method,
          responseTime: result.responseTime,
        });
      }
    } catch (error: any) {
      const errorMsg = error?.message || 'Test failed';
      this.showMessage(errorMsg, 'error');
      this.recordTestResult(testId, false, errorMsg);
    }
  }

  /**
   * Get service health status
   */
  getServiceHealthStatus(serviceName: string): 'healthy' | 'unhealthy' | 'unknown' {
    return this.serviceHealth.get(serviceName)?.status || 'unknown';
  }

  /**
   * Get service health icon
   */
  getServiceHealthIcon(serviceName: string): string {
    const status = this.getServiceHealthStatus(serviceName);
    switch (status) {
      case 'healthy':
        return 'check_circle';
      case 'unhealthy':
        return 'error';
      default:
        return 'help_outline';
    }
  }

  /**
   * Get service health color
   */
  getServiceHealthColor(serviceName: string): string {
    const status = this.getServiceHealthStatus(serviceName);
    switch (status) {
      case 'healthy':
        return 'primary';
      case 'unhealthy':
        return 'warn';
      default:
        return '';
    }
  }

  /**
   * Record test result
   */
  private recordTestResult(
    testId: string, 
    success: boolean, 
    message: string,
    additionalData?: Partial<TestResult>
  ): void {
    const result: TestResult = {
      success,
      message,
      timestamp: new Date(),
      ...additionalData,
    };
    this.testResults.set(testId, result);
    this.saveTestResultsToStorage();
    this.cdr.markForCheck();
  }

  /**
   * Load test results from localStorage
   */
  private loadTestResultsFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        const results = new Map<string, TestResult>();
        Object.entries(parsed).forEach(([key, value]: [string, any]) => {
          results.set(key, {
            ...value,
            timestamp: new Date(value.timestamp),
          });
        });
        this.testResults = results;
        this.cdr.markForCheck();
      }
    } catch (error) {
      this.logger.warn('Failed to load test results from storage', error);
    }
  }

  /**
   * Save test results to localStorage
   */
  private saveTestResultsToStorage(): void {
    try {
      const serializable = Object.fromEntries(this.testResults);
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(serializable));
    } catch (error) {
      this.logger.warn('Failed to save test results to storage', error);
    }
  }

  /**
   * Clear test results
   */
  clearTestResults(): void {
    if (confirm('Clear all test results? This action cannot be undone.')) {
      this.testResults.clear();
      localStorage.removeItem(this.STORAGE_KEY);
      this.showMessage('Test results cleared', 'success');
      this.cdr.markForCheck();
    }
  }

  /**
   * Export test results as JSON
   */
  exportTestResults(): void {
    try {
      const exportData = {
        exportDate: new Date().toISOString(),
        totalResults: this.testResults.size,
        results: Array.from(this.testResults.entries()).map(([testId, result]) => ({
          testId,
          ...result,
          timestamp: result.timestamp.toISOString(),
        })),
      };

      const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `test-results-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      this.showMessage('Test results exported successfully', 'success');
    } catch (error) {
      this.logger.error('Failed to export test results', error);
      this.showMessage('Failed to export test results', 'error');
    }
  }

  /**
   * Export test results as CSV
   */
  exportTestResultsAsCsv(): void {
    try {
      const headers = ['Test ID', 'Success', 'Message', 'Service', 'Endpoint', 'Method', 'Response Time (ms)', 'Timestamp'];
      const rows = Array.from(this.testResults.entries()).map(([testId, result]) => [
        testId,
        result.success ? 'Yes' : 'No',
        `"${result.message.replace(/"/g, '""')}"`, // Escape quotes for CSV
        result.serviceName || '',
        result.endpoint || '',
        result.method || '',
        result.responseTime?.toString() || '',
        result.timestamp.toISOString(),
      ]);

      const csvContent = [
        headers.join(','),
        ...rows.map(row => row.join(',')),
      ].join('\n');

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `test-results-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      this.showMessage('Test results exported as CSV successfully', 'success');
    } catch (error) {
      this.logger.error('Failed to export test results as CSV', error);
      this.showMessage('Failed to export test results as CSV', 'error');
    }
  }

  /**
   * Get test result
   */
  getTestResult(testId: string): TestResult | null {
    return this.testResults.get(testId) || null;
  }

  /**
   * Get all test results as array
   */
  getAllTestResults(): Array<[string, TestResult]> {
    return Array.from(this.testResults.entries());
  }

  /**
   * Show snackbar message
   */
  private showMessage(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: type === 'error' ? 'error-snackbar' : type === 'success' ? 'success-snackbar' : 'info-snackbar',
    });
  }

  /**
   * Get API endpoint URL for display
   */
  getApiEndpointUrl(service: string, endpoint: string): string {
    const baseUrl = this.testingService.getServiceBaseUrl(service);
    return `${baseUrl}${endpoint}`;
  }

  /**
   * Check if scenario is currently loading
   */
  isScenarioLoading(scenarioId: string): boolean {
    return this.loadingScenarioId === scenarioId;
  }
}
