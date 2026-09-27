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
      <PageHeader
        kicker="Kho lớp"
        title="Kiến thức gia sư được đọc"
        description="Cùng kho tầng 2/3: tài liệu đã nạp (quyền rõ) và bảng công thức khóa. Gia sư không đọc lời giải chuẩn."
      />
      <section className="border-y border-line py-6" data-testid="kho-theo-buoc-hs">
        <h2 className="text-base font-semibold">Gia sư đọc theo bước</h2>
        <p className="mt-2 text-sm text-muted">Khi em hỏi, hệ thống lấy đoạn khớp bước đang làm — không bịa công thức ngoài kho.</p>
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
        {kho.taiLieu.length === 0 ? <p className="mt-3 text-sm text-muted">Chưa có tài liệu dùng được.</p> : null}
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
