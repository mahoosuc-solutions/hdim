import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AIAssistantService, AIMessage, AIAnalysis } from '../../services/ai-assistant.service';

/**
 * AI Dashboard Component
 *
 * Displays AI-powered insights, recommendations, and chat interface
 * for continuous UI/UX improvement
 */
@Component({
  selector: 'app-ai-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ai-dashboard">
      <!-- Header -->
      <div class="dashboard-header">
        <div class="title-section">
          <h1>
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" stroke="currentColor" class="ai-icon">
              <circle cx="16" cy="16" r="12" stroke-width="2"/>
              <circle cx="12" cy="14" r="2" fill="currentColor"/>
              <circle cx="20" cy="14" r="2" fill="currentColor"/>
              <path d="M12 20 Q16 22 20 20" stroke-width="2" stroke-linecap="round"/>
            </svg>
            AI-Powered Insights
          </h1>
          <p class="subtitle">Automated UI/UX improvement recommendations</p>
        </div>

        <div class="header-actions">
          <button
            class="btn-secondary"
            (click)="runAnalysis()"
            [disabled]="isAnalyzing">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <path d="M8 2a6 6 0 100 12A6 6 0 008 2zM3.5 8a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0z"/>
            </svg>
            {{ isAnalyzing ? 'Analyzing...' : 'Run Analysis' }}
          </button>

          <button class="btn-primary" (click)="toggleChat()">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <path d="M2 2a2 2 0 012-2h8a2 2 0 012 2v8a2 2 0 01-2 2H6l-4 4V4a2 2 0 012-2z"/>
            </svg>
            AI Chat
          </button>
        </div>
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            📊
          </div>
          <div class="stat-content">
            <div class="stat-label">Total Interactions</div>
            <div class="stat-value">{{ stats.totalInteractions }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            {{ stats.errorRate > 0.15 ? '⚠️' : '✅' }}
          </div>
          <div class="stat-content">
            <div class="stat-label">Error Rate</div>
            <div class="stat-value" [class.high-error]="stats.errorRate > 0.15">
              {{ (stats.errorRate * 100).toFixed(1) }}%
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            💡
          </div>
          <div class="stat-content">
            <div class="stat-label">Recommendations</div>
            <div class="stat-value">{{ analyses.length }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
            🎯
          </div>
          <div class="stat-content">
            <div class="stat-label">Critical Issues</div>
            <div class="stat-value">{{ criticalCount }}</div>
          </div>
        </div>
      </div>

      <!-- Recommendations -->
      <div class="recommendations-section">
        <h2>AI Recommendations</h2>

        <div *ngIf="analyses.length === 0" class="empty-state">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor">
            <circle cx="32" cy="32" r="28" stroke-width="2"/>
            <path d="M32 20v16l12 12" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <p>No recommendations yet. Click "Run Analysis" to get AI-powered insights.</p>
        </div>

        <div class="recommendations-grid">
          <div
            *ngFor="let analysis of analyses"
            class="recommendation-card"
            [class]="'severity-' + analysis.severity">

            <div class="card-header">
              <div class="header-left">
                <span class="icon">{{ getAnalysisIcon(analysis.type) }}</span>
                <div>
                  <h3>{{ analysis.title }}</h3>
                  <span class="type-badge" [class]="'type-' + analysis.type">
                    {{ formatType(analysis.type) }}
                  </span>
                </div>
              </div>
              <span class="severity-badge" [class]="'severity-' + analysis.severity">
                {{ analysis.severity }}
              </span>
            </div>

            <p class="description">{{ analysis.description }}</p>

            <div class="recommendation-box">
              <strong>💡 Recommendation:</strong>
              <p>{{ analysis.recommendation }}</p>
            </div>

            <div class="affected-components">
              <strong>Affected Components:</strong>
              <div class="component-tags">
                <span *ngFor="let comp of analysis.affectedComponents" class="component-tag">
                  {{ comp }}
                </span>
              </div>
            </div>

            <div class="impact-estimate">
              <strong>Estimated Impact:</strong>
              <span class="impact-badge" [class]="'impact-' + analysis.estimatedImpact">
                {{ analysis.estimatedImpact }}
              </span>
            </div>

            <div class="implementation-steps" *ngIf="analysis.implementationSteps">
              <strong>Implementation Steps:</strong>
              <ol>
                <li *ngFor="let step of analysis.implementationSteps">{{ step }}</li>
              </ol>
            </div>

            <pre *ngIf="analysis.codeExample" class="code-example">{{ analysis.codeExample }}</pre>
          </div>
        </div>
      </div>

      <!-- AI Chat Panel -->
      <div class="chat-panel" [class.open]="showChat">
        <div class="chat-header">
          <h3>
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path d="M2 5a2 2 0 012-2h12a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V5z"/>
            </svg>
            AI Assistant
          </h3>
          <button class="close-btn" (click)="toggleChat()">×</button>
        </div>

        <div class="chat-messages" #chatMessages>
          <div
            *ngFor="let msg of messages"
            class="message"
            [class]="'role-' + msg.role">
            <div class="message-avatar">
              {{ msg.role === 'assistant' ? '🤖' : msg.role === 'user' ? '👤' : 'ℹ️' }}
            </div>
            <div class="message-content">
              <div class="message-role">{{ msg.role }}</div>
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <input
            [(ngModel)]="userMessage"
            (keyup.enter)="sendMessage()"
            placeholder="Ask about UI improvements, accessibility, performance..."
            [disabled]="isSending">
          <button
            (click)="sendMessage()"
            [disabled]="!userMessage.trim() || isSending">
            {{ isSending ? '...' : 'Send' }}
          </button>
        </div>

        <div class="quick-actions">
          <button
            *ngFor="let action of quickActions"
            (click)="sendQuickAction(action.message)"
            class="quick-action-btn">
            {{ action.label }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .ai-dashboard {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 32px;
      padding-bottom: 24px;
      border-bottom: 2px solid #e5e7eb;
    }

    .title-section h1 {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0 0 8px 0;
      font-size: 32px;
      color: #1f2937;
    }

    .ai-icon {
      color: #667eea;
    }

    .subtitle {
      margin: 0;
      color: #6b7280;
      font-size: 16px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }

    .btn-primary,
    .btn-secondary {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 24px;
      border: none;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
    }

    .btn-secondary {
      background: white;
      color: #374151;
      border: 1px solid #d1d5db;
    }

    .btn-secondary:hover {
      background: #f9fafb;
      border-color: #9ca3af;
    }

    .btn-secondary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    /* Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      margin-bottom: 32px;
    }

    .stat-card {
      background: white;
      padding: 24px;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      display: flex;
      gap: 16px;
      align-items: center;
      transition: all 0.2s;
    }

    .stat-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    .stat-content {
      flex: 1;
    }

    .stat-label {
      font-size: 13px;
      color: #6b7280;
      margin-bottom: 4px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #1f2937;
    }

    .stat-value.high-error {
      color: #dc2626;
    }

    /* Recommendations */
    .recommendations-section h2 {
      margin: 0 0 24px 0;
      font-size: 24px;
      color: #1f2937;
    }

    .empty-state {
      text-align: center;
      padding: 64px 32px;
      color: #6b7280;
    }

    .empty-state svg {
      margin-bottom: 16px;
      color: #d1d5db;
    }

    .recommendations-grid {
      display: grid;
      gap: 24px;
    }

    .recommendation-card {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border-left: 4px solid #3b82f6;
      transition: all 0.2s;
    }

    .recommendation-card:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    }

    .recommendation-card.severity-critical {
      border-left-color: #dc2626;
      background: #fef2f2;
    }

    .recommendation-card.severity-high {
      border-left-color: #f59e0b;
      background: #fffbeb;
    }

    .recommendation-card.severity-medium {
      border-left-color: #3b82f6;
      background: #eff6ff;
    }

    .recommendation-card.severity-low {
      border-left-color: #10b981;
      background: #f0fdf4;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .header-left {
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }

    .card-header .icon {
      font-size: 28px;
    }

    .card-header h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      color: #1f2937;
    }

    .type-badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      text-transform: capitalize;
    }

    .type-ui_improvement { background: #dbeafe; color: #1e40af; }
    .type-ux_enhancement { background: #fef3c7; color: #92400e; }
    .type-accessibility { background: #e0e7ff; color: #3730a3; }
    .type-performance { background: #d1fae5; color: #065f46; }
    .type-testing { background: #fce7f3; color: #831843; }

    .severity-badge {
      padding: 6px 16px;
      border-radius: 16px;
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
    }

    .severity-badge.severity-critical {
      background: #dc2626;
      color: white;
    }

    .severity-badge.severity-high {
      background: #f59e0b;
      color: white;
    }

    .severity-badge.severity-medium {
      background: #3b82f6;
      color: white;
    }

    .severity-badge.severity-low {
      background: #10b981;
      color: white;
    }

    .description {
      margin: 0 0 16px 0;
      color: #4b5563;
      line-height: 1.6;
    }

    .recommendation-box {
      background: rgba(102, 126, 234, 0.1);
      padding: 16px;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    .recommendation-box strong {
      display: block;
      margin-bottom: 8px;
      color: #667eea;
    }

    .recommendation-box p {
      margin: 0;
      color: #374151;
      line-height: 1.6;
    }

    .affected-components {
      margin-bottom: 16px;
    }

    .affected-components strong {
      display: block;
      margin-bottom: 8px;
      color: #374151;
    }

    .component-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .component-tag {
      padding: 6px 12px;
      background: #f3f4f6;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      font-size: 13px;
      font-family: 'Monaco', monospace;
      color: #374151;
    }

    .impact-estimate {
      margin-bottom: 16px;
    }

    .impact-estimate strong {
      margin-right: 8px;
    }

    .impact-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }

    .impact-badge.impact-high {
      background: #dcfce7;
      color: #166534;
    }

    .impact-badge.impact-medium {
      background: #fef3c7;
      color: #92400e;
    }

    .impact-badge.impact-low {
      background: #f3f4f6;
      color: #374151;
    }

    .implementation-steps {
      margin-bottom: 16px;
    }

    .implementation-steps strong {
      display: block;
      margin-bottom: 8px;
      color: #374151;
    }

    .implementation-steps ol {
      margin: 0;
      padding-left: 24px;
    }

    .implementation-steps li {
      margin-bottom: 8px;
      color: #4b5563;
      line-height: 1.6;
    }

    .code-example {
      background: #1f2937;
      color: #f3f4f6;
      padding: 16px;
      border-radius: 8px;
      font-size: 13px;
      line-height: 1.6;
      overflow-x: auto;
      margin: 0;
    }

    /* Chat Panel */
    .chat-panel {
      position: fixed;
      right: -480px;
      top: 0;
      width: 480px;
      height: 100vh;
      background: white;
      box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
      z-index: 1000;
      display: flex;
      flex-direction: column;
      transition: right 0.3s ease;
    }

    .chat-panel.open {
      right: 0;
    }

    .chat-header {
      padding: 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .chat-header h3 {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .close-btn {
      background: rgba(255, 255, 255, 0.2);
      border: none;
      color: white;
      width: 32px;
      height: 32px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 24px;
      line-height: 1;
    }

    .close-btn:hover {
      background: rgba(255, 255, 255, 0.3);
    }

    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .message {
      display: flex;
      gap: 12px;
      animation: messageSlideIn 0.2s ease;
    }

    @keyframes messageSlideIn {
      from {
        opacity: 0;
        transform: translateY(8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .message-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #f3f4f6;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
    }

    .role-assistant .message-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .role-user .message-avatar {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }

    .message-content {
      flex: 1;
    }

    .message-role {
      font-size: 12px;
      font-weight: 600;
      color: #6b7280;
      text-transform: capitalize;
      margin-bottom: 4px;
    }

    .message-text {
      color: #1f2937;
      line-height: 1.6;
      white-space: pre-wrap;
    }

    .message-time {
      font-size: 11px;
      color: #9ca3af;
      margin-top: 4px;
    }

    .chat-input {
      padding: 16px 24px;
      border-top: 1px solid #e5e7eb;
      display: flex;
      gap: 12px;
    }

    .chat-input input {
      flex: 1;
      padding: 12px;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 14px;
    }

    .chat-input input:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .chat-input button {
      padding: 12px 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
    }

    .chat-input button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .quick-actions {
      padding: 16px 24px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      border-top: 1px solid #e5e7eb;
    }

    .quick-action-btn {
      padding: 8px 16px;
      background: #f3f4f6;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .quick-action-btn:hover {
      background: #e5e7eb;
      border-color: #9ca3af;
    }

    /* Mobile */
    @media (max-width: 768px) {
      .chat-panel {
        width: 100%;
        right: -100%;
      }

      .stats-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AIDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // State
  analyses: AIAnalysis[] = [];
  messages: AIMessage[] = [];
  stats = {
    totalInteractions: 0,
    errorRate: 0
  };

  // UI state
  showChat = false;
  userMessage = '';
  isAnalyzing = false;
  isSending = false;

  // Quick actions
  quickActions = [
    { label: 'Improve measure builder', message: 'How can I improve the measure builder page?' },
    { label: 'Accessibility tips', message: 'What accessibility improvements do you recommend?' },
    { label: 'Performance optimization', message: 'How can I improve performance?' },
    { label: 'Testing recommendations', message: 'What tests should I add?' }
  ];

  constructor(private aiAssistant: AIAssistantService) {}

  ngOnInit() {
    // Subscribe to analysis updates
    this.aiAssistant.analysis$
      .pipe(takeUntil(this.destroy$))
      .subscribe(analyses => {
        this.analyses = analyses;
      });

    // Subscribe to chat messages
    this.aiAssistant.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(messages => {
        this.messages = messages;
        this.scrollChatToBottom();
      });

    // Calculate stats
    this.updateStats();

    // Run initial analysis
    this.runAnalysis();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get criticalCount(): number {
    return this.analyses.filter(a => a.severity === 'critical').length;
  }

  async runAnalysis() {
    this.isAnalyzing = true;
    try {
      await this.aiAssistant.analyzeInteractions();
      this.updateStats();
    } finally {
      this.isAnalyzing = false;
    }
  }

  async sendMessage() {
    if (!this.userMessage.trim()) return;

    this.isSending = true;
    try {
      await this.aiAssistant.sendMessage(this.userMessage);
      this.userMessage = '';
    } finally {
      this.isSending = false;
    }
  }

  async sendQuickAction(message: string) {
    this.userMessage = message;
    await this.sendMessage();
  }

  toggleChat() {
    this.showChat = !this.showChat;
  }

  private updateStats() {
    // In production, get from service
    this.stats = {
      totalInteractions: (this.aiAssistant as any).interactions?.length || 0,
      errorRate: this.calculateErrorRate()
    };
  }

  private calculateErrorRate(): number {
    const interactions = (this.aiAssistant as any).interactions || [];
    if (interactions.length === 0) return 0;

    const errors = interactions.filter((i: any) => !i.success).length;
    return errors / interactions.length;
  }

  private scrollChatToBottom() {
    setTimeout(() => {
      const chatMessages = document.querySelector('.chat-messages');
      if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
      }
    }, 100);
  }

  getAnalysisIcon(type: string): string {
    const icons: Record<string, string> = {
      'ui_improvement': '🎨',
      'ux_enhancement': '✨',
      'accessibility': '♿',
      'performance': '⚡',
      'testing': '🧪'
    };
    return icons[type] || '💡';
  }

  formatType(type: string): string {
    return type.replace('_', ' ');
  }

  formatTime(timestamp: Date): string {
    return new Date(timestamp).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
