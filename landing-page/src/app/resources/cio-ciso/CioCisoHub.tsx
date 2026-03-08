import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';

export default function CioCisoHub() {
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
              { label: 'Executive & Compliance Hub', href: '/resources/executive', freshness: 'Quarterly', dataStatus: 'Observed' },
              { label: 'Release Evidence Hub', href: '/resources/build-evidence', freshness: 'Per release', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Architecture supports safe scale"
            outcome="Event contracts, tenant isolation, and runtime policies are designed for regulated workloads."
            proofItems={[
              { label: 'Technical Evaluation Hub', href: '/resources/technical', freshness: 'Monthly', dataStatus: 'Observed' },
              { label: 'Architecture Evolution', href: '/resources/architecture-evolution', freshness: 'Monthly', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Operational risk is measurable"
            outcome="Leadership can evaluate go/no-go status based on current risk posture and not anecdotal confidence."
            proofItems={[
              { label: 'AI Metrics', href: '/resources/ai-metrics', freshness: 'Monthly', dataStatus: 'Observed' },
              { label: 'Performance', href: '/resources/performance', freshness: 'Monthly', dataStatus: 'Modeled' },
            ]}
          />
        </div>
      </section>
    </>
  );
}
