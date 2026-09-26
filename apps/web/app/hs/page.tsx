import Link from "next/link";
import { eq } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { masteryStates, problems, skills } from "@/lib/db/schema";
import { recommend } from "@/lib/learning";
import { LABEL4, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function HsHome() {
  const u = await requireRole("HS");
  const states = await db.select().from(masteryStates).where(eq(masteryStates.studentId, u.id));
  const skillRows = await db.select().from(skills);
  const name = new Map(skillRows.map((s) => [s.code, s.name]));
  const pubs = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  const waiting = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const goi = await recommend(u.id);
  return (
    <main className="space-y-4">
      <section className="rounded-2xl bg-navy p-4 text-white">
        <h1 className="text-2xl font-bold">Chào {u.displayName}</h1>
        <p className="mt-1 text-sm text-blue-100">Bốn mức: Nhận biết → Thông hiểu → Vận dụng → Vận dụng cao. Lặp đến khi làm được vận dụng cao.</p>
      </section>
      {goi ? (
        <section className="rounded-2xl border border-stone-300 bg-white p-4">
          <h2 className="font-semibold">Bài nên làm tiếp</h2>
          <p className="text-sm text-slate-600">{goi.lyDo}</p>
          <Link href={`/hs/luyen/${goi.problem.id}`} className="mt-2 inline-block font-semibold text-navy">
            {goi.problem.statementText}
          </Link>
        </section>
      ) : null}
      <section className="rounded-2xl border border-stone-300 bg-white p-4">
        <h2 className="font-semibold">Thành thạo theo kỹ năng</h2>
        <p className="text-xs text-slate-500">Số là xác suất thành thạo của mô hình BKT (0–1), không phải điểm bài kiểm tra.</p>
        <ul className="mt-2 space-y-2">
          {states.map((s) => (
            <li key={s.skillCode} className="flex items-center justify-between gap-2 text-sm">
              <span>
                <span className="font-mono text-xs text-slate-500">{s.skillCode}</span> {name.get(s.skillCode)}
              </span>
              <span className="shrink-0 rounded-full bg-stone-100 px-2 py-0.5">
                {LABEL4[s.currentMucDo4 as Muc4] || s.currentMucDo4} · {s.mastery.toFixed(2)}
              </span>
            </li>
          ))}
        </ul>
      </section>
      <section className="rounded-2xl border border-stone-300 bg-white p-4">
        <h2 className="font-semibold">Bài đã phát hành</h2>
        <ul className="mt-2 space-y-2">
          {pubs.map((p) => (
            <li key={p.id}>
              <Link data-testid={`bai-${p.code}`} href={`/hs/luyen/${p.id}`} className="text-sm font-medium text-navy">
                {LABEL4[p.mucDo4 as Muc4]} — {p.statementText}
              </Link>
            </li>
          ))}
        </ul>
        {waiting.length ? (
          <p className="mt-3 text-sm text-amber-800">{waiting.length} bài đang chờ thầy cô duyệt, chưa mở để làm.</p>
        ) : null}
      </section>
    </main>
  );
}
