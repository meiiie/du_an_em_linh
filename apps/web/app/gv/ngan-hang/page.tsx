import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/ui/page-header";
import { LABEL4, STATUS_LABEL, type Muc4 } from "@/lib/levels";
import { db } from "@/lib/db";
import { problems } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

function tone(status: string) {
  if (status === "DA_PHAT_HANH") return "ok" as const;
  if (status === "BI_CHAN") return "bad" as const;
  return "warn" as const;
}

export default async function Page() {
  const rows = await db.select().from(problems);
  return (
    <main>
      <PageHeader kicker="Nội dung" title="Ngân hàng bài" description="Mọi bài đã qua cổng lúc nạp. Đổi nội dung sẽ đổi hash và phải kiểm lại." />
      <ul className="divide-y divide-line border-y border-line">
        {rows.map((p) => (
          <li key={p.id} className="py-4">
            <div className="flex flex-wrap items-center gap-2">
              <p className="font-mono text-sm font-medium" translate="no">
                {p.code}
              </p>
              <Badge>{LABEL4[p.mucDo4 as Muc4] || p.mucDo4}</Badge>
              <Badge tone="neutral">3 mức {p.mucDoBo3}</Badge>
              <Badge tone={tone(p.status)}>{STATUS_LABEL[p.status] || p.status}</Badge>
            </div>
            <p className="mt-2 text-sm">{p.statementText}</p>
            <p className="mt-1 text-xs text-muted">Nguồn {p.origin}</p>
          </li>
        ))}
      </ul>
    </main>
  );
}
