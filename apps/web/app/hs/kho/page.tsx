import type { Metadata } from "next";
import { khoLopCongKhai } from "@/lib/actions/hs";
import { KhoTheoBuoc } from "@/components/kho-theo-buoc";
import { PageHeader } from "@/components/ui/page-header";
import { Tex } from "@/components/tex";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Kho",
};

export default async function Page() {
  const kho = await khoLopCongKhai();
  return (
    <main className="max-w-2xl">
      <PageHeader title="Công thức và tài liệu" description="Gia sư chỉ đọc kho này, không đọc lời giải." />
      <section className="border-y border-line py-6" data-testid="kho-theo-buoc-hs">
        <h2 className="text-base font-semibold">Gia sư đọc theo bước</h2>
        <p className="mt-2 text-sm text-muted">Khi hỏi, lấy đúng đoạn khớp bước đang làm.</p>
        <div className="mt-3">
          <KhoTheoBuoc khung={kho.khung} />
        </div>
      </section>
      <section className="border-b border-line py-6" data-testid="kho-cong-thuc">
        <h2 className="text-base font-semibold">Công thức</h2>
        {kho.congThuc.length === 0 ? <p className="mt-3 text-sm text-muted">Chưa có công thức.</p> : null}
        <ul className="mt-3 divide-y divide-line">
          {kho.congThuc.map((c) => (
            <li key={c.id} className="py-3">
              <p className="font-medium">{c.title}</p>
              <Tex tex={c.latex} />
              <p className="mt-1 text-sm text-muted">{c.noiDung}</p>
            </li>
          ))}
        </ul>
      </section>
      <section className="border-b border-line py-6" data-testid="kho-tai-lieu">
        <h2 className="text-base font-semibold">Tài liệu lớp</h2>
        {kho.taiLieu.length === 0 ? <p className="mt-3 text-sm text-muted">Chưa có tài liệu.</p> : null}
        <ul className="mt-3 divide-y divide-line">
          {kho.taiLieu.map((d) => (
            <li key={d.id} className="py-3">
              <p className="font-medium">{d.title}</p>
              <p className="mt-1 text-sm text-muted">{d.trich}</p>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
