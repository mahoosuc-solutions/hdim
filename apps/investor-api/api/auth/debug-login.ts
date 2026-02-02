/**
 * POST /api/auth/debug-login
 *
 * Debug endpoint to isolate the "Invalid JSON" error in login flow.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import { comparePassword } from '../../lib/auth';

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  const steps: { step: string; success: boolean; data?: any; error?: string }[] = [];

  try {
    // Step 1: Parse request body
    steps.push({ step: '1-parse-body', success: true, data: { bodyType: typeof req.body, body: req.body } });

    const { email, password } = req.body || {};
    steps.push({ step: '2-extract-credentials', success: true, data: { email, hasPassword: !!password } });

    if (!email || !password) {
      res.status(400).json({ steps, error: 'Missing email or password' });
      return;
    }

    // Step 2: Find user in database
    let user;
    try {
      user = await prisma.user.findUnique({
        where: { email: email.toLowerCase() },
      });
      steps.push({ step: '3-find-user', success: true, data: { found: !!user, userId: user?.id } });
    } catch (dbError) {
      steps.push({ step: '3-find-user', success: false, error: dbError instanceof Error ? dbError.message : String(dbError) });
      res.status(500).json({ steps, error: 'Database query failed' });
      return;
    }

    if (!user) {
      res.status(401).json({ steps, error: 'User not found' });
      return;
    }

    // Step 3: Compare password
    let isValid;
    try {
      isValid = await comparePassword(password, user.passwordHash);
      steps.push({ step: '4-compare-password', success: true, data: { isValid } });
    } catch (bcryptError) {
      steps.push({ step: '4-compare-password', success: false, error: bcryptError instanceof Error ? bcryptError.message : String(bcryptError) });
      res.status(500).json({ steps, error: 'Password comparison failed' });
      return;
    }

    if (!isValid) {
      res.status(401).json({ steps, error: 'Invalid password' });
      return;
    }

    // Step 4: All checks passed
    steps.push({ step: '5-success', success: true, data: { userId: user.id, email: user.email, role: user.role } });

    res.status(200).json({
      message: 'Debug login successful - all steps passed',
      steps,
      user: {
        id: user.id,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        role: user.role,
      }
    });

  } catch (error) {
    steps.push({ step: 'unexpected-error', success: false, error: error instanceof Error ? error.message : String(error) });
    res.status(500).json({ steps, error: 'Unexpected error' });
  }
}
