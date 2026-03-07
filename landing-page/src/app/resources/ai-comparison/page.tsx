import type { Metadata } from 'next';
import Link from 'next/link';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'AI-Native vs Non-AI-Native Developers | HDIM Resources',
  description: 'How AI-native architects use AI tools differently from non-AI-native developers. Workflow, quality, testing, and velocity compared.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/ai-comparison' },
};

export default function AIComparisonPage() {
  return (
    <div className={styles.pageWrapper}>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>AI-Native vs Non-AI-Native Developers</h1>
        <p className={styles.heroSubtitle}>
          The difference is not whether you use AI. It is how you direct it.
        </p>
      </div>

      <div className={styles.container}>
        <div className={styles.section}>
          <p className={styles.sectionBody}>
            Every engineering team now has access to AI coding assistants. The gap between
            teams that see marginal gains and teams that see transformational results comes
            down to operating model, not tooling. AI-native architects treat AI as a
            specification executor. Non-AI-native developers treat it as an autocomplete
            upgrade.
          </p>
        </div>

        {/* Workflow Differences */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Workflow Differences</h2>
          <p className={styles.sectionBody}>
            The most visible difference is where the work starts. Non-AI-native developers
            open an editor and begin typing code, occasionally asking AI to complete a line
            or generate a function. AI-native architects open a specification document and
            define the entire contract before any code exists.
          </p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Dimension</th>
                <th>Non-AI-Native</th>
                <th>AI-Native</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Starting point</td>
                <td>Blank editor, prompt-driven</td>
                <td className={styles.highlight}>Specification document with contracts</td>
              </tr>
              <tr>
                <td>AI interaction</td>
                <td>Ad-hoc prompts per function</td>
                <td className={styles.highlight}>Structured spec fed to AI in full context</td>
              </tr>
              <tr>
                <td>Iteration cycle</td>
                <td>Write, test, debug, rewrite</td>
                <td className={styles.highlight}>Spec, generate, review, refine spec</td>
              </tr>
              <tr>
                <td>Context window usage</td>
                <td>Small snippets, lost context</td>
                <td className={styles.highlight}>Full spec + interface contracts in context</td>
              </tr>
              <tr>
                <td>Dependency awareness</td>
                <td>Manual tracking</td>
                <td className={styles.highlight}>Cross-service contracts defined upfront</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Code Quality */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Code Quality Comparison</h2>
          <p className={styles.sectionBody}>
            Prompt-driven AI usage produces code that works in isolation but often drifts
            from architectural intent. Spec-driven usage produces code that conforms to
            explicit contracts, naming conventions, and error-handling patterns because
            those patterns are defined in the specification the AI consumes.
          </p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Quality Dimension</th>
                <th>Non-AI-Native</th>
                <th>AI-Native</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Consistency across services</td>
                <td>Variable — depends on who prompted</td>
                <td className={styles.highlight}>Uniform — spec enforces patterns</td>
              </tr>
              <tr>
                <td>Error handling</td>
                <td>Often missing or inconsistent</td>
                <td className={styles.highlight}>Defined in spec, generated uniformly</td>
              </tr>
              <tr>
                <td>Naming conventions</td>
                <td>AI defaults or developer habits</td>
                <td className={styles.highlight}>Domain-specific, spec-prescribed</td>
              </tr>
              <tr>
                <td>Security patterns</td>
                <td>Bolted on after generation</td>
                <td className={styles.highlight}>Built into spec (RBAC, tenant isolation)</td>
              </tr>
              <tr>
                <td>Technical debt</td>
                <td>Accumulates rapidly</td>
                <td className={styles.highlight}>Controlled by spec revision</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Testing */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Testing Approaches</h2>
          <p className={styles.sectionBody}>
            Non-AI-native teams often generate tests after writing code, resulting in tests
            that validate implementation details rather than business requirements. AI-native
            teams define test requirements in the specification, so generated tests validate
            contracts and behavior.
          </p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Testing Aspect</th>
                <th>Non-AI-Native</th>
                <th>AI-Native</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Test timing</td>
                <td>After implementation</td>
                <td className={styles.highlight}>Defined with specification</td>
              </tr>
              <tr>
                <td>Test scope</td>
                <td>Unit tests for generated code</td>
                <td className={styles.highlight}>Unit, integration, contract, migration</td>
              </tr>
              <tr>
                <td>Coverage strategy</td>
                <td>Line coverage targets</td>
                <td className={styles.highlight}>Behavior and contract coverage</td>
              </tr>
              <tr>
                <td>Multi-tenant validation</td>
                <td>Often missed</td>
                <td className={styles.highlight}>Required by spec, tested explicitly</td>
              </tr>
              <tr>
                <td>Regression detection</td>
                <td>Manual or ad-hoc</td>
                <td className={styles.highlight}>Contract tests catch cross-service breaks</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Documentation */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Documentation Practices</h2>
          <p className={styles.sectionBody}>
            When code is generated from specifications, documentation is a byproduct of the
            process rather than an afterthought. OpenAPI annotations, ADRs, and compliance
            evidence emerge from the same spec artifacts that drive code generation.
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>Non-AI-native: Documentation written after shipping, often incomplete or outdated within weeks.</li>
              <li>AI-native: Specifications are living documents. Code and docs co-evolve because both derive from the same source.</li>
              <li>Non-AI-native: API docs require manual Swagger annotation passes.</li>
              <li>AI-native: OpenAPI annotations generated from spec-defined contracts. HDIM achieved 157 documented endpoints this way.</li>
              <li>Non-AI-native: Architecture Decision Records written retroactively.</li>
              <li>AI-native: ADRs are part of the specification process, written before implementation begins.</li>
            </ul>
          </div>
        </div>

        {/* Velocity */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Development Velocity</h2>
          <p className={styles.sectionBody}>
            The velocity difference is not linear. Spec-driven AI usage enables parallel
            service generation, consistent quality at scale, and dramatically reduced
            rework cycles.
          </p>
          <div className={styles.metricsGrid}>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>12x</div>
              <div className={styles.metricLabel}>Faster Delivery</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>51+</div>
              <div className={styles.metricLabel}>Services in 6 Weeks</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>613+</div>
              <div className={styles.metricLabel}>Tests Generated</div>
            </div>
            <div className={styles.metricCard}>
              <div className={styles.metricValue}>1</div>
              <div className={styles.metricLabel}>Architect Required</div>
            </div>
          </div>
        </div>

        {/* Key Insights */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>Key Insights</h2>
          <div className={styles.keyPoints}>
            <ul>
              <li>AI-native is an operating model, not a skill level. Senior engineers who resist specification-first processes will underperform junior engineers who embrace them.</li>
              <li>The specification is the product. Code is a derivative artifact. Teams that invest in specification quality see compounding returns on every generated service.</li>
              <li>Context window management is a core competency. AI-native architects structure specifications to fit within context limits while preserving cross-service coherence.</li>
              <li>Prompt engineering is necessary but insufficient. Without architectural specifications, prompts produce isolated code fragments that require expensive manual integration.</li>
              <li>The 12x velocity multiplier is real but requires discipline. Ad-hoc AI usage delivers 1.5-2x improvement. Spec-driven AI usage delivers 10-15x improvement.</li>
            </ul>
          </div>
        </div>

        {/* CTA */}
        <div className={styles.ctaSection}>
          <h2>Explore the Full Resource Library</h2>
          <p>Deep-dive whitepapers, technical evidence, and methodology breakdowns for healthcare technology leaders.</p>
          <div className={styles.ctaButtons}>
            <Link className={styles.btnPrimary} href="/resources">Browse All Resources</Link>
            <Link className={styles.btnSecondary} href="/resources/spec-driven-development">Spec-Driven Development</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
