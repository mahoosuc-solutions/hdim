import { TestBed } from '@angular/core/testing';
import { FilterPersistenceService } from './filter-persistence.service';
import { LoggerService } from './logger.service';

const mockLoggerService = createMockLoggerService();

describe('FilterPersistenceService', () => {
  let service: FilterPersistenceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FilterPersistenceService,
        { provide: LoggerService, useValue: mockLoggerService },
      ],
    });
    localStorage.clear();
    service = TestBed.inject(FilterPersistenceService);
  });

  it('saves and loads filters', () => {
    service.saveFilters('reports', { status: 'active' });
    const loaded = service.loadFilters('reports');
    expect(loaded).toEqual({ status: 'active' });
  });

  it('returns null for missing or expired filters', () => {
    expect(service.loadFilters('missing')).toBeNull();

    const key = 'clinical-portal-filters-old';
    const oldTimestamp = new Date(Date.now() - 31 * 24 * 60 * 60 * 1000).toISOString();
    localStorage.setItem(key, JSON.stringify({ filters: { a: 1 }, timestamp: oldTimestamp }));

    const loaded = service.loadFilters('old');
    expect(loaded).toBeNull();
  });

  it('clears filters', () => {
    service.saveFilters('patients', { query: 'abc' });
    service.clearFilters('patients');
    expect(service.loadFilters('patients')).toBeNull();
  });

  it('clears all filters', () => {
    service.saveFilters('patients', { query: 'abc' });
    service.saveFilters('reports', { status: 'active' });

    service.clearAllFilters();
    expect(Object.keys(localStorage).length).toBe(0);
  });
});
