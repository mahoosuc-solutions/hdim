/**
 * GET /api/dashboard/stats
 *
 * Get aggregated dashboard statistics.
 * Returns task counts, contact metrics, activity summaries, and weekly progress.
 */

import type { VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  withAuth,
  withErrorHandler,
  sendSuccess,
  sendError,
} from '../../lib/middleware';
import type { AuthenticatedRequest, DashboardStats } from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  // Only allow GET
  if (req.method !== 'GET') {
    sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
    return;
  }

  // Get all stats in parallel for performance
  const [
    taskStats,
    contactStats,
    activityStats,
    weeklyProgress,
    recentResponses,
  ] = await Promise.all([
    getTaskStats(),
    getContactStats(),
    getActivityStats(),
    getWeeklyProgress(),
    getRecentResponses(),
  ]);

  const stats: DashboardStats = {
    tasks: taskStats,
    contacts: contactStats,
    activities: {
      ...activityStats,
      recentResponses,
    },
    weeklyProgress,
  };

  sendSuccess(res, stats);
}

/**
 * Get task statistics by status.
 */
async function getTaskStats(): Promise<DashboardStats['tasks']> {
  const tasks = await prisma.task.groupBy({
    by: ['status'],
    _count: { id: true },
  });

  const statusCounts = tasks.reduce(
    (acc, t) => {
      acc[t.status] = t._count.id;
      return acc;
    },
    {} as Record<string, number>
  );

  const total = tasks.reduce((sum, t) => sum + t._count.id, 0);

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
async function getContactStats(): Promise<DashboardStats['contacts']> {
  const [tierGroups, statusGroups] = await Promise.all([
    prisma.contact.groupBy({
      by: ['tier'],
      _count: { id: true },
    }),
    prisma.contact.groupBy({
      by: ['status'],
      _count: { id: true },
    }),
  ]);

  const byTier = tierGroups.reduce(
    (acc, t) => {
      acc[t.tier] = t._count.id;
      return acc;
    },
    {} as Record<string, number>
  );

  const byStatus = statusGroups.reduce(
    (acc, s) => {
      acc[s.status] = s._count.id;
      return acc;
    },
    {} as Record<string, number>
  );

  const total = tierGroups.reduce((sum, t) => sum + t._count.id, 0);

  return {
    total,
    byTier,
    byStatus,
  };
}

/**
 * Get activity statistics.
 */
async function getActivityStats(): Promise<{
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

  const [typeGroups, thisWeekCount] = await Promise.all([
    prisma.outreachActivity.groupBy({
      by: ['activityType'],
      _count: { id: true },
    }),
    prisma.outreachActivity.count({
      where: {
        activityDate: { gte: weekStart },
      },
    }),
  ]);

  const byType = typeGroups.reduce(
    (acc, t) => {
      acc[t.activityType] = t._count.id;
      return acc;
    },
    {} as Record<string, number>
  );

  const total = typeGroups.reduce((sum, t) => sum + t._count.id, 0);

  return {
    total,
    thisWeek: thisWeekCount,
    byType,
  };
}

/**
 * Get count of recent responses (last 7 days).
 */
async function getRecentResponses(): Promise<number> {
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  return prisma.outreachActivity.count({
    where: {
      status: 'responded',
      responseReceived: { gte: sevenDaysAgo },
    },
  });
}

/**
 * Get weekly progress for tasks and activities.
 */
async function getWeeklyProgress(): Promise<DashboardStats['weeklyProgress']> {
  const weeks = [1, 2, 3, 4];

  const progress = await Promise.all(
    weeks.map(async (week) => {
      const [taskCounts, activities] = await Promise.all([
        prisma.task.groupBy({
          by: ['status'],
          where: { week },
          _count: { id: true },
        }),
        prisma.outreachActivity.count({
          // Activities don't have week field, so we'll count all
          // In a real app, you might filter by date ranges
        }),
      ]);

      const completed =
        taskCounts.find((t) => t.status === 'completed')?._count.id || 0;
      const total = taskCounts.reduce((sum, t) => sum + t._count.id, 0);

      // For activities, we'd calculate based on date ranges in production
      // For now, return total activities / 4 as approximation
      const responsesReceived = await prisma.outreachActivity.count({
        where: { status: 'responded' },
      });

      return {
        week,
        tasksCompleted: completed,
        tasksTotal: total,
        outreachSent: Math.floor(activities / 4), // Approximation
        responsesReceived: Math.floor(responsesReceived / 4), // Approximation
      };
    })
  );

  return progress;
}

export default withErrorHandler(withAuth(handler));
