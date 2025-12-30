import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#0D4F8B',
          50: '#E8F1F8',
          100: '#C5DCF0',
          200: '#8BB9E0',
          300: '#5196D1',
          400: '#2573B1',
          500: '#0D4F8B',
          600: '#0A3F6F',
          700: '#082F53',
          800: '#052037',
          900: '#03101C',
        },
        accent: {
          DEFAULT: '#4B9CD3',
          50: '#EDF5FB',
          100: '#D4E9F5',
          200: '#A9D3EB',
          300: '#7EBDE1',
          400: '#4B9CD3',
          500: '#2B7DB8',
          600: '#226292',
          700: '#19476B',
          800: '#102D45',
          900: '#08161E',
        },
        success: '#10B981',
        warning: '#F59E0B',
        error: '#EF4444',
      },
      fontFamily: {
        sans: ['var(--font-inter)', 'system-ui', 'sans-serif'],
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-in-out',
        'slide-up': 'slideUp 0.5s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}

export default config
