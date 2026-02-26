CREATE TABLE IF NOT EXISTS contacts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  organization TEXT NOT NULL,
  type TEXT NOT NULL CHECK(type IN ('VC', 'Angel', 'Strategic')),
  tier INTEGER CHECK(tier IN (1, 2, 3)),
  role TEXT,
  check_size TEXT,
  intro_path TEXT,
  portfolio_fit TEXT,
  outreach_angle TEXT,
  notes TEXT,
  status TEXT NOT NULL DEFAULT 'Research' CHECK(status IN (
    'Research', 'Intro Requested', 'Intro Sent', 'Meeting Scheduled',
    'First Meeting', 'Partner Meeting', 'Diligence', 'Term Sheet', 'Passed'
  )),
  last_contact_date TEXT,
  next_action TEXT,
  next_action_date TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS templates (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  category TEXT NOT NULL CHECK(category IN ('Customer', 'Investor', 'Follow-up', 'LOI', 'Partnership')),
  subject TEXT,
  body TEXT NOT NULL,
  placeholders TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS activity_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  contact_id INTEGER REFERENCES contacts(id),
  action TEXT NOT NULL CHECK(action IN ('email_sent', 'call', 'meeting', 'note', 'status_change')),
  details TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS partnerships (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  company TEXT NOT NULL,
  category TEXT NOT NULL,
  product_focus TEXT,
  customer_base TEXT,
  partnership_angle TEXT,
  status TEXT NOT NULL DEFAULT 'Research',
  priority TEXT,
  contact_name TEXT,
  contact_url TEXT,
  notes TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_contacts_status ON contacts(status);
CREATE INDEX IF NOT EXISTS idx_contacts_type ON contacts(type);
CREATE INDEX IF NOT EXISTS idx_activity_contact ON activity_log(contact_id);
CREATE INDEX IF NOT EXISTS idx_partnerships_category ON partnerships(category);
