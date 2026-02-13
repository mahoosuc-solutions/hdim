import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { WebSocketService } from '../services/websocket.service';

interface CoachingMessage {
  type: 'objection' | 'phase_transition' | 'improvement';
  severity: 'low' | 'medium' | 'high';
  message: string;
  reframe?: string;
  suggested_question?: string;
  confidence: number;
  timestamp: number;
}

@Component({
  selector: 'app-coaching-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './coaching-panel.component.html',
  styleUrls: ['./coaching-panel.component.scss'],
})
export class CoachingPanelComponent implements OnInit, OnDestroy {
  messages: CoachingMessage[] = [];
  currentSpeaker: string = '';
  latestTranscript: string = '';
  callActive: boolean = false;

  private destroy$ = new Subject<void>();

  constructor(private wsService: WebSocketService) {}

  ngOnInit() {
    const userId = this.getUserId();
    this.wsService.connect(userId);

    // Subscribe to coaching messages
    this.wsService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe((msg: any) => {
        if (msg.type === 'coaching') {
          this.addCoachingMessage(msg.payload);
        } else if (msg.type === 'transcript') {
          this.updateTranscript(msg.payload);
        } else if (msg.type === 'status') {
          this.updateCallStatus(msg.payload);
        }
      });
  }

  addCoachingMessage(message: CoachingMessage) {
    message.timestamp = Date.now();
    this.messages.unshift(message);

    // Auto-dismiss low-severity after 10 seconds
    if (message.severity === 'low') {
      setTimeout(() => {
        this.dismissMessage(message);
      }, 10000);
    }
  }

  updateTranscript(payload: any) {
    this.currentSpeaker = payload.speaker;
    this.latestTranscript = payload.text;
  }

  updateCallStatus(payload: any) {
    this.callActive = payload.call_active || false;
  }

  dismissMessage(message: CoachingMessage) {
    const index = this.messages.indexOf(message);
    if (index > -1) {
      this.messages.splice(index, 1);
    }
  }

  getSeverityClass(severity: string): string {
    return `severity-${severity}`;
  }

  getSeverityColor(severity: string): string {
    switch (severity) {
      case 'high':
        return '#dc2626';
      case 'medium':
        return '#fb923c';
      case 'low':
        return '#22c55e';
      default:
        return '#6b7280';
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'objection':
        return '🚨 Objection';
      case 'phase_transition':
        return '📊 Phase Transition';
      case 'improvement':
        return '💡 Improvement';
      default:
        return type;
    }
  }

  getUserId(): string {
    // Get from URL param or localStorage
    const params = new URLSearchParams(window.location.search);
    return params.get('userId') || localStorage.getItem('userId') || 'unknown-user';
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.wsService.disconnect();
  }
}
