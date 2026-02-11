/**
 * GET /api/auth/me
 *
 * Get current authenticated user information.
 * Requires valid access token.
 *
 * Security:
 * - Rate limited: 100 requests per minute per IP
 * - JWT access token required
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { Pool } from 'pg';
import jwt from 'jsonwebtoken';
import { checkRateLimit, getClientIp, API_RATE_LIMIT } from '../../lib/rate-limit';

// Validate required environment variables
const JWT_SECRET = process.env.JWT_SECRET?.trim();
if (!JWT_SECRET) {
  if (process.env.NODE_ENV === 'production' || process.env.VERCEL_ENV === 'production') {
    throw new Error('JWT_SECRET environment variable is required in production');
  }
  console.warn('WARNING: Using insecure default JWT_SECRET for development');
}
const EFFECTIVE_JWT_SECRET = JWT_SECRET || 'dev-only-insecure-secret-do-not-use-in-production';

// Allowed origins for CORS
const ALLOWED_ORIGINS = [
  'https://admin-portal-jet-ten.vercel.app',
  'http://localhost:4200',
  'http://localhost:3000',
];

interface JwtPayload {
  userId: string;
  email: string;
  role: string;
  type: 'access' | 'refresh';
}

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  // CORS headers
  const origin = req.headers.origin || '';
  if (ALLOWED_ORIGINS.includes(origin) || process.env.NODE_ENV === 'development') {
    res.setHeader('Access-Control-Allow-Origin', origin || '*');
  }
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  if (req.method !== 'GET') {
    res.status(405).json({
      message: 'Method not allowed',
      code: 'METHOD_NOT_ALLOWED',
    });
    return;
  }

  // Rate limiting
  const clientIp = getClientIp(req.headers as Record<string, string | string[] | undefined>);
  const rateLimit = checkRateLimit(clientIp, API_RATE_LIMIT);

  res.setHeader('X-RateLimit-Limit', rateLimit.limit.toString());
  res.setHeader('X-RateLimit-Remaining', rateLimit.remaining.toString());
  res.setHeader('X-RateLimit-Reset', Math.ceil(rateLimit.resetIn / 1000).toString());

  if (!rateLimit.allowed) {
    res.status(429).json({
      message: 'Too many requests. Please try again later.',
      code: 'RATE_LIMITED',
      retryAfter: Math.ceil(rateLimit.resetIn / 1000),
    });
    return;
  }

  let pool: Pool | null = null;

  try {
    // Extract and verify token
    const authHeader = req.headers.authorization;
    if (!authHeader) {
      res.status(401).json({
        message: 'Authentication required',
        code: 'UNAUTHORIZED',
      });
      return;
    }

    const parts = authHeader.split(' ');
    if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') {
      res.status(401).json({
        message: 'Invalid authorization header',
        code: 'INVALID_AUTH_HEADER',
      });
      return;
    }

    const token = parts[1];

    let payload: JwtPayload;
    try {
      payload = jwt.verify(token, EFFECTIVE_JWT_SECRET) as JwtPayload;
      if (payload.type !== 'access') {
        throw new Error('Invalid token type');
      }
    } catch (error) {
      res.status(401).json({
        message: error instanceof Error ? error.message : 'Invalid token',
        code: 'INVALID_TOKEN',
      });
      return;
    }

    // Connect to database
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false },
      max: 3,
    });

    // Get fresh user data from database
    const userResult = await pool.query(
      `SELECT u.id, u.email, u.first_name, u.last_name, u.role, u.active, u.last_login,
              lc.connected as linkedin_connected, lc.profile_url as linkedin_profile_url
       FROM investor_users u
       LEFT JOIN linkedin_connections lc ON lc.user_id = u.id
       WHERE u.id = $1`,
      [payload.userId]
    );

    if (userResult.rows.length === 0) {
      res.status(404).json({
        message: 'User not found',
        code: 'USER_NOT_FOUND',
      });
      return;
    }

    const user = userResult.rows[0];

    if (!user.active) {
      res.status(403).json({
        message: 'Account is deactivated',
        code: 'ACCOUNT_DEACTIVATED',
      });
      return;
    }

    const response: any = {
      id: user.id,
      email: user.email,
      firstName: user.first_name,
      lastName: user.last_name,
      role: user.role,
    };

    // Include LinkedIn status if connected
    if (user.linkedin_connected) {
      response.linkedIn = {
        connected: true,
        profileUrl: user.linkedin_profile_url,
      };
    }

    res.status(200).json(response);

  } catch (error) {
    console.error('Me endpoint error:', error);
    res.status(500).json({
      message: 'Internal server error',
      code: 'INTERNAL_ERROR',
    });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}
