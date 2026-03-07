import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Traditional vs AI Solutioning | HDIM Resources',
  description: 'Comprehensive comparison of 18-month traditional development vs 1.5-month AI solutioning. Timeline, cost, quality, and architecture compared.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/traditional-comparison' },
};

export default function TraditionalComparisonPage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>Traditional vs AI Solutioning</h1>
        <p className={styles.heroSubtitle}>
          18 months and a 12-person team. Or 6 weeks and one architect. Same platform.
        </p>
      </div>

      <div className={styles.container}>
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            Healthcare interoperability platforms are notoriously expensive and slow to
            build. Industry benchmarks put enterprise FHIR platforms at 12-24 months with
            teams of 8-15 engineers. HDIM was built in 6 weeks by a single architect using
            specification-driven AI solutioning. This page presents a side-by-side comparison
            across every dimension that matters to healthcare technology leaders.
          </p>
        </div>

        {/* Core Comparison Table */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Side-by-Side Comparison</h2>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Dimension</th>
                <th>Traditional Development</th>
                <th>AI Solutioning (HDIM)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Timeline</td>
                <td>12-18 months to MVP</td>
                <td className={styles.highlight}>6 weeks to production-ready</td>
              </tr>
              <tr>
                <td>Team size</td>
                <td>8-15 engineers + PM + QA</td>
                <td className={styles.highlight}>1 architect</td>
              </tr>
              <tr>
                <td>Annual cost</td>
                <td>$1.5M-$3M (salaries + infra)</td>
                <td className={styles.highlight}>Under $200K (architect + compute)</td>
              </tr>
              <tr>
                <td>Services delivered</td>
                <td>5-10 services in year one</td>
                <td className={styles.highlight}>51+ services in 6 weeks</td>
              </tr>
              <tr>
                <td>Test coverage</td>
                <td>60-70% (varies by team discipline)</td>
                <td className={styles.highlight}>613+ tests, contract + migration validated</td>
              </tr>
              <tr>
                <td>API documentation</td>
                <td>Partial, often outdated</td>
                <td className={styles.highlight}>157 endpoints, OpenAPI 3.0, interactive</td>
              </tr>
              <tr>
                <td>Architecture consistency</td>
                <td>Degrades with team size</td>
                <td className={styles.highlight}>Uniform — spec-enforced patterns</td>
              </tr>
              <tr>
                <td>HIPAA compliance</td>
                <td>Requires dedicated security review</td>
                <td className={styles.highlight}>Built into specifications, validated by tests</td>
              </tr>
              <tr>
                <td>Multi-tenant isolation</td>
                <td>Often retrofitted</td>
                <td className={styles.highlight}>Enforced at database level from day one</td>
              </tr>
              <tr>
                <td>Distributed tracing</td>
                <td>Added in later phases</td>
                <td className={styles.highlight}>OpenTelemetry across all services</td>
              </tr>
              <tr>
                <td>Event architecture</td>
                <td>Added incrementally</td>
                <td className={styles.highlight}>CQRS + event sourcing from phase 4</td>
              </tr>
              <tr>
                <td>CI/CD maturity</td>
                <td>6-12 months to optimize</td>
                <td className={styles.highlight}>Parallelized pipeline, 42.5% faster feedback</td>
              </tr>
              <tr>
                <td>Database migrations</td>
                <td>Mix of tools, manual oversight</td>
                <td className={styles.highlight}>100% Liquibase, 199/199 rollback coverage</td>
              </tr>
              <tr>
                <td>Rework rate</td>
                <td>30-40% of sprint capacity</td>
                <td className={styles.highlight}>Under 10% — specs prevent misalignment</td>
              </tr>
              <tr>
                <td>Knowledge concentration risk</td>
                <td>Distributed across team (bus factor 3-5)</td>
                <td>Concentrated in architect + specs (bus factor 1-2)</td>
              </tr>
              <tr>
                <td>Onboarding new engineers</td>
                <td>2-4 weeks with mentoring</td>
                <td className={styles.highlight}>Days — specs + docs + Swagger UI</td>
              </tr>
              <tr>
                <td>Compliance evidence</td>
                <td>Generated at audit time</td>
                <td className={styles.highlight}>Continuous — commit-backed evidence</td>
              </tr>
              <tr>
                <td>Time to first customer value</td>
                <td>6-12 months</td>
                <td className={styles.highlight}>8 weeks</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Timeline Visualization */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Timeline Comparison</h2>
          <p className={styles.sectionBody}>
            Traditional healthcare platform development follows a waterfall-influenced
            cadence even within agile teams. Requirements gathering, architecture review
            boards, vendor evaluation, and compliance checkpoints add months before a
            single service reaches production.
          </p>

          <h3 className={styles.sectionSubtitle}>Traditional: 18-Month Timeline</h3>
          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Months 1-3</div>
              <div>
                <div className={styles.timelineTitle}>Requirements &amp; Architecture</div>
                <p className={styles.sectionBody}>Stakeholder interviews, RFP process, architecture review board approval, technology selection, team hiring.</p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Months 4-8</div>
              <div>
                <div className={styles.timelineTitle}>Core Development</div>
                <p className={styles.sectionBody}>Build 3-5 core services, establish CI/CD, database schema design, initial FHIR integration. First demo to stakeholders.</p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Months 9-12</div>
              <div>
                <div className={styles.timelineTitle}>Integration &amp; Hardening</div>
                <p className={styles.sectionBody}>Cross-service integration, security audit, performance testing, compliance documentation, QA cycles.</p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Months 13-18</div>
              <div>
                <div className={styles.timelineTitle}>Pilot &amp; Iteration</div>
                <p className={styles.sectionBody}>First pilot customer, bug fixes, feature requests, compliance remediation, production hardening.</p>
              </div>
            </div>
          </div>

          <h3 className={styles.sectionSubtitle}>AI Solutioning: 6-Week Timeline</h3>
          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Week 1</div>
              <div>
                <div className={styles.timelineTitle}>Specification &amp; Foundation</div>
                <p className={styles.sectionBody}>Complete architectural specifications, API contracts, data models, security requirements. Generate foundation services and database schemas.</p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Weeks 2-3</div>
              <div>
                <div className={styles.timelineTitle}>Service Generation</div>
                <p className={styles.sectionBody}>Generate 30+ services from specifications. Gateway architecture, FHIR integration, event sourcing, quality measure engine all delivered in parallel.</p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Weeks 4-5</div>
              <div>
                <div className={styles.timelineTitle}>Integration &amp; Validation</div>
                <p className={styles.sectionBody}>Cross-service integration testing, contract validation, distributed tracing verification, compliance evidence generation.</p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Week 6</div>
              <div>
                <div className={styles.timelineTitle}>Production Readiness</div>
                <p className={styles.sectionBody}>CI/CD optimization, documentation completion, demo environment, pilot preparation. 51+ services production-ready.</p>
              </div>
            </div>
          </div>
        </div>

        {/* Outcome Metrics */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Outcome Metrics</h2>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>12x</div>
              <div className={styles.metricLabel}>Faster Time-to-Market</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>90%</div>
              <div className={styles.metricLabel}>Cost Reduction</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>10x</div>
              <div className={styles.metricLabel}>More Services Delivered</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>100%</div>
              <div className={styles.metricLabel}>Liquibase Rollback Coverage</div>
            </div>
          </div>
        </div>

        {/* Honest Assessment */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Where Traditional Still Wins</h2>
          <p className={styles.sectionBody}>
            AI solutioning is not universally superior. Traditional approaches retain
            advantages in specific contexts that healthcare leaders should weigh honestly.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>Team knowledge distribution: traditional teams have lower bus factor risk with more engineers holding institutional knowledge.</li>
              <li>Regulatory familiarity: established teams with HITRUST or SOC 2 experience carry compliance muscle memory that specifications alone cannot encode.</li>
              <li>Complex UI/UX: user interface design and clinical workflow optimization still benefit from dedicated design teams working iteratively with clinicians.</li>
              <li>Vendor relationships: traditional teams often have existing EHR vendor relationships that accelerate integration timelines.</li>
            </ul>
          </div>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>See the Evidence</h2>
          <p>Every claim on this page is backed by commit history, test results, and validation artifacts.</p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/resources">Explore Resources</Link>
            <Link className={styles.btnSecondary} href="/resources/build-evidence">View Build Evidence</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
