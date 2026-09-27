import type { Metadata } from "next";
import { desc, eq } from "drizzle-orm";
import { themCongThuc } from "@/lib/actions/gv";
import { Tex } from "@/components/tex";
import { Button } from "@/components/ui/button";
import { Field, fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { db } from "@/lib/db";
import { formulaSheets, formulas, verificationRuns } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Công thức",
};

export default async function Page() {
  const sheets = await db.select().from(formulaSheets).orderBy(desc(formulaSheets.version));
  const latest = sheets[0];
  const rows = latest ? await db.select().from(formulas).where(eq(formulas.formulaSheetId, latest.id)) : [];
  const stale = await db.select().from(verificationRuns).where(eq(verificationRuns.stale, true));
  return (
    <main className="space-y-8">
      <PageHeader title="Công thức" />
      <section className="border-y border-line py-4">
        {stale.length ? (
          <p className="mb-4 bg-amber-50 px-4 py-3 text-sm">
            {stale.length} lần kiểm cũ — cần kiểm lại khi sửa đề.
          </p>
        ) : null}
        {rows.length === 0 ? <p className="text-sm text-muted">Chưa có công thức.</p> : null}
        <ul className="divide-y divide-line">
          {rows.map((f) => (
            <li key={f.id} className="py-3">
              <p className="font-medium">{f.title}</p>
              <Tex tex={f.latex} />
              <p className="text-sm text-muted">{f.noiDung}</p>
            </li>
          ))}
        </ul>
      </section>
      <form action={themCongThuc} className="space-y-4 border-y border-line py-6">
        <h2 className="text-base font-semibold">Thêm công thức</h2>
        <Field label="Tên">
          <input name="title" required placeholder="Tên công thức…" className={fieldControl} />
        </Field>
        <Field label="LaTeX">
          <input name="latex" placeholder="y' = …" className={fieldControl} autoComplete="off" />
        </Field>
        <Field label="Tiếng Việt">
          <textarea name="noi_dung" required placeholder="Nội dung ngắn…" className={fieldControl} />
        </Field>
        <Button type="submit">Thêm</Button>
      </form>
    </main>
  );
}
