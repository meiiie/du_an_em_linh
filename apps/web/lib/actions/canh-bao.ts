"use server";

import { eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";
import { requireRole } from "../auth";
import { db } from "../db";
import { auditLogs, escalations } from "../db/schema";
import { gvDayHs } from "../lop";

/** UXT-09-d: giáo viên đánh dấu một cảnh báo kẹt / lời nhờ là «Đã xử lý» (chỉ cảnh báo của HS lớp mình dạy, F-08). */
export async function daXuLyCanhBao(id: string) {
  const u = await requireRole("GV");
  const e = (await db.select().from(escalations).where(eq(escalations.id, id)).limit(1))[0];
  if (!e || e.handledAt) return;
  if (!(await gvDayHs(u.id, e.studentId))) return;
  const now = new Date();
  await db.update(escalations).set({ handledAt: now, handledBy: u.id }).where(eq(escalations.id, id));
  await db.insert(auditLogs).values({
    id: crypto.randomUUID(),
    actorUserId: u.id,
    action: "XU_LY_CANH_BAO",
    entity: "escalation",
    entityId: id,
    at: now,
    reason: e.loai,
  });
  revalidatePath("/gv", "layout");
}
