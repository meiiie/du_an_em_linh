import Link from "next/link";
import { eq } from "drizzle-orm";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
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
      <ul className="space-y-3">
        {pubs.map((p) => (
          <li key={p.id}>
            <Card>
              <div className="flex flex-wrap items-center gap-2">
                <Badge tone="info">{LABEL4[p.mucDo4 as Muc4] || p.mucDo4}</Badge>
                <span className="font-mono text-xs text-muted" translate="no">
                  {p.code}
                </span>
              </div>
              <Link
                href={`/hs/luyen/${p.id}`}
                className="mt-2 block font-medium text-primary hover:underline"
                data-testid={`catalog-${p.code}`}
              >
                {p.statementText}
              </Link>
            </Card>
          </li>
        ))}
      </ul>
      {waiting.length ? <p className="mt-4 text-sm text-warn">{waiting.length} bài đang chờ thầy cô duyệt.</p> : null}
    </main>
  );
}
