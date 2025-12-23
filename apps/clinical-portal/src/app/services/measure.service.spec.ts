import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { MeasureService } from './measure.service';
import { CqlLibrary, LibraryStatus } from '../models/cql-library.model';
import { CqlLibraryFactory } from '../../testing/factories/cql-library.factory';
import { API_CONFIG, buildCqlEngineUrl, CQL_ENGINE_ENDPOINTS } from '../config/api.config';

describe('MeasureService', () => {
  let service: MeasureService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MeasureService],
    });

    service = TestBed.inject(MeasureService);
    httpMock = TestBed.inject(HttpTestingController);
    CqlLibraryFactory.reset();
  });

  afterEach(() => {
    httpMock.verify(); // Ensure no outstanding HTTP requests
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('getActiveMeasures', () => {
    it('should fetch active measures successfully', () => {
      // Arrange
      const mockLibraries = [
        CqlLibraryFactory.createHedisCdc(),
        CqlLibraryFactory.createHedisCbp(),
        CqlLibraryFactory.createCms134(),
      ];
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE);

      // Act
      service.getActiveMeasures().subscribe((libraries) => {
        // Assert
        expect(libraries).toEqual(mockLibraries);
        expect(libraries.length).toBe(3);
        expect(libraries[0].status).toBe('ACTIVE');
      });

      // Assert HTTP request
      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockLibraries);
    });

    it('should handle empty response', () => {
      service.getActiveMeasures().subscribe((libraries) => {
        expect(libraries).toEqual([]);
        expect(libraries.length).toBe(0);
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE));
      req.flush([]);
    });

    it('should handle HTTP errors', () => {
      service.getActiveMeasures().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE));
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getActiveMeasuresInfo', () => {
    it('should transform libraries to MeasureInfo', () => {
      const mockLibraries = [
        CqlLibraryFactory.createHedisCdc(),
        CqlLibraryFactory.createHedisCbp(),
      ];

      service.getActiveMeasuresInfo().subscribe((measures) => {
        const hedisCdc = measures.find((measure) => measure.name === 'HEDIS_CDC');
        expect(measures.length).toBe(3);
        expect(hedisCdc).toBeDefined();
        expect(hedisCdc).toHaveProperty('displayName');
        expect(hedisCdc).toHaveProperty('category');
        expect(hedisCdc?.category).toBe('HEDIS');
        expect(hedisCdc?.displayName).toContain('HEDIS-CDC');
        expect(hedisCdc?.displayName).toContain('v1.0.0');
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE));
      req.flush(mockLibraries);
    });

    it('should extract category from library name', () => {
      const cmsLibrary = CqlLibraryFactory.createCms134();

      service.getActiveMeasuresInfo().subscribe((measures) => {
        expect(measures[0].category).toBe('CMS');
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE));
      req.flush([cmsLibrary]);
    });

    it('should use CUSTOM category for non-standard names', () => {
      const customLibrary = CqlLibraryFactory.create({ name: 'CustomMeasure' });

      service.getActiveMeasuresInfo().subscribe((measures) => {
        expect(measures[0].category).toBe('CUSTOM');
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE));
      req.flush([customLibrary]);
    });
  });

  describe('getAllLibraries', () => {
    it('should fetch all libraries with default pagination', () => {
      const mockLibraries = CqlLibraryFactory.createMany(20);
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES, {
        page: '0',
        size: '20',
      });

      service.getAllLibraries().subscribe((libraries) => {
        expect(libraries.length).toBe(20);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockLibraries);
    });

    it('should fetch libraries with custom pagination', () => {
      const mockLibraries = CqlLibraryFactory.createMany(10);
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES, {
        page: '2',
        size: '10',
      });

      service.getAllLibraries(2, 10).subscribe((libraries) => {
        expect(libraries.length).toBe(10);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockLibraries);
    });
  });

  describe('getMeasureById', () => {
    it('should fetch library by ID', () => {
      const mockLibrary = CqlLibraryFactory.createHedisCdc();
      const id = mockLibrary.id;
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id));

      service.getMeasureById(id).subscribe((library) => {
        expect(library).toEqual(mockLibrary);
        expect(library.id).toBe(id);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockLibrary);
    });

    it('should handle 404 when library not found', () => {
      const id = 'non-existent-id';

      service.getMeasureById(id).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id)));
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getMeasureByName', () => {
    it('should fetch library by name and version', () => {
      const mockLibrary = CqlLibraryFactory.createHedisCdc();
      const { name, version } = mockLibrary;
      const expectedUrl = buildCqlEngineUrl(
        CQL_ENGINE_ENDPOINTS.LIBRARY_BY_NAME(name, version)
      );

      service.getMeasureByName(name, version).subscribe((library) => {
        expect(library).toEqual(mockLibrary);
        expect(library.name).toBe(name);
        expect(library.version).toBe(version);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockLibrary);
    });
  });

  describe('getLatestVersion', () => {
    it('should fetch latest version of a library', () => {
      const mockLibrary = CqlLibraryFactory.create({
        name: 'HEDIS-CDC',
        version: '2.0.0',
      });
      const expectedUrl = buildCqlEngineUrl(
        CQL_ENGINE_ENDPOINTS.LIBRARY_LATEST('HEDIS-CDC')
      );

      service.getLatestVersion('HEDIS-CDC').subscribe((library) => {
        expect(library).toEqual(mockLibrary);
        expect(library.version).toBe('2.0.0');
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockLibrary);
    });
  });

  describe('getAllVersions', () => {
    it('should fetch all versions of a library', () => {
      const versions = [
        CqlLibraryFactory.create({ name: 'HEDIS-CDC', version: '1.0.0' }),
        CqlLibraryFactory.create({ name: 'HEDIS-CDC', version: '1.1.0' }),
        CqlLibraryFactory.create({ name: 'HEDIS-CDC', version: '2.0.0' }),
      ];
      const expectedUrl = buildCqlEngineUrl(
        CQL_ENGINE_ENDPOINTS.LIBRARY_VERSIONS('HEDIS-CDC')
      );

      service.getAllVersions('HEDIS-CDC').subscribe((libraries) => {
        expect(libraries.length).toBe(3);
        expect(libraries.every((lib) => lib.name === 'HEDIS-CDC')).toBe(true);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(versions);
    });
  });

  describe('getMeasuresByStatus', () => {
    it('should fetch libraries by ACTIVE status', () => {
      const activeLibraries = CqlLibraryFactory.createMany(3, { status: 'ACTIVE' });
      const expectedUrl = buildCqlEngineUrl(
        CQL_ENGINE_ENDPOINTS.LIBRARIES_BY_STATUS('ACTIVE')
      );

      service.getMeasuresByStatus('ACTIVE').subscribe((libraries) => {
        expect(libraries.length).toBe(3);
        expect(libraries.every((lib) => lib.status === 'ACTIVE')).toBe(true);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(activeLibraries);
    });

    it('should fetch libraries by DRAFT status', () => {
      const draftLibraries = [CqlLibraryFactory.createDraft()];
      const expectedUrl = buildCqlEngineUrl(
        CQL_ENGINE_ENDPOINTS.LIBRARIES_BY_STATUS('DRAFT')
      );

      service.getMeasuresByStatus('DRAFT').subscribe((libraries) => {
        expect(libraries.length).toBe(1);
        expect(libraries[0].status).toBe('DRAFT');
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(draftLibraries);
    });

    it('should fetch libraries by RETIRED status', () => {
      const retiredLibraries = [CqlLibraryFactory.createRetired()];
      const expectedUrl = buildCqlEngineUrl(
        CQL_ENGINE_ENDPOINTS.LIBRARIES_BY_STATUS('RETIRED')
      );

      service.getMeasuresByStatus('RETIRED').subscribe((libraries) => {
        expect(libraries.length).toBe(1);
        expect(libraries[0].status).toBe('RETIRED');
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(retiredLibraries);
    });
  });

  describe('searchMeasures', () => {
    it('should search measures by query string', () => {
      const searchResults = [
        CqlLibraryFactory.createHedisCdc(),
        CqlLibraryFactory.createHedisCbp(),
      ];
      const query = 'HEDIS';
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_SEARCH, {
        q: query,
      });

      service.searchMeasures(query).subscribe((libraries) => {
        expect(libraries.length).toBe(2);
        expect(libraries.every((lib) => lib.name.includes('HEDIS'))).toBe(true);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(searchResults);
    });

    it('should return empty array for no matches', () => {
      const query = 'NonExistent';

      service.searchMeasures(query).subscribe((libraries) => {
        expect(libraries).toEqual([]);
      });

      const req = httpMock.expectOne(
        buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_SEARCH, { q: query })
      );
      req.flush([]);
    });
  });

  describe('getMeasureCount', () => {
    it('should fetch total library count', () => {
      const count = 42;
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_COUNT);

      service.getMeasureCount().subscribe((result) => {
        expect(result).toBe(count);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(count);
    });
  });

  describe('getMeasureCountByStatus', () => {
    it('should fetch count by ACTIVE status', () => {
      const count = 25;
      const expectedUrl = buildCqlEngineUrl(
        `${CQL_ENGINE_ENDPOINTS.LIBRARIES_COUNT}/by-status/ACTIVE`
      );

      service.getMeasureCountByStatus('ACTIVE').subscribe((result) => {
        expect(result).toBe(count);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(count);
    });
  });

  describe('measureExists', () => {
    it('should return true when library exists', () => {
      const name = 'HEDIS-CDC';
      const version = '1.0.0';
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_EXISTS, {
        name,
        version,
      });

      service.measureExists(name, version).subscribe((exists) => {
        expect(exists).toBe(true);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(true);
    });

    it('should return false when library does not exist', () => {
      const name = 'NonExistent';
      const version = '1.0.0';

      service.measureExists(name, version).subscribe((exists) => {
        expect(exists).toBe(false);
      });

      const req = httpMock.expectOne(
        buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_EXISTS, { name, version })
      );
      req.flush(false);
    });
  });

  describe('createMeasure', () => {
    it('should create a new library', () => {
      const request = {
        name: 'New-Measure',
        version: '1.0.0',
        cqlContent: 'library NewMeasure version "1.0.0"',
        status: 'DRAFT' as LibraryStatus,
        description: 'A new measure',
      };
      const createdLibrary = CqlLibraryFactory.create(request);
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES);

      service.createMeasure(request).subscribe((library) => {
        expect(library).toEqual(createdLibrary);
        expect(library.name).toBe(request.name);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(createdLibrary);
    });
  });

  describe('updateMeasure', () => {
    it('should update an existing library', () => {
      const id = 'lib-123';
      const request = {
        name: 'Updated-Measure',
        version: '1.1.0',
        cqlContent: 'updated content',
      };
      const updatedLibrary = CqlLibraryFactory.create({ ...request, id });
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id));

      service.updateMeasure(id, request).subscribe((library) => {
        expect(library).toEqual(updatedLibrary);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(updatedLibrary);
    });
  });

  describe('activateMeasure', () => {
    it('should activate a library', () => {
      const id = 'lib-123';
      const activatedLibrary = CqlLibraryFactory.create({ id, status: 'ACTIVE', active: true });
      const expectedUrl = buildCqlEngineUrl(`${CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id)}/activate`);

      service.activateMeasure(id).subscribe((library) => {
        expect(library.status).toBe('ACTIVE');
        expect(library.active).toBe(true);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(activatedLibrary);
    });
  });

  describe('retireMeasure', () => {
    it('should retire a library', () => {
      const id = 'lib-123';
      const retiredLibrary = CqlLibraryFactory.create({ id, status: 'RETIRED', active: false });
      const expectedUrl = buildCqlEngineUrl(`${CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id)}/retire`);

      service.retireMeasure(id).subscribe((library) => {
        expect(library.status).toBe('RETIRED');
        expect(library.active).toBe(false);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      req.flush(retiredLibrary);
    });
  });

  describe('deleteMeasure', () => {
    it('should delete a library', () => {
      const id = 'lib-123';
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id));

      service.deleteMeasure(id).subscribe((result) => {
        expect(result).toBeUndefined();
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('caching and helpers', () => {
    it('adds preview custom measures for active info', () => {
      const mockLibraries = [
        CqlLibraryFactory.create({ name: 'HEDIS-CDC', version: '1.0.0' }),
      ];

      service.getActiveMeasuresInfo().subscribe((measures) => {
        const preview = measures.find((measure) => measure.id === 'preview-custom-mrna-screening');
        const hedis = measures.find((measure) => measure.name === 'HEDIS_CDC');
        expect(hedis).toBeDefined();
        expect(preview).toBeDefined();
      });

      const req = httpMock.expectOne(buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE));
      req.flush(mockLibraries);
    });

    it('caches active measures within the TTL window', () => {
      const mockLibraries = [CqlLibraryFactory.createHedisCdc()];
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE);

      service.getActiveMeasuresCached().subscribe((libraries) => {
        expect(libraries.length).toBe(1);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockLibraries);

      service.getActiveMeasuresCached().subscribe((libraries) => {
        expect(libraries.length).toBe(1);
      });

      httpMock.expectNone(expectedUrl);
    });

    it('refreshes cache after invalidation', () => {
      const mockLibraries = [CqlLibraryFactory.createHedisCdc()];
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE);

      service.getActiveMeasuresCached().subscribe();
      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockLibraries);

      service.invalidateCache();

      service.getActiveMeasuresCached().subscribe();
      const req2 = httpMock.expectOne(expectedUrl);
      req2.flush(mockLibraries);
    });

    it('caches active measures info within the TTL window', () => {
      const mockLibraries = [CqlLibraryFactory.createHedisCdc()];
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE);

      service.getActiveMeasuresInfoCached().subscribe((measures) => {
        expect(measures.length).toBeGreaterThan(0);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockLibraries);

      service.getActiveMeasuresInfoCached().subscribe((measures) => {
        expect(measures.length).toBeGreaterThan(0);
      });

      httpMock.expectNone(expectedUrl);
    });

    it('checks cache validity based on TTL', () => {
      const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(1000);
      const mockLibraries = [CqlLibraryFactory.createHedisCdc()];
      const expectedUrl = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE);

      service.getActiveMeasuresCached().subscribe();
      const req = httpMock.expectOne(expectedUrl);
      req.flush(mockLibraries);

      expect(service.isCacheValid()).toBe(true);

      nowSpy.mockReturnValue(1000 + 10 * 60 * 1000 + 1);
      expect(service.isCacheValid()).toBe(false);
      nowSpy.mockRestore();
    });
  });
});
