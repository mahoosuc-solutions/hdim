import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';
import ClaimProofCard from '@/components/resources/ClaimProofCard';

export default function ProcurementHub() {
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
              { label: 'Executive Summary', href: '/resources/executive-summary', freshness: 'Quarterly', dataStatus: 'Modeled' },
              { label: 'Clinical Leadership Hub', href: '/resources/clinical', freshness: 'Monthly', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Commercial risk controls are clear"
            outcome="Service expectations and escalation paths can be validated against operational governance artifacts."
            proofItems={[
              { label: 'Executive & Compliance Hub', href: '/resources/executive', freshness: 'Quarterly', dataStatus: 'Observed' },
              { label: 'Release Evidence Hub', href: '/resources/build-evidence', freshness: 'Per release', dataStatus: 'Observed' },
            ]}
          />
          <ClaimProofCard
            claim="Buyer outcomes are measurable"
            outcome="Success criteria can be tracked through quality, operational, and financial indicators."
            proofItems={[
              { label: 'AI Metrics', href: '/resources/ai-metrics', freshness: 'Monthly', dataStatus: 'Observed' },
              { label: 'AI Journey', href: '/resources/ai-journey', freshness: 'Quarterly', dataStatus: 'Modeled' },
            ]}
          />
        </div>
      </section>
    </>
  );
}
