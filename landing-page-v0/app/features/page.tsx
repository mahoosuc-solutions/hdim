import Image from 'next/image'
import Link from 'next/link'
import {
  Activity,
  BarChart3,
  Shield,
  Zap,
  Users,
  Clock,
  CheckCircle2,
  ArrowRight,
  Database,
  Brain,
  Lock,
  GitBranch
} from 'lucide-react'

export const metadata = {
  title: 'Features | HDIM - Healthcare Data in Motion',
  description: 'Explore HDIM\'s comprehensive features for quality measurement, care gap detection, HEDIS compliance, and population health management.',
}

const coreFeatures = [
  {
    icon: Activity,
    title: 'Real-Time Care Gap Detection',
    description: 'Identify care gaps the moment they occur with continuous FHIR data monitoring.',
    benefits: [
      'Sub-second measure evaluation',
      'Automated gap closure workflows',
      'Priority-based alerts',
      'Integration with EHR systems'
    ]
  },
  {
    icon: BarChart3,
    title: 'HEDIS Excellence',
    description: 'Complete HEDIS 2026 measure library with automated calculation and reporting.',
    benefits: [
      'Broad HEDIS measure coverage',
      'Automated data collection',
      'Audit-ready documentation',
      'CMS Star Ratings optimization'
    ]
  },
  {
    icon: Shield,
    title: 'HIPAA-First Architecture',
    description: 'Configurable PHI caching and compliance-aligned design controls.',
    benefits: [
      'Full audit trail',
      'Role-based access control',
      'Encryption at rest and in transit',
      'SOC 2-aligned controls'
    ]
  },
  {
    icon: Zap,
    title: 'FHIR-Native Platform',
    description: 'Built on HL7 FHIR R4 standards for seamless healthcare data interoperability.',
    benefits: [
      'Bi-directional EHR sync',
      'SMART on FHIR support',
      'Bulk data API (FHIR R4)',
      'Standard vocabularies (LOINC, SNOMED)'
    ]
  },
  {
    icon: Users,
    title: 'Population Health Management',
    description: 'Segment and manage patient cohorts with advanced analytics and predictive modeling.',
    benefits: [
      'Risk stratification (HCC-RAF)',
      'Predictive analytics',
      'Custom cohort builder',
      'Social determinants of health (SDOH)'
    ]
  },
  {
    icon: Brain,
    title: 'AI-Powered Insights',
    description: 'Clinical decision support powered by evidence-based algorithms and machine learning.',
    benefits: [
      'Automated measure evaluation',
      'Predictive care gap analysis',
      'Natural language processing',
      'Clinical recommendation engine'
    ]
  }
]

const platformCapabilities = [
  {
    category: 'Data Integration',
    icon: Database,
    features: [
      'HL7 FHIR R4 native',
      'HL7 v2 message processing',
      'C-CDA document parsing',
      'Claims data (837/835)',
      'Lab interfaces (HL7, LIS)',
      'Pharmacy data (NCPDP)'
    ]
  },
  {
    category: 'Quality Measurement',
    icon: BarChart3,
    features: [
      'HEDIS 2026 complete library',
      'CMS quality measures',
      'Custom measure builder',
      'CQL engine support',
      'Automated numerator/denominator',
      'Gap closure tracking'
    ]
  },
  {
    category: 'Security & Compliance',
    icon: Lock,
    features: [
      'HIPAA-aligned controls',
      'Configurable PHI cache policies',
      'Full audit logging',
      'Role-based access (RBAC)',
      'OAuth 2.0 / SMART on FHIR',
      'SOC 2-aligned controls'
    ]
  },
  {
    category: 'Workflows & Automation',
    icon: GitBranch,
    features: [
      'Care gap closure workflows',
      'Provider notifications',
      'Patient outreach automation',
      'Task assignment',
      'SLA tracking',
      'Escalation rules'
    ]
  }
]

const integrations = [
  { name: 'Epic', logo: '/logos/epic.png' },
  { name: 'Cerner', logo: '/logos/cerner.png' },
  { name: 'Allscripts', logo: '/logos/allscripts.png' },
  { name: 'Athenahealth', logo: '/logos/athena.png' },
]

export default function FeaturesPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 to-indigo-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h1 className="text-5xl font-bold text-gray-900 mb-6">
              Enterprise-Grade Platform<br />
              <span className="text-blue-600">Built for Quality Excellence</span>
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto mb-8">
              HDIM combines real-time care gap detection, comprehensive HEDIS support, and FHIR-native architecture 
              to help healthcare organizations achieve quality measurement excellence.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link
                href="/demo"
                className="inline-flex items-center justify-center px-8 py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
              >
                See It In Action
                <ArrowRight className="ml-2 w-5 h-5" />
              </Link>
              <Link
                href="/schedule"
                className="inline-flex items-center justify-center px-8 py-4 border-2 border-blue-600 text-blue-600 font-semibold rounded-lg hover:bg-blue-50 transition-colors"
              >
                Schedule Demo
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Core Features */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Core Capabilities
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Everything you need to close care gaps, optimize quality measures, and improve patient outcomes.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {coreFeatures.map((feature, idx) => (
              <div key={idx} className="bg-white border border-gray-200 rounded-xl p-8 hover:shadow-lg transition-shadow">
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mb-6">
                  <feature.icon className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3">
                  {feature.title}
                </h3>
                <p className="text-gray-600 mb-6">
                  {feature.description}
                </p>
                <ul className="space-y-2">
                  {feature.benefits.map((benefit, bidx) => (
                    <li key={bidx} className="flex items-start">
                      <CheckCircle2 className="w-5 h-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" />
                      <span className="text-gray-700">{benefit}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Platform Capabilities */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Complete Platform Capabilities
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Comprehensive features across data integration, quality measurement, security, and workflow automation.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8">
            {platformCapabilities.map((category, idx) => (
              <div key={idx} className="bg-white border border-gray-200 rounded-xl p-8">
                <div className="flex items-center mb-6">
                  <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mr-4">
                    <category.icon className="w-6 h-6 text-blue-600" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900">
                    {category.category}
                  </h3>
                </div>
                <ul className="grid grid-cols-2 gap-3">
                  {category.features.map((feature, fidx) => (
                    <li key={fidx} className="flex items-start">
                      <CheckCircle2 className="w-4 h-4 text-blue-600 mr-2 mt-0.5 flex-shrink-0" />
                      <span className="text-gray-700 text-sm">{feature}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-blue-600">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-4xl font-bold text-white mb-6">
            Ready to Transform Your Quality Measurement?
          </h2>
          <p className="text-xl text-blue-100 mb-8">
            See how HDIM can help you close care gaps faster and improve quality performance.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/demo"
              className="inline-flex items-center justify-center px-8 py-4 bg-white text-blue-600 font-semibold rounded-lg hover:bg-gray-100 transition-colors"
            >
              Request Demo
              <ArrowRight className="ml-2 w-5 h-5" />
            </Link>
            <Link
              href="/pricing"
              className="inline-flex items-center justify-center px-8 py-4 border-2 border-white text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
            >
              View Pricing
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
