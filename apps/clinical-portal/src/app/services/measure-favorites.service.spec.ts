import { TestBed } from '@angular/core/testing';
import { MeasureFavoritesService, FavoriteMeasure, RecentMeasure } from './measure-favorites.service';
import { MeasureInfo } from '../models/cql-library.model';

describe('MeasureFavoritesService', () => {
  let service: MeasureFavoritesService;

  const mockMeasure: MeasureInfo = {
    id: 'CDC',
    name: 'CDC',
    displayName: 'Comprehensive Diabetes Care',
    version: '2024',
    category: 'CHRONIC_DISEASE',
  };

  const mockMeasure2: MeasureInfo = {
    id: 'BCS',
    name: 'BCS',
    displayName: 'Breast Cancer Screening',
    version: '2024',
    category: 'PREVENTIVE',
  };

  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [MeasureFavoritesService],
    });
    service = TestBed.inject(MeasureFavoritesService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('Initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should start with empty favorites', () => {
      expect(service.favorites().length).toBe(0);
    });

    it('should start with empty recent measures', () => {
      expect(service.recentMeasures().length).toBe(0);
    });

    it('should load favorites from localStorage on init', () => {
      const storedFavorites: FavoriteMeasure[] = [
        {
          measureId: 'CDC',
          measureName: 'CDC',
          displayName: 'Comprehensive Diabetes Care',
          category: 'CHRONIC_DISEASE',
          favoritedAt: new Date().toISOString(),
        },
      ];
      localStorage.setItem('hdim_measure_favorites', JSON.stringify(storedFavorites));

      // Create new instance to trigger load
      const newService = new MeasureFavoritesService();
      expect(newService.favorites().length).toBe(1);
      expect(newService.favorites()[0].measureId).toBe('CDC');
    });
  });

  describe('Favorites Management', () => {
    it('should add a measure to favorites', () => {
      service.addFavorite(mockMeasure);
      expect(service.favorites().length).toBe(1);
      expect(service.favorites()[0].measureId).toBe('CDC');
    });

    it('should not add duplicate favorites', () => {
      service.addFavorite(mockMeasure);
      service.addFavorite(mockMeasure);
      expect(service.favorites().length).toBe(1);
    });

    it('should remove a measure from favorites', () => {
      service.addFavorite(mockMeasure);
      service.removeFavorite('CDC');
      expect(service.favorites().length).toBe(0);
    });

    it('should check if a measure is favorited', () => {
      expect(service.isFavorite('CDC')).toBe(false);
      service.addFavorite(mockMeasure);
      expect(service.isFavorite('CDC')).toBe(true);
    });

    it('should toggle favorite status', () => {
      const result1 = service.toggleFavorite(mockMeasure);
      expect(result1).toBe(true);
      expect(service.isFavorite('CDC')).toBe(true);

      const result2 = service.toggleFavorite(mockMeasure);
      expect(result2).toBe(false);
      expect(service.isFavorite('CDC')).toBe(false);
    });

    it('should persist favorites to localStorage', () => {
      service.addFavorite(mockMeasure);
      const stored = localStorage.getItem('hdim_measure_favorites');
      expect(stored).not.toBeNull();

      const parsed = JSON.parse(stored!);
      expect(parsed.length).toBe(1);
      expect(parsed[0].measureId).toBe('CDC');
    });

    it('should return favorite IDs', () => {
      service.addFavorite(mockMeasure);
      service.addFavorite(mockMeasure2);
      const ids = service.getFavoriteIds();
      expect(ids).toContain('CDC');
      expect(ids).toContain('BCS');
    });

    it('should clear all favorites', () => {
      service.addFavorite(mockMeasure);
      service.addFavorite(mockMeasure2);
      service.clearFavorites();
      expect(service.favorites().length).toBe(0);
    });
  });

  describe('Recent Measures', () => {
    it('should record measure usage', () => {
      service.recordUsage(mockMeasure);
      expect(service.recentMeasures().length).toBe(1);
      expect(service.recentMeasures()[0].measureId).toBe('CDC');
      expect(service.recentMeasures()[0].usageCount).toBe(1);
    });

    it('should increment usage count on repeat usage', () => {
      service.recordUsage(mockMeasure);
      service.recordUsage(mockMeasure);
      expect(service.recentMeasures().length).toBe(1);
      expect(service.recentMeasures()[0].usageCount).toBe(2);
    });

    it('should move recently used measure to top', () => {
      service.recordUsage(mockMeasure);
      service.recordUsage(mockMeasure2);
      service.recordUsage(mockMeasure);

      expect(service.recentMeasures()[0].measureId).toBe('CDC');
      expect(service.recentMeasures()[1].measureId).toBe('BCS');
    });

    it('should limit recent measures to 10', () => {
      for (let i = 0; i < 15; i++) {
        service.recordUsage({
          id: `measure-${i}`,
          name: `Measure ${i}`,
          displayName: `Measure ${i}`,
          version: '2024',
        });
      }
      expect(service.recentMeasures().length).toBe(10);
    });

    it('should persist recent measures to localStorage', () => {
      service.recordUsage(mockMeasure);
      const stored = localStorage.getItem('hdim_measure_recent');
      expect(stored).not.toBeNull();

      const parsed = JSON.parse(stored!);
      expect(parsed.length).toBe(1);
    });

    it('should return recent IDs', () => {
      service.recordUsage(mockMeasure);
      service.recordUsage(mockMeasure2);
      const ids = service.getRecentIds();
      expect(ids).toContain('CDC');
      expect(ids).toContain('BCS');
    });

    it('should clear recent measures', () => {
      service.recordUsage(mockMeasure);
      service.clearRecent();
      expect(service.recentMeasures().length).toBe(0);
    });

    it('should get most frequently used measures', () => {
      service.recordUsage(mockMeasure);
      service.recordUsage(mockMeasure);
      service.recordUsage(mockMeasure);
      service.recordUsage(mockMeasure2);

      const mostUsed = service.getMostUsed(2);
      expect(mostUsed.length).toBe(2);
      expect(mostUsed[0].measureId).toBe('CDC');
      expect(mostUsed[0].usageCount).toBe(3);
    });
  });

  describe('Computed Properties', () => {
    it('should compute favorites count', () => {
      expect(service.favoritesCount()).toBe(0);
      service.addFavorite(mockMeasure);
      expect(service.favoritesCount()).toBe(1);
      service.addFavorite(mockMeasure2);
      expect(service.favoritesCount()).toBe(2);
    });

    it('should compute hasRecent', () => {
      expect(service.hasRecent()).toBe(false);
      service.recordUsage(mockMeasure);
      expect(service.hasRecent()).toBe(true);
    });
  });

  describe('Import/Export', () => {
    it('should export data', () => {
      service.addFavorite(mockMeasure);
      service.recordUsage(mockMeasure2);

      const exported = service.exportData();
      expect(exported.favorites.length).toBe(1);
      expect(exported.recentMeasures.length).toBe(1);
    });

    it('should import data', () => {
      const data = {
        favorites: [
          {
            measureId: 'CDC',
            measureName: 'CDC',
            displayName: 'Comprehensive Diabetes Care',
            category: 'CHRONIC_DISEASE',
            favoritedAt: new Date().toISOString(),
          },
        ],
        recentMeasures: [
          {
            measureId: 'BCS',
            measureName: 'BCS',
            displayName: 'Breast Cancer Screening',
            category: 'PREVENTIVE',
            lastUsedAt: new Date().toISOString(),
            usageCount: 5,
          },
        ],
      };

      service.importData(data);
      expect(service.favorites().length).toBe(1);
      expect(service.recentMeasures().length).toBe(1);
      expect(service.recentMeasures()[0].usageCount).toBe(5);
    });
  });
});
