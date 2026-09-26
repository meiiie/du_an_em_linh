import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "#FFFFFF",
        paper: "#F3F6FB",
        ink: "#1A1D26",
        muted: "#5C6578",
        line: "#D5DCE8",
        board: "#0A2540",
        chalk: "#F7FAFC",
        primary: {
          DEFAULT: "#0B5CAB",
          hover: "#094A8C",
        },
        teal: "#0F766E",
        clay: "#0B5CAB",
        navy: "#0A2540",
        danger: "#B42318",
        warn: "#B45309",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      borderRadius: {
        button: "8px",
        card: "16px",
      },
      boxShadow: {
        lift: "0 1px 0 rgb(26 29 38 / 0.04)",
      },
    },
  },
  plugins: [],
};

export default config;
