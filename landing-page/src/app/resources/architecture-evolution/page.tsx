import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Architecture Evolution Timeline | HDIM Resources',
  description: 'From Node.js prototype to production Java platform. 7 phases of architecture evolution with metrics at each stage.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/architecture-evolution' },
};

export default function ArchitectureEvolutionPage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>Architecture Evolution Timeline</h1>
        <p className={styles.heroSubtitle}>
          From Node.js prototype to 51-service production platform in 7 phases.
        </p>
      </div>

      <div className={styles.container}>
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            HDIM did not emerge fully formed. It evolved through deliberate architectural
            phases, each solving a specific class of problem that the previous phase
            exposed. This timeline documents what changed at each phase, why the change
            was necessary, and what metrics improved as a result.
          </p>
        </div>

        {/* Phase Timeline */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Evolution Phases</h2>

          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phase 0</div>
              <div>
                <div className={styles.timelineTitle}>Initial Architecture (Node.js Prototype)</div>
                <p className={styles.sectionBody}>
                  The original HDIM prototype was a Node.js application with Express
                  endpoints, a single PostgreSQL database, and basic FHIR resource
                  handling. It validated the core hypothesis: healthcare organizations
                  need a unified platform for quality measure evaluation and care gap
                  detection. However, it lacked the type safety, ecosystem maturity, and
                  enterprise patterns required for production healthcare workloads.
                </p>
                <div className={styles.keyPoints}>
                  <ul>
                    <li>Single-service monolith handling all domains</li>
                    <li>No tenant isolation — single-tenant design</li>
                    <li>Manual FHIR resource parsing without HAPI library</li>
                    <li>No event-driven architecture</li>
                  </ul>
                </div>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phase 1</div>
              <div>
                <div className={styles.timelineTitle}>Java Rebuild &amp; Database Standardization</div>
                <p className={styles.sectionBody}>
                  Complete platform rewrite in Java 21 with Spring Boot 3.x. Introduced
                  Liquibase as the sole migration tool, replacing ad-hoc schema management.
                  Every service received its own database with independent schema lifecycle.
                  Hibernate <code className={styles.code}>ddl-auto: validate</code> mode
                  enforced entity-migration synchronization.
                </p>
                <div className={styles.metricsGrid}>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>29</div>
                    <div className={styles.metricLabel}>Independent Databases</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>199</div>
                    <div className={styles.metricLabel}>Liquibase Changesets</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>100%</div>
                    <div className={styles.metricLabel}>Rollback Coverage</div>
                  </div>
                </div>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phase 2</div>
              <div>
                <div className={styles.timelineTitle}>Gateway Trust Architecture</div>
                <p className={styles.sectionBody}>
                  Introduced the gateway-trust authentication pattern. The API gateway
                  validates JWT tokens and injects trusted headers (<code className={styles.code}>X-Auth-User</code>,
                  <code className={styles.code}>X-Auth-Roles</code>, <code className={styles.code}>X-Tenant-ID</code>).
                  Downstream services trust these headers, eliminating redundant token
                  validation. Four specialized gateways were deployed: public, clinical,
                  admin, and data ingestion.
                </p>
                <div className={styles.keyPoints}>
                  <ul>
                    <li>4 specialized gateways with distinct security profiles</li>
                    <li>Gateway-core shared module for common authentication logic</li>
                    <li>RBAC with 5 role tiers (SUPER_ADMIN through VIEWER)</li>
                    <li>Multi-tenant isolation enforced at database query level</li>
                  </ul>
                </div>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phase 3</div>
              <div>
                <div className={styles.timelineTitle}>Distributed Tracing (OpenTelemetry)</div>
                <p className={styles.sectionBody}>
                  Deployed OpenTelemetry across all services for end-to-end request
                  visibility. Trace propagation was configured for HTTP (Feign and
                  RestTemplate), Kafka producers/consumers, and Redis operations. Custom
                  spans were added for business-critical operations like CQL measure
                  evaluation and FHIR bundle processing.
                </p>
                <div className={styles.metricsGrid}>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>51+</div>
                    <div className={styles.metricLabel}>Traced Services</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>3</div>
                    <div className={styles.metricLabel}>Transport Protocols</div>
                  </div>
                </div>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phase 4</div>
              <div>
                <div className={styles.timelineTitle}>Event Sourcing &amp; CQRS</div>
                <p className={styles.sectionBody}>
                  Introduced event-driven architecture with Apache Kafka. Four dedicated
                  event services were built: patient-event-service, care-gap-event-service,
                  evaluation-event-service, and quality-measure-event-service. CQRS
                  pattern separated read and write models for high-throughput clinical
                  data processing.
                </p>
                <div className={styles.keyPoints}>
                  <ul>
                    <li>4 event services with dedicated Kafka topics</li>
                    <li>Event projections for materialized views</li>
                    <li>Dead letter queue handling for failed events</li>
                    <li>Tenant-scoped event partitioning</li>
                  </ul>
                </div>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phases 5-6</div>
              <div>
                <div className={styles.timelineTitle}>Test Infrastructure Modernization</div>
                <p className={styles.sectionBody}>
                  Phase 5 migrated from Docker-dependent Testcontainers to Spring embedded
                  Kafka, cutting test execution time by 50%. Phase 6 introduced parallel
                  test execution with six JVM forks, the TestEventWaiter utility for
                  deterministic event synchronization, and six test execution modes for
                  different development contexts.
                </p>
                <div className={styles.metricsGrid}>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>613+</div>
                    <div className={styles.metricLabel}>Total Tests</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>33%</div>
                    <div className={styles.metricLabel}>Faster Test Suite</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>90%</div>
                    <div className={styles.metricLabel}>Sleep Reduction</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>6</div>
                    <div className={styles.metricLabel}>Test Modes</div>
                  </div>
                </div>
              </div>
            </div>

            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Phase 7</div>
              <div>
                <div className={styles.timelineTitle}>CI/CD Parallelization</div>
                <p className={styles.sectionBody}>
                  The final infrastructure phase parallelized the CI/CD pipeline with
                  intelligent change detection. Twenty-one service-specific filters ensure
                  only affected tests run on each PR. Four parallel test jobs and three
                  parallel validation jobs reduced PR feedback time by 42.5%. Docs-only
                  PRs complete in under one minute.
                </p>
                <div className={styles.metricsGrid}>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>42.5%</div>
                    <div className={styles.metricLabel}>Faster PR Feedback</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>21</div>
                    <div className={styles.metricLabel}>Change Detection Filters</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>7</div>
                    <div className={styles.metricLabel}>Parallel CI Jobs</div>
                  </div>
                  <div className={styles.metricCard}>
                    <div className={styles.metricValue}>359</div>
                    <div className={styles.metricLabel}>Hours Saved Annually</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Cumulative Impact */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Cumulative Impact</h2>
          <p className={styles.sectionBody}>
            Each phase compounded the improvements of the previous phase. The combination
            of standardized databases, gateway trust, distributed tracing, event sourcing,
            and optimized CI/CD creates a platform where new services can be added with
            confidence and deployed without risk.
          </p>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Production Services</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>157</div>
              <div className={styles.metricLabel}>Documented API Endpoints</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>90%+</div>
              <div className={styles.metricLabel}>CI/CD Speed Improvement</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>7</div>
              <div className={styles.metricLabel}>Architecture Phases</div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>Dive Deeper</h2>
          <p>Explore the technical architecture or understand how specifications drove each phase.</p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/resources/architecture">Platform Architecture</Link>
            <Link className={styles.btnSecondary} href="/resources">All Resources</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
