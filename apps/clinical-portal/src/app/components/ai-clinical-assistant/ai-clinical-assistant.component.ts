import { Component, OnInit, OnDestroy, Input, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { AIAssistantService, AIMessage } from '../../services/ai-assistant.service';
import { LoggerService } from '../../services/logger.service';

/**
 * AI Clinical Assistant Chat Component
 *
 * Conversational AI interface for clinical decision support.
 *
 * Features:
 * - Real-time message thread (user vs assistant bubbles)
 * - Typing indicator during AI responses
 * - Context awareness (patient, care gap, measure)
 * - Suggested prompts for quick actions
 * - Auto-scroll to latest message
 * - Clear conversation functionality
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - No console statements (ESLint no-console enforced)
 * - PHI filtering via LoggerService
 *
 * Sprint 3 - Issue #243: AI Clinical Assistant Chat
 */
@Component({
  selector: 'app-ai-clinical-assistant',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './ai-clinical-assistant.component.html',
  styleUrls: ['./ai-clinical-assistant.component.scss'],
})
export class AiClinicalAssistantComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() patientId?: string;
  @Input() careGapId?: string;
  @Input() measureId?: string;

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  messages: AIMessage[] = [];
  userInput = '';
  isTyping = false;
  context: { patient?: string; careGap?: string; measure?: string } | null = null;

  private destroy$ = new Subject<void>();
  private shouldScrollToBottom = false;

  // Suggested prompts based on context
  suggestedPrompts: string[] = [];

  constructor(
    private aiAssistantService: AIAssistantService,
    private logger: LoggerService
  ) {
  }

  ngOnInit(): void {
    this.logger.info('AI Clinical Assistant initialized', {
      patientId: this.patientId,
      careGapId: this.careGapId,
      measureId: this.measureId,
    });

    // Set context based on inputs
    this.context = {
      patient: this.patientId,
      careGap: this.careGapId,
      measure: this.measureId,
    };

    // Subscribe to message stream
    this.aiAssistantService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe((messages: AIMessage[]) => {
        this.messages = messages;
        this.shouldScrollToBottom = true;
      });

    // Set suggested prompts based on context
    this.setSuggestedPrompts();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Send user message to AI assistant
   */
  async sendMessage(): Promise<void> {
    if (!this.userInput.trim()) return;

    const message = this.userInput.trim();
    this.userInput = '';
    this.isTyping = true;

    this.logger.info('Sending message to AI assistant', { messageLength: message.length });

    try {
      await this.aiAssistantService.sendMessage(message, this.context || undefined);
      this.isTyping = false;
      this.shouldScrollToBottom = true;
    } catch (error) {
      this.logger.error('Failed to send message to AI assistant', error as Error);
      this.isTyping = false;
    }
  }

  /**
   * Send suggested prompt
   */
  sendSuggestedPrompt(prompt: string): void {
    this.userInput = prompt;
    this.sendMessage();
  }

  /**
   * Clear conversation
   */
  clearConversation(): void {
    this.logger.info('Clearing AI assistant conversation');
    this.messages = [];
    this.userInput = '';
    this.isTyping = false;
  }

  /**
   * Handle Enter key press
   */
  onEnterKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  /**
   * Scroll to bottom of message container
   */
  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        const element = this.messagesContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    } catch (err) {
      this.logger.error('Failed to scroll to bottom', err as Error);
    }
  }

  /**
   * Set suggested prompts based on context
   */
  private setSuggestedPrompts(): void {
    this.suggestedPrompts = [];

    if (this.patientId) {
      this.suggestedPrompts.push(
        "Summarize this patient's care gaps",
        'What interventions do you recommend?',
        'Review latest clinical data'
      );
    }

    if (this.careGapId) {
      this.suggestedPrompts.push(
        'Explain this care gap',
        'What evidence supports closure?',
        'Suggest intervention strategies'
      );
    }

    if (this.measureId) {
      this.suggestedPrompts.push(
        'Explain this quality measure',
        'What are the numerator criteria?',
        'How can we improve compliance?'
      );
    }

    // Default prompts if no context
    if (this.suggestedPrompts.length === 0) {
      this.suggestedPrompts.push(
        'What can you help me with?',
        'Explain HEDIS quality measures',
        'How do I close a care gap?'
      );
    }
  }

  /**
   * Get display name for context
   */
  getContextDisplay(): string | null {
    if (this.patientId) return `Patient: ${this.patientId}`;
    if (this.careGapId) return `Care Gap: ${this.careGapId}`;
    if (this.measureId) return `Measure: ${this.measureId}`;
    return null;
  }
}
