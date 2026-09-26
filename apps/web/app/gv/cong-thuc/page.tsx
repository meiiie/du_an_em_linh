import { desc, eq } from "drizzle-orm";
import { themCongThuc } from "@/lib/actions/gv";
import { Tex } from "@/components/tex";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Field, fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { db } from "@/lib/db";
import { formulaSheets, formulas, verificationRuns } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function Page() {
  const sheets = await db.select().from(formulaSheets).orderBy(desc(formulaSheets.version));
  const latest = sheets[0];
  const rows = latest ? await db.select().from(formulas).where(eq(formulas.formulaSheetId, latest.id)) : [];
  const stale = await db.select().from(verificationRuns).where(eq(verificationRuns.stale, true));
  return (
    <main className="space-y-4">
      <PageHeader
        kicker="Nội dung"
        title={`Bảng công thức phiên bản ${latest?.version ?? 0}`}
        description="Thêm công thức sẽ khóa phiên bản mới. Bài đã phát hành không bị gỡ; lần kiểm cũ được đánh dấu cũ."
      />
      <Card>
        {stale.length ? (
          <p className="mb-3 rounded-xl bg-amber-50 px-3 py-2 text-sm">
            Đổi phiên bản làm {stale.length} lần kiểm định cũ. Cần kiểm lại khi sửa nội dung bài.
          </p>
        ) : null}
        {rows.length === 0 ? <p className="text-sm text-muted">Chưa có công thức.</p> : null}
        <ul className="space-y-4">
          {rows.map((f) => (
            <li key={f.id}>
              <p className="font-medium">{f.title}</p>
              <Tex tex={f.latex} />
              <p className="text-sm text-muted">{f.noiDung}</p>
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <form action={themCongThuc} className="space-y-3">
          <h2 className="text-xl font-semibold">Thêm công thức — tạo phiên bản mới</h2>
          <Field label="Tên">
            <input name="title" required placeholder="Tên công thức…" className={fieldControl} />
          </Field>
          <Field label="LaTeX">
            <input name="latex" placeholder="y' = …" className={fieldControl} autoComplete="off" />
          </Field>
          <Field label="Nội dung tiếng Việt">
            <textarea name="noi_dung" required placeholder="Giải thích ngắn…" className={fieldControl} />
          </Field>
          <Button type="submit">Khóa phiên bản mới</Button>
        </form>
      </Card>
    </main>
  );
}
