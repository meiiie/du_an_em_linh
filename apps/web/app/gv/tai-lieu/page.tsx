import { desc } from "drizzle-orm";
import { taiTaiLieu } from "@/lib/actions/gv";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
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
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <form action={taiTaiLieu} className="space-y-3">
            <h2 className="text-xl font-semibold">Nạp tài liệu</h2>
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
              <input name="file" type="file" accept=".pdf,.txt,.md" className="text-sm" />
            </Field>
            <Button type="submit">Lưu</Button>
          </form>
        </Card>
        <ul className="space-y-2">
          {docs.length === 0 ? <li className="text-sm text-muted">Chưa có tài liệu.</li> : null}
          {docs.map((d) => (
            <li key={d.id}>
              <Card>
                <p className="font-semibold">{d.title}</p>
                <p className="text-xs text-muted">
                  {d.kind} · quyền {d.licenseStatus} · {d.textContent.length} ký tự
                </p>
              </Card>
            </li>
          ))}
        </ul>
      </div>
    </main>
  );
}
