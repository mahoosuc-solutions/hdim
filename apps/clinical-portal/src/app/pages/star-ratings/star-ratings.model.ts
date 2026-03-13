/**
 * Star Ratings Models
 *
 * TypeScript interfaces matching the care-gap-event-service DTOs:
 * - StarRatingResponse
 * - StarRatingTrendResponse / StarRatingTrendPointResponse
 * - StarRatingSimulationRequest / SimulatedGapClosureRequest
 * - StarDomainSummaryResponse
 * - StarMeasureSummaryResponse
 */

export interface StarRatingResponse {
  tenantId: string;
  overallRating: number;
  roundedRating: number;
  measureCount: number;
  openGapCount: number;
  closedGapCount: number;
  qualityBonusEligible: boolean;
  lastTriggerEvent: string;
  calculatedAt: string; // ISO 8601
  domains: StarDomainSummary[];
  measures: StarMeasureSummary[];
}

export interface StarDomainSummary {
  domain: string;
  domainStars: number;
  measureCount: number;
  averagePerformanceRate: number;
}

export interface StarMeasureSummary {
  measureCode: string;
  measureName: string;
  domain: string;
  numerator: number;
  denominator: number;
  performanceRate: number;
  stars: number;
}

export interface StarRatingTrendResponse {
  tenantId: string;
  points: StarRatingTrendPoint[];
}

export interface StarRatingTrendPoint {
  snapshotDate: string; // YYYY-MM-DD
  granularity: 'WEEKLY' | 'MONTHLY';
  overallRating: number;
  roundedRating: number;
  openGapCount: number;
  closedGapCount: number;
  qualityBonusEligible: boolean;
}

export interface StarRatingSimulationRequest {
  closures: SimulatedGapClosure[];
}

export interface SimulatedGapClosure {
  gapCode: string;
  closures: number;
}

/**
 * Star rating tier for color coding.
 * Maps CMS star levels to visual presentation.
 */
export type StarTier = 'critical' | 'below-average' | 'average' | 'above-average' | 'bonus' | 'exceptional';

export function getStarTier(rating: number): StarTier {
  if (rating < 2.0) return 'critical';
  if (rating < 3.0) return 'below-average';
  if (rating < 3.5) return 'average';
  if (rating < 4.0) return 'above-average';
  if (rating < 4.5) return 'bonus';
  return 'exceptional';
}

export function getStarColor(rating: number): string {
  switch (getStarTier(rating)) {
    case 'critical': return '#D32F2F';
    case 'below-average': return '#F57C00';
    case 'average': return '#FBC02D';
    case 'above-average': return '#66BB6A';
    case 'bonus': return '#388E3C';
    case 'exceptional': return '#FFD700';
  }
}
