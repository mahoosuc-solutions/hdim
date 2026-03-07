import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'How HDIM Was Built | Executive Summary | HDIM Resources',
  description:
    'How one architect built an enterprise healthcare platform in 6 weeks using spec-driven development with AI assistants. Timeline, methodology, and results.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/executive-summary' },
};

export default function ExecutiveSummaryPage() {
  return (
    <div className={styles.pageWrapper}>
      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>How HDIM Was Built</h1>
        <p className={styles.heroSubtitle}>
          One architect. Six weeks. A production-ready healthcare quality platform.
        </p>
      </section>

      <div className={styles.container}>
        {/* Headline Metrics */}
        <div className={styles.metricsGrid}>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>6</div>
            <div className={styles.metricLabel}>Weeks to Production</div>
          </div>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>1</div>
            <div className={styles.metricLabel}>Architect</div>
          </div>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>51+</div>
            <div className={styles.metricLabel}>Services Delivered</div>
          </div>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>613+</div>
            <div className={styles.metricLabel}>Automated Tests</div>
          </div>
        </div>

        {/* The Problem */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>The Problem</h2>
          <p className={styles.sectionBody}>
            Healthcare interoperability platforms are slow and expensive to build. Industry benchmarks put enterprise FHIR platforms at 12-24 months with teams of 8-15 engineers and budgets of $1.5M-$3M (based on average senior healthcare engineer compensation of $150K-$200K/year, from Glassdoor/Levels.fyi 2024 data for healthcare IT roles).
          </p>
          <p className={styles.sectionBody}>
            Most of that time goes to coordination overhead, not engineering. Requirements misalignment, rework cycles, and integration debugging consume 30-40% of sprint capacity in traditional teams (Standish Group CHAOS Report, 2020).
          </p>
        </section>

        {/* The Approach */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>The Approach: Spec-Driven AI Development</h2>
          <p className={styles.sectionBody}>
            HDIM was built by a single architect with 20 years of healthcare IT experience. Instead of prompting AI tools ad hoc, every service was generated from a formal specification document defining API contracts, data models, security requirements, and test criteria.
          </p>

          <div className={styles.keyPoints}>
            <ul>
              <li><strong>Specifications, not prompts:</strong> AI executes well-defined contracts, not vague instructions</li>
              <li><strong>Architecture before code:</strong> Service boundaries, event flows, and security patterns designed upfront</li>
              <li><strong>Domain expertise amplified:</strong> The architect makes decisions, AI handles implementation volume</li>
              <li><strong>Quality built in:</strong> Test requirements and HIPAA controls are part of every spec</li>
            </ul>
          </div>
        </section>

        {/* Timeline */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Build Timeline</h2>
          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Oct 2024</div>
              <div>
                <div className={styles.timelineTitle}>Node.js Prototype</div>
                <p className={styles.sectionBody}>
                  Initial prototype built in Node.js/TypeScript. Rapid prototyping worked for early validation, but the healthcare ecosystem (HAPI FHIR, CQL Engine) is Java-native. Complexity outpaced the runtime.
                </p>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Nov 24, 2024</div>
              <div>
                <div className={styles.timelineTitle}>Decision: Rebuild in Java</div>
                <p className={styles.sectionBody}>
                  Healthcare libraries (HAPI FHIR, CQL Engine) are Java-native. Spring Boot provides mature patterns for microservices, security, and compliance. AI assistants generate high-quality Java from well-documented frameworks.
                </p>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Weeks 1-2</div>
              <div>
                <div className={styles.timelineTitle}>Foundation and Core Services</div>
                <p className={styles.sectionBody}>
                  Architecture specification created. AI directed to implement FHIR Service, CQL Engine Service, Quality Measure Service, and Patient Service. Core platform operational.
                </p>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Weeks 3-4</div>
              <div>
                <div className={styles.timelineTitle}>Service Expansion</div>
                <p className={styles.sectionBody}>
                  Care Gap Service, Consent Service, Analytics Service, Gateway Services, and AI Services. Kafka event streaming, RBAC security, and multi-tenant isolation added.
                </p>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Weeks 5-6</div>
              <div>
                <div className={styles.timelineTitle}>Integration and Production Readiness</div>
                <p className={styles.sectionBody}>
                  Cross-service integration testing, contract validation, distributed tracing, CI/CD pipeline optimization, and compliance evidence generation. 51+ services production-ready.
                </p>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Jan 2025 - Present</div>
              <div>
                <div className={styles.timelineTitle}>Architecture Evolution (7 Phases)</div>
                <p className={styles.sectionBody}>
                  Database standardization (100% Liquibase), gateway trust architecture, distributed tracing, event sourcing, test optimization (33% faster), CI/CD parallelization (42.5% faster feedback).
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Comparison */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>How This Compares</h2>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Dimension</th>
                <th>Traditional Approach</th>
                <th>HDIM (AI Solutioning)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Timeline</strong></td>
                <td>12-18 months to MVP</td>
                <td className={styles.highlight}>6 weeks to production-ready</td>
              </tr>
              <tr>
                <td><strong>Team</strong></td>
                <td>8-15 engineers + PM + QA</td>
                <td className={styles.highlight}>1 architect with AI assistants</td>
              </tr>
              <tr>
                <td><strong>Cost (initial build)</strong></td>
                <td>$1.5M-$3M*</td>
                <td className={styles.highlight}>Under $200K*</td>
              </tr>
              <tr>
                <td><strong>Services delivered</strong></td>
                <td>5-10 in year one</td>
                <td className={styles.highlight}>51+ in 6 weeks</td>
              </tr>
              <tr>
                <td><strong>Test coverage</strong></td>
                <td>60-70% (varies by discipline)</td>
                <td className={styles.highlight}>613+ tests, contract + migration validated</td>
              </tr>
              <tr>
                <td><strong>Architecture consistency</strong></td>
                <td>Degrades with team size</td>
                <td className={styles.highlight}>Uniform — spec-enforced patterns</td>
              </tr>
            </tbody>
          </table>
          <p className={styles.subText} style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>
            * Traditional cost based on 10-14 senior healthcare engineers at $150K-$200K/year for 12-18 months (Glassdoor/Levels.fyi 2024). HDIM cost includes architect compensation, AI tool subscriptions, and infrastructure for the build period.
          </p>
        </section>

        {/* Honest Assessment */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>What This Approach Requires</h2>
          <p className={styles.sectionBody}>
            AI solutioning is not a shortcut — it shifts the bottleneck from coding to specification. These are real requirements:
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li><strong>Deep domain expertise:</strong> The architect must know what to specify. 20 years of healthcare IT experience drove the service boundaries, HIPAA patterns, and clinical logic decisions.</li>
              <li><strong>Bus factor risk:</strong> Single-architect builds concentrate knowledge. HDIM mitigates this with 157 documented API endpoints, comprehensive ADRs, and spec-driven onboarding.</li>
              <li><strong>UI/UX limitations:</strong> AI excels at backend services but clinical workflow design still benefits from dedicated UX teams working iteratively with clinicians.</li>
              <li><strong>Compliance muscle memory:</strong> Teams with existing HITRUST or SOC 2 experience carry institutional compliance knowledge that specifications alone cannot fully encode.</li>
            </ul>
          </div>
        </section>

        {/* CTA Section */}
        <section className={styles.ctaSection}>
          <h2>Go Deeper</h2>
          <p>
            Explore the methodology, architecture, or see the side-by-side comparison with traditional development.
          </p>
          <div className={styles.ctaButtons}>
            <Link href="/resources/spec-driven-development" className={styles.btnPrimary}>
              Spec-Driven Methodology
            </Link>
            <Link href="/resources/traditional-comparison" className={styles.btnSecondary}>
              Full Comparison
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}
