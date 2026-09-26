import type { Metadata } from "next";
import { Source_Sans_3 } from "next/font/google";
import "./globals.css";

const sourceSans = Source_Sans_3({
  subsets: ["latin", "latin-ext", "vietnamese"],
  variable: "--font-sans",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Học toán với AI",
  description: "Nguyên mẫu gia sư toán THPT — đơn điệu và cực trị",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" className={sourceSans.variable}>
      <body className={`${sourceSans.className} min-h-screen antialiased`}>{children}</body>
    </html>
  );
}
