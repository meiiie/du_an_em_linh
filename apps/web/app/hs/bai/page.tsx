import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { PageHeader } from "@/components/ui/page-header";
import { Tex } from "@/components/tex";
import { WorkRow } from "@/components/work-row";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { problems } from "@/lib/db/schema";
import { hamLatex, nhanMuc4 } from "@/lib/de-hoc-sinh";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Ngân bài",
};

export default async function Page() {
  await requireRole("HS");
  const pubs = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  const waiting = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  return (
    <main>
      <PageHeader title="Ngân bài" />
      <div className="border-y border-line">
        {pubs.map((p) => {
          const ham = hamLatex(p.statementLatex);
          return (
            <WorkRow
              key={p.id}
              href={`/hs/luyen/${p.id}`}
              testId={`catalog-${p.code}`}
              title={ham ? <Tex tex={ham} /> : p.statementText}
              meta={nhanMuc4(p.mucDo4)}
            />
          );
        })}
      </div>
      {waiting.length ? <p className="mt-4 text-sm text-warn">{waiting.length} bài thầy cô chưa mở.</p> : null}
    </main>
  );
}
