import type { Metadata } from "next";
import { desc, eq } from "drizzle-orm";
import { FormThemCongThuc } from "@/components/form-them-cong-thuc";
import { HangCongThuc } from "@/components/hang-cong-thuc";
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
    <main>
      <PageHeader title="Công thức" />
      {stale.length ? (
        <p className="mb-4 bg-amber-50 px-4 py-3 text-sm">{stale.length} lần kiểm cũ — sửa đề thì kiểm lại.</p>
      ) : null}
      {rows.length === 0 ? <p className="text-sm text-muted">Chưa có công thức.</p> : null}
      <ul className="divide-y divide-line border-y border-line">
        {rows.map((f) => (
          <HangCongThuc key={f.id} title={f.title} latex={f.latex} noiDung={f.noiDung} gon />
        ))}
      </ul>
      <details className="mt-8 border-t border-line pt-4">
        <summary className="min-h-11 cursor-pointer text-sm font-medium">Thêm công thức</summary>
        <div className="mt-4 max-w-xl">
          <FormThemCongThuc />
        </div>
      </details>
    </main>
  );
}
