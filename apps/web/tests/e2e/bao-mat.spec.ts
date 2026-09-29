import { expect, test } from "@playwright/test";
import postgres from "postgres";
import { vaoLop } from "./vao-lop";

/** F-05 / F-10: kiểm trên app thật + DB test. Mỗi test tự dọn trạng thái nó đổi. */
const url = process.env.DATABASE_URL || "postgres://hoc_toan:hoc_toan@127.0.0.1:5432/hoc_toan";
const LOP_A = "SELECT class_id FROM enrollments e JOIN users u ON u.id = e.user_id WHERE u.email = 'gv@demo.local' AND e.role_in_class = 'GV' LIMIT 1";

test.describe("bảo mật F-05 / F-10", () => {
  let sql: postgres.Sql;
  test.beforeAll(async () => {
    sql = postgres(url, { max: 1 });
  });
  test.afterAll(async () => {
    await sql`delete from rate_limit_events where khoa like 'dang_nhap:%'`;
    await sql.end();
  });

  test("F-05: lời giải không có trong HTML lẫn RSC khi HS chưa xong bài (lớp bật mở lời giải)", async ({ page, request }) => {
    const lop = (await sql.unsafe(LOP_A))[0].class_id as string;
    const cu = (await sql`select mo_loi_giai_sau_khi_nop from class_settings where class_id = ${lop}`)[0].mo_loi_giai_sau_khi_nop;
    await request.post("/api/test/reset", { data: { email: "hs.binh@demo.local" } }).catch(() => null);
    await sql`update class_settings set mo_loi_giai_sau_khi_nop = true where class_id = ${lop}`;
    try {
      const p = (
        await sql`select p.id, s.bai_lam from problems p join solutions s on s.problem_id = p.id
          where p.status = 'DA_PHAT_HANH' and p.ham_sympy = 'x**3 - 6*x**2 + 9*x + 2' limit 1`
      )[0];
      const an = (await sql`select id from users where email = 'hs.binh@demo.local'`)[0].id;
      const xong = await sql`select 1 from submissions where student_id = ${an} and problem_id = ${p.id} and status = 'da_cham'`;
      test.skip(xong.length > 0, "HS Bình đã xong bài này (không chạy được khi thiếu /api/test/reset)");
      const daoHam = (p.bai_lam as { dao_ham: string }).dao_ham;
      await page.goto("/dang-nhap");
      await vaoLop(page, "hs.binh@demo.local", "hocsinh123");
      await page.waitForURL(/\/hs/);
      const html = await (await page.request.get(`/hs/luyen/${p.id}`)).text();
      const rsc = await (await page.request.get(`/hs/luyen/${p.id}`, { headers: { RSC: "1" } })).text();
      expect(html).toContain("tutor-input");
      for (const body of [html, rsc]) {
        expect(body).not.toContain(daoHam);
        expect(body).not.toContain("Cực đại tại x = 1");
        expect(body).not.toContain("Đạo hàm:");
      }
    } finally {
      await sql`update class_settings set mo_loi_giai_sau_khi_nop = ${cu} where class_id = ${lop}`;
    }
  });

  test("F-10: khoá API cũ lưu rõ được mã hoá AES-GCM khi đọc, trang không lộ khoá", async ({ page }) => {
    const lop = (await sql.unsafe(LOP_A))[0].class_id as string;
    const ro = "khoa lop thu e2e khong duoc luu ro 123456";
    await sql`update class_settings set ai_api_key = ${ro} where class_id = ${lop}`;
    try {
      await page.goto("/dang-nhap");
      await vaoLop(page, "gv@demo.local", "giaovien123");
      await page.waitForURL(/\/gv/);
      const html = await (await page.request.get("/gv/ket-noi-ai")).text();
      expect(html).not.toContain(ro);
      const luu = (await sql`select ai_api_key from class_settings where class_id = ${lop}`)[0].ai_api_key as string;
      expect(luu.startsWith("enc:v1:")).toBe(true);
      expect(luu).not.toContain(ro);
    } finally {
      await sql`update class_settings set ai_api_key = null where class_id = ${lop}`;
    }
  });

  test("F-10: sai mật khẩu 5 lần thì lần 6 (dù đúng) bị tạm khoá", async ({ page }) => {
    await sql`delete from rate_limit_events where khoa like 'dang_nhap:hs.chi@demo.local|%'`;
    for (let i = 0; i < 5; i++) {
      await page.goto("/dang-nhap");
      await vaoLop(page, "hs.chi@demo.local", "sai-mat-khau");
      await expect(page).toHaveURL(/loi=1/);
    }
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.chi@demo.local", "hocsinh123");
    await expect(page).toHaveURL(/loi=khoa/);
    await expect(page.getByTestId("khoa-dang-nhap")).toBeVisible();
    await sql`delete from rate_limit_events where khoa like 'dang_nhap:hs.chi@demo.local|%'`;
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.chi@demo.local", "hocsinh123");
    await page.waitForURL(/\/hs/);
  });

  test("F-10: gia sư từ chối câu > 1000 ký tự và khi hết hạn mức", async ({ page }) => {
    const binh = (await sql`select id from users where email = 'hs.binh@demo.local'`)[0].id as string;
    const pid = (await sql`select id from problems where status = 'DA_PHAT_HANH' and ham_sympy is not null limit 1`)[0].id as string;
    await sql`delete from rate_limit_events where khoa = ${"gia_su:" + binh}`;
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.binh@demo.local", "hocsinh123");
    await page.waitForURL(/\/hs/);
    try {
      const dai = await (await page.request.post("/api/hs/gia-su", { data: { problemId: pid, text: "a".repeat(1001) } })).text();
      expect(dai).toContain("qua_dai");
      await sql`insert into rate_limit_events (khoa) select ${"gia_su:" + binh} from generate_series(1, 30)`;
      const het = await (await page.request.post("/api/hs/gia-su", { data: { problemId: pid, text: "Em cần gợi ý" } })).text();
      expect(het).toContain("het_han_muc");
    } finally {
      await sql`delete from rate_limit_events where khoa = ${"gia_su:" + binh}`;
    }
  });

  test("F-08 RLS: vai không phải superuser chỉ thấy dữ liệu học của chính HS (hoặc HS lớp mình dạy)", async () => {
    const id = async (email: string) => (await sql`select id from users where email = ${email}`)[0].id as string;
    const [an, binh, gv] = [await id("hs.an@demo.local"), await id("hs.binh@demo.local"), await id("gv@demo.local")];
    await sql.unsafe(`DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'hoc_toan_rls_thu') THEN CREATE ROLE hoc_toan_rls_thu NOLOGIN; END IF; END $$`);
    await sql.unsafe("GRANT SELECT ON submissions, grading_results, tutor_sessions, tutor_messages, mastery_states, enrollments TO hoc_toan_rls_thu");
    const soCua = async (nguoi: string | null, hs: string) =>
      (await sql.begin(async (tx) => {
        if (nguoi) await tx`select set_config('app.user_id', ${nguoi}, true)`;
        await tx.unsafe("SET LOCAL ROLE hoc_toan_rls_thu");
        const r = await tx`select count(*)::int as n from mastery_states where student_id = ${hs}`;
        return r[0].n as number;
      })) as unknown as number;
    const tong = (await sql`select count(*)::int as n from mastery_states where student_id = ${an}`)[0].n as number;
    expect(tong).toBeGreaterThan(0);
    expect(await soCua(an, an)).toBe(tong);
    expect(await soCua(binh, an)).toBe(0);
    expect(await soCua(gv, an)).toBe(tong);
    expect(await soCua(null, an)).toBe(tong);
  });
});
