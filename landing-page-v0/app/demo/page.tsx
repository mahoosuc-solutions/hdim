'use client'

import { useState } from 'react'
import PortalNav from '../../components/PortalNav'
import {
  Play,
  Building2,
  Users,
  Shield,
  Clock,
  CheckCircle2,
  ArrowRight,
  Activity,
  Heart,
  Brain,
  AlertCircle,
  Loader2,
  ExternalLink,
  BarChart3,
  FileText
} from 'lucide-react'

const demoScenarios = [
  {
    id: 'diabetes-care',
    title: 'Diabetes Care Management',
    description: 'Track HbA1c control, identify gaps in diabetic care, and trigger automated outreach',
    duration: '15 min',
    icon: Activity,
    color: 'blue',
    measures: ['CDC-HBA1C Control', 'Eye Exam', 'Nephropathy Monitoring'],
    outcomes: {
      gapsIdentified: 4200,
      gapsClosed: 3150,
      savingsPerPatient: 850
    }
  },
  {
    id: 'cardiac-risk',
    title: 'Cardiac Risk Stratification',
    description: 'Identify high-risk CHF patients, predict readmissions, and optimize interventions',
    duration: '20 min',
    icon: Heart,
    color: 'red',
    measures: ['Blood Pressure Control', 'Statin Therapy', 'Readmission Prevention'],
    outcomes: {
      gapsIdentified: 5800,
      gapsClosed: 4350,
      savingsPerPatient: 1200
    }
  },
  {
    id: 'behavioral-health',
    title: 'Behavioral Health Integration',
    description: 'Screen for depression, coordinate care transitions, ensure medication adherence',
    duration: '18 min',
    icon: Brain,
    color: 'purple',
    measures: ['PHQ-9 Screening', 'Antidepressant Management', 'Follow-Up After ED'],
    outcomes: {
      gapsIdentified: 3500,
      gapsClosed: 2625,
      savingsPerPatient: 640
    }
  },
  {
    id: 'preventive-care',
    title: 'Preventive Care Optimization',
    description: 'Maximize preventive screenings, immunizations, and annual wellness visits',
    duration: '12 min',
    icon: Shield,
    color: 'green',
    measures: ['Breast Cancer Screening', 'Colorectal Screening', 'Immunizations'],
    outcomes: {
      gapsIdentified: 8200,
      gapsClosed: 6560,
      savingsPerPatient: 320
    }
  },
  {
    id: 'care-transitions',
    title: 'Care Transitions & Readmissions',
    description: 'Manage post-discharge follow-up, medication reconciliation, and 30-day readmission prevention',
    duration: '22 min',
    icon: ArrowRight,
    color: 'orange',
    measures: ['Transitions of Care', 'Medication Reconciliation', '30-Day Readmissions'],
    outcomes: {
      gapsIdentified: 2800,
      gapsClosed: 2240,
      savingsPerPatient: 2100
    }
  }
]

const customerTypes = [
  {
    id: 'hospital',
    name: 'Hospital / Health System',
    icon: Building2,
    patients: '100,000',
    description: 'Enterprise hospital with acute and chronic care mix',
    features: ['Readmission prevention', 'Quality reporting', 'Care coordination']
  },
  {
    id: 'provider',
    name: 'Provider Practice',
    icon: Users,
    patients: '100,000',
    description: 'Multi-specialty outpatient practice',
    features: ['MIPS optimization', 'AWV scheduling', 'Chronic care management']
  },
  {
    id: 'health-plan',
    name: 'Health Plan',
    icon: Shield,
    patients: '100,000',
    description: 'Regional health plan with Stars focus',
    features: ['Stars improvement', 'Risk adjustment', 'Member engagement']
  }
]

export default function DemoPage() {
  const [selectedCustomerType, setSelectedCustomerType] = useState<string | null>(null)
  const [selectedScenario, setSelectedScenario] = useState<string | null>(null)
  const [demoState, setDemoState] = useState<'select' | 'loading' | 'ready'>('select')
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    company: ''
  })

  const handleStartDemo = () => {
    if (!selectedCustomerType || !selectedScenario) return

    setDemoState('loading')
    // Simulate demo environment provisioning
    setTimeout(() => {
      setDemoState('ready')
    }, 3000)
  }

  const getColorClasses = (color: string) => {
    const colors: Record<string, string> = {
      blue: 'bg-blue-100 text-blue-600 border-blue-200',
      red: 'bg-red-100 text-red-600 border-red-200',
      purple: 'bg-purple-100 text-purple-600 border-purple-200',
      green: 'bg-green-100 text-green-600 border-green-200',
      orange: 'bg-orange-100 text-orange-600 border-orange-200'
    }
    return colors[color] || colors.blue
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <PortalNav />

      <main className="pt-24 pb-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Live Demo Environment
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Experience HDIM with real synthetic data. Select your customer type and demo scenario
              to see care gap detection, quality measures, and ROI in action.
            </p>
          </div>

          {demoState === 'select' && (
            <>
              {/* Step 1: Customer Type */}
              <div className="mb-12">
                <div className="flex items-center mb-6">
                  <div className="w-8 h-8 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold text-sm mr-3">
                    1
                  </div>
                  <h2 className="text-xl font-semibold text-gray-900">Select Your Customer Type</h2>
                </div>

                <div className="grid md:grid-cols-3 gap-6">
                  {customerTypes.map((type) => {
                    const Icon = type.icon
                    const isSelected = selectedCustomerType === type.id
                    return (
                      <button
                        key={type.id}
                        onClick={() => setSelectedCustomerType(type.id)}
                        className={`text-left bg-white rounded-xl p-6 shadow-sm border-2 transition-all ${
                          isSelected
                            ? 'border-blue-500 ring-2 ring-blue-200'
                            : 'border-transparent hover:border-gray-200'
                        }`}
                      >
                        <div className="flex items-start justify-between mb-4">
                          <div className={`p-3 rounded-lg ${
                            type.id === 'hospital' ? 'bg-blue-50 text-blue-600' :
                            type.id === 'provider' ? 'bg-green-50 text-green-600' :
                            'bg-purple-50 text-purple-600'
                          }`}>
                            <Icon className="w-6 h-6" />
                          </div>
                          {isSelected && (
                            <CheckCircle2 className="w-6 h-6 text-blue-600" />
                          )}
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-1">
                          {type.name}
                        </h3>
                        <p className="text-sm text-gray-600 mb-3">
                          {type.description}
                        </p>
                        <div className="text-sm text-gray-500 mb-3">
                          {type.patients} sample patients
                        </div>
                        <ul className="space-y-1">
                          {type.features.map((feature, i) => (
                            <li key={i} className="flex items-center text-sm text-gray-600">
                              <CheckCircle2 className="w-3 h-3 text-green-500 mr-2" />
                              {feature}
                            </li>
                          ))}
                        </ul>
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Step 2: Scenario Selection */}
              <div className="mb-12">
                <div className="flex items-center mb-6">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm mr-3 ${
                    selectedCustomerType ? 'bg-blue-600 text-white' : 'bg-gray-300 text-gray-500'
                  }`}>
                    2
                  </div>
                  <h2 className={`text-xl font-semibold ${selectedCustomerType ? 'text-gray-900' : 'text-gray-400'}`}>
                    Choose a Demo Scenario
                  </h2>
                </div>

                <div className={`grid md:grid-cols-2 lg:grid-cols-3 gap-4 ${!selectedCustomerType ? 'opacity-50 pointer-events-none' : ''}`}>
                  {demoScenarios.map((scenario) => {
                    const Icon = scenario.icon
                    const isSelected = selectedScenario === scenario.id
                    return (
                      <button
                        key={scenario.id}
                        onClick={() => setSelectedScenario(scenario.id)}
                        disabled={!selectedCustomerType}
                        className={`text-left bg-white rounded-xl p-5 shadow-sm border-2 transition-all ${
                          isSelected
                            ? 'border-blue-500 ring-2 ring-blue-200'
                            : 'border-transparent hover:border-gray-200'
                        }`}
                      >
                        <div className="flex items-start justify-between mb-3">
                          <div className={`p-2 rounded-lg ${getColorClasses(scenario.color)}`}>
                            <Icon className="w-5 h-5" />
                          </div>
                          <div className="flex items-center text-xs text-gray-500">
                            <Clock className="w-3 h-3 mr-1" />
                            {scenario.duration}
                          </div>
                        </div>
                        <h3 className="font-semibold text-gray-900 mb-1">
                          {scenario.title}
                        </h3>
                        <p className="text-sm text-gray-600 mb-3">
                          {scenario.description}
                        </p>
                        <div className="flex flex-wrap gap-1">
                          {scenario.measures.map((measure, i) => (
                            <span key={i} className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                              {measure}
                            </span>
                          ))}
                        </div>
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Expected Outcomes Preview */}
              {selectedScenario && (
                <div className="bg-white rounded-xl p-6 shadow-sm mb-8">
                  <h3 className="font-semibold text-gray-900 mb-4">Expected Demo Outcomes</h3>
                  <div className="grid grid-cols-3 gap-6">
                    {(() => {
                      const scenario = demoScenarios.find(s => s.id === selectedScenario)
                      if (!scenario) return null
                      return (
                        <>
                          <div className="text-center">
                            <div className="text-3xl font-bold text-orange-600">
                              {scenario.outcomes.gapsIdentified.toLocaleString()}
                            </div>
                            <div className="text-sm text-gray-600">Care Gaps Identified</div>
                          </div>
                          <div className="text-center">
                            <div className="text-3xl font-bold text-green-600">
                              {scenario.outcomes.gapsClosed.toLocaleString()}
                            </div>
                            <div className="text-sm text-gray-600">Gaps Closable</div>
                          </div>
                          <div className="text-center">
                            <div className="text-3xl font-bold text-blue-600">
                              ${scenario.outcomes.savingsPerPatient}
                            </div>
                            <div className="text-sm text-gray-600">Savings Per Patient</div>
                          </div>
                        </>
                      )
                    })()}
                  </div>
                </div>
              )}

              {/* Quick Registration */}
              <div className="max-w-xl mx-auto bg-white rounded-xl p-6 shadow-sm">
                <h3 className="font-semibold text-gray-900 mb-4 text-center">
                  Quick Registration (Optional)
                </h3>
                <p className="text-sm text-gray-600 text-center mb-4">
                  Provide your info to save demo results and receive a summary report
                </p>
                <div className="grid md:grid-cols-3 gap-4 mb-6">
                  <input
                    type="text"
                    placeholder="Your name"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                  <input
                    type="email"
                    placeholder="Work email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                  <input
                    type="text"
                    placeholder="Company"
                    value={formData.company}
                    onChange={(e) => setFormData({ ...formData, company: e.target.value })}
                    className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <button
                  onClick={handleStartDemo}
                  disabled={!selectedCustomerType || !selectedScenario}
                  className={`w-full flex items-center justify-center px-6 py-3 rounded-lg font-medium transition-colors ${
                    selectedCustomerType && selectedScenario
                      ? 'bg-blue-600 text-white hover:bg-blue-700'
                      : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  }`}
                >
                  <Play className="w-5 h-5 mr-2" />
                  Launch Demo Environment
                </button>
              </div>
            </>
          )}

          {demoState === 'loading' && (
            <div className="text-center py-20">
              <div className="inline-flex items-center justify-center w-20 h-20 bg-blue-100 rounded-full mb-6">
                <Loader2 className="w-10 h-10 text-blue-600 animate-spin" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Provisioning Your Demo Environment
              </h2>
              <p className="text-gray-600 mb-8 max-w-md mx-auto">
                Loading {customerTypes.find(c => c.id === selectedCustomerType)?.patients} patients
                and preparing the {demoScenarios.find(s => s.id === selectedScenario)?.title} scenario...
              </p>
              <div className="max-w-md mx-auto">
                <div className="flex items-center justify-between text-sm text-gray-600 mb-2">
                  <span>Progress</span>
                  <span>Processing...</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div className="bg-blue-600 h-2 rounded-full animate-pulse" style={{ width: '60%' }} />
                </div>
              </div>
            </div>
          )}

          {demoState === 'ready' && (
            <div className="text-center py-12">
              <div className="inline-flex items-center justify-center w-20 h-20 bg-green-100 rounded-full mb-6">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Demo Environment Ready!
              </h2>
              <p className="text-gray-600 mb-8 max-w-md mx-auto">
                Your personalized demo is ready. Click below to launch the interactive demo.
              </p>

              <div className="max-w-2xl mx-auto bg-white rounded-xl p-8 shadow-sm mb-8">
                <div className="grid md:grid-cols-2 gap-6 mb-6">
                  <div className="text-left">
                    <h3 className="font-semibold text-gray-900 mb-3">Demo Configuration</h3>
                    <ul className="space-y-2 text-sm text-gray-600">
                      <li className="flex items-center">
                        <Building2 className="w-4 h-4 text-blue-600 mr-2" />
                        {customerTypes.find(c => c.id === selectedCustomerType)?.name}
                      </li>
                      <li className="flex items-center">
                        <Users className="w-4 h-4 text-blue-600 mr-2" />
                        {customerTypes.find(c => c.id === selectedCustomerType)?.patients} patients loaded
                      </li>
                      <li className="flex items-center">
                        <Activity className="w-4 h-4 text-blue-600 mr-2" />
                        {demoScenarios.find(s => s.id === selectedScenario)?.title}
                      </li>
                      <li className="flex items-center">
                        <Clock className="w-4 h-4 text-blue-600 mr-2" />
                        Session expires in 30 minutes
                      </li>
                    </ul>
                  </div>
                  <div className="text-left">
                    <h3 className="font-semibold text-gray-900 mb-3">What You'll See</h3>
                    <ul className="space-y-2 text-sm text-gray-600">
                      <li className="flex items-center">
                        <CheckCircle2 className="w-4 h-4 text-green-600 mr-2" />
                        Real-time care gap detection
                      </li>
                      <li className="flex items-center">
                        <CheckCircle2 className="w-4 h-4 text-green-600 mr-2" />
                        HEDIS measure evaluation
                      </li>
                      <li className="flex items-center">
                        <CheckCircle2 className="w-4 h-4 text-green-600 mr-2" />
                        Patient risk stratification
                      </li>
                      <li className="flex items-center">
                        <CheckCircle2 className="w-4 h-4 text-green-600 mr-2" />
                        ROI and savings projections
                      </li>
                    </ul>
                  </div>
                </div>

                <div className="flex justify-center gap-4">
                  <a
                    href="http://localhost:4200"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    <ExternalLink className="w-5 h-5 mr-2" />
                    Open Clinical Portal
                  </a>
                  <button
                    onClick={() => {
                      setDemoState('select')
                      setSelectedCustomerType(null)
                      setSelectedScenario(null)
                    }}
                    className="px-6 py-3 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Start Over
                  </button>
                </div>
              </div>

              {/* Demo Tips */}
              <div className="max-w-2xl mx-auto bg-blue-50 rounded-xl p-6 text-left">
                <h3 className="font-semibold text-blue-900 mb-3">Demo Tips</h3>
                <ul className="space-y-2 text-sm text-blue-800">
                  <li className="flex items-start">
                    <AlertCircle className="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                    <span>This is a sandboxed environment with synthetic data. No real PHI is involved.</span>
                  </li>
                  <li className="flex items-start">
                    <BarChart3 className="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                    <span>Use the filters to segment patients by payer, condition, or risk level.</span>
                  </li>
                  <li className="flex items-start">
                    <FileText className="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                    <span>Export reports to see how HDIM can integrate with your reporting workflows.</span>
                  </li>
                </ul>
              </div>
            </div>
          )}

          {/* FAQ Section */}
          <div className="mt-16 bg-white rounded-xl p-8 shadow-sm">
            <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">
              Demo FAQ
            </h2>
            <div className="grid md:grid-cols-2 gap-6 max-w-4xl mx-auto">
              <div>
                <h4 className="font-semibold text-gray-900 mb-2">Is this real patient data?</h4>
                <p className="text-sm text-gray-600">
                  No. All data is 100% synthetic and HIPAA-safe. It's designed to be realistic for evaluation purposes.
                </p>
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 mb-2">How long do demo sessions last?</h4>
                <p className="text-sm text-gray-600">
                  Demo sessions are available for 30 minutes. You can request extended access by contacting sales.
                </p>
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 mb-2">Can I use my own data?</h4>
                <p className="text-sm text-gray-600">
                  For POC with your data, contact our sales team to arrange a secure sandbox environment.
                </p>
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 mb-2">What infrastructure is required?</h4>
                <p className="text-sm text-gray-600">
                  HDIM is cloud-native and runs on any container platform. On-premise deployments are also available.
                </p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
