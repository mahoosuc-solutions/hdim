import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { ConnectionStatusComponent } from './connection-status.component';
import { WebSocketService, ConnectionState } from '@health-platform/shared/realtime';

type ConnectionStatusPayload = {
  state: ConnectionState;
  retryCount: number;
  lastConnected: number | null;
  lastError: string | null;
  sessionId: string | null;
};

describe('ConnectionStatusComponent', () => {
  let fixture: ComponentFixture<ConnectionStatusComponent>;
  let component: ConnectionStatusComponent;
  let status$: BehaviorSubject<ConnectionStatusPayload>;

  beforeEach(async () => {
    status$ = new BehaviorSubject<ConnectionStatusPayload>({
      state: ConnectionState.CONNECTING,
      retryCount: 0,
      lastConnected: null,
      lastError: null,
      sessionId: null,
    });

    await TestBed.configureTestingModule({
      imports: [ConnectionStatusComponent],
      providers: [
        {
          provide: WebSocketService,
          useValue: {
            connectionStatus$: status$.asObservable(),
            connect: jest.fn(),
            disconnect: jest.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConnectionStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(false);
  });

  function emit(state: ConnectionState, retryCount = 0): void {
    status$.next({
      state,
      retryCount,
      lastConnected: Date.now(),
      lastError: null,
      sessionId: 'SESSION123',
    });
    fixture.detectChanges(false);
  }

  it('renders initial connecting state', () => {
    expect(component.statusText).toBe('Connecting...');
    expect(component.badgeClass).toBe('badge-warning');
  });

  it('updates to connected state', () => {
    emit(ConnectionState.CONNECTED);

    expect(component.statusText).toBe('Connected');
    expect(component.statusClass).toBe('status-connected');
    expect(component.badgeClass).toBe('badge-success');
  });

  it('updates to reconnecting state with retry count', () => {
    emit(ConnectionState.RECONNECTING, 2);

    expect(component.statusText).toBe('Reconnecting...');
    expect(component.statusClass).toBe('status-reconnecting');
    expect(component.badgeClass).toBe('badge-warning');
    expect(component.retryCount).toBe(2);
  });

  it('updates to disconnected and error states', () => {
    emit(ConnectionState.DISCONNECTED);
    expect(component.statusText).toBe('Disconnected');
    expect(component.statusClass).toBe('status-error');

    emit(ConnectionState.ERROR, 3);
    expect(component.statusText).toBe('Error');
    expect(component.badgeClass).toBe('badge-error');
    expect(component.retryCount).toBe(3);
  });

  it('stops reacting after destroy', () => {
    component.ngOnDestroy();

    const before = component.statusText;
    status$.next({
      state: ConnectionState.RECONNECTING,
      retryCount: 10,
      lastConnected: null,
      lastError: 'Connection failed',
      sessionId: null,
    });

    expect(component.statusText).toBe(before);
  });
});
