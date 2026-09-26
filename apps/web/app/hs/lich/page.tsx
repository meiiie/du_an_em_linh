import { eq } from "drizzle-orm";
import { PageHeader } from "@/components/ui/page-header";
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
    <main className="space-y-8">
      <PageHeader kicker="Học sinh" title="Thời gian biểu" description="Lời khuyên phương pháp và nhắc trong ứng dụng. Chưa gửi email hay SMS." />
      <section>
        <h2 className="text-base font-semibold">Phương pháp học</h2>
        <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-ink">{s?.methodAdvice}</p>
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
        <h2 className="text-base font-semibold">Nhắc trong ứng dụng</h2>
        {rems.length === 0 ? <p className="mt-2 text-sm text-muted">Không có lời nhắc.</p> : null}
        <ul className="mt-2 divide-y divide-line border-y border-line">
          {rems.map((r) => (
            <li key={r.id} className="py-3 text-sm">
              <span className="font-medium">{r.title}</span> — {r.body}
              <span className="mt-1 block text-xs text-muted">
                {r.sendAt} · kênh {r.channel}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
