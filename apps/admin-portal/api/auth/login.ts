/**
 * POST /api/auth/login
 *
 * Authenticate user with email and password.
 * Returns JWT access and refresh tokens.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  generateTokens,
  comparePassword,
  isLockedOut,
  calculateLockoutTime,
} from '../../lib/auth';
import {
  handleCors,
  withRateLimit,
  withErrorHandler,
  sendSuccess,
  sendError,
} from '../../lib/middleware';
import type { LoginRequest, LoginResponse } from '../../lib/types';

async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  // Handle CORS
  if (handleCors(req as any, res)) return;

  // Only allow POST
  if (req.method !== 'POST') {
    sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
    return;
  }

  const { email, password } = req.body as LoginRequest;

  // Validate input
  if (!email || !password) {
    sendError(res, 'Email and password are required', 400, 'VALIDATION_ERROR');
    return;
  }

  // Find user by email
  const user = await prisma.user.findUnique({
    where: { email: email.toLowerCase() },
  });

  if (!user) {
    sendError(res, 'Invalid email or password', 401, 'INVALID_CREDENTIALS');
    return;
  }

  // Check if account is locked
  if (isLockedOut(user.lockedUntil)) {
    const minutesRemaining = Math.ceil(
      (user.lockedUntil!.getTime() - Date.now()) / 60000
    );
    sendError(
      res,
      `Account locked. Try again in ${minutesRemaining} minute(s).`,
      423,
      'ACCOUNT_LOCKED'
    );
    return;
  }

  // Check if account is active
  if (!user.active) {
    sendError(res, 'Account is deactivated', 403, 'ACCOUNT_DEACTIVATED');
    return;
  }

  // Verify password
  const isValidPassword = await comparePassword(password, user.passwordHash);

  if (!isValidPassword) {
    // Increment failed login attempts
    const newFailedAttempts = user.failedLoginAttempts + 1;
    const lockoutTime = calculateLockoutTime(newFailedAttempts);

    await prisma.user.update({
      where: { id: user.id },
      data: {
        failedLoginAttempts: newFailedAttempts,
        lockedUntil: lockoutTime,
      },
    });

    if (lockoutTime) {
      const minutesRemaining = Math.ceil(
        (lockoutTime.getTime() - Date.now()) / 60000
      );
      sendError(
        res,
        `Too many failed attempts. Account locked for ${minutesRemaining} minute(s).`,
        423,
        'ACCOUNT_LOCKED'
      );
      return;
    }

    sendError(res, 'Invalid email or password', 401, 'INVALID_CREDENTIALS');
    return;
  }

  // Reset failed login attempts and update last login
  await prisma.user.update({
    where: { id: user.id },
    data: {
      failedLoginAttempts: 0,
      lockedUntil: null,
      lastLogin: new Date(),
    },
  });

  // Generate tokens
  const userInfo = {
    id: user.id,
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    role: user.role,
  };

  const tokens = generateTokens(userInfo);

  const response: LoginResponse = {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    tokenType: 'Bearer',
    expiresIn: tokens.expiresIn,
    user: userInfo,
  };

  sendSuccess(res, response);
}

// Apply rate limiting: 5 attempts per 15 minutes per IP
export default withErrorHandler(
  withRateLimit(handler as any, {
    windowMs: 900000, // 15 minutes
    maxRequests: 5,
  }) as any
);
