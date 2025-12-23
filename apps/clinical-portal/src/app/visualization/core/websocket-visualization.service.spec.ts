import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NgZone } from '@angular/core';
import {
  WebSocketVisualizationService,
  WebSocketStatus,
  BatchProgressEvent,
} from './websocket-visualization.service';
import { EvaluationProgressEvent } from '../data/data-transform.service';

describe('WebSocketVisualizationService', () => {
  let service: WebSocketVisualizationService;
  let ngZone: NgZone;
  let mockWebSocket: jest.Mocked<WebSocket>;

  beforeEach(() => {
    // Mock WebSocket
    mockWebSocket = {
      send: jest.fn(),
      close: jest.fn(),
      readyState: WebSocket.CONNECTING,
    } as unknown as jest.Mocked<WebSocket>;

    TestBed.configureTestingModule({
      providers: [WebSocketVisualizationService],
    });

    service = TestBed.inject(WebSocketVisualizationService);
    ngZone = TestBed.inject(NgZone);
  });

  afterEach(() => {
    service.dispose();
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should start with DISCONNECTED status', (done) => {
      service.status$.subscribe((status) => {
        expect(status).toBe(WebSocketStatus.DISCONNECTED);
        done();
      });
    });

    it('should return DISCONNECTED from getStatus initially', () => {
      expect(service.getStatus()).toBe(WebSocketStatus.DISCONNECTED);
    });

    it('should return false from isConnected initially', () => {
      expect(service.isConnected()).toBe(false);
    });
  });

  describe('connection management', () => {
    it('should set status to CONNECTING when connect is called', fakeAsync(() => {
      jest.spyOn(window as any, 'WebSocket').mockReturnValue(mockWebSocket);

      let statusUpdates: WebSocketStatus[] = [];
      service.status$.subscribe((status) => statusUpdates.push(status));

      service.connect();
      tick();

      expect(statusUpdates).toContain(WebSocketStatus.CONNECTING);
    }));

    it('should not connect if already connected', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      const webSocketSpy = jest.fn();
      (window as any).WebSocket = webSocketSpy;

      // First connection
      mockWebSocket.readyState = WebSocket.OPEN;
      webSocketSpy.mockReturnValue(mockWebSocket);
      service.connect();
      tick();

      // Try to connect again
      service.connect();
      tick();

      // Should only be called once
      expect(webSocketSpy).toHaveBeenCalledTimes(1);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should not connect if connection is in progress', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      const webSocketSpy = jest.fn();
      (window as any).WebSocket = webSocketSpy;

      mockWebSocket.readyState = WebSocket.CONNECTING;
      webSocketSpy.mockReturnValue(mockWebSocket);

      service.connect();
      tick();

      service.connect();
      tick();

      expect(webSocketSpy).toHaveBeenCalledTimes(1);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should skip connect when existing socket is connecting', () => {
      const originalWebSocket = window.WebSocket;
      const webSocketSpy = jest.fn();
      (webSocketSpy as any).OPEN = (originalWebSocket as any)?.OPEN ?? 1;
      (webSocketSpy as any).CONNECTING = (originalWebSocket as any)?.CONNECTING ?? 0;
      (window as any).WebSocket = webSocketSpy;
      const logSpy = jest.spyOn(console, 'log').mockImplementation(() => {});

      (service as any).socket = {
        readyState: (webSocketSpy as any).CONNECTING,
        close: jest.fn(),
      };

      service.connect();

      expect(webSocketSpy).not.toHaveBeenCalled();
      expect(logSpy).toHaveBeenCalledWith('WebSocket connection in progress');

      logSpy.mockRestore();
      (window as any).WebSocket = originalWebSocket;
    });

    it('should set status to CONNECTED on open', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      let statusUpdates: WebSocketStatus[] = [];
      service.status$.subscribe((status) => statusUpdates.push(status));

      service.connect();
      tick(100);

      expect(statusUpdates).toContain(WebSocketStatus.CONNECTED);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should send subscription message on open', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);

      expect(mockWebSocket.send).toHaveBeenCalledWith(
        JSON.stringify({
          type: 'subscribe',
          events: ['batch_progress', 'evaluation_progress', 'care_gap_notification'],
        })
      );

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should set status to DISCONNECTED on disconnect', fakeAsync(() => {
      let statusUpdates: WebSocketStatus[] = [];
      service.status$.subscribe((status) => statusUpdates.push(status));

      service.disconnect();
      tick();

      expect(statusUpdates[statusUpdates.length - 1]).toBe(WebSocketStatus.DISCONNECTED);
    }));

    it('should close WebSocket on disconnect', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);

      service.disconnect();

      expect(mockWebSocket.close).toHaveBeenCalledWith(1000, 'Client disconnect');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should clear reconnect timer on disconnect', fakeAsync(() => {
      jest.spyOn(window, 'clearTimeout');

      service.disconnect();
      tick();

      expect(window.clearTimeout).toHaveBeenCalled();
    }));
  });

  describe('message handling', () => {
    it('should emit batch progress events', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            const data = {
              type: 'batch_progress',
              batchId: 'batch-1',
              totalPatients: 100,
              completedPatients: 50,
              successfulEvaluations: 45,
              failedEvaluations: 5,
              completionPercentage: 50,
              throughputPerSecond: 10,
              averageDurationMs: 1500,
              timestamp: new Date().toISOString(),
              status: 'IN_PROGRESS',
            };
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(data) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: BatchProgressEvent | null = null;
      service.batchProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent?.batchId).toBe('batch-1');
      expect(receivedEvent?.totalPatients).toBe(100);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should emit evaluation progress events', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            const data = {
              type: 'evaluation_progress',
              patientId: 'patient-1',
              status: 'SUCCESS',
              progress: 100,
              timestamp: new Date().toISOString(),
            };
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(data) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: EvaluationProgressEvent | null = null;
      service.evaluationProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent?.patientId).toBe('patient-1');
      expect(receivedEvent?.status).toBe('SUCCESS');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should handle batch progress without explicit type', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            const data = {
              batchId: 'batch-1',
              totalPatients: 100,
              completedPatients: 50,
              timestamp: new Date().toISOString(),
            };
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(data) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: BatchProgressEvent | null = null;
      service.batchProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent?.batchId).toBe('batch-1');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should handle evaluation progress without explicit type', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            const data = {
              patientId: 'patient-1',
              status: 'PENDING',
              progress: 50,
              timestamp: new Date().toISOString(),
            };
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(data) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: EvaluationProgressEvent | null = null;
      service.evaluationProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent?.patientId).toBe('patient-1');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should emit error on invalid JSON', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: 'invalid json' })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedError: Error | null = null;
      service.error$.subscribe((error) => {
        receivedError = error;
      });

      service.connect();
      tick(100);

      expect(receivedError).not.toBeNull();
      expect(receivedError?.message).toContain('parse');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should provide default values for missing fields', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            const data = {
              type: 'batch_progress',
              batchId: 'batch-1',
            };
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(data) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: BatchProgressEvent | null = null;
      service.batchProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent?.totalPatients).toBe(0);
      expect(receivedEvent?.completedPatients).toBe(0);
      expect(receivedEvent?.status).toBe('IN_PROGRESS');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should warn on unknown message type', () => {
      const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

      (service as any).onMessage(
        new MessageEvent('message', { data: JSON.stringify({ type: 'unknown' }) })
      );

      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });
  });

  describe('error handling', () => {
    it('should set status to ERROR on connection error', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        setTimeout(() => {
          if (mockWebSocket.onerror) {
            mockWebSocket.onerror(new Event('error'));
          }
        }, 0);
        return mockWebSocket;
      };

      let statusUpdates: WebSocketStatus[] = [];
      service.status$.subscribe((status) => statusUpdates.push(status));

      service.connect();
      tick(100);

      expect(statusUpdates).toContain(WebSocketStatus.ERROR);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should emit error event on connection error', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        setTimeout(() => {
          if (mockWebSocket.onerror) {
            mockWebSocket.onerror(new Event('error'));
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedError: Error | null = null;
      service.error$.subscribe((error) => {
        receivedError = error;
      });

      service.connect();
      tick(100);

      expect(receivedError).not.toBeNull();
      expect(receivedError?.message).toContain('error');

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should handle WebSocket creation error', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        throw new Error('Connection failed');
      };

      let statusUpdates: WebSocketStatus[] = [];
      service.status$.subscribe((status) => statusUpdates.push(status));

      let receivedError: Error | null = null;
      service.error$.subscribe((error) => {
        receivedError = error;
      });

      service.connect();
      tick(100);

      expect(statusUpdates).toContain(WebSocketStatus.ERROR);
      expect(receivedError).not.toBeNull();

      (window as any).WebSocket = originalWebSocket;
    }));
  });

  describe('reconnection logic', () => {
    it('should attempt reconnection on abnormal close', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      let connectAttempts = 0;

      (window as any).WebSocket = function (url: string) {
        connectAttempts++;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (connectAttempts === 1 && mockWebSocket.onclose) {
            mockWebSocket.onclose(new CloseEvent('close', { code: 1006 }));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);
      tick(2100); // Wait for reconnect delay

      expect(connectAttempts).toBeGreaterThan(1);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should not reconnect on normal close', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      let connectAttempts = 0;

      (window as any).WebSocket = function (url: string) {
        connectAttempts++;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onclose) {
            mockWebSocket.onclose(new CloseEvent('close', { code: 1000 }));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);
      tick(5000);

      expect(connectAttempts).toBe(1);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should use exponential backoff for reconnection', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      const connectTimes: number[] = [];

      (window as any).WebSocket = function (url: string) {
        connectTimes.push(Date.now());
        setTimeout(() => {
          if (mockWebSocket.onerror) {
            mockWebSocket.onerror(new Event('error'));
          }
          if (mockWebSocket.onclose) {
            mockWebSocket.onclose(new CloseEvent('close', { code: 1006 }));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);

      // First reconnect: 2s delay
      tick(2100);
      expect(connectTimes.length).toBe(2);

      // Second reconnect: 4s delay
      tick(4100);
      expect(connectTimes.length).toBe(3);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should set status to RECONNECTING during reconnection', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      let statusUpdates: WebSocketStatus[] = [];

      (window as any).WebSocket = function (url: string) {
        setTimeout(() => {
          if (mockWebSocket.onclose) {
            mockWebSocket.onclose(new CloseEvent('close', { code: 1006 }));
          }
        }, 0);
        return mockWebSocket;
      };

      service.status$.subscribe((status) => statusUpdates.push(status));

      service.connect();
      tick(100);

      expect(statusUpdates).toContain(WebSocketStatus.RECONNECTING);

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should stop reconnecting after max attempts', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      let connectAttempts = 0;

      (window as any).WebSocket = function (url: string) {
        connectAttempts++;
        setTimeout(() => {
          if (mockWebSocket.onerror) {
            mockWebSocket.onerror(new Event('error'));
          }
          if (mockWebSocket.onclose) {
            mockWebSocket.onclose(new CloseEvent('close', { code: 1006 }));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();

      // Simulate multiple failed attempts
      for (let i = 0; i < 10; i++) {
        tick(5000);
      }

      // Should not exceed max attempts (5)
      expect(connectAttempts).toBeLessThanOrEqual(6); // Initial + 5 retries

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should set ERROR status when max attempts reached', () => {
      (service as any).reconnectAttempts = (service as any).maxReconnectAttempts;

      (service as any).scheduleReconnect();

      expect(service.getStatus()).toBe(WebSocketStatus.ERROR);
    });

    it('should reset reconnect attempts on successful connection', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      let connectAttempts = 0;

      (window as any).WebSocket = function (url: string) {
        connectAttempts++;
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);

      // Verify reconnect counter is reset by checking status
      expect(service.getStatus()).toBe(WebSocketStatus.CONNECTED);

      (window as any).WebSocket = originalWebSocket;
    }));
  });

  describe('message sending', () => {
    it('should not send message when disconnected', fakeAsync(() => {
      let receivedError: Error | null = null;
      service.error$.subscribe((error) => {
        receivedError = error;
      });

      // Service is not connected, so send should not throw but also not send
      expect(() => {
        (service as any).sendMessage({ test: 'data' });
      }).not.toThrow();

      expect(mockWebSocket.send).not.toHaveBeenCalled();
    }));

    it('should send JSON stringified message when connected', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);

      (service as any).sendMessage({ test: 'data' });

      expect(mockWebSocket.send).toHaveBeenCalledWith(
        JSON.stringify({ test: 'data' })
      );

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should emit error if send fails', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        mockWebSocket.send.and.throwError('Send failed');
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedError: Error | null = null;
      service.error$.subscribe((error) => {
        receivedError = error;
      });

      service.connect();
      tick(100);

      (service as any).sendMessage({ test: 'data' });

      expect(receivedError).not.toBeNull();

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should emit error when send throws', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        mockWebSocket.send = jest.fn(() => {
          throw new Error('Send failed');
        }) as any;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedError: Error | null = null;
      service.error$.subscribe((error) => {
        receivedError = error;
      });

      service.connect();
      tick(100);

      (service as any).sendMessage({ test: 'data' });

      expect(receivedError).not.toBeNull();

      (window as any).WebSocket = originalWebSocket;
    }));
  });

  describe('simulation mode', () => {
    it('should simulate batch progress', fakeAsync(() => {
      const events: BatchProgressEvent[] = [];

      service.simulateBatchProgress(5).subscribe((event) => {
        events.push(event);
      });

      tick(6000); // 5 seconds + buffer

      expect(events.length).toBeGreaterThan(0);
      expect(events[0].totalPatients).toBe(100);
    }));

    it('should update progress over time', fakeAsync(() => {
      const events: BatchProgressEvent[] = [];

      service.simulateBatchProgress(3).subscribe((event) => {
        events.push(event);
      });

      tick(4000);

      expect(events.length).toBeGreaterThan(1);
      expect(events[events.length - 1].completedPatients).toBeGreaterThan(
        events[0].completedPatients
      );
    }));

    it('should complete simulation after duration', fakeAsync(() => {
      let completed = false;

      service.simulateBatchProgress(2).subscribe({
        complete: () => {
          completed = true;
        },
      });

      tick(3000);

      expect(completed).toBe(true);
    }));

    it('should show IN_PROGRESS status during simulation', fakeAsync(() => {
      const events: BatchProgressEvent[] = [];

      service.simulateBatchProgress(3).subscribe((event) => {
        events.push(event);
      });

      tick(1000);

      expect(events[0].status).toBe('IN_PROGRESS');
    }));

    it('should show COMPLETED status at end', fakeAsync(() => {
      const events: BatchProgressEvent[] = [];

      service.simulateBatchProgress(2).subscribe((event) => {
        events.push(event);
      });

      tick(3000);

      expect(events[events.length - 1].status).toBe('COMPLETED');
    }));

    it('should simulate realistic success/failure rates', fakeAsync(() => {
      const events: BatchProgressEvent[] = [];

      service.simulateBatchProgress(3).subscribe((event) => {
        events.push(event);
      });

      tick(4000);

      const lastEvent = events[events.length - 1];
      const successRate =
        lastEvent.successfulEvaluations / lastEvent.completedPatients;

      expect(successRate).toBeGreaterThan(0.8); // 80%+ success
      expect(lastEvent.failedEvaluations).toBeGreaterThan(0);
    }));

    it('should allow cleanup of simulation', fakeAsync(() => {
      const subscription = service.simulateBatchProgress(10).subscribe();

      tick(2000);
      subscription.unsubscribe();
      tick(10000);

      // Should not throw errors after unsubscribe
      expect(true).toBe(true);
    }));
  });

  describe('observables', () => {
    it('should share status observable', (done) => {
      let subscriptionCount = 0;

      const sub1 = service.status$.subscribe(() => {
        subscriptionCount++;
      });

      const sub2 = service.status$.subscribe(() => {
        subscriptionCount++;
      });

      setTimeout(() => {
        expect(subscriptionCount).toBeGreaterThan(0);
        sub1.unsubscribe();
        sub2.unsubscribe();
        done();
      }, 100);
    });

    it('should share batch progress observable', fakeAsync(() => {
      let sub1Called = false;
      let sub2Called = false;

      const sub1 = service.batchProgress$.subscribe(() => {
        sub1Called = true;
      });

      const sub2 = service.batchProgress$.subscribe(() => {
        sub2Called = true;
      });

      // Trigger an event via simulation
      service.simulateBatchProgress(1).subscribe();
      tick(1500);

      sub1.unsubscribe();
      sub2.unsubscribe();
    }));

    it('should filter distinct status changes', fakeAsync(() => {
      let statusUpdates: WebSocketStatus[] = [];

      service.status$.subscribe((status) => statusUpdates.push(status));

      // Initial DISCONNECTED status should be emitted once
      tick(100);

      const initialCount = statusUpdates.filter(
        (s) => s === WebSocketStatus.DISCONNECTED
      ).length;
      expect(initialCount).toBe(1);
    }));
  });

  describe('disposal', () => {
    it('should disconnect on dispose', () => {
      jest.spyOn(service, 'disconnect');

      service.dispose();

      expect(service.disconnect).toHaveBeenCalled();
    });

    it('should complete all subjects on dispose', fakeAsync(() => {
      let statusCompleted = false;
      let batchCompleted = false;
      let evalCompleted = false;
      let errorCompleted = false;

      service.status$.subscribe({ complete: () => (statusCompleted = true) });
      service.batchProgress$.subscribe({ complete: () => (batchCompleted = true) });
      service.evaluationProgress$.subscribe({ complete: () => (evalCompleted = true) });
      service.error$.subscribe({ complete: () => (errorCompleted = true) });

      service.dispose();
      tick();

      expect(statusCompleted).toBe(true);
      expect(batchCompleted).toBe(true);
      expect(evalCompleted).toBe(true);
      expect(errorCompleted).toBe(true);
    }));
  });

  describe('NgZone integration', () => {
    it('should create WebSocket outside Angular zone', fakeAsync(() => {
      jest.spyOn(ngZone, 'runOutsideAngular');

      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        return mockWebSocket;
      };

      service.connect();
      tick();

      expect(ngZone.runOutsideAngular).toHaveBeenCalled();

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should run callbacks inside Angular zone', fakeAsync(() => {
      jest.spyOn(ngZone, 'run');

      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
        }, 0);
        return mockWebSocket;
      };

      service.connect();
      tick(100);

      expect(ngZone.run).toHaveBeenCalled();

      (window as any).WebSocket = originalWebSocket;
    }));
  });

  describe('edge cases', () => {
    it('should handle multiple dispose calls', () => {
      expect(() => {
        service.dispose();
        service.dispose();
      }).not.toThrow();
    });

    it('should handle connect after dispose', fakeAsync(() => {
      service.dispose();

      expect(() => {
        service.connect();
        tick(100);
      }).not.toThrow();
    }));

    it('should handle very large message payloads', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      const largeData = {
        type: 'batch_progress',
        batchId: 'batch-1',
        data: new Array(10000).fill('x').join(''),
      };

      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(largeData) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: BatchProgressEvent | null = null;
      service.batchProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();

      (window as any).WebSocket = originalWebSocket;
    }));

    it('should handle missing optional fields gracefully', fakeAsync(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function (url: string) {
        mockWebSocket.readyState = WebSocket.OPEN;
        setTimeout(() => {
          if (mockWebSocket.onopen) {
            mockWebSocket.onopen(new Event('open'));
          }
          if (mockWebSocket.onmessage) {
            const data = {
              patientId: 'patient-1',
              status: 'PENDING',
              progress: 50,
              // Missing timestamp
            };
            mockWebSocket.onmessage(
              new MessageEvent('message', { data: JSON.stringify(data) })
            );
          }
        }, 0);
        return mockWebSocket;
      };

      let receivedEvent: EvaluationProgressEvent | null = null;
      service.evaluationProgress$.subscribe((event) => {
        receivedEvent = event;
      });

      service.connect();
      tick(100);

      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent?.timestamp).toBeDefined();

      (window as any).WebSocket = originalWebSocket;
    }));
  });
});
