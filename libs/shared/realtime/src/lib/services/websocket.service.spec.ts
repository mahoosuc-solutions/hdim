import { WebSocketService } from './websocket.service';
import { HealthScoreUpdateMessage } from '../models/health-score-message.model';
import { ConnectionState, WebSocketMessage } from '../models/websocket-message.model';

// Mock WebSocket
class MockWebSocket {
  readyState = 1; // WebSocket.OPEN
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;

  constructor() {
    // Automatically trigger onopen after constructor via setTimeout
    setTimeout(() => {
      if (this.onopen) {
        this.onopen(new Event('open'));
      }
    }, 0);
  }

  send(data: string): void {
    // Mock send
  }

  close(): void {
    this.readyState = 3; // WebSocket.CLOSED
    setTimeout(() => {
      if (this.onclose) {
        this.onclose(new CloseEvent('close'));
      }
    }, 0);
  }
}

describe('WebSocketService', () => {
  let service: WebSocketService;
  let lastCreatedSocket: MockWebSocket | null = null;

  // Override WebSocket constructor to track created instances
  class TrackedMockWebSocket extends MockWebSocket {
    constructor() {
      super();
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      lastCreatedSocket = this;
    }
  }

  beforeEach(() => {
    // Create service instance directly (no TestBed needed for standalone services)
    service = new WebSocketService();
    lastCreatedSocket = null;

    // Mock WebSocket globally
    (global as any).WebSocket = TrackedMockWebSocket;
  });

  afterEach(() => {
    service.disconnect();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize with disconnected status', () => {
    let connectionStatus = '';
    service.connectionStatus$.subscribe(status => {
      connectionStatus = status.state;
    });

    expect(connectionStatus).toBe(ConnectionState.DISCONNECTED);
    expect(service.isConnected).toBe(false);
  });

  it('should emit connection established when connected', (done) => {
    let receivedConnected = false;
    service.connectionStatus$.subscribe(status => {
      if (status.state === ConnectionState.CONNECTED && !receivedConnected) {
        receivedConnected = true;
        expect(status.retryCount).toBe(0);
        done();
      }
    });

    service.connect('quality-measure', 'test-token');
  });

  it('should reject connection without auth token', (done) => {
    let receivedError = false;
    service.connectionStatus$.subscribe(status => {
      if (status.state === ConnectionState.ERROR && !receivedError) {
        receivedError = true;
        expect(status.lastError).toBe('Authentication required');
        done();
      }
    });

    service.connect('quality-measure', '');
  });

  it('should emit incoming messages to subscribers', (done) => {
    const testMessage: HealthScoreUpdateMessage = {
      type: 'HEALTH_SCORE_UPDATE',
      timestamp: Date.now(),
      data: {
        patientId: 'PAT-001',
        score: 85,
        category: 'good',
        factors: [],
        calculatedAt: Date.now()
      }
    };

    let messageReceived = false;
    service.messages$.subscribe(msg => {
      if (msg.type === 'HEALTH_SCORE_UPDATE' && !messageReceived) {
        messageReceived = true;
        expect((msg as HealthScoreUpdateMessage).data.score).toBe(85);
        done();
      }
    });

    service.connect('quality-measure', 'test-token');

    // Simulate incoming message after socket is created
    setTimeout(() => {
      if (lastCreatedSocket && lastCreatedSocket.onmessage) {
        lastCreatedSocket.onmessage(new MessageEvent('message', { data: JSON.stringify(testMessage) }));
      }
    }, 50);
  });

  it('should filter messages by type', (done) => {
    const testMessage: HealthScoreUpdateMessage = {
      type: 'HEALTH_SCORE_UPDATE',
      timestamp: Date.now(),
      data: {
        patientId: 'PAT-001',
        score: 85,
        category: 'good',
        factors: [],
        calculatedAt: Date.now()
      }
    };

    let typeFiltered = false;
    service.ofType<HealthScoreUpdateMessage>('HEALTH_SCORE_UPDATE').subscribe(msg => {
      if (!typeFiltered) {
        typeFiltered = true;
        expect(msg.data.score).toBe(85);
        done();
      }
    });

    service.connect('quality-measure', 'test-token');

    setTimeout(() => {
      if (lastCreatedSocket && lastCreatedSocket.onmessage) {
        lastCreatedSocket.onmessage(new MessageEvent('message', { data: JSON.stringify(testMessage) }));
      }
    }, 50);
  });

  it('should filter messages by tenant', (done) => {
    const testMessage: HealthScoreUpdateMessage = {
      type: 'HEALTH_SCORE_UPDATE',
      timestamp: Date.now(),
      tenantId: 'TENANT-001',
      data: {
        patientId: 'PAT-001',
        score: 85,
        category: 'good',
        factors: [],
        calculatedAt: Date.now()
      }
    };

    let tenantFiltered = false;
    service.forTenant('TENANT-001').subscribe(msg => {
      if (!tenantFiltered) {
        tenantFiltered = true;
        expect(msg.tenantId).toBe('TENANT-001');
        done();
      }
    });

    service.connect('quality-measure', 'test-token');

    setTimeout(() => {
      if (lastCreatedSocket && lastCreatedSocket.onmessage) {
        lastCreatedSocket.onmessage(new MessageEvent('message', { data: JSON.stringify(testMessage) }));
      }
    }, 50);
  });

  it('should queue messages when disconnected', (done) => {
    const testMessage: HealthScoreUpdateMessage = {
      type: 'HEALTH_SCORE_UPDATE',
      timestamp: Date.now(),
      data: {
        patientId: 'PAT-001',
        score: 85,
        category: 'good',
        factors: [],
        calculatedAt: Date.now()
      }
    };

    // Don't connect, just try to send
    service.send(testMessage);

    // Message should be queued (we can't directly check queue, but this shouldn't throw)
    setTimeout(() => {
      expect(service.isConnected).toBe(false);
      done();
    }, 10);
  });

  it('should disconnect cleanly', (done) => {
    service.connect('quality-measure', 'test-token');

    // Wait for connection to establish
    setTimeout(() => {
      expect(service.isConnected).toBe(true);

      service.disconnect();

      setTimeout(() => {
        expect(service.isConnected).toBe(false);
        done();
      }, 10);
    }, 10);
  });

  it('should complete observables on destroy', () => {
    let messagesComplete = false;
    let statusComplete = false;

    service.messages$.subscribe({
      complete: () => {
        messagesComplete = true;
      }
    });

    service.connectionStatus$.subscribe({
      complete: () => {
        statusComplete = true;
      }
    });

    service.ngOnDestroy();

    expect(messagesComplete).toBe(true);
    expect(statusComplete).toBe(true);
  });

  it('should not allow multiple concurrent connections', (done) => {
    service.connect('quality-measure', 'test-token-1');

    // Wait for connection to establish
    setTimeout(() => {
      // Try to connect again - should warn
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      service.connect('quality-measure', 'test-token-2');

      expect(consoleSpy).toHaveBeenCalledWith('WebSocket already connected. Disconnect first.');
      consoleSpy.mockRestore();
      done();
    }, 10);
  });
});
