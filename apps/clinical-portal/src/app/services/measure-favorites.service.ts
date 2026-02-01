import { Injectable, signal, computed } from '@angular/core';
import { LoggerService } from './logger.service';
import { MeasureInfo } from '../models/cql-library.model';

const STORAGE_KEYS = {
  FAVORITES: 'hdim_measure_favorites',
  RECENT: 'hdim_measure_recent',
};

const MAX_RECENT_MEASURES = 10;

export interface FavoriteMeasure {
  measureId: string;
  measureName: string;
  displayName: string;
  category?: string;
  favoritedAt: string;
}

export interface RecentMeasure {
  measureId: string;
  measureName: string;
  displayName: string;
  category?: string;
  lastUsedAt: string;
  usageCount: number;
}

/**
 * MeasureFavoritesService - Manages user's favorite and recently used measures
 * Uses localStorage for persistence across sessions
 */
@Injectable({
  providedIn: 'root',
})
export class MeasureFavoritesService {
  // Reactive signals for favorites and recents
  private _favorites = signal<FavoriteMeasure[]>([]);
  private _recentMeasures = signal<RecentMeasure[]>([]);

  // Public computed properties
  readonly favorites = computed(() => this._favorites());
  readonly recentMeasures = computed(() => this._recentMeasures());
  readonly favoritesCount = computed(() => this._favorites().length);
  readonly hasRecent = computed(() => this._recentMeasures().length > 0);

  constructor(
    private logger: LoggerService,) {
    this.loadFromStorage();
  }

  /**
   * Load favorites and recents from localStorage
   */
  private loadFromStorage(): void {
    try {
      const favoritesJson = localStorage.getItem(STORAGE_KEYS.FAVORITES);
      const recentJson = localStorage.getItem(STORAGE_KEYS.RECENT);

      if (favoritesJson) {
        this._favorites.set(JSON.parse(favoritesJson));
      }
      if (recentJson) {
        this._recentMeasures.set(JSON.parse(recentJson));
      }
    } catch (error) {
      this.logger.error('Error loading measure favorites from storage:', { error });
      this._favorites.set([]);
      this._recentMeasures.set([]);
    }
  }

  /**
   * Save favorites to localStorage
   */
  private saveFavorites(): void {
    try {
      localStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify(this._favorites()));
    } catch (error) {
      this.logger.error('Error saving measure favorites:', { error });
    }
  }

  /**
   * Save recent measures to localStorage
   */
  private saveRecent(): void {
    try {
      localStorage.setItem(STORAGE_KEYS.RECENT, JSON.stringify(this._recentMeasures()));
    } catch (error) {
      this.logger.error('Error saving recent measures:', { error });
    }
  }

  /**
   * Check if a measure is favorited
   */
  isFavorite(measureId: string): boolean {
    return this._favorites().some(f => f.measureId === measureId);
  }

  /**
   * Add a measure to favorites
   */
  addFavorite(measure: MeasureInfo): void {
    if (this.isFavorite(measure.id)) {
      return; // Already favorited
    }

    const favorite: FavoriteMeasure = {
      measureId: measure.id,
      measureName: measure.name,
      displayName: measure.displayName,
      category: measure.category,
      favoritedAt: new Date().toISOString(),
    };

    this._favorites.update(favorites => [...favorites, favorite]);
    this.saveFavorites();
  }

  /**
   * Remove a measure from favorites
   */
  removeFavorite(measureId: string): void {
    this._favorites.update(favorites =>
      favorites.filter(f => f.measureId !== measureId)
    );
    this.saveFavorites();
  }

  /**
   * Toggle favorite status for a measure
   */
  toggleFavorite(measure: MeasureInfo): boolean {
    if (this.isFavorite(measure.id)) {
      this.removeFavorite(measure.id);
      return false;
    } else {
      this.addFavorite(measure);
      return true;
    }
  }

  /**
   * Record a measure as recently used
   */
  recordUsage(measure: MeasureInfo): void {
    const existingIndex = this._recentMeasures().findIndex(
      r => r.measureId === measure.id
    );

    let updatedRecent: RecentMeasure[];

    if (existingIndex >= 0) {
      // Update existing entry
      const existing = this._recentMeasures()[existingIndex];
      const updated: RecentMeasure = {
        ...existing,
        lastUsedAt: new Date().toISOString(),
        usageCount: existing.usageCount + 1,
      };
      updatedRecent = [
        updated,
        ...this._recentMeasures().filter((_, i) => i !== existingIndex),
      ];
    } else {
      // Add new entry
      const newRecent: RecentMeasure = {
        measureId: measure.id,
        measureName: measure.name,
        displayName: measure.displayName,
        category: measure.category,
        lastUsedAt: new Date().toISOString(),
        usageCount: 1,
      };
      updatedRecent = [newRecent, ...this._recentMeasures()];
    }

    // Keep only the most recent N measures
    updatedRecent = updatedRecent.slice(0, MAX_RECENT_MEASURES);

    this._recentMeasures.set(updatedRecent);
    this.saveRecent();
  }

  /**
   * Get favorite measures as MeasureInfo array
   */
  getFavoriteIds(): string[] {
    return this._favorites().map(f => f.measureId);
  }

  /**
   * Get recent measure IDs
   */
  getRecentIds(): string[] {
    return this._recentMeasures().map(r => r.measureId);
  }

  /**
   * Clear all recent measures
   */
  clearRecent(): void {
    this._recentMeasures.set([]);
    this.saveRecent();
  }

  /**
   * Clear all favorites
   */
  clearFavorites(): void {
    this._favorites.set([]);
    this.saveFavorites();
  }

  /**
   * Get most frequently used measures
   */
  getMostUsed(limit = 5): RecentMeasure[] {
    return [...this._recentMeasures()]
      .sort((a, b) => b.usageCount - a.usageCount)
      .slice(0, limit);
  }

  /**
   * Export favorites and recents for backup
   */
  exportData(): { favorites: FavoriteMeasure[]; recentMeasures: RecentMeasure[] } {
    return {
      favorites: this._favorites(),
      recentMeasures: this._recentMeasures(),
    };
  }

  /**
   * Import favorites and recents from backup
   */
  importData(data: { favorites: FavoriteMeasure[]; recentMeasures: RecentMeasure[] }): void {
    if (data.favorites) {
      this._favorites.set(data.favorites);
      this.saveFavorites();
    }
    if (data.recentMeasures) {
      this._recentMeasures.set(data.recentMeasures);
      this.saveRecent();
    }
  }
}
