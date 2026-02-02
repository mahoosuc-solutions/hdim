/**
 * /api/tasks
 *
 * GET  - List all tasks (with optional filters)
 * POST - Create a new task
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
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
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

  let pool: Pool | null = null;

  try {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false },
      max: 3,
    });

    switch (req.method) {
      case 'GET':
        await listTasks(req, res, pool);
        break;
      case 'POST':
        await createTask(req, res, pool);
        break;
      default:
        res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    }
  } catch (error) {
    console.error('Tasks endpoint error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * GET /api/tasks
 * Query params: status, category, week
 */
async function listTasks(
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  const status = req.query.status as string | undefined;
  const category = req.query.category as string | undefined;
  const week = req.query.week ? parseInt(req.query.week as string, 10) : undefined;

  // Build WHERE clause dynamically
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  let paramIndex = 1;

  if (status) {
    conditions.push(`status = $${paramIndex++}`);
    params.push(status);
  }
  if (category) {
    conditions.push(`category = $${paramIndex++}`);
    params.push(category);
  }
  if (week !== undefined && !isNaN(week)) {
    conditions.push(`week = $${paramIndex++}`);
    params.push(week);
  }

  const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

  const result = await pool.query(
    `SELECT id, subject, description, status, category, week, deliverable, owner,
            due_date, completed_at, notes, sort_order, created_at, updated_at
     FROM investor_tasks
     ${whereClause}
     ORDER BY week ASC, sort_order ASC`,
    params
  );

  const tasks = result.rows.map(formatTaskResponse);
  res.status(200).json(tasks);
}

/**
 * POST /api/tasks
 * Create a new task
 */
async function createTask(
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  // Manual body parsing
  const chunks: Buffer[] = [];
  for await (const chunk of req) {
    chunks.push(Buffer.from(chunk));
  }
  const rawBody = Buffer.concat(chunks).toString('utf-8');

  let data: any;
  try {
    data = JSON.parse(rawBody);
  } catch {
    res.status(400).json({ message: 'Invalid request body', code: 'INVALID_BODY' });
    return;
  }

  // Validate required fields
  if (!data.subject || !data.category || data.week === undefined) {
    res.status(400).json({
      message: 'subject, category, and week are required',
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  // Get next sort order for the week
  const maxSortResult = await pool.query(
    'SELECT MAX(sort_order) as max_sort FROM investor_tasks WHERE week = $1',
    [data.week]
  );
  const nextSortOrder = (maxSortResult.rows[0]?.max_sort || 0) + 1;

  const result = await pool.query(
    `INSERT INTO investor_tasks (id, subject, description, category, week, deliverable, owner, due_date, notes, sort_order, created_at, updated_at)
     VALUES (gen_random_uuid(), $1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW())
     RETURNING id, subject, description, status, category, week, deliverable, owner,
               due_date, completed_at, notes, sort_order, created_at, updated_at`,
    [
      data.subject,
      data.description || null,
      data.category,
      data.week,
      data.deliverable || null,
      data.owner || null,
      data.dueDate ? new Date(data.dueDate) : null,
      data.notes || null,
      nextSortOrder,
    ]
  );

  res.status(201).json(formatTaskResponse(result.rows[0]));
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
