import type { Metadata } from "next";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/ui/page-header";
import { Tex } from "@/components/tex";
import { hamLatex, nhanMuc4 } from "@/lib/de-hoc-sinh";
import { LABEL4, STATUS_LABEL, type Muc4 } from "@/lib/levels";
import { db } from "@/lib/db";
import { problems } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Đề bài",
};

function tone(status: string) {
  if (status === "DA_PHAT_HANH") return "ok" as const;
  if (status === "BI_CHAN") return "bad" as const;
  return "warn" as const;
}

export default async function Page() {
  const rows = await db.select().from(problems);
  return (
    <main>
      <PageHeader title="Đề bài" />
      <ul className="divide-y divide-line border-y border-line">
        {rows.map((p) => {
          const ham = hamLatex(p.statementLatex);
          return (
            <li key={p.id} className="py-4">
              <div className="flex flex-wrap items-center gap-2">
                {ham ? <Tex tex={ham} className="text-sm font-medium" /> : <p className="text-sm font-medium">{p.statementText}</p>}
                <Badge>{LABEL4[p.mucDo4 as Muc4] || nhanMuc4(p.mucDo4)}</Badge>
                <Badge tone={tone(p.status)}>{STATUS_LABEL[p.status] || p.status}</Badge>
              </div>
            </li>
          );
        })}
      </ul>
    </main>
  );
}
