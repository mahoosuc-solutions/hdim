'use client'

import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'

export default function TermsPage() {
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
          <h1 className="text-3xl font-bold text-gray-900">Terms of Service</h1>
          <p className="text-gray-600 mt-2">Last updated: December 30, 2025</p>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-4 py-12">
        <div className="bg-white rounded-xl shadow-sm p-8 space-y-8">

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">1. Agreement to Terms</h2>
            <p className="text-gray-700 leading-relaxed">
              By accessing or using the HealthData-in-Motion (&quot;HDIM&quot;) platform and services,
              you agree to be bound by these Terms of Service. If you do not agree to these terms,
              you may not access or use our services.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">2. Description of Services</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              HDIM provides a healthcare interoperability platform that includes:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>HEDIS quality measure evaluation and reporting</li>
              <li>FHIR R4 compliant data integration</li>
              <li>Care gap detection and management</li>
              <li>Risk stratification and predictive analytics</li>
              <li>Clinical decision support tools</li>
              <li>Quality reporting and QRDA export</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">3. Eligibility</h2>
            <p className="text-gray-700 leading-relaxed">
              Our services are available only to healthcare organizations that are authorized to
              process Protected Health Information (PHI) under HIPAA. By using our services, you
              represent that you are a Covered Entity or Business Associate under HIPAA, or are
              otherwise authorized to access and process PHI.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">4. Account Registration</h2>
            <p className="text-gray-700 leading-relaxed">
              To use our services, you must register for an account and provide accurate, complete,
              and current information. You are responsible for maintaining the confidentiality of
              your account credentials and for all activities that occur under your account.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">5. HIPAA Compliance</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              HDIM operates as a Business Associate under HIPAA. Before processing any PHI,
              we require execution of a Business Associate Agreement (BAA). You agree to:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>Execute a BAA with HDIM prior to transmitting any PHI</li>
              <li>Only transmit PHI that you are authorized to share</li>
              <li>Comply with all applicable HIPAA requirements</li>
              <li>Promptly report any suspected security incidents</li>
              <li>Cooperate with breach notification requirements</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">6. Acceptable Use</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              You agree not to:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li>Use the services for any unlawful purpose</li>
              <li>Attempt to gain unauthorized access to any systems</li>
              <li>Interfere with or disrupt the integrity of the platform</li>
              <li>Transmit viruses, malware, or other harmful code</li>
              <li>Use the services to process data you are not authorized to access</li>
              <li>Reverse engineer or attempt to extract source code</li>
              <li>Resell or sublicense access to the services without authorization</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">7. Intellectual Property</h2>
            <p className="text-gray-700 leading-relaxed">
              The HDIM platform, including all software, algorithms, user interfaces, and documentation,
              is owned by HDIM and protected by intellectual property laws. You are granted a limited,
              non-exclusive license to use the services for your internal business purposes only.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">8. Data Ownership</h2>
            <p className="text-gray-700 leading-relaxed">
              You retain all ownership rights to your data, including PHI. HDIM processes your data
              solely to provide the services and does not use your data for any other purpose without
              your explicit consent.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">9. Service Level Agreement</h2>
            <p className="text-gray-700 leading-relaxed mb-4">
              HDIM commits to the following service levels:
            </p>
            <ul className="list-disc pl-6 text-gray-700 space-y-2">
              <li><strong>Uptime:</strong> 99.9% availability (excluding scheduled maintenance)</li>
              <li><strong>Support:</strong> 24/7 critical incident support</li>
              <li><strong>Response Time:</strong> Critical issues acknowledged within 1 hour</li>
              <li><strong>Data Backup:</strong> Daily backups with 30-day retention</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">10. Limitation of Liability</h2>
            <p className="text-gray-700 leading-relaxed">
              TO THE MAXIMUM EXTENT PERMITTED BY LAW, HDIM SHALL NOT BE LIABLE FOR ANY INDIRECT,
              INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, INCLUDING LOSS OF PROFITS,
              DATA, OR BUSINESS OPPORTUNITIES. OUR TOTAL LIABILITY SHALL NOT EXCEED THE FEES PAID
              BY YOU IN THE TWELVE MONTHS PRECEDING THE CLAIM.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">11. Indemnification</h2>
            <p className="text-gray-700 leading-relaxed">
              You agree to indemnify and hold HDIM harmless from any claims, damages, or expenses
              arising from your use of the services, your violation of these terms, or your violation
              of any applicable laws or regulations.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">12. Termination</h2>
            <p className="text-gray-700 leading-relaxed">
              Either party may terminate this agreement with 30 days written notice. Upon termination,
              you will have 30 days to export your data. HDIM will securely delete your data within
              90 days of termination, except as required by law or the BAA.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">13. Governing Law</h2>
            <p className="text-gray-700 leading-relaxed">
              These terms shall be governed by and construed in accordance with the laws of the
              State of Delaware, without regard to its conflict of law provisions. Any disputes
              shall be resolved in the state or federal courts located in Delaware.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">14. Changes to Terms</h2>
            <p className="text-gray-700 leading-relaxed">
              We may modify these terms at any time. Material changes will be communicated via
              email or platform notification at least 30 days before taking effect. Continued use
              of the services after changes take effect constitutes acceptance of the modified terms.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">15. Contact Information</h2>
            <p className="text-gray-700 leading-relaxed">
              For questions about these terms, contact us at:
            </p>
            <div className="mt-4 p-4 bg-gray-50 rounded-lg">
              <p className="text-gray-700">
                <strong>Legal Department</strong><br />
                HealthData-in-Motion<br />
                Email: legal@hdim.io<br />
                Phone: (555) 123-4567
              </p>
            </div>
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
