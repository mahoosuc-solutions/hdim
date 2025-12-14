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
          <button mat-icon-button (click)="onClose()">
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
              <input
                matInput
                [(ngModel)]="inputMessage"
                placeholder="Type a message..."
                (keydown.enter)="sendMessage()"
                [disabled]="sending || session?.status === 'COMPLETED'" />
              <button
                matSuffix
                mat-icon-button
                (click)="sendMessage()"
                [disabled]="!inputMessage.trim() || sending">
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
                      <span class="metric-value">{{ metrics.avgLatencyMs }}ms</span>
                      <span class="metric-label">Avg Latency</span>
                    </div>
                    <div class="metric">
                      <span class="metric-value">{{ metrics.guardrailTriggers }}</span>
                      <span class="metric-label">Guardrails</span>
                    </div>
                  </div>
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
        @if (session?.status === 'IN_PROGRESS') {
          <div class="rating-section">
            <span>Rate this session:</span>
            <div class="rating-stars">
              @for (star of [1, 2, 3, 4, 5]; track star) {
                <button
                  mat-icon-button
                  (click)="rating = star"
                  [color]="rating >= star ? 'primary' : ''">
                  <mat-icon>{{ rating >= star ? 'star' : 'star_border' }}</mat-icon>
                </button>
              }
            </div>
          </div>
          <mat-form-field appearance="outline" class="feedback-field">
            <mat-label>Feedback (optional)</mat-label>
            <input matInput [(ngModel)]="feedback" placeholder="Any issues or suggestions?" />
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
      align-items: center;
      gap: 16px;
      padding: 16px 24px;

      .rating-section {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .rating-stars {
        display: flex;
      }

      .feedback-field {
        flex: 1;

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

  onClose(): void {
    this.dialogRef.close();
  }
}
