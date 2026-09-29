/**
 * F-08: phân quyền theo lớp. Mọi đọc/ghi dữ liệu lớp đi qua các hàm này; KHÔNG lấy `rows[0]` của cả bảng.
 * - GV chỉ thấy lớp mình dạy (enrollments.role_in_class = 'GV') và HS của các lớp đó.
 * - HS chỉ đọc cài đặt của lớp mình.
 */
import { and, eq, inArray } from "drizzle-orm";
import { redirect } from "next/navigation";
import type { SessionUser } from "./auth";
import { db } from "./db";
import { classSettings, classes, enrollments } from "./db/schema";
import { daMaHoa, giaiMa, maHoa } from "./ma-hoa";

export async function lopGvDay(gvId: string): Promise<string[]> {
  const rows = await db
    .select({ classId: enrollments.classId })
    .from(enrollments)
    .where(and(eq(enrollments.userId, gvId), eq(enrollments.roleInClass, "GV")));
  return rows.map((r) => r.classId);
}

export async function lopCuaHs(hsId: string): Promise<string | null> {
  const rows = await db
    .select({ classId: enrollments.classId })
    .from(enrollments)
    .where(and(eq(enrollments.userId, hsId), eq(enrollments.roleInClass, "HS")))
    .limit(1);
  return rows[0]?.classId ?? null;
}

/** id HS thuộc các lớp GV này dạy (rỗng nếu GV không dạy lớp nào). */
export async function hsCuaGv(gvId: string): Promise<string[]> {
  const lops = await lopGvDay(gvId);
  if (!lops.length) return [];
  const rows = await db
    .select({ userId: enrollments.userId })
    .from(enrollments)
    .where(and(inArray(enrollments.classId, lops), eq(enrollments.roleInClass, "HS")));
  return [...new Set(rows.map((r) => r.userId))];
}

export async function gvDayHs(gvId: string, hsId: string) {
  return (await hsCuaGv(gvId)).includes(hsId);
}

/** Lớp đang thao tác của GV: lớp được chọn (phải là lớp GV dạy) hoặc lớp đầu tiên GV dạy. */
export async function lopDangChon(gv: SessionUser, classId?: string | null) {
  const lops = await lopGvDay(gv.id);
  if (!lops.length) return null;
  const id = classId && lops.includes(classId) ? classId : lops[0];
  const lop = (await db.select().from(classes).where(eq(classes.id, id)).limit(1))[0] ?? null;
  return lop;
}

/** Ném/redirect nếu người dùng không có vai trò này trong lớp này. */
export async function requireClassRole(user: SessionUser, classId: string, role: "GV" | "HS") {
  const row = await db
    .select()
    .from(enrollments)
    .where(and(eq(enrollments.classId, classId), eq(enrollments.userId, user.id), eq(enrollments.roleInClass, role)))
    .limit(1);
  if (!row[0]) redirect(role === "GV" ? "/gv" : "/hs");
  return row[0];
}

/** Cài đặt lớp; ai_api_key trả về ĐÃ GIẢI MÃ (F-10). Khoá cũ còn lưu rõ được mã hoá lại ngay khi đọc. */
export async function caiDatLop(classId: string | null) {
  if (!classId) return null;
  const row = (await db.select().from(classSettings).where(eq(classSettings.classId, classId)).limit(1))[0] ?? null;
  if (!row) return null;
  const raw = row.aiApiKey;
  if (raw && !daMaHoa(raw)) {
    try {
      await db.update(classSettings).set({ aiApiKey: maHoa(raw) }).where(eq(classSettings.classId, classId));
    } catch {
      console.error("caiDatLop: chưa mã hoá lại được khoá cũ (thiếu APP_ENC_KEY?)");
    }
  }
  return { ...row, aiApiKey: giaiMa(raw) };
}

export async function caiDatLopCuaGv(gv: SessionUser, classId?: string | null) {
  const lop = await lopDangChon(gv, classId);
  return { lop, setting: await caiDatLop(lop?.id ?? null) };
}

export async function caiDatLopCuaHs(hsId: string) {
  return caiDatLop(await lopCuaHs(hsId));
}
