'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useState } from 'react';
import styles from '@/styles/content-page.module.css';

type TabId = 'non-technical' | 'mixed' | 'technical';

const tabs: { id: TabId; label: string }[] = [
  { id: 'non-technical', label: 'Non-Technical View' },
  { id: 'mixed', label: 'Mixed View' },
  { id: 'technical', label: 'Technical View' },
];

const journeySteps = [
  'Patient Event Created',
  'Data Ingested',
  'Kafka Stream Routed',
  'Service Processing',
  'Redis Context Lookup',
  'Care Rule Evaluation (1 Patient \u2192 N Measures)',
  'Action Generated',
  'Consumer Delivery',
  'Outcome + Audit',
];

export default function FHIRPipelinePage() {
  const [activeTab, setActiveTab] = useState<TabId>('non-technical');

  return (
    <div className={styles.pageWrapper}>
      <section className={styles.hero}>
        <p
          style={{
            letterSpacing: '0.09em',
            textTransform: 'uppercase',
            fontWeight: 700,
            fontSize: '0.75rem',
            marginBottom: '0.5rem',
            opacity: 0.85,
          }}
        >
          Product + Architecture Narrative
        </p>
        <h1 className={styles.heroTitle}>Race Track FHIR Pipeline: One Journey, Three Views</h1>
        <p className={styles.heroSubtitle}>
          Patients &quot;drive&quot; through one canonical workflow while their data moves through
          FHIR, Kafka, services, and Redis. Switch tabs to compare non-technical, mixed, and
          technical explanations without changing the underlying flow.
        </p>
        <div className={styles.callout} style={{ marginTop: '1rem', color: '#2C3E50' }}>
          Built from real-world HIE operating patterns (Healthix, HealthInfoNet) and modern identity
          strategy (Verato) to remove custom-interface bottlenecks and activate FHIR-native,
          event-driven workflows.
        </div>

        {/* Tab buttons */}
        <div
          role="tablist"
          aria-label="Audience views"
          style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '1.25rem' }}
        >
          {tabs.map((tab) => (
            <button
              key={tab.id}
              role="tab"
              aria-selected={activeTab === tab.id}
              aria-controls={`panel-${tab.id}`}
              onClick={() => setActiveTab(tab.id)}
              className={styles.btnPrimary}
              style={{
                background: activeTab === tab.id ? '#f6cf6a' : '#1a5962',
                color: activeTab === tab.id ? '#163740' : '#d9eff1',
                border: `1px solid ${activeTab === tab.id ? '#f6cf6a' : '#2c727d'}`,
                padding: '0.625rem 0.875rem',
                fontSize: '0.98rem',
                cursor: 'pointer',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </section>

      <div className={styles.container}>
        {/* Pipeline Architecture Diagram */}
        <section className={styles.section}>
          <figure style={{ margin: 0 }}>
            <Image
              src={
                activeTab === 'technical'
                  ? '/resources/race-track-technical.svg'
                  : activeTab === 'mixed'
                    ? '/resources/race-track-mixed.svg'
                    : '/resources/race-track-overview.svg'
              }
              alt={`FHIR pipeline ${activeTab} architecture view`}
              width={1600}
              height={900}
              style={{ width: '100%', height: 'auto', borderRadius: '12px' }}
              priority
            />
            <figcaption style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.5rem', textAlign: 'center' }}>
              {activeTab === 'technical'
                ? 'Technical view: Kafka topology, Redis cache layers, and consumer group detail'
                : activeTab === 'mixed'
                  ? 'Hybrid view: race-track metaphor mapped to Kafka, services, and Redis internals'
                  : 'Overview: patient journey from FHIR ingestion through care gap detection to action delivery'}
            </figcaption>
          </figure>
        </section>

        {/* Canonical Journey */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Canonical Journey (Same Across All Tabs)</h2>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(165px, 1fr))',
              gap: '0.5rem',
              overflowX: 'auto',
            }}
          >
            {journeySteps.map((step, i) => (
              <div key={i} className={styles.card} style={{ minHeight: '74px' }}>
                <div
                  style={{
                    color: '#1f4f58',
                    fontSize: '0.78rem',
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                    fontWeight: 700,
                    marginBottom: '0.25rem',
                  }}
                >
                  {i + 1}
                </div>
                <div style={{ fontSize: '0.98rem' }}>{step}</div>
              </div>
            ))}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.75rem' }}>
            {[
              { label: 'Track lane', bg: 'rgba(31,79,88,0.12)', color: '#1f4f58' },
              { label: 'Data lane', bg: 'rgba(15,139,141,0.12)', color: '#0b6869' },
              { label: 'Kafka lane', bg: 'rgba(209,73,91,0.14)', color: '#a53747' },
              { label: 'Redis lane', bg: 'rgba(239,177,29,0.2)', color: '#8f6800' },
              { label: 'Outcome lane', bg: 'rgba(235,111,45,0.13)', color: '#9f4718' },
            ].map((chip) => (
              <span
                key={chip.label}
                style={{
                  borderRadius: '999px',
                  padding: '0.375rem 0.625rem',
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  background: chip.bg,
                  color: chip.color,
                }}
              >
                {chip.label}
              </span>
            ))}
          </div>
        </section>

        {/* Non-Technical View */}
        {activeTab === 'non-technical' && (
          <section
            id="panel-non-technical"
            role="tabpanel"
            aria-labelledby="tab-non-technical"
            className={styles.section}
          >
            <h2 className={styles.sectionTitle}>Story-First View (Fully Illustrative)</h2>
            <div className={styles.keyPoints}>
              <h3 className={styles.sectionSubtitle} style={{ borderBottom: 'none' }}>
                Track Lane
              </h3>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.5rem',
                }}
              >
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Race Start</h4>
                  <p className={styles.cardBody}>Patient event created in customer systems</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Pit Stop A</h4>
                  <p className={styles.cardBody}>Customer FHIR + CDR provide source data</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Pit Stop B</h4>
                  <p className={styles.cardBody}>Standards-based processing routes events</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Pit Stop C</h4>
                  <p className={styles.cardBody}>Care decision checkpoint applies context</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Finish</h4>
                  <p className={styles.cardBody}>Action delivered and outcome tracked</p>
                </div>
              </div>
            </div>

            <div className={styles.keyPoints} style={{ marginTop: '1rem' }}>
              <h3 className={styles.sectionSubtitle} style={{ borderBottom: 'none' }}>
                Audience Language
              </h3>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.5rem',
                }}
              >
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>FHIR standards integration</h4>
                  <p className={styles.cardBody}>Plug in to existing customer systems</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Events move in real time</h4>
                  <p className={styles.cardBody}>No custom one-off interfaces</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Clinical context applied</h4>
                  <p className={styles.cardBody}>Data is interpreted before action</p>
                </div>
                <div className={styles.card}>
                  <h4 className={styles.cardTitle}>Standard actions back</h4>
                  <p className={styles.cardBody}>Care teams get tasks and guidance</p>
                </div>
              </div>
            </div>

            <div className={styles.callout}>
              <strong>Key takeaway:</strong> Patients follow one continuous workflow. Data moves
              with them end-to-end, so onboarding does not require custom interfaces.
            </div>
          </section>
        )}

        {/* Mixed View */}
        {activeTab === 'mixed' && (
          <section
            id="panel-mixed"
            role="tabpanel"
            aria-labelledby="tab-mixed"
            className={styles.section}
          >
            <h2 className={styles.sectionTitle}>Hybrid View (Metaphor + Architecture)</h2>

            <h3 className={styles.sectionSubtitle}>Customer Domain</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Customer FHIR Server</h4>
                <p className={styles.cardBody}>FHIR R4 APIs</p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Customer CDR/EHR</h4>
                <p className={styles.cardBody}>Historical + current records</p>
              </div>
            </div>

            <h3 className={styles.sectionSubtitle}>Interoperability + Internal FHIR</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Interoperability Gateway</h4>
                <p className={styles.cardBody}>
                  Normalize customer data into internal FHIR model
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Internal FHIR Server</h4>
                <p className={styles.cardBody}>Longitudinal record source-of-truth</p>
              </div>
            </div>

            <h3 className={styles.sectionSubtitle}>Streaming + Services</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Kafka Topics</h4>
                <p className={styles.cardBody}>
                  hdim.patient.events.v1, hdim.care_gap.events.v1, hdim.quality.events.v1,
                  hdim.actions.events.v1
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Measure Fan-Out</h4>
                <p className={styles.cardBody}>
                  One patient event fans out to N active quality measures for the tenant
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Services</h4>
                <p className={styles.cardBody}>
                  Patient Service, Care Gap Service, Quality Measure Service, Event Service
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Redis</h4>
                <p className={styles.cardBody}>
                  Patient Snapshot Cache, Rule Context Cache, Idempotency Keys
                </p>
              </div>
            </div>

            <h3 className={styles.sectionSubtitle}>Write-Back</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Action APIs</h4>
                <p className={styles.cardBody}>Task, CarePlan, Communication</p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Consumer Delivery</h4>
                <p className={styles.cardBody}>Care teams and downstream systems</p>
              </div>
            </div>

            <div className={styles.callout}>
              <strong>Key takeaway:</strong> Customer FHIR/CDR data crosses a standards boundary
              into internal FHIR, one patient event fans out to N active measures, then results are
              returned as FHIR-native actions.
            </div>
          </section>
        )}

        {/* Technical View */}
        {activeTab === 'technical' && (
          <section
            id="panel-technical"
            role="tabpanel"
            aria-labelledby="tab-technical"
            className={styles.section}
          >
            <h2 className={styles.sectionTitle}>Technical View (Full Internals)</h2>

            <h3 className={styles.sectionSubtitle}>Ingestion + Canonicalization</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Producers</h4>
                <p className={styles.cardBody}>
                  Customer FHIR Server, customer CDR feeds, subscription triggers
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Canonical Event Builder</h4>
                <p className={styles.cardBody}>
                  FHIR ingestion to canonical event envelope
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Tenant Boundary</h4>
                <p className={styles.cardBody}>
                  Tenant context and auth enforced at interoperability layer
                </p>
              </div>
            </div>

            <h3 className={styles.sectionSubtitle}>Kafka Processing Topology</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Topics</h4>
                <p className={styles.cardBody}>
                  hdim.patient.events.v1, hdim.care_gap.events.v1, hdim.quality.events.v1,
                  hdim.actions.events.v1
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Per-Patient Fan-Out</h4>
                <p className={styles.cardBody}>
                  One canonical patient event triggers measure evaluations across N configured
                  measure definitions
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Consumer Groups</h4>
                <p className={styles.cardBody}>
                  hdim.patient-service.cg.v1, hdim.care-gap-service.cg.v1,
                  hdim.quality-measure-service.cg.v1, hdim.action-service.cg.v1
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Resilience</h4>
                <p className={styles.cardBody}>
                  hdim.patient.events.retry.v1, hdim.patient.events.dlq.v1, bounded retries and
                  deterministic replay
                </p>
              </div>
            </div>

            <h3 className={styles.sectionSubtitle}>State + Decisioning</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Redis Patient Snapshot Cache</h4>
                <p className={styles.cardBody}>Low-latency clinical context read</p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Redis Rule Context Cache</h4>
                <p className={styles.cardBody}>Guideline and threshold lookup</p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Redis Idempotency Keys</h4>
                <p className={styles.cardBody}>Exactly-once style action protection</p>
              </div>
            </div>

            <h3 className={styles.sectionSubtitle}>Delivery + Observability</h3>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.5rem',
              }}
            >
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Action Publisher</h4>
                <p className={styles.cardBody}>
                  FHIR-native Task, CarePlan, Communication write-back
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>Observability</h4>
                <p className={styles.cardBody}>
                  Consumer lag, distributed traces, audit event stream
                </p>
              </div>
              <div className={styles.card}>
                <h4 className={styles.cardTitle}>PHI Safety</h4>
                <p className={styles.cardBody}>
                  PHI-safe logging boundaries + restricted payload traces
                </p>
              </div>
            </div>

            <div className={styles.callout}>
              <strong>Key takeaway:</strong> Deterministic producer-consumer processing
              where one patient event is evaluated against N measures, with explicit cache roles,
              idempotent action publishing, retry/DLQ controls, and tenant-safe observability.
            </div>
          </section>
        )}

        {/* Navigation */}
        <section className={styles.ctaSection}>
          <h2>Explore More</h2>
          <p>See the validation evidence or return to resources.</p>
          <div className={styles.ctaButtons}>
            <Link href="/resources/fhir-evidence" className={styles.btnPrimary}>
              Validation Approach
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
