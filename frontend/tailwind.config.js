export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: { navy: { DEFAULT: '#0b1f3a', 800: '#12305a' } },
      fontFamily: { sans: ['Inter', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'] },
      keyframes: { fade: { '0%': { opacity: 0, transform: 'translateY(6px)' }, '100%': { opacity: 1, transform: 'none' } } },
      animation: { fade: 'fade .35s ease-out' },
    },
  },
  plugins: [],
};
