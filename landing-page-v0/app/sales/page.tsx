import Link from 'next/link'
import {
  ArrowRight,
  Calendar,
  CheckCircle2,
  Clock,
  HeartPulse,
  MessageSquare,
  Search,
  FileCheck,
  Rocket,
  Handshake,
  Building2,
  Users,
  Shield,
  ChevronDown,
} from 'lucide-react'

export const metadata = {
  title: 'How We Work With You | HDIM',
  description: 'Learn how HDIM partners with health plans, hospitals, and provider groups — from discovery call to pilot results in 30 days.',
}

export default function SalesPage() {
  const steps = [
    {
      number: '01',
      icon: Search,
      name: 'Discovery',
      headline: 'We listen first',
      duration: '30-minute call',
      description:
        'We ask about your current quality program, HEDIS targets, gap closure workflow, and tech stack. No pitch deck. No slides. Just an honest conversation about where you are today.',
      youDo: 'Share your current state and goals',
      weDo: 'Ask the right questions and take notes',
      youGet: 'A conversation partner who actually listens',
    },
    {
      number: '02',
      icon: FileCheck,
      name: 'Fit Assessment',
      headline: "We tell you honestly if we're the right fit",
      duration: 'Within 48 hours',
      description:
        "We send a written summary of what we heard, where HDIM can help, and where it can't. If we're not the right fit for your situation, we'll say so — and point you toward what might be.",
      youDo: 'Review our written assessment',
      weDo: 'Deliver an honest fit analysis in writing',
      youGet: 'Clarity — no wasted time on either side',
    },
    {
      number: '03',
      icon: Rocket,
      name: 'Pilot',
      headline: 'See real results with your data',
      duration: '60–90 day structured pilot',
      description:
        'Defined success criteria before you sign. Observable SLO contracts so you verify our performance in real time via live dashboards — not in a quarterly report we control. First results in 30 days.',
      youDo: 'Provide FHIR data access and success criteria',
      weDo: 'Deploy, configure measures, and show results',
      youGet: 'Live HEDIS baselines and care gap data in 30 days',
    },
    {
      number: '04',
      icon: Handshake,
      name: 'Partnership',
      headline: 'We grow with you',
      duration: 'Ongoing',
      description:
        'Pilot results become your contract baseline. You get a dedicated customer success contact, onboarding training, and a roadmap built around your specific measure priorities.',
      youDo: 'Define priorities for the next 12 months',
      weDo: 'Deliver, support, and iterate alongside you',
      youGet: 'A long-term partner invested in your quality outcomes',
    },
  ]


  const faqs = [
    {
      q: 'We already have a quality vendor.',
      a: "Most of our customers did too. We work alongside existing tools or replace them depending on what's actually working. The discovery call is designed to answer that question honestly — we'll look at what you have and tell you whether switching makes sense.",
    },
    {
      q: "What's the implementation timeline?",
      a: 'Pilot environment up in 2 weeks. First HEDIS measure baselines and care gap data in 30 days. Full pilot readout at 60–90 days. These are commitments, not estimates.',
    },
    {
      q: "How do we know the ROI is real?",
      a: "Observable SLO contracts. You see our performance metrics in real time via live dashboards — not in a quarterly report we control. If we miss an SLO, automatic service credits kick in. We put it in writing before you sign.",
    },
    {
      q: 'Is our data secure?',
      a: "HIPAA-aligned controls, configurable PHI cache policies, full audit trail, role-based access control, and encryption at rest and in transit. The pilot can run on synthetic data until your security team signs off on the full integration.",
    },
    {
      q: "What happens if the pilot doesn't perform?",
      a: "The SLO contract specifies automatic service credits for underperformance. We define success criteria together before the pilot starts, measure against them continuously, and you have the data to hold us accountable.",
    },
    {
      q: 'How does pricing work?',
      a: "Member-volume based. We build a model for your specific population after the discovery call — no surprises. Pilot pricing is structured to give you a real result without a full enterprise commitment.",
    },
  ]

  return (
    <div className="min-h-screen bg-white">

      {/* Simple nav header — matches /schedule pattern */}
      <header className="bg-white border-b sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2 text-gray-900 font-bold text-lg">
            <div className="w-8 h-8 bg-gradient-to-br from-[#0D4F8B] to-teal-500 rounded-lg flex items-center justify-center">
              <HeartPulse className="w-4 h-4 text-white" />
            </div>
            HDIM
          </Link>
          <Link
            href="/schedule"
            className="inline-flex items-center gap-2 bg-[#0D4F8B] text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-[#0A3D6E] transition-colors"
          >
            <Calendar className="w-4 h-4" />
            Schedule a Call
          </Link>
        </div>
      </header>

      <main>

        {/* ── SECTION 1: Hero ── */}
        <section className="bg-gradient-to-br from-[#0D4F8B] to-[#0A3D6E] text-white py-24">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            <p className="text-teal-300 text-sm font-semibold uppercase tracking-widest mb-4">
              Our Sales Process
            </p>
            <h1 className="text-5xl font-bold mb-6 leading-tight">
              Here&apos;s Exactly How<br />We Work With You
            </h1>
            <p className="text-xl text-blue-100 max-w-2xl mx-auto mb-10">
              No black boxes, no surprise timelines. Most healthcare vendors make buying feel like a risk. We make it feel like a partnership — starting with the first call.
            </p>
            <Link
              href="/schedule"
              className="inline-flex items-center gap-2 bg-white text-[#0D4F8B] px-8 py-4 rounded-lg font-semibold text-lg hover:bg-blue-50 transition-colors"
            >
              Schedule Your Discovery Call
              <ArrowRight className="w-5 h-5" />
            </Link>
            <p className="text-blue-200 text-sm mt-4">
              30 minutes · No pitch deck · No obligation
            </p>
          </div>
        </section>

        {/* ── SECTION 2: 4-Step Process ── */}
        <section className="py-24 bg-gray-50">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-16">
              <h2 className="text-4xl font-bold text-gray-900 mb-4">
                The Partnership Process
              </h2>
              <p className="text-xl text-gray-600 max-w-2xl mx-auto">
                Four steps from first conversation to measurable results. Every step has defined outputs — no ambiguity.
              </p>
            </div>

            <div className="grid md:grid-cols-2 gap-8">
              {steps.map((step, idx) => {
                const Icon = step.icon
                return (
                  <div key={idx} className="bg-white rounded-2xl p-8 border border-gray-200 hover:shadow-md transition-shadow">
                    <div className="flex items-start gap-4 mb-6">
                      <div className="text-5xl font-black text-gray-100 leading-none select-none">
                        {step.number}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <div className="w-8 h-8 bg-[#0D4F8B] rounded-lg flex items-center justify-center">
                            <Icon className="w-4 h-4 text-white" />
                          </div>
                          <span className="text-sm font-semibold text-[#0D4F8B] uppercase tracking-wide">
                            {step.name}
                          </span>
                        </div>
                        <h3 className="text-xl font-bold text-gray-900">{step.headline}</h3>
                        <span className="inline-flex items-center gap-1 text-xs text-gray-500 mt-1">
                          <Clock className="w-3 h-3" />
                          {step.duration}
                        </span>
                      </div>
                    </div>

                    <p className="text-gray-600 mb-6 leading-relaxed">{step.description}</p>

                    <div className="grid grid-cols-3 gap-3 text-sm">
                      <div className="bg-gray-50 rounded-lg p-3">
                        <div className="text-xs font-semibold text-gray-400 uppercase mb-1">You do</div>
                        <div className="text-gray-700">{step.youDo}</div>
                      </div>
                      <div className="bg-gray-50 rounded-lg p-3">
                        <div className="text-xs font-semibold text-gray-400 uppercase mb-1">We do</div>
                        <div className="text-gray-700">{step.weDo}</div>
                      </div>
                      <div className="bg-[#0D4F8B]/5 rounded-lg p-3 border border-[#0D4F8B]/10">
                        <div className="text-xs font-semibold text-[#0D4F8B] uppercase mb-1">You get</div>
                        <div className="text-gray-700">{step.youGet}</div>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        {/* ── SECTION 3: What to Expect ── */}
        <section className="py-24 bg-white">
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-16">
              <h2 className="text-4xl font-bold text-gray-900 mb-4">
                What to Expect, and When
              </h2>
              <p className="text-xl text-gray-600">
                Concrete commitments at every stage — not &quot;it depends.&quot;
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
              {[
                {
                  timeframe: '48 hours',
                  label: 'After discovery call',
                  deliverable: 'Written fit assessment delivered to your inbox',
                  detail: "A clear summary of what we heard, where we fit, and recommended next steps — or an honest explanation of why we're not the right solution.",
                  color: 'blue',
                },
                {
                  timeframe: '2 weeks',
                  label: 'Pilot kickoff',
                  deliverable: 'Live environment with your FHIR data ingested',
                  detail: 'FHIR data pipeline connected, HEDIS measures configured for your population, and your team trained on the platform.',
                  color: 'teal',
                },
                {
                  timeframe: '30 days',
                  label: 'Into the pilot',
                  deliverable: 'First real results: care gap data + HEDIS baselines',
                  detail: 'Live care gap detection running against your population, baseline HEDIS measure scores, and an ROI model built from your actual numbers.',
                  color: 'green',
                },
              ].map((item, idx) => (
                <div key={idx} className="text-center">
                  <div className={`inline-flex items-center justify-center w-20 h-20 rounded-full mb-4 ${
                    item.color === 'blue' ? 'bg-blue-100' :
                    item.color === 'teal' ? 'bg-teal-100' : 'bg-green-100'
                  }`}>
                    <span className={`text-2xl font-black ${
                      item.color === 'blue' ? 'text-blue-700' :
                      item.color === 'teal' ? 'text-teal-700' : 'text-green-700'
                    }`}>
                      {item.timeframe}
                    </span>
                  </div>
                  <div className="text-sm text-gray-500 mb-2">{item.label}</div>
                  <h3 className="text-lg font-bold text-gray-900 mb-3">{item.deliverable}</h3>
                  <p className="text-gray-600 text-sm leading-relaxed">{item.detail}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── SECTION 4: FAQ ── */}
        <section className="py-24 bg-gray-50">
          <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-16">
              <h2 className="text-4xl font-bold text-gray-900 mb-4">
                Questions We Hear Every Time
              </h2>
              <p className="text-xl text-gray-600">
                Honest answers — not marketing copy.
              </p>
            </div>

            <div className="space-y-4">
              {faqs.map((faq, idx) => (
                <div key={idx} className="bg-white rounded-xl border border-gray-200 p-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-start gap-3">
                    <span className="text-[#0D4F8B] font-black text-xl leading-none mt-0.5">Q</span>
                    {faq.q}
                  </h3>
                  <p className="text-gray-600 leading-relaxed pl-8">{faq.a}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* rest of sections go here */}

      </main>
    </div>
  )
}
