import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { ApiConfigService } from '../services/api-config.service';

/**
 * Tenant Interceptor (Shared)
 * Automatically adds X-Tenant-ID header to all outgoing HTTP requests
 * to backend services. Works across all micro frontends.
 */
export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  const apiConfig = inject(ApiConfigService);
  const tenantId = apiConfig.getTenantId();

  // Check if the request is to backend services
  const isBackendRequest = apiConfig.isBackendUrl(req.url);

  if (isBackendRequest && tenantId) {
    const clonedRequest = req.clone({
      setHeaders: {
        'X-Tenant-ID': tenantId,
      },
    });

    return next(clonedRequest);
  }

  return next(req);
};
