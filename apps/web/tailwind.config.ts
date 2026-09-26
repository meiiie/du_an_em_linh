import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        paper: "#f3eee4",
        ink: "#1c1712",
        muted: "#6b6258",
        line: "#d9d0c1",
        board: "#1c1712",
        chalk: "#f3ead8",
        teal: "#0f5c56",
        clay: "#b45309",
        navy: "#1a365d",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "var(--font-sans)", "ui-serif", "Georgia", "serif"],
      },
    },
  },
  plugins: [],
};

export default config;
