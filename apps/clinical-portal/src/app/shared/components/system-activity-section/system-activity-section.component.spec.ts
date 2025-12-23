import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { SystemActivitySectionComponent } from './system-activity-section.component';
import { SystemEventsService } from '../../../services/system-events.service';
import { LiveMetrics } from '../../../models/system-event.model';

const createMetrics = (): LiveMetrics => ({
  patientsProcessed: 1,
  patientsProcessedChange: 0,
  throughputPerSecond: 3.4,
  maxThroughput: 10,
  complianceRate: 88.5,
  complianceRateChange: 0,
  openCareGaps: 2,
  careGapsChange: 0,
  successRate: 95,
  avgProcessingTimeMs: 120,
  lastUpdated: new Date().toISOString(),
});

describe('SystemActivitySectionComponent', () => {
  let fixture: ComponentFixture<SystemActivitySectionComponent>;
  let component: SystemActivitySectionComponent;
  let eventsSubject: BehaviorSubject<any[]>;
  let metricsSubject: BehaviorSubject<LiveMetrics>;
  let simSubject: BehaviorSubject<boolean>;
  let statusSubject: BehaviorSubject<'connected' | 'disconnected' | 'simulating'>;
  let eventsService: any;

  beforeEach(async () => {
    eventsSubject = new BehaviorSubject<any[]>([]);
    metricsSubject = new BehaviorSubject<LiveMetrics>(createMetrics());
    simSubject = new BehaviorSubject<boolean>(false);
    statusSubject = new BehaviorSubject<'connected' | 'disconnected' | 'simulating'>('disconnected');

    eventsService = {
      events$: eventsSubject.asObservable(),
      metrics$: metricsSubject.asObservable(),
      isSimulating$: simSubject.asObservable(),
      connectionStatus$: statusSubject.asObservable(),
      startSimulation: jest.fn(),
      stopSimulation: jest.fn(),
      connect: jest.fn(),
      disconnect: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [SystemActivitySectionComponent],
      providers: [{ provide: SystemEventsService, useValue: eventsService }],
    }).compileComponents();

    fixture = TestBed.createComponent(SystemActivitySectionComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  it('initializes and starts simulation when expanded', () => {
    component.collapsed = false;
    component.ngOnInit();

    expect(eventsService.startSimulation).toHaveBeenCalled();
    expect(component.throughput).toBe('3.4');
  });

  it('toggles collapse and persists state', () => {
    component.ngOnInit();

    component.toggleCollapse();
    expect(localStorage.getItem('hdim-system-activity-collapsed')).toBe('true');
    expect(eventsService.stopSimulation).toHaveBeenCalled();

    component.toggleCollapse();
    expect(eventsService.startSimulation).toHaveBeenCalled();
  });

  it('toggles simulation and connection', () => {
    component.ngOnInit();

    component.isSimulating = true;
    component.toggleSimulation();
    expect(eventsService.stopSimulation).toHaveBeenCalled();

    component.isSimulating = false;
    component.toggleSimulation();
    expect(eventsService.disconnect).toHaveBeenCalled();
    expect(eventsService.startSimulation).toHaveBeenCalled();

    component.connectionStatus = 'connected';
    component.toggleConnection();
    expect(eventsService.disconnect).toHaveBeenCalled();

    component.connectionStatus = 'disconnected';
    component.toggleConnection();
    expect(eventsService.stopSimulation).toHaveBeenCalled();
    expect(eventsService.connect).toHaveBeenCalled();
  });
});
