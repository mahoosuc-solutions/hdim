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

        {/* rest of sections go here */}

      </main>
    </div>
  )
}
