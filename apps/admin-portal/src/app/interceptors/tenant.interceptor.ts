import { HttpInterceptorFn } from '@angular/common/http';
import { API_CONFIG, HTTP_HEADERS } from '../config/api.config';

export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  // Add tenant header to all requests
  const clonedRequest = req.clone({
    setHeaders: {
      [HTTP_HEADERS.TENANT_ID]: API_CONFIG.DEFAULT_TENANT_ID,
    },
  });

  return next(clonedRequest);
};
