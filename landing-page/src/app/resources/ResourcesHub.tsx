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
        <div className={styles.heroPanel}>
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
    </>
  );
}
