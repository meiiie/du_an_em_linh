import { eq } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { reminders, studySchedules } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function LichPage() {
  const u = await requireRole("HS");
  const sched = await db.select().from(studySchedules).where(eq(studySchedules.studentId, u.id)).limit(1);
  const s = sched[0];
  const rems = s ? await db.select().from(reminders).where(eq(reminders.scheduleId, s.id)) : [];
  const slots = (s?.weeklySlots as { thu: string; gio: string; viec: string }[]) || [];
  return (
    <main className="space-y-4">
      <section className="rounded-2xl border border-stone-300 bg-white p-4">
        <h1 className="text-xl font-bold">Phương pháp học</h1>
        <p className="mt-2 whitespace-pre-line text-sm leading-relaxed">{s?.methodAdvice}</p>
      </section>
      <section className="rounded-2xl border border-stone-300 bg-white p-4">
        <h2 className="font-semibold">Thời gian biểu tuần</h2>
        <ul className="mt-2 space-y-1 text-sm">
          {slots.map((sl) => (
            <li key={sl.thu + sl.gio} className="flex justify-between gap-2 border-b border-stone-100 py-1">
              <span>{sl.thu}</span>
              <span>{sl.gio}</span>
              <span className="text-slate-600">{sl.viec}</span>
            </li>
          ))}
        </ul>
      </section>
      <section className="rounded-2xl border border-stone-300 bg-white p-4">
        <h2 className="font-semibold">Nhắc trong ứng dụng</h2>
        <ul className="mt-2 space-y-2">
          {rems.map((r) => (
            <li key={r.id} className="rounded-xl bg-amber-50 px-3 py-2 text-sm">
              <span className="font-semibold">{r.title}</span> — {r.body}
              <span className="mt-1 block text-xs text-slate-500">{r.sendAt} · kênh {r.channel}</span>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
