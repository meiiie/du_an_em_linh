import Link from "next/link";
import { Newsreader } from "next/font/google";
import { Search } from "lucide-react";
import { HeroCanh } from "@/components/landing/hero-canh";
import { TheTinhNang } from "@/components/landing/the-tinh-nang";
import { SITE_DESC, SITE_NAME, SITE_VERSION, siteUrl } from "@/lib/site";

const display = Newsreader({
  subsets: ["latin", "latin-ext", "vietnamese"],
  weight: ["400", "500", "600"],
  style: ["normal", "italic"],
  variable: "--font-landing",
  display: "swap",
});

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

const NAV = [
  { href: "#kham-pha", label: "Khám phá" },
  { href: "#lo-trinh", label: "Lộ trình" },
  { href: "#bai-tap", label: "Bài tập" },
  { href: "#tai-nguyen", label: "Tài nguyên" },
  { href: "#ve-chung-toi", label: "Về chúng tôi" },
];

function NutDen({ href, children, testId }: { href: string; children: string; testId?: string }) {
  return (
    <Link
      href={href}
      data-testid={testId}
      className="inline-flex min-h-11 items-center justify-center rounded-full bg-[#1c1c1c] px-5 text-sm font-medium text-white hover:bg-[#2a2a2a]"
    >
      {children}
    </Link>
  );
}

export default function TrangChu() {
  return (
    <div className={`${display.variable} min-h-screen bg-[#f6f4f0] text-[#1c1c1c]`}>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <a href="#noi-dung" className="skip-link">
        Bỏ qua đến nội dung
      </a>

      <header className="sticky top-0 z-30 border-b border-[#e6e1d8] bg-[#f6f4f0]/95 backdrop-blur-sm">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-5 py-3 sm:px-8">
          <Link href="/" className="shrink-0 text-lg font-semibold tracking-tight">
            Học Toán
          </Link>
          <nav className="hidden items-center gap-6 text-sm text-[#5c5954] lg:flex" aria-label="Trang chủ">
            {NAV.map((item) => (
              <a key={item.href} href={item.href} className="hover:text-[#1c1c1c]">
                {item.label}
              </a>
            ))}
          </nav>
          <form action="/dang-nhap" role="search" className="ml-auto hidden items-center gap-2 rounded-full border border-[#e0dbd2] bg-white px-3 md:flex">
            <Search className="size-4 text-[#8d877e]" aria-hidden />
            <label className="sr-only" htmlFor="tim-landing">
              Tìm kiếm
            </label>
            <input
              id="tim-landing"
              name="q"
              placeholder="Tìm kiếm..."
              className="h-10 w-36 bg-transparent text-sm outline-none placeholder:text-[#a39e94] lg:w-44"
            />
          </form>
          <Link href="/dang-nhap" className="hidden text-sm text-[#3c3a36] hover:text-[#1c1c1c] sm:inline">
            Đăng nhập
          </Link>
          <NutDen href="/dang-nhap" testId="vao-hoc">
            Bắt đầu học
          </NutDen>
        </div>
        <nav className="flex gap-4 overflow-x-auto px-5 pb-3 text-sm text-[#5c5954] lg:hidden" aria-label="Mục trang">
          {NAV.map((item) => (
            <a key={item.href} href={item.href} className="shrink-0">
              {item.label}
            </a>
          ))}
        </nav>
      </header>

      <main id="noi-dung">
        <section id="kham-pha" className="lg:grid lg:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)] lg:items-center">
          <div className="px-5 py-14 sm:px-8 lg:py-20 lg:pl-10 xl:pl-16">
            <p className="text-xs font-medium tracking-[0.22em] text-[#8d877e]">TOÁN HỌC · TƯ DUY · TƯƠNG LAI</p>
            <h1 className="font-landing mt-6 max-w-[12ch] text-[2.75rem] font-medium leading-[1.08] tracking-tight sm:text-6xl lg:text-[4.25rem]">
              Toán học mở ra những cách nhìn mới.
            </h1>
            <p className="mt-6 max-w-[38ch] text-base leading-relaxed text-[#5c5954] sm:text-lg">
              Học toán với AI – lộ trình cá nhân hóa, bài tập phong phú và giải thích sâu sắc, giúp bạn hiểu bản chất, không chỉ làm được.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-6">
              <NutDen href="/dang-nhap">Bắt đầu miễn phí →</NutDen>
              <a href="#lo-trinh" className="text-sm font-medium underline decoration-[#1c1c1c] underline-offset-4">
                Khám phá lộ trình
              </a>
            </div>
          </div>
          <HeroCanh />
        </section>

        <section className="mx-auto grid max-w-6xl gap-10 px-5 py-14 sm:grid-cols-3 sm:px-8 sm:py-16" aria-label="Con số minh họa">
          <div>
            <p className="font-landing text-4xl font-medium tracking-tight sm:text-5xl">50.000+</p>
            <p className="mt-2 max-w-[18ch] text-sm leading-relaxed text-[#6d6962]">học sinh đang học cùng chúng tôi (dữ liệu mẫu)</p>
          </div>
          <div>
            <p className="font-landing text-4xl font-medium tracking-tight sm:text-5xl">95%</p>
            <p className="mt-2 max-w-[22ch] text-sm leading-relaxed text-[#6d6962]">học sinh cảm thấy hiểu bài hơn sau 4 tuần (khảo sát nội bộ)</p>
          </div>
          <div>
            <p className="font-landing text-4xl font-medium tracking-tight sm:text-5xl">∞</p>
            <p className="mt-2 max-w-[18ch] text-sm leading-relaxed text-[#6d6962]">Toán học vẫn luôn đầy điều thú vị</p>
          </div>
        </section>

        <section id="trai-nghiem" className="mx-auto max-w-7xl px-5 pb-20 sm:px-8">
          <div className="grid items-end gap-10 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.3fr)] lg:gap-12">
            <div>
              <p className="text-xs font-medium tracking-[0.2em] text-[#8d877e]">TRẢI NGHIỆM HỌC TOÁN KHÁC BIỆT</p>
              <h2 className="font-landing mt-4 text-4xl font-medium leading-[1.12] tracking-tight sm:text-5xl">
                Không chỉ là bài tập, mà là hành trình tư duy.
              </h2>
              <p className="mt-5 max-w-[40ch] text-base leading-relaxed text-[#5c5954]">
                Chúng tôi kết hợp sức mạnh của AI, phương pháp sư phạm và thiết kế tối giản để giúp bạn học toán hiệu quả, chủ động và truyền cảm hứng.
              </p>
              <a href="#ve-chung-toi" className="mt-6 inline-block text-sm font-medium underline underline-offset-4">
                Tìm hiểu thêm về chúng tôi →
              </a>
            </div>
            <div id="lo-trinh">
              <div id="bai-tap" />
              <div id="tai-nguyen" />
              <TheTinhNang />
            </div>
          </div>
        </section>
      </main>

      <section id="ve-chung-toi" className="relative overflow-hidden bg-[#070b14] text-[#f4f1eb]">
        <div
          className="pointer-events-none absolute inset-0 opacity-80"
          aria-hidden
          style={{
            backgroundImage:
              "radial-gradient(1px 1px at 12% 30%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 28% 70%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 64% 24%, #fff 50%, transparent 51%), radial-gradient(1.5px 1.5px at 80% 60%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 46% 48%, #fff 50%, transparent 51%), radial-gradient(circle at 8% 120%, #1d3358 0, #0c1424 28%, transparent 46%)",
          }}
        />
        <div className="relative mx-auto grid max-w-7xl items-center gap-10 px-5 py-16 sm:px-8 lg:grid-cols-2 lg:py-20">
          <blockquote>
            <p className="font-landing text-2xl italic leading-snug sm:text-3xl">
              “Những ý tưởng lớn thường bắt đầu từ những câu hỏi đơn giản.”
            </p>
            <footer className="mt-4 text-sm text-[#b7b2a8]">— Richard Feynman</footer>
          </blockquote>
          <div className="lg:text-right">
            <p className="font-landing text-3xl font-medium sm:text-4xl">Sẵn sàng khám phá?</p>
            <p className="mt-3 text-sm text-[#c8c2b6]">Hãy bắt đầu hành trình học toán của riêng bạn ngay hôm nay.</p>
            <Link
              href="/dang-nhap"
              className="mt-6 inline-flex min-h-11 items-center rounded-full bg-white px-5 text-sm font-medium text-[#1c1c1c] hover:bg-[#f6f4f0]"
            >
              Bắt đầu học →
            </Link>
          </div>
        </div>
      </section>

      <footer className="bg-[#f6f4f0]">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-5 py-6 text-xs text-[#8d877e] sm:px-8">
          <p>
            {SITE_NAME} · MIT · v{SITE_VERSION}
          </p>
          <a href="https://github.com/meiiie/du_an_em_linh">GitHub</a>
        </div>
      </footer>
    </div>
  );
}
