import { desc } from "drizzle-orm";
import { taiTaiLieu } from "@/lib/actions/gv";
import { Button } from "@/components/ui/button";
import { Field, fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { db } from "@/lib/db";
import { documents } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function Page() {
  const docs = await db.select().from(documents).orderBy(desc(documents.createdAt));
  return (
    <main>
      <PageHeader
        kicker="Nội dung"
        title="Tài liệu"
        description="Tầng 2 chỉ dùng văn bản đã nạp có quyền rõ. Tài liệu «chưa rõ quyền» bị bỏ qua."
      />
      <div className="grid gap-8 lg:grid-cols-2">
        <form action={taiTaiLieu} className="space-y-4 border-y border-line py-6">
          <h2 className="text-base font-semibold">Nạp tài liệu</h2>
          <Field label="Tên tài liệu">
            <input name="title" required placeholder="Ví dụ: Tóm tắt đơn điệu…" className={fieldControl} />
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
              <option value="chua_ro">Chưa rõ quyền — không dùng ở tầng 2</option>
            </select>
          </Field>
          <Field label="Văn bản">
            <textarea name="text" rows={6} placeholder="Dán văn bản…" className={fieldControl} />
          </Field>
          <Field label="Tệp đính kèm (tùy chọn)">
            <input name="file" type="file" accept=".pdf,.txt,.md" className="min-h-10 text-sm [@media(pointer:coarse)]:min-h-11" />
          </Field>
          <Button type="submit">Lưu</Button>
        </form>
        <ul className="divide-y divide-line border-y border-line">
          {docs.length === 0 ? <li className="py-3 text-sm text-muted">Chưa có tài liệu.</li> : null}
          {docs.map((d) => (
            <li key={d.id} className="py-3">
              <p className="font-medium">{d.title}</p>
              <p className="text-xs text-muted">
                {d.kind} · quyền {d.licenseStatus} · {d.textContent.length} ký tự
              </p>
            </li>
          ))}
        </ul>
      </div>
    </main>
  );
}
