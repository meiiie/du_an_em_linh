import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { tuVanHocTap } from "@/lib/counsel";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { masteryStates, reminders, studySchedules } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Lịch",
};

export default async function LichPage() {
  const u = await requireRole("HS");
  const sched = await db.select().from(studySchedules).where(eq(studySchedules.studentId, u.id)).limit(1);
  const s = sched[0];
  const rems = s ? await db.select().from(reminders).where(eq(reminders.scheduleId, s.id)) : [];
  const states = await db.select().from(masteryStates).where(eq(masteryStates.studentId, u.id));
  const live = tuVanHocTap(states);
  const slots = live.slots;
  return (
    <main>
      <header className="mb-6 border-b border-line pb-4">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Lịch học</h1>
        <p className="mt-2 max-w-[65ch] text-sm leading-relaxed text-muted">{live.loiKhuyen}</p>
      </header>
      {slots.length === 0 && rems.length === 0 ? <p className="text-sm text-muted">Chưa có khung giờ.</p> : null}
      {slots.length || rems.length ? (
        <ul className="divide-y divide-line border-b border-line text-sm">
          {slots.map((sl) => (
            <li
              key={sl.thu + sl.gio}
              className="grid min-h-11 grid-cols-[6.5rem_4rem_minmax(0,1fr)] items-center gap-3 py-3"
            >
              <span className="font-medium">{sl.thu}</span>
              <span className="tabular text-muted">{sl.gio}</span>
              <span className="min-w-0 text-muted">{sl.viec}</span>
            </li>
          ))}
          {rems.map((r) => (
            <li key={r.id} className="py-3">
              <p className="font-medium">{r.title}</p>
              <p className="mt-1 text-muted">{r.body}</p>
              <p className="mt-1 text-xs text-muted">{r.sendAt}</p>
            </li>
          ))}
        </ul>
      ) : null}
    </main>
  );
}
