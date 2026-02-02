/**
 * GET /api/dashboard/stats
 *
 * Get aggregated dashboard statistics.
 * Returns task counts, contact metrics, activity summaries, and weekly progress.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { Pool } from 'pg';
import jwt from 'jsonwebtoken';

const JWT_SECRET = (process.env.JWT_SECRET || 'dev-secret-change-in-production').trim();

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
    res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    return;
  }

  // Verify auth
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    res.status(401).json({ message: 'Authentication required', code: 'UNAUTHORIZED' });
    return;
  }

  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') {
    res.status(401).json({ message: 'Invalid authorization header', code: 'INVALID_AUTH_HEADER' });
    return;
  }

  try {
    const payload = jwt.verify(parts[1], JWT_SECRET) as JwtPayload;
    if (payload.type !== 'access') {
      throw new Error('Invalid token type');
    }
  } catch {
    res.status(401).json({ message: 'Invalid token', code: 'INVALID_TOKEN' });
    return;
  }

  let pool: Pool | null = null;

  try {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false },
      max: 3,
    });

    // Get all stats in parallel for performance
    const [
      taskStats,
      contactStats,
      activityStats,
      weeklyProgress,
      recentResponses,
    ] = await Promise.all([
      getTaskStats(pool),
      getContactStats(pool),
      getActivityStats(pool),
      getWeeklyProgress(pool),
      getRecentResponses(pool),
    ]);

    const stats = {
      tasks: taskStats,
      contacts: contactStats,
      activities: {
        ...activityStats,
        recentResponses,
      },
      weeklyProgress,
    };

    res.status(200).json(stats);
  } catch (error) {
    console.error('Dashboard stats error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * Get task statistics by status.
 */
async function getTaskStats(pool: Pool): Promise<any> {
  const result = await pool.query(
    `SELECT status, COUNT(*)::int as count FROM investor_tasks GROUP BY status`
  );

  const statusCounts = result.rows.reduce(
    (acc, row) => {
      acc[row.status] = row.count;
      return acc;
    },
    {} as Record<string, number>
  );

  const total = result.rows.reduce((sum, row) => sum + row.count, 0);

  return {
    total,
    completed: statusCounts['completed'] || 0,
    inProgress: statusCounts['in_progress'] || 0,
    pending: statusCounts['pending'] || 0,
    blocked: statusCounts['blocked'] || 0,
  };
}

/**
 * Get contact statistics by tier and status.
 */
async function getContactStats(pool: Pool): Promise<any> {
  const [tierResult, statusResult] = await Promise.all([
    pool.query(`SELECT tier, COUNT(*)::int as count FROM investor_contacts GROUP BY tier`),
    pool.query(`SELECT status, COUNT(*)::int as count FROM investor_contacts GROUP BY status`),
  ]);

  const byTier = tierResult.rows.reduce(
    (acc, row) => {
      acc[row.tier] = row.count;
      return acc;
    },
    {} as Record<string, number>
  );

  const byStatus = statusResult.rows.reduce(
    (acc, row) => {
      acc[row.status] = row.count;
      return acc;
    },
    {} as Record<string, number>
  );

  const total = tierResult.rows.reduce((sum, row) => sum + row.count, 0);

  return {
    total,
    byTier,
    byStatus,
  };
}

/**
 * Get activity statistics.
 */
async function getActivityStats(pool: Pool): Promise<{
  total: number;
  thisWeek: number;
  byType: Record<string, number>;
}> {
  // Calculate start of current week (Monday)
  const now = new Date();
  const dayOfWeek = now.getDay();
  const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  const weekStart = new Date(now);
  weekStart.setDate(now.getDate() + mondayOffset);
  weekStart.setHours(0, 0, 0, 0);

  const [typeResult, weekCountResult] = await Promise.all([
    pool.query(
      `SELECT activity_type, COUNT(*)::int as count FROM outreach_activities GROUP BY activity_type`
    ),
    pool.query(
      `SELECT COUNT(*)::int as count FROM outreach_activities WHERE activity_date >= $1`,
      [weekStart]
    ),
  ]);

  const byType = typeResult.rows.reduce(
    (acc, row) => {
      acc[row.activity_type] = row.count;
      return acc;
    },
    {} as Record<string, number>
  );

  const total = typeResult.rows.reduce((sum, row) => sum + row.count, 0);

  return {
    total,
    thisWeek: weekCountResult.rows[0]?.count || 0,
    byType,
  };
}

/**
 * Get count of recent responses (last 7 days).
 */
async function getRecentResponses(pool: Pool): Promise<number> {
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  const result = await pool.query(
    `SELECT COUNT(*)::int as count FROM outreach_activities
     WHERE status = 'responded' AND response_received >= $1`,
    [sevenDaysAgo]
  );

  return result.rows[0]?.count || 0;
}

/**
 * Get weekly progress for tasks and activities.
 */
async function getWeeklyProgress(pool: Pool): Promise<any[]> {
  const weeks = [1, 2, 3, 4];

  const progress = await Promise.all(
    weeks.map(async (week) => {
      const [taskResult, totalActivities, responsesResult] = await Promise.all([
        pool.query(
          `SELECT status, COUNT(*)::int as count FROM investor_tasks WHERE week = $1 GROUP BY status`,
          [week]
        ),
        pool.query(`SELECT COUNT(*)::int as count FROM outreach_activities`),
        pool.query(`SELECT COUNT(*)::int as count FROM outreach_activities WHERE status = 'responded'`),
      ]);

      const completed =
        taskResult.rows.find((r) => r.status === 'completed')?.count || 0;
      const total = taskResult.rows.reduce((sum, r) => sum + r.count, 0);

      // For activities, we calculate based on approximation
      const totalActivityCount = totalActivities.rows[0]?.count || 0;
      const responsesReceived = responsesResult.rows[0]?.count || 0;

      return {
        week,
        tasksCompleted: completed,
        tasksTotal: total,
        outreachSent: Math.floor(totalActivityCount / 4), // Approximation
        responsesReceived: Math.floor(responsesReceived / 4), // Approximation
      };
    })
  );

  return progress;
}
