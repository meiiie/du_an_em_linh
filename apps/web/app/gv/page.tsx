import Link from "next/link";
import { eq, isNull } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { db } from "@/lib/db";
import { escalations, problems, users } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function GvHome() {
  const u = await requireRole("GV");
  await ghiNhatKy(u.id, "XEM_TONG_QUAN", "class", "12A1");
  const stuck = await db.select().from(escalations).where(isNull(escalations.handledAt));
  const names = await db.select().from(users);
  const name = new Map(names.map((n) => [n.id, n.displayName]));
  const queue = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const blocked = await db.select().from(problems).where(eq(problems.status, "BI_CHAN"));
  return (
    <main className="space-y-4">
      <section className="rounded-2xl bg-clay p-4 text-white">
        <h1 className="text-2xl font-bold">Lớp 12A1 thử</h1>
        <p className="text-sm text-orange-50">Dữ liệu tổng hợp, không có học sinh thật. Cổng phụ huynh chưa mở.</p>
      </section>
      <section className="rounded-2xl border border-red-200 bg-white p-4" data-testid="canh-bao-ket">
        <h2 className="font-semibold text-red-800">Học sinh bị kẹt</h2>
        {stuck.length === 0 ? <p className="text-sm">Chưa có cảnh báo.</p> : null}
        <ul className="mt-2 space-y-1 text-sm">
          {stuck.map((e) => (
            <li key={e.id}>
              {name.get(e.studentId)} — {e.skillCode}: {e.reason}
            </li>
          ))}
        </ul>
      </section>
      <section className="grid gap-3 sm:grid-cols-2">
        <Link href="/gv/duyet" className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
          <p className="text-3xl font-bold">{queue.length}</p>
          <p>bài chờ duyệt</p>
        </Link>
        <Link href="/gv/ngan-hang" className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
          <p className="text-3xl font-bold">{blocked.length}</p>
          <p>bài bị chặn</p>
        </Link>
      </section>
    </main>
  );
}
