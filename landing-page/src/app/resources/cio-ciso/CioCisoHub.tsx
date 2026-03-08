import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';
import { getEvidenceArtifact } from '@/lib/resources/evidenceManifest';

export default function CioCisoHub() {
  const executiveHub = getEvidenceArtifact('executiveHub');
  const releaseEvidence = getEvidenceArtifact('releaseEvidence');
  const technicalHub = getEvidenceArtifact('technicalHub');
  const architectureEvolution = getEvidenceArtifact('architectureEvolution');
  const aiMetrics = getEvidenceArtifact('aiMetrics');
  const performance = getEvidenceArtifact('performance');

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>CIO / CISO BUYER PAGE</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            What technical and security leaders need to validate before approval.
          </h1>
          <p className={styles.heroBody}>
            This page gives CIO and CISO stakeholders a direct diligence path from architecture and controls to release governance evidence.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/evidence-room">Request deep evidence</Link>
            <Link className={styles.btnGhost} href="/resources/trust-center">Back to trust center</Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          CIO/CISO diligence checklist
        </h2>
        <div className={styles.cardGrid}>
          <ClaimProofCard
            claim="Security controls are operational, not aspirational"
            outcome="Control ownership, cadence, and exceptions are auditable and tied to release readiness."
            proofItems={[
              executiveHub,
              releaseEvidence,
            ]}
          />
          <ClaimProofCard
            claim="Architecture supports safe scale"
            outcome="Event contracts, tenant isolation, and runtime policies are designed for regulated workloads."
            proofItems={[
              technicalHub,
              architectureEvolution,
            ]}
          />
          <ClaimProofCard
            claim="Operational risk is measurable"
            outcome="Leadership can evaluate go/no-go status based on current risk posture and not anecdotal confidence."
            proofItems={[
              aiMetrics,
              performance,
            ]}
          />
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Security diligence pack
        </h2>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Architecture deep-dive</h3>
            <p className={styles.cardBody}>
              Data flow boundaries, tenant isolation model, gateway controls, and deployment topology.
            </p>
            <Link className={styles.btnPrimary} href="/resources/technical">
              Open architecture details
            </Link>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Incident response summary</h3>
            <p className={styles.cardBody}>
              Escalation ownership, response timelines, and post-incident evidence loop are mapped in governance assets.
            </p>
            <Link className={styles.btnPrimary} href="/resources/executive">
              Open governance posture
            </Link>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Control ownership matrix</h3>
            <p className={styles.cardBody}>
              Control owners, review cadence, and release sign-off conditions are available in the gated packet.
            </p>
            <Link className={styles.btnPrimary} href="/resources/evidence-room">
              Request gated security packet
            </Link>
          </article>
        </div>
      </section>
    </>
  );
}
