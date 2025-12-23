import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { DataFlowPipelineComponent } from './data-flow-pipeline.component';
import { SystemEventsService } from '../../../services/system-events.service';
import { PipelineState } from '../../../models/system-event.model';

const createPipeline = (): PipelineState => ({
  nodes: [
    { id: 'fhir', name: 'FHIR', status: 'active', throughput: 2, lastActivity: new Date().toISOString(), description: 'FHIR' },
    { id: 'cql', name: 'CQL', status: 'processing', throughput: 3, lastActivity: new Date().toISOString(), description: 'CQL' },
  ],
  connections: [
    { id: 'conn1', from: 'fhir', to: 'cql', isActive: true, throughput: 4 },
  ],
  lastUpdated: new Date().toISOString(),
});

describe('DataFlowPipelineComponent', () => {
  let fixture: ComponentFixture<DataFlowPipelineComponent>;
  let component: DataFlowPipelineComponent;
  let pipelineSubject: BehaviorSubject<PipelineState>;

  beforeEach(async () => {
    pipelineSubject = new BehaviorSubject(createPipeline());

    await TestBed.configureTestingModule({
      imports: [DataFlowPipelineComponent],
      providers: [
        {
          provide: SystemEventsService,
          useValue: { pipeline$: pipelineSubject.asObservable() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DataFlowPipelineComponent);
    component = fixture.componentInstance;
  });

  it('subscribes to pipeline updates', () => {
    component.ngOnInit();
    expect(component.pipeline.nodes.length).toBe(2);
  });

  it('builds connection paths and midpoints', () => {
    component.pipeline = createPipeline();

    const path = component.getConnectionPath(component.pipeline.connections[0]);
    const midpoint = component.getConnectionMidpoint(component.pipeline.connections[0]);

    expect(path).toContain('M');
    expect(midpoint.x).toBeGreaterThan(0);
  });

  it('returns empty path when nodes are missing', () => {
    component.pipeline = {
      nodes: [],
      connections: [{ id: 'conn1', from: 'missing', to: 'none', isActive: true, throughput: 1 }],
      lastUpdated: new Date().toISOString(),
    };

    expect(component.getConnectionPath(component.pipeline.connections[0])).toBe('');
  });

  it('returns particles based on throughput', () => {
    component.pipeline = createPipeline();

    const particles = component.getParticles(0);
    expect(particles.length).toBeGreaterThanOrEqual(2);
  });

  it('returns no particles when connection is inactive', () => {
    component.pipeline = {
      ...createPipeline(),
      connections: [{ id: 'conn1', from: 'fhir', to: 'cql', isActive: false, throughput: 4 }],
    };

    const particles = component.getParticles(0);
    expect(particles.length).toBe(0);
  });

  it('returns node positions and icon fallbacks', () => {
    expect(component.getNodePosition(99)).toEqual({ x: 0, y: 0 });
    expect(component.getNodeIconChar('unknown')).toBe('\ue88e');
  });

  it('returns particle colors and durations', () => {
    const conn = createPipeline().connections[0];
    expect(component.getParticleColor(conn)).toBe('#4caf50');
    expect(component.getParticleDuration({ ...conn, throughput: 10 })).toBe('1s');
  });

  it('maps status color and indicator types', () => {
    expect(component.getStatusType('active')).toBe('active');
    expect(component.getStatusType('processing')).toBe('processing');
    expect(component.getStatusType('unknown')).toBe('idle');
    expect(component.getStatusColor('active')).toBeDefined();
  });

  it('selects and clears nodes', () => {
    component.pipeline = createPipeline();
    const node = component.pipeline.nodes[0];

    component.selectNode(node);
    expect(component.selectedNode?.id).toBe(node.id);

    component.selectNode(node);
    expect(component.selectedNode).toBeNull();
  });

  it('formats timestamps', () => {
    const result = component.formatTime(new Date('2024-01-01T12:00:00Z').toISOString());
    expect(result).toContain(':');
  });
});
