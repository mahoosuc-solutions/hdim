import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoggerService } from '../../services/logger.service';
import { buildStarRatingUrl, STAR_RATING_ENDPOINTS } from '../../config/api.config';
import {
  StarRatingResponse,
  StarRatingTrendResponse,
  StarRatingSimulationRequest,
} from './star-ratings.model';

@Injectable({ providedIn: 'root' })
export class StarRatingsService {
  private readonly logger;

  constructor(
    private http: HttpClient,
    loggerService: LoggerService,
  ) {
    this.logger = loggerService.withContext('StarRatingsService');
  }

  getCurrentRating(): Observable<StarRatingResponse> {
    const url = buildStarRatingUrl(STAR_RATING_ENDPOINTS.CURRENT);
    this.logger.info('Fetching current star rating');
    return this.http.get<StarRatingResponse>(url);
  }

  getTrend(weeks = 12, granularity = 'WEEKLY'): Observable<StarRatingTrendResponse> {
    const url = buildStarRatingUrl(STAR_RATING_ENDPOINTS.TREND, {
      weeks: weeks.toString(),
      granularity,
    });
    this.logger.info('Fetching star rating trend', { weeks, granularity });
    return this.http.get<StarRatingTrendResponse>(url);
  }

  simulate(request: StarRatingSimulationRequest): Observable<StarRatingResponse> {
    const url = buildStarRatingUrl(STAR_RATING_ENDPOINTS.SIMULATE);
    this.logger.info('Running star rating simulation', { closureCount: request.closures.length });
    return this.http.post<StarRatingResponse>(url, request);
  }
}
