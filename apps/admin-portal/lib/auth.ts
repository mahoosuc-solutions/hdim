/**
 * Authentication Utilities
 *
 * JWT token generation, verification, and password hashing utilities.
 */

import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import type { JwtPayload, UserInfo } from './types';

// Environment variables with defaults
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-in-production';
const JWT_ACCESS_EXPIRY = process.env.JWT_ACCESS_EXPIRY || '24h';
const JWT_REFRESH_EXPIRY = process.env.JWT_REFRESH_EXPIRY || '7d';

// Salt rounds for bcrypt (10-12 recommended for production)
const SALT_ROUNDS = 10;

// ============================================
// Token Generation
// ============================================

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * Generate access and refresh tokens for a user.
 */
export function generateTokens(user: UserInfo): TokenPair {
  const accessPayload: JwtPayload = {
    userId: user.id,
    email: user.email,
    role: user.role,
    type: 'access',
  };

  const refreshPayload: JwtPayload = {
    userId: user.id,
    email: user.email,
    role: user.role,
    type: 'refresh',
  };

  const accessToken = jwt.sign(accessPayload, JWT_SECRET, {
    expiresIn: JWT_ACCESS_EXPIRY,
  });

  const refreshToken = jwt.sign(refreshPayload, JWT_SECRET, {
    expiresIn: JWT_REFRESH_EXPIRY,
  });

  // Parse expiry to seconds
  const expiresIn = parseExpiry(JWT_ACCESS_EXPIRY);

  return {
    accessToken,
    refreshToken,
    expiresIn,
  };
}

/**
 * Generate only an access token (for refresh operations).
 */
export function generateAccessToken(user: UserInfo): string {
  const payload: JwtPayload = {
    userId: user.id,
    email: user.email,
    role: user.role,
    type: 'access',
  };

  return jwt.sign(payload, JWT_SECRET, {
    expiresIn: JWT_ACCESS_EXPIRY,
  });
}

// ============================================
// Token Verification
// ============================================

/**
 * Verify an access token and return the payload.
 */
export function verifyAccessToken(token: string): JwtPayload {
  try {
    const payload = jwt.verify(token, JWT_SECRET) as JwtPayload;

    if (payload.type !== 'access') {
      throw new Error('Invalid token type');
    }

    return payload;
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      throw new Error('Token expired');
    }
    if (error instanceof jwt.JsonWebTokenError) {
      throw new Error('Invalid token');
    }
    throw error;
  }
}

/**
 * Verify a refresh token and return the payload.
 */
export function verifyRefreshToken(token: string): JwtPayload {
  try {
    const payload = jwt.verify(token, JWT_SECRET) as JwtPayload;

    if (payload.type !== 'refresh') {
      throw new Error('Invalid token type');
    }

    return payload;
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      throw new Error('Refresh token expired');
    }
    if (error instanceof jwt.JsonWebTokenError) {
      throw new Error('Invalid refresh token');
    }
    throw error;
  }
}

/**
 * Extract token from Authorization header.
 */
export function extractToken(authHeader: string | undefined): string | null {
  if (!authHeader) return null;

  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') {
    return null;
  }

  return parts[1];
}

// ============================================
// Password Hashing
// ============================================

/**
 * Hash a password using bcrypt.
 */
export async function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, SALT_ROUNDS);
}

/**
 * Compare a password against a hash.
 */
export async function comparePassword(
  password: string,
  hash: string
): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

// ============================================
// Utilities
// ============================================

/**
 * Parse expiry string to seconds.
 * Supports: '1h', '24h', '7d', '30d', etc.
 */
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

/**
 * Check if a lockout period has expired.
 */
export function isLockedOut(lockedUntil: Date | null): boolean {
  if (!lockedUntil) return false;
  return new Date() < lockedUntil;
}

/**
 * Calculate lockout time based on failed attempts.
 * Progressive lockout: 1 min, 5 min, 15 min, 1 hour.
 */
export function calculateLockoutTime(failedAttempts: number): Date | null {
  if (failedAttempts < 3) return null;

  const lockoutMinutes = [1, 5, 15, 60][Math.min(failedAttempts - 3, 3)];
  const lockoutTime = new Date();
  lockoutTime.setMinutes(lockoutTime.getMinutes() + lockoutMinutes);

  return lockoutTime;
}
