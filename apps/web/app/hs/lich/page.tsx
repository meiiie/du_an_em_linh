import { eq } from "drizzle-orm";
import { Card } from "@/components/ui/card";
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
    <main className="space-y-4">
      <PageHeader kicker="Học sinh" title="Thời gian biểu" description="Lời khuyên phương pháp và nhắc trong ứng dụng. Chưa gửi email hay SMS." />
      <Card>
        <h2 className="text-xl font-semibold">Phương pháp học</h2>
        <p className="mt-2 whitespace-pre-line text-sm leading-relaxed">{s?.methodAdvice}</p>
      </Card>
      <Card>
        <h2 className="text-xl font-semibold">Tuần này</h2>
        {slots.length === 0 ? <p className="mt-2 text-sm text-muted">Chưa có khung giờ.</p> : null}
        <ul className="mt-2 space-y-1 text-sm">
          {slots.map((sl) => (
            <li key={sl.thu + sl.gio} className="flex justify-between gap-2 border-b border-line/70 py-2 last:border-0">
              <span className="font-medium">{sl.thu}</span>
              <span className="tabular">{sl.gio}</span>
              <span className="text-muted">{sl.viec}</span>
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <h2 className="text-xl font-semibold">Nhắc trong ứng dụng</h2>
        {rems.length === 0 ? <p className="mt-2 text-sm text-muted">Không có lời nhắc.</p> : null}
        <ul className="mt-2 space-y-2">
          {rems.map((r) => (
            <li key={r.id} className="rounded-xl bg-amber-50 px-3 py-2 text-sm">
              <span className="font-semibold">{r.title}</span> — {r.body}
              <span className="mt-1 block text-xs text-muted">
                {r.sendAt} · kênh {r.channel}
              </span>
            </li>
          ))}
        </ul>
      </Card>
    </main>
  );
}
