import { of, throwError } from 'rxjs';
import { CqlEngineService, ValueSet } from './cql-engine.service';

const createValueSet = (overrides: Partial<ValueSet> = {}): ValueSet => ({
  id: '1',
  tenantId: 't1',
  oid: '1.2.3',
  name: 'Test',
  version: '1',
  codeSystem: 'http://loinc.org',
  codes: '["1","2"]',
  status: 'ACTIVE',
  active: true,
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('CqlEngineService', () => {
  let http: { get: jest.Mock; post: jest.Mock };
  let service: CqlEngineService;

  beforeEach(() => {
    http = { get: jest.fn(), post: jest.fn() };
    service = new CqlEngineService(http as any);
  });

  it('lists value sets and maps to display', (done) => {
    http.get.mockReturnValueOnce(of([createValueSet()]));

    service.listValueSets().subscribe((valueSets) => {
      expect(valueSets[0].category).toBe('Laboratory');
      done();
    });
  });

  it('maps unknown code system and invalid codes safely', (done) => {
    const valueSet = createValueSet({
      codeSystem: 'http://unknown-system',
      codes: 'not-json',
    });
    http.get.mockReturnValueOnce(of([valueSet]));

    service.listValueSets().subscribe((valueSets) => {
      expect(valueSets[0].category).toBe('Other');
      expect(valueSets[0].codeCount).toBe(0);
      done();
    });
  });

  it('falls back to sample data on list error', (done) => {
    http.get.mockReturnValueOnce(throwError(() => new Error('fail')));

    service.listValueSets().subscribe((valueSets) => {
      expect(valueSets.length).toBeGreaterThan(0);
      done();
    });
  });

  it('searches value sets with fallback filtering', (done) => {
    http.get.mockReturnValueOnce(throwError(() => new Error('fail')));

    service.searchValueSets('diabetes').subscribe((valueSets) => {
      expect(valueSets.some((vs) => vs.name.toLowerCase().includes('diabetes'))).toBe(true);
      done();
    });
  });

  it('searches value sets with category parameter', (done) => {
    http.get.mockReturnValueOnce(of([createValueSet()]));

    service.searchValueSets('test', 'Diagnoses').subscribe((valueSets) => {
      expect(valueSets.length).toBe(1);
      done();
    });

    expect(http.get).toHaveBeenCalled();
  });

  it('returns a value set when lookup succeeds', (done) => {
    http.get.mockReturnValueOnce(of(createValueSet()));

    service.getValueSetByOid('1.2.3').subscribe((valueSet) => {
      expect(valueSet?.oid).toBe('1.2.3');
      done();
    });
  });

  it('returns null when value set lookup fails', (done) => {
    http.get.mockReturnValueOnce(throwError(() => new Error('fail')));

    service.getValueSetByOid('1.2.3').subscribe((valueSet) => {
      expect(valueSet).toBeNull();
      done();
    });
  });

  it('validates CQL and returns fallback on error', (done) => {
    http.post.mockReturnValueOnce(throwError(() => ({ name: 'TimeoutError' })));

    service.validateCql('library Test').subscribe((result) => {
      expect(result.valid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
      done();
    });
  });

  it('handles validation errors with non-timeout message', (done) => {
    http.post.mockReturnValueOnce(throwError(() => ({ name: 'ServerError', message: 'down' })));

    service.validateCql('library Test').subscribe((result) => {
      expect(result.valid).toBe(false);
      expect(result.errors[0].message).toContain('Validation service unavailable');
      done();
    });
  });

  it('handles evaluation timeout errors', (done) => {
    http.post.mockReturnValueOnce(throwError(() => ({ name: 'TimeoutError' })));

    service.evaluateCql({ patientId: 'p1' }).subscribe({
      error: (err) => {
        expect(err.message).toContain('timed out');
        done();
      },
    });
  });

  it('handles batch evaluation timeout errors', (done) => {
    http.post.mockReturnValueOnce(throwError(() => ({ name: 'TimeoutError' })));

    service.batchEvaluateCql('lib', ['p1']).subscribe({
      error: (err) => {
        expect(err.message).toContain('timed out');
        done();
      },
    });
  });

  it('propagates evaluation errors for non-timeout cases', (done) => {
    http.post.mockReturnValueOnce(throwError(() => new Error('boom')));

    service.evaluateCql({ patientId: 'p1' }).subscribe({
      error: (err) => {
        expect(err.message).toContain('boom');
        done();
      },
    });
  });

  it('propagates batch evaluation errors for non-timeout cases', (done) => {
    http.post.mockReturnValueOnce(throwError(() => new Error('boom')));

    service.batchEvaluateCql('lib', ['p1']).subscribe({
      error: (err) => {
        expect(err.message).toContain('boom');
        done();
      },
    });
  });
});
