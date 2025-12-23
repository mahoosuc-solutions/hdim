import { of } from 'rxjs';
import { CustomMeasureService } from './custom-measure.service';

const createMeasure = () => ({
  id: 'm1',
  tenantId: 't1',
  name: 'Measure',
  version: '1.0',
  status: 'DRAFT',
  createdBy: 'user',
  createdAt: new Date().toISOString(),
});

describe('CustomMeasureService', () => {
  let http: { get: jest.Mock; post: jest.Mock; put: jest.Mock; delete: jest.Mock };
  let service: CustomMeasureService;

  beforeEach(() => {
    http = {
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    };
    service = new CustomMeasureService(http as any);
  });

  it('creates and lists drafts', (done) => {
    http.post.mockReturnValueOnce(of(createMeasure()));
    http.get.mockReturnValueOnce(of([createMeasure()]));

    service.createDraft({ name: 'Measure' }).subscribe((measure) => {
      expect(measure.id).toBe('m1');

      service.list('DRAFT').subscribe((list) => {
        expect(list.length).toBe(1);
        done();
      });
    });
  });

  it('updates and deletes measures', (done) => {
    http.put.mockReturnValueOnce(of(createMeasure()));
    http.delete.mockReturnValueOnce(of(undefined));

    service.update('m1', { name: 'Updated' }).subscribe(() => {
      service.delete('m1').subscribe(() => {
        expect(http.delete).toHaveBeenCalled();
        done();
      });
    });
  });

  it('publishes and batches measures', (done) => {
    http.post
      .mockReturnValueOnce(of(createMeasure()))
      .mockReturnValueOnce(of({ deleted: 1, failed: [] }))
      .mockReturnValueOnce(of({ published: 1, failed: [] }));

    service.publish('m1').subscribe(() => {
      service.batchDelete(['m1']).subscribe(() => {
        service.batchPublish(['m1']).subscribe((result) => {
          expect(result.published).toBe(1);
          done();
        });
      });
    });
  });

  it('updates CQL and value sets', (done) => {
    http.put
      .mockReturnValueOnce(of(createMeasure()))
      .mockReturnValueOnce(of(createMeasure()));

    service.updateCql('m1', 'library Test').subscribe(() => {
      service.updateValueSets('m1', ['vs1']).subscribe(() => {
        expect(http.put).toHaveBeenCalledTimes(2);
        done();
      });
    });
  });

  it('gets versions and compares', (done) => {
    http.get
      .mockReturnValueOnce(of([createMeasure()]))
      .mockReturnValueOnce(of([{ field: 'name', oldValue: 'A', newValue: 'B' }]));

    service.getVersionHistory('m1').subscribe(() => {
      service.compareVersions('m1', '1.0', '1.1').subscribe((diffs) => {
        expect(diffs.length).toBe(1);
        done();
      });
    });
  });

  it('gets by id, tests, clones, and publishes versions', (done) => {
    http.get.mockReturnValueOnce(of(createMeasure()));
    http.post
      .mockReturnValueOnce(of({ measureId: 'm1', measureName: 'Measure', testDate: 'now', totalPatients: 1, results: [], summary: { passed: 1, failed: 0, notEligible: 0, errors: 0 } }))
      .mockReturnValueOnce(of(createMeasure()))
      .mockReturnValueOnce(of(createMeasure()))
      .mockReturnValueOnce(of(createMeasure()));

    service.getById('m1').subscribe(() => {
      service.testMeasure('m1').subscribe(() => {
        service.cloneMeasure('m1').subscribe(() => {
          service.publishVersion('m1', '1.0').subscribe(() => {
            service.retireVersion('m1', '1.0').subscribe(() => {
              done();
            });
          });
        });
      });
    });
  });
});
