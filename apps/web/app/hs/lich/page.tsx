import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { LichTuan } from "@/components/lich-tuan";
import { tuVanHocTap } from "@/lib/counsel";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { masteryStates, reminders, studySchedules } from "@/lib/db/schema";
import { ghepNhacVaoSlot, thuBuoiTiep, thuHomNay } from "@/lib/lich";

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
  const homNay = thuHomNay();
  const { slots } = ghepNhacVaoSlot(
    live.slots,
    rems.map((r) => ({ id: r.id, title: r.title, body: r.body, sendAt: r.sendAt || "" })),
    homNay,
  );
  const buoiTiep = thuBuoiTiep(live.slots, homNay);
  return (
    <main>
      <header className="mb-6">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Lịch học</h1>
        <p className="mt-2 max-w-[65ch] text-sm leading-relaxed text-muted">{live.loiKhuyen}</p>
      </header>
      {slots.length === 0 ? <p className="text-sm text-muted">Chưa có khung giờ.</p> : null}
      {slots.length ? <LichTuan slots={slots} homNay={homNay} buoiTiep={buoiTiep} /> : null}
    </main>
  );
}
