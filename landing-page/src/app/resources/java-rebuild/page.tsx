import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Java Rebuild Deep Dive | HDIM Resources',
  description: 'Why Java was chosen over Node.js and how the rebuild succeeded. Technical deep-dive on language selection, architecture decisions, and AI assistance patterns.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/java-rebuild' },
};

export default function JavaRebuildPage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>Java Rebuild Deep Dive</h1>
        <p className={styles.heroSubtitle}>
          Why we rewrote a working Node.js prototype in Java. And why it was the right call.
        </p>
      </div>

      <div className={styles.container}>
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            Rewriting a working system is one of the most contentious decisions in software
            engineering. The Node.js prototype validated the HDIM concept. The Java rebuild
            made it production-ready for healthcare. This page documents the reasoning,
            the execution, and the patterns that made a full-platform rewrite viable in
            weeks rather than months.
          </p>
        </div>

        {/* Why Java */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Why Java</h2>
          <p className={styles.sectionBody}>
            The decision was driven by four factors, each independently sufficient to
            justify the rewrite.
          </p>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>1. Healthcare Ecosystem Alignment</h3>
            <p className={styles.cardBody}>
              HAPI FHIR, the gold-standard open-source FHIR implementation, is a Java
              library. CQL evaluation engines are Java-based. HL7 reference implementations
              are Java. The healthcare interoperability ecosystem is overwhelmingly JVM.
              Building in Node.js meant reimplementing or wrapping every critical dependency
              with FFI bridges, losing type safety and library-level validation at each
              boundary.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>2. Enterprise Hiring and Skills Market</h3>
            <p className={styles.cardBody}>
              Healthcare IT organizations hire Java engineers. Payer technology teams,
              health system integration departments, and EHR vendors maintain Java codebases.
              Building HDIM in Java means the engineers who will eventually maintain and
              extend it already exist in the target market. A Node.js healthcare platform
              would require hiring from a smaller, less domain-experienced talent pool.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>3. AI Code Generation Effectiveness</h3>
            <p className={styles.cardBody}>
              Java&apos;s explicit type system, annotation-driven configuration, and
              well-established patterns (Spring Boot, JPA, Liquibase) make AI-generated
              code significantly more reliable. The compiler catches type errors that
              would be runtime failures in Node.js. Spring Boot conventions mean AI
              assistants produce consistent, idiomatic code because the &quot;right way&quot;
              is well-defined and heavily represented in training data.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>4. Long-Term Maintainability</h3>
            <p className={styles.cardBody}>
              Healthcare platforms have 10-15 year lifecycles. Java 21 LTS provides a
              stable foundation with predictable upgrade paths. Spring Boot&apos;s backwards
              compatibility track record, Gradle&apos;s build reproducibility, and the
              JVM&apos;s performance characteristics at scale all favor long-term
              operational stability over rapid prototyping convenience.
            </p>
          </div>
        </div>

        {/* Preserved Concepts */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Preserved from Node.js</h2>
          <p className={styles.sectionBody}>
            The rewrite was not a rejection of the prototype. Several architectural
            concepts proved their value in Node.js and were carried forward.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>Domain model boundaries: patient, care gap, quality measure, and evaluation domains remained the core service decomposition.</li>
              <li>FHIR R4 as the canonical data format for clinical resources.</li>
              <li>REST-first API design with versioned endpoints and consistent error response shapes.</li>
              <li>Tenant isolation as a first-class architectural concern, not a retrofit.</li>
              <li>Event-driven notifications for cross-service state changes.</li>
              <li>Health check endpoints and readiness probes on every service.</li>
            </ul>
          </div>
        </div>

        {/* Redesigned Components */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Redesigned Components</h2>
          <p className={styles.sectionBody}>
            Other aspects of the architecture were fundamentally redesigned to take
            advantage of Java ecosystem capabilities.
          </p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Component</th>
                <th>Node.js Prototype</th>
                <th>Java Platform</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>FHIR handling</td>
                <td>Manual JSON parsing</td>
                <td className={styles.highlight}>HAPI FHIR 7.x with validation</td>
              </tr>
              <tr>
                <td>Database migrations</td>
                <td>Knex.js migrations</td>
                <td className={styles.highlight}>Liquibase with rollback coverage</td>
              </tr>
              <tr>
                <td>Authentication</td>
                <td>Passport.js middleware</td>
                <td className={styles.highlight}>Spring Security + gateway trust</td>
              </tr>
              <tr>
                <td>Event processing</td>
                <td>Bull queues (Redis-backed)</td>
                <td className={styles.highlight}>Kafka with CQRS event sourcing</td>
              </tr>
              <tr>
                <td>API documentation</td>
                <td>Manual Swagger files</td>
                <td className={styles.highlight}>SpringDoc OpenAPI annotations</td>
              </tr>
              <tr>
                <td>Test infrastructure</td>
                <td>Jest + Supertest</td>
                <td className={styles.highlight}>JUnit 5 + MockMvc + embedded Kafka</td>
              </tr>
              <tr>
                <td>Caching</td>
                <td>Redis with manual TTL</td>
                <td className={styles.highlight}>Spring Cache + HIPAA-compliant TTL</td>
              </tr>
              <tr>
                <td>Service communication</td>
                <td>Axios HTTP calls</td>
                <td className={styles.highlight}>Spring Cloud Feign with retry</td>
              </tr>
              <tr>
                <td>Observability</td>
                <td>Winston logging</td>
                <td className={styles.highlight}>OpenTelemetry + Prometheus + Grafana</td>
              </tr>
              <tr>
                <td>Build system</td>
                <td>npm scripts</td>
                <td className={styles.highlight}>Gradle Kotlin DSL with parallel tasks</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Architecture Decisions */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Key Architecture Decisions</h2>
          <p className={styles.sectionBody}>
            The rebuild was guided by Architecture Decision Records (ADRs) that documented
            the rationale for each major choice.
          </p>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Database Per Service</h3>
            <p className={styles.cardBody}>
              Each of the 29 databases has its own schema lifecycle managed by Liquibase.
              This eliminates cross-service schema coupling and enables independent
              deployment. The tradeoff is operational complexity in database management,
              mitigated by standardized Docker Compose configurations and automated
              migration validation.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Gateway Trust Over Token Forwarding</h3>
            <p className={styles.cardBody}>
              Rather than forwarding JWT tokens to every downstream service for independent
              validation, the gateway validates once and injects trusted headers. This
              reduces latency on the critical path and simplifies service-level security
              configuration. The tradeoff is that internal network security becomes
              critical — mitigated by network policies and mTLS.
            </p>
          </div>

          <div className={styles.card}>
            <h3 className={styles.cardTitle}>Embedded Kafka for Testing</h3>
            <p className={styles.cardBody}>
              The decision to use Spring embedded Kafka instead of Testcontainers
              eliminated Docker daemon dependency for test execution. This enabled
              CI/CD parallelization and reduced test suite execution from 60 minutes
              to 10-15 minutes. Tests run identically on developer laptops and CI servers.
            </p>
          </div>
        </div>

        {/* AI Assistance Patterns */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>AI Assistance Patterns</h2>
          <p className={styles.sectionBody}>
            The Java rebuild was executed using spec-driven AI assistance. Several patterns
            emerged that were specific to Java and Spring Boot code generation.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>Spring Boot conventions made AI output highly predictable. Controller-service-repository layering, annotation-driven configuration, and property file conventions are well-represented in training data.</li>
              <li>JPA entity generation from data model specs was nearly zero-defect. Column types, constraints, and relationships mapped cleanly from specification to annotation.</li>
              <li>Liquibase migration generation required the most human oversight. Migration ordering, rollback directives, and cross-service schema references needed architect review.</li>
              <li>Test generation benefited from Java&apos;s type system. Mockito mocks, assertion chains, and test data builders were generated correctly because the type signatures constrain the output space.</li>
              <li>Security annotations were the highest-risk generation target. RBAC matrices in specs translated to @PreAuthorize annotations, but the architect verified every endpoint&apos;s access control independently.</li>
              <li>Kafka consumer/producer code required cross-service specification review. Topic names, serialization formats, and consumer group IDs had to match across independently generated services.</li>
            </ul>
          </div>
        </div>

        {/* Results */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Rebuild Results</h2>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Services Delivered</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>613+</div>
              <div className={styles.metricLabel}>Passing Tests</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>157</div>
              <div className={styles.metricLabel}>API Endpoints Documented</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>6 wks</div>
              <div className={styles.metricLabel}>Total Build Time</div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>See the Architecture</h2>
          <p>Explore the platform architecture and evolution timeline that resulted from this rebuild.</p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/resources/architecture">Platform Architecture</Link>
            <Link className={styles.btnSecondary} href="/resources">All Resources</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
