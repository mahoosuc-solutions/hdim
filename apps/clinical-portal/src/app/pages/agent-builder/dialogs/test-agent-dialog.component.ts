import { Component, Inject, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { Subject, takeUntil } from 'rxjs';

import { AgentBuilderService } from '../services/agent-builder.service';
import {
  AgentConfiguration,
  AgentTestSession,
  TestMessage,
  ToolInvocation,
  TestMetrics,
} from '../models/agent.model';
import { ToastService } from '../../../services/toast.service';

export interface TestAgentDialogData {
  agent: AgentConfiguration;
}

@Component({
  selector: 'app-test-agent-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    MatTabsModule,
    MatMenuModule,
  ],
  template: `
    <div class="test-dialog">
      <div class="dialog-header">
        <div class="header-info">
          <h2>Test: {{ data.agent.name }}</h2>
          <p class="subtitle">{{ data.agent.personaRole || 'AI Agent' }}</p>
        </div>
        <div class="header-actions">
          @if (session) {
            <mat-chip [color]="session.status === 'IN_PROGRESS' ? 'primary' : 'accent'">
              {{ session.status }}
            </mat-chip>
          }
          <button mat-icon-button (click)="onClose()" aria-label="Close dialog">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-body">
        <!-- Chat Panel -->
        <div class="chat-panel">
          <!-- Messages -->
          <div class="messages-container" #messagesContainer>
            @if (!session) {
              <div class="start-message">
                <mat-icon>smart_toy</mat-icon>
                <h3>Ready to Test</h3>
                <p>Send a message to start testing this agent</p>
              </div>
            } @else {
              @for (message of messages; track $index) {
                <div class="message" [class]="message.role">
                  <div class="message-avatar">
                    @if (message.role === 'user') {
                      <mat-icon>person</mat-icon>
                    } @else if (message.role === 'assistant') {
                      <mat-icon>smart_toy</mat-icon>
                    } @else {
                      <mat-icon>info</mat-icon>
                    }
                  </div>
                  <div class="message-content">
                    <div class="message-header">
                      <span class="message-role">
                        {{ message.role === 'user' ? 'You' : message.role === 'assistant' ? data.agent.personaName || 'Agent' : 'System' }}
                      </span>
                      @if (message.latencyMs) {
                        <span class="message-latency">{{ message.latencyMs }}ms</span>
                      }
                    </div>
                    <div class="message-text">{{ message.content }}</div>
                  </div>
                </div>
              }

              @if (sending) {
                <div class="message assistant">
                  <div class="message-avatar">
                    <mat-icon>smart_toy</mat-icon>
                  </div>
                  <div class="message-content">
                    <div class="typing-indicator">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              }
            }
          </div>

          <!-- Input -->
          <div class="input-container">
            <mat-form-field appearance="outline" class="message-input">
              <mat-label>Test message</mat-label>
              <input
                matInput
                [(ngModel)]="inputMessage"
                placeholder="Type a message..."
                aria-label="Type a test message"
                (keydown.enter)="sendMessage()"
                [disabled]="sending || session?.status === 'COMPLETED'" />
              <button
                matSuffix
                mat-icon-button
                (click)="sendMessage()"
                [disabled]="!inputMessage.trim() || sending"
                aria-label="Send message">
                <mat-icon>send</mat-icon>
              </button>
            </mat-form-field>
          </div>
        </div>

        <!-- Info Panel -->
        <div class="info-panel">
          <mat-tab-group>
            <!-- Metrics Tab -->
            <mat-tab label="Metrics">
              <div class="tab-content">
                @if (metrics) {
                  <div class="metrics-grid">
                    <div class="metric">
                      <span class="metric-value">{{ metrics.totalMessages }}</span>
                      <span class="metric-label">Messages</span>
                    </div>
                    <div class="metric">
                      <span class="metric-value">{{ metrics.toolInvocations }}</span>
                      <span class="metric-label">Tool Calls</span>
                    </div>
                    <div class="metric">
                      <span class="metric-value">{{ metrics.averageLatency }}ms</span>
                      <span class="metric-label">Avg Latency</span>
                    </div>
                    <div class="metric">
                      <span class="metric-value">{{ metrics.guardrailsTriggered }}</span>
                      <span class="metric-label">Guardrails</span>
                    </div>
                  </div>

                  <!-- Guardrail Triggers Detail -->
                  @if (metrics.guardrailTriggers && metrics.guardrailTriggers.length > 0) {
                    <mat-divider style="margin: 16px 0;"></mat-divider>
                    <h4 class="section-header">Guardrail Activity</h4>
                    <div class="guardrail-triggers">
                      @for (trigger of metrics.guardrailTriggers; track $index) {
                        <mat-card class="trigger-card" [class]="'trigger-action-' + trigger.action.toLowerCase()">
                          <mat-card-header>
                            <mat-icon [color]="getGuardrailActionColor(trigger.action)">
                              {{ getGuardrailActionIcon(trigger.action) }}
                            </mat-icon>
                            <mat-card-title>{{ formatGuardrailType(trigger.guardrailType) }}</mat-card-title>
                            <span class="trigger-time">
                              {{ formatTimestamp(trigger.timestamp) }}
                            </span>
                          </mat-card-header>
                          <mat-card-content>
                            <div class="trigger-detail">
                              <strong>Action:</strong>
                              <mat-chip
                                size="small"
                                [class]="'action-chip-' + trigger.action.toLowerCase()">
                                {{ trigger.action }}
                              </mat-chip>
                            </div>
                            <div class="trigger-detail">
                              <strong>Triggered By:</strong>
                              <span>{{ trigger.triggeredBy }}</span>
                            </div>
                            <div class="trigger-detail">
                              <strong>Message Index:</strong>
                              <span>#{{ trigger.messageIndex }}</span>
                            </div>
                            @if (trigger.details) {
                              <div class="trigger-detail">
                                <strong>Details:</strong>
                                <span class="trigger-details-text">{{ trigger.details }}</span>
                              </div>
                            }
                          </mat-card-content>
                        </mat-card>
                      }
                    </div>
                  }
                } @else {
                  <p class="empty-state">Metrics will appear once testing begins</p>
                }
              </div>
            </mat-tab>

            <!-- Tools Tab -->
            <mat-tab label="Tools">
              <div class="tab-content">
                @if (toolInvocations.length > 0) {
                  @for (invocation of toolInvocations; track $index) {
                    <mat-card class="tool-card">
                      <mat-card-header>
                        <mat-icon [color]="invocation.success ? 'primary' : 'warn'">
                          {{ invocation.success ? 'check_circle' : 'error' }}
                        </mat-icon>
                        <mat-card-title>{{ invocation.name }}</mat-card-title>
                        <span class="tool-duration">{{ invocation.durationMs }}ms</span>
                      </mat-card-header>
                      <mat-card-content>
                        @if (invocation.result) {
                          <pre class="tool-result">{{ invocation.result }}</pre>
                        }
                      </mat-card-content>
                    </mat-card>
                  }
                } @else {
                  <p class="empty-state">No tools invoked yet</p>
                }
              </div>
            </mat-tab>

            <!-- Config Tab -->
            <mat-tab label="Config">
              <div class="tab-content">
                <div class="config-item">
                  <span class="config-label">Provider</span>
                  <span class="config-value">{{ data.agent.modelProvider }}</span>
                </div>
                <div class="config-item">
                  <span class="config-label">Model</span>
                  <span class="config-value">{{ data.agent.modelId || 'Default' }}</span>
                </div>
                <div class="config-item">
                  <span class="config-label">Temperature</span>
                  <span class="config-value">{{ data.agent.temperature || 0.7 }}</span>
                </div>
                <div class="config-item">
                  <span class="config-label">Max Tokens</span>
                  <span class="config-value">{{ data.agent.maxTokens || 2048 }}</span>
                </div>
                <mat-divider></mat-divider>
                <h4>Enabled Tools</h4>
                <div class="enabled-tools">
                  @for (tool of enabledTools; track tool) {
                    <mat-chip>{{ tool }}</mat-chip>
                  }
                  @if (enabledTools.length === 0) {
                    <span class="empty-state">No tools enabled</span>
                  }
                </div>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>
      </div>

      <mat-divider></mat-divider>

      <div class="dialog-footer">
        <div class="footer-left">
          @if (messages.length > 0) {
            <button
              mat-button
              [matMenuTriggerFor]="exportMenu"
              aria-label="Export conversation">
              <mat-icon>download</mat-icon>
              Export
            </button>
            <mat-menu #exportMenu="matMenu">
              <button mat-menu-item (click)="exportConversation('json')">
                <mat-icon>code</mat-icon>
                <span>Export as JSON</span>
              </button>
              <button mat-menu-item (click)="exportConversation('markdown')">
                <mat-icon>description</mat-icon>
                <span>Export as Markdown</span>
              </button>
              <button mat-menu-item (click)="exportConversation('csv')">
                <mat-icon>table_chart</mat-icon>
                <span>Export as CSV</span>
              </button>
            </mat-menu>
          }
        </div>

        <div class="footer-right">
          @if (session?.status === 'IN_PROGRESS') {
            <div class="rating-section">
              <span>Rate this session:</span>
              <div class="rating-stars">
                @for (star of [1, 2, 3, 4, 5]; track star) {
                  <button
                    mat-icon-button
                    (click)="rating = star"
                    [color]="rating >= star ? 'primary' : ''"
                    [attr.aria-label]="'Rate ' + star + ' out of 5 stars'">
                    <mat-icon>{{ rating >= star ? 'star' : 'star_border' }}</mat-icon>
                  </button>
                }
              </div>
            </div>
            <mat-form-field appearance="outline" class="feedback-field">
              <mat-label>Feedback (optional)</mat-label>
              <input matInput [(ngModel)]="feedback" placeholder="Any issues or suggestions?" aria-label="Provide feedback" />
            </mat-form-field>
            <button
              mat-flat-button
              color="primary"
              (click)="completeSession()"
              [disabled]="completing">
              @if (completing) {
                <mat-spinner diameter="20"></mat-spinner>
              } @else {
                Complete Session
              }
            </button>
          } @else {
            <button mat-button (click)="onClose()">Close</button>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .test-dialog {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 16px 24px;

      h2 {
        margin: 0;
      }

      .subtitle {
        margin: 4px 0 0;
        color: var(--mat-sys-on-surface-variant);
      }

      .header-actions {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .dialog-body {
      display: flex;
      flex: 1;
      min-height: 0;
    }

    .chat-panel {
      flex: 1;
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--mat-sys-outline-variant);
    }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }

    .start-message {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        opacity: 0.5;
      }

      h3 {
        margin: 16px 0 8px;
      }

      p {
        margin: 0;
      }
    }

    .message {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;

      &.user {
        flex-direction: row-reverse;

        .message-content {
          background: var(--mat-sys-primary-container);
          color: var(--mat-sys-on-primary-container);
        }
      }

      &.assistant {
        .message-content {
          background: var(--mat-sys-surface-variant);
        }
      }

      &.system {
        .message-content {
          background: var(--mat-sys-error-container);
          color: var(--mat-sys-on-error-container);
        }
      }
    }

    .message-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--mat-sys-surface-variant);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .message-content {
      max-width: 70%;
      padding: 12px 16px;
      border-radius: 12px;
    }

    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }

    .message-role {
      font-weight: 500;
      font-size: 0.875rem;
    }

    .message-latency {
      font-size: 0.75rem;
      color: var(--mat-sys-outline);
    }

    .message-text {
      white-space: pre-wrap;
      word-break: break-word;
    }

    .typing-indicator {
      display: flex;
      gap: 4px;
      padding: 8px 0;

      span {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--mat-sys-on-surface-variant);
        animation: typing 1.4s infinite ease-in-out;

        &:nth-child(2) {
          animation-delay: 0.2s;
        }

        &:nth-child(3) {
          animation-delay: 0.4s;
        }
      }
    }

    @keyframes typing {
      0%, 80%, 100% {
        opacity: 0.3;
        transform: scale(0.8);
      }
      40% {
        opacity: 1;
        transform: scale(1);
      }
    }

    .input-container {
      padding: 16px;
      border-top: 1px solid var(--mat-sys-outline-variant);
    }

    .message-input {
      width: 100%;

      ::ng-deep .mat-mdc-form-field-subscript-wrapper {
        display: none;
      }
    }

    .info-panel {
      width: 300px;
      display: flex;
      flex-direction: column;
    }

    .tab-content {
      padding: 16px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }

    .metric {
      text-align: center;
      padding: 12px;
      background: var(--mat-sys-surface-variant);
      border-radius: 8px;

      .metric-value {
        display: block;
        font-size: 1.5rem;
        font-weight: 600;
        color: var(--mat-sys-primary);
      }

      .metric-label {
        font-size: 0.75rem;
        color: var(--mat-sys-on-surface-variant);
      }
    }

    .section-header {
      margin: 0 0 12px;
      font-size: 1rem;
      font-weight: 500;
      color: var(--mat-sys-on-surface);
    }

    .guardrail-triggers {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 300px;
      overflow-y: auto;
    }

    .trigger-card {
      margin-bottom: 0;

      mat-card-header {
        display: flex;
        align-items: center;
        gap: 8px;

        mat-icon {
          margin-right: 8px;
        }

        .trigger-time {
          margin-left: auto;
          font-size: 0.75rem;
          color: var(--mat-sys-outline);
        }
      }

      .trigger-detail {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 6px 0;
        font-size: 0.875rem;

        strong {
          min-width: 120px;
          color: var(--mat-sys-on-surface-variant);
        }

        .trigger-details-text {
          color: var(--mat-sys-on-surface);
          font-style: italic;
        }
      }

      .action-chip-filtered {
        background: var(--mat-sys-primary-container);
        color: var(--mat-sys-on-primary-container);
      }

      .action-chip-blocked {
        background: var(--mat-sys-error-container);
        color: var(--mat-sys-on-error-container);
      }

      .action-chip-warning {
        background: var(--mat-sys-tertiary-container);
        color: var(--mat-sys-on-tertiary-container);
      }

      .action-chip-requires_review {
        background: var(--mat-sys-secondary-container);
        color: var(--mat-sys-on-secondary-container);
      }

      &.trigger-action-filtered {
        border-left: 4px solid var(--mat-sys-primary);
      }

      &.trigger-action-blocked {
        border-left: 4px solid var(--mat-sys-error);
      }

      &.trigger-action-warning {
        border-left: 4px solid var(--mat-sys-tertiary);
      }

      &.trigger-action-requires_review {
        border-left: 4px solid var(--mat-sys-secondary);
      }
    }

    .tool-card {
      margin-bottom: 12px;

      mat-card-header {
        display: flex;
        align-items: center;
        gap: 8px;

        mat-icon {
          margin-right: 8px;
        }

        .tool-duration {
          margin-left: auto;
          font-size: 0.75rem;
          color: var(--mat-sys-outline);
        }
      }

      .tool-result {
        font-size: 0.75rem;
        background: var(--mat-sys-surface-variant);
        padding: 8px;
        border-radius: 4px;
        overflow-x: auto;
        max-height: 100px;
      }
    }

    .config-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid var(--mat-sys-outline-variant);

      .config-label {
        color: var(--mat-sys-on-surface-variant);
      }

      .config-value {
        font-weight: 500;
      }
    }

    .enabled-tools {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 8px;
    }

    .empty-state {
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
      padding: 24px;
    }

    .dialog-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      padding: 16px 24px;

      .footer-left {
        display: flex;
        gap: 8px;
      }

      .footer-right {
        display: flex;
        align-items: center;
        gap: 16px;
        flex: 1;
        justify-content: flex-end;
      }

      .rating-section {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .rating-stars {
        display: flex;
      }

      .feedback-field {
        width: 250px;

        ::ng-deep .mat-mdc-form-field-subscript-wrapper {
          display: none;
        }
      }
    }
  `],
})
export class TestAgentDialogComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  private destroy$ = new Subject<void>();
  private shouldScrollToBottom = false;

  session: AgentTestSession | null = null;
  messages: TestMessage[] = [];
  toolInvocations: ToolInvocation[] = [];
  metrics: TestMetrics | null = null;
  enabledTools: string[] = [];

  inputMessage = '';
  sending = false;
  completing = false;
  rating = 0;
  feedback = '';

  constructor(
    private agentService: AgentBuilderService,
    private toast: ToastService,
    private dialogRef: MatDialogRef<TestAgentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TestAgentDialogData
  ) {}

  ngOnInit(): void {
    // Extract enabled tools from agent configuration
    if (this.data.agent.toolConfiguration) {
      this.enabledTools = this.data.agent.toolConfiguration
        .filter((t) => t.enabled)
        .map((t) => t.toolName);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  sendMessage(): void {
    const message = this.inputMessage.trim();
    if (!message || this.sending) return;

    this.inputMessage = '';
    this.sending = true;
    this.shouldScrollToBottom = true;

    // Start session if not exists
    if (!this.session) {
      this.agentService
        .startTestSession(this.data.agent.id, 'INTERACTIVE')
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (session) => {
            this.session = session;
            this.sendTestMessage(session.id, message);
          },
          error: () => {
            this.toast.error('Failed to start test session');
            this.sending = false;
          },
        });
    } else {
      this.sendTestMessage(this.session.id, message);
    }
  }

  private sendTestMessage(sessionId: string, message: string): void {
    // Add user message immediately
    this.messages.push({
      role: 'user',
      content: message,
      timestamp: new Date().toISOString(),
    });
    this.shouldScrollToBottom = true;

    this.agentService
      .sendTestMessage(sessionId, message)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.sending = false;

          if (result.success && result.content) {
            this.messages.push({
              role: 'assistant',
              content: result.content,
              timestamp: new Date().toISOString(),
              latencyMs: result.latencyMs,
            });
          } else if (result.error) {
            this.messages.push({
              role: 'system',
              content: `Error: ${result.error}`,
              timestamp: new Date().toISOString(),
            });
          }

          this.shouldScrollToBottom = true;
          this.refreshSession();
        },
        error: (err) => {
          this.sending = false;
          this.messages.push({
            role: 'system',
            content: `Error: ${err.message || 'Failed to send message'}`,
            timestamp: new Date().toISOString(),
          });
          this.shouldScrollToBottom = true;
        },
      });
  }

  private refreshSession(): void {
    if (!this.session) return;

    this.agentService
      .getTestSession(this.session.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (session) => {
          this.session = session;
          this.toolInvocations = session.toolInvocations || [];
          this.metrics = session.metrics || null;
        },
      });
  }

  completeSession(): void {
    if (!this.session) return;

    this.completing = true;
    this.agentService
      .completeTestSession(this.session.id, this.feedback, this.rating)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (session) => {
          this.session = session;
          this.completing = false;
          this.toast.success('Test session completed');
        },
        error: () => {
          this.completing = false;
          this.toast.error('Failed to complete session');
        },
      });
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const element = this.messagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  exportConversation(format: 'json' | 'markdown' | 'csv'): void {
    if (!this.messages.length) {
      this.toast.info('No conversation to export');
      return;
    }

    let content: string;
    let mimeType: string;
    let fileExtension: string;

    switch (format) {
      case 'json':
        content = this.exportAsJson();
        mimeType = 'application/json';
        fileExtension = 'json';
        break;
      case 'markdown':
        content = this.exportAsMarkdown();
        mimeType = 'text/markdown';
        fileExtension = 'md';
        break;
      case 'csv':
        content = this.exportAsCsv();
        mimeType = 'text/csv';
        fileExtension = 'csv';
        break;
    }

    const blob = new Blob([content], { type: mimeType });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const agentName = this.data.agent.name.replace(/[^a-z0-9]/gi, '_').toLowerCase();

    link.href = url;
    link.download = `agent-test_${agentName}_${timestamp}.${fileExtension}`;
    link.click();
    window.URL.revokeObjectURL(url);

    this.toast.success(`Conversation exported as ${format.toUpperCase()}`);
  }

  private exportAsJson(): string {
    const exportData = {
      agent: {
        id: this.data.agent.id,
        name: this.data.agent.name,
        version: this.data.agent.version,
      },
      session: {
        id: this.session?.id,
        status: this.session?.status,
        startedAt: this.session?.startedAt,
        completedAt: this.session?.completedAt,
      },
      messages: this.messages,
      toolInvocations: this.toolInvocations,
      metrics: this.metrics,
      rating: this.rating || null,
      feedback: this.feedback || null,
      exportedAt: new Date().toISOString(),
    };
    return JSON.stringify(exportData, null, 2);
  }

  private exportAsMarkdown(): string {
    let markdown = `# Agent Test Conversation\n\n`;
    markdown += `**Agent:** ${this.data.agent.name}\n`;
    markdown += `**Version:** ${this.data.agent.version}\n`;
    markdown += `**Session ID:** ${this.session?.id || 'N/A'}\n`;
    markdown += `**Exported:** ${new Date().toLocaleString()}\n\n`;
    markdown += `---\n\n`;

    // Messages
    markdown += `## Conversation\n\n`;
    this.messages.forEach((msg) => {
      const role = msg.role.charAt(0).toUpperCase() + msg.role.slice(1);
      const time = new Date(msg.timestamp).toLocaleTimeString();
      markdown += `### ${role} (${time})\n\n`;
      markdown += `${msg.content}\n\n`;
      if (msg.latencyMs) {
        markdown += `*Response time: ${msg.latencyMs}ms*\n\n`;
      }
    });

    // Tool Invocations
    if (this.toolInvocations.length > 0) {
      markdown += `---\n\n## Tool Invocations\n\n`;
      this.toolInvocations.forEach((tool, idx) => {
        markdown += `### ${idx + 1}. ${tool.toolName}\n\n`;
        markdown += `**Status:** ${tool.status}\n`;
        markdown += `**Timestamp:** ${new Date(tool.timestamp).toLocaleString()}\n`;
        if (tool.input) {
          markdown += `**Input:**\n\`\`\`json\n${JSON.stringify(tool.input, null, 2)}\n\`\`\`\n\n`;
        }
        if (tool.output) {
          markdown += `**Output:**\n\`\`\`json\n${JSON.stringify(tool.output, null, 2)}\n\`\`\`\n\n`;
        }
        if (tool.error) {
          markdown += `**Error:** ${tool.error}\n\n`;
        }
      });
    }

    // Metrics
    if (this.metrics) {
      markdown += `---\n\n## Session Metrics\n\n`;
      markdown += `- **Total Messages:** ${this.metrics.totalMessages}\n`;
      markdown += `- **User Messages:** ${this.metrics.userMessages}\n`;
      markdown += `- **Assistant Messages:** ${this.metrics.assistantMessages}\n`;
      markdown += `- **Tool Invocations:** ${this.metrics.toolInvocations}\n`;
      markdown += `- **Average Latency:** ${this.metrics.averageLatency}ms\n`;
      markdown += `- **Total Tokens:** ${this.metrics.totalTokens}\n`;
      markdown += `- **Guardrails Triggered:** ${this.metrics.guardrailsTriggered}\n`;
    }

    if (this.rating > 0) {
      markdown += `\n---\n\n## Feedback\n\n`;
      markdown += `**Rating:** ${'⭐'.repeat(this.rating)}\n`;
      if (this.feedback) {
        markdown += `**Comments:** ${this.feedback}\n`;
      }
    }

    return markdown;
  }

  private exportAsCsv(): string {
    let csv = 'Timestamp,Role,Content,Latency (ms)\n';

    this.messages.forEach((msg) => {
      const timestamp = new Date(msg.timestamp).toISOString();
      const role = msg.role;
      const content = msg.content.replace(/"/g, '""'); // Escape quotes
      const latency = msg.latencyMs || '';

      csv += `"${timestamp}","${role}","${content}","${latency}"\n`;
    });

    return csv;
  }

  getGuardrailActionColor(action: string): string {
    const colorMap: Record<string, string> = {
      FILTERED: 'primary',
      BLOCKED: 'warn',
      WARNING: 'accent',
      REQUIRES_REVIEW: 'warn',
    };
    return colorMap[action] || 'primary';
  }

  getGuardrailActionIcon(action: string): string {
    const iconMap: Record<string, string> = {
      FILTERED: 'filter_alt',
      BLOCKED: 'block',
      WARNING: 'warning',
      REQUIRES_REVIEW: 'visibility',
    };
    return iconMap[action] || 'info';
  }

  formatGuardrailType(type: string): string {
    return type
      .split('_')
      .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  }

  formatTimestamp(timestamp: string): string {
    return new Date(timestamp).toLocaleTimeString();
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
