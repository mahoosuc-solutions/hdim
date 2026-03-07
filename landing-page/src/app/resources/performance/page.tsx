import type { Metadata } from 'next';
import Link from 'next/link';
import Image from 'next/image';
import styles from '@/styles/content-page.module.css';

export const metadata: Metadata = {
  title: 'Performance Benchmarking | HDIM Resources',
  description:
    'CQL/FHIR vs Traditional SQL performance comparison. Demonstrating 2-4x performance improvement through modern architecture.',
  alternates: { canonical: 'https://healthdatainmotion.com/resources/performance' },
};

export default function PerformancePage() {
  return (
    <div className={styles.pageWrapper}>
      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>Performance Benchmarking</h1>
        <p className={styles.heroSubtitle}>CQL/FHIR vs Traditional SQL -- 2-4x Faster</p>
      </section>

      <div className={styles.container}>
        <p className={styles.sectionBody}>
          <strong>
            Demonstrating 2-4x Performance Improvement Through Modern Architecture
          </strong>
        </p>

        {/* Executive Summary */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Executive Summary</h2>
          <p className={styles.sectionBody}>
            Our FHIR/CQL-based quality measure evaluation system delivers{' '}
            <strong>2-4x faster performance</strong> compared to traditional SQL-based approaches.
            This document presents comprehensive benchmarking results demonstrating measurable
            performance improvements across multiple scenarios.
          </p>

          <h3 className={styles.sectionSubtitle}>Key Findings</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Metric</th>
                <th>CQL/FHIR</th>
                <th>SQL Traditional</th>
                <th>Improvement</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Average Latency</strong></td>
                <td className={styles.highlight}>85ms</td>
                <td>280ms</td>
                <td className={styles.highlight}>3.3x faster</td>
              </tr>
              <tr>
                <td><strong>P95 Latency</strong></td>
                <td className={styles.highlight}>180ms</td>
                <td>520ms</td>
                <td className={styles.highlight}>2.9x faster</td>
              </tr>
              <tr>
                <td><strong>P99 Latency</strong></td>
                <td className={styles.highlight}>320ms</td>
                <td>780ms</td>
                <td className={styles.highlight}>2.4x faster</td>
              </tr>
              <tr>
                <td><strong>Overall Improvement</strong></td>
                <td colSpan={2}></td>
                <td className={styles.highlight}>69.6% faster</td>
              </tr>
            </tbody>
          </table>
        </section>

        {/* Why Performance Matters */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Why Performance Matters</h2>
          <p className={styles.sectionBody}>
            In healthcare quality measurement, performance directly impacts:
          </p>
          <div className={styles.keyPoints}>
            <ul>
              <li>
                <strong>Patient Care:</strong> Faster evaluation means quicker care gap
                identification
              </li>
              <li>
                <strong>Provider Efficiency:</strong> Reduced wait times for quality measure results
              </li>
              <li>
                <strong>Cost Savings:</strong> Lower compute requirements reduce infrastructure
                costs
              </li>
              <li>
                <strong>Scalability:</strong> Better performance enables larger patient populations
              </li>
              <li>
                <strong>User Experience:</strong> Sub-second response times improve clinical
                workflows
              </li>
            </ul>
          </div>
        </section>

        {/* Benchmarking Methodology */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Benchmarking Methodology</h2>

          <h3 className={styles.sectionSubtitle}>Test Scenarios</h3>
          <p className={styles.sectionBody}>We benchmarked four key scenarios:</p>

          <div className={styles.card}>
            <h4 className={styles.cardTitle}>1. Single Patient, Single Measure</h4>
            <p className={styles.cardBody}>
              Baseline performance comparison. 100 iterations per test. Measures: HEDIS-CDC,
              HEDIS-CBP, HEDIS-BCS.
            </p>
          </div>
          <div className={styles.card}>
            <h4 className={styles.cardTitle}>2. Single Patient, Multiple Measures</h4>
            <p className={styles.cardBody}>
              Parallel processing advantage. Tests: 5, 10, 52 measures. CQL uses parallel
              execution; SQL uses sequential execution.
            </p>
          </div>
          <div className={styles.card}>
            <h4 className={styles.cardTitle}>3. Batch Evaluation</h4>
            <p className={styles.cardBody}>
              Scalability with multiple patients. Tests: 10, 100, 1000 patients. Single measure per
              batch.
            </p>
          </div>
          <div className={styles.card}>
            <h4 className={styles.cardTitle}>4. Concurrent Load</h4>
            <p className={styles.cardBody}>
              System performance under load. Tests: 10, 50, 100, 500 concurrent users. Real-world
              usage patterns.
            </p>
          </div>

          <h3 className={styles.sectionSubtitle}>Measurement Standards</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>
                <strong>Statistical Validity:</strong> Minimum 100 iterations per test
              </li>
              <li>
                <strong>Warmup Period:</strong> 10 iterations to account for JIT and cache warming
              </li>
              <li>
                <strong>Percentiles:</strong> Reported P50, P95, P99 (not just averages)
              </li>
              <li>
                <strong>Fair Comparison:</strong> Same data, same environment, same indexes
              </li>
            </ul>
          </div>
        </section>

        {/* Detailed Results */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Detailed Results</h2>

          <h3 className={styles.sectionSubtitle}>Single Patient Evaluation</h3>
          <p className={styles.sectionBody}>
            One patient, one measure (HEDIS-CDC -- Diabetes HbA1c Control):
          </p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Approach</th>
                <th>Average</th>
                <th>P50</th>
                <th>P95</th>
                <th>P99</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>CQL/FHIR (Cached)</strong></td>
                <td className={styles.highlight}>85ms</td>
                <td className={styles.highlight}>75ms</td>
                <td className={styles.highlight}>180ms</td>
                <td className={styles.highlight}>320ms</td>
              </tr>
              <tr>
                <td><strong>CQL/FHIR (Uncached)</strong></td>
                <td>220ms</td>
                <td>180ms</td>
                <td>400ms</td>
                <td>600ms</td>
              </tr>
              <tr>
                <td><strong>SQL Traditional</strong></td>
                <td>280ms</td>
                <td>250ms</td>
                <td>520ms</td>
                <td>780ms</td>
              </tr>
            </tbody>
          </table>
          <p className={styles.sectionBody}>
            <strong>Key Insight:</strong> CQL/FHIR with caching is <strong>3.3x faster</strong> than
            SQL. Even without caching, it is <strong>1.3x faster</strong> due to optimized
            architecture.
          </p>

          <h3 className={styles.sectionSubtitle}>Multi-Measure Evaluation</h3>
          <p className={styles.sectionBody}>One patient, 52 HEDIS measures:</p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Approach</th>
                <th>Total Time</th>
                <th>Avg per Measure</th>
                <th>Speedup</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>CQL/FHIR (Parallel + Cache)</strong></td>
                <td className={styles.highlight}>1.8s</td>
                <td className={styles.highlight}>35ms</td>
                <td className={styles.highlight}>4.4x faster</td>
              </tr>
              <tr>
                <td><strong>CQL/FHIR (Parallel, No Cache)</strong></td>
                <td>4.5s</td>
                <td>87ms</td>
                <td>1.8x faster</td>
              </tr>
              <tr>
                <td><strong>SQL Traditional (Sequential)</strong></td>
                <td>8.0s</td>
                <td>154ms</td>
                <td>--</td>
              </tr>
            </tbody>
          </table>
          <p className={styles.sectionBody}>
            <strong>Key Insight:</strong> Parallel processing provides significant advantage.
            CQL/FHIR evaluates 52 measures in <strong>1.8 seconds</strong> vs SQL&apos;s{' '}
            <strong>8 seconds</strong>.
          </p>

          <h3 className={styles.sectionSubtitle}>Batch Evaluation</h3>
          <p className={styles.sectionBody}>100 patients, single measure:</p>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Approach</th>
                <th>Total Time</th>
                <th>Avg per Patient</th>
                <th>Throughput</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>CQL/FHIR (Parallel)</strong></td>
                <td className={styles.highlight}>2-5s</td>
                <td className={styles.highlight}>20-50ms</td>
                <td className={styles.highlight}>20-50 patients/s</td>
              </tr>
              <tr>
                <td><strong>SQL Traditional</strong></td>
                <td>8-15s</td>
                <td>80-150ms</td>
                <td>7-12 patients/s</td>
              </tr>
            </tbody>
          </table>
          <p className={styles.sectionBody}>
            <strong>Key Insight:</strong> CQL/FHIR processes{' '}
            <strong>3-5x more patients per second</strong> than SQL.
          </p>

          <h3 className={styles.sectionSubtitle}>Concurrent Load Performance</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Concurrent Users</th>
                <th>CQL/FHIR P95</th>
                <th>SQL P95</th>
                <th>CQL Advantage</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>10</td>
                <td className={styles.highlight}>95ms</td>
                <td>180ms</td>
                <td className={styles.highlight}>1.9x</td>
              </tr>
              <tr>
                <td>50</td>
                <td className={styles.highlight}>140ms</td>
                <td>420ms</td>
                <td className={styles.highlight}>3.0x</td>
              </tr>
              <tr>
                <td>100</td>
                <td className={styles.highlight}>220ms</td>
                <td>650ms</td>
                <td className={styles.highlight}>3.0x</td>
              </tr>
              <tr>
                <td>500</td>
                <td className={styles.highlight}>450ms</td>
                <td>1,200ms</td>
                <td className={styles.highlight}>2.7x</td>
              </tr>
            </tbody>
          </table>
          <p className={styles.sectionBody}>
            <strong>Key Insight:</strong> CQL/FHIR maintains better performance under load, with{' '}
            <strong>2-3x advantage</strong> even at high concurrency.
          </p>
        </section>

        {/* Production Screenshot */}
        <section className={styles.section}>
          <figure style={{ margin: 0 }}>
            <Image
              src="/resources/screenshots/results-detail.jpg"
              alt="HDIM results table showing 150 HEDIS evaluations with severity, trend, and compliance rate columns"
              width={1915}
              height={924}
              style={{ width: '100%', height: 'auto', borderRadius: '8px', border: '1px solid #e2e8f0' }}
            />
            <figcaption style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.5rem' }}>
              150 HEDIS evaluations processed and displayed with severity classification, compliance trending, and one-click outreach actions.
            </figcaption>
          </figure>
        </section>

        {/* Why CQL/FHIR is Faster */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Why CQL/FHIR is Faster</h2>

          <div className={styles.card}>
            <h4 className={styles.cardTitle}>1. Intelligent Caching (87% Hit Rate)</h4>
            <p className={styles.cardBody}>
              Reduces database load by 7x. FHIR resources, measure definitions, and query results
              cached in Redis. 87% of requests served from cache.
            </p>
          </div>

          <div className={styles.card}>
            <h4 className={styles.cardTitle}>2. Parallel Processing</h4>
            <p className={styles.cardBody}>
              4-6x faster for multi-measure evaluation. Multiple measures evaluated concurrently
              with thread pool optimization and better CPU utilization. 52 measures in 1.8s vs 8s
              sequential.
            </p>
          </div>

          <div className={styles.card}>
            <h4 className={styles.cardTitle}>3. Optimized Data Access</h4>
            <p className={styles.cardBody}>
              2-3x faster data retrieval. FHIR service optimized for common queries with indexed
              resource lookups and reduced data transfer.
            </p>
          </div>

          <div className={styles.card}>
            <h4 className={styles.cardTitle}>4. Code Reuse</h4>
            <p className={styles.cardBody}>
              Reduced redundant queries through shared FHIR resources across measures,
              template-based evaluation, and less redundant querying.
            </p>
          </div>

          <div className={styles.card}>
            <h4 className={styles.cardTitle}>5. Modern Architecture</h4>
            <p className={styles.cardBody}>
              Better scalability and performance through microservices with dedicated caching,
              connection pooling, and async processing where possible.
            </p>
          </div>
        </section>

        {/* Real-World Impact */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Real-World Impact</h2>

          <h3 className={styles.sectionSubtitle}>Cost Savings</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>SQL approach: ~18 instances for 100K patients/month</li>
              <li>CQL/FHIR approach: ~3 instances for 100K patients/month</li>
              <li>
                <strong>Savings: 83% reduction in compute resources</strong>
              </li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>Scalability</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>SQL approach: Limited by sequential processing</li>
              <li>CQL/FHIR approach: Scales horizontally with parallel processing</li>
              <li>
                <strong>Result: 3-5x better throughput</strong>
              </li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>User Experience</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>SQL approach: 200-800ms average</li>
              <li>CQL/FHIR approach: 50-200ms average</li>
              <li>
                <strong>Result: 3-4x faster user experience</strong>
              </li>
            </ul>
          </div>
        </section>

        {/* Technical Comparison */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Technical Comparison</h2>

          <h3 className={styles.sectionSubtitle}>Architecture Differences</h3>
          <table className={styles.comparisonTable}>
            <thead>
              <tr>
                <th>Aspect</th>
                <th>Traditional SQL</th>
                <th>CQL/FHIR</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Data Flow</strong></td>
                <td>User Request to SQL Query to Database</td>
                <td className={styles.highlight}>
                  User Request to API Gateway to CQL Engine (Cached)
                </td>
              </tr>
              <tr>
                <td><strong>Processing</strong></td>
                <td>Complex JOINs, sequential</td>
                <td className={styles.highlight}>FHIR resource queries, parallel evaluation</td>
              </tr>
              <tr>
                <td><strong>Code System Mapping</strong></td>
                <td>Manual</td>
                <td className={styles.highlight}>Value set lookups (cached)</td>
              </tr>
              <tr>
                <td><strong>Execution Time</strong></td>
                <td>200-400ms</td>
                <td className={styles.highlight}>50-150ms (cached)</td>
              </tr>
            </tbody>
          </table>
        </section>

        {/* Validation */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Validation</h2>

          <h3 className={styles.sectionSubtitle}>Reproducibility</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>Reproducible with provided scripts</li>
              <li>Documented with full methodology</li>
              <li>Validated with statistical analysis</li>
              <li>Tested on production-like data</li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>Statistical Validity</h3>
          <ul>
            <li>Minimum 100 iterations per test</li>
            <li>Warmup period included</li>
            <li>Percentiles reported (P50, P95, P99)</li>
            <li>Multiple test runs averaged</li>
          </ul>
        </section>

        {/* Conclusion */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Conclusion</h2>

          <h3 className={styles.sectionSubtitle}>Performance Summary</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>
                <strong>2-4x faster</strong> average response times
              </li>
              <li>
                <strong>3-5x better</strong> throughput
              </li>
              <li>
                <strong>83% reduction</strong> in compute requirements
              </li>
              <li>
                <strong>Better scalability</strong> under concurrent load
              </li>
              <li>
                <strong>Improved user experience</strong> with sub-second responses
              </li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>Business Value</h3>
          <div className={styles.keyPoints}>
            <ul>
              <li>Lower infrastructure costs (83% reduction)</li>
              <li>Faster time-to-insight (3-4x improvement)</li>
              <li>Better scalability (3-5x throughput)</li>
              <li>Improved user satisfaction (sub-second responses)</li>
            </ul>
          </div>

          <h3 className={styles.sectionSubtitle}>Recommendation</h3>
          <p className={styles.sectionBody}>
            <strong>Use CQL/FHIR approach for:</strong> Production deployments, large patient
            populations, real-time quality measurement, cost-sensitive environments, and
            high-concurrency scenarios.
          </p>

          <p className={styles.subText}>
            <em>Last Updated: January 2025 -- Version: 1.0 -- Status: Production Validated</em>
          </p>
        </section>

        {/* CTA */}
        <section className={styles.ctaSection}>
          <h2>Explore More</h2>
          <p>
            See the FHIR pipeline architecture or dive into the AI solutioning metrics.
          </p>
          <div className={styles.ctaButtons}>
            <Link href="/resources/fhir-pipeline" className={styles.btnPrimary}>
              View FHIR Pipeline
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
