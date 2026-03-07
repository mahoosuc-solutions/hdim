'use client';

import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/agui-portal.module.css';

export default function ExecutiveHub() {
  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>EXECUTIVE + COMPLIANCE PATH</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Control posture, audit traceability, and release confidence in one operational view.
          </h1>
          <p className={styles.heroBody}>
            This hub consolidates regulatory controls, vendor risk posture, gap status, and release-governance checkpoints so leadership teams can evaluate readiness with command-level evidence.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/build-evidence">
              Review Gap &amp; Evidence Story
            </Link>
            <Link className={styles.btnGhost} href="/resources/fhir-evidence">
              Validation Approach
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Decision cockpit
        </h2>
        <p className={styles.sectionLead}>
          Each decision path references a control, an owner, and an evidence artifact. Missing evidence defaults to fail-closed behavior.
        </p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Regulatory control matrix</h3>
            <p className={styles.cardBody}>
              Maps release-critical controls to concrete logs and reports. Used for release board sign-off and oversight responses.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Gap closure governance</h3>
            <p className={styles.cardBody}>
              Tracks closure lifecycle for critical and high findings, including ownership, ETA, and validator outputs.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Third-party risk operations</h3>
            <p className={styles.cardBody}>
              Vendor risk register captures BAA/DPA/SOC posture, exceptions, and quarterly review cadence.
            </p>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Control-to-release loop
        </h2>
        <div className={styles.cardGrid}>
          <figure className={styles.figure}>
            <Image
              src="/resources/compliance-loop.svg"
              alt="Compliance loop"
              width={560}
              height={400}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              AI-generated compliance loop visual with release-governance decision points.
            </figcaption>
          </figure>
        </div>
        <div className={styles.notice}>
          Required evidence set: gap register, scorecard, control matrix, regulatory readiness report, and validation logs.
        </div>
      </section>
    </>
  );
}
