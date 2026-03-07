import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Vision Deck | HDIM Resources',
  description: 'HDIM platform vision and roadmap. The age of AI solutioning for healthcare interoperability.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/vision' },
};

export default function VisionDeckPage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>The Age of AI Solutioning</h1>
        <p className={styles.heroSubtitle}>
          Healthcare IT has a delivery problem. Spec-driven AI development is the answer.
        </p>
      </div>

      <div className={styles.container}>
        {/* The Problem */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>The Problem</h2>
          <p className={styles.sectionBody}>
            Healthcare IT delivery is broken. The numbers tell the story clearly:
            enterprise healthcare platforms take 12-24 months to reach MVP. They require
            teams of 8-15 engineers. They cost $1.5M-$3M annually. And after all that
            investment, most deliver 5-10 services in year one with inconsistent quality
            and incomplete compliance evidence.
          </p>
          <p className={styles.sectionBody}>
            The root cause is not engineering talent. It is the operating model. Traditional
            software development was designed for an era when writing code was the bottleneck.
            Today, the bottleneck is architectural coherence across dozens of services,
            compliance validation across hundreds of endpoints, and consistency enforcement
            across thousands of database queries. Writing code is fast. Keeping code
            architecturally coherent is slow.
          </p>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>18mo</div>
              <div className={styles.metricLabel}>Typical Time to MVP</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>$2M+</div>
              <div className={styles.metricLabel}>Annual Cost</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>30-40%</div>
              <div className={styles.metricLabel}>Sprint Capacity Lost to Rework</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>5-10</div>
              <div className={styles.metricLabel}>Services Delivered Year One</div>
            </div>
          </div>
        </div>

        {/* The Solution */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>The Solution</h2>
          <p className={styles.sectionBody}>
            AI solutioning replaces the traditional operating model with a specification-first
            approach. The architect writes complete architectural specifications covering
            API contracts, data models, security requirements, and test criteria. AI
            assistants generate production-quality code from these specifications. The
            architect reviews for conformance, not correctness.
          </p>
          <p className={styles.sectionBody}>
            This is not &quot;AI-assisted coding&quot; — the incremental use of copilot
            tools to complete individual functions. This is a fundamentally different
            operating model where specifications are the product and code is a derived
            artifact. The shift is analogous to the transition from hand-drafting to
            CAD in mechanical engineering: the engineer&apos;s role changes from drawing
            to designing.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>Specifications define what to build. AI generates how to build it. The architect validates that what was generated matches what was specified.</li>
              <li>Cross-service consistency is enforced by shared specification contracts, not by developer discipline across a 12-person team.</li>
              <li>Compliance evidence is generated as a byproduct of the specification process, not assembled retroactively at audit time.</li>
              <li>The architect maintains full architectural control. AI handles the implementation volume. Neither operates effectively without the other.</li>
            </ul>
          </div>
        </div>

        {/* The Proof */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>The Proof</h2>
          <p className={styles.sectionBody}>
            HDIM is the existence proof. A single architect built a production-ready
            healthcare interoperability platform in 6 weeks using spec-driven AI
            solutioning. Every claim is backed by commit history, test results, and
            validation artifacts.
          </p>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Production Services</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>6 wks</div>
              <div className={styles.metricLabel}>Build Time</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>1</div>
              <div className={styles.metricLabel}>Architect</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>613+</div>
              <div className={styles.metricLabel}>Passing Tests</div>
            </div>
          </div>

          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Capability</th>
                <th>Delivered</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>FHIR R4 resource management</td>
                <td className={styles.highlight}>HAPI FHIR 7.x with validation</td>
              </tr>
              <tr>
                <td>CQL quality measure evaluation</td>
                <td className={styles.highlight}>HEDIS measure engine</td>
              </tr>
              <tr>
                <td>Care gap detection and tracking</td>
                <td className={styles.highlight}>Full lifecycle management</td>
              </tr>
              <tr>
                <td>Multi-tenant isolation</td>
                <td className={styles.highlight}>Database-level enforcement</td>
              </tr>
              <tr>
                <td>HIPAA-compliant architecture</td>
                <td className={styles.highlight}>Audit logging, PHI controls, encryption</td>
              </tr>
              <tr>
                <td>Event-driven architecture</td>
                <td className={styles.highlight}>Kafka CQRS with 4 event services</td>
              </tr>
              <tr>
                <td>API documentation</td>
                <td className={styles.highlight}>157 endpoints, OpenAPI 3.0, Swagger UI</td>
              </tr>
              <tr>
                <td>Distributed tracing</td>
                <td className={styles.highlight}>OpenTelemetry across all services</td>
              </tr>
              <tr>
                <td>Database migrations</td>
                <td className={styles.highlight}>199 changesets, 100% rollback coverage</td>
              </tr>
              <tr>
                <td>CI/CD pipeline</td>
                <td className={styles.highlight}>Parallelized, 42.5% faster feedback</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* The Personas */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Who This Serves</h2>
          <p className={styles.sectionBody}>
            HDIM addresses distinct needs across three organizational personas.
          </p>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Executive Persona</h3>
            <p className={styles.cardBody}>
              CIOs and VPs of Engineering evaluating build-vs-buy decisions for quality
              measurement infrastructure. Key concern: time-to-value and total cost of
              ownership. HDIM demonstrates that a production-ready platform can be delivered
              at 10% of traditional cost in 10% of traditional time. The specification
              artifacts provide audit-ready evidence for board presentations and regulatory
              submissions.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Clinical Persona</h3>
            <p className={styles.cardBody}>
              Quality Directors and Clinical Informatics leads responsible for HEDIS
              reporting, Stars ratings improvement, and care gap closure rates. Key
              concern: clinical accuracy and workflow integration. HDIM provides CQL-based
              measure evaluation with FHIR R4 data, producing clinically validated care
              gap lists that integrate with existing care management workflows.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Technical Persona</h3>
            <p className={styles.cardBody}>
              Enterprise Architects and Principal Engineers evaluating platform capabilities
              and integration feasibility. Key concern: architectural quality, scalability,
              and maintainability. HDIM provides full source access, 157 documented
              endpoints with interactive Swagger UI, comprehensive test coverage, and
              a clear architecture evolution path documented through 7 phases.
            </p>
          </div>
        </div>

        {/* The Path Forward */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>The Path Forward</h2>
          <p className={styles.sectionBody}>
            HDIM is a platform, not a finished product. The roadmap extends the core
            capabilities into adjacent healthcare domains while deepening the AI
            solutioning methodology.
          </p>

          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Q1 2026</div>
              <div>
                <div className={styles.timelineTitle}>Platform Foundation</div>
                <p className={styles.sectionBody}>
                  51+ services production-ready. FHIR R4, CQL evaluation, care gap
                  detection, event sourcing, and full observability stack deployed.
                  API documentation complete for 157 endpoints. CI/CD pipeline optimized
                  with 42.5% faster feedback.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Q2 2026</div>
              <div>
                <div className={styles.timelineTitle}>Pilot Deployments</div>
                <p className={styles.sectionBody}>
                  First payer and ACO pilot customers. EHR integration adapters for Epic,
                  Cerner, and InterSystems. Clinical workflow validation with real patient
                  populations. HITRUST readiness assessment.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Q3 2026</div>
              <div>
                <div className={styles.timelineTitle}>Scale and Optimize</div>
                <p className={styles.sectionBody}>
                  Population health processing at scale. Advanced risk stratification
                  models. Real-time care gap alerting. Multi-region deployment capability.
                  SOC 2 Type II audit initiation.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Q4 2026</div>
              <div>
                <div className={styles.timelineTitle}>Market Expansion</div>
                <p className={styles.sectionBody}>
                  Self-service tenant onboarding. Expanded measure library beyond HEDIS.
                  AI-powered care gap prioritization. Partner ecosystem for clinical
                  content and workflow integration.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>See It in Action</h2>
          <p>
            Schedule a technical demo to see the platform architecture, FHIR processing
            pipeline, and quality measure evaluation workflow.
          </p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/about">Schedule a Demo</Link>
            <Link className={styles.btnSecondary} href="/resources">Explore Resources</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
