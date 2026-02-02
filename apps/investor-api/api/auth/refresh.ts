/**
 * POST /api/auth/refresh
 *
 * Refresh access token using a valid refresh token.
 * Returns new access and refresh tokens.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { Pool } from 'pg';
import jwt, { SignOptions } from 'jsonwebtoken';

// Environment variables (trimmed to handle trailing newlines from Vercel env)
const JWT_SECRET = (process.env.JWT_SECRET || 'dev-secret-change-in-production').trim();
const JWT_ACCESS_EXPIRY = (process.env.JWT_ACCESS_EXPIRY || '24h').trim();
const JWT_REFRESH_EXPIRY = (process.env.JWT_REFRESH_EXPIRY || '7d').trim();

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
  res.setHeader('Access-Control-Allow-Methods', 'POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  if (req.method !== 'POST') {
    res.status(405).json({
      message: 'Method not allowed',
      code: 'METHOD_NOT_ALLOWED',
    });
    return;
  }

  let pool: Pool | null = null;

  try {
    // Vercel auto-parses JSON body when Content-Type is application/json
    let refreshToken: string;

    try {
      if (req.body && typeof req.body === 'object') {
        refreshToken = req.body.refreshToken;
      } else if (typeof req.body === 'string') {
        const parsed = JSON.parse(req.body);
        refreshToken = parsed.refreshToken;
      } else {
        const chunks: Buffer[] = [];
        for await (const chunk of req) {
          chunks.push(Buffer.from(chunk));
        }
        const rawBody = Buffer.concat(chunks).toString('utf-8');
        const parsed = JSON.parse(rawBody);
        refreshToken = parsed.refreshToken;
      }
    } catch {
      res.status(400).json({
        message: 'Invalid request body',
        code: 'INVALID_BODY',
      });
      return;
    }

    // Validate input
    if (!refreshToken) {
      res.status(400).json({
        message: 'Refresh token is required',
        code: 'VALIDATION_ERROR',
      });
      return;
    }

    // Verify refresh token
    let payload: JwtPayload;
    try {
      payload = jwt.verify(refreshToken, JWT_SECRET) as JwtPayload;

      if (payload.type !== 'refresh') {
        throw new Error('Invalid token type');
      }
    } catch (error) {
      res.status(401).json({
        message: error instanceof Error ? error.message : 'Invalid refresh token',
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

    // Get user from database (to ensure they still exist and are active)
    const userResult = await pool.query(
      'SELECT id, email, first_name, last_name, role, active FROM investor_users WHERE id = $1',
      [payload.userId]
    );

    if (userResult.rows.length === 0) {
      res.status(401).json({
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

    // Generate new tokens
    const accessPayload = {
      userId: user.id,
      email: user.email,
      role: user.role,
      type: 'access',
    };

    const refreshPayload = {
      userId: user.id,
      email: user.email,
      role: user.role,
      type: 'refresh',
    };

    const accessToken = jwt.sign(accessPayload, JWT_SECRET, {
      expiresIn: JWT_ACCESS_EXPIRY,
    } as SignOptions);

    const newRefreshToken = jwt.sign(refreshPayload, JWT_SECRET, {
      expiresIn: JWT_REFRESH_EXPIRY,
    } as SignOptions);

    const expiresIn = parseExpiry(JWT_ACCESS_EXPIRY);

    res.status(200).json({
      accessToken,
      refreshToken: newRefreshToken,
      tokenType: 'Bearer',
      expiresIn,
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name,
        role: user.role,
      },
    });

  } catch (error) {
    console.error('Refresh error:', error);
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

function parseExpiry(expiry: string): number {
  const match = expiry.match(/^(\d+)([hdm])$/);
  if (!match) return 86400;
  const value = parseInt(match[1], 10);
  const unit = match[2];
  switch (unit) {
    case 'h': return value * 3600;
    case 'd': return value * 86400;
    case 'm': return value * 60;
    default: return 86400;
  }
}
