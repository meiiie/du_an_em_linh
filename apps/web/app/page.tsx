import Link from "next/link";
import { MinhHoaDaoHam } from "@/components/minh-hoa-dao-ham";
import { buttonClasses } from "@/components/ui/button";
import { cn } from "@/lib/cn";
import { soBuoc, tenBuocTrang } from "@/lib/de-hoc-sinh";
import { SITE_DESC, SITE_NAME, SITE_VERSION, siteUrl } from "@/lib/site";

const BUOC_TRANG = ["B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN"] as const;

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
      <header className="sticky top-0 z-30 border-b border-line bg-canvas">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-5 sm:px-10">
          <h1 className="min-w-0 truncate text-sm font-semibold tracking-tight">
            <Link href="/">{SITE_NAME}</Link>
          </h1>
          <nav className="ml-auto hidden items-center gap-6 text-sm text-muted sm:flex" aria-label="Trang chủ">
            <a href="#hinh" className="hover:text-ink">
              Hình
            </a>
            <a href="#nam-buoc" className="hover:text-ink">
              Năm bước
            </a>
          </nav>
          <Link href="/dang-nhap" className={cn(buttonClasses(), "ml-auto sm:ml-0")} data-testid="vao-hoc">
            Vào học
          </Link>
        </div>
      </header>
      <main id="noi-dung">
        <section className="mx-auto grid max-w-6xl items-center gap-12 px-5 py-16 sm:px-10 lg:grid-cols-[minmax(0,0.86fr)_minmax(0,1.14fr)] lg:gap-16 lg:py-24">
          <div>
            <p className="text-sm text-muted">Toán 12 · Đơn điệu và cực trị</p>
            <p className="mt-6 text-pretty text-4xl font-semibold leading-[1.12] tracking-tight sm:text-5xl">
              Nhìn tiếp tuyến,
              <br />
              đọc được đạo hàm.
            </p>
            <p className="mt-6 max-w-[36ch] text-base leading-relaxed text-muted">
              Phiếu năm bước. Gia sư sửa bài, không đưa đáp án.
            </p>
            <Link href="/dang-nhap" className={cn(buttonClasses(), "mt-8")}>
              Vào học
            </Link>
          </div>
          <MinhHoaDaoHam />
        </section>

        <section className="border-y border-line" aria-label="Việc trang này làm">
          <dl className="mx-auto grid max-w-6xl gap-8 px-5 py-12 sm:grid-cols-3 sm:px-10 sm:py-14">
            <div>
              <dt className="text-2xl font-semibold tracking-tight">Năm bước</dt>
              <dd className="mt-2 text-sm leading-relaxed text-muted">Từ tập xác định tới kết luận.</dd>
            </div>
            <div>
              <dt className="text-2xl font-semibold tracking-tight">Một chủ đề</dt>
              <dd className="mt-2 text-sm leading-relaxed text-muted">Đơn điệu và cực trị, Toán 12.</dd>
            </div>
            <div>
              <dt className="text-2xl font-semibold tracking-tight">Không đáp án</dt>
              <dd className="mt-2 text-sm leading-relaxed text-muted">Gia sư chỉ sửa bài.</dd>
            </div>
          </dl>
        </section>

        <section id="nam-buoc" className="mx-auto grid max-w-6xl items-start gap-10 px-5 py-16 sm:px-10 lg:grid-cols-2 lg:gap-16 lg:py-24">
          <div>
            <h2 className="text-pretty text-3xl font-semibold tracking-tight">Làm trên phiếu</h2>
            <p className="mt-4 max-w-[36ch] text-base leading-relaxed text-muted">
              Học sinh viết từng bước. Đồ thị của bài không mở trước kết luận.
            </p>
          </div>
          <ol>
            {BUOC_TRANG.map((ma) => (
              <li key={ma} className="flex min-h-12 items-baseline gap-4 border-b border-line">
                <span className="w-8 font-mono text-xs tabular text-muted">{soBuoc(ma)}</span>
                <span className="text-base">{tenBuocTrang(ma)}</span>
              </li>
            ))}
          </ol>
        </section>
      </main>
      <section className="bg-ink text-chalk">
        <div className="mx-auto flex max-w-6xl flex-col items-start gap-6 px-5 py-16 sm:flex-row sm:items-center sm:justify-between sm:px-10 sm:py-20">
          <p className="text-pretty text-3xl font-semibold tracking-tight">Vào làm một bài.</p>
          <Link href="/dang-nhap" className={cn(buttonClasses(), "bg-chalk text-ink hover:bg-white")}>
            Vào học
          </Link>
        </div>
      </section>
      <footer>
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-5 py-6 text-xs text-muted sm:px-10">
          <p>
            {SITE_NAME} · MIT · v{SITE_VERSION}
          </p>
          <a href="https://github.com/meiiie/du_an_em_linh">GitHub</a>
        </div>
      </footer>
    </div>
  );
}
