import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CustomMeasureService, CreateCustomMeasureRequest, UpdateCustomMeasureRequest, MeasureVersion, VersionDiff } from './custom-measure.service';
import { API_CONFIG } from '../config/api.config';

describe('CustomMeasureService', () => {
  let service: CustomMeasureService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CustomMeasureService],
    });
    service = TestBed.inject(CustomMeasureService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create a draft with tenant header', () => {
    const reqBody: CreateCustomMeasureRequest = { name: 'Test', description: 'Desc', category: 'CUSTOM', createdBy: 'ui' };

    service.createDraft(reqBody).subscribe((resp) => {
      expect(resp.name).toBe('Test');
    });

    const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures`);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
    req.flush({
      id: '1',
      tenantId: API_CONFIG.DEFAULT_TENANT_ID,
      name: 'Test',
      version: '1.0.0',
      status: 'DRAFT',
      description: 'Desc',
      category: 'CUSTOM',
      createdBy: 'ui',
      createdAt: new Date().toISOString(),
    });
  });

  it('should list drafts with optional status filter', () => {
    service.list('DRAFT').subscribe((resp) => {
      expect(resp.length).toBe(1);
      expect(resp[0].status).toBe('DRAFT');
    });

    const req = httpMock.expectOne(
      `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures?status=DRAFT`
    );
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: '1',
        tenantId: API_CONFIG.DEFAULT_TENANT_ID,
        name: 'Test',
        version: '1.0.0',
        status: 'DRAFT',
        createdBy: 'ui',
        createdAt: new Date().toISOString(),
      },
    ]);
  });

  it('should update a draft', () => {
    const updateBody: UpdateCustomMeasureRequest = { description: 'Updated' };

    service.update('1', updateBody).subscribe((resp) => {
      expect(resp.description).toBe('Updated');
    });

    const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateBody);
    req.flush({
      id: '1',
      tenantId: API_CONFIG.DEFAULT_TENANT_ID,
      name: 'Test',
      version: '1.0.0',
      status: 'DRAFT',
      description: 'Updated',
      createdBy: 'ui',
      createdAt: new Date().toISOString(),
    });
  });

  describe('Version Management', () => {
    describe('createNewVersion', () => {
      it('should create new patch version (1.0.0 -> 1.0.1)', () => {
        const measureId = 'measure-1';
        const versionType = 'patch';

        service.createNewVersion(measureId, versionType).subscribe((resp) => {
          expect(resp.version).toBe('1.0.1');
          expect(resp.status).toBe('draft');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ versionType });
        expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
        req.flush({
          id: 'measure-1-v2',
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure',
          version: '1.0.1',
          status: 'draft',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });

      it('should create new minor version (1.0.1 -> 1.1.0)', () => {
        const measureId = 'measure-1';
        const versionType = 'minor';

        service.createNewVersion(measureId, versionType).subscribe((resp) => {
          expect(resp.version).toBe('1.1.0');
          expect(resp.status).toBe('draft');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ versionType });
        req.flush({
          id: 'measure-1-v3',
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure',
          version: '1.1.0',
          status: 'draft',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });

      it('should create new major version (1.1.0 -> 2.0.0)', () => {
        const measureId = 'measure-1';
        const versionType = 'major';

        service.createNewVersion(measureId, versionType).subscribe((resp) => {
          expect(resp.version).toBe('2.0.0');
          expect(resp.status).toBe('draft');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ versionType });
        req.flush({
          id: 'measure-1-v4',
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure',
          version: '2.0.0',
          status: 'draft',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });

      it('should use custom tenant ID', () => {
        const measureId = 'measure-1';
        const versionType = 'patch';
        const customTenantId = 'custom-tenant';

        service.createNewVersion(measureId, versionType, customTenantId).subscribe();

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        expect(req.request.headers.get('X-Tenant-ID')).toBe(customTenantId);
        req.flush({
          id: 'measure-1-v2',
          tenantId: customTenantId,
          name: 'Test',
          version: '1.0.1',
          status: 'draft',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });
    });

    describe('getVersionHistory', () => {
      it('should retrieve version history for a measure', () => {
        const measureId = 'measure-1';

        service.getVersionHistory(measureId).subscribe((versions) => {
          expect(versions.length).toBe(3);
          expect(versions[0].version).toBe('1.0.0');
          expect(versions[1].version).toBe('1.0.1');
          expect(versions[2].version).toBe('1.1.0');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        expect(req.request.method).toBe('GET');
        expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
        req.flush([
          {
            version: '1.0.0',
            status: 'active',
            createdAt: new Date('2024-01-01').toISOString(),
            createdBy: 'user1',
            changelog: 'Initial version',
          },
          {
            version: '1.0.1',
            status: 'active',
            createdAt: new Date('2024-02-01').toISOString(),
            createdBy: 'user1',
            changelog: 'Bug fixes',
          },
          {
            version: '1.1.0',
            status: 'draft',
            createdAt: new Date('2024-03-01').toISOString(),
            createdBy: 'user2',
            changelog: 'New features',
          },
        ]);
      });

      it('should preserve version history with all statuses', () => {
        const measureId = 'measure-1';

        service.getVersionHistory(measureId).subscribe((versions) => {
          expect(versions.length).toBe(3);
          expect(versions[0].status).toBe('retired');
          expect(versions[1].status).toBe('active');
          expect(versions[2].status).toBe('draft');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        req.flush([
          {
            version: '1.0.0',
            status: 'retired',
            createdAt: new Date('2024-01-01').toISOString(),
            createdBy: 'user1',
          },
          {
            version: '2.0.0',
            status: 'active',
            createdAt: new Date('2024-02-01').toISOString(),
            createdBy: 'user1',
          },
          {
            version: '2.1.0',
            status: 'draft',
            createdAt: new Date('2024-03-01').toISOString(),
            createdBy: 'user2',
          },
        ]);
      });
    });

    describe('compareVersions', () => {
      it('should compare two versions and return differences', () => {
        const measureId = 'measure-1';
        const v1 = '1.0.0';
        const v2 = '1.0.1';

        service.compareVersions(measureId, v1, v2).subscribe((diffs) => {
          expect(diffs.length).toBe(2);
          expect(diffs[0].field).toBe('description');
          expect(diffs[0].oldValue).toBe('Original description');
          expect(diffs[0].newValue).toBe('Updated description');
          expect(diffs[1].field).toBe('cqlText');
        });

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/compare?v1=${v1}&v2=${v2}`
        );
        expect(req.request.method).toBe('GET');
        expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
        req.flush([
          {
            field: 'description',
            oldValue: 'Original description',
            newValue: 'Updated description',
          },
          {
            field: 'cqlText',
            oldValue: 'library Test version "1.0.0"',
            newValue: 'library Test version "1.0.1"',
          },
        ]);
      });

      it('should handle empty differences', () => {
        const measureId = 'measure-1';
        const v1 = '1.0.0';
        const v2 = '1.0.0';

        service.compareVersions(measureId, v1, v2).subscribe((diffs) => {
          expect(diffs.length).toBe(0);
        });

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/compare?v1=${v1}&v2=${v2}`
        );
        req.flush([]);
      });
    });

    describe('publishVersion', () => {
      it('should publish a draft version', () => {
        const measureId = 'measure-1';
        const version = '1.0.1';

        service.publishVersion(measureId, version).subscribe((resp) => {
          expect(resp.status).toBe('active');
          expect(resp.version).toBe(version);
        });

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/${version}/publish`
        );
        expect(req.request.method).toBe('POST');
        expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
        req.flush({
          id: measureId,
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure',
          version: version,
          status: 'active',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });

      it('should validate version before publishing', () => {
        const measureId = 'measure-1';
        const version = '1.0.1';

        service.publishVersion(measureId, version).subscribe(
          () => fail('should have failed'),
          (error) => {
            expect(error.status).toBe(400);
            expect(error.error.message).toContain('validation failed');
          }
        );

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/${version}/publish`
        );
        req.flush(
          { message: 'Measure validation failed: missing required CQL text' },
          { status: 400, statusText: 'Bad Request' }
        );
      });

      it('should prevent publishing already published versions', () => {
        const measureId = 'measure-1';
        const version = '1.0.0';

        service.publishVersion(measureId, version).subscribe(
          () => fail('should have failed'),
          (error) => {
            expect(error.status).toBe(409);
            expect(error.error.message).toContain('already published');
          }
        );

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/${version}/publish`
        );
        req.flush(
          { message: 'Version 1.0.0 is already published' },
          { status: 409, statusText: 'Conflict' }
        );
      });
    });

    describe('retireVersion', () => {
      it('should retire an active version', () => {
        const measureId = 'measure-1';
        const version = '1.0.0';

        service.retireVersion(measureId, version).subscribe((resp) => {
          expect(resp.status).toBe('retired');
          expect(resp.version).toBe(version);
        });

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/${version}/retire`
        );
        expect(req.request.method).toBe('POST');
        expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
        req.flush({
          id: measureId,
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure',
          version: version,
          status: 'retired',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });

      it('should use custom tenant ID', () => {
        const measureId = 'measure-1';
        const version = '1.0.0';
        const customTenantId = 'custom-tenant';

        service.retireVersion(measureId, version, customTenantId).subscribe();

        const req = httpMock.expectOne(
          `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions/${version}/retire`
        );
        expect(req.request.headers.get('X-Tenant-ID')).toBe(customTenantId);
        req.flush({
          id: measureId,
          tenantId: customTenantId,
          name: 'Test',
          version: version,
          status: 'retired',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });
    });

    describe('cloneMeasure', () => {
      it('should clone a measure as a new draft', () => {
        const measureId = 'measure-1';

        service.cloneMeasure(measureId).subscribe((resp) => {
          expect(resp.id).not.toBe(measureId);
          expect(resp.status).toBe('draft');
          expect(resp.version).toBe('1.0.0');
          expect(resp.name).toBe('Test Measure (Copy)');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/clone`);
        expect(req.request.method).toBe('POST');
        expect(req.request.headers.get('X-Tenant-ID')).toBe(API_CONFIG.DEFAULT_TENANT_ID);
        req.flush({
          id: 'measure-2',
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure (Copy)',
          version: '1.0.0',
          status: 'draft',
          description: 'Cloned measure',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });

      it('should track who made the clone (audit trail)', () => {
        const measureId = 'measure-1';

        service.cloneMeasure(measureId).subscribe((resp) => {
          expect(resp.createdBy).toBe('user2');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/clone`);
        req.flush({
          id: 'measure-2',
          tenantId: API_CONFIG.DEFAULT_TENANT_ID,
          name: 'Test Measure (Copy)',
          version: '1.0.0',
          status: 'draft',
          createdBy: 'user2',
          createdAt: new Date().toISOString(),
        });
      });

      it('should use custom tenant ID', () => {
        const measureId = 'measure-1';
        const customTenantId = 'custom-tenant';

        service.cloneMeasure(measureId, customTenantId).subscribe();

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/clone`);
        expect(req.request.headers.get('X-Tenant-ID')).toBe(customTenantId);
        req.flush({
          id: 'measure-2',
          tenantId: customTenantId,
          name: 'Test Measure (Copy)',
          version: '1.0.0',
          status: 'draft',
          createdBy: 'user1',
          createdAt: new Date().toISOString(),
        });
      });
    });

    describe('Version Status Management', () => {
      it('should support draft status', () => {
        service.list('draft').subscribe((resp) => {
          expect(resp.every((m) => m.status === 'draft')).toBe(true);
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures?status=draft`);
        req.flush([
          {
            id: '1',
            tenantId: API_CONFIG.DEFAULT_TENANT_ID,
            name: 'Draft Measure',
            version: '1.0.0',
            status: 'draft',
            createdBy: 'user1',
            createdAt: new Date().toISOString(),
          },
        ]);
      });

      it('should support active status', () => {
        service.list('active').subscribe((resp) => {
          expect(resp.every((m) => m.status === 'active')).toBe(true);
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures?status=active`);
        req.flush([
          {
            id: '1',
            tenantId: API_CONFIG.DEFAULT_TENANT_ID,
            name: 'Active Measure',
            version: '1.0.0',
            status: 'active',
            createdBy: 'user1',
            createdAt: new Date().toISOString(),
          },
        ]);
      });

      it('should support retired status', () => {
        service.list('retired').subscribe((resp) => {
          expect(resp.every((m) => m.status === 'retired')).toBe(true);
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures?status=retired`);
        req.flush([
          {
            id: '1',
            tenantId: API_CONFIG.DEFAULT_TENANT_ID,
            name: 'Retired Measure',
            version: '1.0.0',
            status: 'retired',
            createdBy: 'user1',
            createdAt: new Date().toISOString(),
          },
        ]);
      });
    });

    describe('Audit Trail', () => {
      it('should track who made changes in version history', () => {
        const measureId = 'measure-1';

        service.getVersionHistory(measureId).subscribe((versions) => {
          expect(versions[0].createdBy).toBe('user1');
          expect(versions[1].createdBy).toBe('user2');
          expect(versions[2].createdBy).toBe('user1');
        });

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/versions`);
        req.flush([
          {
            version: '1.0.0',
            status: 'active',
            createdAt: new Date('2024-01-01').toISOString(),
            createdBy: 'user1',
          },
          {
            version: '1.0.1',
            status: 'active',
            createdAt: new Date('2024-02-01').toISOString(),
            createdBy: 'user2',
          },
          {
            version: '1.1.0',
            status: 'draft',
            createdAt: new Date('2024-03-01').toISOString(),
            createdBy: 'user1',
          },
        ]);
      });
    });

    describe('Prevent Editing Published Versions', () => {
      it('should fail to update a published version', () => {
        const measureId = 'measure-1';
        const updateBody: UpdateCustomMeasureRequest = { description: 'Updated' };

        service.update(measureId, updateBody).subscribe(
          () => fail('should have failed'),
          (error) => {
            expect(error.status).toBe(403);
            expect(error.error.message).toContain('Cannot edit published version');
          }
        );

        const req = httpMock.expectOne(`${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}`);
        req.flush(
          { message: 'Cannot edit published version. Create a new version instead.' },
          { status: 403, statusText: 'Forbidden' }
        );
      });
    });
  });
});
