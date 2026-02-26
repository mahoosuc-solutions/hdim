const BASE = '/api';

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(`GET ${path}: ${res.status}`);
  return res.json() as Promise<T>;
}

async function post<T>(path: string, body: Record<string, unknown>): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST ${path}: ${res.status}`);
  return res.json() as Promise<T>;
}

export interface Contact {
  id: number;
  name: string;
  organization: string;
  type: 'VC' | 'Angel' | 'Strategic';
  tier: number | null;
  role: string;
  check_size: string;
  intro_path: string;
  portfolio_fit: string;
  outreach_angle: string;
  notes: string;
  status: string;
  last_contact_date: string | null;
  next_action: string | null;
  next_action_date: string | null;
  created_at: string;
  updated_at: string;
}

export interface Template {
  id: number;
  name: string;
  category: string;
  subject: string;
  body: string;
  placeholders: string;
  created_at: string;
}

export interface Activity {
  id: number;
  contact_id: number;
  action: string;
  details: string | null;
  created_at: string;
  contact_name?: string;
}

export interface Partnership {
  id: number;
  company: string;
  category: string;
  product_focus: string;
  customer_base: string;
  partnership_angle: string;
  status: string;
  created_at: string;
  updated_at: string;
}

export interface DashboardData {
  stats: { total: number; active: number; meetings: number; partnerships: number };
  funnel: Record<string, number>;
  byType: Record<string, number>;
  recentActivity: Activity[];
  needsFollowUp: Contact[];
  statuses: string[];
}

export const api = {
  dashboard: () => get<DashboardData>('/dashboard'),
  contacts: () => get<{ contacts: Contact[]; statuses: string[] }>('/contacts'),
  contact: (id: number) => get<{ contact: Contact; activities: Activity[]; statuses: string[] }>(`/contacts/${id}`),
  updateContact: (id: number, data: Record<string, unknown>) => post<{ ok: boolean }>(`/contacts/${id}/update`, data),
  logActivity: (id: number, data: { action: string; details: string }) => post<{ ok: boolean }>(`/contacts/${id}/activity`, data),
  templates: () => get<{ templates: Template[] }>('/templates'),
  partnerships: () => get<{ partnerships: Partnership[] }>('/partnerships'),
  updatePartnership: (id: number, status: string) => post<{ ok: boolean }>(`/partnerships/${id}/status`, { status }),
  logCompose: (contactId: number, details: string) => post<{ ok: boolean }>('/compose/log', { contact_id: contactId, details }),
};
