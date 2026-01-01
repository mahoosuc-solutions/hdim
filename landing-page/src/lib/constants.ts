// Landing page copy and messaging
export const MESSAGING = {
  // Hero Section
  HERO_HEADLINE: 'Calculate Clinical Value in Real-Time, Your Way',
  HERO_SUBHEADLINE:
    'Deploy HDIM your way: Pilot single-node, scale to enterprise. Integrate with Epic, Cerner, Athena, or any FHIR server. Zero vendor lock-in.',
  HERO_CTA: 'Schedule Demo',
  HERO_SECONDARY_CTA: 'Explore Architectures',

  // Problem Statement
  PROBLEM_HEADLINE: "Healthcare Quality Measurement Has a Data Problem",
  PROBLEM_DESCRIPTION: 'Manual processes, siloed data, and vendor lock-in slow down clinical insights.',
  
  // Solution Overview
  SOLUTION_HEADLINE: 'Real-Time Intelligence Without Moving Your Data',
  SOLUTION_DESCRIPTION:
    'HDIM's gateway architecture queries your FHIR servers directly, evaluates 52+ HEDIS measures in real-time, and identifies care gaps instantly.',

  // Deployment Models
  DEPLOYMENT_HEADLINE: 'Start Small, Scale as You Grow',
  DEPLOYMENT_DESCRIPTION: 'Five deployment models to match your organization, from $500/month to enterprise scale.',

  // Customization
  CUSTOMIZATION_HEADLINE: 'From 52 Pre-Built Measures to Unlimited Customization',
  CUSTOMIZATION_DESCRIPTION: 'Start with HEDIS. Add custom measures, integrations, and AI/ML as you grow.',

  // ROI
  ROI_HEADLINE: 'See Your Financial Impact',
  ROI_DESCRIPTION: 'Calculate your specific ROI based on organization type, patient volume, and improvement targets.',

  // Trust & Compliance
  TRUST_HEADLINE: 'Enterprise-Grade Security & HIPAA Compliance',
  TRUST_DESCRIPTION: 'Multi-tenant isolation, HIPAA audit logging, TLS 1.3 encryption, role-based access control.',

  // Pricing
  PRICING_HEADLINE: 'Transparent Pricing for Every Stage',
  PRICING_DESCRIPTION: 'No enterprise minimums. Pay for what you need, upgrade as you grow.',

  // CTA
  DEMO_CTA: "Schedule a 30-minute demo with our healthcare experts",
  CONTACT_CTA: "Have questions? Let's talk",
}

// Deployment models configuration
export const DEPLOYMENT_MODELS = [
  {
    id: 'pilot',
    name: 'Pilot',
    description: 'Single-node Docker deployment for proof of concept',
    patients: 'Up to 50K',
    timeline: '2-3 weeks',
    cost: '$500/month',
    infrastructure: {
      servers: 1,
      cpu: '4 CPUs',
      memory: '16 GB RAM',
      storage: '500 GB SSD',
    },
    features: [
      '52 HEDIS measures',
      'Basic dashboards',
      'Single FHIR server',
      'Email notifications',
      'Basic reporting',
    ],
    useCases: ['Proof of concept', 'Small practice pilot', 'Integration testing'],
  },
  {
    id: 'growth',
    name: 'Growth',
    description: 'Clustered deployment with high availability',
    patients: '50K - 500K',
    timeline: '4-8 weeks',
    cost: '$2,500/month',
    infrastructure: {
      servers: '3-5',
      cpu: '8+ CPUs per server',
      memory: '32 GB RAM per server',
      storage: '1 TB SSD per server',
    },
    features: [
      'All Pilot features',
      'High availability',
      'Multiple FHIR servers',
      'Custom dashboards',
      'Advanced reporting',
      'SMS notifications',
    ],
    useCases: ['Growing health system', 'ACO network', 'Regional deployment'],
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    description: 'Kubernetes deployment for maximum scale',
    patients: '500K+',
    timeline: '8-12 weeks',
    cost: '$5,000-15,000/month',
    infrastructure: {
      servers: '10+',
      cpu: '16+ CPUs',
      memory: '64 GB+ RAM',
      storage: '5+ TB distributed',
    },
    features: [
      'All Growth features',
      'Kubernetes orchestration',
      'Multi-region deployment',
      'Custom measures',
      'SMART on FHIR',
      'CDS Hooks integration',
      'Advanced analytics',
    ],
    useCases: ['Large health system', 'National ACO', 'Payer organization'],
  },
  {
    id: 'hybrid',
    name: 'Hybrid Cloud',
    description: 'On-premise gateway with cloud compute',
    patients: '100K - 2M+',
    timeline: '6-10 weeks',
    cost: '$10,000-20,000/month',
    infrastructure: {
      servers: 'On-prem: 3+, Cloud: elastic',
      cpu: 'Mixed on-prem/cloud',
      memory: 'On-prem: 64GB+, Cloud: auto-scaling',
      storage: 'Hybrid distributed',
    },
    features: [
      'All Enterprise features',
      'Multi-region capability',
      'On-premise data residency',
      'Cloud auto-scaling',
      'Disaster recovery',
      'Global failover',
    ],
    useCases: ['Multi-region health system', 'International deployments', 'Strict data residency requirements'],
  },
]

// Customer scenarios with ROI
export const CUSTOMER_SCENARIOS = [
  {
    id: 'solo-practice',
    name: 'Solo Practice (Epic)',
    description: 'Single primary care practice with 50K patients',
    ehr: 'Epic',
    ehrMarketShare: '36% US',
    currentState: {
      patients: 50000,
      manualHours: 8,
      quality_metrics: 12,
      gap_closure: '15%',
    },
    hdimOutcome: {
      timeline: '3 weeks',
      deployment: 'Pilot (Single-Node)',
      measures: '52 HEDIS',
      automation: '80% of quality work',
    },
    roi: {
      year1: {
        laborSavings: 18000,
        qualityBonus: 25000,
        implementationCost: 6000,
        netROI: 37000,
        payback: '1.5 months',
        roiPercent: '517%',
      },
    },
    keyBenefits: ['Immediate labor savings', 'Quality bonus qualification', 'Scalable foundation'],
  },
  {
    id: 'regional-health-system',
    name: 'Regional Health System (Epic + Cerner)',
    description: '500K patient health system with multiple EHRs',
    ehr: 'Epic + Cerner',
    ehrMarketShare: '63% US combined',
    currentState: {
      patients: 500000,
      manualHours: 400,
      quality_metrics: 45,
      gap_closure: '25%',
      multi_ehr_complexity: 'High',
    },
    hdimOutcome: {
      timeline: '8 weeks',
      deployment: 'Growth (Clustered)',
      measures: '52 HEDIS + Custom',
      automation: 'Multi-EHR unified view',
    },
    roi: {
      year1: {
        laborSavings: 800000,
        qualityBonus: 2300000,
        implementationCost: 60000,
        netROI: 3040000,
        payback: '1 week',
        roiPercent: '5067%',
      },
    },
    keyBenefits: [
      'Multi-EHR consolidation',
      'Massive quality bonus impact',
      'Unified operations dashboard',
    ],
  },
  {
    id: 'aco-network',
    name: 'ACO Network (Multi-EHR)',
    description: 'ACO with 20 clinics across multiple EHRs',
    ehr: 'Epic, Cerner, Athena, Generic FHIR',
    ehrMarketShare: '96% US coverage',
    currentState: {
      patients: 150000,
      clinics: 20,
      fragmented_reporting: true,
      gap_closure: '20%',
      coordination_delay: '3-5 days',
    },
    hdimOutcome: {
      timeline: '10 weeks',
      deployment: 'Enterprise (Kubernetes)',
      measures: '52 HEDIS + 10 custom',
      automation: 'Real-time unified reporting',
    },
    roi: {
      year1: {
        laborSavings: 500000,
        qualityBonus: 1200000,
        implementationCost: 80000,
        netROI: 1620000,
        payback: '1 month',
        roiPercent: '2025%',
      },
    },
    keyBenefits: [
      'Coordinated care gap management',
      'Real-time network visibility',
      '4x faster gap identification',
    ],
  },
  {
    id: 'payer',
    name: 'Payer (Claims + FHIR)',
    description: 'Regional payer managing Star ratings for 500K members',
    ehr: 'Claims data + FHIR',
    ehrMarketShare: 'Custom integration',
    currentState: {
      members: 500000,
      star_ratings: 'Manual calculation',
      update_frequency: 'Quarterly',
      member_engagement: 'Low',
    },
    hdimOutcome: {
      timeline: '12 weeks',
      deployment: 'Enterprise (Kubernetes + Hybrid)',
      measures: '52 HEDIS + Star-specific',
      automation: 'Real-time Star calculation',
    },
    roi: {
      year1: {
        laborSavings: 1200000,
        qualityBonus: 8500000,
        memberEngagement: 3000000,
        implementationCost: 120000,
        netROI: 12580000,
        payback: '< 1 month',
        roiPercent: '10483%',
      },
    },
    keyBenefits: [
      'Real-time Star rating optimization',
      'Member-targeted interventions',
      'Massive quality bonus opportunity',
    ],
  },
]

// Customization levels
export const CUSTOMIZATION_LEVELS = [
  {
    level: 1,
    name: 'Pre-Built',
    description: '52 HEDIS measures with basic dashboards',
    features: [
      '52 HEDIS measures',
      'Patient-level dashboards',
      'Care gap reporting',
      'Email notifications',
    ],
    timeline: 'Included',
    cost: '$500/month',
    customerType: 'Pilot',
  },
  {
    level: 2,
    name: 'Configuration',
    description: 'Custom dashboards, thresholds, workflows',
    features: [
      'All Level 1 features',
      'Custom dashboard layouts',
      'Threshold customization',
      'Workflow automation rules',
      'SMS notifications',
    ],
    timeline: '2-4 weeks',
    cost: '+$2,500/month',
    customerType: 'Growing',
  },
  {
    level: 3,
    name: 'Custom Measures',
    description: 'Organization-specific clinical measures',
    features: [
      'All Level 2 features',
      'Custom CQL measures',
      'Proprietary metric support',
      'Clinical workflow integration',
      'Advanced reporting',
    ],
    timeline: '2-4 weeks per measure',
    cost: '+$3K-8K per measure',
    customerType: 'Competitive',
  },
  {
    level: 4,
    name: 'Integrations',
    description: 'SMART on FHIR, CDS Hooks, BI tools',
    features: [
      'All Level 3 features',
      'SMART on FHIR apps',
      'CDS Hooks for EHR alerts',
      'BI tool connectors',
      'Custom data exports',
    ],
    timeline: '8-16 weeks',
    cost: '+$8K-25K',
    customerType: 'Enterprise',
  },
  {
    level: 5,
    name: 'Advanced',
    description: 'AI/ML models, proprietary analytics',
    features: [
      'All Level 4 features',
      'Readmission risk prediction',
      'Cost forecasting',
      'Disease progression modeling',
      'Behavioral analytics',
    ],
    timeline: '12-24 weeks',
    cost: '+$25K-100K+',
    customerType: 'Strategic',
  },
]

// Pricing tiers
export const PRICING_TIERS = [
  {
    name: 'Pilot',
    price: 500,
    period: '/month',
    description: 'Single-node proof of concept',
    features: [
      'Up to 50K patients',
      '52 HEDIS measures',
      'Single FHIR server',
      'Basic dashboards',
      'Email notifications',
      'Community support',
    ],
    cta: 'Start Free Trial',
    highlighted: false,
  },
  {
    name: 'Growth',
    price: 2500,
    period: '/month',
    description: 'High-availability clustered deployment',
    features: [
      '50K - 500K patients',
      '52 HEDIS + custom measures',
      'Multiple FHIR servers',
      'Advanced dashboards',
      'SMS notifications',
      'Priority support',
      'Custom thresholds',
    ],
    cta: 'Schedule Demo',
    highlighted: true,
  },
  {
    name: 'Enterprise',
    price: null,
    period: 'Custom',
    description: 'Kubernetes deployment with full customization',
    features: [
      '500K+ patients',
      'All measures + AI/ML',
      'Multi-region deployment',
      'SMART on FHIR',
      'CDS Hooks',
      '24/7 support',
      'Custom integrations',
    ],
    cta: 'Contact Sales',
    highlighted: false,
  },
]

// Feature comparison
export const FEATURE_COMPARISON = {
  categories: [
    {
      name: 'Core Capabilities',
      features: [
        { name: 'Pre-built HEDIS measures', hdim: true, competitors: true },
        { name: 'Real-time measure calculation', hdim: true, competitors: false },
        { name: 'Custom measure support', hdim: true, competitors: 'Limited' },
        { name: 'Multi-EHR support', hdim: true, competitors: false },
        { name: 'Care gap detection', hdim: true, competitors: true },
      ],
    },
    {
      name: 'Deployment',
      features: [
        { name: 'Single-node deployment', hdim: true, competitors: false },
        { name: 'On-premise option', hdim: true, competitors: 'Cloud-only' },
        { name: 'Kubernetes support', hdim: true, competitors: true },
        { name: 'Hybrid cloud', hdim: true, competitors: false },
        { name: 'Multi-region', hdim: true, competitors: 'Enterprise only' },
      ],
    },
    {
      name: 'Compliance & Security',
      features: [
        { name: 'HIPAA compliance', hdim: true, competitors: true },
        { name: 'Multi-tenant isolation', hdim: true, competitors: 'Limited' },
        { name: 'HIPAA audit logging', hdim: true, competitors: true },
        { name: 'SOC 2 certified', hdim: true, competitors: true },
        { name: 'BAA included', hdim: true, competitors: true },
      ],
    },
    {
      name: 'Integrations',
      features: [
        { name: 'Epic integration', hdim: true, competitors: true },
        { name: 'Cerner integration', hdim: true, competitors: 'Limited' },
        { name: 'Athena integration', hdim: true, competitors: false },
        { name: 'SMART on FHIR', hdim: true, competitors: false },
        { name: 'CDS Hooks', hdim: true, competitors: false },
      ],
    },
  ],
}

// Case studies
export const CASE_STUDIES = [
  {
    id: 'case-study-1',
    organization: 'Midwest Regional Health',
    logo: '/logos/midwest-health.svg',
    metric: '1,800% ROI in Year 1',
    challenge: 'Manual quality measure tracking across 8 clinics with Epic',
    solution: 'Deployed HDIM Growth tier in 6 weeks',
    results: [
      '+1.5 HEDIS points (quality bonus)',
      '400 hours/year labor savings',
      '$2.1M additional quality bonus revenue',
      '85% automated care gap detection',
    ],
    quote:
      "HDIM transformed how we manage clinical quality. We went from manual spreadsheet tracking to real-time insights across all our clinics. The ROI was immediate.",
    quoteAuthor: 'Chief Medical Officer, Midwest Regional Health',
    image: '/case-studies/midwest-health.jpg',
  },
  {
    id: 'case-study-2',
    organization: 'Community Care ACO',
    logo: '/logos/community-care.svg',
    metric: '4.2x Patient Care Improvement',
    challenge: 'Multi-EHR coordination across 20 clinics (Epic, Cerner, Athena)',
    solution: 'Deployed HDIM Enterprise tier with multi-EHR support',
    results: [
      'Unified view across all EHRs',
      '65% gap closure rate (up from 20%)',
      '$1.2M annual quality bonuses',
      '5-day faster care gap identification',
    ],
    quote:
      "Before HDIM, coordinating quality across multiple EHRs was impossible. Now we have real-time visibility that drives 4x faster clinical decisions.",
    quoteAuthor: 'Executive Director, Community Care ACO',
    image: '/case-studies/community-care.jpg',
  },
  {
    id: 'case-study-3',
    organization: 'Strategic Health Payer',
    logo: '/logos/strategic-health.svg',
    metric: '$8.5M Quality Bonus Uplift',
    challenge: 'Real-time Star rating calculation for 500K members',
    solution: 'Custom HDIM deployment with claims data integration',
    results: [
      'Real-time Star rating optimization',
      '+2.5 Star rating points',
      '$8.5M quality bonus improvement',
      'Member engagement increase from 25% to 61%',
    ],
    quote:
      "HDIM enabled us to identify gaps in real-time rather than quarterly. The impact on our Star ratings and member outcomes has been transformational.",
    quoteAuthor: 'VP of Quality, Strategic Health Payer',
    image: '/case-studies/strategic-health.jpg',
  },
]

// FAQ
export const FAQ = [
  {
    question: 'How long does deployment take?',
    answer:
      'Pilot deployment takes 2-3 weeks, Growth tier 4-8 weeks, and Enterprise 8-12 weeks. The timeline depends on your infrastructure readiness and integration complexity.',
  },
  {
    question: 'Do you support our EHR?',
    answer:
      'We support Epic, Cerner, Athena, and any FHIR R4-compliant server. If your EHR supports FHIR, we can integrate with it.',
  },
  {
    question: 'Can we customize the measures?',
    answer:
      'Yes! We include 52 HEDIS measures out of the box, and you can add unlimited custom measures using our CQL framework. Custom measures start at $3K-8K each.',
  },
  {
    question: 'Is our data secure and compliant?',
    answer:
      'Yes. We're HIPAA-compliant with multi-tenant isolation, HIPAA audit logging, TLS 1.3 encryption, and role-based access control. Data never leaves your FHIR server - we query it directly.',
  },
  {
    question: 'Can we start small and scale later?',
    answer:
      'Absolutely. Start with the Pilot tier ($500/month) for a small proof of concept, then upgrade to Growth or Enterprise as you expand. Zero switching costs.',
  },
  {
    question: 'What kind of ROI can we expect?',
    answer:
      'ROI varies by organization, but typical customers see 50-500% Year 1 ROI from quality bonus impact and labor savings. Use our ROI calculator to estimate your specific impact.',
  },
]
