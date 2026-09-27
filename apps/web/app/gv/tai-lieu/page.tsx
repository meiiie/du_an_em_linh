import type { Metadata } from "next";
import { desc } from "drizzle-orm";
import { taiTaiLieu } from "@/lib/actions/gv";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Field, fieldControl } from "@/components/ui/field";
import { KhoTheoBuoc } from "@/components/kho-theo-buoc";
import { PageHeader } from "@/components/ui/page-header";
import { taiNguyenKhoLop } from "@/lib/kho-lop";
import { xemKhoTheoKhung } from "@/lib/kien-thuc";
import { db } from "@/lib/db";
import { documents } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Tài liệu",
};

export default async function Page() {
  const docs = await db.select().from(documents).orderBy(desc(documents.createdAt));
  const khung = xemKhoTheoKhung(await taiNguyenKhoLop());
  return (
    <main>
      <PageHeader title="Tài liệu" />
      {docs.length === 0 ? <p className="text-sm text-muted">Chưa có tài liệu.</p> : null}
      {docs.length ? (
        <ul className="divide-y divide-line border-y border-line">
          {docs.map((d) => (
            <li key={d.id} className="py-4">
              <p className="font-medium">{d.title}</p>
              <p className="mt-1">
                {d.licenseStatus === "chua_ro" ? (
                  <Badge tone="warn">Gia sư bỏ qua</Badge>
                ) : (
                  <Badge tone="ok">Gia sư được đọc</Badge>
                )}
              </p>
            </li>
          ))}
        </ul>
      ) : null}
      <details className="mt-8 border-t border-line pt-4">
        <summary className="min-h-11 cursor-pointer text-sm font-medium">Thêm tài liệu</summary>
        <form action={taiTaiLieu} className="mt-4 max-w-xl space-y-4">
          <Field label="Tên">
            <input name="title" required placeholder="Tóm tắt đơn điệu…" className={fieldControl} />
          </Field>
          <Field label="Loại">
            <select name="kind" className={fieldControl} autoComplete="off">
              <option value="tu_soan">Tự soạn</option>
              <option value="de_mau">Đề mẫu</option>
              <option value="tham_khao">Tham khảo</option>
            </select>
          </Field>
          <Field label="Quyền">
            <select name="license" className={fieldControl} autoComplete="off">
              <option value="tu_soan">Tự soạn</option>
              <option value="cong_khai">Công khai</option>
              <option value="chua_ro">Chưa rõ — gia sư bỏ qua</option>
            </select>
          </Field>
          <Field label="Văn bản">
            <textarea name="text" rows={6} placeholder="Dán văn bản…" className={fieldControl} />
          </Field>
          <Field label="Tệp (nếu có)">
            <input name="file" type="file" accept=".pdf,.txt,.md" className="min-h-10 text-sm [@media(pointer:coarse)]:min-h-11" />
          </Field>
          <Button type="submit">Lưu</Button>
        </form>
      </details>
      <section className="sr-only" data-testid="gia-su-doc-kho">
        <KhoTheoBuoc khung={khung} />
      </section>
    </main>
  );
}
