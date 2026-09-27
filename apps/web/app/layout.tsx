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
  category: "education",
  formatDetection: { telephone: false, email: false, address: false },
  alternates: { canonical: "/" },
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
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" className={`${plexSans.variable} ${plexMono.variable}`}>
      <body className={`${plexSans.className} min-h-screen antialiased`}>{children}</body>
    </html>
  );
}
