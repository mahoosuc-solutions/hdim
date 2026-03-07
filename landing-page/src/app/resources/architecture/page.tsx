import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Platform Architecture | HDIM Resources',
  description: 'Overview of the HDIM 51+ microservice architecture. Service layers, data flow, infrastructure, and security architecture.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/architecture' },
};

export default function PlatformArchitecturePage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>Platform Architecture</h1>
        <p className={styles.heroSubtitle}>
          51+ microservices. 4 gateways. 29 databases. One coherent platform for
          healthcare quality measurement and care gap detection.
        </p>
      </div>

      <div className={styles.container}>
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            HDIM is a healthcare interoperability platform built on Java 21 and Spring
            Boot 3.x. It evaluates clinical quality measures (CQL/HEDIS), identifies
            care gaps, performs risk stratification, and generates quality reports for
            value-based care contracts. This page provides a technical overview of the
            platform&apos;s architecture across four layers: service organization, data
            flow, infrastructure, and security.
          </p>
        </div>

        {/* Service Layer Overview */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Service Layer Overview</h2>
          <p className={styles.sectionBody}>
            Services are organized into four categories, each with distinct
            responsibilities and deployment characteristics.
          </p>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>API Gateways (4 services)</h3>
            <p className={styles.cardBody}>
              Four specialized gateways handle traffic routing, JWT validation, and
              trusted header injection. The <strong>public gateway</strong> serves
              unauthenticated endpoints (health checks, FHIR metadata).
              The <strong>clinical gateway</strong> routes authenticated clinical
              operations. The <strong>admin gateway</strong> handles tenant management
              and system configuration. The <strong>data ingestion gateway</strong> manages
              bulk FHIR resource imports with rate limiting and backpressure. All four
              share a <code className={styles.code}>gateway-core</code> module for
              common authentication and routing logic.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Event Services (4 services)</h3>
            <p className={styles.cardBody}>
              Dedicated event services implement CQRS and event sourcing patterns.
              <strong> Patient Event Service</strong> captures patient lifecycle events.
              <strong> Care Gap Event Service</strong> tracks gap status changes.
              <strong> Evaluation Event Service</strong> records measure evaluation
              results. <strong>Quality Measure Event Service</strong> handles measure
              definition updates. Each service publishes to Kafka topics and maintains
              materialized views for read-optimized queries.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Domain Services (30+ services)</h3>
            <p className={styles.cardBody}>
              Core business logic resides in domain services organized by bounded context.
              Key services include: <strong>Patient Service</strong> (demographics,
              health records), <strong>Care Gap Service</strong> (gap detection, closure
              tracking), <strong>Quality Measure Service</strong> (HEDIS measure
              definitions), <strong>CQL Engine</strong> (Clinical Quality Language
              evaluation), <strong>FHIR Service</strong> (R4 resource management),
              <strong> Risk Stratification Service</strong> (population health scoring),
              and <strong>Reporting Service</strong> (quality report generation).
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Shared Modules (6+ modules)</h3>
            <p className={styles.cardBody}>
              Cross-cutting concerns are encapsulated in shared Gradle modules.
              <strong> gateway-core</strong> provides authentication filters and
              routing utilities. <strong>domain-common</strong> defines shared value
              objects and tenant context. <strong>contract-testing</strong> provides Pact
              test infrastructure. <strong>openapi-validation</strong> validates API
              compliance. <strong>event-common</strong> standardizes Kafka event schemas.
              <strong> test-common</strong> provides shared test utilities including
              TestEventWaiter.
            </p>
          </div>
        </div>

        {/* Data Flow */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Data Flow</h2>
          <p className={styles.sectionBody}>
            Clinical data flows through four stages, from ingestion to actionable
            care gap detection.
          </p>

          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Stage 1</div>
              <div>
                <div className={styles.timelineTitle}>FHIR Ingestion</div>
                <p className={styles.sectionBody}>
                  Clinical data enters the platform as FHIR R4 resources through the data
                  ingestion gateway. Supported formats include individual FHIR resources,
                  FHIR Bundles, and NDJSON bulk imports. HAPI FHIR validates resource
                  structure and profiles. Valid resources are persisted and published to
                  Kafka for downstream processing.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Stage 2</div>
              <div>
                <div className={styles.timelineTitle}>Event Processing</div>
                <p className={styles.sectionBody}>
                  Event services consume ingestion events and maintain materialized views.
                  Patient demographics are denormalized for fast lookup. Clinical resource
                  indexes are updated. Event projections create read-optimized data
                  structures for the evaluation engine.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Stage 3</div>
              <div>
                <div className={styles.timelineTitle}>Quality Evaluation</div>
                <p className={styles.sectionBody}>
                  The CQL Engine evaluates clinical quality measures against patient data.
                  HEDIS measure definitions specify the clinical logic. The engine processes
                  patient populations and produces measure-level and patient-level results.
                  Evaluation events are published for downstream consumption.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Stage 4</div>
              <div>
                <div className={styles.timelineTitle}>Care Gap Detection</div>
                <p className={styles.sectionBody}>
                  Evaluation results feed the Care Gap Service, which identifies patients
                  with unmet quality measure criteria. Gaps are tracked through their
                  lifecycle: open, addressed, closed, and excluded. Risk stratification
                  scores prioritize outreach. Reports are generated for quality submission
                  and operational dashboards.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Infrastructure */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Infrastructure</h2>
          <p className={styles.sectionBody}>
            The platform runs on four core infrastructure components, each selected
            for healthcare workload characteristics.
          </p>

          <div className={styles.panelGrid}>
            <div className={styles.card}>
              <h3 className={styles.cardTitle}>PostgreSQL 16</h3>
              <p className={styles.cardBody}>
                Primary data store with 29 independent databases. Each service owns its
                schema lifecycle via Liquibase. 199 changesets with 100% rollback coverage.
                Tenant isolation enforced at query level with
                <code className={styles.code}> WHERE tenant_id = :tenantId</code> on every query.
              </p>
            </div>
            <div className={styles.card}>
              <h3 className={styles.cardTitle}>Redis 7</h3>
              <p className={styles.cardBody}>
                Caching layer with HIPAA-compliant TTL enforcement. PHI-containing cache
                entries expire within 5 minutes. Session state, rate limiting counters,
                and OAuth state parameters stored with appropriate TTL policies.
              </p>
            </div>
            <div className={styles.card}>
              <h3 className={styles.cardTitle}>Apache Kafka 3.x</h3>
              <p className={styles.cardBody}>
                Event streaming backbone for CQRS and event sourcing. Tenant-scoped topic
                partitioning ensures data isolation. Dead letter queues capture failed
                events for retry and audit. OpenTelemetry trace context propagated through
                message headers.
              </p>
            </div>
            <div className={styles.card}>
              <h3 className={styles.cardTitle}>Kong API Gateway</h3>
              <p className={styles.cardBody}>
                Production API gateway providing rate limiting, request transformation,
                and upstream load balancing. Configured with declarative YAML for
                reproducible deployments. Health check probes verify upstream service
                availability.
              </p>
            </div>
          </div>
        </div>

        {/* Security Architecture */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Security Architecture</h2>
          <p className={styles.sectionBody}>
            Security is enforced at three layers: gateway, service, and data.
          </p>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Gateway Trust</h3>
            <p className={styles.cardBody}>
              The gateway validates JWT tokens and injects trusted headers:
              <code className={styles.code}> X-Auth-User</code>,
              <code className={styles.code}> X-Auth-Roles</code>, and
              <code className={styles.code}> X-Tenant-ID</code>. Downstream services
              trust these headers via <code className={styles.code}>TrustedHeaderAuthFilter</code>.
              This eliminates redundant token validation and reduces request latency.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Role-Based Access Control</h3>
            <p className={styles.cardBody}>
              Five role tiers provide granular access control. SUPER_ADMIN has full system
              access. ADMIN manages tenant-level configuration. EVALUATOR runs quality
              evaluations. ANALYST views reports. VIEWER has read-only access. Every API
              endpoint is annotated with <code className={styles.code}>@PreAuthorize</code> specifying
              required roles.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Tenant Isolation</h3>
            <p className={styles.cardBody}>
              Multi-tenancy is enforced at the database query level. Every repository
              method filters by <code className={styles.code}>tenantId</code>. Integration
              tests explicitly verify that Tenant A cannot access Tenant B&apos;s data.
              Kafka event partitioning ensures tenant-scoped event processing. Cache keys
              are prefixed with tenant identifiers.
            </p>
          </div>
        </div>

        {/* Monitoring */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Observability Stack</h2>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>OTel</div>
              <div className={styles.metricLabel}>Distributed Tracing</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>Prom</div>
              <div className={styles.metricLabel}>Metrics Collection</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>Grafana</div>
              <div className={styles.metricLabel}>Dashboard UI</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>ECS</div>
              <div className={styles.metricLabel}>Structured Logging</div>
            </div>
          </div>
        </div>

        {/* Link to Evolution */}
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            This architecture was not designed in a single session. It evolved through
            seven deliberate phases, each addressing problems exposed by the previous
            phase.{' '}
            <Link className={styles.link} href="/resources/architecture-evolution">
              Read the Architecture Evolution Timeline
            </Link>{' '}
            for the full story.
          </p>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>Explore the Platform</h2>
          <p>Technical deep-dives, methodology analysis, and build evidence for healthcare technology leaders.</p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/resources">All Resources</Link>
            <Link className={styles.btnSecondary} href="/resources/java-rebuild">Java Rebuild Deep Dive</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
