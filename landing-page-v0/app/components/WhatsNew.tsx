import { DollarSign, Wrench, Users, Shield } from 'lucide-react'

const NEW_CAPABILITIES = [
  {
    icon: DollarSign,
    title: 'Revenue Cycle',
    badge: 'Wave-1',
    description: 'Claims processing, remittance reconciliation (ERA/835), price transparency APIs, and ADT event handling.',
  },
  {
    icon: Wrench,
    title: 'Custom Measure Builder',
    badge: 'New',
    description: 'Create and deploy custom quality measures via UI. 7 configurable metadata fields, no engineering required.',
  },
  {
    icon: Users,
    title: 'CMO Onboarding',
    badge: 'New',
    description: 'Structured evaluation-to-adoption path for health plan CMOs with dashboard workflows and validation hooks.',
  },
  {
    icon: Shield,
    title: 'Security Hardening',
    badge: 'Updated',
    description: 'Pre-NVD CVE packets, immutable evidence manifests, 16-class operations orchestration with header security.',
  },
]

export function WhatsNew() {
  return (
    <section className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="section-heading">What&apos;s New -- March 2026</h2>
          <p className="section-subheading mt-4">
            Latest capabilities added to the platform this quarter.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {NEW_CAPABILITIES.map((cap) => (
            <div
              key={cap.title}
              className="p-6 bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="w-12 h-12 rounded-lg bg-primary-50 flex items-center justify-center">
                  <cap.icon className="w-6 h-6 text-primary" />
                </div>
                <span className="text-xs font-semibold px-2 py-1 rounded-full bg-accent-50 text-accent-600">
                  {cap.badge}
                </span>
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">{cap.title}</h3>
              <p className="text-sm text-gray-600 leading-relaxed">{cap.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
