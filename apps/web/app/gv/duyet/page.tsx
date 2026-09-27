import type { Metadata } from "next";
import { bacBai, duyetBai } from "@/lib/actions/gv";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { fieldControl } from "@/components/ui/field";
import { Tex } from "@/components/tex";
import { db } from "@/lib/db";
import { problems, verificationRuns, verificationTierResults } from "@/lib/db/schema";
import { moTaTrichDan } from "@/lib/citations";
import { gonLyDoDuyet, hamLatex, tenCuaTang } from "@/lib/de-hoc-sinh";
import { STATUS_LABEL } from "@/lib/levels";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Duyệt",
};

function tone(status: string) {
  if (status === "DAT" || status === "GV_DUYET" || status === "DA_PHAT_HANH") return "ok" as const;
  if (status === "SAI" || status === "BI_CHAN") return "bad" as const;
  return "warn" as const;
}

export default async function Page() {
  const rows = await db.select().from(problems);
  const queue = rows.filter((p) => p.status === "CHO_GIAO_VIEN_DUYET" || p.status === "BI_CHAN");
  const runs = await db.select().from(verificationRuns);
  const tiers = await db.select().from(verificationTierResults);
  return (
    <main className="space-y-6" data-testid="hang-doi">
      <PageHeader title="Duyệt" />
      {queue.length === 0 ? <p className="text-sm text-muted">Không còn bài chờ.</p> : null}
      {queue.map((p) => {
        const run = runs.filter((r) => r.problemId === p.id).sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())[0];
        const ts = tiers.filter((t) => t.runId === run?.id).sort((a, b) => a.tier - b.tier);
        const ham = hamLatex(p.statementLatex);
        return (
          <article key={p.id} data-testid={`duyet-${p.code}`} className="border-t border-line pt-6">
            <div className="flex flex-wrap items-center gap-3">
              {ham ? <Tex tex={ham} className="text-sm" /> : <p className="text-sm font-medium">{p.statementText}</p>}
              <Badge tone={tone(p.status)}>{STATUS_LABEL[p.status] || p.status}</Badge>
            </div>
            <ul className="mt-3 divide-y divide-line border-y border-line text-sm">
              {ts.map((t) => {
                const cites = moTaTrichDan(t.citation);
                return (
                  <li key={t.id} className="flex gap-3 py-3">
                    <span className="w-20 shrink-0 text-xs text-muted">{tenCuaTang(t.tier)}</span>
                    <div className="min-w-0">
                      <p>
                        <span className="font-medium">{STATUS_LABEL[t.status] || t.status}</span>
                        {t.reasonText ? ` — ${gonLyDoDuyet(t.reasonText)}` : ""}
                      </p>
                      {cites.length ? (
                        <ul className="mt-1 space-y-0.5 text-xs text-muted">
                          {cites.map((c) => (
                            <li key={c}>{c}</li>
                          ))}
                        </ul>
                      ) : null}
                    </div>
                  </li>
                );
              })}
            </ul>
            {p.status === "CHO_GIAO_VIEN_DUYET" ? (
              <div className="mt-4 flex flex-wrap gap-2">
                <form
                  action={async (fd) => {
                    "use server";
                    await duyetBai(p.id, String(fd.get("note") || "Đã xem, cho học."));
                  }}
                  className="flex flex-1 flex-wrap gap-2"
                >
                  <label className="sr-only" htmlFor={`note-${p.id}`}>
                    Ghi chú
                  </label>
                  <input
                    id={`note-${p.id}`}
                    name="note"
                    defaultValue="Đã xem, cho học."
                    className={`min-w-[220px] flex-1 ${fieldControl}`}
                  />
                  <Button type="submit" variant="accent">
                    Mở
                  </Button>
                </form>
                <form
                  action={async () => {
                    "use server";
                    await bacBai(p.id, "Chưa đủ căn cứ");
                  }}
                >
                  <Button type="submit" variant="danger">
                    Không mở
                  </Button>
                </form>
              </div>
            ) : null}
          </article>
        );
      })}
    </main>
  );
}
