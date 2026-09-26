import { bacBai, duyetBai } from "@/lib/actions/gv";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { fieldControl } from "@/components/ui/field";
import { db } from "@/lib/db";
import { problems, verificationRuns, verificationTierResults } from "@/lib/db/schema";
import { moTaTrichDan } from "@/lib/citations";
import { STATUS_LABEL } from "@/lib/levels";

export const dynamic = "force-dynamic";

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
    <main className="space-y-4" data-testid="hang-doi">
      <PageHeader
        kicker="Kiểm định"
        title="Hàng đợi kiểm định"
        description="Mỗi tầng ghi Đạt / Sai / Không kiểm được. Không kiểm được thì chờ duyệt. Sai thì bị chặn."
      />
      {queue.length === 0 ? <Card>Không còn bài trong hàng đợi.</Card> : null}
      {queue.map((p) => {
        const run = runs.filter((r) => r.problemId === p.id).sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())[0];
        const ts = tiers.filter((t) => t.runId === run?.id).sort((a, b) => a.tier - b.tier);
        return (
          <article key={p.id} data-testid={`duyet-${p.code}`}>
            <Card>
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-mono text-sm font-semibold" translate="no">
                  {p.code}
                </p>
                <Badge tone={tone(p.status)}>{STATUS_LABEL[p.status] || p.status}</Badge>
              </div>
              <p className="mt-2 text-sm">{p.statementText}</p>
              <ul className="mt-3 space-y-2 text-sm">
                {ts.map((t) => {
                  const cites = moTaTrichDan(t.citation);
                  return (
                    <li key={t.id} className="rounded-xl bg-paper px-3 py-2">
                      <span className="font-semibold">Tầng {t.tier}:</span> {STATUS_LABEL[t.status] || t.status}
                      {t.reasonText ? ` — ${t.reasonText}` : ""}
                      {cites.length ? (
                        <ul className="mt-1 list-disc space-y-0.5 pl-5 text-xs text-muted">
                          {cites.map((c) => (
                            <li key={c}>{c}</li>
                          ))}
                        </ul>
                      ) : null}
                    </li>
                  );
                })}
              </ul>
              {p.status === "CHO_GIAO_VIEN_DUYET" ? (
                <div className="mt-3 flex flex-wrap gap-2">
                  <form
                    action={async (fd) => {
                      "use server";
                      await duyetBai(p.id, String(fd.get("note") || "Đồng ý phát hành sau khi xem tầng không kiểm được"));
                    }}
                    className="flex flex-1 flex-wrap gap-2"
                  >
                    <label className="sr-only" htmlFor={`note-${p.id}`}>
                      Ghi chú duyệt
                    </label>
                    <input
                      id={`note-${p.id}`}
                      name="note"
                      defaultValue="Đã xem trích dẫn và lời giải, cho phát hành."
                      className={`min-w-[220px] flex-1 ${fieldControl}`}
                    />
                    <Button type="submit" variant="accent">
                      Duyệt
                    </Button>
                  </form>
                  <form
                    action={async () => {
                      "use server";
                      await bacBai(p.id, "Chưa đủ căn cứ");
                    }}
                  >
                    <Button type="submit" variant="danger">
                      Bác
                    </Button>
                  </form>
                </div>
              ) : null}
            </Card>
          </article>
        );
      })}
    </main>
  );
}
