import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { AiAuditStreamService } from '../../services/ai-audit-stream.service';
import { LoggerService } from '../../services/logger.service';

/**
 * Real-time AI audit dashboard component.
 *
 * Displays:
 * - Live stream of AI decisions, config changes, and user actions
 * - Real-time metrics and analytics
 * - Natural language query interface
 * - Decision replay functionality
 */
@Component({
  selector: 'app-ai-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-audit-dashboard.component.html',
  styleUrls: ['./ai-audit-dashboard.component.scss']
})
export class AiAuditDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private logger!: ReturnType<LoggerService['withContext']>;

  // Natural language query
  naturalLanguageQuery = '';
  queryResults: any[] = [];
  queryLoading = false;
  queryError: string | null = null;

  // Real-time event streams
  aiDecisions: any[] = [];
  configChanges: any[] = [];
  userActions: any[] = [];

  // SSE connection status
  connectionStatus: 'connected' | 'disconnected' | 'connecting' = 'disconnected';

  // Analytics
  analytics = {
    totalAIDecisions: 0,
    averageConfidence: 0,
    acceptanceRate: 0,
    highRiskChanges: 0,
    pendingApprovals: 0
  };

  // Filters
  timeRange = 'last-24-hours';
  eventType = 'all';
  tenantId: string | null = null;

  // View mode
  activeTab: 'stream' | 'query' | 'analytics' | 'trail' = 'stream';

  constructor(
    private aiAuditStream: AiAuditStreamService,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('AiAuditDashboardComponent');
  }

  ngOnInit(): void {
    this.loadRealtimeEvents();
    this.loadAnalytics();
    this.subscribeToConnectionStatus();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.aiAuditStream.disconnect();
  }

  /**
   * Load real-time event streams using SSE.
   */
  loadRealtimeEvents(): void {
    this.logger.info('Starting SSE connection for real-time AI audit events');

    // Subscribe to SSE events
    this.aiAuditStream.events$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event) => {
          this.logger.debug('Received AI decision event', event.eventId);
          this.addDecisionToTimeline(event);
          this.updateAnalytics(event);
        },
        error: (error) => {
          this.logger.error('Error receiving SSE event', error);
        }
      });

    // Connect to the stream
    this.aiAuditStream.connect();
  }

  /**
   * Subscribe to connection status updates.
   */
  private subscribeToConnectionStatus(): void {
    this.aiAuditStream.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe((status) => {
        this.connectionStatus = status;
        this.logger.info('SSE connection status changed', status);
      });
  }

  /**
   * Add AI decision to the timeline (prepend to show latest first).
   */
  private addDecisionToTimeline(event: any): void {
    // Prepend to array to show latest events first
    this.aiDecisions.unshift(event);

    // Keep only last 100 events to avoid memory issues
    if (this.aiDecisions.length > 100) {
      this.aiDecisions = this.aiDecisions.slice(0, 100);
    }
  }

  /**
   * Update analytics based on new event.
   */
  private updateAnalytics(event: any): void {
    this.analytics.totalAIDecisions++;

    // Update average confidence score
    const totalConfidence = this.aiDecisions.reduce((sum, d) => sum + (d.confidenceScore || 0), 0);
    this.analytics.averageConfidence = totalConfidence / this.aiDecisions.length;

    // Update acceptance rate
    const acceptedDecisions = this.aiDecisions.filter(d => d.outcome === 'ACCEPTED').length;
    this.analytics.acceptanceRate = (acceptedDecisions / this.aiDecisions.length) * 100;
  }

  /**
   * Load analytics data.
   */
  loadAnalytics(): void {
    this.logger.info('Loading analytics');

    // Mock analytics data
    this.analytics = {
      totalAIDecisions: 127,
      averageConfidence: 0.87,
      acceptanceRate: 0.73,
      highRiskChanges: 3,
      pendingApprovals: 2
    };
  }

  /**
   * Manually reconnect to SSE stream (if connection is lost).
   */
  reconnectStream(): void {
    this.logger.info('Manual reconnect requested');
    this.aiAuditStream.disconnect();
    setTimeout(() => {
      this.aiAuditStream.connect();
    }, 1000);
  }

  /**
   * Execute natural language query.
   */
  async executeNaturalLanguageQuery(): Promise<void> {
    if (!this.naturalLanguageQuery.trim()) {
      return;
    }

    this.queryLoading = true;
    this.queryError = null;

    try {
      // TODO: Call NLQ API
      this.logger.info('Executing NLQ', { query: this.naturalLanguageQuery });

      // Mock response
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      this.queryResults = [
        {
          eventId: '423e4567-e89b-12d3-a456-426614174000',
          timestamp: new Date(),
          type: 'AI_DECISION',
          description: 'Pool size optimization recommendation'
        }
      ];

    } catch (error) {
      this.logger.error('NLQ error', error);
      this.queryError = 'Failed to execute query. Please try again.';
    } finally {
      this.queryLoading = false;
    }
  }

  /**
   * Switch active tab.
   */
  selectTab(tab: 'stream' | 'query' | 'analytics' | 'trail'): void {
    this.activeTab = tab;
  }

  /**
   * Format timestamp for display.
   */
  formatTimestamp(timestamp: Date): string {
    return new Date(timestamp).toLocaleString();
  }

  /**
   * Get confidence score color.
   */
  getConfidenceColor(score: number): string {
    if (score >= 0.9) return 'success';
    if (score >= 0.7) return 'warning';
    return 'danger';
  }

  /**
   * Get impact severity badge class.
   */
  getImpactBadgeClass(severity: string): string {
    const map: Record<string, string> = {
      'POSITIVE_SIGNIFICANT': 'badge-success',
      'POSITIVE_MINOR': 'badge-info',
      'NEUTRAL': 'badge-secondary',
      'NEGATIVE_MINOR': 'badge-warning',
      'NEGATIVE_SIGNIFICANT': 'badge-danger',
      'CRITICAL': 'badge-danger'
    };
    return map[severity] || 'badge-secondary';
  }

  /**
   * View decision details.
   */
  viewDecisionDetails(decision: any): void {
    this.logger.info('View decision details', { decisionId: decision?.id });
    // TODO: Open modal with full decision details and replay capability
  }

  /**
   * View audit trail.
   */
  viewAuditTrail(correlationId: string): void {
    this.logger.info('View audit trail', { correlationId });
    // TODO: Load and display complete audit trail
  }

  /**
   * Replay AI decision.
   */
  replayDecision(decision: any): void {
    this.logger.info('Replay decision', { decisionId: decision?.id });
    // TODO: Implement decision replay functionality
  }
}
