import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import { verifyEvidenceToken } from '@/lib/server/evidenceGate';
import { getPacketAssets } from '@/lib/server/evidenceAssets';
import EvidenceAccessTelemetry from '@/components/resources/EvidenceAccessTelemetry';

const packetResources: Record<string, Array<{ label: string; href: string; notes: string }>> = {
  security: [
    { label: 'Executive & Compliance Hub', href: '/resources/executive', notes: 'Control mapping and governance narrative.' },
    { label: 'Technical Evaluation Hub', href: '/resources/technical', notes: 'Architecture and runtime control details.' },
  ],
  reliability: [
    { label: 'Release Evidence Hub', href: '/resources/build-evidence', notes: 'Go/No-Go evidence index and release proofs.' },
    { label: 'FHIR Validation Evidence', href: '/resources/fhir-evidence', notes: 'Clinical correctness and validation method.' },
  ],
  procurement: [
    { label: 'Procurement Buyer Page', href: '/resources/procurement', notes: 'Commercial and implementation diligence path.' },
    { label: 'Executive Summary', href: '/resources/executive-summary', notes: 'Program-level outcomes and assumptions.' },
  ],
};

export default async function EvidenceAccessPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const params = await searchParams;
  const token = params.token;
  const secret = process.env.EVIDENCE_ROOM_TOKEN_SECRET;

  if (!token || !secret) {
    return (
      <section className={styles.section}>
        <h1 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>Evidence access unavailable</h1>
        <p className={styles.sectionLead}>
          This access link is missing required credentials. Request a new evidence room link.
        </p>
        <Link className={styles.btnPrimary} href="/resources/evidence-room">Back to Evidence Room</Link>
      </section>
    );
  }

  const verified = verifyEvidenceToken(token, secret);
  if (!verified.valid || !verified.payload) {
    return (
      <section className={styles.section}>
        <h1 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>Evidence access denied</h1>
        <p className={styles.sectionLead}>Token validation failed: {verified.reason ?? 'Invalid or expired token.'}</p>
        <Link className={styles.btnPrimary} href="/resources/evidence-room">Request new access</Link>
      </section>
    );
  }

  const packetKey = verified.payload.packet in packetResources ? verified.payload.packet : 'security';
  const resources = packetResources[packetKey] ?? packetResources.security;
  const privateAssets = getPacketAssets(packetKey);

  return (
    <>
      <EvidenceAccessTelemetry packet={packetKey} role={verified.payload.role} />
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>EVIDENCE ROOM ACCESS GRANTED</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Approved packet: {verified.payload.packet}
          </h1>
          <p className={styles.heroBody}>
            Request ID: {verified.payload.requestId} | Organization: {verified.payload.organization} | Role: {verified.payload.role}
          </p>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Available evidence resources
        </h2>
        <div className={styles.cardGrid}>
          {resources.map((resource) => (
            <article className={styles.card} key={resource.href}>
              <h3 className={styles.cardTitle}>{resource.label}</h3>
              <p className={styles.cardBody}>{resource.notes}</p>
              <Link className={styles.btnPrimary} href={resource.href}>Open resource</Link>
            </article>
          ))}
        </div>
        {privateAssets.length > 0 ? (
          <>
            <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)', marginTop: '1.5rem' }}>
              Private artifact downloads
            </h2>
            <div className={styles.cardGrid}>
              {privateAssets.map((asset) => (
                <article className={styles.card} key={asset.key}>
                  <h3 className={styles.cardTitle}>{asset.label}</h3>
                  <p className={styles.cardBody}>{asset.notes}</p>
                  <a
                    className={styles.btnPrimary}
                    href={`/api/evidence-download?token=${encodeURIComponent(token)}&asset=${encodeURIComponent(asset.key)}`}
                  >
                    Download artifact
                  </a>
                </article>
              ))}
            </div>
          </>
        ) : null}
        <div className={styles.notice}>
          Artifact downloads are token-validated and limited to your approved packet scope.
        </div>
      </section>
    </>
  );
}
