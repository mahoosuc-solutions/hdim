import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebSocketService, ConnectionState } from '@health-platform/shared/realtime';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Connection Status Component
 *
 * Displays real-time WebSocket connection status in the shell-app header.
 * Shows connection state (Connected, Reconnecting, Error) with visual indicators.
 *
 * ★ Insight ─────────────────────────────────────
 * This component leverages RxJS takeUntil pattern for automatic subscription
 * cleanup on destroy, preventing memory leaks. The component subscribes to
 * connectionStatus$ Observable from WebSocketService for reactive status updates.
 * ─────────────────────────────────────────────────
 */
@Component({
  selector: 'app-connection-status',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="connection-status-indicator" [ngClass]="statusClass">
      <span class="connection-status-badge" [ngClass]="badgeClass"></span>
      <span class="connection-status-text">{{ statusText }}</span>
      <span *ngIf="retryCount > 0" class="retry-count">(Retry {{ retryCount }})</span>
    </div>
  `,
  styles: [`
    .connection-status-indicator {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.25rem 0.75rem;
      border-radius: 4px;
      font-size: 0.875rem;
      font-weight: 500;
      background: rgba(255, 255, 255, 0.1);
      transition: all 0.3s ease;
    }

    .connection-status-badge {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      animation: pulse 2s infinite;
    }

    .badge-success {
      background: #4caf50;
    }

    .badge-warning {
      background: #ff9800;
      animation: pulse-warning 1s infinite;
    }

    .badge-error {
      background: #f44336;
      animation: pulse-error 0.5s infinite;
    }

    .status-connected {
      color: #4caf50;
    }

    .status-reconnecting {
      color: #ff9800;
    }

    .status-error {
      color: #f44336;
    }

    .retry-count {
      font-size: 0.75rem;
      opacity: 0.8;
    }

    @keyframes pulse {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.5;
      }
    }

    @keyframes pulse-warning {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.6;
      }
    }

    @keyframes pulse-error {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.3;
      }
    }
  `],
})
export class ConnectionStatusComponent implements OnInit, OnDestroy {
  statusText = 'Connecting...';
  statusClass = '';
  badgeClass = '';
  retryCount = 0;

  private destroy$ = new Subject<void>();

  constructor(private websocket: WebSocketService) {}

  ngOnInit(): void {
    // Subscribe to connection status updates
    this.websocket.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe((status) => {
        this.updateStatusDisplay(status.state);
        this.retryCount = status.retryCount;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateStatusDisplay(state: ConnectionState): void {
    switch (state) {
      case ConnectionState.CONNECTED:
        this.statusText = 'Connected';
        this.statusClass = 'status-connected';
        this.badgeClass = 'badge-success';
        break;
      case ConnectionState.CONNECTING:
        this.statusText = 'Connecting...';
        this.statusClass = 'status-connecting';
        this.badgeClass = 'badge-warning';
        break;
      case ConnectionState.RECONNECTING:
        this.statusText = 'Reconnecting...';
        this.statusClass = 'status-reconnecting';
        this.badgeClass = 'badge-warning';
        break;
      case ConnectionState.DISCONNECTED:
        this.statusText = 'Disconnected';
        this.statusClass = 'status-error';
        this.badgeClass = 'badge-error';
        break;
      case ConnectionState.ERROR:
        this.statusText = 'Error';
        this.statusClass = 'status-error';
        this.badgeClass = 'badge-error';
        break;
      default:
        this.statusText = 'Unknown';
        this.statusClass = '';
        this.badgeClass = 'badge-warning';
    }
  }
}
