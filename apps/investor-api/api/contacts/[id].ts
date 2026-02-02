/**
 * /api/contacts/:id
 *
 * GET    - Get a single contact with activities
 * PUT    - Update a contact
 * PATCH  - Update contact status
 * DELETE - Delete a contact
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
    res.status(400).json({ message: 'Contact ID is required', code: 'VALIDATION_ERROR' });
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
        await getContact(id, res, pool);
        break;
      case 'PUT':
        await updateContact(id, req, res, pool);
        break;
      case 'PATCH':
        await patchContactStatus(id, req, res, pool);
        break;
      case 'DELETE':
        await deleteContact(id, res, pool);
        break;
      default:
        res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    }
  } catch (error) {
    console.error('Contact endpoint error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * GET /api/contacts/:id
 * Returns contact with recent activities
 */
async function getContact(id: string, res: VercelResponse, pool: Pool): Promise<void> {
  const contactResult = await pool.query(
    `SELECT id, name, title, organization, email, phone, linkedin_url, linkedin_profile_id,
            category, status, tier, investment_thesis, notes, last_contacted, next_follow_up,
            created_at, updated_at
     FROM investor_contacts WHERE id = $1`,
    [id]
  );

  if (contactResult.rows.length === 0) {
    res.status(404).json({ message: 'Contact not found', code: 'NOT_FOUND' });
    return;
  }

  // Get recent activities
  const activitiesResult = await pool.query(
    `SELECT id, activity_type, status, subject, activity_date, notes
     FROM outreach_activities
     WHERE contact_id = $1
     ORDER BY activity_date DESC
     LIMIT 10`,
    [id]
  );

  const contact = contactResult.rows[0];
  const response = {
    ...formatContactResponse(contact),
    activities: activitiesResult.rows.map((a) => ({
      id: a.id,
      activityType: a.activity_type,
      status: a.status,
      subject: a.subject,
      activityDate: a.activity_date?.toISOString() || null,
      notes: a.notes,
    })),
  };

  res.status(200).json(response);
}

/**
 * PUT /api/contacts/:id
 * Full update of a contact
 */
async function updateContact(
  id: string,
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  // Check if contact exists
  const existing = await pool.query('SELECT * FROM investor_contacts WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Contact not found', code: 'NOT_FOUND' });
    return;
  }

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

  // Build update dynamically
  const updates: string[] = [];
  const params: any[] = [];
  let paramIndex = 1;

  if (data.name !== undefined) {
    updates.push(`name = $${paramIndex++}`);
    params.push(data.name);
  }
  if (data.title !== undefined) {
    updates.push(`title = $${paramIndex++}`);
    params.push(data.title);
  }
  if (data.organization !== undefined) {
    updates.push(`organization = $${paramIndex++}`);
    params.push(data.organization);
  }
  if (data.email !== undefined) {
    updates.push(`email = $${paramIndex++}`);
    params.push(data.email);
  }
  if (data.phone !== undefined) {
    updates.push(`phone = $${paramIndex++}`);
    params.push(data.phone);
  }
  if (data.linkedInUrl !== undefined) {
    updates.push(`linkedin_url = $${paramIndex++}`);
    params.push(data.linkedInUrl);
  }
  if (data.category !== undefined) {
    updates.push(`category = $${paramIndex++}`);
    params.push(data.category);
  }
  if (data.status !== undefined) {
    updates.push(`status = $${paramIndex++}`);
    params.push(data.status);
    // Update lastContacted when status changes to contacted or beyond
    if (data.status !== 'identified') {
      updates.push(`last_contacted = $${paramIndex++}`);
      params.push(new Date());
    }
  }
  if (data.tier !== undefined) {
    updates.push(`tier = $${paramIndex++}`);
    params.push(data.tier);
  }
  if (data.notes !== undefined) {
    updates.push(`notes = $${paramIndex++}`);
    params.push(data.notes);
  }
  if (data.nextFollowUp !== undefined) {
    updates.push(`next_follow_up = $${paramIndex++}`);
    params.push(data.nextFollowUp ? new Date(data.nextFollowUp) : null);
  }

  if (updates.length === 0) {
    res.status(200).json(formatContactResponse(existing.rows[0]));
    return;
  }

  // Add updated_at
  updates.push(`updated_at = $${paramIndex++}`);
  params.push(new Date());

  // Add id as last param
  params.push(id);

  const result = await pool.query(
    `UPDATE investor_contacts SET ${updates.join(', ')} WHERE id = $${paramIndex}
     RETURNING id, name, title, organization, email, phone, linkedin_url, linkedin_profile_id,
               category, status, tier, investment_thesis, notes, last_contacted, next_follow_up,
               created_at, updated_at`,
    params
  );

  res.status(200).json(formatContactResponse(result.rows[0]));
}

/**
 * PATCH /api/contacts/:id
 * Update only the status field
 */
async function patchContactStatus(
  id: string,
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  // Check if contact exists
  const existing = await pool.query('SELECT * FROM investor_contacts WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Contact not found', code: 'NOT_FOUND' });
    return;
  }

  // Try query param first, then body
  let status = req.query.status as string | undefined;

  if (!status) {
    const chunks: Buffer[] = [];
    for await (const chunk of req) {
      chunks.push(Buffer.from(chunk));
    }
    const rawBody = Buffer.concat(chunks).toString('utf-8');
    if (rawBody) {
      try {
        const data = JSON.parse(rawBody);
        status = data.status;
      } catch {
        // ignore parse errors
      }
    }
  }

  if (!status) {
    res.status(400).json({ message: 'Status is required', code: 'VALIDATION_ERROR' });
    return;
  }

  const validStatuses = [
    'identified',
    'contacted',
    'engaged',
    'meeting_scheduled',
    'follow_up',
    'declined',
    'invested',
  ];
  if (!validStatuses.includes(status)) {
    res.status(400).json({
      message: `Invalid status. Must be one of: ${validStatuses.join(', ')}`,
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  // Update lastContacted when status changes
  const lastContacted = status !== 'identified' ? new Date() : existing.rows[0].last_contacted;

  const result = await pool.query(
    `UPDATE investor_contacts SET status = $1, last_contacted = $2, updated_at = $3 WHERE id = $4
     RETURNING id, name, title, organization, email, phone, linkedin_url, linkedin_profile_id,
               category, status, tier, investment_thesis, notes, last_contacted, next_follow_up,
               created_at, updated_at`,
    [status, lastContacted, new Date(), id]
  );

  res.status(200).json(formatContactResponse(result.rows[0]));
}

/**
 * DELETE /api/contacts/:id
 * Also deletes all associated activities (cascade)
 */
async function deleteContact(id: string, res: VercelResponse, pool: Pool): Promise<void> {
  const existing = await pool.query('SELECT id FROM investor_contacts WHERE id = $1', [id]);
  if (existing.rows.length === 0) {
    res.status(404).json({ message: 'Contact not found', code: 'NOT_FOUND' });
    return;
  }

  // Delete activities first (if no cascade)
  await pool.query('DELETE FROM outreach_activities WHERE contact_id = $1', [id]);
  await pool.query('DELETE FROM investor_contacts WHERE id = $1', [id]);

  res.status(204).end();
}

/**
 * Format contact for API response (snake_case → camelCase)
 */
function formatContactResponse(contact: any): any {
  return {
    id: contact.id,
    name: contact.name,
    title: contact.title,
    organization: contact.organization,
    email: contact.email,
    phone: contact.phone,
    linkedInUrl: contact.linkedin_url,
    category: contact.category,
    status: contact.status,
    tier: contact.tier,
    investmentThesis: contact.investment_thesis,
    notes: contact.notes,
    lastContacted: contact.last_contacted?.toISOString() || null,
    nextFollowUp: contact.next_follow_up?.toISOString() || null,
    createdAt: contact.created_at?.toISOString() || new Date().toISOString(),
    updatedAt: contact.updated_at?.toISOString() || new Date().toISOString(),
  };
}
