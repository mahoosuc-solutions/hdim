'use client'

import { useState } from 'react'
import PortalNav from '../../components/PortalNav'
import {
  Download,
  FileJson,
  Database,
  Users,
  Building2,
  Shield,
  CheckCircle2,
  ArrowRight,
  Lock
} from 'lucide-react'

const datasets = [
  {
    id: 'hospital',
    name: 'Academic Medical Center',
    type: 'Hospital',
    icon: Building2,
    description: 'Enterprise hospital dataset with acute and chronic care focus',
    patients: '100,000',
    resources: '400,000',
    size: '433 MB',
    formats: ['FHIR R4 Bundle', 'NDJSON'],
    highlights: [
      'Medicare 45%, Commercial 35%, Medicaid 15%',
      'Conditions: Diabetes, CHF, COPD, CKD, Depression',
      'Observations: BP, BMI, Labs',
      'Age distribution: 12% pediatric to 16% 75+'
    ],
    color: 'blue'
  },
  {
    id: 'provider',
    name: 'Large Multi-Specialty Practice',
    type: 'Provider',
    icon: Users,
    description: 'Multi-specialty group with outpatient care focus',
    patients: '100,000',
    resources: '400,000',
    size: '431 MB',
    formats: ['FHIR R4 Bundle', 'NDJSON'],
    highlights: [
      'Commercial 48%, Medicare 35%, Medicaid 12%',
      'Conditions: HTN, Hyperlipidemia, Anxiety, Back Pain',
      'MIPS quality measure ready',
      'Realistic ambulatory utilization patterns'
    ],
    color: 'green'
  },
  {
    id: 'healthplan',
    name: 'Regional Health Plan',
    type: 'Payer',
    icon: Shield,
    description: 'Health plan population with Stars measure focus',
    patients: '100,000',
    resources: '400,000',
    size: '430 MB',
    formats: ['FHIR R4 Bundle', 'NDJSON'],
    highlights: [
      'Commercial 50%, Medicare 28%, Medicaid 22%',
      'Broad condition mix for population health',
      'Stars measure denominator ready',
      'Care gap baseline data included'
    ],
    color: 'purple'
  }
]

const sampleSizes = [
  { label: '100 patients', size: '~500 KB', free: true },
  { label: '1,000 patients', size: '~5 MB', free: true },
  { label: '10,000 patients', size: '~50 MB', free: false },
  { label: '100,000 patients', size: '~430 MB', free: false },
]

export default function DownloadsPage() {
  const [selectedDataset, setSelectedDataset] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    company: '',
    role: '',
    useCase: ''
  })
  const [submitted, setSubmitted] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // In production, this would submit to your CRM/backend
    console.log('Lead captured:', formData, 'Dataset:', selectedDataset)
    setSubmitted(true)
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <PortalNav />

      <main className="pt-24 pb-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Sample FHIR Datasets
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Download realistic, synthetic patient data to validate your HDIM integration.
              All datasets are HIPAA-safe synthetic data generated for testing purposes.
            </p>
          </div>

          {/* Stats Bar */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
            <div className="bg-white rounded-xl p-4 text-center shadow-sm">
              <div className="text-3xl font-bold text-blue-600">300K</div>
              <div className="text-sm text-gray-600">Total Patients</div>
            </div>
            <div className="bg-white rounded-xl p-4 text-center shadow-sm">
              <div className="text-3xl font-bold text-green-600">1.2M</div>
              <div className="text-sm text-gray-600">FHIR Resources</div>
            </div>
            <div className="bg-white rounded-xl p-4 text-center shadow-sm">
              <div className="text-3xl font-bold text-purple-600">3</div>
              <div className="text-sm text-gray-600">Customer Types</div>
            </div>
            <div className="bg-white rounded-xl p-4 text-center shadow-sm">
              <div className="text-3xl font-bold text-orange-600">1.3 GB</div>
              <div className="text-sm text-gray-600">Total Data</div>
            </div>
          </div>

          {/* Dataset Cards */}
          <div className="grid md:grid-cols-3 gap-6 mb-12">
            {datasets.map((dataset) => {
              const Icon = dataset.icon
              const colorClasses = {
                blue: 'bg-blue-50 text-blue-600 border-blue-200',
                green: 'bg-green-50 text-green-600 border-green-200',
                purple: 'bg-purple-50 text-purple-600 border-purple-200'
              }
              const isSelected = selectedDataset === dataset.id

              return (
                <div
                  key={dataset.id}
                  onClick={() => setSelectedDataset(dataset.id)}
                  className={`bg-white rounded-xl p-6 shadow-sm border-2 cursor-pointer transition-all ${
                    isSelected ? 'border-blue-500 ring-2 ring-blue-200' : 'border-transparent hover:border-gray-200'
                  }`}
                >
                  <div className="flex items-start justify-between mb-4">
                    <div className={`p-3 rounded-lg ${colorClasses[dataset.color as keyof typeof colorClasses]}`}>
                      <Icon className="w-6 h-6" />
                    </div>
                    <span className="text-xs font-medium text-gray-500 bg-gray-100 px-2 py-1 rounded">
                      {dataset.type}
                    </span>
                  </div>

                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    {dataset.name}
                  </h3>
                  <p className="text-sm text-gray-600 mb-4">
                    {dataset.description}
                  </p>

                  <div className="grid grid-cols-3 gap-2 mb-4 text-center">
                    <div className="bg-gray-50 rounded-lg p-2">
                      <div className="text-sm font-semibold text-gray-900">{dataset.patients}</div>
                      <div className="text-xs text-gray-500">Patients</div>
                    </div>
                    <div className="bg-gray-50 rounded-lg p-2">
                      <div className="text-sm font-semibold text-gray-900">{dataset.resources}</div>
                      <div className="text-xs text-gray-500">Resources</div>
                    </div>
                    <div className="bg-gray-50 rounded-lg p-2">
                      <div className="text-sm font-semibold text-gray-900">{dataset.size}</div>
                      <div className="text-xs text-gray-500">Size</div>
                    </div>
                  </div>

                  <ul className="space-y-1 mb-4">
                    {dataset.highlights.slice(0, 3).map((highlight, i) => (
                      <li key={i} className="flex items-start text-xs text-gray-600">
                        <CheckCircle2 className="w-3 h-3 text-green-500 mr-1.5 mt-0.5 flex-shrink-0" />
                        {highlight}
                      </li>
                    ))}
                  </ul>

                  <div className="flex items-center text-xs text-gray-500">
                    <FileJson className="w-3 h-3 mr-1" />
                    {dataset.formats.join(', ')}
                  </div>
                </div>
              )
            })}
          </div>

          {/* Download Form */}
          {selectedDataset && !submitted && (
            <div className="max-w-2xl mx-auto bg-white rounded-xl shadow-sm p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Download {datasets.find(d => d.id === selectedDataset)?.name}
              </h2>
              <p className="text-gray-600 mb-6">
                Complete the form below to access your sample dataset.
              </p>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Full Name *
                    </label>
                    <input
                      type="text"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Work Email *
                    </label>
                    <input
                      type="email"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Company *
                    </label>
                    <input
                      type="text"
                      required
                      value={formData.company}
                      onChange={(e) => setFormData({ ...formData, company: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Role
                    </label>
                    <select
                      value={formData.role}
                      onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                      <option value="">Select role...</option>
                      <option value="executive">Executive / C-Suite</option>
                      <option value="it">IT / Engineering</option>
                      <option value="clinical">Clinical / Quality</option>
                      <option value="analytics">Analytics / Data</option>
                      <option value="other">Other</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    What will you use this data for?
                  </label>
                  <textarea
                    value={formData.useCase}
                    onChange={(e) => setFormData({ ...formData, useCase: e.target.value })}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="e.g., Integration testing, POC development, HEDIS evaluation..."
                  />
                </div>

                <div className="bg-gray-50 rounded-lg p-4">
                  <h4 className="font-medium text-gray-900 mb-2">Select Sample Size</h4>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                    {sampleSizes.map((size) => (
                      <label
                        key={size.label}
                        className={`flex items-center p-3 border rounded-lg cursor-pointer transition-colors ${
                          size.free ? 'hover:border-blue-300 hover:bg-blue-50' : 'opacity-60'
                        }`}
                      >
                        <input
                          type="radio"
                          name="sampleSize"
                          disabled={!size.free}
                          className="mr-2"
                        />
                        <div>
                          <div className="text-sm font-medium">{size.label}</div>
                          <div className="text-xs text-gray-500">{size.size}</div>
                        </div>
                        {!size.free && <Lock className="w-3 h-3 ml-auto text-gray-400" />}
                      </label>
                    ))}
                  </div>
                  <p className="text-xs text-gray-500 mt-2">
                    * Full datasets available after sales consultation
                  </p>
                </div>

                <button
                  type="submit"
                  className="w-full flex items-center justify-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors"
                >
                  <Download className="w-5 h-5 mr-2" />
                  Get Sample Data
                  <ArrowRight className="w-5 h-5 ml-2" />
                </button>
              </form>
            </div>
          )}

          {/* Success State */}
          {submitted && (
            <div className="max-w-2xl mx-auto bg-white rounded-xl shadow-sm p-8 text-center">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <CheckCircle2 className="w-8 h-8 text-green-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Download Ready!
              </h2>
              <p className="text-gray-600 mb-6">
                Your sample dataset is ready. Check your email for the download link.
              </p>
              <div className="space-y-3">
                <a
                  href="#"
                  className="block w-full px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors"
                >
                  <Download className="w-5 h-5 inline mr-2" />
                  Download 1,000 Patient Sample (FHIR R4)
                </a>
                <button
                  onClick={() => {
                    setSubmitted(false)
                    setSelectedDataset(null)
                  }}
                  className="text-blue-600 hover:text-blue-700 text-sm"
                >
                  ← Download a different dataset
                </button>
              </div>
            </div>
          )}

          {/* Data Quality Section */}
          <div className="mt-16 bg-white rounded-xl p-8 shadow-sm">
            <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">
              Data Quality & Compliance
            </h2>
            <div className="grid md:grid-cols-3 gap-6">
              <div className="text-center">
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                  <FileJson className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="font-semibold text-gray-900 mb-2">FHIR R4 Compliant</h3>
                <p className="text-sm text-gray-600">
                  All resources validated against US Core Implementation Guide
                </p>
              </div>
              <div className="text-center">
                <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                  <Shield className="w-6 h-6 text-green-600" />
                </div>
                <h3 className="font-semibold text-gray-900 mb-2">100% Synthetic</h3>
                <p className="text-sm text-gray-600">
                  No real patient data - safe for testing and demos
                </p>
              </div>
              <div className="text-center">
                <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                  <Database className="w-6 h-6 text-purple-600" />
                </div>
                <h3 className="font-semibold text-gray-900 mb-2">Realistic Distributions</h3>
                <p className="text-sm text-gray-600">
                  Demographics and conditions match US healthcare patterns
                </p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
