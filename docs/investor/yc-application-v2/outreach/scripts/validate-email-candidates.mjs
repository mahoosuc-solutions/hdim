import fs from 'node:fs/promises';
import path from 'node:path';
import dns from 'node:dns/promises';

const root = path.resolve(process.cwd());
const dataDir = path.join(root, 'data');

function validFormat(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

async function mxExists(domain) {
  try {
    const records = await dns.resolveMx(domain);
    return Array.isArray(records) && records.length > 0;
  } catch {
    return false;
  }
}

async function run() {
  const src = path.join(dataDir, 'investor-firms.enriched.json');
  const firms = JSON.parse(await fs.readFile(src, 'utf8'));

  const output = [];
  for (const firm of firms) {
    const domainHasMx = await mxExists(firm.domain);
    const pool = [...(firm.discoveredEmails || []), ...(firm.candidateEmails || [])];

    const validated = pool.map((email) => ({
      email,
      formatValid: validFormat(email),
      domainHasMx,
      confidence: validFormat(email) && domainHasMx ? 'medium' : 'low'
    }));

    output.push({
      name: firm.name,
      domain: firm.domain,
      domainHasMx,
      validatedCandidates: validated
    });
  }

  await fs.writeFile(path.join(dataDir, 'investor-email-validation.json'), JSON.stringify(output, null, 2));
  console.log(`Validated candidate emails for ${output.length} firms.`);
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
