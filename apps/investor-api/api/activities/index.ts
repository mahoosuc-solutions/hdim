/**
 * /api/activities
 *
 * GET  - List activities (filter by contactId)
 * POST - Log a new activity
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

  let userId: string;
  try {
    const payload = jwt.verify(parts[1], JWT_SECRET) as JwtPayload;
    if (payload.type !== 'access') {
      throw new Error('Invalid token type');
    }
    userId = payload.userId;
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
        await listActivities(req, res, pool);
        break;
      case 'POST':
        await createActivity(req, res, pool, userId);
        break;
      default:
        res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    }
  } catch (error) {
    console.error('Activities endpoint error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * GET /api/activities
 * Query params: contactId, activityType, status, limit
 */
async function listActivities(
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  const contactId = req.query.contactId as string | undefined;
  const activityType = req.query.activityType as string | undefined;
  const status = req.query.status as string | undefined;
  const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : 50;

  // Build WHERE clause dynamically
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  let paramIndex = 1;

  if (contactId) {
    conditions.push(`a.contact_id = $${paramIndex++}`);
    params.push(contactId);
  }
  if (activityType) {
    conditions.push(`a.activity_type = $${paramIndex++}`);
    params.push(activityType);
  }
  if (status) {
    conditions.push(`a.status = $${paramIndex++}`);
    params.push(status);
  }

  const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

  const result = await pool.query(
    `SELECT a.id, a.contact_id, a.activity_type, a.status, a.subject, a.content,
            a.activity_date, a.scheduled_time, a.response_received, a.response_content,
            a.notes, a.linkedin_message_id, a.linkedin_connection_status, a.created_by,
            a.created_at, a.updated_at, c.name as contact_name
     FROM outreach_activities a
     LEFT JOIN investor_contacts c ON c.id = a.contact_id
     ${whereClause}
     ORDER BY a.activity_date DESC
     LIMIT $${paramIndex}`,
    [...params, limit]
  );

  const activities = result.rows.map(formatActivityResponse);
  res.status(200).json(activities);
}

/**
 * POST /api/activities
 * Log a new outreach activity
 */
async function createActivity(
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool,
  userId: string
): Promise<void> {
  const data = await parseBody(req);
  if (!data) {
    res.status(400).json({ message: 'Invalid request body', code: 'INVALID_BODY' });
    return;
  }

  // Validate required fields
  if (!data.contactId || !data.activityType || !data.activityDate) {
    res.status(400).json({
      message: 'contactId, activityType, and activityDate are required',
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  // Validate activity type
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

  // Check if contact exists
  const contactResult = await pool.query(
    'SELECT id, name FROM investor_contacts WHERE id = $1',
    [data.contactId]
  );
  if (contactResult.rows.length === 0) {
    res.status(404).json({ message: 'Contact not found', code: 'CONTACT_NOT_FOUND' });
    return;
  }
  const contactName = contactResult.rows[0].name;

  // Create activity
  const result = await pool.query(
    `INSERT INTO outreach_activities (id, contact_id, activity_type, subject, content, activity_date, scheduled_time, notes, created_by, created_at, updated_at)
     VALUES (gen_random_uuid(), $1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())
     RETURNING id, contact_id, activity_type, status, subject, content,
               activity_date, scheduled_time, response_received, response_content,
               notes, linkedin_message_id, linkedin_connection_status, created_by,
               created_at, updated_at`,
    [
      data.contactId,
      data.activityType,
      data.subject || null,
      data.content || null,
      new Date(data.activityDate),
      data.scheduledTime ? new Date(data.scheduledTime) : null,
      data.notes || null,
      userId,
    ]
  );

  // Update contact's lastContacted
  await pool.query(
    'UPDATE investor_contacts SET last_contacted = $1, updated_at = $2 WHERE id = $3',
    [new Date(), new Date(), data.contactId]
  );

  const activity = result.rows[0];
  res.status(201).json({
    ...formatActivityResponse(activity),
    contactName,
  });
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
