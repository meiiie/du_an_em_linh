import { desc, eq } from "drizzle-orm";
import { themCongThuc } from "@/lib/actions/gv";
import { Tex } from "@/components/tex";
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
      <section className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
        <h1 className="text-xl font-bold">Bảng công thức phiên bản {latest?.version ?? 0}</h1>
        {stale.length ? (
          <p className="mt-2 rounded-xl bg-amber-50 px-3 py-2 text-sm">
            Đổi phiên bản bảng công thức làm {stale.length} lần kiểm định cũ. Bài đang phát hành không bị gỡ; cần kiểm định lại khi sửa nội dung.
          </p>
        ) : null}
        <ul className="mt-3 space-y-3">
          {rows.map((f) => (
            <li key={f.id}>
              <p className="font-medium">{f.title}</p>
              <Tex tex={f.latex} />
              <p className="text-sm text-slate-600">{f.noiDung}</p>
            </li>
          ))}
        </ul>
      </section>
      <form action={themCongThuc} className="space-y-2 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
        <h2 className="font-semibold">Thêm công thức — tạo phiên bản mới</h2>
        <input name="title" required placeholder="Tên" className="w-full rounded-lg border px-2 py-1.5" />
        <input name="latex" placeholder="LaTeX" className="w-full rounded-lg border px-2 py-1.5" />
        <textarea name="noi_dung" required placeholder="Nội dung tiếng Việt" className="w-full rounded-lg border px-2 py-1.5" />
        <button className="rounded-xl bg-clay px-4 py-2 font-semibold text-white" type="submit">
          Khóa phiên bản mới
        </button>
      </form>
    </main>
  );
}
