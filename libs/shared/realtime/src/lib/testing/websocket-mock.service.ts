import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import {
  WebSocketMessage,
  ConnectionState,
  ConnectionStatus
} from '../models/websocket-message.model';

/**
 * Mock WebSocket Service for Testing
 *
 * Simulates WebSocket behavior without actual network connections.
 * Useful for testing components that depend on WebSocket service.
 *
 * Usage in tests:
 * ```typescript
 * TestBed.configureTestingModule({
 *   providers: [
 *     { provide: WebSocketService, useClass: MockWebSocketService }
 *   ]
 * });
 * ```
 */
export class MockWebSocketService {
  private messageSubject = new Subject<WebSocketMessage>();
  private statusSubject = new BehaviorSubject<ConnectionStatus>({
    state: ConnectionState.DISCONNECTED,
    retryCount: 0
  });

  public messages$ = this.messageSubject.asObservable();
  public connectionStatus$ = this.statusSubject.asObservable();
  public isConnected = false;

  /**
   * Connect to a mock WebSocket
   */
  connect(endpoint: string, token: string): void {
    this.isConnected = true;
    this.statusSubject.next({
      state: ConnectionState.CONNECTED,
      retryCount: 0,
      sessionId: 'mock-session-id'
    });
  }

  /**
   * Disconnect from mock WebSocket
   */
  disconnect(): void {
    this.isConnected = false;
    this.statusSubject.next({
      state: ConnectionState.DISCONNECTED,
      retryCount: 0
    });
  }

  /**
   * Send a message (no-op for mock)
   */
  send(message: WebSocketMessage): void {
    // No-op for mock
  }

  /**
   * Get messages of a specific type
   */
  ofType<T extends WebSocketMessage>(type: string): Observable<T> {
    return this.messages$.pipe(
      filter(msg => msg.type === type),
      map(msg => msg as T)
    );
  }

  /**
   * Get messages for a specific tenant
   */
  forTenant(tenantId: string): Observable<WebSocketMessage> {
    return this.messages$.pipe(
      filter(msg => !msg.tenantId || msg.tenantId === tenantId)
    );
  }

  ngOnDestroy(): void {
    this.messageSubject.complete();
    this.statusSubject.complete();
  }

  // Test helper methods

  /**
   * Simulate receiving a message (test helper)
   */
  simulateMessage(message: WebSocketMessage): void {
    this.messageSubject.next(message);
  }

  /**
   * Simulate disconnect (test helper)
   */
  simulateDisconnect(): void {
    this.isConnected = false;
    this.statusSubject.next({
      state: ConnectionState.DISCONNECTED,
      retryCount: 0
    });
  }

  /**
   * Simulate error (test helper)
   */
  simulateError(error: string): void {
    this.isConnected = false;
    this.statusSubject.next({
      state: ConnectionState.ERROR,
      lastError: error,
      retryCount: 0
    });
  }

  /**
   * Simulate reconnecting (test helper)
   */
  simulateReconnecting(retryCount: number): void {
    this.statusSubject.next({
      state: ConnectionState.RECONNECTING,
      retryCount
    });
  }
}
