'use client'
import PortalNav from '../../components/PortalNav'
import {
  TrendingUp,
  DollarSign,
  Users,
  Heart,
  ExternalLink,
  Calculator
} from 'lucide-react'

const hedisOutcomes = [
  {
    measure: 'CDC - HbA1c Control',
    category: 'Diabetes Care',
    description: 'A1c control and monitoring for members with diabetes'
  },
  {
    measure: 'CBP - Blood Pressure',
    category: 'Cardiovascular',
    description: 'Blood pressure control and measurement adherence'
  },
  {
    measure: 'BCS - Breast Cancer Screening',
    category: 'Preventive Care',
    description: 'Screening completion and outreach readiness'
  },
  {
    measure: 'COL - Colorectal Screening',
    category: 'Preventive Care',
    description: 'Screening compliance and gap closure workflows'
  },
  {
    measure: 'TRC - Transitions of Care',
    category: 'Care Coordination',
    description: 'Post-discharge follow-up and care coordination'
  },
  {
    measure: 'FUM - Follow-Up Mental Health',
    category: 'Behavioral Health',
    description: 'Timely follow-up and behavioral health engagement'
  }
]

const caseStudies = [
  {
    title: 'Medicare Advantage Plan',
    type: 'Health Plan',
    summary: 'Built a standardized gap-closure workflow tied to HEDIS, Stars, and member outreach operations.',
    focus: ['Measure workflow standardization', 'Member outreach orchestration', 'Audit-ready reporting']
  },
  {
    title: 'Academic Medical Center',
    type: 'Hospital',
    summary: 'Integrated inpatient, outpatient, and post-acute data to coordinate transitions of care.',
    focus: ['Transitions of care gaps', 'Risk stratification signals', 'Care manager prioritization']
  },
  {
    title: 'Multi-Specialty Group',
    type: 'Provider Practice',
    summary: 'Unified measure tracking for MIPS, quality contracts, and internal performance targets.',
    focus: ['Measure inventory governance', 'Provider-level dashboards', 'Quality reporting readiness']
  }
]

export default function ResearchPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <PortalNav />

      <main className="pt-24 pb-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Outcomes & Evidence
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              HDIM provides transparent measure logic, audit-ready reporting, and a clear methodology for outcomes tracking.
              Client-validated results are available on request.
            </p>
          </div>

          {/* Outcome Areas */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
            <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white">
              <TrendingUp className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-lg font-semibold">Quality Performance</div>
              <div className="text-sm opacity-80">HEDIS and program readiness</div>
            </div>
            <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white">
              <DollarSign className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-lg font-semibold">Financial Impact</div>
              <div className="text-sm opacity-80">Incentives, penalties, and ROI</div>
            </div>
            <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white">
              <Users className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-lg font-semibold">Member Engagement</div>
              <div className="text-sm opacity-80">Outreach and care management</div>
            </div>
            <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-6 text-white">
              <Heart className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-lg font-semibold">Clinical Outcomes</div>
              <div className="text-sm opacity-80">Preventive and chronic care</div>
            </div>
          </div>

          {/* HEDIS Outcomes Table */}
          <div className="bg-white rounded-xl shadow-sm overflow-hidden mb-12">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-xl font-bold text-gray-900">HEDIS Measure Coverage</h2>
              <p className="text-sm text-gray-600">Representative measures with standardized gap definitions</p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Measure</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Category</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">What We Track</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {hedisOutcomes.map((outcome, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="font-medium text-gray-900">{outcome.measure}</div>
                        <div className="text-xs text-gray-500">{outcome.description}</div>
                      </td>
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                          {outcome.category}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-left text-gray-600">
                        Standardized gap definition, denominator logic, and outreach readiness
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* ROI Framework */}
          <div id="calculator" className="bg-gradient-to-br from-blue-600 to-blue-800 rounded-xl p-8 mb-12 text-white">
            <div className="flex items-center mb-6">
              <Calculator className="w-8 h-8 mr-3" />
              <h2 className="text-2xl font-bold">ROI &amp; Business Case</h2>
            </div>

            <div className="grid md:grid-cols-2 gap-8">
              <div>
                <p className="text-white/90 mb-6">
                  We build a custom ROI model using your baseline performance, measure mix, and member population.
                  The model highlights the largest value levers and the operational changes required to realize them.
                </p>
                <ul className="space-y-3 text-sm text-white/90">
                  <li>• Reduced manual measure calculation and reconciliation</li>
                  <li>• Earlier gap detection for preventive and chronic care</li>
                  <li>• Quality incentive protection and audit readiness</li>
                  <li>• Care management prioritization with measurable impact</li>
                </ul>
              </div>

              <div className="bg-white/10 rounded-xl p-6">
                <h3 className="text-lg font-semibold mb-4">Get a Customized Model</h3>
                <p className="text-sm text-white/80 mb-6">
                  Share your program goals and we&apos;ll deliver a tailored ROI and implementation plan.
                </p>
                <a
                  href="/schedule"
                  className="inline-flex items-center justify-center px-4 py-2 bg-white text-blue-700 rounded-lg text-sm font-semibold hover:bg-white/90 transition-colors"
                >
                  Request ROI Analysis
                </a>
              </div>
            </div>
          </div>

          {/* Case Studies */}
          <div className="mb-12">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Customer Success Stories</h2>
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-6">
              <p className="text-sm text-amber-800">
                <strong>Note:</strong> Client-validated outcome briefs are available on request and tailored to your
                measure mix, baseline performance, and care management model.
              </p>
            </div>
            <div className="grid md:grid-cols-3 gap-6">
              {caseStudies.map((study, i) => (
                <div key={i} className="bg-white rounded-xl p-6 shadow-sm">
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 mb-3">
                    {study.type}
                  </span>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">{study.title}</h3>
                  <p className="text-sm text-gray-600 mb-4">{study.summary}</p>
                  <div className="space-y-2">
                    {study.focus.map((item, j) => (
                      <div key={j} className="text-sm text-gray-600">
                        • {item}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Supporting References */}
          <div className="bg-white rounded-xl p-8 shadow-sm mb-12">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Industry Standards & References</h2>
            <p className="text-gray-600 mb-6">
              HDIM is built on industry-standard specifications and best practices from leading healthcare organizations.
            </p>
            <div className="grid md:grid-cols-2 gap-6">
              {[
                {
                  title: 'HEDIS Measures and Technical Specifications',
                  source: 'National Committee for Quality Assurance (NCQA)',
                  year: '2024',
                  summary: 'Official HEDIS measure specifications used for CQL-based quality evaluation in HDIM.',
                  url: 'https://www.ncqa.org/hedis/'
                },
                {
                  title: 'FHIR R4 Specification',
                  source: 'HL7 International',
                  year: '2023',
                  summary: 'The FHIR R4 standard that HDIM uses for clinical data interoperability via HAPI FHIR 7.x.',
                  url: 'https://hl7.org/fhir/R4/'
                },
                {
                  title: 'Clinical Quality Language (CQL) Specification',
                  source: 'HL7 International',
                  year: '2023',
                  summary: 'CQL specification enabling native quality measure logic execution without proprietary translation.',
                  url: 'https://cql.hl7.org/'
                },
                {
                  title: 'CMS Quality Reporting Programs',
                  source: 'Centers for Medicare & Medicaid Services',
                  year: '2024',
                  summary: 'Federal quality reporting requirements including MIPS, Stars, and value-based care programs.',
                  url: 'https://www.cms.gov/medicare/quality/value-based-programs'
                }
              ].map((ref, i) => (
                <a
                  key={i}
                  href={ref.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-start p-4 border border-gray-200 rounded-lg hover:border-blue-300 hover:bg-blue-50 transition-colors"
                >
                  <ExternalLink className="w-5 h-5 text-blue-500 mr-3 mt-1 flex-shrink-0" />
                  <div>
                    <h4 className="font-medium text-gray-900">{ref.title}</h4>
                    <p className="text-xs text-gray-500 mt-1">{ref.source} • {ref.year}</p>
                    <p className="text-sm text-gray-600 mt-2">{ref.summary}</p>
                  </div>
                </a>
              ))}
            </div>
          </div>

          {/* Care Transitions Pilot */}
          <div className="bg-gradient-to-br from-[#0D4F8B] to-[#0A3D6E] rounded-xl p-8 mb-12 text-white">
            <div className="md:flex md:items-center md:justify-between gap-8">
              <div>
                <h2 className="text-2xl font-bold mb-3">
                  New: Care Transitions Pilot Blueprint
                </h2>
                <p className="text-blue-100 max-w-2xl">
                  Review the public framework for a 90-day transitions-of-care pilot,
                  including scope, security baseline, KPI model, and deployment options
                  across vendor-hosted, customer-hosted cloud, and on-prem environments.
                </p>
              </div>
              <div className="mt-6 md:mt-0">
                <a
                  href="/solutions/transitions-of-care"
                  className="inline-flex items-center justify-center px-5 py-3 bg-white text-[#0D4F8B] rounded-lg text-sm font-semibold hover:bg-blue-50 transition-colors"
                >
                  View Pilot Blueprint
                </a>
              </div>
            </div>
          </div>

          {/* Technical Methodology */}
          <div className="bg-gray-100 rounded-xl p-8 shadow-sm">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Technical Methodology</h2>
            <div className="grid md:grid-cols-2 gap-8">
              <div>
                <h3 className="font-semibold text-gray-900 mb-3">Quality Measure Evaluation</h3>
                <ul className="text-sm text-gray-700 space-y-2">
                  <li className="flex items-start">
                    <span className="text-blue-500 mr-2">1.</span>
                    <span><strong>FHIR R4 Data Ingestion:</strong> Patient data loaded as native FHIR resources.</span>
                  </li>
                  <li className="flex items-start">
                    <span className="text-blue-500 mr-2">2.</span>
                    <span><strong>CQL-Native Execution:</strong> HEDIS CQL definitions executed using open-source CQL tooling.</span>
                  </li>
                  <li className="flex items-start">
                    <span className="text-blue-500 mr-2">3.</span>
                    <span><strong>Care Gap Detection:</strong> Measure results analyzed to identify actionable care gaps per patient</span>
                  </li>
                  <li className="flex items-start">
                    <span className="text-blue-500 mr-2">4.</span>
                    <span><strong>Performance Reporting:</strong> Aggregated results for HEDIS submission, Stars, MIPS reporting</span>
                  </li>
                </ul>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-3">Performance Targets</h3>
                <div className="bg-white rounded-lg p-4">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left py-2 text-gray-500">Metric</th>
                        <th className="text-right py-2 text-gray-500">Target</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      <tr>
                        <td className="py-2">FHIR Query Latency (p95)</td>
                        <td className="text-right">&lt;200ms</td>
                      </tr>
                      <tr>
                        <td className="py-2">CQL Execution (avg)</td>
                        <td className="text-right">&lt;600ms</td>
                      </tr>
                      <tr>
                        <td className="py-2">Throughput</td>
                        <td className="text-right">150/sec</td>
                      </tr>
                      <tr>
                        <td className="py-2">Cache Hit Rate</td>
                        <td className="text-right">&gt;80%</td>
                      </tr>
                    </tbody>
                  </table>
                  <p className="text-xs text-gray-500 mt-3">
                    Targets vary by deployment size, data sources, and measure mix.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
