const express = require('express');
const path = require('path');
const { getDb } = require('./db');

const app = express();
const PORT = process.env.PORT || 4720;

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

const STATUSES = [
  'Research', 'Intro Requested', 'Intro Sent', 'Meeting Scheduled',
  'First Meeting', 'Partner Meeting', 'Diligence', 'Term Sheet', 'Passed'
];

function db() { return getDb(); }

// --- Dashboard ---
app.get('/', (req, res) => {
  const d = db();

  const total = d.prepare('SELECT COUNT(*) as n FROM contacts').get().n;
  const active = d.prepare("SELECT COUNT(*) as n FROM contacts WHERE status NOT IN ('Research', 'Passed')").get().n;
  const meetings = d.prepare("SELECT COUNT(*) as n FROM contacts WHERE status IN ('Meeting Scheduled', 'First Meeting', 'Partner Meeting', 'Diligence', 'Term Sheet')").get().n;
  const partnershipCount = d.prepare('SELECT COUNT(*) as n FROM partnerships').get().n;

  const funnel = {};
  for (const s of STATUSES) {
    funnel[s] = d.prepare('SELECT COUNT(*) as n FROM contacts WHERE status = ?').get(s).n;
  }

  const byType = {};
  d.prepare('SELECT type, COUNT(*) as n FROM contacts GROUP BY type').all().forEach(r => { byType[r.type] = r.n; });

  const recentActivity = d.prepare(`
    SELECT a.*, c.name as contact_name
    FROM activity_log a LEFT JOIN contacts c ON a.contact_id = c.id
    ORDER BY a.created_at DESC LIMIT 10
  `).all();

  const needsFollowUp = d.prepare(`
    SELECT * FROM contacts
    WHERE next_action_date IS NOT NULL AND next_action_date <= date('now')
    ORDER BY next_action_date ASC LIMIT 10
  `).all();

  d.close();
  res.render('dashboard', {
    title: 'Dashboard',
    active: 'dashboard',
    stats: { total, active, meetings, partnerships: partnershipCount },
    funnel, byType, recentActivity, needsFollowUp
  });
});

// --- Contacts ---
app.get('/contacts', (req, res) => {
  const d = db();
  const contacts = d.prepare('SELECT * FROM contacts ORDER BY tier ASC, type ASC, name ASC').all();
  d.close();
  res.render('contacts', { title: 'Contacts', active: 'contacts', contacts, statuses: STATUSES });
});

app.get('/contacts/:id', (req, res) => {
  const d = db();
  const contact = d.prepare('SELECT * FROM contacts WHERE id = ?').get(req.params.id);
  if (!contact) { d.close(); return res.status(404).send('Not found'); }
  const activities = d.prepare('SELECT * FROM activity_log WHERE contact_id = ? ORDER BY created_at DESC').all(req.params.id);
  d.close();
  res.render('contact-detail', { title: contact.name, active: 'contacts', contact, activities, statuses: STATUSES });
});

app.post('/contacts/:id/update', (req, res) => {
  const d = db();
  const old = d.prepare('SELECT status FROM contacts WHERE id = ?').get(req.params.id);
  d.prepare(`
    UPDATE contacts SET status = ?, next_action = ?, next_action_date = ?, last_contact_date = date('now'), updated_at = datetime('now')
    WHERE id = ?
  `).run(req.body.status, req.body.next_action || null, req.body.next_action_date || null, req.params.id);

  if (old && old.status !== req.body.status) {
    d.prepare('INSERT INTO activity_log (contact_id, action, details) VALUES (?, ?, ?)').run(
      req.params.id, 'status_change', old.status + ' -> ' + req.body.status
    );
  }
  d.close();
  res.redirect('/contacts/' + req.params.id);
});

app.post('/contacts/:id/activity', (req, res) => {
  const d = db();
  d.prepare('INSERT INTO activity_log (contact_id, action, details) VALUES (?, ?, ?)').run(
    req.params.id, req.body.action, req.body.details || null
  );
  d.prepare("UPDATE contacts SET last_contact_date = date('now'), updated_at = datetime('now') WHERE id = ?").run(req.params.id);
  d.close();
  res.redirect('/contacts/' + req.params.id);
});

// --- Compose ---
app.get('/compose', (req, res) => {
  const d = db();
  const contacts = d.prepare('SELECT * FROM contacts ORDER BY name').all();
  const templates = d.prepare('SELECT * FROM templates ORDER BY category, name').all();
  let selectedContact = null;
  if (req.query.contact_id) {
    selectedContact = d.prepare('SELECT * FROM contacts WHERE id = ?').get(req.query.contact_id);
  }
  d.close();
  res.render('compose', { title: 'Compose', active: 'compose', contacts, templates, selectedContact });
});

app.post('/compose/log', (req, res) => {
  const d = db();
  if (req.body.contact_id) {
    d.prepare('INSERT INTO activity_log (contact_id, action, details) VALUES (?, ?, ?)').run(
      req.body.contact_id, 'email_sent', req.body.details || 'Email composed and sent'
    );
    d.prepare("UPDATE contacts SET last_contact_date = date('now'), updated_at = datetime('now') WHERE id = ?").run(req.body.contact_id);
  }
  d.close();
  res.redirect('/compose?contact_id=' + (req.body.contact_id || ''));
});

// --- Templates ---
app.get('/templates', (req, res) => {
  const d = db();
  const templates = d.prepare('SELECT * FROM templates ORDER BY category, name').all();
  d.close();
  res.render('templates', { title: 'Templates', active: 'templates', templates });
});

// --- Partnerships ---
app.get('/partnerships', (req, res) => {
  const d = db();
  const partnerships = d.prepare('SELECT * FROM partnerships ORDER BY category, company').all();
  d.close();
  res.render('partnerships', { title: 'Partnerships', active: 'partnerships', partnerships });
});

app.post('/partnerships/:id/status', (req, res) => {
  const d = db();
  d.prepare("UPDATE partnerships SET status = ?, updated_at = datetime('now') WHERE id = ?").run(req.body.status, req.params.id);
  d.close();
  res.redirect('/partnerships');
});

// --- Start ---
app.listen(PORT, () => {
  console.log('Investor Dashboard running at http://localhost:' + PORT);
});
