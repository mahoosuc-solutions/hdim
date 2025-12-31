'use client'

import { useState, useMemo } from 'react'
import PortalNav from '../../components/PortalNav'
import {
  Search,
  Filter,
  Users,
  Activity,
  Heart,
  Brain,
  Pill,
  Calendar,
  ChevronDown,
  ChevronRight,
  BarChart3,
  PieChart,
  Table,
  Download,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  XCircle
} from 'lucide-react'

// Simulated patient cohort data based on our generated datasets
const samplePatients = [
  { id: 'P001', name: 'Maria Garcia', age: 67, gender: 'Female', payer: 'Medicare', conditions: ['Diabetes Type 2', 'Hypertension'], riskScore: 3.2, careGaps: 2 },
  { id: 'P002', name: 'James Wilson', age: 58, gender: 'Male', payer: 'Commercial', conditions: ['CHF', 'CKD Stage 3'], riskScore: 4.1, careGaps: 1 },
  { id: 'P003', name: 'Sarah Johnson', age: 45, gender: 'Female', payer: 'Commercial', conditions: ['Depression', 'Anxiety'], riskScore: 1.8, careGaps: 0 },
  { id: 'P004', name: 'Robert Chen', age: 72, gender: 'Male', payer: 'Medicare', conditions: ['COPD', 'Diabetes Type 2'], riskScore: 3.8, careGaps: 3 },
  { id: 'P005', name: 'Emily Brown', age: 34, gender: 'Female', payer: 'Medicaid', conditions: ['Asthma'], riskScore: 1.2, careGaps: 1 },
  { id: 'P006', name: 'Michael Davis', age: 63, gender: 'Male', payer: 'Medicare', conditions: ['Hypertension', 'Hyperlipidemia'], riskScore: 2.4, careGaps: 0 },
  { id: 'P007', name: 'Linda Martinez', age: 55, gender: 'Female', payer: 'Commercial', conditions: ['Breast Cancer (Remission)', 'Osteoporosis'], riskScore: 2.9, careGaps: 1 },
  { id: 'P008', name: 'William Thompson', age: 78, gender: 'Male', payer: 'Medicare', conditions: ['CHF', 'Atrial Fibrillation', 'CKD Stage 4'], riskScore: 4.8, careGaps: 2 },
  { id: 'P009', name: 'Jennifer Lee', age: 42, gender: 'Female', payer: 'Commercial', conditions: ['Migraine', 'Hypothyroidism'], riskScore: 1.5, careGaps: 0 },
  { id: 'P010', name: 'David Anderson', age: 69, gender: 'Male', payer: 'Medicare', conditions: ['Diabetes Type 2', 'Neuropathy'], riskScore: 3.4, careGaps: 2 }
]

const conditionCategories = [
  { name: 'Diabetes', count: 45000, icon: Activity, color: 'blue' },
  { name: 'Cardiovascular', count: 62000, icon: Heart, color: 'red' },
  { name: 'Behavioral Health', count: 28000, icon: Brain, color: 'purple' },
  { name: 'Respiratory', count: 35000, icon: Activity, color: 'cyan' },
  { name: 'Chronic Kidney', count: 18000, icon: Activity, color: 'orange' }
]

const payerDistribution = [
  { name: 'Medicare', percentage: 38, count: 114000 },
  { name: 'Commercial', percentage: 39, count: 117000 },
  { name: 'Medicaid', percentage: 18, count: 54000 },
  { name: 'Self-Pay', percentage: 5, count: 15000 }
]

const ageDistribution = [
  { range: '0-17', percentage: 12, count: 36000 },
  { range: '18-34', percentage: 18, count: 54000 },
  { range: '35-54', percentage: 26, count: 78000 },
  { range: '55-64', percentage: 20, count: 60000 },
  { range: '65-74', percentage: 14, count: 42000 },
  { range: '75+', percentage: 10, count: 30000 }
]

export default function ExplorerPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedPayer, setSelectedPayer] = useState<string>('all')
  const [selectedCondition, setSelectedCondition] = useState<string>('all')
  const [riskFilter, setRiskFilter] = useState<string>('all')
  const [careGapFilter, setCareGapFilter] = useState<boolean>(false)
  const [viewMode, setViewMode] = useState<'table' | 'cards'>('table')
  const [expandedPatient, setExpandedPatient] = useState<string | null>(null)

  // Filter patients based on criteria
  const filteredPatients = useMemo(() => {
    return samplePatients.filter(patient => {
      if (searchQuery && !patient.name.toLowerCase().includes(searchQuery.toLowerCase()) &&
          !patient.id.toLowerCase().includes(searchQuery.toLowerCase())) {
        return false
      }
      if (selectedPayer !== 'all' && patient.payer !== selectedPayer) return false
      if (selectedCondition !== 'all' && !patient.conditions.some(c =>
        c.toLowerCase().includes(selectedCondition.toLowerCase())
      )) return false
      if (riskFilter === 'high' && patient.riskScore < 3.5) return false
      if (riskFilter === 'medium' && (patient.riskScore < 2.0 || patient.riskScore >= 3.5)) return false
      if (riskFilter === 'low' && patient.riskScore >= 2.0) return false
      if (careGapFilter && patient.careGaps === 0) return false
      return true
    })
  }, [searchQuery, selectedPayer, selectedCondition, riskFilter, careGapFilter])

  const getRiskColor = (score: number) => {
    if (score >= 3.5) return 'text-red-600 bg-red-100'
    if (score >= 2.0) return 'text-yellow-600 bg-yellow-100'
    return 'text-green-600 bg-green-100'
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <PortalNav />

      <main className="pt-24 pb-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 mb-2">
                Interactive Data Explorer
              </h1>
              <p className="text-gray-600">
                Explore 300,000 synthetic patients across 3 customer segments
              </p>
            </div>
            <div className="mt-4 md:mt-0 flex items-center space-x-3">
              <span className="text-sm text-gray-500">
                Viewing sample of 300K patients
              </span>
              <button className="inline-flex items-center px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white hover:bg-gray-50">
                <RefreshCw className="w-4 h-4 mr-2" />
                Refresh Data
              </button>
            </div>
          </div>

          {/* Population Overview */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            {/* Payer Distribution */}
            <div className="bg-white rounded-xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-gray-900">Payer Mix</h3>
                <PieChart className="w-5 h-5 text-gray-400" />
              </div>
              <div className="space-y-3">
                {payerDistribution.map((payer) => (
                  <div key={payer.name}>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-600">{payer.name}</span>
                      <span className="font-medium">{payer.percentage}%</span>
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full ${
                          payer.name === 'Medicare' ? 'bg-blue-500' :
                          payer.name === 'Commercial' ? 'bg-green-500' :
                          payer.name === 'Medicaid' ? 'bg-purple-500' : 'bg-gray-400'
                        }`}
                        style={{ width: `${payer.percentage}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Age Distribution */}
            <div className="bg-white rounded-xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-gray-900">Age Distribution</h3>
                <BarChart3 className="w-5 h-5 text-gray-400" />
              </div>
              <div className="flex items-end justify-between h-32 px-2">
                {ageDistribution.map((age) => (
                  <div key={age.range} className="flex flex-col items-center">
                    <div
                      className="w-8 bg-blue-500 rounded-t"
                      style={{ height: `${age.percentage * 4}px` }}
                    />
                    <span className="text-xs text-gray-500 mt-2">{age.range}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Condition Categories */}
            <div className="bg-white rounded-xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-gray-900">Top Conditions</h3>
                <Activity className="w-5 h-5 text-gray-400" />
              </div>
              <div className="space-y-2">
                {conditionCategories.map((condition) => (
                  <div key={condition.name} className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">{condition.name}</span>
                    <span className="text-sm font-medium text-gray-900">
                      {(condition.count / 1000).toFixed(0)}K
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Filters Bar */}
          <div className="bg-white rounded-xl p-4 shadow-sm mb-6">
            <div className="flex flex-wrap items-center gap-4">
              {/* Search */}
              <div className="flex-1 min-w-[200px]">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search patients by name or ID..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Payer Filter */}
              <select
                value={selectedPayer}
                onChange={(e) => setSelectedPayer(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="all">All Payers</option>
                <option value="Medicare">Medicare</option>
                <option value="Commercial">Commercial</option>
                <option value="Medicaid">Medicaid</option>
              </select>

              {/* Condition Filter */}
              <select
                value={selectedCondition}
                onChange={(e) => setSelectedCondition(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="all">All Conditions</option>
                <option value="Diabetes">Diabetes</option>
                <option value="CHF">Heart Failure</option>
                <option value="COPD">COPD</option>
                <option value="CKD">Kidney Disease</option>
                <option value="Depression">Depression</option>
              </select>

              {/* Risk Filter */}
              <select
                value={riskFilter}
                onChange={(e) => setRiskFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="all">All Risk Levels</option>
                <option value="high">High Risk (3.5+)</option>
                <option value="medium">Medium Risk (2.0-3.5)</option>
                <option value="low">Low Risk (&lt;2.0)</option>
              </select>

              {/* Care Gap Toggle */}
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={careGapFilter}
                  onChange={(e) => setCareGapFilter(e.target.checked)}
                  className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                />
                <span className="text-sm text-gray-600">Has Care Gaps</span>
              </label>

              {/* View Toggle */}
              <div className="flex items-center border border-gray-300 rounded-lg overflow-hidden">
                <button
                  onClick={() => setViewMode('table')}
                  className={`p-2 ${viewMode === 'table' ? 'bg-blue-50 text-blue-600' : 'text-gray-400'}`}
                >
                  <Table className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setViewMode('cards')}
                  className={`p-2 ${viewMode === 'cards' ? 'bg-blue-50 text-blue-600' : 'text-gray-400'}`}
                >
                  <Users className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* Results Summary */}
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-gray-600">
              Showing <span className="font-semibold">{filteredPatients.length}</span> of 10 sample patients
              <span className="text-gray-400 ml-2">(300K total in full dataset)</span>
            </p>
            <button className="inline-flex items-center text-sm text-blue-600 hover:text-blue-700">
              <Download className="w-4 h-4 mr-1" />
              Export Results
            </button>
          </div>

          {/* Patient Table/Cards */}
          {viewMode === 'table' ? (
            <div className="bg-white rounded-xl shadow-sm overflow-hidden">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Patient</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Demographics</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Payer</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Conditions</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Risk Score</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Care Gaps</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {filteredPatients.map((patient) => (
                    <>
                      <tr key={patient.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4">
                          <div className="font-medium text-gray-900">{patient.name}</div>
                          <div className="text-xs text-gray-500">{patient.id}</div>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-600">
                          {patient.age}y, {patient.gender}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                            patient.payer === 'Medicare' ? 'bg-blue-100 text-blue-700' :
                            patient.payer === 'Commercial' ? 'bg-green-100 text-green-700' :
                            'bg-purple-100 text-purple-700'
                          }`}>
                            {patient.payer}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex flex-wrap gap-1">
                            {patient.conditions.slice(0, 2).map((condition, i) => (
                              <span key={i} className="inline-flex px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded">
                                {condition}
                              </span>
                            ))}
                            {patient.conditions.length > 2 && (
                              <span className="text-xs text-gray-400">+{patient.conditions.length - 2}</span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-center">
                          <span className={`inline-flex px-2 py-1 text-xs font-bold rounded ${getRiskColor(patient.riskScore)}`}>
                            {patient.riskScore.toFixed(1)}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-center">
                          {patient.careGaps > 0 ? (
                            <span className="inline-flex items-center text-orange-600">
                              <AlertCircle className="w-4 h-4 mr-1" />
                              {patient.careGaps}
                            </span>
                          ) : (
                            <span className="inline-flex items-center text-green-600">
                              <CheckCircle2 className="w-4 h-4" />
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => setExpandedPatient(expandedPatient === patient.id ? null : patient.id)}
                            className="text-blue-600 hover:text-blue-700"
                          >
                            {expandedPatient === patient.id ? (
                              <ChevronDown className="w-5 h-5" />
                            ) : (
                              <ChevronRight className="w-5 h-5" />
                            )}
                          </button>
                        </td>
                      </tr>
                      {expandedPatient === patient.id && (
                        <tr>
                          <td colSpan={7} className="px-6 py-4 bg-gray-50">
                            <div className="grid grid-cols-3 gap-6">
                              <div>
                                <h4 className="text-sm font-medium text-gray-900 mb-2">FHIR Resources</h4>
                                <ul className="text-sm text-gray-600 space-y-1">
                                  <li>Patient Resource: {patient.id}</li>
                                  <li>Conditions: {patient.conditions.length}</li>
                                  <li>Observations: 12 recent</li>
                                  <li>Encounters: 8 in last year</li>
                                </ul>
                              </div>
                              <div>
                                <h4 className="text-sm font-medium text-gray-900 mb-2">Care Gaps</h4>
                                {patient.careGaps > 0 ? (
                                  <ul className="text-sm text-orange-600 space-y-1">
                                    {patient.conditions.includes('Diabetes Type 2') && <li>HbA1c not done in 90 days</li>}
                                    {patient.age >= 50 && <li>Colorectal screening overdue</li>}
                                    {patient.riskScore > 3 && <li>Annual wellness visit needed</li>}
                                  </ul>
                                ) : (
                                  <p className="text-sm text-green-600">All care gaps closed</p>
                                )}
                              </div>
                              <div>
                                <h4 className="text-sm font-medium text-gray-900 mb-2">Actions</h4>
                                <div className="flex flex-wrap gap-2">
                                  <button className="px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700">
                                    View FHIR Bundle
                                  </button>
                                  <button className="px-3 py-1 text-xs border border-gray-300 rounded hover:bg-gray-50">
                                    Export JSON
                                  </button>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredPatients.map((patient) => (
                <div key={patient.id} className="bg-white rounded-xl p-6 shadow-sm">
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <h3 className="font-semibold text-gray-900">{patient.name}</h3>
                      <p className="text-sm text-gray-500">{patient.id} | {patient.age}y, {patient.gender}</p>
                    </div>
                    <span className={`px-2 py-1 text-xs font-bold rounded ${getRiskColor(patient.riskScore)}`}>
                      {patient.riskScore.toFixed(1)}
                    </span>
                  </div>

                  <div className="mb-4">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                      patient.payer === 'Medicare' ? 'bg-blue-100 text-blue-700' :
                      patient.payer === 'Commercial' ? 'bg-green-100 text-green-700' :
                      'bg-purple-100 text-purple-700'
                    }`}>
                      {patient.payer}
                    </span>
                  </div>

                  <div className="mb-4">
                    <p className="text-xs text-gray-500 mb-1">Conditions</p>
                    <div className="flex flex-wrap gap-1">
                      {patient.conditions.map((condition, i) => (
                        <span key={i} className="inline-flex px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded">
                          {condition}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                    <div className="flex items-center">
                      {patient.careGaps > 0 ? (
                        <span className="inline-flex items-center text-sm text-orange-600">
                          <AlertCircle className="w-4 h-4 mr-1" />
                          {patient.careGaps} care gaps
                        </span>
                      ) : (
                        <span className="inline-flex items-center text-sm text-green-600">
                          <CheckCircle2 className="w-4 h-4 mr-1" />
                          No gaps
                        </span>
                      )}
                    </div>
                    <button className="text-sm text-blue-600 hover:text-blue-700">
                      View Details
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* CTA Section */}
          <div className="mt-12 bg-gradient-to-r from-blue-600 to-blue-800 rounded-xl p-8 text-white text-center">
            <h2 className="text-2xl font-bold mb-2">Ready to explore your own data?</h2>
            <p className="text-blue-100 mb-6 max-w-2xl mx-auto">
              Schedule a demo to see HDIM process your patient population and identify care gaps in real-time.
            </p>
            <div className="flex justify-center gap-4">
              <a
                href="/demo"
                className="px-6 py-3 bg-white text-blue-600 font-medium rounded-lg hover:bg-blue-50 transition-colors"
              >
                Try Live Demo
              </a>
              <a
                href="/schedule"
                className="px-6 py-3 border border-white text-white font-medium rounded-lg hover:bg-white/10 transition-colors"
              >
                Schedule Demo
              </a>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
