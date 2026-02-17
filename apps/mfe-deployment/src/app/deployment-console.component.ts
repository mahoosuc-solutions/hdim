import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, timer, firstValueFrom } from 'rxjs';
import { switchMap } from 'rxjs/operators';

type DeploymentStatus = {
  timestamp?: string;
  services?: Array<{ name: string; state: string; health?: string; ports?: string }>;
  seedingTail?: string[];
  lastCommand?: { action: string; exitCode: number; durationMs: number; outputTail?: string[] };
  error?: string;
};

@Component({
  selector: 'app-deployment-console',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './deployment-console.component.html',
  styleUrls: ['./deployment-console.component.scss'],
})
export class DeploymentConsoleComponent implements OnInit, OnDestroy {
  opsBaseUrl = (window as any).__HDIM_OPS_BASE_URL || 'http://localhost:4710';
  deploymentMode: 'onprem' | 'cloud' = 'onprem';
  patientCount = 1200;
  batchSize = 50;
  maxConcurrentRequests = 2;
  enableQualityMeasures = false;
  javaXmx = '2g';
  javaXms = '1g';
  stackProfile: 'demo' | 'core' = 'demo';
  tenantId = 'acme-health';
  status: DeploymentStatus = {};
  statusError = '';
  commandBusy = false;
  generatedOverride = '';
  private statusSub?: Subscription;

  private http = inject(HttpClient);

  ngOnInit(): void {
    this.refreshOverride();
    this.statusSub = timer(0, 5000)
      .pipe(switchMap(() => this.http.get<DeploymentStatus>(`${this.opsBaseUrl}/ops/status`)))
      .subscribe({
        next: (status) => {
          this.status = status;
          this.statusError = '';
        },
        error: (err) => {
          this.statusError = err?.message || 'Unable to reach ops service';
        },
      });
  }

  ngOnDestroy(): void {
    this.statusSub?.unsubscribe();
  }

  refreshOverride(): void {
    this.generatedOverride = [
      'services:',
      '  demo-seeding-service:',
      '    environment:',
      `      JAVA_OPTS: "-Xmx${this.javaXmx} -Xms${this.javaXms}"`,
      `      DEMO_QUALITY_MEASURES_ENABLED: "${this.enableQualityMeasures}"`,
      `      DEMO_GENERATION_DEFAULT_PATIENT_COUNT: "${this.patientCount}"`,
      `      DEMO_PERFORMANCE_MAX_CONCURRENT_REQUESTS: "${this.maxConcurrentRequests}"`,
      `      DEMO_GENERATION_BATCH_SIZE: "${this.batchSize}"`,
      '    deploy:',
      '      resources:',
      '        limits:',
      '          memory: 2048M',
      '',
    ].join('\n');
  }

  async runAction(action: 'start' | 'seed' | 'validate' | 'capture-logs' | 'stop'): Promise<void> {
    if (this.commandBusy) {
      return;
    }
    this.commandBusy = true;
    try {
      const payload = {
        action,
        tenantId: this.tenantId,
        patientCount: this.patientCount,
        batchSize: this.batchSize,
        maxConcurrentRequests: this.maxConcurrentRequests,
        enableQualityMeasures: this.enableQualityMeasures,
        javaXmx: this.javaXmx,
        javaXms: this.javaXms,
        stackProfile: this.stackProfile,
      };
      this.status = await firstValueFrom(
        this.http.post<DeploymentStatus>(`${this.opsBaseUrl}/ops/command`, payload)
      );
    } catch (err: any) {
      this.statusError = err?.message || 'Command failed';
    } finally {
      this.commandBusy = false;
    }
  }
}
