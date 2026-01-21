/**
 * Business Metrics Service
 *
 * Tracks user engagement, feature adoption, ROI metrics, and satisfaction scores.
 *
 * Features:
 * - User engagement metrics (views, interactions, time on page)
 * - Feature adoption tracking and rate calculation
 * - ROI calculation (time saved, cost saved)
 * - User satisfaction scoring (1-5 scale)
 * - Metric aggregation and trending
 * - Observable streams for metric updates
 *
 * Usage:
 * ```typescript
 * constructor(private businessMetrics: BusinessMetricsService) {}
 *
 * onPageView() {
 *   this.businessMetrics.recordPageView('dashboard');
 * }
 *
 * onUserInteraction() {
 *   this.businessMetrics.recordInteraction('notification_clicked', {
 *     notificationId: '123'
 *   });
 * }
 *
 * onFeatureUsage(featureName: string) {
 *   this.businessMetrics.recordFeatureAdoption(featureName);
 * }
 *
 * onSubmitSatisfaction(score: number) {
 *   this.businessMetrics.recordSatisfactionScore(score, 'feature-x');
 * }
 *
 * // Get metrics
 * const engagement = this.businessMetrics.getEngagementMetrics();
 * const adoption = this.businessMetrics.getAdoptionMetrics();
 * const roi = this.businessMetrics.getROIMetrics();
 *
 * // Observables
 * this.businessMetrics.metrics$.subscribe(metrics => {
 *   console.log('Metrics updated:', metrics);
 * });
 * ```
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

export interface EngagementMetrics {
  pageViews: number;
  pageViewsBySection: Record<string, number>;
  interactionCount: number;
  interactionsByType: Record<string, number>;
  timeOnPage: number; // seconds
  sessionsCount: number;
  averageSessionDuration: number; // seconds
  bounceRate: number; // percentage
}

export interface AdoptionMetrics {
  featureName: string;
  adoptionRate: number; // percentage
  usageCount: number;
  uniqueUsers: number;
  lastUsedAt: number;
  adoptionTrend: 'growing' | 'stable' | 'declining';
}

export interface ROIMetrics {
  featureName: string;
  timeSavedPerUser: number; // minutes
  totalTimeSaved: number; // minutes
  costSavedPerUser: number; // dollars
  totalCostSaved: number; // dollars
  roi: number; // percentage
  paybackPeriod: number; // days
}

export interface SatisfactionMetrics {
  featureName: string;
  averageScore: number; // 1-5
  totalScores: number;
  scoreDistribution: Record<number, number>; // score -> count
  nps: number; // Net Promoter Score (9-10 are promoters, 7-8 are passives, 0-6 are detractors)
}

export interface BusinessMetricsData {
  engagement: EngagementMetrics;
  adoption: Map<string, AdoptionMetrics>;
  roi: Map<string, ROIMetrics>;
  satisfaction: Map<string, SatisfactionMetrics>;
}

@Injectable({
  providedIn: 'root'
})
export class BusinessMetricsService {
  private metricsSubject = new BehaviorSubject<BusinessMetricsData>({
    engagement: this.createEmptyEngagementMetrics(),
    adoption: new Map(),
    roi: new Map(),
    satisfaction: new Map()
  });

  public readonly metrics$: Observable<BusinessMetricsData> = this.metricsSubject.asObservable();

  private sessionId: string;
  private sessionStartTime: number;
  private pageViewStack: Array<{ page: string; startTime: number }> = [];
  private featureAdoptionLog = new Map<string, number[]>(); // featureName -> [timestamps]
  private satisfactionScores = new Map<string, number[]>(); // featureName -> [scores]
  private interactions: Array<{ type: string; timestamp: number }> = [];
  private pageViews: Array<{ page: string; timestamp: number }> = [];

  constructor() {
    this.sessionId = uuidv4();
    this.sessionStartTime = Date.now();
    this.loadMetricsFromStorage();
  }

  /**
   * Record page view
   */
  recordPageView(pageName: string): void {
    const now = Date.now();

    // End previous page view
    if (this.pageViewStack.length > 0) {
      const previous = this.pageViewStack[this.pageViewStack.length - 1];
      const timeOnPage = (now - previous.startTime) / 1000;
      this.updateEngagementMetrics(metrics => {
        metrics.timeOnPage += timeOnPage;
      });
    }

    this.pageViewStack.push({ page: pageName, startTime: now });
    this.pageViews.push({ page: pageName, timestamp: now });

    this.updateEngagementMetrics(metrics => {
      metrics.pageViews++;
      metrics.pageViewsBySection[pageName] = (metrics.pageViewsBySection[pageName] || 0) + 1;
    });
  }

  /**
   * Record user interaction
   */
  recordInteraction(interactionType: string, metadata?: Record<string, any>): void {
    this.interactions.push({
      type: interactionType,
      timestamp: Date.now()
    });

    this.updateEngagementMetrics(metrics => {
      metrics.interactionCount++;
      metrics.interactionsByType[interactionType] = (metrics.interactionsByType[interactionType] || 0) + 1;
    });
  }

  /**
   * Record feature adoption
   */
  recordFeatureAdoption(featureName: string): void {
    const now = Date.now();

    if (!this.featureAdoptionLog.has(featureName)) {
      this.featureAdoptionLog.set(featureName, []);
    }
    this.featureAdoptionLog.get(featureName)!.push(now);

    // Update adoption metrics
    this.updateAdoptionMetrics(featureName);
  }

  /**
   * Record user satisfaction score
   */
  recordSatisfactionScore(score: number, featureName: string): void {
    if (score < 1 || score > 5) {
      throw new Error('Score must be between 1 and 5');
    }

    if (!this.satisfactionScores.has(featureName)) {
      this.satisfactionScores.set(featureName, []);
    }
    this.satisfactionScores.get(featureName)!.push(score);

    // Update satisfaction metrics
    this.updateSatisfactionMetrics(featureName);
  }

  /**
   * Update ROI metrics for feature
   */
  updateROI(
    featureName: string,
    timeSavedPerUser: number,
    costSavedPerUser: number
  ): void {
    const adoption = this.metricsSubject.value.adoption.get(featureName);
    if (!adoption) {
      return;
    }

    const roi: ROIMetrics = {
      featureName,
      timeSavedPerUser,
      totalTimeSaved: timeSavedPerUser * adoption.uniqueUsers,
      costSavedPerUser,
      totalCostSaved: costSavedPerUser * adoption.uniqueUsers,
      roi: ((costSavedPerUser * adoption.uniqueUsers) / (adoption.uniqueUsers * 100)) * 100,
      paybackPeriod: 0 // Would be calculated based on development cost
    };

    const updated = { ...this.metricsSubject.value };
    updated.roi.set(featureName, roi);
    this.metricsSubject.next(updated);
    this.persistMetrics();
  }

  /**
   * Get engagement metrics
   */
  getEngagementMetrics(): EngagementMetrics {
    return this.metricsSubject.value.engagement;
  }

  /**
   * Get adoption metrics for feature
   */
  getAdoptionMetrics(featureName?: string): AdoptionMetrics | Map<string, AdoptionMetrics> {
    if (featureName) {
      return this.metricsSubject.value.adoption.get(featureName) || this.createEmptyAdoptionMetrics(featureName);
    }
    return this.metricsSubject.value.adoption;
  }

  /**
   * Get ROI metrics for feature
   */
  getROIMetrics(featureName?: string): ROIMetrics | Map<string, ROIMetrics> {
    if (featureName) {
      return this.metricsSubject.value.roi.get(featureName) || this.createEmptyROIMetrics(featureName);
    }
    return this.metricsSubject.value.roi;
  }

  /**
   * Get satisfaction metrics for feature
   */
  getSatisfactionMetrics(featureName?: string): SatisfactionMetrics | Map<string, SatisfactionMetrics> {
    if (featureName) {
      return this.metricsSubject.value.satisfaction.get(featureName) || this.createEmptySatisfactionMetrics(featureName);
    }
    return this.metricsSubject.value.satisfaction;
  }

  /**
   * Get all metrics
   */
  getAllMetrics(): BusinessMetricsData {
    return this.metricsSubject.value;
  }

  /**
   * Get session ID
   */
  getSessionId(): string {
    return this.sessionId;
  }

  /**
   * Get session duration
   */
  getSessionDuration(): number {
    return (Date.now() - this.sessionStartTime) / 1000; // seconds
  }

  /**
   * Clear all metrics (for testing)
   */
  clearMetrics(): void {
    this.metricsSubject.next({
      engagement: this.createEmptyEngagementMetrics(),
      adoption: new Map(),
      roi: new Map(),
      satisfaction: new Map()
    });
    localStorage.removeItem('business_metrics');
  }

  private updateEngagementMetrics(updater: (metrics: EngagementMetrics) => void): void {
    const current = this.metricsSubject.value;
    const updated = { ...current };
    updater(updated.engagement);

    // Recalculate derived metrics
    updated.engagement.averageSessionDuration = updated.engagement.timeOnPage / Math.max(updated.engagement.sessionsCount, 1);
    updated.engagement.bounceRate = updated.engagement.pageViews > 0
      ? (updated.engagement.sessionsCount / updated.engagement.pageViews) * 100
      : 0;

    this.metricsSubject.next(updated);
    this.persistMetrics();
  }

  private updateAdoptionMetrics(featureName: string): void {
    const adoptionLog = this.featureAdoptionLog.get(featureName) || [];
    const current = this.metricsSubject.value;
    const updated = { ...current };

    // Calculate adoption rate (assuming ~100 users for demo)
    const uniqueUsers = new Set([this.sessionId]).size; // In real app, track actual unique users
    const adoptionRate = Math.min((adoptionLog.length / Math.max(uniqueUsers, 1)) * 100, 100);

    // Determine trend
    let adoptionTrend: 'growing' | 'stable' | 'declining' = 'stable';
    if (adoptionLog.length > 2) {
      const recent = adoptionLog.slice(-5);
      const older = adoptionLog.slice(-10, -5);
      if (recent.length > older.length) {
        adoptionTrend = 'growing';
      } else if (recent.length < older.length) {
        adoptionTrend = 'declining';
      }
    }

    const adoption: AdoptionMetrics = {
      featureName,
      adoptionRate,
      usageCount: adoptionLog.length,
      uniqueUsers,
      lastUsedAt: adoptionLog.length > 0 ? adoptionLog[adoptionLog.length - 1] : 0,
      adoptionTrend
    };

    updated.adoption.set(featureName, adoption);
    this.metricsSubject.next(updated);
    this.persistMetrics();
  }

  private updateSatisfactionMetrics(featureName: string): void {
    const scores = this.satisfactionScores.get(featureName) || [];
    const current = this.metricsSubject.value;
    const updated = { ...current };

    // Calculate distribution
    const distribution: Record<number, number> = {};
    for (let i = 1; i <= 5; i++) {
      distribution[i] = scores.filter(s => s === i).length;
    }

    // Calculate NPS (promoters - detractors)
    const promoters = scores.filter(s => s >= 9).length;
    const detractors = scores.filter(s => s <= 6).length;
    const nps = scores.length > 0 ? ((promoters - detractors) / scores.length) * 100 : 0;

    // Calculate average
    const average = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0;

    const satisfaction: SatisfactionMetrics = {
      featureName,
      averageScore: average,
      totalScores: scores.length,
      scoreDistribution: distribution,
      nps
    };

    updated.satisfaction.set(featureName, satisfaction);
    this.metricsSubject.next(updated);
    this.persistMetrics();
  }

  private createEmptyEngagementMetrics(): EngagementMetrics {
    return {
      pageViews: 0,
      pageViewsBySection: {},
      interactionCount: 0,
      interactionsByType: {},
      timeOnPage: 0,
      sessionsCount: 1,
      averageSessionDuration: 0,
      bounceRate: 0
    };
  }

  private createEmptyAdoptionMetrics(featureName: string): AdoptionMetrics {
    return {
      featureName,
      adoptionRate: 0,
      usageCount: 0,
      uniqueUsers: 0,
      lastUsedAt: 0,
      adoptionTrend: 'stable'
    };
  }

  private createEmptyROIMetrics(featureName: string): ROIMetrics {
    return {
      featureName,
      timeSavedPerUser: 0,
      totalTimeSaved: 0,
      costSavedPerUser: 0,
      totalCostSaved: 0,
      roi: 0,
      paybackPeriod: 0
    };
  }

  private createEmptySatisfactionMetrics(featureName: string): SatisfactionMetrics {
    return {
      featureName,
      averageScore: 0,
      totalScores: 0,
      scoreDistribution: {},
      nps: 0
    };
  }

  private persistMetrics(): void {
    const data = this.metricsSubject.value;
    const serialized = {
      engagement: data.engagement,
      adoption: Array.from(data.adoption.entries()),
      roi: Array.from(data.roi.entries()),
      satisfaction: Array.from(data.satisfaction.entries())
    };
    localStorage.setItem('business_metrics', JSON.stringify(serialized));
  }

  private loadMetricsFromStorage(): void {
    const stored = localStorage.getItem('business_metrics');
    if (stored) {
      try {
        const data = JSON.parse(stored);
        const current = this.metricsSubject.value;
        this.metricsSubject.next({
          engagement: data.engagement || current.engagement,
          adoption: new Map(data.adoption || []),
          roi: new Map(data.roi || []),
          satisfaction: new Map(data.satisfaction || [])
        });
      } catch (e) {
        console.error('Failed to load business metrics:', e);
      }
    }
  }
}
