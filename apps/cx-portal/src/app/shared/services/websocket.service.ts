import { Injectable } from '@angular/core';
import { Observable, Subject, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { retry, tap, delayWhen } from 'rxjs/operators';

export interface WebSocketMessage {
  type: 'pipeline_update' | 'lead_created' | 'lead_updated' | 'lead_deleted';
  data: any;
  timestamp: string;
}

@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  private socket$: WebSocketSubject<WebSocketMessage> | null = null;
  private messagesSubject$ = new Subject<WebSocketMessage>();
  public messages$ = this.messagesSubject$.asObservable();

  private readonly WS_URL = 'ws://localhost:8201/ws/pipeline';

  connect(): void {
    if (!this.socket$ || this.socket$.closed) {
      this.socket$ = webSocket<WebSocketMessage>({
        url: this.WS_URL,
        openObserver: {
          next: () => {
            console.log('WebSocket connected');
          },
        },
        closeObserver: {
          next: () => {
            console.log('WebSocket disconnected');
          },
        },
      });

      this.socket$
        .pipe(
          tap((msg) => console.log('WebSocket message received:', msg)),
          retry({
            delay: (error, retryCount) => {
              console.error(`WebSocket error (retry ${retryCount}):`, error);
              // Exponential backoff: 1s, 2s, 4s, 8s, max 30s
              const delay = Math.min(1000 * Math.pow(2, retryCount - 1), 30000);
              return timer(delay);
            },
          })
        )
        .subscribe({
          next: (msg) => this.messagesSubject$.next(msg),
          error: (err) => console.error('WebSocket error:', err),
        });
    }
  }

  disconnect(): void {
    if (this.socket$) {
      this.socket$.complete();
      this.socket$ = null;
    }
  }

  send(message: WebSocketMessage): void {
    if (this.socket$) {
      this.socket$.next(message);
    }
  }
}
