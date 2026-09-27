import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { PageHeader } from "@/components/ui/page-header";
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
    <main className="space-y-8">
      <PageHeader title="Lịch học" />
      <section>
        <h2 className="text-base font-semibold">Cách học</h2>
        <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-ink">{live.loiKhuyen}</p>
      </section>
      <section>
        <h2 className="text-base font-semibold">Tuần này</h2>
        {slots.length === 0 ? <p className="mt-2 text-sm text-muted">Chưa có khung giờ.</p> : null}
        <ul className="mt-2 divide-y divide-line border-y border-line text-sm">
          {slots.map((sl) => (
            <li key={sl.thu + sl.gio} className="flex min-h-11 justify-between gap-2 py-3">
              <span className="font-medium">{sl.thu}</span>
              <span className="tabular text-muted">{sl.gio}</span>
              <span className="text-muted">{sl.viec}</span>
            </li>
          ))}
        </ul>
      </section>
      <section>
        <h2 className="text-base font-semibold">Lời nhắc</h2>
        {rems.length === 0 ? <p className="mt-2 text-sm text-muted">Không có lời nhắc.</p> : null}
        <ul className="mt-2 divide-y divide-line border-y border-line">
          {rems.map((r) => (
            <li key={r.id} className="py-3 text-sm">
              <span className="font-medium">{r.title}</span> — {r.body}
              <span className="mt-1 block text-xs text-muted">{r.sendAt}</span>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
