import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'AI Solutioning Metrics | HDIM Resources',
  description:
    'Quantifying the achievement. Data-driven analysis of HDIM development success comparing AI solutioning to traditional development.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/ai-metrics' },
};

export default function AIMetricsPage() {
  return (
    <div className={styles.pageWrapper}>
      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>AI Solutioning Metrics</h1>
        <p className={styles.heroSubtitle}>Quantifying the Achievement</p>
      </section>

      <div className={styles.container}>
        <p className={styles.sectionBody}>
          <strong>Data-Driven Analysis of HDIM&apos;s Development Success</strong>
        </p>

        {/* Executive Summary */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Executive Summary</h2>
          <p className={styles.sectionBody}>
            This document quantifies HDIM&apos;s development achievement using concrete metrics. It
            compares the AI solutioning approach to traditional development and industry benchmarks,
            demonstrating significant improvements in speed, quality, cost, and outcomes.
          </p>
        </section>

        {/* Codebase Metrics */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Codebase Metrics</h2>

          <h3 className={styles.sectionSubtitle}>Current Codebase Statistics</h3>

          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>2,064</div>
              <div className={styles.metricLabel}>Java Files</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>411K</div>
              <div className={styles.metricLabel}>Lines of Code</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Microservices</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>515</div>
              <div className={styles.metricLabel}>Test Files</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>22</div>
              <div className={styles.metricLabel}>Shared Modules</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>386</div>
              <div className={styles.metricLabel}>Doc Files</div>
            </div>
          </div>

          <h3 className={styles.sectionSubtitle}>Codebase at a Glance</h3>
          <p className={styles.sectionBody}>
            These are measured values from the HDIM repository, not estimates.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>51+ microservices</strong> covering FHIR ingestion, CQL evaluation, care gap detection, event sourcing, and gateway orchestration</li>
              <li><strong>411K lines of Java code</strong> across 2,064 source files</li>
              <li><strong>515 test files</strong> with 613+ automated tests (unit, integration, contract, migration)</li>
              <li><strong>386 documentation files</strong> including ADRs, API specs, and compliance evidence</li>
              <li><strong>157 OpenAPI-documented endpoints</strong> with interactive Swagger UI</li>
            </ul>
          </div>
        </section>

        {/* Development Velocity */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Development Velocity Metrics</h2>

          <h3 className={styles.sectionSubtitle}>Timeline Comparison</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Phase</th>
                <th>Traditional</th>
                <th>AI Solutioning</th>
                <th>Improvement</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Requirements</strong></td>
                <td>2 months</td>
                <td className={styles.highlight}>1 week</td>
                <td className={styles.highlight}>87% faster</td>
              </tr>
              <tr>
                <td><strong>Design</strong></td>
                <td>2 months</td>
                <td className={styles.highlight}>1 week</td>
                <td className={styles.highlight}>87% faster</td>
              </tr>
              <tr>
                <td><strong>Development</strong></td>
                <td>8 months</td>
                <td className={styles.highlight}>4 weeks</td>
                <td className={styles.highlight}>87% faster</td>
              </tr>
              <tr>
                <td><strong>Testing</strong></td>
                <td>4 months</td>
                <td className={styles.highlight}>1 week</td>
                <td className={styles.highlight}>93% faster</td>
              </tr>
              <tr>
                <td><strong>Launch</strong></td>
                <td>2 months</td>
                <td className={styles.highlight}>1 week</td>
                <td className={styles.highlight}>87% faster</td>
              </tr>
              <tr>
                <td><strong>Total</strong></td>
                <td>12-18 months</td>
                <td className={styles.highlight}>6 weeks</td>
                <td className={styles.highlight}>12x faster</td>
              </tr>
            </tbody>
          </table>

          <h3 className={styles.sectionSubtitle}>Feature Development Speed</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Task</th>
                <th>Traditional</th>
                <th>AI Solutioning</th>
                <th>Improvement</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Service Implementation</strong></td>
                <td>2-3 weeks</td>
                <td className={styles.highlight}>2-3 days</td>
                <td className={styles.highlight}>80-90% faster</td>
              </tr>
              <tr>
                <td><strong>Feature Development</strong></td>
                <td>2-3 days</td>
                <td className={styles.highlight}>1-2 hours</td>
                <td className={styles.highlight}>90% faster</td>
              </tr>
              <tr>
                <td><strong>Test Generation</strong></td>
                <td>1 week</td>
                <td className={styles.highlight}>Concurrent</td>
                <td className={styles.highlight}>100% faster</td>
              </tr>
              <tr>
                <td><strong>Documentation</strong></td>
                <td>1 week</td>
                <td className={styles.highlight}>Concurrent</td>
                <td className={styles.highlight}>100% faster</td>
              </tr>
            </tbody>
          </table>

          <h3 className={styles.sectionSubtitle}>Development Velocity</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>
                <strong>Traditional:</strong> 400 story points per 2 weeks (team of 10), 800/month,
                14,400 over 18 months (estimated)
              </li>
              <li>
                <strong>AI Solutioning:</strong> 2,000+ story points per week (estimated),
                8,000+/month, 12,000+ over 6 weeks
              </li>
              <li>
                <strong>Velocity Improvement:</strong> 10x faster
              </li>
            </ul>
          </div>
        </section>

        {/* Cost Comparison */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Cost Metrics</h2>

          <h3 className={styles.sectionSubtitle}>Development Cost</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Item</th>
                <th>Traditional</th>
                <th>AI Solutioning</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Team</strong></td>
                <td>10-14 engineers @ $150-200K/yr</td>
                <td className={styles.highlight}>1 architect + AI tools</td>
              </tr>
              <tr>
                <td><strong>Duration</strong></td>
                <td>12-18 months</td>
                <td className={styles.highlight}>6 weeks</td>
              </tr>
              <tr>
                <td><strong>Infrastructure</strong></td>
                <td>$100K-$200K</td>
                <td className={styles.highlight}>Under $30K</td>
              </tr>
              <tr>
                <td><strong>Total (initial build)</strong></td>
                <td>$1.5M-$3.0M*</td>
                <td className={styles.highlight}>Under $200K* (85-95% savings)</td>
              </tr>
            </tbody>
          </table>

          <h3 className={styles.sectionSubtitle}>3-Year Total Cost of Ownership</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Period</th>
                <th>Traditional</th>
                <th>AI Solutioning</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Initial Build</strong></td>
                <td>$1.5M-$3.0M</td>
                <td className={styles.highlight}>Under $200K</td>
              </tr>
              <tr>
                <td><strong>Year 1 Operations</strong></td>
                <td>$500K-$850K</td>
                <td className={styles.highlight}>$150K-$250K</td>
              </tr>
              <tr>
                <td><strong>Year 2 Operations</strong></td>
                <td>$500K-$850K</td>
                <td className={styles.highlight}>$150K-$250K</td>
              </tr>
              <tr>
                <td><strong>Year 3 Operations</strong></td>
                <td>$500K-$850K</td>
                <td className={styles.highlight}>$150K-$250K</td>
              </tr>
              <tr>
                <td><strong>3-Year TCO</strong></td>
                <td>$3M-$5.5M*</td>
                <td className={styles.highlight}>$650K-$950K (70-85% savings)</td>
              </tr>
            </tbody>
          </table>
        </section>

        {/* Quality Metrics */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Quality Metrics</h2>

          <h3 className={styles.sectionSubtitle}>Quality Engineering</h3>
          <p className={styles.sectionBody}>
            Measured from the HDIM test suite and CI/CD pipeline:
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>613+ automated tests</strong> across unit, integration, contract, and entity-migration validation</li>
              <li><strong>100% test pass rate</strong> enforced by CI — merges blocked on any failure</li>
              <li><strong>Full test suite in under 15 minutes</strong> (Phase 6 optimization: 33% faster than baseline)</li>
              <li><strong>6 test execution modes</strong> from 30-second unit tests to comprehensive 15-minute full suite</li>
              <li><strong>Contract testing</strong> via Pact between frontend and backend services</li>
              <li><strong>Entity-migration validation</strong> catches schema drift at test time, not runtime</li>
            </ul>
          </div>
        </section>

        {/* Performance Metrics */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Performance Metrics</h2>

          <h3 className={styles.sectionSubtitle}>Performance Characteristics</h3>
          <p className={styles.sectionBody}>
            Benchmarked on a local Docker Compose stack (details on the <Link href="/resources/performance" className={styles.link}>Performance Benchmarking</Link> page):
          </p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Metric</th>
                <th>HDIM Measured</th>
                <th>Context</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Single measure evaluation</strong></td>
                <td className={styles.highlight}>85ms avg (cached)</td>
                <td>CQL/FHIR with Redis caching</td>
              </tr>
              <tr>
                <td><strong>52 measures, one patient</strong></td>
                <td className={styles.highlight}>1.8s total</td>
                <td>Parallel CQL execution</td>
              </tr>
              <tr>
                <td><strong>Care gap detection</strong></td>
                <td className={styles.highlight}>Sub-second</td>
                <td>Event-driven, not batch</td>
              </tr>
              <tr>
                <td><strong>P95 under load (100 users)</strong></td>
                <td className={styles.highlight}>220ms</td>
                <td>Concurrent load test</td>
              </tr>
            </tbody>
          </table>
        </section>

        {/* Security and Compliance */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Security and Compliance Metrics</h2>

          <h3 className={styles.sectionSubtitle}>Security and Compliance</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>Authentication:</strong> Gateway trust architecture — JWT validated at gateway, trusted headers propagated to all services</li>
              <li><strong>Authorization:</strong> Role-based access control (RBAC) with @PreAuthorize on every endpoint</li>
              <li><strong>Audit logging:</strong> 100% HTTP call coverage via interceptor, PHI-safe log filtering, session timeout tracking</li>
              <li><strong>Multi-tenant isolation:</strong> Database-level tenant filtering on every query (WHERE tenantId = :tenantId)</li>
              <li><strong>Encryption:</strong> TLS in transit, AES-256-GCM for sensitive credentials at rest</li>
              <li><strong>Vulnerability scanning:</strong> Automated Trivy container scanning on every build</li>
            </ul>
          </div>
          <p className={styles.sectionBody}>
            HDIM is designed for HIPAA compliance and SOC 2 Type II readiness. Formal certification requires third-party audit, which is planned as part of the first customer deployment.
          </p>
        </section>

        {/* Success Metrics Summary */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Success Metrics Summary</h2>

          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>12x</div>
              <div className={styles.metricLabel}>Faster Time-to-Market</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>85-95%</div>
              <div className={styles.metricLabel}>Cost Reduction</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>1</div>
              <div className={styles.metricLabel}>Architect (vs 10-14)</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Services Delivered</div>
            </div>
          </div>

          <h3 className={styles.sectionSubtitle}>Speed</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>Timeline:</strong> 12x faster (6 weeks vs 12-18 months)</li>
              <li><strong>Velocity:</strong> 10x faster development</li>
              <li><strong>Feature Development:</strong> 90% faster</li>
              <li><strong>Service Implementation:</strong> 80-90% faster</li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>Quality</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>613+ automated tests</strong> with 100% pass rate enforced by CI</li>
              <li><strong>157 OpenAPI-documented endpoints</strong> with interactive Swagger UI</li>
              <li><strong>100% Liquibase rollback coverage</strong> (199/199 changesets)</li>
              <li><strong>HIPAA compliance built in</strong> from specifications, not retrofitted</li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>Scale</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>51+ microservices</strong> covering the full quality measurement lifecycle</li>
              <li><strong>411K lines of Java</strong> across 2,064 source files</li>
              <li><strong>515 test files</strong> with unit, integration, contract, and migration tests</li>
              <li><strong>1,400+ documentation files</strong> including ADRs, compliance evidence, and API specs</li>
            </ul>
          </div>
        </section>

        {/* Conclusion */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Conclusion</h2>
          <p className={styles.sectionBody}>
            These are measured outcomes from one project, not theoretical projections. The results suggest that spec-driven AI development can meaningfully compress delivery timelines and reduce costs for domain experts who know exactly what to build.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>12x faster time-to-market</strong> — 6 weeks vs 12-18 months</li>
              <li><strong>85-95% cost reduction</strong> — under $200K vs $1.5M-$3M (initial build)</li>
              <li><strong>51+ services</strong> with consistent architecture enforced by specifications</li>
              <li><strong>613+ automated tests</strong> with CI-enforced pass rate</li>
            </ul>
          </div>
          <p className={styles.sectionBody}>
            <strong>
              The key requirement:</strong> deep domain expertise. AI amplifies what you know — it does not replace the knowledge needed to write correct specifications.
          </p>
          <p className={styles.subText}>
            <em>AI Solutioning Metrics -- January 2026</em>
          </p>
        </section>

        {/* CTA */}
        <section className={styles.ctaSection}>
          <h2>Explore the Journey</h2>
          <p>See how these metrics were achieved through spec-driven development.</p>
          <div className={styles.ctaButtons}>
            <Link href="/resources/executive-summary" className={styles.btnPrimary}>
              Read the Executive Summary
            </Link>
            <Link href="/resources" className={styles.btnSecondary}>
              Back to Resources
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}
