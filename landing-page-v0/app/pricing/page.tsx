import {
  Check,
  X,
  ArrowRight,
  HeartPulse,
  Building2,
  Users,
  Heart,
  Hospital,
  Zap,
  Shield,
  HelpCircle,
} from 'lucide-react'
import Link from 'next/link'

export const metadata = {
  title: 'Pricing - HDIM | Healthcare Quality Platform',
  description: 'Transparent pricing for healthcare quality automation. From $80/month for developers to enterprise deployments. 60-95% lower than competitors.',
}

const tiers = [
  {
    id: 'developer',
    name: 'Developer',
    price: '$80',
    period: '/month',
    description: 'Perfect for proof of concept and technical evaluation',
    highlight: false,
    features: [
      { text: 'Full platform (all 28 services)', included: true },
      { text: 'Up to 1,000 patients', included: true },
      { text: 'Docker deployment', included: true },
      { text: 'Community support', included: true },
      { text: 'Documentation access', included: true },
      { text: 'Single tenant', included: true },
      { text: 'Priority support', included: false },
      { text: 'Custom measures', included: false },
    ],
    cta: 'Start Free Trial',
    ctaLink: '/demo',
    idealFor: ['Developers', 'Proof of concept', 'Small clinics', 'Technical evaluation'],
  },
  {
    id: 'professional',
    name: 'Professional',
    price: '$500',
    period: '/month',
    description: 'For FQHCs, small ACOs, and independent practices',
    highlight: true,
    badge: 'Most Popular for FQHCs',
    features: [
      { text: 'Up to 10,000 patients', included: true },
      { text: '5 admin users', included: true },
      { text: 'Email support (48hr)', included: true },
      { text: 'Monthly platform updates', included: true },
      { text: 'Basic analytics dashboards', included: true },
      { text: 'UDS/HEDIS automation', included: true },
      { text: 'Priority support', included: false },
      { text: 'Custom measures', included: false },
    ],
    cta: 'Get Started',
    ctaLink: '/schedule',
    idealFor: ['FQHCs', 'Small ACOs', 'Independent practices', 'Rural health networks'],
  },
  {
    id: 'business',
    name: 'Business',
    price: '$2,500',
    period: '/month',
    description: 'For mid-size organizations with advanced needs',
    highlight: false,
    features: [
      { text: 'Up to 50,000 patients', included: true },
      { text: '25 admin users', included: true },
      { text: 'Priority support (24hr)', included: true },
      { text: 'Weekly platform updates', included: true },
      { text: 'Advanced analytics', included: true },
      { text: '5 custom measures/year', included: true },
      { text: 'Multi-tenant option', included: true },
      { text: 'Quarterly business reviews', included: true },
    ],
    cta: 'Contact Sales',
    ctaLink: '/schedule',
    idealFor: ['Mid-size ACOs', 'Regional health systems', 'Community health centers', 'HIEs'],
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    price: 'Custom',
    period: 'starting at $75K/year',
    description: 'For large organizations with complex requirements',
    highlight: false,
    features: [
      { text: 'Unlimited patients', included: true },
      { text: 'Unlimited users', included: true },
      { text: 'Dedicated support (4hr SLA)', included: true },
      { text: 'On-premise or private cloud', included: true },
      { text: 'Custom integrations', included: true },
      { text: 'Unlimited custom measures', included: true },
      { text: 'Dedicated success manager', included: true },
      { text: 'Security audit support', included: true },
    ],
    cta: 'Talk to Sales',
    ctaLink: '/schedule',
    idealFor: ['Large ACOs', 'Health systems', 'Payer organizations', 'State HIEs'],
  },
]

const addOns = [
  { name: 'Mental Health Screening', price: '+$200/mo', description: 'PHQ-9, GAD-7, PHQ-2 with auto-gap creation' },
  { name: 'Risk Stratification', price: '+$300/mo', description: 'ML-powered predictive models, readmission risk' },
  { name: 'Prior Authorization', price: '+$400/mo', description: 'CMS-0057-F compliant workflow automation' },
  { name: 'SDOH Integration', price: '+$250/mo', description: 'Gravity Project, community resource directory' },
  { name: 'AI Assistant', price: '+$500/mo', description: 'Natural language queries, automated insights' },
  { name: 'White Label', price: '+$1,000/mo', description: 'Custom branding, subdomain' },
]

const competitors = [
  { name: 'HDIM Business', price: '$45K', threeYear: '$108K', highlight: true },
  { name: 'Salesforce Health Cloud', price: '$195K+', threeYear: '$585K+', highlight: false },
  { name: 'Optum Analytics', price: '$500K+', threeYear: '$1.5M+', highlight: false },
  { name: 'Epic Healthy Planet', price: '$750K+', threeYear: '$2.25M+', highlight: false },
  { name: 'Innovaccer', price: '$400K+', threeYear: '$1.2M+', highlight: false },
]

const faqs = [
  {
    question: 'That seems too cheap. What\'s the catch?',
    answer: 'No catch. We built HDIM on modern cloud-native architecture - 28 microservices that scale independently. We don\'t have legacy technical debt or enterprise sales teams to fund. Lower infrastructure costs = pricing that reflects actual value.',
  },
  {
    question: 'Do you offer a free trial?',
    answer: 'Yes! Our Developer tier at $80/month is designed for evaluation. You get full platform access for 1,000 patients. If it doesn\'t work for you, you\'ve invested less than a dinner with your EHR vendor.',
  },
  {
    question: 'What if we need more than 50,000 patients?',
    answer: 'Enterprise tier supports unlimited patients with custom pricing based on your needs. Contact our sales team for a custom quote.',
  },
  {
    question: 'Can we start small and scale up?',
    answer: 'Absolutely. Many customers start with Professional tier to validate the platform, then upgrade as their population grows. Upgrades are seamless with no data migration required.',
  },
  {
    question: 'Do you sign BAAs?',
    answer: 'Yes. Business Associate Agreements are included with all tiers. We take HIPAA compliance seriously - our 5-minute PHI cache TTL is proof of that commitment.',
  },
  {
    question: 'What\'s included in implementation?',
    answer: 'Quick Start ($5K) includes 30-day guided deployment. Standard Implementation ($15K) includes EHR integration and 60-day support. Enterprise implementations are custom-scoped.',
  },
]

export default function PricingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Navigation */}
      <nav className="bg-white border-b sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <Link href="/" className="flex items-center space-x-2">
              <div className="w-10 h-10 bg-gradient-to-br from-primary to-accent rounded-lg flex items-center justify-center">
                <HeartPulse className="w-6 h-6 text-white" />
              </div>
              <span className="text-xl font-bold text-gray-900">HDIM</span>
            </Link>
            <div className="flex items-center space-x-4">
              <Link href="/" className="text-gray-600 hover:text-primary text-sm">Home</Link>
              <Link href="/demo" className="btn-primary text-sm">Try Demo</Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="py-16 bg-gradient-to-br from-primary via-primary-600 to-primary-800 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h1 className="text-4xl md:text-5xl font-bold mb-4">
            Transparent Pricing for Healthcare Quality
          </h1>
          <p className="text-xl text-white/80 max-w-2xl mx-auto mb-6">
            Enterprise-grade quality automation at 60-95% lower cost than competitors.
            No hidden fees. No long-term contracts required.
          </p>
          <div className="flex flex-wrap justify-center gap-6 text-sm">
            <div className="flex items-center gap-2">
              <Check className="w-5 h-5 text-accent" />
              <span>BAA Included</span>
            </div>
            <div className="flex items-center gap-2">
              <Check className="w-5 h-5 text-accent" />
              <span>HIPAA Compliant</span>
            </div>
            <div className="flex items-center gap-2">
              <Check className="w-5 h-5 text-accent" />
              <span>90-Day Implementation</span>
            </div>
            <div className="flex items-center gap-2">
              <Check className="w-5 h-5 text-accent" />
              <span>Cancel Anytime</span>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing Tiers */}
      <section className="py-16 bg-gray-50" id="tiers">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {tiers.map((tier) => (
              <div
                key={tier.id}
                id={tier.id}
                className={`relative bg-white rounded-2xl shadow-sm border-2 transition-all hover:shadow-lg ${
                  tier.highlight ? 'border-primary ring-2 ring-primary/20' : 'border-gray-200'
                }`}
              >
                {tier.badge && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                    <span className="bg-primary text-white text-xs font-semibold px-3 py-1 rounded-full">
                      {tier.badge}
                    </span>
                  </div>
                )}

                <div className="p-6">
                  <h3 className="text-xl font-bold text-gray-900">{tier.name}</h3>
                  <div className="mt-4">
                    <span className="text-4xl font-bold text-gray-900">{tier.price}</span>
                    <span className="text-gray-500 text-sm">{tier.period}</span>
                  </div>
                  <p className="mt-2 text-gray-600 text-sm">{tier.description}</p>

                  <Link
                    href={tier.ctaLink}
                    className={`mt-6 block w-full text-center py-3 px-4 rounded-lg font-semibold transition-colors ${
                      tier.highlight
                        ? 'bg-primary text-white hover:bg-primary-600'
                        : 'bg-gray-100 text-gray-900 hover:bg-gray-200'
                    }`}
                  >
                    {tier.cta}
                  </Link>

                  <ul className="mt-6 space-y-3">
                    {tier.features.map((feature, idx) => (
                      <li key={idx} className="flex items-start gap-2">
                        {feature.included ? (
                          <Check className="w-5 h-5 text-green-500 flex-shrink-0" />
                        ) : (
                          <X className="w-5 h-5 text-gray-300 flex-shrink-0" />
                        )}
                        <span className={feature.included ? 'text-gray-700' : 'text-gray-400'}>
                          {feature.text}
                        </span>
                      </li>
                    ))}
                  </ul>

                  <div className="mt-6 pt-6 border-t border-gray-100">
                    <p className="text-xs text-gray-500 font-medium mb-2">Ideal for:</p>
                    <div className="flex flex-wrap gap-1">
                      {tier.idealFor.map((item, idx) => (
                        <span
                          key={idx}
                          className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded"
                        >
                          {item}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Volume Discounts */}
          <div className="mt-12 text-center">
            <p className="text-gray-600">
              <strong>Volume discounts:</strong> 10% off 2-year, 20% off 3-year commitments.
              Multi-site discounts available.
            </p>
          </div>
        </div>
      </section>

      {/* Add-On Modules */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Add-On Modules</h2>
            <p className="text-gray-600 max-w-2xl mx-auto">
              Extend your platform with specialized capabilities
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {addOns.map((addon, idx) => (
              <div key={idx} className="bg-gray-50 rounded-xl p-6">
                <div className="flex justify-between items-start mb-2">
                  <h3 className="font-semibold text-gray-900">{addon.name}</h3>
                  <span className="text-primary font-bold">{addon.price}</span>
                </div>
                <p className="text-gray-600 text-sm">{addon.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Competitor Comparison */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">How We Compare</h2>
            <p className="text-gray-600">
              50,000 member ACO example - Year 1 and 3-Year TCO
            </p>
          </div>

          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-100">
                <tr>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Vendor</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-900">Year 1</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-900">3-Year TCO</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {competitors.map((comp, idx) => (
                  <tr key={idx} className={comp.highlight ? 'bg-primary/5' : ''}>
                    <td className={`px-6 py-4 ${comp.highlight ? 'font-bold text-primary' : 'text-gray-700'}`}>
                      {comp.name}
                      {comp.highlight && (
                        <span className="ml-2 text-xs bg-primary text-white px-2 py-0.5 rounded-full">You</span>
                      )}
                    </td>
                    <td className={`px-6 py-4 text-right ${comp.highlight ? 'font-bold text-primary' : 'text-gray-700'}`}>
                      {comp.price}
                    </td>
                    <td className={`px-6 py-4 text-right ${comp.highlight ? 'font-bold text-primary' : 'text-gray-700'}`}>
                      {comp.threeYear}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <p className="text-center text-gray-500 text-sm mt-4">
            <strong>HDIM Savings: 70-95% lower TCO</strong> vs. enterprise competitors
          </p>
        </div>
      </section>

      {/* Segment-Specific Pricing */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Pricing by Organization Type</h2>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl p-6">
              <Building2 className="w-8 h-8 text-blue-600 mb-4" />
              <h3 className="font-bold text-gray-900 mb-2">Health Plans</h3>
              <p className="text-gray-600 text-sm mb-4">175K+ members with Star Rating focus</p>
              <div className="text-2xl font-bold text-blue-600">Enterprise</div>
              <p className="text-gray-500 text-sm">Custom pricing based on member count</p>
              <Link href="/schedule" className="mt-4 inline-flex items-center text-blue-600 text-sm font-medium">
                Get Quote <ArrowRight className="w-4 h-4 ml-1" />
              </Link>
            </div>

            <div className="bg-gradient-to-br from-green-50 to-green-100 rounded-xl p-6">
              <Users className="w-8 h-8 text-green-600 mb-4" />
              <h3 className="font-bold text-gray-900 mb-2">ACOs</h3>
              <p className="text-gray-600 text-sm mb-4">MSSP, ACO REACH, shared savings</p>
              <div className="text-2xl font-bold text-green-600">$2,500/mo</div>
              <p className="text-gray-500 text-sm">Business tier for 50K patients</p>
              <Link href="/schedule" className="mt-4 inline-flex items-center text-green-600 text-sm font-medium">
                Get Started <ArrowRight className="w-4 h-4 ml-1" />
              </Link>
            </div>

            <div className="bg-gradient-to-br from-red-50 to-red-100 rounded-xl p-6">
              <Heart className="w-8 h-8 text-red-600 mb-4" />
              <h3 className="font-bold text-gray-900 mb-2">FQHCs</h3>
              <p className="text-gray-600 text-sm mb-4">UDS reporting, 330 grant protection</p>
              <div className="text-2xl font-bold text-red-600">$500/mo</div>
              <p className="text-gray-500 text-sm">Professional tier for 10K patients</p>
              <Link href="/schedule" className="mt-4 inline-flex items-center text-red-600 text-sm font-medium">
                Get Started <ArrowRight className="w-4 h-4 ml-1" />
              </Link>
            </div>

            <div className="bg-gradient-to-br from-purple-50 to-purple-100 rounded-xl p-6">
              <Hospital className="w-8 h-8 text-purple-600 mb-4" />
              <h3 className="font-bold text-gray-900 mb-2">Health Systems</h3>
              <p className="text-gray-600 text-sm mb-4">Multi-EHR, enterprise scale</p>
              <div className="text-2xl font-bold text-purple-600">Enterprise</div>
              <p className="text-gray-500 text-sm">Custom for 500K+ patients</p>
              <Link href="/schedule" className="mt-4 inline-flex items-center text-purple-600 text-sm font-medium">
                Get Quote <ArrowRight className="w-4 h-4 ml-1" />
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* FAQs */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Frequently Asked Questions</h2>
          </div>

          <div className="space-y-6">
            {faqs.map((faq, idx) => (
              <div key={idx} className="bg-white rounded-xl p-6 shadow-sm">
                <h3 className="font-semibold text-gray-900 flex items-start gap-2">
                  <HelpCircle className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
                  {faq.question}
                </h3>
                <p className="mt-2 text-gray-600 ml-7">{faq.answer}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 bg-gradient-to-br from-primary via-primary-600 to-primary-800 text-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl font-bold mb-4">Ready to Transform Your Quality Program?</h2>
          <p className="text-white/80 mb-8 max-w-2xl mx-auto">
            See how HDIM delivers 2,964% ROI with 24-day payback. Schedule a demo to get started.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/demo"
              className="inline-flex items-center justify-center px-8 py-4 bg-white text-primary rounded-lg font-semibold hover:bg-white/90 transition-colors"
            >
              <Zap className="w-5 h-5 mr-2" />
              Try Interactive Demo
            </Link>
            <Link
              href="/schedule"
              className="inline-flex items-center justify-center px-8 py-4 border-2 border-white/30 rounded-lg font-semibold hover:bg-white/10 transition-colors"
            >
              Schedule Consultation
              <ArrowRight className="w-5 h-5 ml-2" />
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center">
            <div className="flex items-center space-x-2 mb-4 md:mb-0">
              <div className="w-8 h-8 bg-gradient-to-br from-primary to-accent rounded-lg flex items-center justify-center">
                <HeartPulse className="w-5 h-5 text-white" />
              </div>
              <span className="font-bold">HDIM</span>
            </div>
            <div className="flex items-center space-x-4 text-gray-400 text-sm">
              <Shield className="w-4 h-4" />
              <span>HIPAA Compliant</span>
              <span>|</span>
              <span>BAA Available</span>
              <span>|</span>
              <span>SOC 2 Type II</span>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-800 text-center text-gray-400 text-sm">
            &copy; {new Date().getFullYear()} HealthData-in-Motion. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  )
}
