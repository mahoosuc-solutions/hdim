'use client';

import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/agui-portal.module.css';

export default function ClinicalHub() {
  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroPanel}>
          <span className={styles.kicker}>CLINICAL LEADERSHIP PATH</span>
          <h1 className={styles.heroTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
            Real-time care gap detection across 52 HEDIS measures — from fragmented records to prioritized interventions.
          </h1>
          <p className={styles.heroBody}>
            HDIM evaluates quality measures as patient events arrive, not in overnight batches. Clinical teams see care gaps within seconds and get ranked next-best actions with transparent rationale.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.btnPrimary} href="/resources/fhir-pipeline">
              See the FHIR Pipeline
            </Link>
            <Link className={styles.btnGhost} href="/resources/performance">
              Performance Benchmarks
            </Link>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          HEDIS measures supported
        </h2>
        <p className={styles.sectionLead}>
          HDIM supports the full spectrum of HEDIS quality measures used in Medicare Advantage Star Ratings, Medicaid managed care, and commercial quality programs.
        </p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Preventive Screening</h3>
            <p className={styles.cardBody}>
              <strong>BCS</strong> (Breast Cancer Screening), <strong>CCS</strong> (Cervical Cancer Screening), <strong>COL</strong> (Colorectal Cancer Screening) — identify members overdue for age-appropriate cancer screenings.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Chronic Disease Management</h3>
            <p className={styles.cardBody}>
              <strong>CDC</strong> (Comprehensive Diabetes Care — HbA1c, eye exam, nephropathy), <strong>CBP</strong> (Controlling Blood Pressure), <strong>SPD</strong> (Statin Therapy for Diabetes) — monitor ongoing management of chronic conditions.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Behavioral Health</h3>
            <p className={styles.cardBody}>
              <strong>FUH</strong> (Follow-Up After Hospitalization for Mental Illness), <strong>FUM</strong> (Follow-Up After ED Visit for Mental Illness), <strong>ADD</strong> (ADHD Medication Follow-Up) — ensure timely behavioral health follow-up.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>Medication Management</h3>
            <p className={styles.cardBody}>
              <strong>SPC</strong> (Statin Therapy for CVD), <strong>PBH</strong> (Persistence of Beta-Blocker Treatment), <strong>SAA</strong> (Adherence to Antipsychotics) — track medication adherence and persistence for high-risk populations.
            </p>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          How care gaps are detected
        </h2>
        <p className={styles.sectionLead}>
          Each patient event triggers CQL evaluation against all applicable measures for their demographics and conditions. Gaps are surfaced in sub-second time.
        </p>
        <div className={styles.cardGrid}>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>1. Event arrives</h3>
            <p className={styles.cardBody}>
              A clinical event (encounter, lab result, prescription) enters the FHIR pipeline via EHR integration, ADT feed, or HIE subscription.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>2. Measure fan-out</h3>
            <p className={styles.cardBody}>
              One patient event fans out to N active quality measures. A 65-year-old diabetic patient triggers CDC, BCS, COL, CBP, and SPD evaluations simultaneously.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>3. Gap identification</h3>
            <p className={styles.cardBody}>
              CQL logic evaluates initial population, denominator, numerator, and exclusions. Unmet numerator criteria become actionable care gaps with clinical context.
            </p>
          </article>
          <article className={styles.card}>
            <h3 className={styles.cardTitle}>4. Action delivery</h3>
            <p className={styles.cardBody}>
              Care gaps generate FHIR Task, CarePlan, or Communication resources delivered to care teams with ranked priority and intervention guidance.
            </p>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle} style={{ fontFamily: 'var(--font-space-grotesk)' }}>
          Clinical data flow
        </h2>
        <div className={styles.cardGrid}>
          <figure className={styles.figure}>
            <Image
              src="/resources/event-fabric.svg"
              alt="Event fabric showing patient data flowing through FHIR ingestion, CQL evaluation, and care gap detection"
              width={560}
              height={400}
              className={styles.figureImg}
            />
            <figcaption className={styles.figureCaption}>
              Event-driven pipeline: patient events flow through FHIR normalization, CQL measure evaluation, and care gap action generation.
            </figcaption>
          </figure>
        </div>
      </section>
    </>
  );
}
