import Image from 'next/image'
import Link from 'next/link'
import {
  Heart,
  Shield,
  Code,
  Users,
  Lightbulb,
  Target,
  Award,
  ArrowRight,
  CheckCircle2,
  Clock
} from 'lucide-react'

export const metadata = {
  title: 'About Us | HDIM - Healthcare Data in Motion',
  description: 'Learn about HDIM\'s mission to build healthcare software that saves lives through better quality measurement and care gap detection.',
}

const values = [
  {
    icon: Heart,
    title: 'Patient Safety Over Shortcuts',
    description: 'You can\'t "move fast and break things" when the things you break are people\'s lives.',
    commitment: 'Configurable PHI cache limits and audit-ready privacy controls.'
  },
  {
    icon: Shield,
    title: 'Standards Over Lock-in',
    description: 'We chose openness when we could have chosen control.',
    commitment: 'Built on HL7 FHIR R4, SMART on FHIR, and open healthcare standards.'
  },
  {
    icon: Code,
    title: 'Testing Over Speed',
    description: 'Untested code in healthcare is unethical.',
    commitment: 'Extensive test coverage on critical paths with measure validation workflows.'
  },
  {
    icon: Users,
    title: 'Evidence Over Opinion',
    description: 'Every clinical decision is backed by peer-reviewed research.',
    commitment: 'All quality measures derived from NCQA HEDIS, CMS, and clinical guidelines.'
  }
]

const story = {
  challenge: 'Healthcare organizations lose momentum in quality programs when data arrives late and workflows aren\'t built for how care actually happens.',
  decision: 'We chose to build a real-time execution layer that connects the clinical reality to the CMS ideals of better care, smarter spending, and healthier people.',
  difference: 'We chose harder engineering, privacy-by-design, and patient-first defaults. That\'s the difference — execution with integrity.'
}

const team = [
  {
    role: 'Engineering Excellence',
    description: 'Healthcare software engineers with experience at Epic, Cerner, and leading health systems. We\'ve seen what breaks in production at 3 AM - and we build to prevent it.',
  },
  {
    role: 'Clinical Expertise',
    description: 'Physicians, nurses, and quality measure specialists who understand the real-world impact of every feature we build.',
  },
  {
    role: 'Compliance & Security',
    description: 'Compliance and security professionals who make privacy, auditability, and risk management the foundation, not an afterthought.',
  }
]

const roadmap = [
  {
    quarter: 'Q1 2026 — Pilot Launch',
    status: 'in_progress' as const,
    milestones: [
      'FHIR R4 core platform with CQL-native measure execution',
      'HEDIS 2026 measure library (key measures)',
      'Care gap detection and closure workflows',
      'Multi-tenant security with HIPAA-aligned controls',
      'Observable SLO contracts with real-time performance monitoring',
      'Pilot customer onboarding and deployment',
    ]
  },
  {
    quarter: 'Q2 2026 — Clinical Depth',
    status: 'planned' as const,
    milestones: [
      'Full HEDIS 2026 measure library coverage',
      'Clinical portal: patient search, risk stratification, care gap management',
      'Provider notification and outreach automation',
      'EHR integrations: Epic, Cerner (bi-directional FHIR)',
      'Developer portal with API docs and sandbox',
      'SSO, MFA, and enterprise RBAC',
    ]
  },
  {
    quarter: 'Q3 2026 — Scale & Intelligence',
    status: 'planned' as const,
    milestones: [
      'AI-powered care gap prioritization (predictive, 30-60 day lead time)',
      'Population health cohort builder and analytics',
      'Claims data ingestion (837/835) and pharmacy data (NCPDP)',
      'QRDA I/III export for CMS quality reporting',
      'Social determinants of health (SDOH) integration',
      'Multi-region deployment and enterprise SLA tiers',
    ]
  },
  {
    quarter: 'Q4 2026 — Enterprise GA',
    status: 'planned' as const,
    milestones: [
      'General availability with full enterprise feature set',
      'CMS Stars optimization dashboards',
      'Custom measure builder for organization-specific measures',
      'Advanced analytics and ROI reporting',
      'Partner ecosystem and marketplace integrations',
      'Series A funded — accelerated roadmap',
    ]
  },
]

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 to-indigo-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="max-w-4xl mx-auto text-center">
            <h1 className="text-5xl font-bold text-gray-900 mb-6">
              We Build Healthcare Software<br />
              <span className="text-blue-600">As If Patients' Lives Depend On It</span>
            </h1>
            <p className="text-xl text-gray-600 mb-4">
              Because they do.
            </p>
            <p className="text-lg text-gray-600 max-w-3xl mx-auto">
              HDIM aims to be the ultimate healthcare execution platform for the CMS ideals of better care, smarter spending, and healthier people.
            </p>
          </div>
        </div>
      </section>

      {/* Our Story */}
      <section className="py-20 bg-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              The HDIM Story
            </h2>
            <p className="text-xl text-gray-600">
              Built by people who care.
            </p>
          </div>

          <div className="space-y-8">
            <div className="bg-gray-50 rounded-xl p-8">
              <h3 className="text-2xl font-bold text-gray-900 mb-4">The Challenge</h3>
              <p className="text-lg text-gray-700 leading-relaxed">
                {story.challenge}
              </p>
            </div>

            <div className="bg-blue-50 rounded-xl p-8">
              <h3 className="text-2xl font-bold text-gray-900 mb-4">The Decision</h3>
              <p className="text-lg text-gray-700 leading-relaxed mb-4">
                {story.decision}
              </p>
              <div className="bg-white border-l-4 border-blue-600 p-4 rounded">
                <code className="text-sm text-gray-800 font-mono">
                  commit refactor(privacy): Reduce PHI cache TTL to limit exposure
                </code>
              </div>
            </div>

            <div className="bg-gradient-to-br from-gray-900 to-gray-800 text-white rounded-xl p-8">
              <h3 className="text-2xl font-bold mb-4">The Difference</h3>
              <p className="text-lg leading-relaxed">
                {story.difference}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Our Values */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Our Values
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              The Four Non-Negotiables
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8">
            {values.map((value, idx) => (
              <div key={idx} className="bg-white border border-gray-200 rounded-xl p-8">
                <div className="flex items-start mb-4">
                  <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mr-4 flex-shrink-0">
                    <value.icon className="w-6 h-6 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="text-2xl font-bold text-gray-900 mb-2">
                      {value.title}
                    </h3>
                    <p className="text-gray-600 italic mb-4">
                      "{value.description}"
                    </p>
                    <p className="text-gray-700 font-medium">
                      {value.commitment}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Our Team */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Built By Experts Who Care
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Our team combines deep healthcare expertise with world-class engineering.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {team.map((member, idx) => (
              <div key={idx} className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-8">
                <h3 className="text-xl font-bold text-gray-900 mb-4">
                  {member.role}
                </h3>
                <p className="text-gray-700">
                  {member.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Roadmap */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Product Roadmap
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Our path from pilot to enterprise general availability. Built iteratively with customer feedback at every stage.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            {roadmap.map((phase, idx) => (
              <div
                key={idx}
                className={`bg-white rounded-xl p-6 border-2 ${
                  phase.status === 'in_progress'
                    ? 'border-blue-500 shadow-md'
                    : 'border-gray-200'
                }`}
              >
                <div className="flex items-center gap-3 mb-4">
                  {phase.status === 'in_progress' ? (
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-700 bg-blue-100 px-2.5 py-1 rounded-full">
                      <span className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-pulse" />
                      In Progress
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-500 bg-gray-100 px-2.5 py-1 rounded-full">
                      <Clock className="w-3 h-3" />
                      Planned
                    </span>
                  )}
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-4">
                  {phase.quarter}
                </h3>
                <ul className="space-y-2.5">
                  {phase.milestones.map((milestone, midx) => (
                    <li key={midx} className="flex items-start gap-2 text-sm text-gray-700">
                      <CheckCircle2 className={`w-4 h-4 mt-0.5 flex-shrink-0 ${
                        phase.status === 'in_progress' ? 'text-blue-500' : 'text-gray-300'
                      }`} />
                      {milestone}
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
            Join Us in Building Better Healthcare
          </h2>
          <p className="text-xl text-blue-100 mb-8">
            See how HDIM can help your organization achieve quality measurement excellence.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/schedule"
              className="inline-flex items-center justify-center px-8 py-4 bg-white text-blue-600 font-semibold rounded-lg hover:bg-gray-100 transition-colors"
            >
              Schedule a Demo
              <ArrowRight className="ml-2 w-5 h-5" />
            </Link>
            <Link
              href="/contact"
              className="inline-flex items-center justify-center px-8 py-4 border-2 border-white text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
            >
              Contact Us
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
