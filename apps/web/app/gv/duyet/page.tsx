import { bacBai, duyetBai } from "@/lib/actions/gv";
import { db } from "@/lib/db";
import { problems, verificationRuns, verificationTierResults } from "@/lib/db/schema";
import { STATUS_LABEL } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function Page() {
  const rows = await db.select().from(problems);
  const queue = rows.filter((p) => p.status === "CHO_GIAO_VIEN_DUYET" || p.status === "BI_CHAN");
  const runs = await db.select().from(verificationRuns);
  const tiers = await db.select().from(verificationTierResults);
  return (
    <main className="space-y-3" data-testid="hang-doi">
      <h1 className="text-xl font-bold">Hàng đợi kiểm định</h1>
      {queue.map((p) => {
        const run = runs.filter((r) => r.problemId === p.id).sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())[0];
        const ts = tiers.filter((t) => t.runId === run?.id).sort((a, b) => a.tier - b.tier);
        return (
          <article key={p.id} className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200" data-testid={`duyet-${p.code}`}>
            <p className="font-semibold">{p.code}</p>
            <p className="text-sm">{p.statementText}</p>
            <p className="text-xs text-slate-500">Trạng thái: {STATUS_LABEL[p.status] || p.status}</p>
            <ul className="mt-2 space-y-1 text-sm">
              {ts.map((t) => (
                <li key={t.id}>
                  <span className="font-semibold">Tầng {t.tier}:</span> {STATUS_LABEL[t.status] || t.status}
                  {t.reasonText ? ` — ${t.reasonText}` : ""}
                  {t.citation ? (
                    <span className="mt-1 block text-xs text-slate-600">Trích: {JSON.stringify(t.citation).slice(0, 280)}</span>
                  ) : null}
                </li>
              ))}
            </ul>
            {p.status === "CHO_GIAO_VIEN_DUYET" ? (
              <div className="mt-3 flex flex-wrap gap-2">
                <form
                  action={async (fd) => {
                    "use server";
                    await duyetBai(p.id, String(fd.get("note") || "Đồng ý phát hành sau khi xem tầng không kiểm được"));
                  }}
                >
                  <input name="note" defaultValue="Đã xem trích dẫn và lời giải, cho phát hành." className="mr-2 rounded border px-2 py-1 text-sm" />
                  <button className="rounded-lg bg-teal px-3 py-1.5 text-sm font-semibold text-white" type="submit">
                    Duyệt
                  </button>
                </form>
                <form
                  action={async () => {
                    "use server";
                    await bacBai(p.id, "Chưa đủ căn cứ");
                  }}
                >
                  <button className="rounded-lg bg-red-800 px-3 py-1.5 text-sm font-semibold text-white" type="submit">
                    Bác
                  </button>
                </form>
              </div>
            ) : null}
          </article>
        );
      })}
    </main>
  );
}
