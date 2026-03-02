import Link from 'next/link'
import { HeartPulse } from 'lucide-react'

export function SiteFooter() {
  return (
    <footer className="bg-gray-900 text-gray-400 py-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center space-x-2 mb-4">
              <div className="w-8 h-8 bg-gradient-to-br from-primary to-accent rounded-lg flex items-center justify-center">
                <HeartPulse className="w-5 h-5 text-white" />
              </div>
              <span className="text-white font-bold">HDIM</span>
            </div>
            <p className="text-sm">FHIR-native healthcare quality measurement platform.</p>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Solutions</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/health-plans" className="hover:text-white transition-colors">Health Plans</Link></li>
              <li><Link href="/health-systems" className="hover:text-white transition-colors">Health Systems</Link></li>
              <li><Link href="/acos" className="hover:text-white transition-colors">ACOs</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Platform</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/platform" className="hover:text-white transition-colors">Capabilities</Link></li>
              <li><Link href="/security" className="hover:text-white transition-colors">Security</Link></li>
              <li><Link href="/pricing" className="hover:text-white transition-colors">Pricing</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Company</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/about" className="hover:text-white transition-colors">About</Link></li>
              <li><Link href="/contact" className="hover:text-white transition-colors">Contact</Link></li>
              <li><Link href="/privacy" className="hover:text-white transition-colors">Privacy</Link></li>
              <li><Link href="/terms" className="hover:text-white transition-colors">Terms</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-12 pt-8 text-center text-sm">
          <p>&copy; 2026 HealthData-in-Motion. All rights reserved.</p>
        </div>
      </div>
    </footer>
  )
}
