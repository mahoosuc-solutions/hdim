'use client'

import { useState } from 'react'
import Link from 'next/link'
import {
  Mail,
  Send,
  CheckCircle2,
  ArrowLeft,
  Calendar,
  MessageSquare,
  Linkedin,
  Github
} from 'lucide-react'

declare global {
  interface Window {
    grecaptcha: {
      execute: (siteKey: string, options: { action: string }) => Promise<string>
    }
    gtag: (...args: unknown[]) => void
  }
}

const RECAPTCHA_SITE_KEY = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY ?? ''

export default function ContactPage() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    company: '',
    phone: '',
    subject: '',
    message: ''
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSubmitted, setIsSubmitted] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)
    setError('')

    try {
      let recaptchaToken = ''
      if (typeof window !== 'undefined' && window.grecaptcha && RECAPTCHA_SITE_KEY) {
        recaptchaToken = await window.grecaptcha.execute(RECAPTCHA_SITE_KEY, { action: 'contact_page' })
      }

      const res = await fetch('/api/leads', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...formData, source: 'contact_page', recaptchaToken }),
      })

      if (!res.ok) {
        const data = await res.json().catch(() => ({}))
        throw new Error(data.error || 'Submission failed')
      }

      setIsSubmitted(true)

      if (typeof window !== 'undefined' && window.gtag) {
        window.gtag('event', 'generate_lead', { source: 'contact_page' })
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const contactMethods = [
    {
      icon: Mail,
      title: 'General Inquiries',
      detail: 'info@mahoosuc.solutions',
      description: 'General questions and support',
      action: 'mailto:info@mahoosuc.solutions'
    },
    {
      icon: Mail,
      title: 'Sales',
      detail: 'sales@mahoosuc.solutions',
      description: 'Pricing and partnership questions',
      action: 'mailto:sales@mahoosuc.solutions'
    },
    {
      icon: Calendar,
      title: 'Schedule a Demo',
      detail: 'Book a time',
      description: '30 or 60 minute sessions available',
      action: '/schedule'
    },
    {
      icon: MessageSquare,
      title: 'Send a Message',
      detail: 'Use the form below',
      description: 'We respond within 24 hours',
      action: '#contact-form'
    }
  ]

  if (isSubmitted) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gray-50 to-white flex items-center justify-center p-4">
        <div className="max-w-md w-full text-center">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle2 className="w-8 h-8 text-green-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-4">
            Message Sent Successfully!
          </h1>
          <p className="text-gray-600 mb-8">
            Thank you for reaching out. Our team will get back to you within 24 hours.
          </p>
          <Link
            href="/"
            className="inline-flex items-center justify-center w-full px-6 py-3 border border-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-50 transition-colors"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            Return to Home
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 to-indigo-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h1 className="text-5xl font-bold text-gray-900 mb-6">
              Get In Touch
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Have questions about HDIM? We're here to help. Reach out to our team for demos,
              pricing information, or technical support.
            </p>
          </div>
        </div>
      </section>

      {/* Contact Methods */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-20">
            {contactMethods.map((method, idx) => (
              <a
                key={idx}
                href={method.action}
                className="bg-white border border-gray-200 rounded-xl p-6 hover:shadow-lg transition-shadow"
              >
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
                  <method.icon className="w-6 h-6 text-blue-600" />
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">
                  {method.title}
                </h3>
                <p className="text-blue-600 font-semibold mb-2">
                  {method.detail}
                </p>
                <p className="text-sm text-gray-600">
                  {method.description}
                </p>
              </a>
            ))}
          </div>

          <div className="grid lg:grid-cols-2 gap-12">
            {/* Contact Form */}
            <div id="contact-form">
              <h2 className="text-3xl font-bold text-gray-900 mb-6">
                Send Us a Message
              </h2>
              <form onSubmit={handleSubmit} className="space-y-6">
                <div className="grid sm:grid-cols-2 gap-6">
                  <div>
                    <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                      Name *
                    </label>
                    <input
                      type="text"
                      id="name"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({...formData, name: e.target.value})}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      placeholder="John Smith"
                    />
                  </div>
                  <div>
                    <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                      Email *
                    </label>
                    <input
                      type="email"
                      id="email"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({...formData, email: e.target.value})}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      placeholder="john@healthplan.com"
                    />
                  </div>
                </div>

                <div className="grid sm:grid-cols-2 gap-6">
                  <div>
                    <label htmlFor="company" className="block text-sm font-medium text-gray-700 mb-2">
                      Company
                    </label>
                    <input
                      type="text"
                      id="company"
                      value={formData.company}
                      onChange={(e) => setFormData({...formData, company: e.target.value})}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Regional Health Plan"
                    />
                  </div>
                  <div>
                    <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-2">
                      Phone
                    </label>
                    <input
                      type="tel"
                      id="phone"
                      value={formData.phone}
                      onChange={(e) => setFormData({...formData, phone: e.target.value})}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      placeholder="(555) 123-4567"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="subject" className="block text-sm font-medium text-gray-700 mb-2">
                    Subject *
                  </label>
                  <input
                    type="text"
                    id="subject"
                    required
                    value={formData.subject}
                    onChange={(e) => setFormData({...formData, subject: e.target.value})}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="How can we help you?"
                  />
                </div>

                <div>
                  <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-2">
                    Message *
                  </label>
                  <textarea
                    id="message"
                    required
                    rows={6}
                    value={formData.message}
                    onChange={(e) => setFormData({...formData, message: e.target.value})}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="Tell us more about your needs..."
                  />
                </div>

                {error && (
                  <div className="bg-red-50 text-red-600 px-4 py-3 rounded-lg text-sm">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full flex items-center justify-center px-6 py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? (
                    'Sending...'
                  ) : (
                    <>
                      <Send className="w-5 h-5 mr-2" />
                      Send Message
                    </>
                  )}
                </button>
              </form>
            </div>

            {/* Connect With Us */}
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-6">
                Connect With Us
              </h2>

              <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-8 mb-8">
                <p className="text-gray-700 mb-6">
                  We're a remote-first team building the future of healthcare quality measurement. Reach us at:
                </p>
                <div className="space-y-3">
                  <a
                    href="mailto:sales@mahoosuc.solutions"
                    className="flex items-center text-blue-600 hover:text-blue-800 font-medium"
                  >
                    <Mail className="w-5 h-5 mr-3 flex-shrink-0" />
                    sales@mahoosuc.solutions
                  </a>
                  <a
                    href="mailto:info@mahoosuc.solutions"
                    className="flex items-center text-blue-600 hover:text-blue-800 font-medium"
                  >
                    <Mail className="w-5 h-5 mr-3 flex-shrink-0" />
                    info@mahoosuc.solutions
                  </a>
                </div>
              </div>

              <div className="bg-gray-50 rounded-xl p-8">
                <h3 className="text-xl font-bold text-gray-900 mb-4">
                  Follow Our Work
                </h3>
                <div className="flex gap-4">
                  <a
                    href="https://www.linkedin.com/company/mahoosuc-solutions"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-12 h-12 bg-white rounded-lg flex items-center justify-center hover:shadow-md transition-shadow"
                    aria-label="LinkedIn"
                  >
                    <Linkedin className="w-6 h-6 text-blue-600" />
                  </a>
                  <a
                    href="https://github.com/webemo-aaron"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-12 h-12 bg-white rounded-lg flex items-center justify-center hover:shadow-md transition-shadow"
                    aria-label="GitHub"
                  >
                    <Github className="w-6 h-6 text-gray-900" />
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
