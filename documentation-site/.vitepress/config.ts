import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'HDIM Clinical Portal',
  description: 'HealthData-in-Motion - Quality Measure & Care Gap Management Platform',

  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }]
  ],

  themeConfig: {
    logo: '/logo.svg',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'User Stories', link: '/user-stories/' },
      { text: 'Workflows', link: '/workflows/' },
      { text: 'Guides', link: '/guides/' },
      { text: 'API Reference', link: '/api/' },
      { text: 'Security', link: '/security/' },
      { text: 'About', link: '/about/' }
    ],

    sidebar: {
      '/user-stories/': [
        {
          text: 'User Stories',
          items: [
            { text: 'Overview', link: '/user-stories/' },
            { text: 'Patient Management', link: '/user-stories/patient-management' },
            { text: 'Quality Evaluations', link: '/user-stories/quality-evaluations' },
            { text: 'Care Gap Management', link: '/user-stories/care-gaps' },
            { text: 'Care Recommendations', link: '/user-stories/care-recommendations' },
            { text: 'Reports & Analytics', link: '/user-stories/reports' },
            { text: 'Dashboards', link: '/user-stories/dashboards' }
          ]
        }
      ],
      '/workflows/': [
        {
          text: 'Clinical Workflows',
          items: [
            { text: 'Overview', link: '/workflows/' },
            { text: 'Daily Provider Workflow', link: '/workflows/provider-daily' },
            { text: 'Care Gap Closure', link: '/workflows/care-gap-closure' },
            { text: 'Quality Evaluation', link: '/workflows/quality-evaluation' },
            { text: 'Patient Outreach', link: '/workflows/patient-outreach' },
            { text: 'Report Generation', link: '/workflows/report-generation' }
          ]
        }
      ],
      '/guides/': [
        {
          text: 'Role Guides',
          items: [
            { text: 'Overview', link: '/guides/' },
            { text: 'Provider Guide', link: '/guides/provider' },
            { text: 'RN Guide', link: '/guides/rn' },
            { text: 'Medical Assistant Guide', link: '/guides/ma' },
            { text: 'Quality Analyst Guide', link: '/guides/quality-analyst' },
            { text: 'Administrator Guide', link: '/guides/admin' }
          ]
        }
      ],
      '/api/': [
        {
          text: 'API Reference',
          items: [
            { text: 'Overview', link: '/api/' },
            { text: 'Patient Service', link: '/api/patient-service' },
            { text: 'Quality Measure Service', link: '/api/quality-measure-service' },
            { text: 'CQL Engine Service', link: '/api/cql-engine-service' },
            { text: 'Care Gap Service', link: '/api/care-gap-service' }
          ]
        }
      ],
      '/security/': [
        {
          text: 'Security',
          items: [
            { text: 'Security Architecture', link: '/security/' }
          ]
        }
      ],
      '/about/': [
        {
          text: 'About HDIM',
          items: [
            { text: 'Overview', link: '/about/' },
            { text: 'Development Case Study', link: '/about/development-case-study' }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/webemo-aaron/hdim' }
    ],

    search: {
      provider: 'local'
    },

    footer: {
      message: 'HDIM Clinical Portal Documentation',
      copyright: 'Copyright © 2024-2025 HealthData-in-Motion'
    },

    outline: {
      level: [2, 3]
    },

    editLink: {
      pattern: 'https://github.com/webemo-aaron/hdim/edit/main/documentation-site/:path',
      text: 'Edit this page on GitHub'
    },

    lastUpdated: {
      text: 'Last updated',
      formatOptions: {
        dateStyle: 'medium',
        timeStyle: 'short'
      }
    }
  },

  markdown: {
    lineNumbers: true
  }
})
