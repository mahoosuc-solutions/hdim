import { formatDaysOverdue, getCareGapIcon, getUrgencyColor } from './care-gap.model';

describe('care-gap.model helpers', () => {
  it('returns icon for known gap types', () => {
    expect(getCareGapIcon('screening')).toBe('health_and_safety');
    expect(getCareGapIcon('medication')).toBe('medication');
    expect(getCareGapIcon('followup')).toBe('event_repeat');
    expect(getCareGapIcon('lab')).toBe('biotech');
    expect(getCareGapIcon('assessment')).toBe('psychology');
  });

  it('falls back to warning icon for unknown gap types', () => {
    expect(getCareGapIcon('other' as any)).toBe('warning');
  });

  it('returns colors for urgency levels', () => {
    expect(getUrgencyColor('high')).toBe('warn');
    expect(getUrgencyColor('medium')).toBe('accent');
    expect(getUrgencyColor('low')).toBe('primary');
  });

  it('falls back to primary for unknown urgency', () => {
    expect(getUrgencyColor('other' as any)).toBe('primary');
  });

  it('formats days overdue for edge cases', () => {
    expect(formatDaysOverdue(0)).toBe('Due today');
    expect(formatDaysOverdue(1)).toBe('1 day overdue');
    expect(formatDaysOverdue(-3)).toBe('Due in 3 days');
    expect(formatDaysOverdue(10)).toBe('10 days overdue');
  });

  it('formats months and years overdue', () => {
    expect(formatDaysOverdue(60)).toBe('2 months overdue');
    expect(formatDaysOverdue(400)).toBe('1 year overdue');
    expect(formatDaysOverdue(800)).toBe('2 years overdue');
  });
});
