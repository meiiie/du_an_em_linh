import Link from "next/link";
import { buttonClasses } from "@/components/ui/button";
import { SITE_DESC, SITE_NAME, SITE_VERSION, siteUrl } from "@/lib/site";

const GOC = siteUrl().origin;

const jsonLd = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "Organization",
      "@id": `${GOC}/#to-chuc`,
      name: SITE_NAME,
      url: `${GOC}/`,
      logo: {
        "@type": "ImageObject",
        url: `${GOC}/icon-512.png`,
        width: 512,
        height: 512,
      },
      sameAs: ["https://github.com/meiiie/du_an_em_linh"],
    },
    {
      "@type": "WebSite",
      "@id": `${GOC}/#website`,
      name: SITE_NAME,
      url: `${GOC}/`,
      inLanguage: "vi",
      description: SITE_DESC,
      publisher: { "@id": `${GOC}/#to-chuc` },
    },
    {
      "@type": "WebApplication",
      "@id": `${GOC}/#app`,
      name: SITE_NAME,
      applicationCategory: "EducationalApplication",
      operatingSystem: "Web",
      inLanguage: "vi",
      description: SITE_DESC,
      url: `${GOC}/`,
      softwareVersion: SITE_VERSION,
      isAccessibleForFree: true,
      image: `${GOC}/icon-512.png`,
      publisher: { "@id": `${GOC}/#to-chuc` },
      offers: { "@type": "Offer", price: "0", priceCurrency: "VND" },
    },
    {
      "@type": "BreadcrumbList",
      itemListElement: [{ "@type": "ListItem", position: 1, name: "Trang chủ", item: `${GOC}/` }],
    },
  ],
};

export default function TrangChu() {
  return (
    <div className="min-h-screen bg-canvas">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <a href="#noi-dung" className="skip-link">
        Bỏ qua đến nội dung
      </a>
      <header className="border-b border-line">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-5 py-4 sm:px-10">
          <div className="flex items-center gap-2">
            {/* SVG tĩnh = logo tab; next/image không cần cho file public 32px */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/icon.svg" alt="" width={32} height={32} className="size-8" aria-hidden />
            <p className="text-sm text-muted">Toán 12</p>
          </div>
          <Link href="/dang-nhap" className={buttonClasses()} data-testid="vao-hoc">
            Vào học
          </Link>
        </div>
      </header>
      <main id="noi-dung" className="mx-auto max-w-5xl px-5 py-16 sm:px-10">
        <p className="text-sm text-muted">Đơn điệu và cực trị</p>
        <h1 className="mt-2 text-pretty text-4xl font-semibold leading-tight sm:text-5xl">{SITE_NAME}</h1>
        <p className="mt-4 max-w-2xl text-base leading-relaxed text-muted">{SITE_DESC}</p>
        <div className="mt-8">
          <Link href="/dang-nhap" className={buttonClasses()}>
            Vào lớp thử
          </Link>
        </div>
      </main>
      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-5 py-6 text-xs text-muted sm:px-10">
          <p>
            {SITE_NAME} · MIT · v{SITE_VERSION}
          </p>
          <nav className="flex gap-4" aria-label="Chân trang">
            <a href="https://github.com/meiiie/du_an_em_linh">GitHub</a>
            <Link href="/dang-nhap">Đăng nhập</Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}
