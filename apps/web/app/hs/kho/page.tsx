import type { Metadata } from "next";
import Link from "next/link";
import { HangCongThuc } from "@/components/hang-cong-thuc";
import { KhoTheoBuoc } from "@/components/kho-theo-buoc";
import { soTabClass } from "@/components/so-nav";
import { khoLopCongKhai } from "@/lib/actions/hs";
import { tenTaiLieuNgan, thanTrich } from "@/lib/de-hoc-sinh";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Công thức",
};

export default async function Page({ searchParams }: { searchParams: Promise<{ muc?: string }> }) {
  const lieu = (await searchParams).muc === "lieu";
  const kho = await khoLopCongKhai();
  return (
    <main>
      <header className="mb-6 border-b border-line pb-4">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Công thức và tài liệu</h1>
      </header>

      <nav aria-label="Công thức và tài liệu" className="mb-6 flex items-end border-b border-line">
        <Link
          href="/hs/kho"
          scroll={false}
          data-testid="tab-cong-thuc"
          aria-current={!lieu ? "page" : undefined}
          className={soTabClass(!lieu)}
        >
          Công thức
        </Link>
        <Link
          href="/hs/kho?muc=lieu"
          scroll={false}
          data-testid="tab-tai-lieu"
          aria-current={lieu ? "page" : undefined}
          className={soTabClass(lieu)}
        >
          Tài liệu
        </Link>
      </nav>

      <section className={lieu ? "hidden" : undefined} data-testid="kho-cong-thuc" aria-labelledby="kho-cong-thuc-tieu">
        <h2 id="kho-cong-thuc-tieu" className="sr-only">
          Công thức
        </h2>
        {kho.congThuc.length === 0 ? <p className="text-sm text-muted">Chưa có công thức.</p> : null}
        <ul className="divide-y divide-line border-y border-line">
          {kho.congThuc.map((c) => (
            <HangCongThuc key={c.id} title={c.title} latex={c.latex} noiDung={c.noiDung} gon />
          ))}
        </ul>
      </section>

      <section className={lieu ? undefined : "hidden"} data-testid="kho-tai-lieu" aria-labelledby="kho-tai-lieu-tieu">
        <h2 id="kho-tai-lieu-tieu" className="sr-only">
          Tài liệu lớp
        </h2>
        {kho.taiLieu.length === 0 ? <p className="text-sm text-muted">Chưa có tài liệu.</p> : null}
        <ul className="divide-y divide-line border-y border-line">
          {kho.taiLieu.map((d) => (
            <li key={d.id} className="py-4">
              <p className="text-sm font-medium">{tenTaiLieuNgan(d.title)}</p>
              <p className="mt-1 text-sm leading-relaxed text-muted">{thanTrich(d.trich)}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="sr-only" data-testid="kho-theo-buoc-hs">
        <h2>Theo năm bước</h2>
        <KhoTheoBuoc khung={kho.khung} />
      </section>
    </main>
  );
}
