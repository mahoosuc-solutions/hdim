const fs = require('fs');
const path = require('path');
const { getDb } = require('./index');

const DOCS_DIR = path.resolve(__dirname, '../../../docs/investor');

function parseMarkdownTable(text) {
  const lines = text.split('\n').filter(l => l.trim().startsWith('|'));
  if (lines.length < 2) return [];
  const headers = lines[0].split('|').map(h => h.trim()).filter(Boolean);
  return lines.slice(2).map(line => {
    const cells = line.split('|').map(c => c.trim()).filter(Boolean);
    const row = {};
    headers.forEach((h, i) => {
      row[h] = (cells[i] || '').replace(/\*\*/g, '').replace(/\u2b50.*?(?=\s|$)/, '').trim();
    });
    return row;
  });
}

function findPlaceholders(text) {
  const bracketed = text.match(/\[([A-Z][a-z][\w\s]*?)\]/g) || [];
  const mustache = text.match(/\{\{(\w+)\}\}/g) || [];
  const all = [
    ...mustache.map(m => m.replace(/[{}]/g, '')),
    ...bracketed.map(m => m.replace(/[\[\]]/g, ''))
  ];
  return [...new Set(all)];
}

function seedContacts(db) {
  const investorMd = fs.readFileSync(path.join(DOCS_DIR, 'investor-target-list.md'), 'utf-8');
  const angelMd = fs.readFileSync(path.join(DOCS_DIR, 'angel-outreach-list.md'), 'utf-8');

  const insert = db.prepare(`
    INSERT OR IGNORE INTO contacts (name, organization, type, tier, role, check_size, intro_path, portfolio_fit, notes)
    VALUES (@name, @organization, @type, @tier, @role, @check_size, @intro_path, @portfolio_fit, @notes)
  `);

  // Parse VC tiers from investor-target-list.md
  const sections = investorMd.split(/^## /m);
  for (const section of sections) {
    let tier = 1;
    let contactType = 'VC';
    if (section.includes('Tier 2')) tier = 2;
    if (section.includes('Tier 3')) tier = 3;
    if (section.includes('Strategic')) contactType = 'Strategic';
    if (section.includes('Healthcare Angels')) contactType = 'Angel';

    const rows = parseMarkdownTable(section);
    for (const row of rows) {
      const name = row['Partner'] || row['Contact'] || row['Name'] || '';
      const org = row['Fund'] || row['Organization'] || '';
      if (!name || name.startsWith('#') || name.startsWith('-')) continue;

      insert.run({
        name: name.replace(/^#?\d+\s*/, '').trim(),
        organization: org || '',
        type: contactType,
        tier,
        role: row['Partner'] ? 'Partner' : (row['Background'] || ''),
        check_size: row['Check Size'] || row['Check size'] || '',
        intro_path: row['Intro Path'] || row['Intro path'] || '',
        portfolio_fit: row['Portfolio Fit'] || row['Healthcare Portfolio'] || row['Focus'] || '',
        notes: row['Notes'] || ''
      });
    }
  }

  // Parse angels from angel-outreach-list.md
  const angelSections = angelMd.split(/^### \d+\.\s*/m);
  for (const sec of angelSections) {
    const nameMatch = sec.match(/^(.+?)$/m);
    if (!nameMatch) continue;
    const name = nameMatch[1].trim();
    if (!name || name.startsWith('#') || name.length < 3 || name.startsWith('Tier')) continue;

    const bgMatch = sec.match(/\*\*Background:\*\*\s*(.+)/);
    const sizeMatch = sec.match(/\*\*Check size:\*\*\s*(.+)/);
    const angleMatch = sec.match(/\*\*Angle for outreach:\*\*\s*"(.+?)"/);
    const findMatch = sec.match(/\*\*Find via:\*\*\s*(.+)/);

    let tier = null;
    if (sec.includes('Operator') || sec.includes('Healthcare')) tier = 1;
    if (sec.includes('Founder') || sec.includes('Health Tech')) tier = 2;
    if (sec.includes('Enterprise SaaS')) tier = 3;

    insert.run({
      name,
      organization: '',
      type: 'Angel',
      tier,
      role: bgMatch ? bgMatch[1] : '',
      check_size: sizeMatch ? sizeMatch[1] : '',
      intro_path: findMatch ? findMatch[1] : '',
      portfolio_fit: '',
      notes: angleMatch ? angleMatch[1] : ''
    });
  }

  const count = db.prepare('SELECT COUNT(*) as n FROM contacts').get();
  console.log('Seeded ' + count.n + ' contacts');
}

function seedTemplates(db) {
  const md = fs.readFileSync(path.join(DOCS_DIR, 'outreach-templates.md'), 'utf-8');

  const insert = db.prepare(`
    INSERT INTO templates (name, category, subject, body, placeholders)
    VALUES (@name, @category, @subject, @body, @placeholders)
  `);

  const sections = md.split(/^### /m).filter(s => s.trim());

  for (const sec of sections) {
    const titleMatch = sec.match(/^(.+?)$/m);
    if (!titleMatch) continue;
    const title = titleMatch[1].trim();
    if (title.startsWith('#')) continue;

    const codeBlockRegex = /```[\s\S]*?\n([\s\S]*?)```/g;
    const codeBlocks = [];
    let match;
    while ((match = codeBlockRegex.exec(sec)) !== null) {
      codeBlocks.push(match[1].trim());
    }
    if (codeBlocks.length === 0) continue;

    const body = codeBlocks[0];
    const subjectMatch = sec.match(/\*\*Subject:\*\*\s*(.+)/);

    let category = 'Customer';
    const lowerSec = (title + sec).toLowerCase();
    if (lowerSec.includes('investor') || lowerSec.includes('warm intro') || lowerSec.includes('forwardable')) category = 'Investor';
    if (lowerSec.includes('follow-up') || lowerSec.includes('follow up')) category = 'Follow-up';
    if (lowerSec.includes('loi') || lowerSec.includes('letter of intent')) category = 'LOI';
    if (lowerSec.includes('monthly') || lowerSec.includes('update template')) category = 'Investor';

    const placeholders = findPlaceholders(body);

    insert.run({
      name: title,
      category,
      subject: subjectMatch ? subjectMatch[1] : '',
      body,
      placeholders: JSON.stringify(placeholders)
    });
  }

  const count = db.prepare('SELECT COUNT(*) as n FROM templates').get();
  console.log('Seeded ' + count.n + ' templates');
}

function seedPartnerships(db) {
  const md = fs.readFileSync(path.join(DOCS_DIR, 'healthtech-partnership-outreach.md'), 'utf-8');

  const insert = db.prepare(`
    INSERT INTO partnerships (company, category, product_focus, customer_base, partnership_angle)
    VALUES (@company, @category, @product_focus, @customer_base, @partnership_angle)
  `);

  const catSections = md.split(/^### Category \d+:\s*/m);
  for (let sec of catSections) {
    const catMatch = sec.match(/^(.+?)$/m);
    if (!catMatch) continue;
    const category = catMatch[1].trim();
    if (category.startsWith('#') || category.length < 3) continue;

    // Truncate at next major section to avoid parsing non-table content
    const endMarker = sec.search(/^---\s*$/m);
    if (endMarker > 0) sec = sec.substring(0, endMarker);

    const rows = parseMarkdownTable(sec);
    for (const row of rows) {
      const company = row['Company'] || '';
      if (!company) continue;
      insert.run({
        company,
        category,
        product_focus: row['Product Focus'] || '',
        customer_base: row['Customer Base'] || '',
        partnership_angle: row['Partnership Angle'] || ''
      });
    }
  }

  const count = db.prepare('SELECT COUNT(*) as n FROM partnerships').get();
  console.log('Seeded ' + count.n + ' partnerships');
}

// Run seeding
const db = getDb();

// Clear for re-seed
const clearStatements = [
  'DELETE FROM activity_log',
  'DELETE FROM contacts',
  'DELETE FROM templates',
  'DELETE FROM partnerships',
  "DELETE FROM sqlite_sequence WHERE name IN ('contacts', 'templates', 'activity_log', 'partnerships')"
];
clearStatements.forEach(sql => db.prepare(sql).run());

seedContacts(db);
seedTemplates(db);
seedPartnerships(db);

db.close();
console.log('Seeding complete.');
