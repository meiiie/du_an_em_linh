import Link from "next/link";
import { eq } from "drizzle-orm";
import { Badge } from "@/components/ui/badge";
import { buttonClasses } from "@/components/ui/button";
import { MasteryCells } from "@/components/mastery-cells";
import { WorkRow } from "@/components/work-row";
import { cn } from "@/lib/cn";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { assignments, masteryStates, problems, skills } from "@/lib/db/schema";
import { recommend } from "@/lib/learning";
import { LABEL4, labelBloom, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function HsHome() {
  const u = await requireRole("HS");
  const states = await db.select().from(masteryStates).where(eq(masteryStates.studentId, u.id));
  const skillRows = await db.select().from(skills);
  const name = new Map(skillRows.map((s) => [s.code, s.name]));
  const pubs = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  const waiting = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const giao = await db.select().from(assignments).where(eq(assignments.studentId, u.id));
  const giaoIds = new Set(giao.map((a) => a.problemId));
  const baiGiao = pubs.filter((p) => giaoIds.has(p.id));
  const danhSach = baiGiao.length ? baiGiao : pubs;
  const goi = await recommend(u.id);
  return (
    <main className="space-y-8">
      <header>
        <p className="text-sm text-muted">Lộ trình bốn mức · thang Bloom trên từng bài · tới vận dụng cao</p>
        <h1 className="mt-2 text-pretty text-[1.75rem] font-semibold tracking-tight">Chào {u.displayName}</h1>
      </header>

      {goi ? (
        <section className="border-y border-line py-6">
          <p className="text-sm text-muted">Bài cho em — dạng yếu / cùng mức / nâng một nấc</p>
          <p className="mt-2 text-lg font-medium leading-snug">{goi.problem.statementText}</p>
          <p className="mt-2 text-sm text-muted">
            {goi.lyDo} · Bloom {labelBloom(goi.problem.bloomLevel)}
          </p>
          <Link href={`/hs/luyen/${goi.problem.id}`} className={cn(buttonClasses(), "mt-4")}>
            Làm bước tiếp
          </Link>
        </section>
      ) : null}

      <section>
        <h2 className="text-base font-semibold">Thành thạo theo kỹ năng</h2>
        <p className="mt-1 text-xs text-muted">Ô đặc = xác suất BKT, không phải điểm kiểm tra.</p>
        <ul className="mt-4 divide-y divide-line border-y border-line">
          {states.map((s) => (
            <li key={s.skillCode} className="flex flex-wrap items-center justify-between gap-3 py-3">
              <div className="min-w-0">
                <p className="font-mono text-xs text-muted" translate="no">
                  {s.skillCode}
                </p>
                <p className="text-sm">{name.get(s.skillCode)}</p>
              </div>
              <div className="flex items-center gap-3">
                <MasteryCells value={s.mastery} />
                <Badge>
                  {LABEL4[s.currentMucDo4 as Muc4] || s.currentMucDo4} · {s.mastery.toFixed(2)}
                </Badge>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div className="mb-1 flex items-end justify-between gap-2">
          <h2 className="text-base font-semibold">Bài giao cho em</h2>
          <Link href="/hs/bai" className="text-sm text-muted underline-offset-2 hover:text-ink hover:underline">
            Xem ngân bài
          </Link>
        </div>
        <div className="border-y border-line">
          {danhSach.map((p) => (
            <WorkRow
              key={p.id}
              href={`/hs/luyen/${p.id}`}
              testId={`bai-${p.code}`}
              kicker={`${LABEL4[p.mucDo4 as Muc4] || p.mucDo4} · Bloom ${labelBloom(p.bloomLevel)}`}
              title={p.statementText}
            />
          ))}
        </div>
        {waiting.length ? (
          <p className="mt-3 text-sm text-warn">{waiting.length} bài đang chờ thầy cô duyệt, chưa mở để làm.</p>
        ) : null}
      </section>
    </main>
  );
}
