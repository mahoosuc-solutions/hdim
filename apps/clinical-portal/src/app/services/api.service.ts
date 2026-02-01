import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, timer, of } from 'rxjs';
import { catchError, retry, retryWhen, mergeMap, finalize, tap } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';
import { LoggerService } from './logger.service';

/**
 * Base API Service - Central configuration for all HTTP operations
 * Provides common HTTP methods with error handling, retry logic, and logging
 *
 * Features:
 * - Automatic retry with exponential backoff
 * - Request/response logging
 * - Error transformation
 * - Timeout handling
 * - API versioning support
 */
@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly defaultTimeout = 30000; // 30 seconds
  private readonly maxRetries = 3;
  private readonly retryDelay = 1000; // 1 second
  private readonly maxRetryDelay = 10000; // 10 seconds  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {}

  /**
   * Generic GET request with error handling and retry
   */
  get<T>(
    url: string,
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): Observable<T> {
    const requestOptions = this.buildRequestOptions(params, options);

    return this.http.get<T>(url, requestOptions).pipe(
      tap((response) => this.logResponse('GET', url, response)),
      retry(options.retry !== undefined ? options.retry : this.maxRetries),
      catchError((error) => this.handleError(error, 'GET', url))
    );
  }

  /**
   * Generic POST request with error handling
   */
  post<T>(
    url: string,
    body: unknown,
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): Observable<T> {
    const requestOptions = this.buildRequestOptions(params, options);

    return this.http.post<T>(url, body, requestOptions).pipe(
      tap((response) => this.logResponse('POST', url, response)),
      retry(options.retry !== undefined ? options.retry : 0), // POST typically shouldn't auto-retry
      catchError((error) => this.handleError(error, 'POST', url))
    );
  }

  /**
   * Generic PUT request with error handling
   */
  put<T>(
    url: string,
    body: unknown,
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): Observable<T> {
    const requestOptions = this.buildRequestOptions(params, options);

    return this.http.put<T>(url, body, requestOptions).pipe(
      tap((response) => this.logResponse('PUT', url, response)),
      catchError((error) => this.handleError(error, 'PUT', url))
    );
  }

  /**
   * Generic PATCH request with error handling
   */
  patch<T>(
    url: string,
    body: unknown,
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): Observable<T> {
    const requestOptions = this.buildRequestOptions(params, options);

    return this.http.patch<T>(url, body, requestOptions).pipe(
      tap((response) => this.logResponse('PATCH', url, response)),
      catchError((error) => this.handleError(error, 'PATCH', url))
    );
  }

  /**
   * Generic DELETE request with error handling
   */
  delete<T>(
    url: string,
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): Observable<T> {
    const requestOptions = this.buildRequestOptions(params, options);

    return this.http.delete<T>(url, requestOptions).pipe(
      tap((response) => this.logResponse('DELETE', url, response)),
      catchError((error) => this.handleError(error, 'DELETE', url))
    );
  }

  /**
   * GET request with retry logic using exponential backoff
   */
  getWithRetry<T>(
    url: string,
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): Observable<T> {
    const requestOptions = this.buildRequestOptions(params, options);
    const maxRetries = options.maxRetries || this.maxRetries;

    return this.http.get<T>(url, requestOptions).pipe(
      tap((response) => this.logResponse('GET', url, response)),
      retryWhen((errors) => this.exponentialBackoff(errors, maxRetries)),
      catchError((error) => this.handleError(error, 'GET', url))
    );
  }

  /**
   * Build request options with headers and params
   *
   * SECURITY: withCredentials is set to true by default to support HttpOnly cookies.
   * This is required for HIPAA-compliant JWT storage that protects against XSS attacks.
   */
  private buildRequestOptions(
    params?: HttpParams | { [param: string]: string | string[] },
    options: RequestOptions = {}
  ): {
    headers?: HttpHeaders;
    params?: HttpParams | { [param: string]: string | string[] };
    observe?: 'body';
    responseType?: 'json';
    withCredentials: boolean;
  } {
    let headers = options.headers || new HttpHeaders();

    // Add default headers
    if (!headers.has('Content-Type')) {
      headers = headers.set('Content-Type', 'application/json');
    }

    // Add API version header if specified
    if (options.apiVersion) {
      headers = headers.set('API-Version', options.apiVersion);
    }

    return {
      headers,
      params,
      observe: 'body',
      responseType: 'json',
      // SECURITY: Enable credentials for HttpOnly cookie support
      // Required for HIPAA-compliant JWT storage that prevents XSS token theft
      withCredentials: options.withCredentials !== false,
    };
  }

  /**
   * Exponential backoff retry strategy
   */
  private exponentialBackoff(errors: Observable<unknown>, maxRetries: number): Observable<unknown> {
    return errors.pipe(
      mergeMap((error, index) => {
        const retryAttempt = index + 1;

        if (retryAttempt > maxRetries) {
          return throwError(() => error);
        }

        // Check if error is retryable
        if (error instanceof HttpErrorResponse) {
          // Don't retry client errors (4xx), only server errors (5xx) and network errors
          if (error.status >= 400 && error.status < 500) {
            return throwError(() => error);
          }
        }

        const delay = Math.min(
          this.retryDelay * Math.pow(2, retryAttempt - 1),
          this.maxRetryDelay
        );

        this.logRetry(retryAttempt, maxRetries, delay);

        return timer(delay);
      })
    );
  }

  /**
   * Handle HTTP errors and transform them into user-friendly messages
   */
  private handleError(error: HttpErrorResponse, method: string, url: string): Observable<never> {
    let errorMessage = 'An unknown error occurred';

    if (error.error instanceof ErrorEvent) {
      // Client-side or network error
      errorMessage = `Network error: ${error.error.message}`;
    } else {
      // Backend error
      switch (error.status) {
        case 0:
          errorMessage = 'Unable to connect to server. Please check your network connection.';
          break;
        case 400:
          errorMessage = error.error?.message || 'Bad request. Please check your input.';
          break;
        case 401:
          errorMessage = 'Unauthorized. Please log in again.';
          break;
        case 403:
          errorMessage = 'Access denied. You do not have permission to access this resource.';
          break;
        case 404:
          errorMessage = 'Resource not found.';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflict. The resource already exists or has been modified.';
          break;
        case 422:
          errorMessage = error.error?.message || 'Validation error. Please check your input.';
          break;
        case 429:
          errorMessage = 'Too many requests. Please try again later.';
          break;
        case 500:
          errorMessage = 'Internal server error. Please try again later.';
          break;
        case 502:
          errorMessage = 'Bad gateway. The server is temporarily unavailable.';
          break;
        case 503:
          errorMessage = 'Service unavailable. Please try again later.';
          break;
        case 504:
          errorMessage = 'Gateway timeout. The request took too long to complete.';
          break;
        default:
          errorMessage = error.error?.message || `Server error: ${error.status} ${error.statusText}`;
      }
    }

    this.logError(method, url, error, errorMessage);

    return throwError(() => ({
      originalError: error,
      message: errorMessage,
      status: error.status,
      statusText: error.statusText,
      url: error.url,
    }));
  }

  /**
   * Log successful response (can be disabled in production)
   */
  private logResponse(method: string, url: string, response: unknown): void {
    if (!API_CONFIG.ENABLE_LOGGING) return;

    this.logger.info(`${method} ${url}`, response);
  }

  /**
   * Log error response
   */
  private logError(method: string, url: string, error: HttpErrorResponse, message: string): void {
    if (!API_CONFIG.ENABLE_LOGGING) return;

    this.logger.error(`${method} ${url} - Error`, {
      message,
      status: error.status,
      statusText: error.statusText,
      error: error.error,
    });
  }

  /**
   * Log retry attempt
   */
  private logRetry(attempt: number, maxRetries: number, delay: number): void {
    if (!API_CONFIG.ENABLE_LOGGING) return;

    this.logger.info(`Retry attempt ${attempt}/${maxRetries} after ${delay}ms`);
  }
}

/**
 * Request options interface
 */
export interface RequestOptions {
  headers?: HttpHeaders;
  retry?: number;
  maxRetries?: number;
  apiVersion?: string;
  skipAuth?: boolean;
  /**
   * Include credentials (cookies) with the request.
   * Default: true (for HttpOnly cookie authentication support)
   * Set to false only for cross-origin requests that shouldn't send cookies.
   */
  withCredentials?: boolean;
}
