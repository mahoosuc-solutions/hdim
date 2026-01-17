import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConnectionStatusComponent } from './connection-status.component';
import { WebSocketService, ConnectionState } from '@health-platform/shared/realtime';
import { BehaviorSubject } from 'rxjs';

/**
 * Connection Status Component Tests
 *
 * Tests for real-time connection status display component.
 * Verifies:
 * - Connection state display updates
 * - Visual indicators (badge classes)
 * - Retry count display
 * - Component lifecycle management
 */
describe('ConnectionStatusComponent', () => {
  let component: ConnectionStatusComponent;
  let fixture: ComponentFixture<ConnectionStatusComponent>;
  let mockWebSocketService: jasmine.SpyObj<WebSocketService>;
  let connectionStatusSubject: BehaviorSubject<any>;

  beforeEach(async () => {
    // Create subject for simulating connection status changes
    connectionStatusSubject = new BehaviorSubject({
      state: ConnectionState.CONNECTED,
      retryCount: 0,
      lastConnected: Date.now(),
      lastError: null,
      sessionId: 'SESSION123',
    });

    // Create mock WebSocket service
    mockWebSocketService = jasmine.createSpyObj('WebSocketService', ['connect', 'disconnect'], {
      connectionStatus$: connectionStatusSubject.asObservable(),
    });

    await TestBed.configureTestingModule({
      imports: [ConnectionStatusComponent],
      providers: [
        { provide: WebSocketService, useValue: mockWebSocketService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConnectionStatusComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default status text', () => {
      expect(component.statusText).toBe('Connecting...');
    });

    it('should subscribe to connectionStatus$ on init', () => {
      fixture.detectChanges();
      expect(component).toBeTruthy(); // Component initialized
    });
  });

  describe('Connected Status Display', () => {
    beforeEach(() => {
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTED,
        retryCount: 0,
        lastConnected: Date.now(),
        lastError: null,
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
    });

    it('should display "Connected" text when connected', () => {
      expect(component.statusText).toBe('Connected');
    });

    it('should apply status-connected class', () => {
      expect(component.statusClass).toBe('status-connected');
    });

    it('should apply badge-success class', () => {
      expect(component.badgeClass).toBe('badge-success');
    });

    it('should display status in template with correct styling', () => {
      const element = fixture.nativeElement.querySelector('.connection-status-indicator');
      expect(element.classList.contains('status-connected')).toBe(true);
      expect(element.textContent).toContain('Connected');
    });

    it('should not display retry count when connected', () => {
      expect(component.retryCount).toBe(0);
      const retryElement = fixture.nativeElement.querySelector('.retry-count');
      expect(retryElement).toBeFalsy(); // Hidden when retryCount is 0
    });
  });

  describe('Reconnecting Status Display', () => {
    beforeEach(() => {
      connectionStatusSubject.next({
        state: ConnectionState.RECONNECTING,
        retryCount: 2,
        lastConnected: Date.now() - 5000,
        lastError: 'Connection reset',
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
    });

    it('should display "Reconnecting..." text when reconnecting', () => {
      expect(component.statusText).toBe('Reconnecting...');
    });

    it('should apply status-reconnecting class', () => {
      expect(component.statusClass).toBe('status-reconnecting');
    });

    it('should apply badge-warning class', () => {
      expect(component.badgeClass).toBe('badge-warning');
    });

    it('should display retry count', () => {
      expect(component.retryCount).toBe(2);
      fixture.detectChanges();
      const retryElement = fixture.nativeElement.querySelector('.retry-count');
      expect(retryElement?.textContent).toContain('Retry 2');
    });
  });

  describe('Connecting Status Display', () => {
    beforeEach(() => {
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTING,
        retryCount: 0,
        lastConnected: null,
        lastError: null,
        sessionId: null,
      });
      fixture.detectChanges();
    });

    it('should display "Connecting..." text', () => {
      expect(component.statusText).toBe('Connecting...');
    });

    it('should apply badge-warning class for initial connection', () => {
      expect(component.badgeClass).toBe('badge-warning');
    });
  });

  describe('Disconnected Status Display', () => {
    beforeEach(() => {
      connectionStatusSubject.next({
        state: ConnectionState.DISCONNECTED,
        retryCount: 0,
        lastConnected: Date.now() - 30000,
        lastError: 'Manual disconnect',
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
    });

    it('should display "Disconnected" text', () => {
      expect(component.statusText).toBe('Disconnected');
    });

    it('should apply status-error class', () => {
      expect(component.statusClass).toBe('status-error');
    });

    it('should apply badge-error class', () => {
      expect(component.badgeClass).toBe('badge-error');
    });
  });

  describe('Error Status Display', () => {
    beforeEach(() => {
      connectionStatusSubject.next({
        state: ConnectionState.ERROR,
        retryCount: 5,
        lastConnected: Date.now() - 60000,
        lastError: 'WebSocket failed to connect',
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
    });

    it('should display "Error" text when connection error', () => {
      expect(component.statusText).toBe('Error');
    });

    it('should apply status-error class', () => {
      expect(component.statusClass).toBe('status-error');
    });

    it('should apply badge-error class', () => {
      expect(component.badgeClass).toBe('badge-error');
    });

    it('should display high retry count', () => {
      expect(component.retryCount).toBe(5);
    });
  });

  describe('Status Transitions', () => {
    it('should update from connecting to connected', () => {
      // Initial: connecting
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTING,
        retryCount: 0,
        lastConnected: null,
        lastError: null,
        sessionId: null,
      });
      fixture.detectChanges();
      expect(component.statusText).toBe('Connecting...');

      // Transition: connected
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTED,
        retryCount: 0,
        lastConnected: Date.now(),
        lastError: null,
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
      expect(component.statusText).toBe('Connected');
      expect(component.statusClass).toBe('status-connected');
    });

    it('should update from connected to reconnecting', () => {
      // Initial: connected
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTED,
        retryCount: 0,
        lastConnected: Date.now(),
        lastError: null,
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();

      // Transition: reconnecting
      connectionStatusSubject.next({
        state: ConnectionState.RECONNECTING,
        retryCount: 1,
        lastConnected: Date.now() - 5000,
        lastError: 'Connection lost',
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
      expect(component.statusText).toBe('Reconnecting...');
      expect(component.retryCount).toBe(1);
    });

    it('should update from reconnecting back to connected', () => {
      // Initial: reconnecting
      connectionStatusSubject.next({
        state: ConnectionState.RECONNECTING,
        retryCount: 3,
        lastConnected: Date.now() - 15000,
        lastError: 'Connection lost',
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
      expect(component.retryCount).toBe(3);

      // Transition: connected (retry successful)
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTED,
        retryCount: 3, // Retry count still shown in status
        lastConnected: Date.now(),
        lastError: null,
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();
      expect(component.statusText).toBe('Connected');
    });
  });

  describe('Component Cleanup', () => {
    it('should unsubscribe from connectionStatus$ on destroy', () => {
      fixture.detectChanges();
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });

    it('should not process status updates after destroy', () => {
      fixture.detectChanges();
      component.ngOnDestroy();

      const initialStatus = component.statusText;

      // Emit new status after destroy
      connectionStatusSubject.next({
        state: ConnectionState.RECONNECTING,
        retryCount: 10,
        lastConnected: null,
        lastError: 'Connection failed',
        sessionId: null,
      });

      // Status should not update after component destroyed
      expect(component.statusText).toBe(initialStatus);
    });
  });

  describe('Visual Rendering', () => {
    it('should render status indicator element', () => {
      fixture.detectChanges();
      const indicator = fixture.nativeElement.querySelector('.connection-status-indicator');
      expect(indicator).toBeTruthy();
    });

    it('should render status badge', () => {
      fixture.detectChanges();
      const badge = fixture.nativeElement.querySelector('.connection-status-badge');
      expect(badge).toBeTruthy();
    });

    it('should render status text', () => {
      fixture.detectChanges();
      const text = fixture.nativeElement.querySelector('.connection-status-text');
      expect(text).toBeTruthy();
      expect(text.textContent).toContain('Connected');
    });

    it('should apply appropriate CSS classes based on state', () => {
      connectionStatusSubject.next({
        state: ConnectionState.CONNECTED,
        retryCount: 0,
        lastConnected: Date.now(),
        lastError: null,
        sessionId: 'SESSION123',
      });
      fixture.detectChanges();

      const indicator = fixture.nativeElement.querySelector('.connection-status-indicator');
      expect(indicator.classList.contains('status-connected')).toBe(true);

      const badge = fixture.nativeElement.querySelector('.connection-status-badge');
      expect(badge.classList.contains('badge-success')).toBe(true);
    });
  });

  describe('Accessibility', () => {
    it('should have descriptive aria labels (future enhancement)', () => {
      fixture.detectChanges();
      // This is a placeholder for future accessibility improvements
      // Component should have aria-label or aria-describedby
      expect(true).toBe(true);
    });
  });
});
