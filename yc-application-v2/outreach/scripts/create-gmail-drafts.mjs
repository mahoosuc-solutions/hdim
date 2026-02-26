import fs from 'node:fs/promises';
import path from 'node:path';

const token = process.env.GMAIL_ACCESS_TOKEN;
if (!token) {
  console.error('Missing GMAIL_ACCESS_TOKEN');
  process.exit(1);
}

const root = path.resolve(process.cwd());
const draftsFile = path.join(root, 'drafts', 'investor-drafts.json');

function toBase64Url(input) {
  return Buffer.from(input, 'utf8').toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function buildMime(d) {
  return [
    `To: ${d.toEmail}`,
    `Subject: ${d.subject}`,
    'Content-Type: text/plain; charset="UTF-8"',
    '',
    d.body
  ].join('\n');
}

async function createDraft(raw) {
  const res = await fetch('https://gmail.googleapis.com/gmail/v1/users/me/drafts', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ message: { raw } })
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Gmail draft create failed (${res.status}): ${text}`);
  }
  return await res.json();
}

async function run() {
  const drafts = JSON.parse(await fs.readFile(draftsFile, 'utf8'));
  let count = 0;

  for (const d of drafts) {
    if (!d.toEmail || d.reviewStatus !== 'approved') continue;
    const mime = buildMime(d);
    const raw = toBase64Url(mime);
    await createDraft(raw);
    count += 1;
  }

  console.log(`Created ${count} Gmail drafts (approved only).`);
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
