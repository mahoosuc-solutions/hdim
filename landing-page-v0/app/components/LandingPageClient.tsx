'use client'

import { useState, useEffect, ReactNode } from 'react'
import {
  HeartPulse,
  Menu,
  X,
  Play,
} from 'lucide-react'

interface LandingPageClientProps {
  children: ReactNode
}

export function LandingPageClient({ children }: LandingPageClientProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const [showStickyCTA, setShowStickyCTA] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
      setShowStickyCTA(window.scrollY > 600)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  return (
    <div className="min-h-screen bg-white">
      {/* Skip to main content link for accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-[60] focus:px-4 focus:py-2 focus:bg-white focus:text-primary focus:rounded"
      >
        Skip to main content
      </a>

      {/* Urgency Banner */}
      <div className="bg-gradient-to-r from-red-600 to-red-700 text-white py-2 px-4 text-center text-sm fixed top-0 left-0 right-0 z-50">
        <span className="font-semibold">HEDIS 2025 Season:</span> Measurement year ends soon - schedule your demo today.
        <a href="#demo" className="underline ml-2 hover:text-white/90">Get started &rarr;</a>
      </div>

      {/* Navigation */}
      <nav
        className={`fixed top-8 left-0 right-0 z-50 transition-all duration-300 ${
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
                href="/research"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Research
              </a>
              <a
                href="/explorer"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Data Explorer
              </a>
              <a
                href="/downloads"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Sample Data
              </a>
              <a
                href="/pricing"
                className={`text-sm font-medium transition-colors ${
                  scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
                }`}
              >
                Pricing
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
              aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
              aria-expanded={mobileMenuOpen}
              aria-controls="mobile-menu"
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
          <div id="mobile-menu" className="md:hidden bg-white border-t">
            <div className="px-4 py-4 space-y-3">
              <a href="#features" className="block text-gray-600 hover:text-primary">Features</a>
              <a href="#solutions" className="block text-gray-600 hover:text-primary">Solutions</a>
              <a href="/research" className="block text-gray-600 hover:text-primary">Research</a>
              <a href="/explorer" className="block text-gray-600 hover:text-primary">Data Explorer</a>
              <a href="/downloads" className="block text-gray-600 hover:text-primary">Sample Data</a>
              <a href="/pricing" className="block text-gray-600 hover:text-primary">Pricing</a>
              <a href="/demo" className="btn-primary w-full text-center mt-4">Try Demo</a>
            </div>
          </div>
        )}
      </nav>

      {children}

      {/* Sticky Mobile CTA */}
      {showStickyCTA && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t shadow-lg py-3 px-4 z-40 md:hidden">
          <a
            href="/demo"
            className="flex items-center justify-center gap-2 w-full bg-primary text-white text-center py-3 rounded-lg font-semibold hover:bg-primary-600 transition-colors"
          >
            <Play className="w-4 h-4" />
            Try Interactive Demo
          </a>
        </div>
      )}
    </div>
  )
}
