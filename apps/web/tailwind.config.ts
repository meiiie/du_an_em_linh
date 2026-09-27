import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "#FFFFFF",
        paper: "#FFFFFF",
        wash: "#F6F6F7",
        ink: "#17181C",
        muted: "#5C5F66",
        line: "#E2E3E6",
        board: "#17181C",
        chalk: "#F4F4F5",
        primary: {
          DEFAULT: "#17181C",
          hover: "#2A2C33",
        },
        teal: "#1B7A4B",
        clay: "#17181C",
        navy: "#17181C",
        danger: "#C81E1E",
        warn: "#9A6700",
        mark: "#C81E1E",
        pass: "#1B7A4B",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "ui-monospace", "SFMono-Regular", "monospace"],
        landing: ["var(--font-landing)", "Georgia", "serif"],
      },
      borderRadius: {
        button: "6px",
        card: "6px",
      },
      minHeight: {
        control: "var(--control-h)",
        target: "var(--target)",
      },
      height: {
        control: "var(--control-h)",
        target: "var(--target)",
      },
    },
  },
  plugins: [],
};

export default config;
