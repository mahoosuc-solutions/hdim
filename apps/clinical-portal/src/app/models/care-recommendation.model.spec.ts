import {
  CATEGORY_DISPLAY_NAMES,
  calculateDaysOverdue,
  getRiskLevelBadgeType,
  getUrgencyBadgeType,
  groupRecommendations,
  sortByUrgency,
  DashboardRecommendation,
} from './care-recommendation.model';

describe('care-recommendation.model helpers', () => {
  const baseRecommendation: DashboardRecommendation = {
    id: 'rec-1',
    type: 'care-gap',
    patientId: 'patient-1',
    patientName: 'Patient One',
    mrn: 'MRN-1',
    patientRiskLevel: 'high',
    category: 'preventive',
    title: 'Annual visit',
    description: 'Schedule annual wellness visit',
    urgency: 'urgent',
    priority: 5,
    status: 'pending',
    createdDate: new Date('2024-01-01T00:00:00Z'),
    actionItems: [],
  };

  it('exposes category display names', () => {
    expect(CATEGORY_DISPLAY_NAMES.preventive).toBe('Preventive Care');
    expect(CATEGORY_DISPLAY_NAMES['mental-health']).toBe('Mental Health');
  });

  it('maps urgency to badge type', () => {
    expect(getUrgencyBadgeType('emergent')).toBe('error');
    expect(getUrgencyBadgeType('urgent')).toBe('warning');
    expect(getUrgencyBadgeType('soon')).toBe('info');
    expect(getUrgencyBadgeType('routine')).toBe('success');
    expect(getUrgencyBadgeType('other' as any)).toBe('info');
  });

  it('maps risk level to badge type', () => {
    expect(getRiskLevelBadgeType('critical')).toBe('error');
    expect(getRiskLevelBadgeType('high')).toBe('warning');
    expect(getRiskLevelBadgeType('moderate')).toBe('info');
    expect(getRiskLevelBadgeType('low')).toBe('success');
    expect(getRiskLevelBadgeType('other' as any)).toBe('info');
  });

  describe('calculateDaysOverdue', () => {
    beforeEach(() => {
      jest.useFakeTimers().setSystemTime(new Date('2024-01-10T00:00:00Z'));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('returns undefined when no due date', () => {
      expect(calculateDaysOverdue()).toBeUndefined();
    });

    it('returns undefined for due dates in the future', () => {
      expect(calculateDaysOverdue(new Date('2024-01-20T00:00:00Z'))).toBeUndefined();
    });

    it('returns positive days for overdue items', () => {
      expect(calculateDaysOverdue(new Date('2024-01-05T00:00:00Z'))).toBe(5);
    });
  });

  it('sorts by urgency priority', () => {
    const urgent = { ...baseRecommendation, urgency: 'urgent' as const };
    const routine = { ...baseRecommendation, urgency: 'routine' as const };

    expect(sortByUrgency(urgent, routine)).toBeLessThan(0);
  });

  it('groups recommendations by field', () => {
    const recA = { ...baseRecommendation, id: 'a', category: 'preventive' as const };
    const recB = { ...baseRecommendation, id: 'b', category: 'medication' as const };

    const grouped = groupRecommendations([recA, recB], 'category');

    expect(grouped.get('preventive')).toEqual([recA]);
    expect(grouped.get('medication')).toEqual([recB]);
  });

  it('groups all recommendations when field is none', () => {
    const recs = [baseRecommendation];
    const grouped = groupRecommendations(recs, 'none');

    expect(grouped.get('all')).toEqual(recs);
  });

  it('uses unknown key for missing fields', () => {
    const rec = { ...baseRecommendation, category: undefined } as any;
    const grouped = groupRecommendations([rec], 'category');

    expect(grouped.get('unknown')).toEqual([rec]);
  });
});
