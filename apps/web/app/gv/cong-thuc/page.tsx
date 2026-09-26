import { desc, eq } from "drizzle-orm";
import { themCongThuc } from "@/lib/actions/gv";
import { Tex } from "@/components/tex";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { fieldControl } from "@/components/ui/field";
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
        <form action={themCongThuc} className="space-y-2">
          <h2 className="font-display text-xl">Thêm công thức — tạo phiên bản mới</h2>
          <input name="title" required placeholder="Tên" className={fieldControl} />
          <input name="latex" placeholder="LaTeX" className={fieldControl} />
          <textarea name="noi_dung" required placeholder="Nội dung tiếng Việt" className={fieldControl} />
          <button className="rounded-lg bg-clay px-4 py-2 font-semibold text-white" type="submit">
            Khóa phiên bản mới
          </button>
        </form>
      </Card>
    </main>
  );
}
