import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';
import EvidenceRequestForm from '@/components/resources/EvidenceRequestForm';
import { getEvidenceArtifact } from '@/lib/resources/evidenceManifest';

export default function EvidenceRoomHub() {
  const executiveHub = getEvidenceArtifact('executiveHub');
  const architectureHub = getEvidenceArtifact('architectureHub');
  const releaseEvidence = getEvidenceArtifact('releaseEvidence');
  const technicalHub = getEvidenceArtifact('technicalHub');
  const clinicalHub = getEvidenceArtifact('clinicalHub');
  const executiveSummary = getEvidenceArtifact('executiveSummary');

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>GATED EVIDENCE ROOM</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Structured diligence packets for security, operations, and procurement reviews.
          </h1>
          <p className={styles.heroBody}>
            Use this page to request artifacts not published publicly, including detailed architecture diagrams, control evidence exports, and commercial packet details.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/trust-center">
              Back to Trust Center
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Request access
        </h2>
        <p className={styles.sectionLead}>
          Complete the request to receive the packet aligned to your role and review scope.
        </p>
        <EvidenceRequestForm />
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Available packet categories
        </h2>
        <div className={styles.cardGrid}>
          <ClaimProofCard
            claim="Security and compliance packet"
            outcome="Detailed control mapping, incident readiness process, and latest attestation package."
            proofItems={[
              executiveHub,
              architectureHub,
            ]}
          />
          <ClaimProofCard
            claim="Reliability and release packet"
            outcome="Go/no-go criteria, release scorecards, validation evidence, and operational runbooks."
            proofItems={[
              releaseEvidence,
              technicalHub,
            ]}
          />
          <ClaimProofCard
            claim="Commercial and implementation packet"
            outcome="Implementation sequencing, success metrics model, and service expectation documentation."
            proofItems={[
              clinicalHub,
              executiveSummary,
            ]}
          />
        </div>
      </section>
    </>
  );
}
