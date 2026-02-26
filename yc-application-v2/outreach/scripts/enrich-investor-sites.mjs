import fs from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(process.cwd());
const dataDir = path.join(root, 'data');

function uniq(arr) {
  return [...new Set(arr.filter(Boolean))];
}

function pickLinks(html, domain) {
  const links = [];
  const regex = /href=["']([^"']+)["']/gi;
  let match;
  while ((match = regex.exec(html)) !== null) {
    const href = match[1].trim();
    if (!href || href.startsWith('#') || href.startsWith('javascript:')) continue;
    let absolute = href;
    if (href.startsWith('/')) absolute = `https://${domain}${href}`;
    if (!absolute.startsWith('http')) continue;
    if (!absolute.includes(domain)) continue;
    if (/\.(css|js|png|jpg|jpeg|gif|svg|webp|woff2?|ttf|ico)(\?|$)/i.test(absolute)) continue;
    links.push(absolute);
  }

  const priority = ['contact', 'team', 'people', 'partners', 'portfolio', 'about', 'invest', 'apply'];
  const prioritized = links
    .filter((l) => priority.some((p) => l.toLowerCase().includes(p)))
    .map((l) => ({
      link: l.split('#')[0],
      score: priority.findIndex((p) => l.toLowerCase().includes(p))
    }))
    .map((x) => ({ ...x, score: x.score === -1 ? 99 : x.score }))
    .sort((a, b) => a.score - b.score)
    .map((x) => x.link);

  return uniq(prioritized);
}

function mailtoEmails(html) {
  const hits = [];
  const regex = /mailto:([^"'\s>]+)/gi;
  let match;
  while ((match = regex.exec(html)) !== null) {
    hits.push(match[1].split('?')[0].trim().toLowerCase());
  }
  return uniq(hits);
}

async function fetchText(url) {
  const res = await fetch(url, { redirect: 'follow' });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  return await res.text();
}

function candidateEmails(domain) {
  const locals = ['investments', 'investorrelations', 'partners', 'partnerships', 'contact', 'hello', 'info'];
  return locals.map((l) => `${l}@${domain}`);
}

async function run() {
  const src = path.join(dataDir, 'investor-firms.json');
  const firms = JSON.parse(await fs.readFile(src, 'utf8'));

  const enriched = [];
  for (const firm of firms) {
    const seeded = [firm.contactPage || '', firm.teamPage || ''].filter(Boolean);
    const item = {
      ...firm,
      contactUrls: seeded,
      discoveredEmails: [],
      candidateEmails: candidateEmails(firm.domain),
      enrichmentStatus: 'ok'
    };
    try {
      const html = await fetchText(firm.website);
      item.contactUrls = uniq([...seeded, ...pickLinks(html, firm.domain)]).slice(0, 8);
      item.discoveredEmails = mailtoEmails(html);
    } catch (err) {
      item.enrichmentStatus = `error: ${err.message}`;
    }
    enriched.push(item);
  }

  await fs.writeFile(path.join(dataDir, 'investor-firms.enriched.json'), JSON.stringify(enriched, null, 2));

  const csvLines = ['name,domain,website,enrichmentStatus,contactUrl1,contactUrl2,contactUrl3'];
  for (const e of enriched) {
    const row = [
      e.name,
      e.domain,
      e.website,
      e.enrichmentStatus,
      e.contactUrls[0] || '',
      e.contactUrls[1] || '',
      e.contactUrls[2] || ''
    ].map((v) => `"${String(v).replaceAll('"', '""')}"`);
    csvLines.push(row.join(','));
  }
  await fs.writeFile(path.join(dataDir, 'investor-firms.enriched.csv'), csvLines.join('\n') + '\n');

  console.log(`Enriched ${enriched.length} firms.`);
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
