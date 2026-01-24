'use client'

import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-4xl mx-auto px-4 py-6">
          <Link
            href="/"
            className="inline-flex items-center text-primary hover:underline mb-4"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Home
          </Link>
          <h1 className="text-3xl font-bold text-gray-900">Privacy Policy</h1>
          <p className="text-gray-600 mt-2">Last updated: December 30, 2025</p>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-4 py-12">
        <div className="bg-white rounded-xl shadow-sm p-8 space-y-8">

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">1. Introduction</h2>
            <p className="text-gray-700 leading-relaxed">
              HealthData-in-Motion (&quot;HDIM&quot;, &quot;we&quot;, &quot;us&quot;, or &quot;our&quot;) is committed to protecting
              your privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard
              your information when you use our healthcare interoperability platform and related services.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">2. HIPAA Compliance</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              As a healthcare technology platform, HDIM is designed to be fully compliant with the
              Health Insurance Portability and Accountability Act (HIPAA). We implement appropriate
              administrative, physical, and technical safeguards to protect Protected Health Information (PHI).
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>All PHI is encrypted at rest and in transit using AES-256 encryption</li>
              <li>Access to PHI is strictly controlled through role-based access controls</li>
              <li>All PHI access is logged and auditable</li>
              <li>We execute Business Associate Agreements (BAAs) with all covered entities</li>
              <li>PHI cache TTLs are limited to 5 minutes maximum</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">3. Information We Collect</h2>
            <h3 className="text-lg font-medium text-gray-800 mb-2">3.1 Information You Provide</h3>
            <ul className="list-disc pl-6 text-gray-700 space-y-2 mb-4">
              <li>Contact information (name, email, phone number)</li>
              <li>Organization details (company name, role, healthcare organization type)</li>
              <li>Account credentials</li>
              <li>Communication preferences</li>
            </ul>

            <h3 className="text-lg font-medium text-gray-800 mb-2">3.2 Protected Health Information</h3>
            <p className="text-gray-700 leading-relaxed">
              PHI processed through our platform is handled in accordance with HIPAA regulations
              and applicable Business Associate Agreements. We process PHI only as necessary to
              provide our services to covered entities.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">4. How We Use Your Information</h2>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>To provide and maintain our healthcare interoperability services</li>
              <li>To process quality measure evaluations and care gap detection</li>
              <li>To communicate with you about our services</li>
              <li>To comply with legal obligations and healthcare regulations</li>
              <li>To improve our platform and develop new features</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">5. Data Retention</h2>
            <p className="text-gray-700 leading-relaxed">
              We retain data in accordance with applicable healthcare regulations and contractual
              obligations. PHI retention periods are determined by applicable law and BAA terms.
              Non-PHI data is retained for the duration of your account plus 7 years for compliance purposes.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">6. Data Security</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              We implement comprehensive security measures including:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>SOC 2-aligned infrastructure</li>
              <li>End-to-end encryption (TLS 1.3 in transit, AES-256 at rest)</li>
              <li>Multi-factor authentication</li>
              <li>Regular security assessments and penetration testing</li>
              <li>24/7 security monitoring</li>
              <li>Disaster recovery and business continuity planning</li>
            </ul>
          </section>

          <section id="cookies">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">7. Cookies and Tracking</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              We use cookies and similar technologies to enhance your experience:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li><strong>Essential Cookies:</strong> Required for platform functionality</li>
              <li><strong>Analytics Cookies:</strong> Help us understand platform usage (anonymized)</li>
              <li><strong>Preference Cookies:</strong> Remember your settings and preferences</li>
            </ul>
            <p className="text-gray-700 leading-relaxed mt-4">
              You can control cookie preferences through your browser settings. Note that disabling
              essential cookies may impact platform functionality.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">8. Your Rights</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              Depending on your jurisdiction, you may have the right to:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>Access your personal information</li>
              <li>Correct inaccurate data</li>
              <li>Request deletion of your data (subject to legal retention requirements)</li>
              <li>Object to processing</li>
              <li>Data portability</li>
              <li>Withdraw consent</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">9. Contact Us</h2>
            <p className="text-gray-700 leading-relaxed">
              For privacy-related inquiries or to exercise your rights, contact us at:
            </p>
            <div className="mt-4 p-4 bg-gray-50 rounded-lg">
              <p className="text-gray-700">
                <strong>Privacy Officer</strong><br />
                HealthData-in-Motion<br />
                Email: privacy@hdim.io<br />
                Phone: (555) 123-4567
              </p>
            </div>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">10. Changes to This Policy</h2>
            <p className="text-gray-700 leading-relaxed">
              We may update this Privacy Policy from time to time. We will notify you of any
              material changes by posting the new policy on this page and updating the
              &quot;Last updated&quot; date.
            </p>
          </section>

        </div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-8 mt-12">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <p className="text-gray-400">
            &copy; {new Date().getFullYear()} HealthData-in-Motion. All rights reserved.
          </p>
          <div className="mt-4 space-x-6">
            <Link href="/privacy" className="text-gray-400 hover:text-white">Privacy Policy</Link>
            <Link href="/terms" className="text-gray-400 hover:text-white">Terms of Service</Link>
            <Link href="/schedule" className="text-gray-400 hover:text-white">Contact</Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
