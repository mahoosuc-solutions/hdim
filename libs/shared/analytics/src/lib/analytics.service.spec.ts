import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService, AnalyticsEvent } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalyticsService]
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    service.clear();
  });

  describe('Event Recording', () => {
    it('should record analytics event', (done) => {
      let eventRecorded = false;

      service.events$.subscribe(event => {
        if (event.eventName === 'test_event') {
          eventRecorded = true;
          expect(event.userId).toBeDefined();
          expect(event.tenantId).toBeDefined();
          expect(event.sessionId).toBeDefined();
        }
      });

      service.recordEvent('test_event', { value: 123 });

      setTimeout(() => {
        expect(eventRecorded).toBe(true);
        done();
      }, 50);
    });

    it('should include metadata in event', (done) => {
      const metadata = { key1: 'value1', key2: 42 };
      let metadataMatches = false;

      service.events$.subscribe(event => {
        if (event.eventName === 'test_event') {
          metadataMatches = JSON.stringify(event.metadata) === JSON.stringify(metadata);
        }
      });

      service.recordEvent('test_event', metadata);

      setTimeout(() => {
        expect(metadataMatches).toBe(true);
        done();
      }, 50);
    });

    it('should set correct event category', (done) => {
      let categoryCorrect = false;

      service.events$.subscribe(event => {
        if (event.eventName === 'ws_connected') {
          categoryCorrect = event.category === 'connection';
        }
      });

      service.recordEvent('ws_connected', {}, 'connection');

      setTimeout(() => {
        expect(categoryCorrect).toBe(true);
        done();
      }, 50);
    });

    it('should include timestamp in event', (done) => {
      let timestampValid = false;
      const beforeTime = Date.now();

      service.events$.subscribe(event => {
        if (event.eventName === 'test_event') {
          const afterTime = Date.now();
          timestampValid = event.timestamp >= beforeTime && event.timestamp <= afterTime;
        }
      });

      service.recordEvent('test_event');

      setTimeout(() => {
        expect(timestampValid).toBe(true);
        done();
      }, 50);
    });

    it('should include correlation ID when provided', (done) => {
      const correlationId = 'test-correlation-123';
      let correlationMatches = false;

      service.events$.subscribe(event => {
        if (event.eventName === 'test_event') {
          correlationMatches = event.correlationId === correlationId;
        }
      });

      service.recordEvent('test_event', {}, 'engagement', correlationId);

      setTimeout(() => {
        expect(correlationMatches).toBe(true);
        done();
      }, 50);
    });
  });

  describe('Event Batching', () => {
    it('should auto-flush batch when size reached', (done) => {
      let batchFlushed = false;

      service.batches$.subscribe(batch => {
        batchFlushed = true;
        expect(batch.events.length).toBe(10);
      });

      // Record 10 events (default batch size)
      for (let i = 0; i < 10; i++) {
        service.recordEvent(`event_${i}`);
      }

      // Should trigger HTTP request and batch emission
      const req = httpMock.expectOne('/api/analytics/events');
      expect(req.request.method).toBe('POST');
      req.flush({});

      setTimeout(() => {
        expect(batchFlushed).toBe(true);
        done();
      }, 100);
    });

    it('should flush batch manually', (done) => {
      let batchFlushed = false;

      service.batches$.subscribe(batch => {
        batchFlushed = true;
        expect(batch.events.length).toBe(3);
      });

      // Record 3 events
      service.recordEvent('event_1');
      service.recordEvent('event_2');
      service.recordEvent('event_3');

      // Manually flush
      service.flushBatch();

      const req = httpMock.expectOne('/api/analytics/events');
      req.flush({});

      setTimeout(() => {
        expect(batchFlushed).toBe(true);
        done();
      }, 100);
    });

    it('should not flush empty batch', () => {
      service.flushBatch();
      httpMock.expectNone('/api/analytics/events');
    });

    it('should return current batch', () => {
      service.recordEvent('event_1');
      service.recordEvent('event_2');

      const batch = service.getCurrentBatch();
      expect(batch.length).toBe(2);
      expect(batch[0].eventName).toBe('event_1');
      expect(batch[1].eventName).toBe('event_2');
    });

    it('should track batch queue', () => {
      service.recordEvent('event_1');
      service.flushBatch();

      const req = httpMock.expectOne('/api/analytics/events');
      req.flush({});

      const queue = service.getBatchQueue();
      expect(queue.length).toBe(1);
      expect(queue[0].events.length).toBe(1);
    });
  });

  describe('User and Tenant Management', () => {
    it('should set and get user ID', () => {
      service.setUserId('user-123');
      expect(service.getUserId()).toBe('user-123');
    });

    it('should set and get tenant ID', () => {
      service.setTenantId('tenant-456');
      expect(service.getTenantId()).toBe('tenant-456');
    });

    it('should use set user ID in events', (done) => {
      const userId = 'user-789';
      service.setUserId(userId);

      let eventUserId = '';
      service.events$.subscribe(event => {
        eventUserId = event.userId;
      });

      service.recordEvent('test_event');

      setTimeout(() => {
        expect(eventUserId).toBe(userId);
        done();
      }, 50);
    });

    it('should use set tenant ID in events', (done) => {
      const tenantId = 'tenant-999';
      service.setTenantId(tenantId);

      let eventTenantId = '';
      service.events$.subscribe(event => {
        eventTenantId = event.tenantId;
      });

      service.recordEvent('test_event');

      setTimeout(() => {
        expect(eventTenantId).toBe(tenantId);
        done();
      }, 50);
    });

    it('should generate unique session ID', () => {
      const sessionId = service.getSessionId();
      expect(sessionId).toBeTruthy();
      expect(sessionId.length).toBeGreaterThan(0);
    });
  });

  describe('Configuration', () => {
    it('should set batch size configuration', () => {
      service.setConfig({ batchSize: 5 });
      const config = service.getConfig();
      expect(config.batchSize).toBe(5);
    });

    it('should set flush interval configuration', () => {
      service.setConfig({ flushInterval: 3000 });
      const config = service.getConfig();
      expect(config.flushInterval).toBe(3000);
    });

    it('should set API endpoint configuration', () => {
      service.setConfig({ apiEndpoint: '/custom/analytics' });
      const config = service.getConfig();
      expect(config.apiEndpoint).toBe('/custom/analytics');
    });

    it('should disable analytics', (done) => {
      service.setConfig({ enabled: false });
      let eventRecorded = false;

      service.events$.subscribe(() => {
        eventRecorded = true;
      });

      service.recordEvent('test_event');

      setTimeout(() => {
        expect(eventRecorded).toBe(false);
        done();
      }, 50);
    });

    it('should get current configuration', () => {
      const config = service.getConfig();
      expect(config.batchSize).toBeGreaterThan(0);
      expect(config.flushInterval).toBeGreaterThan(0);
      expect(config.enabled).toBe(true);
    });
  });

  describe('Auto-flush Interval', () => {
    it('should auto-flush on interval', (done) => {
      service.setConfig({ flushInterval: 100 });
      let batchFlushed = false;

      service.batches$.subscribe(() => {
        batchFlushed = true;
      });

      // Record 1 event (less than batch size)
      service.recordEvent('event_1');

      // Wait for auto-flush interval
      setTimeout(() => {
        const req = httpMock.expectOne('/api/analytics/events');
        req.flush({});

        setTimeout(() => {
          expect(batchFlushed).toBe(true);
          done();
        }, 50);
      }, 150);
    });
  });

  describe('Error Handling', () => {
    it('should handle batch flush errors', (done) => {
      service.recordEvent('event_1');
      service.flushBatch();

      const req = httpMock.expectOne('/api/analytics/events');
      req.error(new ErrorEvent('Network error'));

      setTimeout(() => {
        // Event should be re-added to current batch
        const batch = service.getCurrentBatch();
        expect(batch.length).toBe(1);
        done();
      }, 100);
    });
  });

  describe('Observables', () => {
    it('should emit events observable', (done) => {
      let emissionCount = 0;

      service.events$.subscribe(() => {
        emissionCount++;
      });

      service.recordEvent('event_1');
      service.recordEvent('event_2');

      setTimeout(() => {
        expect(emissionCount).toBeGreaterThanOrEqual(2);
        done();
      }, 100);
    });

    it('should emit batches observable on flush', (done) => {
      let batchEmitted = false;

      service.batches$.subscribe(batch => {
        batchEmitted = true;
        expect(batch.id).toBeTruthy();
        expect(batch.events).toBeDefined();
      });

      service.recordEvent('event_1');
      service.flushBatch();

      const req = httpMock.expectOne('/api/analytics/events');
      req.flush({});

      setTimeout(() => {
        expect(batchEmitted).toBe(true);
        done();
      }, 100);
    });
  });

  describe('Clear and Reset', () => {
    it('should clear all events and batches', () => {
      service.recordEvent('event_1');
      service.recordEvent('event_2');

      let currentBatch = service.getCurrentBatch();
      expect(currentBatch.length).toBe(2);

      service.clear();

      currentBatch = service.getCurrentBatch();
      expect(currentBatch.length).toBe(0);

      const queue = service.getBatchQueue();
      expect(queue.length).toBe(0);
    });
  });

  describe('Multiple Event Categories', () => {
    it('should record connection events', (done) => {
      let eventCategory = '';

      service.events$.subscribe(event => {
        if (event.eventName === 'ws_connected') {
          eventCategory = event.category;
        }
      });

      service.recordEvent('ws_connected', {}, 'connection');

      setTimeout(() => {
        expect(eventCategory).toBe('connection');
        done();
      }, 50);
    });

    it('should record notification events', (done) => {
      let eventCategory = '';

      service.events$.subscribe(event => {
        if (event.eventName === 'notification_shown') {
          eventCategory = event.category;
        }
      });

      service.recordEvent('notification_shown', {}, 'notification');

      setTimeout(() => {
        expect(eventCategory).toBe('notification');
        done();
      }, 50);
    });

    it('should record performance events', (done) => {
      let eventCategory = '';

      service.events$.subscribe(event => {
        if (event.eventName === 'latency_spike') {
          eventCategory = event.category;
        }
      });

      service.recordEvent('latency_spike', {}, 'performance');

      setTimeout(() => {
        expect(eventCategory).toBe('performance');
        done();
      }, 50);
    });

    it('should record engagement events', (done) => {
      let eventCategory = '';

      service.events$.subscribe(event => {
        if (event.eventName === 'feature_used') {
          eventCategory = event.category;
        }
      });

      service.recordEvent('feature_used', {}, 'engagement');

      setTimeout(() => {
        expect(eventCategory).toBe('engagement');
        done();
      }, 50);
    });

    it('should record error events', (done) => {
      let eventCategory = '';

      service.events$.subscribe(event => {
        if (event.eventName === 'api_error') {
          eventCategory = event.category;
        }
      });

      service.recordEvent('api_error', {}, 'error');

      setTimeout(() => {
        expect(eventCategory).toBe('error');
        done();
      }, 50);
    });

    it('should record business events', (done) => {
      let eventCategory = '';

      service.events$.subscribe(event => {
        if (event.eventName === 'roi_calculated') {
          eventCategory = event.category;
        }
      });

      service.recordEvent('roi_calculated', {}, 'business');

      setTimeout(() => {
        expect(eventCategory).toBe('business');
        done();
      }, 50);
    });
  });
});
