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
  ArrowRight
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
    commitment: '5-minute PHI cache maximum - we chose compliance when no one was watching.'
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
    commitment: '100% test coverage on critical paths. Every measure validated against HEDIS specs.'
  },
  {
    icon: Users,
    title: 'Evidence Over Opinion',
    description: 'Every clinical decision is backed by peer-reviewed research.',
    commitment: 'All quality measures derived from NCQA HEDIS, CMS, and clinical guidelines.'
  }
]

const story = {
  challenge: 'Healthcare organizations lose millions in quality bonuses every year. Not because they don\'t care - but because their tools weren\'t built for the humans who depend on them.',
  decision: 'On December 27, 2025, at 10:31 PM, we made a choice. We could have kept the 24-hour cache. Faster performance. Smoother demos. Every competitor does it. Instead, we wrote 22 words that define who we are: "fix(hipaa): Reduce PHI cache TTL to ≤5min for HIPAA compliance"',
  difference: 'We chose slower performance and better patient privacy. We chose harder engineering and audit-ready architecture. We chose the right thing. That\'s the difference. We chose compliance when no one was watching.'
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
    description: 'HIPAA, HITRUST, and SOC 2 experts who make security and compliance the foundation, not an afterthought.',
  }
]

const milestones = [
  {
    year: '2024',
    title: 'Foundation',
    description: 'HDIM founded with mission to bring evidence-based quality measurement to healthcare.',
  },
  {
    year: '2025',
    title: 'Platform Launch',
    description: 'Launched FHIR-native platform with real-time care gap detection and HEDIS 2025 support.',
  },
  {
    year: '2026',
    title: 'Enterprise Growth',
    description: 'Serving health plans, ACOs, and health systems across the United States.',
  }
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
              A world where no patient falls through the cracks - where every diabetic gets their A1C checked, 
              every cancer screening happens on time, and every person struggling with depression gets the follow-up they deserve.
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
                  commit fix(hipaa): Reduce PHI cache TTL to ≤5min for HIPAA compliance
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
              The Five Non-Negotiables
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

      {/* Timeline */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Our Journey
            </h2>
          </div>

          <div className="space-y-8">
            {milestones.map((milestone, idx) => (
              <div key={idx} className="flex items-start">
                <div className="w-24 flex-shrink-0">
                  <div className="text-2xl font-bold text-blue-600">
                    {milestone.year}
                  </div>
                </div>
                <div className="flex-1 bg-white border border-gray-200 rounded-xl p-6">
                  <h3 className="text-xl font-bold text-gray-900 mb-2">
                    {milestone.title}
                  </h3>
                  <p className="text-gray-700">
                    {milestone.description}
                  </p>
                </div>
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
              href="/demo"
              className="inline-flex items-center justify-center px-8 py-4 bg-white text-blue-600 font-semibold rounded-lg hover:bg-gray-100 transition-colors"
            >
              Request Demo
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
