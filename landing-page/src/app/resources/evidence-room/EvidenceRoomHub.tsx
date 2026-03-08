import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';
import EvidenceRequestForm from '@/components/resources/EvidenceRequestForm';

export default function EvidenceRoomHub() {
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
              { label: 'Executive & Compliance Hub', href: '/resources/executive', freshness: 'Quarterly', dataStatus: 'Observed' },
              { label: 'Architecture Hub', href: '/resources/architecture', freshness: 'Monthly', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Reliability and release packet"
            outcome="Go/no-go criteria, release scorecards, validation evidence, and operational runbooks."
            proofItems={[
              { label: 'Release Evidence Hub', href: '/resources/build-evidence', freshness: 'Per release', dataStatus: 'Observed' },
              { label: 'Technical Evaluation Hub', href: '/resources/technical', freshness: 'Monthly', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Commercial and implementation packet"
            outcome="Implementation sequencing, success metrics model, and service expectation documentation."
            proofItems={[
              { label: 'Clinical Leadership Hub', href: '/resources/clinical', freshness: 'Monthly', dataStatus: 'Observed' },
              { label: 'Executive Summary', href: '/resources/executive-summary', freshness: 'Quarterly', dataStatus: 'Modeled' },
            ]}
          />
        </div>
      </section>
    </>
  );
}
