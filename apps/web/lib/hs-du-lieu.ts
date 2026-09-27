import { cache } from "react";
import { and, desc, eq } from "drizzle-orm";
import { db } from "./db";
import { assignments, classes, enrollments, gradingResults, submissions } from "./db/schema";
import { BUOC } from "./levels";

export const layTenLopHs = cache(async (userId: string) => {
  const en = await db.select().from(enrollments).where(eq(enrollments.userId, userId)).limit(1);
  if (!en[0]) return null;
  const lop = await db.select().from(classes).where(eq(classes.id, en[0].classId)).limit(1);
  return lop[0]?.name ?? null;
});

export const layIdBaiDaDat = cache(async (studentId: string) => {
  const rows = await db
    .select({ problemId: submissions.problemId })
    .from(submissions)
    .where(and(eq(submissions.studentId, studentId), eq(submissions.status, "da_cham"), eq(submissions.ketQua, "DAT")));
  return new Set(rows.map((r) => r.problemId));
});

export const layBaiGiao = cache(async (studentId: string) => {
  return db.select().from(assignments).where(eq(assignments.studentId, studentId));
});

export const demBaiChuaXong = cache(async (studentId: string) => {
  const giao = await layBaiGiao(studentId);
  const dat = await layIdBaiDaDat(studentId);
  return giao.filter((a) => !dat.has(a.problemId)).length;
});

export type TrangThaiBuoc = {
  done: string[];
  current: string | null;
  finished: boolean;
};

export async function trangThaiPhieu(studentId: string, problemId: string): Promise<TrangThaiBuoc> {
  const sub = await db
    .select()
    .from(submissions)
    .where(and(eq(submissions.studentId, studentId), eq(submissions.problemId, problemId)))
    .orderBy(desc(submissions.submittedAt))
    .limit(1);
  if (!sub[0]) return { done: [], current: BUOC[0].ma, finished: false };
  const finished = sub[0].status === "da_cham" && sub[0].ketQua === "DAT";
  if (finished) return { done: BUOC.map((b) => b.ma), current: null, finished: true };
  const g = await db.select().from(gradingResults).where(eq(gradingResults.submissionId, sub[0].id)).limit(1);
  const per = (g[0]?.perBuoc || {}) as Record<string, string>;
  const done = BUOC.map((b) => b.ma).filter((ma) => per[ma] === "DAT");
  const current = BUOC.map((b) => b.ma).find((ma) => per[ma] !== "DAT") ?? null;
  return { done, current, finished: false };
}
