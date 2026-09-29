/**
 * F-08 (RLS): chạy truy vấn trong một giao dịch có app.user_id = người dùng hiện tại, để chính sách
 * RLS ở drizzle/0010 lọc submissions / grading_results / tutor_sessions / tutor_messages / mastery_states.
 * Ngoài helper này (app.user_id chưa đặt) chính sách cho qua: phân quyền tầng ứng dụng (lib/lop.ts) vẫn là lớp chính.
 */
import type postgres from "postgres";
import { sql } from "./db";

export async function trongPhienCua<T>(userId: string, fn: (tx: postgres.TransactionSql) => Promise<T>): Promise<T> {
  return (await sql.begin(async (tx) => {
    await tx`select set_config('app.user_id', ${userId}, true)`;
    return fn(tx);
  })) as T;
}
