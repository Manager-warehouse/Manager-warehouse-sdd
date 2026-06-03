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
      }
    },
  },
  plugins: [],
}
