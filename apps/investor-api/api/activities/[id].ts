/**
 * /api/activities/:id
 *
 * GET    - Get a single activity
 * PUT    - Update an activity
 * DELETE - Delete an activity
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
  res.setHeader('Access-Control-Allow-Methods', 'GET,PUT,DELETE,OPTIONS');
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
    res.status(400).json({ message: 'Activity ID is required', code: 'VALIDATION_ERROR' });
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
        await getActivity(id, res, pool);
        break;
      case 'PUT':
        await updateActivity(id, req, res, pool);
        break;
      case 'DELETE':
        await deleteActivity(id, res, pool);
        break;
      default:
        res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    }
  } catch (error) {
    console.error('Activity endpoint error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * GET /api/activities/:id
 */
async function getActivity(id: string, res: VercelResponse, pool: Pool): Promise<void> {
  const result = await pool.query(
    `SELECT a.id, a.contact_id, a.activity_type, a.status, a.subject, a.content,
            a.activity_date, a.scheduled_time, a.response_received, a.response_content,
            a.notes, a.linkedin_message_id, a.linkedin_connection_status, a.created_by,
            a.created_at, a.updated_at, c.name as contact_name
     FROM outreach_activities a
     LEFT JOIN investor_contacts c ON c.id = a.contact_id
     WHERE a.id = $1`,
    [id]
  );

  if (result.rows.length === 0) {
    res.status(404).json({ message: 'Activity not found', code: 'NOT_FOUND' });
    return;
  }

  res.status(200).json(formatActivityResponse(result.rows[0]));
}

/**
 * PUT /api/activities/:id
 * Update an activity
 */
async function updateActivity(
  id: string,
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  // Check if activity exists
  const existing = await pool.query(
    'SELECT * FROM outreach_activities WHERE id = $1',
    [id]
  );
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Activity not found', code: 'NOT_FOUND' });
    return;
  }

  const data = await parseBody(req);
  if (!data) {
    res.status(400).json({ message: 'Invalid request body', code: 'INVALID_BODY' });
    return;
  }

  const existingActivity = existing.rows[0];

  // Build update dynamically
  const updates: string[] = [];
  const params: any[] = [];
  let paramIndex = 1;

  if (data.activityType !== undefined) {
    const validTypes = [
      'linkedin_connect',
      'linkedin_message',
      'email',
      'call',
      'meeting',
      'intro_request',
    ];
    if (!validTypes.includes(data.activityType)) {
      res.status(400).json({
        message: `Invalid activityType. Must be one of: ${validTypes.join(', ')}`,
        code: 'VALIDATION_ERROR',
      });
      return;
    }
    updates.push(`activity_type = $${paramIndex++}`);
    params.push(data.activityType);
  }

  if (data.status !== undefined) {
    const validStatuses = ['pending', 'sent', 'responded', 'completed', 'no_response'];
    if (!validStatuses.includes(data.status)) {
      res.status(400).json({
        message: `Invalid status. Must be one of: ${validStatuses.join(', ')}`,
        code: 'VALIDATION_ERROR',
      });
      return;
    }
    updates.push(`status = $${paramIndex++}`);
    params.push(data.status);

    // Set responseReceived when status is 'responded'
    if (data.status === 'responded' && !existingActivity.response_received) {
      updates.push(`response_received = $${paramIndex++}`);
      params.push(new Date());
    }
  }

  if (data.subject !== undefined) {
    updates.push(`subject = $${paramIndex++}`);
    params.push(data.subject);
  }
  if (data.content !== undefined) {
    updates.push(`content = $${paramIndex++}`);
    params.push(data.content);
  }
  if (data.activityDate !== undefined) {
    updates.push(`activity_date = $${paramIndex++}`);
    params.push(new Date(data.activityDate));
  }
  if (data.scheduledTime !== undefined) {
    updates.push(`scheduled_time = $${paramIndex++}`);
    params.push(data.scheduledTime ? new Date(data.scheduledTime) : null);
  }
  if (data.responseContent !== undefined) {
    updates.push(`response_content = $${paramIndex++}`);
    params.push(data.responseContent);
  }
  if (data.notes !== undefined) {
    updates.push(`notes = $${paramIndex++}`);
    params.push(data.notes);
  }

  if (updates.length === 0) {
    // Get contact name for response
    const contactResult = await pool.query(
      'SELECT name FROM investor_contacts WHERE id = $1',
      [existingActivity.contact_id]
    );
    res.status(200).json({
      ...formatActivityResponse(existingActivity),
      contactName: contactResult.rows[0]?.name,
    });
    return;
  }

  // Add updated_at
  updates.push(`updated_at = $${paramIndex++}`);
  params.push(new Date());

  // Add id as last param
  params.push(id);

  const result = await pool.query(
    `UPDATE outreach_activities SET ${updates.join(', ')} WHERE id = $${paramIndex}
     RETURNING id, contact_id, activity_type, status, subject, content,
               activity_date, scheduled_time, response_received, response_content,
               notes, linkedin_message_id, linkedin_connection_status, created_by,
               created_at, updated_at`,
    params
  );

  // Get contact name
  const contactResult = await pool.query(
    'SELECT name FROM investor_contacts WHERE id = $1',
    [result.rows[0].contact_id]
  );

  res.status(200).json({
    ...formatActivityResponse(result.rows[0]),
    contactName: contactResult.rows[0]?.name,
  });
}

/**
 * DELETE /api/activities/:id
 */
async function deleteActivity(id: string, res: VercelResponse, pool: Pool): Promise<void> {
  const existing = await pool.query('SELECT id FROM outreach_activities WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Activity not found', code: 'NOT_FOUND' });
    return;
  }

  await pool.query('DELETE FROM outreach_activities WHERE id = $1', [id]);
  res.status(204).end();
}

/**
 * Format activity for API response (snake_case → camelCase)
 */
function formatActivityResponse(activity: any): any {
  return {
    id: activity.id,
    contactId: activity.contact_id,
    contactName: activity.contact_name || undefined,
    activityType: activity.activity_type,
    status: activity.status,
    subject: activity.subject,
    content: activity.content,
    activityDate: activity.activity_date?.toISOString() || null,
    scheduledTime: activity.scheduled_time?.toISOString() || null,
    responseReceived: activity.response_received?.toISOString() || null,
    responseContent: activity.response_content,
    notes: activity.notes,
    createdAt: activity.created_at?.toISOString() || new Date().toISOString(),
    updatedAt: activity.updated_at?.toISOString() || new Date().toISOString(),
  };
}
