import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

/**
 * Loading Interceptor - Tracks HTTP request loading state
 *
 * Features:
 * - Shows loading indicator when requests are in progress
 * - Hides loading indicator when all requests complete
 * - Tracks multiple concurrent requests
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  // Don't show loading for certain endpoints
  const skipLoading = req.url.includes('/_health') || req.url.includes('/ping');

  if (!skipLoading) {
    loadingService.show();
  }

  return next(req).pipe(
    finalize(() => {
      if (!skipLoading) {
        loadingService.hide();
      }
    })
  );
};
