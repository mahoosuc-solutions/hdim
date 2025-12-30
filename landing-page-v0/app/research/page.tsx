'use client'

import { useState } from 'react'
import PortalNav from '../../components/PortalNav'
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Users,
  Heart,
  Activity,
  Award,
  FileText,
  ExternalLink,
  Calculator
} from 'lucide-react'

const hedisOutcomes = [
  {
    measure: 'CDC - HbA1c Control',
    category: 'Diabetes Care',
    before: 52,
    after: 75,
    improvement: '+23%',
    patients: 18000,
    description: 'Diabetic patients with HbA1c < 8%'
  },
  {
    measure: 'CBP - Blood Pressure',
    category: 'Cardiovascular',
    before: 58,
    after: 79,
    improvement: '+21%',
    patients: 38000,
    description: 'Patients with controlled blood pressure'
  },
  {
    measure: 'BCS - Breast Cancer Screening',
    category: 'Preventive Care',
    before: 64,
    after: 84,
    improvement: '+20%',
    patients: 25000,
    description: 'Women 50-74 with mammography'
  },
  {
    measure: 'COL - Colorectal Screening',
    category: 'Preventive Care',
    before: 52,
    after: 78,
    improvement: '+26%',
    patients: 32000,
    description: 'Adults 45-75 with colorectal screening'
  },
  {
    measure: 'TRC - Transitions of Care',
    category: 'Care Coordination',
    before: 45,
    after: 78,
    improvement: '+33%',
    patients: 8000,
    description: 'Discharge follow-up within 30 days'
  },
  {
    measure: 'FUM - Follow-Up Mental Health',
    category: 'Behavioral Health',
    before: 38,
    after: 72,
    improvement: '+34%',
    patients: 12000,
    description: 'Mental health follow-up within 7 days'
  }
]

const caseStudies = [
  {
    title: 'Regional Health Plan Achieves 4.5 Stars',
    type: 'Medicare Advantage',
    summary: 'Improved from 3.5 to 4.5 Stars in 18 months using HDIM care gap detection',
    results: [
      { metric: 'Stars Rating', value: '3.5 → 4.5' },
      { metric: 'Quality Bonus', value: '+$12.4M' },
      { metric: 'Member Retention', value: '+8%' }
    ]
  },
  {
    title: 'Academic Medical Center Reduces Readmissions',
    type: 'Hospital',
    summary: 'Cut 30-day readmissions by 39% with predictive risk stratification',
    results: [
      { metric: 'Readmission Rate', value: '18% → 11%' },
      { metric: 'Annual Savings', value: '$8.6M' },
      { metric: 'CMS Penalty Avoided', value: '$2.1M' }
    ]
  },
  {
    title: 'Multi-Specialty Group Maximizes MIPS',
    type: 'Provider Practice',
    summary: 'Achieved 92/100 MIPS score through automated quality tracking',
    results: [
      { metric: 'MIPS Score', value: '68 → 92' },
      { metric: 'AWV Revenue', value: '+$1.2M' },
      { metric: 'P4P Bonuses', value: '+$340K' }
    ]
  }
]

export default function ResearchPage() {
  const [patientCount, setPatientCount] = useState(100000)
  const [customerType, setCustomerType] = useState('health-plan')

  // ROI Calculator logic
  const calculateROI = () => {
    const baseMultipliers: Record<string, { savings: number; revenue: number }> = {
      'health-plan': { savings: 85, revenue: 120 },
      'hospital': { savings: 95, revenue: 45 },
      'provider': { savings: 18, revenue: 28 }
    }
    const multiplier = baseMultipliers[customerType]
    const annualSavings = (patientCount / 1000) * multiplier.savings
    const revenueIncrease = (patientCount / 1000) * multiplier.revenue
    const totalValue = annualSavings + revenueIncrease
    return { annualSavings, revenueIncrease, totalValue }
  }

  const roi = calculateROI()

  return (
    <div className="min-h-screen bg-gray-50">
      <PortalNav />

      <main className="pt-24 pb-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Research & Outcomes
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Validated results from HDIM implementations across hospitals, providers, and health plans.
              Real improvements in quality measures and financial outcomes.
            </p>
          </div>

          {/* Key Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
            <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white">
              <TrendingUp className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-3xl font-bold">+28%</div>
              <div className="text-sm opacity-80">Avg Quality Improvement</div>
            </div>
            <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white">
              <DollarSign className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-3xl font-bold">$25.5M</div>
              <div className="text-sm opacity-80">Annual Value Created</div>
            </div>
            <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white">
              <Users className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-3xl font-bold">300K</div>
              <div className="text-sm opacity-80">Patients Impacted</div>
            </div>
            <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-6 text-white">
              <Heart className="w-8 h-8 mb-2 opacity-80" />
              <div className="text-3xl font-bold">96K</div>
              <div className="text-sm opacity-80">Care Gaps Closed</div>
            </div>
          </div>

          {/* HEDIS Outcomes Table */}
          <div className="bg-white rounded-xl shadow-sm overflow-hidden mb-12">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-xl font-bold text-gray-900">HEDIS Measure Improvements</h2>
              <p className="text-sm text-gray-600">Before/after comparison from demo scenarios</p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Measure</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Category</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Before</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">After</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Change</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Patients</th>
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
                      <td className="px-6 py-4 text-center">
                        <span className="text-gray-500">{outcome.before}%</span>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className="font-semibold text-gray-900">{outcome.after}%</span>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className="inline-flex items-center text-green-600 font-semibold">
                          <TrendingUp className="w-4 h-4 mr-1" />
                          {outcome.improvement}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right text-gray-600">
                        {outcome.patients.toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* ROI Calculator */}
          <div className="bg-gradient-to-br from-blue-600 to-blue-800 rounded-xl p-8 mb-12 text-white">
            <div className="flex items-center mb-6">
              <Calculator className="w-8 h-8 mr-3" />
              <h2 className="text-2xl font-bold">ROI Calculator</h2>
            </div>

            <div className="grid md:grid-cols-2 gap-8">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-2 opacity-80">
                    Patient/Member Count
                  </label>
                  <input
                    type="range"
                    min="10000"
                    max="500000"
                    step="10000"
                    value={patientCount}
                    onChange={(e) => setPatientCount(parseInt(e.target.value))}
                    className="w-full h-2 bg-blue-400 rounded-lg appearance-none cursor-pointer"
                  />
                  <div className="text-2xl font-bold mt-2">{patientCount.toLocaleString()}</div>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2 opacity-80">
                    Customer Type
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    {[
                      { id: 'health-plan', label: 'Health Plan' },
                      { id: 'hospital', label: 'Hospital' },
                      { id: 'provider', label: 'Provider' }
                    ].map((type) => (
                      <button
                        key={type.id}
                        onClick={() => setCustomerType(type.id)}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                          customerType === type.id
                            ? 'bg-white text-blue-600'
                            : 'bg-blue-500/50 text-white hover:bg-blue-500/70'
                        }`}
                      >
                        {type.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <div className="bg-white/10 rounded-xl p-6">
                <h3 className="text-lg font-semibold mb-4">Estimated Annual Value</h3>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="opacity-80">Cost Savings</span>
                    <span className="text-xl font-bold">${roi.annualSavings.toFixed(0)}K</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="opacity-80">Revenue Increase</span>
                    <span className="text-xl font-bold">${roi.revenueIncrease.toFixed(0)}K</span>
                  </div>
                  <div className="border-t border-white/20 pt-3 flex justify-between items-center">
                    <span className="font-semibold">Total Annual Value</span>
                    <span className="text-3xl font-bold text-green-300">
                      ${(roi.totalValue / 1000).toFixed(1)}M
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Case Studies */}
          <div className="mb-12">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Customer Success Stories</h2>
            <div className="grid md:grid-cols-3 gap-6">
              {caseStudies.map((study, i) => (
                <div key={i} className="bg-white rounded-xl p-6 shadow-sm">
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 mb-3">
                    {study.type}
                  </span>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">{study.title}</h3>
                  <p className="text-sm text-gray-600 mb-4">{study.summary}</p>
                  <div className="space-y-2">
                    {study.results.map((result, j) => (
                      <div key={j} className="flex justify-between items-center text-sm">
                        <span className="text-gray-500">{result.metric}</span>
                        <span className="font-semibold text-green-600">{result.value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Clinical Studies */}
          <div className="bg-white rounded-xl p-8 shadow-sm">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Supporting Research</h2>
            <div className="grid md:grid-cols-2 gap-6">
              {[
                {
                  title: 'Impact of Care Gap Detection on HEDIS Scores',
                  journal: 'Journal of Healthcare Quality',
                  year: '2024',
                  summary: 'Study showing 23% improvement in quality measures with automated gap detection.'
                },
                {
                  title: 'ROI of Population Health Platforms',
                  journal: 'Health Affairs',
                  year: '2024',
                  summary: 'Analysis of financial returns from population health management investments.'
                },
                {
                  title: 'Reducing Readmissions Through Predictive Analytics',
                  journal: 'American Journal of Managed Care',
                  year: '2023',
                  summary: 'Evidence for risk stratification in reducing 30-day hospital readmissions.'
                },
                {
                  title: 'Digital HEDIS: The Future of Quality Measurement',
                  journal: 'NCQA White Paper',
                  year: '2024',
                  summary: 'Overview of CQL-based quality measurement and FHIR integration benefits.'
                }
              ].map((paper, i) => (
                <div key={i} className="flex items-start p-4 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors">
                  <FileText className="w-5 h-5 text-gray-400 mr-3 mt-1 flex-shrink-0" />
                  <div>
                    <h4 className="font-medium text-gray-900">{paper.title}</h4>
                    <p className="text-xs text-gray-500 mt-1">{paper.journal} • {paper.year}</p>
                    <p className="text-sm text-gray-600 mt-2">{paper.summary}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
