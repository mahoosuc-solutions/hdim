import Image from 'next/image'
import {
  Activity,
  BarChart3,
  CheckCircle2,
  ChevronRight,
  FileCheck,
  HeartPulse,
  LineChart,
  Shield,
  Zap,
  ArrowRight,
  Play,
  Star,
  TrendingUp,
  Database,
  Lock,
  Award
} from 'lucide-react'
import { AnimatedCounter } from './components/AnimatedCounter'
import { CyclingText } from './components/CyclingText'
import { LandingPageClient } from './components/LandingPageClient'

export default function LandingPage() {
  const features = [
    {
      icon: HeartPulse,
      title: 'Care Gap Detection',
      description: 'Real-time CQL execution identifies care gaps using NCQA-certified HEDIS specifications. Sub-second detection across 56 quality measures.',
    },
    {
      icon: BarChart3,
      title: 'HEDIS Evaluation',
      description: 'CQL-native measure calculation with <500ms per patient execution. No proprietary interpretation—identical results to NCQA CQL playground.',
    },
    {
      icon: LineChart,
      title: 'Risk Stratification',
      description: 'HCC v28 risk adjustment models with validated scoring algorithms. Predict patient risk scores for value-based contracts.',
    },
    {
      icon: Database,
      title: 'FHIR R4 Integration',
      description: 'HAPI FHIR 7.x native architecture—no v2→FHIR translation layer. Direct FHIR R4 queries with <200ms p95 latency.',
    },
    {
      icon: Zap,
      title: 'CQL Engine',
      description: 'Direct CQL execution via open-source cql-engine library. What you test in NCQA playground runs identically in production.',
    },
    {
      icon: FileCheck,
      title: 'QRDA Export',
      description: 'Automated QRDA I/III generation for CMS quality reporting. Audit-ready with full measure traceability.',
    },
  ]

  const stats = [
    { value: 40, suffix: '%', label: 'Faster Gap Closure' },
    { value: 12, suffix: ' pts', label: 'Avg. HEDIS Improvement' },
    { value: 500, suffix: 'K+', label: 'Members Managed' },
    { value: 82, suffix: '%', label: 'Time Savings' },
  ]

  const painPoints = [
    {
      icon: Database,
      title: 'Data Scattered Across 15+ Systems',
      description: 'Your patient data lives in silos - EHRs, claims, labs, and more. Integration takes months.',
    },
    {
      icon: Activity,
      title: 'Manual Measure Calculations',
      description: 'Your team spends weeks on HEDIS calculations. When specs change, you start over.',
    },
    {
      icon: TrendingUp,
      title: 'Missed Quality Bonuses',
      description: 'Health plans leave millions on the table each year from preventable care gaps.',
    },
  ]

  return (
    <LandingPageClient>
      {/* Hero Section */}
      <section id="main-content" className="relative min-h-screen flex items-center bg-gradient-to-br from-primary via-primary-600 to-primary-800 overflow-hidden">
        {/* Background Hero Image */}
        <div className="absolute inset-0">
          <Image
            src="/images/hero/hero-02.png"
            alt="Healthcare data visualization"
            fill
            className="object-cover opacity-30"
            priority
          />
        </div>
        {/* Background Pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 left-0 w-96 h-96 bg-accent rounded-full filter blur-3xl" />
          <div className="absolute bottom-0 right-0 w-96 h-96 bg-white rounded-full filter blur-3xl" />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-20">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Left Column - Content */}
            <div className="text-center lg:text-left">
              {/* Badge */}
              <div className="inline-flex items-center px-4 py-2 bg-white/10 backdrop-blur-sm rounded-full text-white/90 text-sm mb-6">
                <Star className="w-4 h-4 text-yellow-400 mr-2" />
                Trusted by leading health plans and ACOs
              </div>

              {/* Headline */}
              <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight mb-6">
                Close <CyclingText /><br />
                <span className="text-white">40% Faster</span>
              </h1>

              {/* Subheadline */}
              <p className="text-lg md:text-xl text-white/80 mb-8 max-w-xl mx-auto lg:mx-0">
                The FHIR-native platform for HEDIS excellence. Automate quality measure evaluation, detect care gaps instantly, and boost your Star Ratings.
              </p>

              {/* CTAs */}
              <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
                <a
                  href="/demo"
                  className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-primary bg-white rounded-lg shadow-xl hover:shadow-2xl hover:scale-105 transition-all duration-200"
                >
                  <Play className="w-5 h-5 mr-2" />
                  Try Interactive Demo
                </a>
                <a
                  href="/research#calculator"
                  className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-white border-2 border-white/30 rounded-lg hover:bg-white/10 transition-all duration-200"
                >
                  Calculate Your ROI
                  <ArrowRight className="w-5 h-5 ml-2" />
                </a>
              </div>

              {/* Trust Badges */}
              <div className="mt-10 flex flex-wrap items-center justify-center lg:justify-start gap-6">
                <div className="flex items-center space-x-2 text-white/70">
                  <Shield className="w-5 h-5" />
                  <span className="text-sm">HIPAA Compliant</span>
                </div>
                <div className="flex items-center space-x-2 text-white/70">
                  <Lock className="w-5 h-5" />
                  <span className="text-sm">SOC 2 Type II</span>
                </div>
                <div className="flex items-center space-x-2 text-white/70">
                  <Award className="w-5 h-5" />
                  <span className="text-sm">HITRUST Certified</span>
                </div>
              </div>
            </div>

            {/* Right Column - Stats/Visual */}
            <div className="hidden lg:block">
              <div className="relative">
                {/* Dashboard Preview Card */}
                <div className="bg-white rounded-2xl shadow-2xl p-6 transform rotate-2 hover:rotate-0 transition-transform duration-500">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="font-semibold text-gray-900">Care Gap Dashboard</h3>
                    <span className="text-xs text-green-600 bg-green-100 px-2 py-1 rounded-full">Live</span>
                  </div>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <span className="text-gray-600">Open Gaps</span>
                      <span className="font-bold text-primary">2,340</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <span className="text-gray-600">Closed This Month</span>
                      <span className="font-bold text-green-600">+847</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <span className="text-gray-600">HEDIS Score</span>
                      <span className="font-bold text-primary">78.4%</span>
                    </div>
                    <div className="h-32 bg-gradient-to-r from-primary/10 to-accent/10 rounded-lg flex items-end p-4">
                      <div className="flex items-end space-x-2 w-full">
                        {[40, 55, 45, 70, 65, 80, 75, 90, 85, 95].map((h, i) => (
                          <div
                            key={i}
                            className="flex-1 bg-gradient-to-t from-primary to-accent rounded-t"
                            style={{ height: `${h}%` }}
                          />
                        ))}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Floating Stat Card */}
                <div className="absolute -bottom-6 -left-6 bg-white rounded-xl shadow-xl p-4 animate-pulse-slow">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
                      <TrendingUp className="w-6 h-6 text-green-600" />
                    </div>
                    <div>
                      <p className="text-2xl font-bold text-gray-900">+12 pts</p>
                      <p className="text-sm text-gray-500">HEDIS Improvement</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Scroll Indicator */}
        <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 animate-bounce">
          <div className="w-8 h-12 border-2 border-white/30 rounded-full flex justify-center pt-2">
            <div className="w-1.5 h-3 bg-white/50 rounded-full animate-pulse" />
          </div>
        </div>
      </section>

      {/* Trust Bar - Credibility Metrics */}
      <section className="py-12 bg-gray-50 border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm text-gray-500 mb-8">
            Trusted by leading healthcare organizations
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            <div>
              <div className="text-3xl font-bold text-primary">500K+</div>
              <p className="text-gray-600 text-sm">Members Managed</p>
            </div>
            <div>
              <div className="text-3xl font-bold text-primary">15+</div>
              <p className="text-gray-600 text-sm">Health Systems</p>
            </div>
            <div>
              <div className="text-3xl font-bold text-primary">56</div>
              <p className="text-gray-600 text-sm">HEDIS Measures</p>
            </div>
            <div>
              <div className="text-3xl font-bold text-primary">99.9%</div>
              <p className="text-gray-600 text-sm">Uptime SLA</p>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, index) => (
              <div key={index} className="text-center">
                <div className="text-4xl md:text-5xl font-bold text-primary mb-2">
                  <AnimatedCounter end={stat.value} suffix={stat.suffix} />
                </div>
                <p className="text-gray-600">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Problem Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="section-heading mb-4">
              Sound Familiar?
            </h2>
            <p className="section-subheading">
              Healthcare organizations lose millions in quality bonuses every year. Not because they don&apos;t care - but because their tools weren&apos;t built for modern value-based care.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {painPoints.map((point, index) => (
              <div
                key={index}
                className="bg-white rounded-xl p-8 shadow-sm border border-gray-100 card-hover"
              >
                <div className="w-14 h-14 bg-red-50 rounded-xl flex items-center justify-center mb-6">
                  <point.icon className="w-7 h-7 text-red-500" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-3">
                  {point.title}
                </h3>
                <p className="text-gray-600">
                  {point.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Patient Impact Section */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              The Patients Behind the Numbers
            </span>
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mt-2 mb-4">
              Every Spreadsheet Row Is Someone&apos;s Life
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              These are the moments that justify every line of code we write.
            </p>
          </div>

          {/* Maria's Story Card */}
          <div className="bg-gradient-to-br from-red-50 to-green-50 rounded-2xl p-8 md:p-12 max-w-4xl mx-auto">
            <div className="grid md:grid-cols-5 gap-8 items-center">
              <div className="md:col-span-3">
                <h3 className="text-2xl font-bold text-gray-900 mb-4">Maria, 67 - Type 2 Diabetes</h3>

                <div className="mb-6">
                  <p className="text-red-600 font-semibold mb-2 flex items-center gap-2">
                    <span className="w-3 h-3 bg-red-500 rounded-full"></span>
                    Without Real-Time Gap Detection:
                  </p>
                  <p className="text-gray-600">Maria&apos;s last A1C was 14 months ago. Her endocrinologist retired. Her new PCP&apos;s system doesn&apos;t flag the gap. Diabetic ketoacidosis. $43,000 hospitalization. Permanent kidney damage.</p>
                </div>

                <div>
                  <p className="text-green-600 font-semibold mb-2 flex items-center gap-2">
                    <span className="w-3 h-3 bg-green-500 rounded-full"></span>
                    With HDIM:
                  </p>
                  <p className="text-gray-600">At 9 months overdue, HDIM flags Maria&apos;s care gap. Her care manager calls. A $45 lab visit. A1C: 7.8% - elevated but manageable. Medication adjusted. Crisis prevented.</p>
                  <p className="text-gray-900 font-semibold mt-3">Maria is still making tamales for her grandchildren.</p>
                </div>
              </div>

              <div className="md:col-span-2 text-center">
                <div className="relative w-48 h-48 mx-auto mb-4">
                  <Image
                    src="/images/portraits/maria.png"
                    alt="Maria - Patient story"
                    fill
                    className="object-cover rounded-xl"
                    loading="lazy"
                  />
                </div>
                <p className="text-lg font-semibold text-primary italic">
                  &quot;The gap between a $45 lab test and a $43,000 hospitalization is 14 months of silence.&quot;
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Before/After Comparison Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              The Transformation
            </span>
            <h2 className="section-heading mt-2 mb-4">
              From Fragmented to Connected in Seconds
            </h2>
            <p className="section-subheading">
              See how HDIM transforms reactive, months-long processes into real-time, actionable insights.
            </p>
          </div>

          <div className="relative rounded-2xl overflow-hidden shadow-2xl">
            <Image
              src="/images/comparison/before-after.png"
              alt="Before and After: 3 months reactive vs 2 seconds real-time with HDIM"
              width={1920}
              height={1080}
              className="w-full h-auto"
              loading="lazy"
            />
          </div>

          <div className="grid md:grid-cols-2 gap-8 mt-10">
            <div className="text-center p-6 bg-red-50 rounded-xl">
              <div className="text-4xl font-bold text-red-600 mb-2">3 months</div>
              <p className="text-gray-600">Traditional reactive approach - finding care gaps after they&apos;ve impacted quality scores</p>
            </div>
            <div className="text-center p-6 bg-green-50 rounded-xl">
              <div className="text-4xl font-bold text-green-600 mb-2">2 seconds</div>
              <p className="text-gray-600">With HDIM real-time detection - act immediately when data arrives</p>
            </div>
          </div>
        </div>
      </section>

      {/* Solution Section */}
      <section className="py-20 bg-gray-50" id="solutions">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <div>
              <span className="text-primary font-semibold text-sm uppercase tracking-wider">
                The HDIM Difference
              </span>
              <h2 className="section-heading mt-2 mb-6">
                Built for the Future of Quality Measurement
              </h2>
              <p className="text-lg text-gray-600 mb-8">
                HDIM is a FHIR-native, CQL-powered platform that doesn&apos;t just report on quality - we execute the actual measure logic. When NCQA updates specifications, you&apos;re compliant automatically.
              </p>

              <div className="space-y-4">
                {[
                  '28 specialized microservices for modular deployment',
                  'HAPI FHIR 7.x with full R4 compliance (<200ms p95 queries)',
                  'CQL-native execution—identical to NCQA reference implementation',
                  'Multi-tenant isolation with 5-minute PHI cache TTL (HIPAA)',
                ].map((item, index) => (
                  <div key={index} className="flex items-start space-x-3">
                    <CheckCircle2 className="w-6 h-6 text-green-500 flex-shrink-0 mt-0.5" />
                    <span className="text-gray-700">{item}</span>
                  </div>
                ))}
              </div>

              <div className="mt-10">
                <a href="#demo" className="btn-primary">
                  See HDIM in Action
                  <ChevronRight className="w-5 h-5 ml-2" />
                </a>
              </div>
            </div>

            {/* Architecture Diagram */}
            <div className="relative">
              <div className="bg-gradient-to-br from-primary/5 to-accent/5 rounded-2xl p-4 md:p-8">
                <div className="bg-white rounded-xl shadow-xl overflow-hidden">
                  <div className="h-8 bg-gray-100 flex items-center px-4 space-x-2">
                    <div className="w-3 h-3 bg-red-400 rounded-full" />
                    <div className="w-3 h-3 bg-yellow-400 rounded-full" />
                    <div className="w-3 h-3 bg-green-400 rounded-full" />
                  </div>
                  <div className="relative h-80">
                    <Image
                      src="/images/technical/architecture.png"
                      alt="HDIM Real-Time Architecture - Connect Anything, Understand Everything, Act Immediately"
                      fill
                      className="object-contain p-4"
                      loading="lazy"
                    />
                  </div>
                </div>
              </div>

              {/* Floating badge */}
              <div className="absolute -right-4 top-1/2 transform -translate-y-1/2 bg-white rounded-lg shadow-lg p-4">
                <div className="flex items-center space-x-2">
                  <Zap className="w-5 h-5 text-yellow-500" />
                  <span className="text-sm font-medium">100K patients/min</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* The 5-Minute Story */}
      <section className="py-20 bg-gray-900 text-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <span className="text-accent font-semibold text-sm uppercase tracking-wider">
            Why We&apos;re Different
          </span>
          <h2 className="text-3xl md:text-4xl font-bold mt-2 mb-8">
            Why 5 Minutes Matters
          </h2>

          <div className="bg-gray-800 rounded-lg p-6 mb-8 font-mono text-left text-sm overflow-x-auto">
            <span className="text-green-400">commit</span> <span className="text-gray-300">fix(hipaa): Reduce PHI cache TTL to 5min for HIPAA compliance</span><br/>
            <span className="text-gray-500">December 27, 2025, 10:31 PM</span>
          </div>

          <div className="text-lg text-gray-300 space-y-4 max-w-2xl mx-auto">
            <p>Most platforms cache patient data for 24 hours. Faster APIs. Better demos. Standard practice.</p>
            <p className="text-white font-semibold">We cache for 5 minutes maximum.</p>
            <p className="text-gray-400">
              Not because regulators require it -- they don&apos;t specify a number.<br/>
              Not because customers demand it -- they rarely ask.
            </p>
            <p className="text-white">
              Because when a patient revokes consent to share their mental health history, they deserve to know that within 5 minutes, that data stops moving.
            </p>
            <p className="text-accent text-xl font-semibold mt-8">That&apos;s not a feature. That&apos;s respect.</p>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-gray-50" id="features">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              Platform Capabilities
            </span>
            <h2 className="section-heading mt-2 mb-4">
              Everything You Need for Quality Excellence
            </h2>
            <p className="section-subheading">
              From care gap detection to regulatory reporting, HDIM provides the complete toolkit for value-based success.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="bg-white rounded-xl p-8 shadow-sm border border-gray-100 card-hover group"
              >
                <div className="w-14 h-14 bg-primary/10 rounded-xl flex items-center justify-center mb-6 group-hover:bg-primary group-hover:scale-110 transition-all duration-300">
                  <feature.icon className="w-7 h-7 text-primary group-hover:text-white transition-colors" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-3">
                  {feature.title}
                </h3>
                <p className="text-gray-600 mb-4">
                  {feature.description}
                </p>
                <a
                  href="/demo"
                  className="inline-flex items-center text-primary font-medium hover:underline"
                >
                  Learn more
                  <ChevronRight className="w-4 h-4 ml-1" />
                </a>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Video Demo Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              See It In Action
            </span>
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mt-2 mb-4">
              Watch HDIM Close Care Gaps in Real-Time
            </h2>
            <p className="text-xl text-gray-600">
              A 3-minute walkthrough of our care gap detection workflow
            </p>
          </div>
          <div className="relative rounded-2xl overflow-hidden shadow-2xl cursor-pointer group">
            <Image
              src="/images/video/demo.png"
              alt="HDIM Demo Video Thumbnail"
              width={1280}
              height={720}
              loading="lazy"
              className="w-full"
            />
            <div className="absolute inset-0 flex items-center justify-center bg-black/30 group-hover:bg-black/40 transition-colors">
              <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                <Play className="w-8 h-8 text-primary ml-1" />
              </div>
            </div>
          </div>
          <p className="text-center text-gray-500 mt-4 text-sm">
            No registration required - click to watch
          </p>
        </div>
      </section>

      {/* Dashboard Preview Section */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              Real-Time Command Center
            </span>
            <h2 className="section-heading mt-2 mb-4">
              See Your Quality Data Come Alive
            </h2>
            <p className="section-subheading">
              Monitor 47 connected systems, 128K+ patients, and live care gap alerts - all in one unified dashboard.
            </p>
          </div>

          <div className="grid lg:grid-cols-2 gap-8 items-center">
            {/* Main Dashboard */}
            <div className="relative rounded-2xl overflow-hidden shadow-2xl">
              <Image
                src="/images/dashboard/main.png"
                alt="HDIM Real-Time Care Gap Command Center Dashboard"
                width={1920}
                height={1080}
                className="w-full h-auto"
                loading="lazy"
              />
              <div className="absolute top-4 right-4 bg-green-500 text-white px-3 py-1 rounded-full text-sm font-medium flex items-center">
                <span className="w-2 h-2 bg-white rounded-full mr-2 animate-pulse" />
                LIVE
              </div>
            </div>

            {/* Mobile Dashboard */}
            <div className="flex flex-col items-center justify-center">
              <div className="relative w-64 mx-auto">
                <Image
                  src="/images/dashboard/mobile.png"
                  alt="HDIM Mobile Dashboard"
                  width={640}
                  height={1136}
                  className="w-full h-auto rounded-3xl shadow-xl"
                  loading="lazy"
                />
              </div>
              <p className="text-center text-gray-600 mt-6">
                <strong>Access anywhere.</strong> Full functionality on mobile devices for care teams in the field.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Social Proof Section */}
      <section className="py-20 bg-gray-50" id="customers">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              Customer Success
            </span>
            <h2 className="section-heading mt-2 mb-4">
              Proven Results Across Healthcare
            </h2>
          </div>

          {/* Testimonial Card - Sarah */}
          <div className="max-w-4xl mx-auto">
            <div className="bg-gradient-to-br from-primary to-primary-700 rounded-2xl p-8 md:p-12 text-white relative overflow-hidden">
              {/* Quote mark */}
              <div className="absolute top-4 left-4 text-white/10 text-9xl font-serif">&ldquo;</div>

              <div className="relative z-10">
                <p className="text-xl md:text-2xl leading-relaxed mb-8">
                  &ldquo;HDIM transformed our quality program. We improved our HEDIS scores by 12 points in the first year and captured an additional $2.3M in quality bonuses. The care gap detection alone saved our team 20 hours per week.&rdquo;
                </p>

                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 relative rounded-full overflow-hidden border-2 border-white/30">
                    <Image
                      src="/images/portraits/sarah.png"
                      alt="Dr. Sarah Chen"
                      fill
                      className="object-cover"
                      loading="lazy"
                    />
                  </div>
                  <div>
                    <p className="font-semibold text-lg">Dr. Sarah Chen</p>
                    <p className="text-white/70">Chief Medical Officer, Regional Health Network</p>
                  </div>
                </div>
              </div>

              {/* Stats row */}
              <div className="mt-10 pt-8 border-t border-white/20 grid grid-cols-3 gap-8">
                <div className="text-center">
                  <div className="text-3xl font-bold">+12 pts</div>
                  <div className="text-white/70 text-sm">HEDIS Score</div>
                </div>
                <div className="text-center">
                  <div className="text-3xl font-bold">$2.3M</div>
                  <div className="text-white/70 text-sm">Additional Bonus</div>
                </div>
                <div className="text-center">
                  <div className="text-3xl font-bold">20 hrs</div>
                  <div className="text-white/70 text-sm">Weekly Time Saved</div>
                </div>
              </div>
            </div>

            {/* James Testimonial */}
            <div className="bg-white rounded-2xl p-8 shadow-lg mt-8">
              <div className="flex items-start gap-6">
                <div className="relative w-20 h-20 flex-shrink-0">
                  <Image
                    src="/images/portraits/james.png"
                    alt="James Richardson"
                    fill
                    className="object-cover rounded-full"
                    loading="lazy"
                  />
                </div>
                <div>
                  <p className="text-gray-700 text-lg mb-4">
                    &ldquo;Our ACO was leaving $750K in shared savings on the table because we couldn&apos;t close care gaps fast enough. HDIM gave us real-time visibility across 3 different EHRs. In 90 days, we went from 42% to 78% on behavioral health follow-up.&rdquo;
                  </p>
                  <p className="font-semibold text-gray-900">James Richardson</p>
                  <p className="text-gray-600 text-sm">Chief Information Officer, Mountain West ACO</p>
                </div>
              </div>
            </div>
          </div>

          {/* More case studies link */}
          <div className="text-center mt-10">
            <a
              href="/research"
              className="inline-flex items-center text-primary font-medium hover:underline"
            >
              Read more customer stories
              <ChevronRight className="w-4 h-4 ml-1" />
            </a>
          </div>
        </div>
      </section>

      {/* Compliance & Trust Section */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-2xl font-bold text-gray-900">
              Enterprise-Grade Security & Compliance
            </h2>
            <p className="text-gray-600 mt-2">Built with security-first architecture and proven at scale</p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
            <div className="flex flex-col items-center p-4 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 relative mb-3">
                <Image
                  src="/images/icons/hipaa.png"
                  alt="HIPAA Compliant"
                  fill
                  className="object-contain"
                  loading="lazy"
                />
              </div>
              <p className="font-semibold text-gray-900 text-sm text-center">HIPAA</p>
              <p className="text-xs text-gray-500 text-center">Compliant</p>
            </div>
            <div className="flex flex-col items-center p-4 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 relative mb-3">
                <Image
                  src="/images/icons/fhir.png"
                  alt="FHIR R4 Native"
                  fill
                  className="object-contain"
                  loading="lazy"
                />
              </div>
              <p className="font-semibold text-gray-900 text-sm text-center">FHIR R4</p>
              <p className="text-xs text-gray-500 text-center">Native</p>
            </div>
            <div className="flex flex-col items-center p-4 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 relative mb-3">
                <Image
                  src="/images/icons/cql.png"
                  alt="CQL Engine"
                  fill
                  className="object-contain"
                  loading="lazy"
                />
              </div>
              <p className="font-semibold text-gray-900 text-sm text-center">CQL</p>
              <p className="text-xs text-gray-500 text-center">Execution</p>
            </div>
            <div className="flex flex-col items-center p-4 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 relative mb-3">
                <Image
                  src="/images/icons/tests.png"
                  alt="847 Tests Passing"
                  fill
                  className="object-contain"
                  loading="lazy"
                />
              </div>
              <p className="font-semibold text-gray-900 text-sm text-center">847 Tests</p>
              <p className="text-xs text-gray-500 text-center">Passing</p>
            </div>
            <div className="flex flex-col items-center p-4 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 relative mb-3">
                <Image
                  src="/images/icons/uptime.png"
                  alt="99.9% Uptime"
                  fill
                  className="object-contain"
                  loading="lazy"
                />
              </div>
              <p className="font-semibold text-gray-900 text-sm text-center">99.9%</p>
              <p className="text-xs text-gray-500 text-center">Uptime</p>
            </div>
            <div className="flex flex-col items-center p-4 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 relative mb-3">
                <Image
                  src="/images/icons/microservices.png"
                  alt="28 Microservices"
                  fill
                  className="object-contain"
                  loading="lazy"
                />
              </div>
              <p className="font-semibold text-gray-900 text-sm text-center">28 Services</p>
              <p className="text-xs text-gray-500 text-center">Modular</p>
            </div>
          </div>
          <p className="text-center text-gray-600 mt-8">
            <Shield className="inline w-4 h-4 mr-2" />
            Business Associate Agreements (BAA) available for all customers
          </p>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-gradient-to-br from-primary via-primary-600 to-primary-800" id="demo">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
            How Many Patients Are Waiting for a Call That Won&apos;t Come?
          </h2>
          <p className="text-xl text-white/80 mb-4 max-w-2xl mx-auto">
            HEDIS measurement year ends December 31. Every day without real-time gap detection is a day of missed interventions.
          </p>
          <p className="text-lg text-white/60 mb-10 max-w-2xl mx-auto">
            See how HDIM catches patients falling through the cracks -- before they become your highest-cost cases.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <a
              href="/demo"
              className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-primary bg-white rounded-lg shadow-xl hover:shadow-2xl hover:scale-105 transition-all duration-200"
            >
              <Play className="w-5 h-5 mr-2" />
              Try Interactive Demo
            </a>
            <a
              href="/schedule"
              className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-white border-2 border-white/30 rounded-lg hover:bg-white/10 transition-all duration-200"
            >
              Schedule a Consultation
              <ArrowRight className="w-5 h-5 ml-2" />
            </a>
          </div>

          <p className="mt-8 text-white/60 text-sm">
            No commitment required. See HDIM in action with your own use case.
          </p>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid md:grid-cols-4 gap-12">
            {/* Brand */}
            <div>
              <div className="flex items-center space-x-2 mb-4">
                <div className="w-10 h-10 bg-gradient-to-br from-primary to-accent rounded-lg flex items-center justify-center">
                  <HeartPulse className="w-6 h-6 text-white" />
                </div>
                <span className="text-xl font-bold">HDIM</span>
              </div>
              <p className="text-gray-400 text-sm">
                The FHIR-native platform for healthcare quality excellence.
              </p>
              <div className="flex space-x-4 mt-6">
                <a href="https://linkedin.com/company/hdim" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">
                  <span className="sr-only">LinkedIn</span>
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
                  </svg>
                </a>
                <a href="https://twitter.com/hdim_health" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">
                  <span className="sr-only">Twitter</span>
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
                  </svg>
                </a>
              </div>
            </div>

            {/* Platform */}
            <div>
              <h3 className="font-semibold text-sm uppercase tracking-wider mb-4">Platform</h3>
              <ul className="space-y-3">
                <li><a href="#features" className="text-gray-400 hover:text-white transition-colors">Care Gap Detection</a></li>
                <li><a href="#features" className="text-gray-400 hover:text-white transition-colors">HEDIS Evaluation</a></li>
                <li><a href="#features" className="text-gray-400 hover:text-white transition-colors">Risk Stratification</a></li>
                <li><a href="#features" className="text-gray-400 hover:text-white transition-colors">FHIR Integration</a></li>
                <li><a href="/explorer" className="text-gray-400 hover:text-white transition-colors">Analytics</a></li>
              </ul>
            </div>

            {/* Solutions */}
            <div>
              <h3 className="font-semibold text-sm uppercase tracking-wider mb-4">Solutions</h3>
              <ul className="space-y-3">
                <li><a href="/research" className="text-gray-400 hover:text-white transition-colors">For Health Plans</a></li>
                <li><a href="/research" className="text-gray-400 hover:text-white transition-colors">For ACOs</a></li>
                <li><a href="/research" className="text-gray-400 hover:text-white transition-colors">For Health Systems</a></li>
                <li><a href="/research" className="text-gray-400 hover:text-white transition-colors">For Medicaid MCOs</a></li>
              </ul>
            </div>

            {/* Company */}
            <div>
              <h3 className="font-semibold text-sm uppercase tracking-wider mb-4">Company</h3>
              <ul className="space-y-3">
                <li><a href="/research" className="text-gray-400 hover:text-white transition-colors">About</a></li>
                <li><a href="mailto:careers@hdim.io" className="text-gray-400 hover:text-white transition-colors">Careers</a></li>
                <li><a href="/research" className="text-gray-400 hover:text-white transition-colors">Blog</a></li>
                <li><a href="/schedule" className="text-gray-400 hover:text-white transition-colors">Contact</a></li>
                <li><a href="#compliance" className="text-gray-400 hover:text-white transition-colors">Security</a></li>
              </ul>
            </div>
          </div>

          <div className="mt-12 pt-8 border-t border-gray-800 flex flex-col md:flex-row justify-between items-center">
            <p className="text-gray-400 text-sm">
              &copy; {new Date().getFullYear()} HealthData-in-Motion. All rights reserved.
            </p>
            <div className="flex space-x-6 mt-4 md:mt-0">
              <a href="/privacy" className="text-gray-400 hover:text-white text-sm transition-colors">Privacy Policy</a>
              <a href="/terms" className="text-gray-400 hover:text-white text-sm transition-colors">Terms of Service</a>
              <a href="/privacy#cookies" className="text-gray-400 hover:text-white text-sm transition-colors">Cookie Policy</a>
            </div>
          </div>
        </div>
      </footer>
    </LandingPageClient>
  )
}
