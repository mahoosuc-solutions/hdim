/**
 * /api/tasks/:id
 *
 * GET    - Get a single task
 * PUT    - Update a task
 * PATCH  - Update task status
 * DELETE - Delete a task
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { Pool } from 'pg';
import jwt from 'jsonwebtoken';
import { parseBody } from '../../lib/parse-body';

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
  res.setHeader('Access-Control-Allow-Methods', 'GET,PUT,PATCH,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
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

  const id = req.query.id as string;
  if (!id) {
    res.status(400).json({ message: 'Task ID is required', code: 'VALIDATION_ERROR' });
    return;
  }

  let pool: Pool | null = null;

  try {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false },
      max: 3,
    });

    switch (req.method) {
      case 'GET':
        await getTask(id, res, pool);
        break;
      case 'PUT':
        await updateTask(id, req, res, pool);
        break;
      case 'PATCH':
        await patchTaskStatus(id, req, res, pool);
        break;
      case 'DELETE':
        await deleteTask(id, res, pool);
        break;
      default:
        res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    }
  } catch (error) {
    console.error('Task endpoint error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * GET /api/tasks/:id
 */
async function getTask(id: string, res: VercelResponse, pool: Pool): Promise<void> {
  const result = await pool.query(
    `SELECT id, subject, description, status, category, week, deliverable, owner,
            due_date, completed_at, notes, sort_order, created_at, updated_at
     FROM investor_tasks WHERE id = $1`,
    [id]
  );

  if (result.rows.length === 0) {
    res.status(404).json({ message: 'Task not found', code: 'NOT_FOUND' });
    return;
  }

  res.status(200).json(formatTaskResponse(result.rows[0]));
}

/**
 * PUT /api/tasks/:id
 * Full update of a task
 */
async function updateTask(
  id: string,
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  // Check if task exists
  const existing = await pool.query('SELECT * FROM investor_tasks WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Task not found', code: 'NOT_FOUND' });
    return;
  }

  const data = await parseBody(req);
  if (!data) {
    res.status(400).json({ message: 'Invalid request body', code: 'INVALID_BODY' });
    return;
  }

  const existingTask = existing.rows[0];

  // Build update dynamically
  const updates: string[] = [];
  const params: any[] = [];
  let paramIndex = 1;

  if (data.subject !== undefined) {
    updates.push(`subject = $${paramIndex++}`);
    params.push(data.subject);
  }
  if (data.description !== undefined) {
    updates.push(`description = $${paramIndex++}`);
    params.push(data.description);
  }
  if (data.status !== undefined) {
    updates.push(`status = $${paramIndex++}`);
    params.push(data.status);
    // Set completedAt when marking as completed
    if (data.status === 'completed' && !existingTask.completed_at) {
      updates.push(`completed_at = $${paramIndex++}`);
      params.push(new Date());
    } else if (data.status !== 'completed') {
      updates.push(`completed_at = $${paramIndex++}`);
      params.push(null);
    }
  }
  if (data.category !== undefined) {
    updates.push(`category = $${paramIndex++}`);
    params.push(data.category);
  }
  if (data.week !== undefined) {
    updates.push(`week = $${paramIndex++}`);
    params.push(data.week);
  }
  if (data.deliverable !== undefined) {
    updates.push(`deliverable = $${paramIndex++}`);
    params.push(data.deliverable);
  }
  if (data.owner !== undefined) {
    updates.push(`owner = $${paramIndex++}`);
    params.push(data.owner);
  }
  if (data.dueDate !== undefined) {
    updates.push(`due_date = $${paramIndex++}`);
    params.push(data.dueDate ? new Date(data.dueDate) : null);
  }
  if (data.notes !== undefined) {
    updates.push(`notes = $${paramIndex++}`);
    params.push(data.notes);
  }

  if (updates.length === 0) {
    res.status(200).json(formatTaskResponse(existingTask));
    return;
  }

  // Add updated_at
  updates.push(`updated_at = $${paramIndex++}`);
  params.push(new Date());

  // Add id as last param
  params.push(id);

  const result = await pool.query(
    `UPDATE investor_tasks SET ${updates.join(', ')} WHERE id = $${paramIndex}
     RETURNING id, subject, description, status, category, week, deliverable, owner,
               due_date, completed_at, notes, sort_order, created_at, updated_at`,
    params
  );

  res.status(200).json(formatTaskResponse(result.rows[0]));
}

/**
 * PATCH /api/tasks/:id
 * Update only the status field
 */
async function patchTaskStatus(
  id: string,
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  // Check if task exists
  const existing = await pool.query('SELECT * FROM investor_tasks WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Task not found', code: 'NOT_FOUND' });
    return;
  }

  // Try query param first, then body
  let status = req.query.status as string | undefined;

  if (!status) {
    const data = await parseBody<{ status?: string }>(req);
    if (data?.status) {
      status = data.status;
    }
  }

  if (!status) {
    res.status(400).json({ message: 'Status is required', code: 'VALIDATION_ERROR' });
    return;
  }

  const validStatuses = ['pending', 'in_progress', 'completed', 'blocked'];
  if (!validStatuses.includes(status)) {
    res.status(400).json({
      message: `Invalid status. Must be one of: ${validStatuses.join(', ')}`,
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  const existingTask = existing.rows[0];
  let completedAt = existingTask.completed_at;

  // Set completedAt when marking as completed
  if (status === 'completed' && !existingTask.completed_at) {
    completedAt = new Date();
  } else if (status !== 'completed') {
    completedAt = null;
  }

  const result = await pool.query(
    `UPDATE investor_tasks SET status = $1, completed_at = $2, updated_at = $3 WHERE id = $4
     RETURNING id, subject, description, status, category, week, deliverable, owner,
               due_date, completed_at, notes, sort_order, created_at, updated_at`,
    [status, completedAt, new Date(), id]
  );

  res.status(200).json(formatTaskResponse(result.rows[0]));
}

/**
 * DELETE /api/tasks/:id
 */
async function deleteTask(id: string, res: VercelResponse, pool: Pool): Promise<void> {
  const existing = await pool.query('SELECT id FROM investor_tasks WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Task not found', code: 'NOT_FOUND' });
    return;
  }

  await pool.query('DELETE FROM investor_tasks WHERE id = $1', [id]);
  res.status(204).end();
}

/**
 * Format task for API response (snake_case → camelCase)
 */
function formatTaskResponse(task: any): any {
  return {
    id: task.id,
    subject: task.subject,
    description: task.description,
    status: task.status,
    category: task.category,
    week: task.week,
    deliverable: task.deliverable,
    owner: task.owner,
    dueDate: task.due_date?.toISOString() || null,
    completedAt: task.completed_at?.toISOString() || null,
    notes: task.notes,
    sortOrder: task.sort_order,
    createdAt: task.created_at?.toISOString() || new Date().toISOString(),
    updatedAt: task.updated_at?.toISOString() || new Date().toISOString(),
  };
}
