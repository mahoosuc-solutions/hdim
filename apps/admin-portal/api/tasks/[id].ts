/**
 * /api/tasks/:id
 *
 * GET    - Get a single task
 * PUT    - Update a task
 * PATCH  - Update task status
 * DELETE - Delete a task
 */

import type { VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  withAuth,
  withErrorHandler,
  sendSuccess,
  sendError,
  getQueryString,
} from '../../lib/middleware';
import type {
  AuthenticatedRequest,
  UpdateTaskRequest,
  TaskResponse,
} from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const id = getQueryString(req.query.id);

  if (!id) {
    sendError(res, 'Task ID is required', 400, 'VALIDATION_ERROR');
    return;
  }

  switch (req.method) {
    case 'GET':
      await getTask(id, res);
      break;
    case 'PUT':
      await updateTask(id, req, res);
      break;
    case 'PATCH':
      await patchTaskStatus(id, req, res);
      break;
    case 'DELETE':
      await deleteTask(id, res);
      break;
    default:
      sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
  }
}

/**
 * GET /api/tasks/:id
 */
async function getTask(id: string, res: VercelResponse): Promise<void> {
  const task = await prisma.task.findUnique({
    where: { id },
  });

  if (!task) {
    sendError(res, 'Task not found', 404, 'NOT_FOUND');
    return;
  }

  sendSuccess(res, formatTaskResponse(task));
}

/**
 * PUT /api/tasks/:id
 * Full update of a task
 */
async function updateTask(
  id: string,
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const data = req.body as UpdateTaskRequest;

  // Check if task exists
  const existing = await prisma.task.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Task not found', 404, 'NOT_FOUND');
    return;
  }

  // Build update data
  const updateData: Record<string, unknown> = {};

  if (data.subject !== undefined) updateData.subject = data.subject;
  if (data.description !== undefined) updateData.description = data.description;
  if (data.status !== undefined) {
    updateData.status = data.status;
    // Set completedAt when marking as completed
    if (data.status === 'completed' && !existing.completedAt) {
      updateData.completedAt = new Date();
    } else if (data.status !== 'completed') {
      updateData.completedAt = null;
    }
  }
  if (data.category !== undefined) updateData.category = data.category;
  if (data.week !== undefined) updateData.week = data.week;
  if (data.deliverable !== undefined) updateData.deliverable = data.deliverable;
  if (data.owner !== undefined) updateData.owner = data.owner;
  if (data.dueDate !== undefined) {
    updateData.dueDate = data.dueDate ? new Date(data.dueDate) : null;
  }
  if (data.notes !== undefined) updateData.notes = data.notes;

  const task = await prisma.task.update({
    where: { id },
    data: updateData,
  });

  sendSuccess(res, formatTaskResponse(task));
}

/**
 * PATCH /api/tasks/:id/status
 * Update only the status field
 * Query param: status
 */
async function patchTaskStatus(
  id: string,
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const status = getQueryString(req.query.status) || req.body?.status;

  if (!status) {
    sendError(res, 'Status is required', 400, 'VALIDATION_ERROR');
    return;
  }

  const validStatuses = ['pending', 'in_progress', 'completed', 'blocked'];
  if (!validStatuses.includes(status)) {
    sendError(
      res,
      `Invalid status. Must be one of: ${validStatuses.join(', ')}`,
      400,
      'VALIDATION_ERROR'
    );
    return;
  }

  // Check if task exists
  const existing = await prisma.task.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Task not found', 404, 'NOT_FOUND');
    return;
  }

  const updateData: Record<string, unknown> = { status };

  // Set completedAt when marking as completed
  if (status === 'completed' && !existing.completedAt) {
    updateData.completedAt = new Date();
  } else if (status !== 'completed') {
    updateData.completedAt = null;
  }

  const task = await prisma.task.update({
    where: { id },
    data: updateData,
  });

  sendSuccess(res, formatTaskResponse(task));
}

/**
 * DELETE /api/tasks/:id
 */
async function deleteTask(id: string, res: VercelResponse): Promise<void> {
  // Check if task exists
  const existing = await prisma.task.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Task not found', 404, 'NOT_FOUND');
    return;
  }

  await prisma.task.delete({ where: { id } });

  res.status(204).end();
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
