/**
 * POST /api/auth/login
 *
 * Authenticate user with email and password.
 * Returns JWT access and refresh tokens.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { Pool } from 'pg';
import bcrypt from 'bcryptjs';
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
  res.setHeader('Access-Control-Max-Age', '86400');

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
    // Read and parse body manually to avoid Vercel body parser issues
    const chunks: Buffer[] = [];
    for await (const chunk of req) {
      chunks.push(Buffer.from(chunk));
    }
    const rawBody = Buffer.concat(chunks).toString('utf-8');

    let email: string;
    let password: string;

    try {
      const parsed = JSON.parse(rawBody);
      email = parsed.email;
      password = parsed.password;
    } catch {
      res.status(400).json({
        message: 'Invalid request body - expected JSON with email and password',
        code: 'INVALID_BODY',
      });
      return;
    }

    // Validate input
    if (!email || !password) {
      res.status(400).json({
        message: 'Email and password are required',
        code: 'VALIDATION_ERROR',
      });
      return;
    }

    // Connect to database
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false },
      max: 3, // Limit connections for serverless
    });

    // Find user by email
    const userResult = await pool.query(
      `SELECT id, email, first_name, last_name, role, password_hash,
              active, failed_login_attempts, locked_until
       FROM investor_users WHERE email = $1`,
      [email.toLowerCase()]
    );

    if (userResult.rows.length === 0) {
      res.status(401).json({
        message: 'Invalid email or password',
        code: 'INVALID_CREDENTIALS',
      });
      return;
    }

    const user = userResult.rows[0];

    // Check if account is locked
    if (user.locked_until && new Date(user.locked_until) > new Date()) {
      const minutesRemaining = Math.ceil(
        (new Date(user.locked_until).getTime() - Date.now()) / 60000
      );
      res.status(423).json({
        message: `Account locked. Try again in ${minutesRemaining} minute(s).`,
        code: 'ACCOUNT_LOCKED',
      });
      return;
    }

    // Check if account is active
    if (!user.active) {
      res.status(403).json({
        message: 'Account is deactivated',
        code: 'ACCOUNT_DEACTIVATED',
      });
      return;
    }

    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password_hash);

    if (!isValidPassword) {
      // Increment failed login attempts
      const newFailedAttempts = user.failed_login_attempts + 1;
      let lockoutTime: Date | null = null;

      // Progressive lockout: 3 fails = 1min, 4 = 5min, 5 = 15min, 6+ = 1hour
      if (newFailedAttempts >= 3) {
        const lockoutMinutes = [1, 5, 15, 60][Math.min(newFailedAttempts - 3, 3)];
        lockoutTime = new Date(Date.now() + lockoutMinutes * 60000);
      }

      await pool.query(
        'UPDATE investor_users SET failed_login_attempts = $1, locked_until = $2 WHERE id = $3',
        [newFailedAttempts, lockoutTime, user.id]
      );

      if (lockoutTime) {
        const minutesRemaining = Math.ceil(
          (lockoutTime.getTime() - Date.now()) / 60000
        );
        res.status(423).json({
          message: `Too many failed attempts. Account locked for ${minutesRemaining} minute(s).`,
          code: 'ACCOUNT_LOCKED',
        });
        return;
      }

      res.status(401).json({
        message: 'Invalid email or password',
        code: 'INVALID_CREDENTIALS',
      });
      return;
    }

    // Reset failed login attempts and update last login
    await pool.query(
      'UPDATE investor_users SET failed_login_attempts = 0, locked_until = NULL, last_login = NOW() WHERE id = $1',
      [user.id]
    );

    // Generate tokens
    const userInfo = {
      id: user.id,
      email: user.email,
      firstName: user.first_name,
      lastName: user.last_name,
      role: user.role,
    };

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

    const refreshToken = jwt.sign(refreshPayload, JWT_SECRET, {
      expiresIn: JWT_REFRESH_EXPIRY,
    } as SignOptions);

    // Parse expiry for response
    const expiresIn = parseExpiry(JWT_ACCESS_EXPIRY);

    res.status(200).json({
      accessToken,
      refreshToken,
      tokenType: 'Bearer',
      expiresIn,
      user: userInfo,
    });

  } catch (error) {
    console.error('Login error:', error);
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
  if (!match) return 86400; // Default 24 hours

  const value = parseInt(match[1], 10);
  const unit = match[2];

  switch (unit) {
    case 'h':
      return value * 3600;
    case 'd':
      return value * 86400;
    case 'm':
      return value * 60;
    default:
      return 86400;
  }
}
