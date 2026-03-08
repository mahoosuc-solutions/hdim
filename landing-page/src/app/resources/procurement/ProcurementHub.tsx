import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';
import { getEvidenceArtifact } from '@/lib/resources/evidenceManifest';

export default function ProcurementHub() {
  const executiveSummary = getEvidenceArtifact('executiveSummary');
  const clinicalHub = getEvidenceArtifact('clinicalHub');
  const executiveHub = getEvidenceArtifact('executiveHub');
  const releaseEvidence = getEvidenceArtifact('releaseEvidence');
  const aiMetrics = getEvidenceArtifact('aiMetrics');
  const aiJourney = getEvidenceArtifact('aiJourney');

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>PROCUREMENT BUYER PAGE</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Commercial diligence with clear implementation and accountability expectations.
          </h1>
          <p className={styles.heroBody}>
            Procurement and sourcing teams can use this page to align contract expectations with delivery evidence, rollout controls, and measured outcomes.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/evidence-room">Request procurement packet</Link>
            <Link className={styles.btnGhost} href="/resources/trust-center">Back to trust center</Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Procurement diligence checklist
        </h2>
        <div className={styles.cardGrid}>
          <ClaimProofCard
            claim="Implementation approach is explicit"
            outcome="Scope, sequencing, and risk dependencies are made visible before contract signature."
            proofItems={[
              executiveSummary,
              clinicalHub,
            ]}
          />
          <ClaimProofCard
            claim="Commercial risk controls are clear"
            outcome="Service expectations and escalation paths can be validated against operational governance artifacts."
            proofItems={[
              executiveHub,
              releaseEvidence,
            ]}
          />
          <ClaimProofCard
            claim="Buyer outcomes are measurable"
            outcome="Success criteria can be tracked through quality, operational, and financial indicators."
            proofItems={[
              aiMetrics,
              aiJourney,
            ]}
          />
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Commercial trust assets
        </h2>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Contracting posture</h3>
            <p className={styles.cardBody}>
              BAA and DPA availability, security exhibit references, and legal escalation path for procurement review.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Service-level expectations</h3>
            <p className={styles.cardBody}>
              Response-time objectives, change-control commitments, and governance checkpoints tied to release evidence.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Implementation SOW baseline</h3>
            <p className={styles.cardBody}>
              Standard scope structure, onboarding milestones, and dependency model are included in the gated packet.
            </p>
            <Link className={styles.btnPrimary} href="/resources/evidence-room">
              Request procurement packet
            </Link>
          </article>
        </div>
      </section>
    </>
  );
}
