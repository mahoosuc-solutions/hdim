import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Spec-Driven Development Analysis | HDIM Resources',
  description: 'How architectural specifications enabled AI solutioning. Specification structure, AI workflow, success factors, and methodology comparison.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/spec-driven-development' },
};

export default function SpecDrivenDevelopmentPage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>Spec-Driven Development Analysis</h1>
        <p className={styles.heroSubtitle}>
          Specifications are the product. Code is the derivative.
        </p>
      </div>

      <div className={styles.container}>
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            The core insight behind HDIM&apos;s delivery velocity is that AI assistants
            produce dramatically better output when given complete architectural
            specifications rather than ad-hoc prompts. This page documents the
            specification structure, the AI workflow it enables, and the success factors
            that made it work for a 51-service healthcare platform.
          </p>
        </div>

        {/* Specification Structure */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Specification Structure</h2>
          <p className={styles.sectionBody}>
            Every HDIM service was generated from a specification document containing six
            mandatory sections. This structure was designed to provide AI assistants with
            complete context while fitting within context window limits.
          </p>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>1. Purpose &amp; Scope</h3>
            <p className={styles.cardBody}>
              Defines what the service does, which domain it belongs to, and its boundaries.
              Includes the service name, port assignment, database name, and upstream/downstream
              dependencies. This section prevents AI from generating features outside the
              service&apos;s responsibility.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>2. API Contracts</h3>
            <p className={styles.cardBody}>
              Complete REST endpoint definitions including HTTP method, path, request/response
              schemas, status codes, and required headers. Specifies <code className={styles.code}>X-Tenant-ID</code> and
              authentication requirements per endpoint. AI generates controller classes,
              request validation, and OpenAPI annotations directly from these contracts.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>3. Data Models</h3>
            <p className={styles.cardBody}>
              Entity definitions with column names, types, constraints, and relationships.
              Includes the Liquibase migration structure so database schema and JPA entities
              are generated in lockstep. Specifies tenant isolation columns and audit fields
              (<code className={styles.code}>created_at</code>, <code className={styles.code}>updated_at</code>, <code className={styles.code}>tenant_id</code>).
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>4. Integration Points</h3>
            <p className={styles.cardBody}>
              Defines how the service communicates with other services: Feign client
              interfaces, Kafka topic names and event schemas, Redis cache keys and TTL
              values. This section is critical for cross-service consistency. When two
              services share a Kafka topic, both specifications reference the same topic
              name and event schema.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>5. Security Requirements</h3>
            <p className={styles.cardBody}>
              Role-based access control matrix per endpoint. Specifies which roles
              (SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER) can access each operation.
              Includes HIPAA-specific requirements: PHI cache TTL limits, audit logging
              directives, and no-cache response headers for sensitive endpoints.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>6. Testing Requirements</h3>
            <p className={styles.cardBody}>
              Defines required test categories: unit tests for service methods, integration
              tests for API endpoints, entity-migration validation tests, multi-tenant
              isolation tests, and RBAC permission tests. Includes specific test scenarios
              that must pass before the service is considered complete.
            </p>
          </div>
        </div>

        {/* AI Workflow */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>AI Assistant Workflow</h2>
          <p className={styles.sectionBody}>
            The specification structure enables a four-step workflow that produces
            production-quality code with minimal manual intervention.
          </p>

          <div className={styles.timeline}>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Step 1</div>
              <div>
                <div className={styles.timelineTitle}>Read Specification</div>
                <p className={styles.sectionBody}>
                  The AI assistant receives the complete specification document. The six-section
                  structure fits within context windows while providing full architectural
                  context. The AI understands not just what to build, but how it fits into
                  the broader system.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Step 2</div>
              <div>
                <div className={styles.timelineTitle}>Generate Artifacts</div>
                <p className={styles.sectionBody}>
                  From a single specification, the AI generates: JPA entities, Liquibase
                  migrations, repository interfaces, service classes, REST controllers,
                  Feign clients, Kafka producers/consumers, configuration files, and test
                  classes. All artifacts conform to the patterns defined in the spec.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Step 3</div>
              <div>
                <div className={styles.timelineTitle}>Architect Review</div>
                <p className={styles.sectionBody}>
                  The architect reviews generated code against the specification. Review
                  focuses on architectural conformance, not syntactic correctness. Common
                  review items: cross-service contract alignment, security annotation
                  completeness, and migration rollback coverage.
                </p>
              </div>
            </div>
            <div className={styles.timelineItem}>
              <div className={styles.timelineDate}>Step 4</div>
              <div>
                <div className={styles.timelineTitle}>Iterate on Specification</div>
                <p className={styles.sectionBody}>
                  When review reveals gaps, the architect updates the specification rather
                  than manually editing generated code. The AI regenerates from the updated
                  spec. This keeps the specification as the single source of truth and
                  prevents spec-code drift.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Success Factors */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Success Factors</h2>
          <p className={styles.sectionBody}>
            Spec-driven development with AI is not automatic. Several factors determined
            whether the approach produced maintainable production code or generated
            plausible-looking artifacts that failed under real conditions.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>Specification completeness matters more than specification length. A concise spec with all six sections outperforms a verbose spec missing security or testing requirements.</li>
              <li>Cross-service contracts must be synchronized. When the patient-service spec defines an event schema, the care-gap-service spec must reference the identical schema.</li>
              <li>The architect must understand the generated code deeply enough to review it. Spec-driven development does not eliminate the need for engineering expertise. It amplifies it.</li>
              <li>Migration files must never be regenerated. Once a Liquibase migration is committed, it is immutable. New changes require new migration files. This was enforced by convention and CI validation.</li>
              <li>Test requirements in specs must be specific. Saying &quot;add unit tests&quot; produces generic tests. Saying &quot;test that tenant A cannot access tenant B&apos;s patients&quot; produces meaningful validation.</li>
              <li>Context window management is a skill. Specifications were structured to provide maximum architectural context within token limits, with cross-references to related specs rather than inline duplication.</li>
            </ul>
          </div>
        </div>

        {/* Methodology Comparison */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Methodology Comparison</h2>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Dimension</th>
                <th>Prompt-Driven</th>
                <th>Spec-Driven</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Input to AI</td>
                <td>Natural language prompt</td>
                <td className={styles.highlight}>Structured specification document</td>
              </tr>
              <tr>
                <td>Output scope</td>
                <td>Single file or function</td>
                <td className={styles.highlight}>Complete service (10-20 files)</td>
              </tr>
              <tr>
                <td>Architectural coherence</td>
                <td>Depends on prompt quality</td>
                <td className={styles.highlight}>Enforced by spec structure</td>
              </tr>
              <tr>
                <td>Reproducibility</td>
                <td>Low — prompts are informal</td>
                <td className={styles.highlight}>High — specs are versioned documents</td>
              </tr>
              <tr>
                <td>Review efficiency</td>
                <td>Line-by-line code review</td>
                <td className={styles.highlight}>Spec-conformance review</td>
              </tr>
              <tr>
                <td>Iteration cost</td>
                <td>Manual editing of generated code</td>
                <td className={styles.highlight}>Update spec, regenerate</td>
              </tr>
              <tr>
                <td>Cross-service consistency</td>
                <td>Manual coordination</td>
                <td className={styles.highlight}>Shared contract definitions</td>
              </tr>
              <tr>
                <td>Onboarding value</td>
                <td>Prompts are ephemeral</td>
                <td className={styles.highlight}>Specs serve as living documentation</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Metrics */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Results by the Numbers</h2>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>6</div>
              <div className={styles.metricLabel}>Spec Sections per Service</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Services Generated</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>157</div>
              <div className={styles.metricLabel}>Documented Endpoints</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>29</div>
              <div className={styles.metricLabel}>Independent Database Schemas</div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>See How It Compares</h2>
          <p>Explore the methodology differences and real-world delivery outcomes.</p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/resources">All Resources</Link>
            <Link className={styles.btnSecondary} href="/resources/ai-comparison">AI-Native Comparison</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
