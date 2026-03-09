'use client';

import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/agui-portal.module.css';

export default function TechnicalHub() {
  const transformationPath = [
    { title: 'Origin Story', href: '/resources/origin-story' },
    { title: 'Executive Summary', href: '/resources/executive-summary' },
    { title: 'Java Rebuild', href: '/resources/java-rebuild' },
    { title: 'Architecture Evolution', href: '/resources/architecture-evolution' },
    { title: 'Evidence Room', href: '/resources/evidence-room' },
  ];

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>TECHNICAL EVALUATION PATH</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Architecture, contracts, runtime orchestration, and evidence-driven release policy.
          </h1>
          <p className={styles.heroBody}>
            This hub maps event flow, gateway trust, tenant isolation, contract strictness, and release gate behavior to concrete implementation and validation artifacts.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/architecture-evolution">
              Open Architecture Timeline
            </Link>
            <Link className={styles.btnGhost} href="/resources/spec-driven-development">
              View Spec-Driven Model
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Transformation Path
        </h2>
        <p className={styles.sectionLead}>
          Follow the ordered path from story context to executive framing, rebuild deep dive, architecture timeline, and final evidence validation.
        </p>
        <div className={styles.cardGrid}>
          {transformationPath.map((step, index) => (
            <article key={step.href} className={styles.card}>
              <h3 className={styles.cardTitle}>
                {index + 1}. {step.title}
              </h3>
              <p>
                <Link className={styles.btnPrimary} href={step.href}>
                  Open {step.title}
                </Link>
              </p>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Engineering decision map
        </h2>
        <p className={styles.sectionLead}>
          Runtime behavior is constrained by architecture decisions and test gates. Changes are expected to maintain contract compatibility and release evidence freshness.
        </p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Event-native services</h3>
            <p className={styles.cardBody}>
              Service boundaries follow event contracts and explicit failure handling with tenant-safe DLQ patterns.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Release gates</h3>
            <p className={styles.cardBody}>
              Preflight, contract tests, security regressions, and upstream CI freshness checks are mandatory before go/no-go.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Observability + proof</h3>
            <p className={styles.cardBody}>
              Every release decision references validator logs, control matrix state, and evidence index entries.
            </p>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Control plane reference
        </h2>
        <div className={styles.cardGrid}>
          <figure className={styles.figure}>
            <Image
              src="/resources/platform-control-plane.svg"
              alt="Platform control plane"
              width={560}
              height={400}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              AI-generated control-plane visual linked to ADR and release governance narratives.
            </figcaption>
          </figure>
        </div>
      </section>
    </>
  );
}
