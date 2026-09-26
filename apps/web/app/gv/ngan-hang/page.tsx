import { LABEL4, STATUS_LABEL, type Muc4 } from "@/lib/levels";
import { db } from "@/lib/db";
import { problems } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function Page() {
  const rows = await db.select().from(problems);
  return (
    <main>
      <h1 className="mb-3 text-xl font-bold">Ngân hàng bài</h1>
      <ul className="space-y-2">
        {rows.map((p) => (
          <li key={p.id} className="rounded-2xl bg-white p-3 text-sm shadow-sm ring-1 ring-stone-200">
            <p className="font-semibold">{p.code}</p>
            <p>{p.statementText}</p>
            <p className="text-xs text-slate-500">
              {LABEL4[p.mucDo4 as Muc4] || p.mucDo4} · 3 mức {p.mucDoBo3} · {STATUS_LABEL[p.status] || p.status} · {p.origin}
            </p>
          </li>
        ))}
      </ul>
    </main>
  );
}
