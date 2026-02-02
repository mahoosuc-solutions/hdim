/**
 * /api/tasks
 *
 * GET  - List all tasks (with optional filters)
 * POST - Create a new task
 */

import type { VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  withAuth,
  withErrorHandler,
  sendSuccess,
  sendError,
  getQueryString,
  getQueryNumber,
} from '../../lib/middleware';
import type {
  AuthenticatedRequest,
  CreateTaskRequest,
  TaskResponse,
} from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  switch (req.method) {
    case 'GET':
      await listTasks(req, res);
      break;
    case 'POST':
      await createTask(req, res);
      break;
    default:
      sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
  }
}

/**
 * GET /api/tasks
 * Query params: status, category, week
 */
async function listTasks(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const status = getQueryString(req.query.status);
  const category = getQueryString(req.query.category);
  const week = getQueryNumber(req.query.week);

  // Build where clause
  const where: Record<string, unknown> = {};
  if (status) where.status = status;
  if (category) where.category = category;
  if (week) where.week = week;

  const tasks = await prisma.task.findMany({
    where,
    orderBy: [{ week: 'asc' }, { sortOrder: 'asc' }],
  });

  const response: TaskResponse[] = tasks.map(formatTaskResponse);

  sendSuccess(res, response);
}

/**
 * POST /api/tasks
 * Create a new task
 */
async function createTask(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const data = req.body as CreateTaskRequest;

  // Validate required fields
  if (!data.subject || !data.category || data.week === undefined) {
    sendError(
      res,
      'subject, category, and week are required',
      400,
      'VALIDATION_ERROR'
    );
    return;
  }

  // Get next sort order for the week
  const maxSort = await prisma.task.aggregate({
    where: { week: data.week },
    _max: { sortOrder: true },
  });
  const nextSortOrder = (maxSort._max.sortOrder || 0) + 1;

  const task = await prisma.task.create({
    data: {
      subject: data.subject,
      description: data.description,
      category: data.category,
      week: data.week,
      deliverable: data.deliverable,
      owner: data.owner,
      dueDate: data.dueDate ? new Date(data.dueDate) : null,
      notes: data.notes,
      sortOrder: nextSortOrder,
    },
  });

  sendSuccess(res, formatTaskResponse(task), 201);
}

/**
 * Format task for API response.
 */
function formatTaskResponse(task: any): TaskResponse {
  return {
    id: task.id,
    subject: task.subject,
    description: task.description,
    status: task.status,
    category: task.category,
    week: task.week,
    deliverable: task.deliverable,
    owner: task.owner,
    dueDate: task.dueDate?.toISOString() || null,
    completedAt: task.completedAt?.toISOString() || null,
    notes: task.notes,
    sortOrder: task.sortOrder,
    createdAt: task.createdAt.toISOString(),
    updatedAt: task.updatedAt.toISOString(),
  };
}

export default withErrorHandler(withAuth(handler));
