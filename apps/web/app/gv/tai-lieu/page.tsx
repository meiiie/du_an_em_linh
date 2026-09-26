import { desc } from "drizzle-orm";
import { taiTaiLieu } from "@/lib/actions/gv";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { fieldControl } from "@/components/ui/field";
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
          <form action={taiTaiLieu} className="space-y-2">
            <h2 className="font-display text-xl">Nạp tài liệu</h2>
            <input name="title" required placeholder="Tên tài liệu" className={fieldControl} />
            <select name="kind" className={fieldControl}>
              <option value="tu_soan">Tự soạn</option>
              <option value="de_mau">Đề mẫu</option>
              <option value="tham_khao">Tham khảo</option>
            </select>
            <select name="license" className={fieldControl}>
              <option value="tu_soan">Tự soạn</option>
              <option value="cong_khai">Công khai</option>
              <option value="chua_ro">Chưa rõ quyền — không dùng ở tầng 2</option>
            </select>
            <textarea name="text" rows={6} placeholder="Dán văn bản" className={fieldControl} />
            <input name="file" type="file" accept=".pdf,.txt,.md" className="text-sm" />
            <button className="rounded-lg bg-clay px-4 py-2 font-semibold text-white" type="submit">
              Lưu
            </button>
          </form>
        </Card>
        <ul className="space-y-2">
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
