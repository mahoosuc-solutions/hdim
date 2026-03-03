import fs from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(process.cwd());
const dataDir = path.join(root, 'data');
const draftsDir = path.join(root, 'drafts');

const senderName = 'Aaron';
const senderEmail = 'aaron@mahoosuc.solutions';

function chooseRecipient(validationRow) {
  const medium = validationRow.validatedCandidates.filter((c) => c.confidence === 'medium');
  if (medium.length > 0) return medium[0].email;
  return '';
}

function subjectFor(firm) {
  return `${firm.name} x HDIM - AI-native healthcare quality infrastructure`;
}

function bodyFor(firm) {
  return `Hi ${firm.name} team,\n\nI lead HDIM (HealthData-in-Motion), where we built a production-grade healthcare quality platform with an AI-native engineering approach.\n\nWhy this may fit your thesis:\n- Real-time quality and care-gap infrastructure (vs batch legacy workflows)\n- FHIR-native, event-driven architecture for payer/provider integration\n- Mid-market GTM wedge with strong unit economics focus\n\nWe recently completed a deep architecture + release evidence package and would value a short conversation to share traction, product direction, and fundraising plans.\n\nIf helpful, I can send a concise 1-page summary and a 10-minute product walkthrough link first.\n\nBest,\n${senderName}\n${senderEmail}\nhttps://healthdatainmotion.com\n`;
}

function sanitize(name) {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
}

async function run() {
  const firms = JSON.parse(await fs.readFile(path.join(dataDir, 'investor-firms.enriched.json'), 'utf8'));
  const validation = JSON.parse(await fs.readFile(path.join(dataDir, 'investor-email-validation.json'), 'utf8'));

  await fs.mkdir(draftsDir, { recursive: true });

  const bundle = [];
  for (const firm of firms) {
    const v = validation.find((x) => x.name === firm.name);
    const toEmail = v ? chooseRecipient(v) : '';
    const draft = {
      firm: firm.name,
      toEmail,
      subject: subjectFor(firm),
      body: bodyFor(firm),
      notes: {
        tier: firm.tier,
        targetType: firm.targetType,
        thesisFit: firm.thesisFit,
        contactUrls: firm.contactUrls || []
      },
      reviewStatus: 'draft'
    };
    bundle.push(draft);

    const md = [
      `# Draft - ${firm.name}`,
      '',
      `- To: ${toEmail || '[set recipient after review]'}`,
      `- Subject: ${draft.subject}`,
      `- Tier: ${firm.tier}`,
      `- Target type: ${firm.targetType}`,
      '',
      '## Email',
      '',
      '```text',
      draft.body.trimEnd(),
      '```',
      '',
      '## Contact URLs',
      ...(draft.notes.contactUrls.length ? draft.notes.contactUrls.map((u) => `- ${u}`) : ['- [none discovered]'])
    ].join('\n');

    await fs.writeFile(path.join(draftsDir, `${sanitize(firm.name)}.md`), md + '\n');
  }

  await fs.writeFile(path.join(draftsDir, 'investor-drafts.json'), JSON.stringify(bundle, null, 2));
  console.log(`Generated ${bundle.length} drafts.`);
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
