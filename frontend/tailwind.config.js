/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Design system colors from DESIGN.md
        aloe: {
          10: '#c1fbd4',
        },
        pistachio: {
          10: '#d4f9e0',
        },
        canvas: {
          night: '#000000',
          nightElevated: '#0a0a0a',
          light: '#ffffff',
          cream: '#fbfbf5',
        },
        hairline: {
          light: '#e4e4e7',
          dark: '#1e2c31',
        },
        shade: {
          30: '#d4d4d8',
          40: '#a1a1aa',
          50: '#71717a',
          60: '#52525b',
          70: '#3f3f46',
        },
        link: {
          cool1: '#9dabad',
          cool2: '#9797a2',
          cool3: '#bdbdca',
          mint: '#99b3ad',
        },
        ink: '#000000',
        onPrimary: '#ffffff',
        // Semantic status colors — centralized so Badge/Toast/Button/status maps
        // stay consistent instead of every page picking its own red/amber/blue shade.
        danger: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
          800: '#991b1b',
          900: '#7f1d1d',
          950: '#450a0a',
        },
        warning: {
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
        },
        info: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
        success: {
          50: '#e2fbeb',
          100: '#d1fae5',
          200: '#a2f7c0',
          300: '#6ee7b7',
          400: '#34d399',
          500: '#10b981',
          600: '#059669',
          700: '#127a3c',
          800: '#065f46',
          900: '#064e3b',
          950: '#022c22',
        },
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        display: ['Neue Haas Grotesk Display', 'Helvetica Neue', 'Arial', 'sans-serif'],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      },
      borderRadius: {
        xs: '4px',
        sm: '5px',
        md: '8px',
        lg: '12px',
        xl: '20px',
        pill: '9999px',
      },
      spacing: {
        xxs: '2px',
        xs: '4px',
        sm: '8px',
        md: '12px',
        lg: '16px',
        xl: '24px',
        xxl: '32px',
        huge: '64px',
      },
      boxShadow: {
        // Stacked tiny shadows (Level 3 Elevation from DESIGN.md)
        'level-3': '0 8px 8px rgba(0,0,0,0.02), 0 4px 4px rgba(0,0,0,0.02), 0 2px 2px rgba(0,0,0,0.02), 0 0 0 1px rgba(0,0,0,0.05)',
        'level-1-inset': 'inset 0 1px 0 rgba(255,255,255,0.04)',
        // Level 4 elevation from DESIGN.md — modal / floating panel on light
        'level-4': '0 25px 50px -12px rgba(0,0,0,0.25)',
      }
    },
  },
  plugins: [],
}
