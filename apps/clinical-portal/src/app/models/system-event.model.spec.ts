import {
  DEFAULT_LIVE_METRICS,
  DEFAULT_PIPELINE_CONNECTIONS,
  DEFAULT_PIPELINE_NODES,
  createSystemEvent,
  formatRelativeTime,
  generateEventId,
  getCategoryFromType,
  getEventIcon,
  getNodeStatusColor,
  getSeverityColor,
} from './system-event.model';

describe('system-event model helpers', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2025-01-01T00:00:00Z'));
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('maps event types to icons', () => {
    expect(getEventIcon('FHIR_RESOURCE_CREATED')).toBe('add_circle');
    expect(getEventIcon('EVALUATION_FAILED')).toBe('error');
    expect(getEventIcon('MEASURE_CALCULATED')).toBe('calculate');
    expect(getEventIcon('CARE_GAP_ESCALATED')).toBe('priority_high');
    expect(getEventIcon('CONSENT_REVOKED')).toBe('thumb_down');
    expect(getEventIcon('SERVICE_STOPPED')).toBe('power_off');
    expect(getEventIcon('UNKNOWN_EVENT' as any)).toBe('info');
  });

  it('derives categories from event types', () => {
    expect(getCategoryFromType('FHIR_RESOURCE_UPDATED')).toBe('fhir');
    expect(getCategoryFromType('EVALUATION_STARTED')).toBe('evaluation');
    expect(getCategoryFromType('BATCH_PROGRESS')).toBe('evaluation');
    expect(getCategoryFromType('MEASURE_CALCULATED')).toBe('quality');
    expect(getCategoryFromType('HEALTH_SCORE_UPDATED')).toBe('quality');
    expect(getCategoryFromType('CARE_GAP_CLOSED')).toBe('care-gap');
    expect(getCategoryFromType('CONSENT_GRANTED')).toBe('consent');
    expect(getCategoryFromType('SERVICE_STARTED')).toBe('system');
  });

  it('maps severity and node status to colors', () => {
    expect(getSeverityColor('info')).toBe('primary');
    expect(getSeverityColor('success')).toBe('success');
    expect(getSeverityColor('warning')).toBe('warn');
    expect(getSeverityColor('error')).toBe('error');
    expect(getSeverityColor('unknown' as any)).toBe('primary');

    expect(getNodeStatusColor('active')).toBe('#4caf50');
    expect(getNodeStatusColor('processing')).toBe('#2196f3');
    expect(getNodeStatusColor('error')).toBe('#f44336');
    expect(getNodeStatusColor('idle')).toBe('#9e9e9e');
    expect(getNodeStatusColor('unknown' as any)).toBe('#9e9e9e');
  });

  it('formats relative time correctly', () => {
    const now = new Date('2025-01-01T00:00:00Z').toISOString();
    expect(formatRelativeTime(now)).toBe('just now');

    const secondsAgo = new Date('2024-12-31T23:59:30Z').toISOString();
    expect(formatRelativeTime(secondsAgo)).toBe('30s ago');

    const minutesAgo = new Date('2024-12-31T23:45:00Z').toISOString();
    expect(formatRelativeTime(minutesAgo)).toBe('15m ago');

    const hoursAgo = new Date('2024-12-31T02:00:00Z').toISOString();
    expect(formatRelativeTime(hoursAgo)).toBe('22h ago');

    const daysAgo = new Date('2024-12-25T00:00:00Z').toISOString();
    expect(formatRelativeTime(daysAgo)).toBe('7d ago');
  });

  it('generates deterministic event ids when time and random are mocked', () => {
    jest.spyOn(Date, 'now').mockReturnValue(1234567890);
    jest.spyOn(Math, 'random').mockReturnValue(0.123456789);
    expect(generateEventId()).toBe('evt-1234567890-4fzzzxjyl');
  });

  it('creates system events with defaults and overrides', () => {
    const event = createSystemEvent(
      'EVALUATION_COMPLETED',
      'Evaluation Complete',
      'Completed successfully',
      {
        severity: 'success',
        source: 'cql-engine',
        patient: { id: 'patient-1' },
      }
    );

    expect(event.type).toBe('EVALUATION_COMPLETED');
    expect(event.category).toBe('evaluation');
    expect(event.severity).toBe('success');
    expect(event.source).toBe('cql-engine');
    expect(event.patient?.id).toBe('patient-1');
    expect(event.title).toBe('Evaluation Complete');
    expect(event.description).toBe('Completed successfully');
  });

  it('provides default pipeline and metrics definitions', () => {
    expect(DEFAULT_PIPELINE_NODES.length).toBeGreaterThan(0);
    expect(DEFAULT_PIPELINE_CONNECTIONS.length).toBeGreaterThan(0);
    expect(DEFAULT_LIVE_METRICS.successRate).toBe(100);
  });
});
