import type { Metadata, Viewport } from "next";
import { IBM_Plex_Mono, IBM_Plex_Sans } from "next/font/google";
import { SITE_DESC, SITE_LOCALE, SITE_NAME, siteUrl } from "@/lib/site";
import "./globals.css";

const plexSans = IBM_Plex_Sans({
  subsets: ["latin", "latin-ext", "vietnamese"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-sans",
  display: "swap",
});

const plexMono = IBM_Plex_Mono({
  subsets: ["latin", "latin-ext", "vietnamese"],
  weight: ["400", "500"],
  variable: "--font-mono",
  display: "swap",
});

const goc = siteUrl();

export const viewport: Viewport = {
  themeColor: "#17181C",
  width: "device-width",
  initialScale: 1,
};

export const metadata: Metadata = {
  metadataBase: goc,
  title: {
    default: SITE_NAME,
    template: `%s · ${SITE_NAME}`,
  },
  description: SITE_DESC,
  applicationName: SITE_NAME,
  keywords: ["học toán", "gia sư AI", "đạo hàm", "đơn điệu", "cực trị", "Toán 12", "NCKH", "THPT"],
  authors: [{ name: "du_an_em_linh", url: "https://github.com/meiiie/du_an_em_linh" }],
  creator: "du_an_em_linh",
  publisher: "du_an_em_linh",
  category: "education",
  referrer: "strict-origin-when-cross-origin",
  formatDetection: { telephone: false, email: false, address: false },
  alternates: {
    canonical: "/",
    languages: { "vi-VN": "/", "x-default": "/" },
  },
  icons: {
    icon: [
      { url: "/favicon.ico", sizes: "48x48", type: "image/x-icon" },
      { url: "/icon.svg", type: "image/svg+xml" },
      { url: "/icon-48.png", sizes: "48x48", type: "image/png" },
      { url: "/icon-96.png", sizes: "96x96", type: "image/png" },
      { url: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { url: "/icon-512.png", sizes: "512x512", type: "image/png" },
    ],
    apple: [{ url: "/apple-touch-icon.png", sizes: "180x180", type: "image/png" }],
  },
  appleWebApp: {
    capable: true,
    title: "Học toán AI",
    statusBarStyle: "default",
  },
  openGraph: {
    type: "website",
    locale: SITE_LOCALE,
    url: goc.origin + "/",
    siteName: SITE_NAME,
    title: SITE_NAME,
    description: SITE_DESC,
  },
  twitter: {
    card: "summary_large_image",
    title: SITE_NAME,
    description: SITE_DESC,
  },
  robots: {
    index: true,
    follow: true,
    googleBot: { index: true, follow: true },
  },
  other: {
    "msapplication-TileColor": "#17181C",
    "msapplication-TileImage": "/icon-192.png",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" className={`${plexSans.variable} ${plexMono.variable}`}>
      <body className={`${plexSans.className} min-h-screen antialiased`}>{children}</body>
    </html>
  );
}
