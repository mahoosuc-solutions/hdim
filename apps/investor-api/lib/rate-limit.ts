/**
 * Rate Limiting for Vercel Serverless Functions
 *
 * Uses in-memory store with IP-based tracking.
 * Note: In serverless, each function instance has its own memory,
 * so this provides per-instance rate limiting. For distributed
 * rate limiting, use Vercel KV or Redis.
 */

interface RateLimitEntry {
  count: number;
  resetTime: number;
}

// In-memory store (per function instance)
const rateLimitStore = new Map<string, RateLimitEntry>();

// Clean up expired entries periodically
const CLEANUP_INTERVAL = 60000; // 1 minute
let lastCleanup = Date.now();

function cleanupExpiredEntries(): void {
  const now = Date.now();
  if (now - lastCleanup < CLEANUP_INTERVAL) return;

  lastCleanup = now;
  for (const [key, entry] of rateLimitStore.entries()) {
    if (entry.resetTime < now) {
      rateLimitStore.delete(key);
    }
  }
}

export interface RateLimitConfig {
  /** Maximum requests allowed in the window */
  maxRequests: number;
  /** Window duration in milliseconds */
  windowMs: number;
  /** Key prefix for namespacing */
  keyPrefix?: string;
}

export interface RateLimitResult {
  /** Whether the request is allowed */
  allowed: boolean;
  /** Remaining requests in current window */
  remaining: number;
  /** Time until rate limit resets (ms) */
  resetIn: number;
  /** Total limit */
  limit: number;
}

/**
 * Check rate limit for a given identifier (usually IP or user ID)
 */
export function checkRateLimit(
  identifier: string,
  config: RateLimitConfig
): RateLimitResult {
  cleanupExpiredEntries();

  const key = `${config.keyPrefix || 'rl'}:${identifier}`;
  const now = Date.now();

  let entry = rateLimitStore.get(key);

  // If no entry or window expired, create new window
  if (!entry || entry.resetTime < now) {
    entry = {
      count: 1,
      resetTime: now + config.windowMs,
    };
    rateLimitStore.set(key, entry);

    return {
      allowed: true,
      remaining: config.maxRequests - 1,
      resetIn: config.windowMs,
      limit: config.maxRequests,
    };
  }

  // Increment count
  entry.count++;
  rateLimitStore.set(key, entry);

  const remaining = Math.max(0, config.maxRequests - entry.count);
  const resetIn = entry.resetTime - now;

  return {
    allowed: entry.count <= config.maxRequests,
    remaining,
    resetIn,
    limit: config.maxRequests,
  };
}

/**
 * Get client IP from Vercel request headers
 */
export function getClientIp(headers: Record<string, string | string[] | undefined>): string {
  // Vercel provides the real IP in x-forwarded-for
  const forwarded = headers['x-forwarded-for'];
  if (forwarded) {
    const ip = Array.isArray(forwarded) ? forwarded[0] : forwarded.split(',')[0];
    return ip.trim();
  }

  // Fallback to x-real-ip
  const realIp = headers['x-real-ip'];
  if (realIp) {
    return Array.isArray(realIp) ? realIp[0] : realIp;
  }

  return 'unknown';
}

// Pre-configured rate limiters
export const AUTH_RATE_LIMIT: RateLimitConfig = {
  maxRequests: 5,      // 5 attempts
  windowMs: 15 * 60 * 1000, // per 15 minutes
  keyPrefix: 'auth',
};

export const API_RATE_LIMIT: RateLimitConfig = {
  maxRequests: 100,    // 100 requests
  windowMs: 60 * 1000, // per minute
  keyPrefix: 'api',
};
