import { CANH_BAO_MAU } from "@/lib/canh-bao-mau";
import { cheDoTest } from "@/lib/che-do-test";
import { sql } from "@/lib/db";

export const dynamic = "force-dynamic";

/**
 * CHỈ DÙNG CHO TEST UI (Playwright): đặt lại bài làm / gia sư / mức của học sinh demo.
 * Bật khi APP_ENV=test (hoặc NODE_ENV=test) VÀ không chạy trên Render. Bản host không bao giờ bật (trả 404).
 * Body JSON tuỳ chọn: { "email": "hs.an@..." } để chỉ đặt lại một học sinh; bỏ trống = mọi học sinh (vai HS).
 */
export async function POST(req: Request) {
  if (!cheDoTest()) return new Response("Not found", { status: 404 });
  let email: string | null = null;
  try {
    const body = (await req.json()) as { email?: string };
    email = body.email ? String(body.email).trim().toLowerCase() : null;
  } catch {
    email = null;
  }
  const hs = email
    ? await sql<{ id: string }[]>`select u.id from users u join user_roles r on r.user_id = u.id where r.role_code = 'HS' and lower(u.email) = ${email}`
    : await sql<{ id: string }[]>`select u.id from users u join user_roles r on r.user_id = u.id where r.role_code = 'HS'`;
  const ids = hs.map((h) => h.id);
  if (!ids.length) return Response.json({ ok: true, so_hs: 0 });
  await sql.begin(async (tx) => {
    await tx`delete from tutor_messages where session_id in (select id from tutor_sessions where student_id = any(${ids}::uuid[]))`;
    await tx`delete from tutor_sessions where student_id = any(${ids}::uuid[])`;
    const subRows = await tx<{ id: string }[]>`select id from submissions where student_id = any(${ids}::uuid[])`;
    const subs = subRows.map((r) => r.id);
    await tx`delete from grading_results where submission_id = any(${subs}::uuid[])`;
    await tx`delete from submission_table_cells where table_id in (select id from submission_tables where submission_id = any(${subs}::uuid[]))`;
    await tx`delete from submission_tables where submission_id = any(${subs}::uuid[])`;
    await tx`delete from submission_steps where submission_id = any(${subs}::uuid[])`;
    await tx`delete from input_events where submission_id = any(${subs}::uuid[])`;
    await tx`delete from mastery_events where student_id = any(${ids}::uuid[])`;
    await tx`delete from submissions where student_id = any(${ids}::uuid[])`;
    await tx`delete from escalations where student_id = any(${ids}::uuid[])`;
    // Trả cảnh báo mẫu của seed (vd 'Chi — Điểm tới hạn') cho học sinh vừa đặt lại, như lúc mới nạp dữ liệu.
    for (const m of CANH_BAO_MAU) {
      await tx`insert into escalations (id, student_id, skill_code, reason, loai)
        select gen_random_uuid(), u.id, ${m.skillCode}, ${m.reason}, 'KET' from users u
        where lower(u.email) = ${m.email} and u.id = any(${ids}::uuid[])`;
    }
    await tx`update mastery_states set stuck_counter = 0, last_error_codes = '[]'::jsonb where student_id = any(${ids}::uuid[])`;
    // F-10: xoá bộ đếm hạn mức (gia sư, nộp bước) của các HS này và mọi khoá đăng nhập đang tính
    await tx`delete from rate_limit_events where khoa = any(${ids.flatMap((i) => [`gia_su:${i}`, `nop_buoc:${i}`])}::text[]) or khoa like 'dang_nhap:%'`;
  });
  return Response.json({ ok: true, so_hs: ids.length });
}
