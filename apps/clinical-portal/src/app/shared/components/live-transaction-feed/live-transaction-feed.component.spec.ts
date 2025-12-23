import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { LiveTransactionFeedComponent } from './live-transaction-feed.component';
import { SystemEventsService } from '../../../services/system-events.service';
import { SystemEvent } from '../../../models/system-event.model';

const createEvent = (overrides: Partial<SystemEvent> = {}): SystemEvent => ({
  id: 'e1',
  type: 'CARE_GAP_DETECTED',
  category: 'care-gap',
  severity: 'warning',
  title: 'Care Gap',
  description: 'desc',
  timestamp: new Date().toISOString(),
  source: 'service',
  ...overrides,
});

describe('LiveTransactionFeedComponent', () => {
  let fixture: ComponentFixture<LiveTransactionFeedComponent>;
  let component: LiveTransactionFeedComponent;
  let eventsSubject: BehaviorSubject<SystemEvent[]>;
  let pausedSubject: BehaviorSubject<boolean>;
  let statusSubject: BehaviorSubject<'connected' | 'disconnected' | 'simulating'>;
  let eventsService: { events$: any; isPaused$: any; connectionStatus$: any; togglePause: jest.Mock; clearEvents: jest.Mock };

  beforeEach(async () => {
    eventsSubject = new BehaviorSubject<SystemEvent[]>([]);
    pausedSubject = new BehaviorSubject<boolean>(false);
    statusSubject = new BehaviorSubject<'connected' | 'disconnected' | 'simulating'>('connected');

    eventsService = {
      events$: eventsSubject.asObservable(),
      isPaused$: pausedSubject.asObservable(),
      connectionStatus$: statusSubject.asObservable(),
      togglePause: jest.fn(),
      clearEvents: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LiveTransactionFeedComponent],
      providers: [
        { provide: SystemEventsService, useValue: eventsService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LiveTransactionFeedComponent);
    component = fixture.componentInstance;
  });

  it('subscribes to events and updates state', () => {
    component.ngOnInit();
    eventsSubject.next([createEvent()]);

    expect(component.events.length).toBe(1);
    expect(component.filteredEvents.length).toBe(1);
    expect(component.lastEventTime).toBeTruthy();
  });

  it('toggles pause and clears events', () => {
    component.togglePause();
    expect(eventsService.togglePause).toHaveBeenCalled();

    component.clearEvents();
    expect(eventsService.clearEvents).toHaveBeenCalled();
    expect(component.expandedEventId).toBeNull();
  });

  it('applies filters for category and severity', () => {
    component.events = [
      createEvent({ id: 'e1', category: 'care-gap', severity: 'warning' }),
      createEvent({ id: 'e2', category: 'system', severity: 'info' }),
    ];

    component.toggleCategoryFilter('care-gap');
    expect(component.filteredEvents.length).toBe(1);

    component.toggleSeverityFilter('warning');
    expect(component.getActiveFilterCount()).toBe(2);

    component.clearFilters();
    expect(component.getActiveFilterCount()).toBe(0);
  });

  it('toggles event details and tracks ids', () => {
    component.toggleEventDetails('e1');
    expect(component.expandedEventId).toBe('e1');

    component.toggleEventDetails('e1');
    expect(component.expandedEventId).toBeNull();

    expect(component.trackByEventId(0, createEvent({ id: 'e2' }))).toBe('e2');
  });
});
