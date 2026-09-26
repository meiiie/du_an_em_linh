import { eq } from "drizzle-orm";
import { PageHeader } from "@/components/ui/page-header";
import { WorkRow } from "@/components/work-row";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { problems } from "@/lib/db/schema";
import { LABEL4, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function Page() {
  await requireRole("HS");
  const pubs = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  const waiting = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  return (
    <main>
      <PageHeader
        kicker="Học sinh"
        title="Ngân bài đã phát hành"
        description="Chỉ bài qua cổng ba tầng hoặc được giáo viên duyệt mới mở. Bài chờ duyệt không làm được."
      />
      <div className="border-y border-line">
        {pubs.map((p) => (
          <WorkRow
            key={p.id}
            href={`/hs/luyen/${p.id}`}
            testId={`catalog-${p.code}`}
            kicker={
              <>
                {LABEL4[p.mucDo4 as Muc4] || p.mucDo4} · <span translate="no">{p.code}</span>
              </>
            }
            title={p.statementText}
          />
        ))}
      </div>
      {waiting.length ? <p className="mt-4 text-sm text-warn">{waiting.length} bài đang chờ thầy cô duyệt.</p> : null}
    </main>
  );
}
