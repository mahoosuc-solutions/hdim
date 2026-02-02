/**
 * API Middleware
 *
 * Authentication, CORS, and rate limiting middleware for Vercel Functions.
 */

import type { VercelResponse } from '@vercel/node';
import type { AuthenticatedRequest, ApiHandler } from './types';
import { verifyAccessToken, extractToken } from './auth';

// Allowed origins for CORS
const ALLOWED_ORIGINS = [
  'https://admin-portal-jet-ten.vercel.app',
  'http://localhost:4200',
  'http://localhost:3000',
];

// Rate limiting storage (in-memory, resets on cold start)
const rateLimitStore = new Map<string, { count: number; resetTime: number }>();

// ============================================
// CORS Middleware
// ============================================

/**
 * Add CORS headers to response.
 */
export function setCorsHeaders(
  req: AuthenticatedRequest,
  res: VercelResponse
): void {
  const origin = req.headers.origin || '';

  // Check if origin is allowed
  if (ALLOWED_ORIGINS.includes(origin) || process.env.NODE_ENV === 'development') {
    res.setHeader('Access-Control-Allow-Origin', origin || '*');
  }

  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,PATCH,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization,X-Tenant-ID');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Max-Age', '86400');
}

/**
 * Handle CORS preflight requests.
 */
export function handleCors(
  req: AuthenticatedRequest,
  res: VercelResponse
): boolean {
  setCorsHeaders(req, res);

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return true;
  }

  return false;
}

// ============================================
// Authentication Middleware
// ============================================

/**
 * Wrap a handler to require authentication.
 */
export function withAuth(handler: ApiHandler): ApiHandler {
  return async (req: AuthenticatedRequest, res: VercelResponse) => {
    // Handle CORS preflight
    if (handleCors(req, res)) return;

    const token = extractToken(req.headers.authorization);

    if (!token) {
      res.status(401).json({
        message: 'Authentication required',
        code: 'UNAUTHORIZED',
      });
      return;
    }

    try {
      const payload = verifyAccessToken(token);
      req.user = {
        userId: payload.userId,
        email: payload.email,
        role: payload.role,
      };

      await handler(req, res);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Invalid token';

      res.status(401).json({
        message,
        code: 'INVALID_TOKEN',
      });
    }
  };
}

/**
 * Wrap a handler with optional authentication.
 * Populates req.user if token is valid, but doesn't require it.
 */
export function withOptionalAuth(handler: ApiHandler): ApiHandler {
  return async (req: AuthenticatedRequest, res: VercelResponse) => {
    if (handleCors(req, res)) return;

    const token = extractToken(req.headers.authorization);

    if (token) {
      try {
        const payload = verifyAccessToken(token);
        req.user = {
          userId: payload.userId,
          email: payload.email,
          role: payload.role,
        };
      } catch {
        // Token invalid, but that's ok - continue without auth
      }
    }

    await handler(req, res);
  };
}

// ============================================
// Rate Limiting Middleware
// ============================================

interface RateLimitConfig {
  windowMs: number; // Time window in milliseconds
  maxRequests: number; // Max requests per window
}

/**
 * Wrap a handler with rate limiting.
 */
export function withRateLimit(
  handler: ApiHandler,
  config: RateLimitConfig = { windowMs: 900000, maxRequests: 5 } // Default: 5 per 15 min
): ApiHandler {
  return async (req: AuthenticatedRequest, res: VercelResponse) => {
    if (handleCors(req, res)) return;

    // Get client identifier (IP or forwarded IP)
    const clientId =
      (req.headers['x-forwarded-for'] as string)?.split(',')[0] ||
      req.socket.remoteAddress ||
      'unknown';

    const now = Date.now();
    const record = rateLimitStore.get(clientId);

    if (record) {
      if (now > record.resetTime) {
        // Window expired, reset
        rateLimitStore.set(clientId, {
          count: 1,
          resetTime: now + config.windowMs,
        });
      } else if (record.count >= config.maxRequests) {
        // Rate limit exceeded
        const retryAfter = Math.ceil((record.resetTime - now) / 1000);
        res.setHeader('Retry-After', String(retryAfter));
        res.status(429).json({
          message: 'Too many requests. Please try again later.',
          code: 'RATE_LIMITED',
          retryAfter,
        });
        return;
      } else {
        // Increment counter
        record.count++;
      }
    } else {
      // First request from this client
      rateLimitStore.set(clientId, {
        count: 1,
        resetTime: now + config.windowMs,
      });
    }

    await handler(req, res);
  };
}

// ============================================
// Role-Based Access Control
// ============================================

/**
 * Wrap a handler to require specific roles.
 */
export function withRole(handler: ApiHandler, allowedRoles: string[]): ApiHandler {
  return withAuth(async (req: AuthenticatedRequest, res: VercelResponse) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      res.status(403).json({
        message: 'Insufficient permissions',
        code: 'FORBIDDEN',
      });
      return;
    }

    await handler(req, res);
  });
}

// ============================================
// Error Handling Middleware
// ============================================

/**
 * Wrap a handler with error handling.
 */
export function withErrorHandler(handler: ApiHandler): ApiHandler {
  return async (req: AuthenticatedRequest, res: VercelResponse) => {
    try {
      await handler(req, res);
    } catch (error) {
      console.error('API Error:', error);

      if (error instanceof Error) {
        // Check for known error types
        if (error.name === 'HttpError') {
          const httpError = error as Error & { statusCode: number; code?: string };
          res.status(httpError.statusCode).json({
            message: httpError.message,
            code: httpError.code,
          });
          return;
        }

        // Prisma errors
        if (error.message.includes('Unique constraint')) {
          res.status(409).json({
            message: 'Resource already exists',
            code: 'CONFLICT',
          });
          return;
        }

        if (error.message.includes('Record to update not found')) {
          res.status(404).json({
            message: 'Resource not found',
            code: 'NOT_FOUND',
          });
          return;
        }
      }

      // Generic error response - include details in non-production for debugging
      res.status(500).json({
        message: 'Internal server error',
        code: 'INTERNAL_ERROR',
        ...(process.env.NODE_ENV !== 'production' && error instanceof Error && {
          details: error.message,
          stack: error.stack,
        }),
        // Always show error details for now during debugging
        debug: error instanceof Error ? error.message : String(error),
      });
    }
  };
}

// ============================================
// Utility Functions
// ============================================

/**
 * Send a success response with data.
 */
export function sendSuccess<T>(
  res: VercelResponse,
  data: T,
  statusCode = 200
): void {
  res.status(statusCode).json(data);
}

/**
 * Send an error response.
 */
export function sendError(
  res: VercelResponse,
  message: string,
  statusCode = 400,
  code?: string
): void {
  res.status(statusCode).json({
    message,
    code: code || 'ERROR',
  });
}

/**
 * Parse query parameter as string.
 */
export function getQueryString(
  value: string | string[] | undefined
): string | undefined {
  if (Array.isArray(value)) return value[0];
  return value;
}

/**
 * Parse query parameter as number.
 */
export function getQueryNumber(
  value: string | string[] | undefined
): number | undefined {
  const str = getQueryString(value);
  if (!str) return undefined;
  const num = parseInt(str, 10);
  return isNaN(num) ? undefined : num;
}
