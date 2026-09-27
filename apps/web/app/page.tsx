import Link from "next/link";
import { BrandMark } from "@/components/brand-mark";
import { buttonClasses } from "@/components/ui/button";
import { SITE_DESC, SITE_NAME, siteUrl } from "@/lib/site";

const BUOC = ["Tập xác định", "Đạo hàm", "Nghiệm y′", "Xét dấu", "Kết luận"];

const jsonLd = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "WebSite",
      name: SITE_NAME,
      url: siteUrl().origin + "/",
      inLanguage: "vi",
      description: SITE_DESC,
    },
    {
      "@type": "SoftwareApplication",
      name: SITE_NAME,
      applicationCategory: "EducationalApplication",
      operatingSystem: "Web",
      inLanguage: "vi",
      description: SITE_DESC,
      url: siteUrl().origin + "/",
      offers: { "@type": "Offer", price: "0", priceCurrency: "VND" },
    },
  ],
};

export default function TrangChu() {
  return (
    <main className="min-h-screen bg-canvas">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <header className="border-b border-line">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-5 py-4 sm:px-10">
          <div className="flex items-center gap-2">
            <BrandMark />
            <p className="text-sm text-muted">Nguyên mẫu NCKH</p>
          </div>
          <Link href="/dang-nhap" className={buttonClasses()} data-testid="vao-hoc">
            Vào học
          </Link>
        </div>
      </header>
      <div className="mx-auto max-w-5xl px-5 py-16 sm:px-10">
        <p className="text-sm text-muted">Toán 12 · ứng dụng đạo hàm</p>
        <h1 className="mt-2 text-pretty text-4xl font-semibold leading-tight sm:text-5xl">{SITE_NAME}</h1>
        <p className="mt-4 max-w-2xl text-base leading-relaxed text-muted">{SITE_DESC}</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link href="/dang-nhap" className={buttonClasses()}>
            Vào lớp thử
          </Link>
          <a
            href="https://github.com/meiiie/du_an_em_linh"
            className={buttonClasses({ variant: "secondary" })}
            rel="noreferrer"
          >
            Mã nguồn
          </a>
        </div>
        <ol className="mt-14 max-w-md">
          {BUOC.map((ten, i) => (
            <li
              key={ten}
              className="flex min-h-11 items-center gap-3 border-b border-line py-3 text-sm last:border-0"
            >
              <span className="tabular w-6 font-mono text-muted">{i + 1}</span>
              {ten}
            </li>
          ))}
        </ol>
        <p className="mt-10 max-w-2xl text-sm leading-relaxed text-muted">
          Cổng ba tầng (SymPy, tài liệu lớp, bảng công thức) trước khi phát hành bài. Dữ liệu thử tổng hợp — không có
          học sinh thật. Tài khoản: <span className="font-mono">gv@demo.local</span> /{" "}
          <span className="font-mono">hs.an@demo.local</span>.
        </p>
      </div>
    </main>
  );
}
