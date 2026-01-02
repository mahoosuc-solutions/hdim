import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, switchMap, mergeMap } from 'rxjs/operators';
import { CareRecommendationService } from '../../services/care-recommendation.service';
import { NotificationService } from '../../services/notification.service';
import * as RecommendationActions from '../actions/care-recommendation.actions';

/**
 * Care Recommendation Effects - Handle side effects for care recommendation actions
 * Uses inject() to ensure dependencies are available before class field initializers
 */
@Injectable()
export class CareRecommendationEffects {
  private readonly actions$ = inject(Actions);
  private readonly recommendationService = inject(CareRecommendationService);
  private readonly notificationService = inject(NotificationService);

  /**
   * Load recommendations effect
   */
  loadRecommendations$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.loadRecommendations),
      switchMap(({ refresh }) =>
        this.recommendationService.getDashboardRecommendations(refresh ?? false).pipe(
          map((recommendations) =>
            RecommendationActions.loadRecommendationsSuccess({ recommendations })
          ),
          catchError((error) => {
            this.notificationService.error('Failed to load care recommendations');
            return of(
              RecommendationActions.loadRecommendationsFailure({
                error: error.message || 'Unknown error',
              })
            );
          })
        )
      )
    )
  );

  /**
   * Refresh recommendations effect - triggers load with refresh flag
   */
  refreshRecommendations$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.refreshRecommendations),
      map(() => RecommendationActions.loadRecommendations({ refresh: true }))
    )
  );

  /**
   * Load statistics effect
   */
  loadStats$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.loadStats),
      switchMap(() =>
        this.recommendationService.getDashboardStats().pipe(
          map((stats) => RecommendationActions.loadStatsSuccess({ stats })),
          catchError((error) => {
            this.notificationService.error('Failed to load dashboard statistics');
            return of(
              RecommendationActions.loadStatsFailure({
                error: error.message || 'Unknown error',
              })
            );
          })
        )
      )
    )
  );

  /**
   * Accept recommendation effect
   */
  acceptRecommendation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.acceptRecommendation),
      mergeMap(({ id, notes }) =>
        this.recommendationService.acceptRecommendation(id, notes).pipe(
          map((recommendation) => {
            this.notificationService.success('Recommendation accepted');
            return RecommendationActions.acceptRecommendationSuccess({
              recommendation,
            });
          }),
          catchError((error) => {
            this.notificationService.error(
              `Failed to accept recommendation: ${error.message}`
            );
            return of(
              RecommendationActions.acceptRecommendationFailure({
                id,
                error: error.message || 'Unknown error',
              })
            );
          })
        )
      )
    )
  );

  /**
   * Decline recommendation effect
   */
  declineRecommendation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.declineRecommendation),
      mergeMap(({ id, reason, notes }) =>
        this.recommendationService.declineRecommendation(id, reason, notes).pipe(
          map((recommendation) => {
            this.notificationService.success('Recommendation declined');
            return RecommendationActions.declineRecommendationSuccess({
              recommendation,
            });
          }),
          catchError((error) => {
            this.notificationService.error(
              `Failed to decline recommendation: ${error.message}`
            );
            return of(
              RecommendationActions.declineRecommendationFailure({
                id,
                error: error.message || 'Unknown error',
              })
            );
          })
        )
      )
    )
  );

  /**
   * Complete recommendation effect
   */
  completeRecommendation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.completeRecommendation),
      mergeMap(({ id, outcome, notes }) =>
        this.recommendationService.completeRecommendation(id, outcome, notes).pipe(
          map((recommendation) => {
            this.notificationService.success('Recommendation completed');
            return RecommendationActions.completeRecommendationSuccess({
              recommendation,
            });
          }),
          catchError((error) => {
            this.notificationService.error(
              `Failed to complete recommendation: ${error.message}`
            );
            return of(
              RecommendationActions.completeRecommendationFailure({
                id,
                error: error.message || 'Unknown error',
              })
            );
          })
        )
      )
    )
  );

  /**
   * Bulk action effect
   */
  performBulkAction$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.performBulkAction),
      switchMap(({ request }) =>
        this.recommendationService.performBulkAction(request).pipe(
          map((result) => {
            this.notificationService.success(
              `Bulk action completed: ${result.successCount} succeeded, ${result.failureCount} failed`
            );
            return RecommendationActions.performBulkActionSuccess({ result });
          }),
          catchError((error) => {
            this.notificationService.error(
              `Bulk action failed: ${error.message}`
            );
            return of(
              RecommendationActions.performBulkActionFailure({
                error: error.message || 'Unknown error',
              })
            );
          })
        )
      )
    )
  );

  /**
   * Reload recommendations after successful bulk action
   */
  bulkActionSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RecommendationActions.performBulkActionSuccess),
      map(() => RecommendationActions.loadRecommendations({ refresh: true }))
    )
  );
}
