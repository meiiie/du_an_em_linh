import Link from "next/link";
import { eq } from "drizzle-orm";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
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
    <main className="space-y-5">
      <section className="rounded-card bg-navy px-6 py-8 text-chalk">
        <p className="text-sm font-medium text-[#8EB7E0]">Lộ trình bốn mức</p>
        <h1 className="mt-1 text-pretty text-3xl font-semibold">Chào {u.displayName}</h1>
        <p className="mt-2 max-w-xl text-sm leading-relaxed text-chalk/75">
          Nhận biết → Thông hiểu → Vận dụng → Vận dụng cao. Gia sư không đưa đáp án. Lặp đến khi làm được vận dụng cao.
        </p>
      </section>
      {goi ? (
        <Card className="border-l-4 border-primary">
          <p className="text-sm font-medium text-primary">Bài nên làm tiếp</p>
          <p className="mt-1 text-sm text-muted">{goi.lyDo}</p>
          <Link href={`/hs/luyen/${goi.problem.id}`} className="mt-2 inline-block font-semibold text-primary hover:underline">
            {goi.problem.statementText}
          </Link>
        </Card>
      ) : null}
      <Card>
        <h2 className="text-xl font-semibold">Thành thạo theo kỹ năng</h2>
        <p className="mt-1 text-xs text-muted">Số là xác suất thành thạo của mô hình BKT (0–1), không phải điểm bài kiểm tra.</p>
        <ul className="mt-3 space-y-3">
          {states.map((s) => (
            <li key={s.skillCode}>
              <div className="mb-1 flex items-center justify-between gap-2 text-sm">
                <span className="min-w-0">
                  <span className="font-mono text-xs text-muted" translate="no">
                    {s.skillCode}
                  </span>{" "}
                  {name.get(s.skillCode)}
                </span>
                <Badge>
                  {LABEL4[s.currentMucDo4 as Muc4] || s.currentMucDo4} · {s.mastery.toFixed(2)}
                </Badge>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-paper">
                <div className="h-full bg-primary" style={{ width: `${Math.round(s.mastery * 100)}%` }} />
              </div>
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-xl font-semibold">Bài đã phát hành</h2>
          <Link href="/hs/bai" className="text-sm font-medium text-primary hover:underline">
            Xem ngân bài
          </Link>
        </div>
        <ul className="mt-3 space-y-2">
          {pubs.map((p) => (
            <li key={p.id}>
              <Link
                data-testid={`bai-${p.code}`}
                href={`/hs/luyen/${p.id}`}
                className="text-sm font-medium text-primary hover:underline"
              >
                {LABEL4[p.mucDo4 as Muc4]} — {p.statementText}
              </Link>
            </li>
          ))}
        </ul>
        {waiting.length ? (
          <p className="mt-3 text-sm text-warn">{waiting.length} bài đang chờ thầy cô duyệt, chưa mở để làm.</p>
        ) : null}
      </Card>
    </main>
  );
}
