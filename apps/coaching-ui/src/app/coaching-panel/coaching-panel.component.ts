import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Subject, interval, of, takeUntil } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
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

type ActionStatus = 'pending' | 'approved' | 'rejected' | 'revision_requested';
type OperatorRole = 'viewer' | 'operator';

interface AgentState {
  objective: string;
  phase: string;
  confidence: number;
  nextAction: string;
  riskLevel: 'low' | 'medium' | 'high';
  autonomy: 'manual' | 'assisted' | 'auto';
}

interface PendingAction {
  id: string;
  title: string;
  rationale: string;
  impact: 'low' | 'medium' | 'high';
  createdAt: number;
  status: ActionStatus;
}

interface DecisionEvent {
  actionId: string;
  actionTitle: string;
  decision: ActionStatus;
  decidedAt: number;
  operator: string;
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
  wsConnected: boolean = false;
  apiConnected: boolean = false;
  role: OperatorRole = 'operator';

  actionQueue: PendingAction[] = [];
  decisionHistory: DecisionEvent[] = [];

  agentState: AgentState = {
    objective: 'Guide discovery call to qualified next step',
    phase: 'Discovery',
    confidence: 0.62,
    nextAction: 'Ask pain-clarification follow-up',
    riskLevel: 'medium',
    autonomy: 'assisted',
  };

  private readonly apiBase = 'http://localhost:8090/api/sales/operator';
  private destroy$ = new Subject<void>();

  constructor(
    private wsService: WebSocketService,
    private http: HttpClient,
  ) {}

  ngOnInit() {
    this.role = this.getRole();
    const userId = this.getUserId();

    this.wsService.connect(userId);
    this.wsService.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe((connected: boolean) => {
        this.wsConnected = connected;
      });

    this.wsService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe((msg: any) => {
        if (msg.type === 'coaching') {
          this.addCoachingMessage(msg.payload);
        } else if (msg.type === 'transcript') {
          this.updateTranscript(msg.payload);
        } else if (msg.type === 'status') {
          this.updateCallStatus(msg.payload);
        } else if (msg.type === 'agent_state') {
          this.updateAgentStateFromWs(msg.payload);
        }
      });

    this.loadOperatorData();
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadOperatorData());
  }

  private loadOperatorData() {
    this.http.get<any>(`${this.apiBase}/state`)
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => {
          this.apiConnected = false;
          return of(null);
        }),
      )
      .subscribe((state) => {
        if (!state) {
          return;
        }
        this.apiConnected = true;
        this.agentState = {
          objective: state.objective,
          phase: state.phase,
          confidence: state.confidence,
          nextAction: state.next_action,
          riskLevel: state.risk_level,
          autonomy: state.autonomy,
        };
      });

    this.http.get<any>(`${this.apiBase}/actions/pending`)
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => of({ actions: [] })),
      )
      .subscribe((response) => {
        this.actionQueue = (response.actions || []).map((action: any) => ({
          id: action.id,
          title: action.title,
          rationale: action.rationale,
          impact: action.impact,
          createdAt: action.created_at,
          status: action.status,
        }));
      });

    this.http.get<any>(`${this.apiBase}/decisions?limit=50`)
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => of({ decisions: [] })),
      )
      .subscribe((response) => {
        this.decisionHistory = (response.decisions || []).map((event: any) => ({
          actionId: event.action_id,
          actionTitle: event.action_title,
          decision: event.decision,
          decidedAt: event.decided_at,
          operator: event.operator,
        }));
      });
  }

  addCoachingMessage(message: CoachingMessage) {
    message.timestamp = Date.now();
    this.messages.unshift(message);
    if (message.severity === 'low') {
      setTimeout(() => this.dismissMessage(message), 10000);
    }
  }

  updateTranscript(payload: any) {
    this.currentSpeaker = payload.speaker;
    this.latestTranscript = payload.text;
  }

  updateCallStatus(payload: any) {
    this.callActive = payload.call_active || false;
  }

  updateAgentStateFromWs(payload: Partial<AgentState>) {
    this.agentState = {
      ...this.agentState,
      ...payload,
    };
  }

  dismissMessage(message: CoachingMessage) {
    const index = this.messages.indexOf(message);
    if (index > -1) {
      this.messages.splice(index, 1);
    }
  }

  decideAction(action: PendingAction, decision: ActionStatus) {
    if (this.role !== 'operator' || action.status !== 'pending') {
      return;
    }

    this.http.post<any>(`${this.apiBase}/actions/${action.id}/decision`, {
      decision,
      operator_id: this.getUserId(),
    }, {
      headers: {
        'X-Operator-Role': this.role,
      },
    })
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => {
          this.wsService.send('operator_decision', {
            action_id: action.id,
            decision,
            operator_id: this.getUserId(),
            timestamp: Date.now(),
          });
          return of(true);
        }),
        catchError(() => of(false)),
      )
      .subscribe(() => this.loadOperatorData());
  }

  getSeverityClass(severity: string): string {
    return `severity-${severity}`;
  }

  getRiskClass(riskLevel: string): string {
    return `risk-${riskLevel}`;
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'objection':
        return 'Objection';
      case 'phase_transition':
        return 'Phase Transition';
      case 'improvement':
        return 'Improvement';
      default:
        return type;
    }
  }

  getDecisionLabel(decision: ActionStatus): string {
    if (decision === 'approved') {
      return 'Approved';
    }
    if (decision === 'rejected') {
      return 'Rejected';
    }
    if (decision === 'revision_requested') {
      return 'Revision requested';
    }
    return 'Pending';
  }

  exportActivity(format: 'json' | 'csv') {
    const url = `${this.apiBase}/activity/export?format=${format}&limit=200`;
    this.http.get(url, { responseType: 'text' })
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => of('')),
      )
      .subscribe((content) => {
        if (!content) {
          return;
        }
        const filename = `operator-activity-${Date.now()}.${format}`;
        const mimeType = format === 'csv' ? 'text/csv;charset=utf-8' : 'application/json;charset=utf-8';
        const blob = new Blob([content], { type: mimeType });
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(objectUrl);
      });
  }

  private getRole(): OperatorRole {
    const params = new URLSearchParams(window.location.search);
    const role = params.get('role');
    return role === 'viewer' ? 'viewer' : 'operator';
  }

  getUserId(): string {
    const params = new URLSearchParams(window.location.search);
    return params.get('userId') || localStorage.getItem('userId') || 'operator-user';
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.wsService.disconnect();
  }
}
