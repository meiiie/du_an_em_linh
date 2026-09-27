import type { Metadata } from "next";
import { HangCongThuc } from "@/components/hang-cong-thuc";
import { KhoTheoBuoc } from "@/components/kho-theo-buoc";
import { khoLopCongKhai } from "@/lib/actions/hs";
import { thanTrich } from "@/lib/de-hoc-sinh";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Công thức",
};

export default async function Page() {
  const kho = await khoLopCongKhai();
  return (
    <main>
      <header className="mb-6 border-b border-line pb-4">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Công thức và tài liệu</h1>
        <p className="mt-1 text-sm text-muted">Gia sư chỉ đọc phần này, không đọc lời giải.</p>
      </header>

      <div className="lg:grid lg:grid-cols-[minmax(0,1fr)_minmax(18rem,24rem)] lg:items-start">
        <section className="min-w-0 lg:border-r lg:border-line lg:pr-8" data-testid="kho-cong-thuc" aria-labelledby="kho-cong-thuc-tieu">
          <h2 id="kho-cong-thuc-tieu" className="sr-only">
            Công thức
          </h2>
          {kho.congThuc.length === 0 ? <p className="text-sm text-muted">Chưa có công thức.</p> : null}
          <ul className="divide-y divide-line border-y border-line">
            {kho.congThuc.map((c) => (
              <HangCongThuc key={c.id} title={c.title} latex={c.latex} noiDung={c.noiDung} />
            ))}
          </ul>
        </section>

        <div className="mt-8 min-w-0 space-y-8 lg:mt-0 lg:pl-8">
          <section data-testid="kho-theo-buoc-hs">
            <h2 className="text-base font-semibold">Theo năm bước</h2>
            <div className="mt-1">
              <KhoTheoBuoc khung={kho.khung} />
            </div>
          </section>

          <section data-testid="kho-tai-lieu">
            <h2 className="text-base font-semibold">Tài liệu lớp</h2>
            {kho.taiLieu.length === 0 ? <p className="mt-3 text-sm text-muted">Chưa có tài liệu.</p> : null}
            <ul className="mt-1 divide-y divide-line border-y border-line">
              {kho.taiLieu.map((d) => (
                <li key={d.id} className="py-3">
                  <p className="text-sm font-medium">{d.title}</p>
                  <p className="mt-1 text-sm leading-relaxed text-muted">{thanTrich(d.trich)}</p>
                </li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </main>
  );
}
