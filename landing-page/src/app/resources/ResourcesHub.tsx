'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import styles from '@/styles/agui-portal.module.css';

export default function ResourcesHub() {
  const { t } = useLanguage();

  return (
    <>
      <section className={styles.hero}>
        <div
          className={styles.heroPanel}
          style={{
            background:
              'linear-gradient(135deg, rgba(7, 26, 45, 0.92), rgba(18, 77, 102, 0.82)), url("/resources/screenshots/results-evaluation.jpg") center/cover',
          }}
        >
          <span className={styles.kicker}>{t('hero_kicker')}</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk), var(--font-manrope), sans-serif' }}>
            {t('hero_title')}
          </h1>
          <p className={styles.heroBody}>{t('hero_body')}</p>
          <div className={styles.heroActions}>
            <a className={styles.btnPrimary} href="#personas">
              {t('hero_cta_personas')}
            </a>
            <Link className={styles.btnGhost} href="/resources/architecture">
              {t('hero_cta_arch')}
            </Link>
          </div>
          <div className={styles.metricsGrid}>
            <article className={styles.metricCard}>
              <div className={styles.metricValue} style={{ fontFamily: 'var(--font-space-grotesk)' }}>52</div>
              <div className={styles.metricLabel}>HEDIS measures supported</div>
            </article>
            <article className={styles.metricCard}>
              <div className={styles.metricValue} style={{ fontFamily: 'var(--font-space-grotesk)' }}>&lt;1s</div>
              <div className={styles.metricLabel}>care gap detection</div>
            </article>
            <article className={styles.metricCard}>
              <div className={styles.metricValue} style={{ fontFamily: 'var(--font-space-grotesk)' }}>8+</div>
              <div className={styles.metricLabel}>EHR integrations</div>
            </article>
            <article className={styles.metricCard}>
              <div className={styles.metricValue} style={{ fontFamily: 'var(--font-space-grotesk)' }}>100%</div>
              <div className={styles.metricLabel}>HIPAA audit coverage</div>
            </article>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Human impact behind the metrics
        </h2>
        <p className={styles.sectionLead}>
          The platform is built to help clinicians, care teams, and patients. These views show how technology supports people at the point of action.
        </p>
        <div className={styles.cardGrid}>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/provider-dashboard.jpg"
              alt="Provider dashboard with quality signals and patient-focused actions"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              Provider workflows prioritize next-best actions for patient outcomes.
            </figcaption>
          </figure>
          <figure className={styles.figure}>
            <Image
              src="/resources/screenshots/patient-management.jpg"
              alt="Patient management panel with outreach and quality context"
              width={560}
              height={360}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              Care coordination and patient management stay tied to measurable quality goals.
            </figcaption>
          </figure>
        </div>
      </section>

      <section className={styles.section} id="personas">
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          {t('personas_title')}
        </h2>
        <p className={styles.sectionLead}>{t('personas_body')}</p>
        <div className={styles.personaGrid}>
          <article className={styles.personaCard}>
            <div className={`${styles.personaTop} ${styles.personaTopExec}`} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
              {t('persona_exec_title')}
            </div>
            <div className={styles.personaBody}>
              <ul>
                <li>{t('persona_exec_li1')}</li>
                <li>{t('persona_exec_li2')}</li>
                <li>{t('persona_exec_li3')}</li>
              </ul>
              <p>
                <Link className={styles.btnPrimary} href="/resources/executive">
                  {t('persona_exec_cta')}
                </Link>
              </p>
            </div>
          </article>

          <article className={styles.personaCard}>
            <div className={`${styles.personaTop} ${styles.personaTopClinical}`} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
              {t('persona_clinical_title')}
            </div>
            <div className={styles.personaBody}>
              <ul>
                <li>{t('persona_clinical_li1')}</li>
                <li>{t('persona_clinical_li2')}</li>
                <li>{t('persona_clinical_li3')}</li>
              </ul>
              <p>
                <Link className={styles.btnPrimary} href="/resources/clinical">
                  {t('persona_clinical_cta')}
                </Link>
              </p>
            </div>
          </article>

          <article className={styles.personaCard}>
            <div className={`${styles.personaTop} ${styles.personaTopTechnical}`} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
              {t('persona_technical_title')}
            </div>
            <div className={styles.personaBody}>
              <ul>
                <li>{t('persona_technical_li1')}</li>
                <li>{t('persona_technical_li2')}</li>
                <li>{t('persona_technical_li3')}</li>
              </ul>
              <p>
                <Link className={styles.btnPrimary} href="/resources/technical">
                  {t('persona_technical_cta')}
                </Link>
              </p>
            </div>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          {t('stack_title')}
        </h2>
        <p className={styles.sectionLead}>{t('stack_body')}</p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Event-based data backbone</h3>
            <p className={styles.cardBody}>
              FHIR and HL7 flows enter tenant-aware pipelines with policy checks, dead-letter controls, and reprocessing safety rails.
            </p>
            <div className={styles.badges}>
              <span className={styles.badge}>Kafka</span>
              <span className={styles.badge}>FHIR</span>
              <span className={styles.badge}>Validation Gates</span>
            </div>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>AI-assisted introspection</h3>
            <p className={styles.cardBody}>
              Known patterns drive deterministic workflows while unknown patterns are surfaced with explainable rationale and review checkpoints.
            </p>
            <div className={styles.badges}>
              <span className={styles.badge}>Agent Orchestration</span>
              <span className={styles.badge}>Explainability</span>
              <span className={styles.badge}>Audit Trail</span>
            </div>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Regulated release rail</h3>
            <p className={styles.cardBody}>
              Release decisions are tied to evidence: gap register, control matrix, scorecard, and command-level validation outputs.
            </p>
            <div className={styles.badges}>
              <span className={styles.badge}>SOC2/HIPAA Alignment</span>
              <span className={styles.badge}>Go/No-Go</span>
              <span className={styles.badge}>Evidence Index</span>
            </div>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          {t('proof_title')}
        </h2>
        <p className={styles.sectionLead}>{t('proof_body')}</p>
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
              Platform control plane showing service topology and governance boundaries.
            </figcaption>
          </figure>
          <figure className={styles.figure}>
            <Image
              src="/resources/persona-map.svg"
              alt="Persona map"
              width={560}
              height={400}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              Persona routing map used by the landing and subarea navigation model.
            </figcaption>
          </figure>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Enterprise buyer diligence
        </h2>
        <p className={styles.sectionLead}>
          Start with public trust signals, then request role-aligned gated evidence for CIO/CISO and procurement workflows.
        </p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>HIMSS Briefing</h3>
            <p className={styles.cardBody}>
              Conference-ready narrative connecting accelerator, platform, validation, and performance into one briefing flow.
            </p>
            <p>
              <Link className={styles.btnPrimary} href="/resources/himss-brief">
                Open HIMSS Brief
              </Link>
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Trust Center</h3>
            <p className={styles.cardBody}>
              Public claim-to-proof mapping with evidence freshness and observed versus modeled data labels.
            </p>
            <p>
              <Link className={styles.btnPrimary} href="/resources/trust-center">
                Open Trust Center
              </Link>
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Gated Evidence Room</h3>
            <p className={styles.cardBody}>
              Request deeper diligence packets for security, reliability, and commercial reviews.
            </p>
            <p>
              <Link className={styles.btnPrimary} href="/resources/evidence-room">
                Open Evidence Room
              </Link>
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>BSL & licensing transparency</h3>
            <p className={styles.cardBody}>
              Review the Business Source License rollout plan, controlled-content boundaries, and public compliance references.
            </p>
            <p>
              <Link className={styles.btnPrimary} href="/resources/licensing">
                Open Licensing Transparency
              </Link>
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Role-specific pathways</h3>
            <p className={styles.cardBody}>
              Access tailored flows for CIO/CISO technical diligence and procurement buyer decision support.
            </p>
            <p>
              <Link className={styles.btnPrimary} href="/resources/cio-ciso">
                Open CIO/CISO Path
              </Link>
            </p>
            <p>
              <Link className={styles.btnGhostDark} href="/resources/procurement">
                Open Procurement Path
              </Link>
            </p>
          </article>
        </div>
      </section>
    </>
  );
}
