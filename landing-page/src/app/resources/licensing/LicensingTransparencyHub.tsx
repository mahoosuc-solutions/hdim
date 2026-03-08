import Link from 'next/link';
import styles from '@/styles/agui-portal.module.css';

const repoBase = 'https://github.com/mahoosuc-solutions/hdim/blob/master';

export default function LicensingTransparencyHub() {
  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>LICENSING TRANSPARENCY</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Business Source License rollout with explicit boundaries and public proof links.
          </h1>
          <p className={styles.heroBody}>
            This page explains how HDIM is being prepared for BSL release, what content is open versus controlled, and where buyers can validate compliance posture.
          </p>
          <div className={styles.heroActions}>
            <a className={styles.btnPrimary} href={`${repoBase}/docs/compliance/BSL_RELEASE_PLAN.md`} target="_blank" rel="noreferrer">
              Open BSL Release Plan
            </a>
            <Link className={styles.btnGhost} href="/resources/trust-center">
              Open Trust Center
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Public transparency links
        </h2>
        <p className={styles.sectionLead}>
          Use these links in customer and investor materials so licensing and diligence context is always visible.
        </p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Portal pages</h3>
            <ul style={{ lineHeight: 1.9, paddingLeft: '1rem' }}>
              <li><Link href="/resources">Resources hub</Link></li>
              <li><Link href="/resources/licensing">Licensing transparency</Link></li>
              <li><Link href="/resources/trust-center">Trust center</Link></li>
              <li><Link href="/resources/evidence-room">Evidence room</Link></li>
              <li><Link href="/resources/procurement">Procurement path</Link></li>
              <li><Link href="/terms">Terms of service</Link></li>
            </ul>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Repository source-of-truth</h3>
            <ul style={{ lineHeight: 1.9, paddingLeft: '1rem' }}>
              <li>
                <a href={`${repoBase}/docs/compliance/BSL_RELEASE_PLAN.md`} target="_blank" rel="noreferrer">
                  BSL release plan
                </a>
              </li>
              <li>
                <a href={`${repoBase}/docs/compliance/LICENSING-BOUNDARY.md`} target="_blank" rel="noreferrer">
                  Licensing boundary
                </a>
              </li>
              <li>
                <a href={`${repoBase}/docs/compliance/THIRD_PARTY_NOTICES.md`} target="_blank" rel="noreferrer">
                  Third-party notices
                </a>
              </li>
            </ul>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Controlled content policy snapshot
        </h2>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Open distribution</h3>
            <p className={styles.cardBody}>
              Core platform code, infrastructure automation, and integration tooling may be distributed when they do not embed licensed HEDIS content or third-party datasets.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Restricted distribution</h3>
            <p className={styles.cardBody}>
              NCQA-owned HEDIS content, HL7 specification text, and licensed code-system datasets must not be redistributed through public repositories or public portal artifacts.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Commercial/legal handling</h3>
            <p className={styles.cardBody}>
              Customer platform usage, SLAs, and PHI obligations remain governed by executed MSA and BAA terms.
            </p>
          </article>
        </div>
        <p className={styles.sectionLead}>
          This page is operational guidance for transparency and is not legal advice.
        </p>
      </section>
    </>
  );
}
