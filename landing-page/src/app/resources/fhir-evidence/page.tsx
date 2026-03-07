import type { Metadata } from 'next';
import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Clinical Validation Approach | HDIM Resources',
  description:
    'How HDIM validates quality measure accuracy: automated test pipelines, FHIR R4 conformance, and continuous evidence generation for regulatory readiness.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/fhir-evidence' },
};

export default function FHIREvidencePage() {
  return (
    <div className={styles.pageWrapper}>
      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>Clinical Validation Approach</h1>
        <p className={styles.heroSubtitle}>
          How HDIM ensures quality measure accuracy through automated validation, FHIR R4 conformance testing, and continuous evidence generation.
        </p>
      </section>

      <div className={styles.container}>
        {/* Why Validation Matters */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Why Validation Matters</h2>
          <p className={styles.sectionBody}>
            Healthcare quality measurement errors have real consequences: incorrect HEDIS rates affect Star Ratings, which determine Medicare Advantage reimbursement. A 0.5-star drop can cost a health plan $50M+ in lost revenue annually (CMS Star Ratings methodology, 42 CFR 422.166).
          </p>
          <p className={styles.sectionBody}>
            HDIM treats validation as a continuous process, not a phase. Every code change triggers automated checks against clinical logic, FHIR conformance, and measure accuracy before reaching production.
          </p>
        </section>

        {/* Validation Layers */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Three Layers of Validation</h2>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>1. FHIR R4 Conformance</h3>
            <p className={styles.cardBody}>
              Every clinical resource processed by HDIM is validated against FHIR R4 profiles before storage. This catches data quality issues at ingestion — malformed observations, missing code systems, or invalid reference chains — rather than at measure evaluation time.
            </p>
            <div className={styles.keyPoints}>
              <ul>
                <li>US Core profile validation on Patient, Condition, Observation, Procedure, MedicationRequest</li>
                <li>Value set binding checks against NLM-published VSAC terminologies</li>
                <li>Reference integrity validation across resource bundles</li>
              </ul>
            </div>
          </div>

          <div className={styles.card} style={{ marginTop: '1rem' }}>
            <h3 className={styles.cardTitle}>2. Measure Logic Verification</h3>
            <p className={styles.cardBody}>
              CQL measure logic is tested against known patient scenarios with expected outcomes. Each HEDIS measure includes test patients that exercise initial population, denominator, numerator, and exclusion paths.
            </p>
            <div className={styles.keyPoints}>
              <ul>
                <li>613+ automated tests across quality measure, care gap, and patient services</li>
                <li>Contract tests between frontend and backend validate API stability</li>
                <li>Entity-migration tests prevent schema drift between JPA entities and Liquibase migrations</li>
              </ul>
            </div>
          </div>

          <div className={styles.card} style={{ marginTop: '1rem' }}>
            <h3 className={styles.cardTitle}>3. Pipeline Integration Testing</h3>
            <p className={styles.cardBody}>
              End-to-end validation confirms that a patient event entering the Kafka pipeline produces the correct care gap actions after flowing through FHIR ingestion, measure fan-out, and CQL evaluation.
            </p>
            <div className={styles.keyPoints}>
              <ul>
                <li>One-patient-to-N-measures fan-out validated with 5 and 10 concurrent measures</li>
                <li>Multi-tenant isolation confirmed — tenant A data never surfaces in tenant B evaluations</li>
                <li>Dead letter queue handling tested for malformed events and transient failures</li>
              </ul>
            </div>
          </div>
        </section>

        {/* Real Output */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>What Validation Looks Like</h2>
          <figure style={{ margin: 0 }}>
            <Image
              src="/resources/screenshots/results-evaluation.jpg"
              alt="HDIM Clinical Portal showing HEDIS evaluation results with Breast Cancer Screening measure detail"
              width={1916}
              height={922}
              style={{ width: '100%', height: 'auto', borderRadius: '8px', border: '1px solid #e2e8f0' }}
            />
            <figcaption style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.5rem' }}>
              Clinical Portal: HEDIS evaluation results with measure-level detail (BCS, CCS, CDC, CBP, SPC). Each row shows patient context, severity, HEDIS category, and compliance outcome.
            </figcaption>
          </figure>
        </section>

        {/* Continuous Evidence */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Continuous Evidence Generation</h2>
          <p className={styles.sectionBody}>
            Regulatory audits require proof that validation actually ran, not just that it could run. HDIM generates timestamped evidence artifacts on every release candidate.
          </p>

          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Evidence Type</th>
                <th>Purpose</th>
                <th>Frequency</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Test execution reports</strong></td>
                <td>Prove all 613+ tests passed before deployment</td>
                <td>Every PR and release</td>
              </tr>
              <tr>
                <td><strong>FHIR conformance reports</strong></td>
                <td>Confirm R4 profile compliance</td>
                <td>Every ingestion pipeline change</td>
              </tr>
              <tr>
                <td><strong>Migration rollback coverage</strong></td>
                <td>199/199 changesets have verified rollback SQL</td>
                <td>Every schema change</td>
              </tr>
              <tr>
                <td><strong>Security scan results</strong></td>
                <td>Trivy container scanning, dependency audit</td>
                <td>Every build</td>
              </tr>
              <tr>
                <td><strong>Compliance control matrix</strong></td>
                <td>Map controls to evidence for HIPAA / SOC 2 readiness</td>
                <td>Per release candidate</td>
              </tr>
            </tbody>
          </table>
        </section>

        {/* What This Means for Buyers */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>What This Means for Your Organization</h2>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>Faster audits:</strong> Pre-generated evidence packages reduce audit preparation from weeks to hours</li>
              <li><strong>Lower risk:</strong> Automated validation catches clinical logic errors before they affect Star Ratings</li>
              <li><strong>Regulatory confidence:</strong> Commit-backed evidence ties every claim to a verifiable code change</li>
              <li><strong>Continuous compliance:</strong> Validation runs on every change, not just before audits</li>
            </ul>
          </div>
        </section>

        {/* Navigation */}
        <section className={styles.ctaSection}>
          <h2>See the Pipeline in Action</h2>
          <p>Explore how patient events flow through the FHIR pipeline from ingestion to care gap detection.</p>
          <div className={styles.ctaButtons}>
            <Link href="/resources/fhir-pipeline" className={styles.btnPrimary}>
              View FHIR Pipeline
            </Link>
            <Link href="/resources/performance" className={styles.btnSecondary}>
              Performance Benchmarks
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}
