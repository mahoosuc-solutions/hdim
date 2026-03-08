import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';

export default function TrustCenterHub() {
  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>TRUST CENTER</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Enterprise trust signals for healthcare buyers in one verifiable path.
          </h1>
          <p className={styles.heroBody}>
            This public trust center maps core platform claims to proof artifacts, evidence freshness, and observed versus modeled data status.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/evidence-room">
              Enter Evidence Room
            </Link>
            <Link className={styles.btnGhost} href="/resources/build-evidence">
              View Release Evidence Narrative
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Claim to proof index
        </h2>
        <p className={styles.sectionLead}>
          Buyers can validate platform claims without waiting for custom demos. Each claim links to specific operational evidence.
        </p>
        <div className={styles.cardGrid}>
          <ClaimProofCard
            claim="Release governance is fail-closed"
            outcome="High-risk gaps or stale evidence trigger No-Go outcomes until remediation and revalidation complete."
            proofItems={[
              { label: 'Release Evidence Hub', href: '/resources/build-evidence', freshness: 'Updated each release', dataStatus: 'Observed' },
              { label: 'Technical Evaluation Hub', href: '/resources/technical', freshness: 'Monthly architecture review', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Security posture is measurable"
            outcome="Control coverage and exceptions are documented with ownership, review cadence, and audit traceability."
            proofItems={[
              { label: 'Executive & Compliance Hub', href: '/resources/executive', freshness: 'Quarterly control refresh', dataStatus: 'Observed' },
              { label: 'FHIR Validation Evidence', href: '/resources/fhir-evidence', freshness: 'Pipeline-driven', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Clinical impact and ROI are accountable"
            outcome="Clinical and executive stakeholders can tie quality, operational, and financial outcomes to explicit assumptions."
            proofItems={[
              { label: 'Clinical Leadership Hub', href: '/resources/clinical', freshness: 'Monthly program review', dataStatus: 'Observed' },
              { label: 'Executive Summary', href: '/resources/executive-summary', freshness: 'Quarterly update', dataStatus: 'Modeled' },
            ]}
          />
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Role-specific entry points
        </h2>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>CIO/CISO diligence path</h3>
            <p className={styles.cardBody}>Security architecture, control operations, incident and release governance posture.</p>
            <Link className={styles.btnPrimary} href="/resources/cio-ciso">Open CIO/CISO page</Link>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Procurement diligence path</h3>
            <p className={styles.cardBody}>Commercial structure, service levels, onboarding plan, and contracting evidence.</p>
            <Link className={styles.btnPrimary} href="/resources/procurement">Open Procurement page</Link>
          </article>
        </div>
      </section>
    </>
  );
}
