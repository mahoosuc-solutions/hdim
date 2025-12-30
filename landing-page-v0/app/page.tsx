'use client'

import { useState, useEffect } from 'react'
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
  Menu,
  X,
  ArrowRight,
  Play,
  Star,
  Users,
  TrendingUp,
  Database,
  Lock,
  Award
} from 'lucide-react'

// Animated counter component
function AnimatedCounter({ end, suffix = '', prefix = '' }: { end: number; suffix?: string; prefix?: string }) {
  const [count, setCount] = useState(0)

  useEffect(() => {
    const duration = 2000
    const steps = 60
    const increment = end / steps
    let current = 0

    const timer = setInterval(() => {
      current += increment
      if (current >= end) {
        setCount(end)
        clearInterval(timer)
      } else {
        setCount(Math.floor(current))
      }
    }, duration / steps)

    return () => clearInterval(timer)
  }, [end])

  return <span>{prefix}{count.toLocaleString()}{suffix}</span>
}

// Cycling text animation for hero
function CyclingText() {
  const words = ['care gaps', 'HEDIS scores', 'Star Ratings', 'quality bonuses']
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isVisible, setIsVisible] = useState(true)

  useEffect(() => {
    const interval = setInterval(() => {
      setIsVisible(false)
      setTimeout(() => {
        setCurrentIndex((prev) => (prev + 1) % words.length)
        setIsVisible(true)
      }, 300)
    }, 3000)

    return () => clearInterval(interval)
  }, [])

  return (
    <span
      className={`inline-block transition-all duration-300 text-accent ${
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'
      }`}
    >
      {words[currentIndex]}
    </span>
  )
}

export default function LandingPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const features = [
    {
      icon: HeartPulse,
      title: 'Care Gap Detection',
      description: 'AI-powered identification of care gaps with prioritized outreach recommendations.',
    },
    {
      icon: BarChart3,
      title: 'HEDIS Evaluation',
      description: 'Automated quality measure calculation with real-time compliance tracking.',
    },
    {
      icon: LineChart,
      title: 'Risk Stratification',
      description: 'HCC-aligned scoring with predictive analytics for proactive care management.',
    },
    {
      icon: Database,
      title: 'FHIR R4 Integration',
      description: 'Native FHIR architecture with 20+ resource types and seamless EHR connectivity.',
    },
    {
      icon: Zap,
      title: 'CQL Engine',
      description: 'Execute clinical quality language directly - ready for digital HEDIS 2030.',
    },
    {
      icon: FileCheck,
      title: 'QRDA Export',
      description: 'One-click regulatory reporting with QRDA I/III generation.',
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
    <div className="min-h-screen bg-white">
      {/* Navigation */}
      <nav
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled ? 'bg-white/95 backdrop-blur-md shadow-sm' : 'bg-transparent'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16 md:h-20">
            {/* Logo */}
            <div className="flex items-center">
              <a href="/" className="flex items-center space-x-2">
                <div className="w-10 h-10 bg-gradient-to-br from-primary to-accent rounded-lg flex items-center justify-center">
                  <HeartPulse className="w-6 h-6 text-white" />
                </div>
                <span className={`text-xl font-bold ${scrolled ? 'text-gray-900' : 'text-white'}`}>
                  HDIM
                </span>
              </a>
            </div>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center space-x-8">
              <a
                href="#features"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Features
              </a>
              <a
                href="#solutions"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Solutions
              </a>
              <a
                href="#customers"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Customers
              </a>
              <a
                href="#resources"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Resources
              </a>
            </div>

            {/* CTA Buttons */}
            <div className="hidden md:flex items-center space-x-4">
              <a
                href="#demo"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Login
              </a>
              <a
                href="#demo"
                className="btn-primary text-sm"
              >
                Request Demo
              </a>
            </div>

            {/* Mobile menu button */}
            <button
              className="md:hidden p-2"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? (
                <X className={scrolled ? 'text-gray-900' : 'text-white'} />
              ) : (
                <Menu className={scrolled ? 'text-gray-900' : 'text-white'} />
              )}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-white border-t">
            <div className="px-4 py-4 space-y-3">
              <a href="#features" className="block text-gray-600 hover:text-primary">Features</a>
              <a href="#solutions" className="block text-gray-600 hover:text-primary">Solutions</a>
              <a href="#customers" className="block text-gray-600 hover:text-primary">Customers</a>
              <a href="#resources" className="block text-gray-600 hover:text-primary">Resources</a>
              <a href="#demo" className="btn-primary w-full text-center mt-4">Request Demo</a>
            </div>
          </div>
        )}
      </nav>

      {/* Hero Section */}
      <section className="relative min-h-screen flex items-center bg-gradient-to-br from-primary via-primary-600 to-primary-800 overflow-hidden">
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
                  href="#demo"
                  className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-primary bg-white rounded-lg shadow-xl hover:shadow-2xl hover:scale-105 transition-all duration-200"
                >
                  <Play className="w-5 h-5 mr-2" />
                  Try Interactive Demo
                </a>
                <a
                  href="#calculator"
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

      {/* Trust Bar - Customer Logos */}
      <section className="py-12 bg-gray-50 border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm text-gray-500 mb-8">
            Trusted by leading healthcare organizations
          </p>
          <div className="flex flex-wrap justify-center items-center gap-8 md:gap-16 opacity-60 grayscale hover:grayscale-0 hover:opacity-100 transition-all duration-500">
            {/* Placeholder logos - replace with actual customer logos */}
            {['Health System A', 'ACO Network', 'Regional MCO', 'Medicare Plan', 'Medicaid Health'].map((name, i) => (
              <div
                key={i}
                className="h-8 px-4 flex items-center justify-center text-gray-400 font-semibold text-sm border border-gray-200 rounded bg-white"
              >
                {name}
              </div>
            ))}
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

      {/* Solution Section */}
      <section className="py-20 bg-white" id="solutions">
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
                  '27 specialized microservices for modular deployment',
                  'HAPI FHIR 7.x with full R4 compliance',
                  'Real-time CQL execution for instant measure calculation',
                  'Multi-tenant architecture with HIPAA-compliant caching',
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

            {/* Product Screenshot Placeholder */}
            <div className="relative">
              <div className="bg-gradient-to-br from-primary/5 to-accent/5 rounded-2xl p-8">
                <div className="bg-white rounded-xl shadow-xl overflow-hidden">
                  <div className="h-8 bg-gray-100 flex items-center px-4 space-x-2">
                    <div className="w-3 h-3 bg-red-400 rounded-full" />
                    <div className="w-3 h-3 bg-yellow-400 rounded-full" />
                    <div className="w-3 h-3 bg-green-400 rounded-full" />
                  </div>
                  <div className="p-6 h-80 bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center">
                    <div className="text-center">
                      <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                        <Play className="w-10 h-10 text-primary" />
                      </div>
                      <p className="text-gray-500">Product Demo Preview</p>
                      <a href="#demo" className="text-primary font-medium hover:underline mt-2 inline-block">
                        Watch Full Demo
                      </a>
                    </div>
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
                  href="#"
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

      {/* Social Proof Section */}
      <section className="py-20 bg-white" id="customers">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="text-primary font-semibold text-sm uppercase tracking-wider">
              Customer Success
            </span>
            <h2 className="section-heading mt-2 mb-4">
              Proven Results Across Healthcare
            </h2>
          </div>

          {/* Testimonial Card */}
          <div className="max-w-4xl mx-auto">
            <div className="bg-gradient-to-br from-primary to-primary-700 rounded-2xl p-8 md:p-12 text-white relative overflow-hidden">
              {/* Quote mark */}
              <div className="absolute top-4 left-4 text-white/10 text-9xl font-serif">&ldquo;</div>

              <div className="relative z-10">
                <p className="text-xl md:text-2xl leading-relaxed mb-8">
                  &ldquo;HDIM transformed our quality program. We improved our HEDIS scores by 12 points in the first year and captured an additional $2.3M in quality bonuses. The care gap detection alone saved our team 20 hours per week.&rdquo;
                </p>

                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 bg-white/20 rounded-full flex items-center justify-center">
                    <Users className="w-7 h-7" />
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
          </div>

          {/* More case studies link */}
          <div className="text-center mt-10">
            <a
              href="#"
              className="inline-flex items-center text-primary font-medium hover:underline"
            >
              Read more customer stories
              <ChevronRight className="w-4 h-4 ml-1" />
            </a>
          </div>
        </div>
      </section>

      {/* Compliance & Trust Section */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-2xl font-bold text-gray-900">
              Enterprise-Grade Security & Compliance
            </h2>
          </div>

          <div className="flex flex-wrap justify-center items-center gap-8 md:gap-16">
            <div className="flex items-center space-x-3 bg-white px-6 py-4 rounded-lg shadow-sm">
              <Shield className="w-8 h-8 text-primary" />
              <div>
                <p className="font-semibold text-gray-900">HIPAA Compliant</p>
                <p className="text-sm text-gray-500">PHI Protection</p>
              </div>
            </div>
            <div className="flex items-center space-x-3 bg-white px-6 py-4 rounded-lg shadow-sm">
              <Lock className="w-8 h-8 text-primary" />
              <div>
                <p className="font-semibold text-gray-900">SOC 2 Type II</p>
                <p className="text-sm text-gray-500">Certified</p>
              </div>
            </div>
            <div className="flex items-center space-x-3 bg-white px-6 py-4 rounded-lg shadow-sm">
              <Award className="w-8 h-8 text-primary" />
              <div>
                <p className="font-semibold text-gray-900">HITRUST CSF</p>
                <p className="text-sm text-gray-500">Certified</p>
              </div>
            </div>
            <div className="flex items-center space-x-3 bg-white px-6 py-4 rounded-lg shadow-sm">
              <CheckCircle2 className="w-8 h-8 text-primary" />
              <div>
                <p className="font-semibold text-gray-900">FHIR R4</p>
                <p className="text-sm text-gray-500">Native Support</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-gradient-to-br from-primary via-primary-600 to-primary-800" id="demo">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
            Ready to Transform Your Quality Program?
          </h2>
          <p className="text-xl text-white/80 mb-10 max-w-2xl mx-auto">
            See how HDIM can help you close care gaps faster, improve HEDIS scores, and capture more quality bonuses.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <a
              href="#"
              className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-primary bg-white rounded-lg shadow-xl hover:shadow-2xl hover:scale-105 transition-all duration-200"
            >
              <Play className="w-5 h-5 mr-2" />
              Try Interactive Demo
            </a>
            <a
              href="#"
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
                <a href="#" className="text-gray-400 hover:text-white transition-colors">
                  <span className="sr-only">LinkedIn</span>
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
                  </svg>
                </a>
                <a href="#" className="text-gray-400 hover:text-white transition-colors">
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
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Care Gap Detection</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">HEDIS Evaluation</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Risk Stratification</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">FHIR Integration</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Analytics</a></li>
              </ul>
            </div>

            {/* Solutions */}
            <div>
              <h3 className="font-semibold text-sm uppercase tracking-wider mb-4">Solutions</h3>
              <ul className="space-y-3">
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">For Health Plans</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">For ACOs</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">For Health Systems</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">For Medicaid MCOs</a></li>
              </ul>
            </div>

            {/* Company */}
            <div>
              <h3 className="font-semibold text-sm uppercase tracking-wider mb-4">Company</h3>
              <ul className="space-y-3">
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">About</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Careers</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Blog</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Contact</a></li>
                <li><a href="#" className="text-gray-400 hover:text-white transition-colors">Security</a></li>
              </ul>
            </div>
          </div>

          <div className="mt-12 pt-8 border-t border-gray-800 flex flex-col md:flex-row justify-between items-center">
            <p className="text-gray-400 text-sm">
              &copy; {new Date().getFullYear()} HealthData-in-Motion. All rights reserved.
            </p>
            <div className="flex space-x-6 mt-4 md:mt-0">
              <a href="#" className="text-gray-400 hover:text-white text-sm transition-colors">Privacy Policy</a>
              <a href="#" className="text-gray-400 hover:text-white text-sm transition-colors">Terms of Service</a>
              <a href="#" className="text-gray-400 hover:text-white text-sm transition-colors">Cookie Policy</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
