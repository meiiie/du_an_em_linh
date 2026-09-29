import { expect, test } from "@playwright/test";
import postgres from "postgres";
import { vaoLop } from "./vao-lop";

/**
 * F-08: phân quyền theo lớp. Dựng lớp thứ hai (GV B, HS Dũng) thẳng vào DB test,
 * kiểm GV mỗi lớp chỉ thấy HS và cài đặt của lớp mình, rồi dọn sạch.
 */
const LOP_B = "0b000000-0000-4000-8000-00000000000b";
const GV_B = "0b000000-0000-4000-8000-0000000000a1";
const HS_B = "0b000000-0000-4000-8000-0000000000a2";

const url = process.env.DATABASE_URL || "postgres://hoc_toan:hoc_toan@127.0.0.1:5432/hoc_toan";

async function donLopB(sql: postgres.Sql) {
  const ids = [GV_B, HS_B];
  await sql`delete from sessions where user_id = any(${ids}::uuid[])`;
  await sql`delete from study_schedules where student_id = any(${ids}::uuid[])`;
  await sql`delete from escalations where student_id = any(${ids}::uuid[])`;
  await sql`delete from mastery_states where student_id = any(${ids}::uuid[])`;
  await sql`delete from audit_logs where actor_user_id = any(${ids}::uuid[])`;
  await sql`delete from consent_records where subject_user_id = any(${ids}::uuid[])`;
  await sql`delete from class_settings where class_id = ${LOP_B}`;
  await sql`delete from enrollments where class_id = ${LOP_B}`;
  await sql`delete from classes where id = ${LOP_B}`;
  await sql`delete from user_roles where user_id = any(${ids}::uuid[])`;
  await sql`delete from users where id = any(${ids}::uuid[])`;
}

test.describe("phân quyền theo lớp (F-08)", () => {
  let sql: postgres.Sql;

  test.beforeAll(async () => {
    sql = postgres(url, { max: 1 });
    await donLopB(sql);
    await sql`insert into users (id, email, password_hash, display_name, birth_year, status, is_synthetic, pseudonym_id)
      select ${GV_B}, 'gv.b@demo.local', password_hash, 'Giáo viên B', null, 'active', true, 'ps-gv-b' from users where email = 'gv@demo.local'`;
    await sql`insert into users (id, email, password_hash, display_name, birth_year, status, is_synthetic, pseudonym_id)
      select ${HS_B}, 'hs.dung@demo.local', password_hash, 'Dũng Lớp B', 2008, 'active', true, 'ps-dung' from users where email = 'hs.an@demo.local'`;
    await sql`insert into user_roles (user_id, role_code) values (${GV_B}, 'GV'), (${HS_B}, 'HS')`;
    await sql`insert into classes (id, name, grade, year) values (${LOP_B}, '12B thử', 12, 2026)`;
    await sql`insert into enrollments (class_id, user_id, role_in_class) values (${LOP_B}, ${GV_B}, 'GV'), (${LOP_B}, ${HS_B}, 'HS')`;
    await sql`insert into class_settings (class_id, mo_loi_giai_sau_khi_nop, ai_provider, ai_allow_local) values (${LOP_B}, true, 'offline', true)`;
  });

  test.afterAll(async () => {
    await donLopB(sql);
    await sql.end();
  });

  test("GV lớp B chỉ thấy HS và cài đặt lớp B", async ({ page }) => {
    await page.goto("/dang-nhap");
    await vaoLop(page, "gv.b@demo.local", "giaovien123");
    await page.waitForURL(/\/gv/);
    await page.goto("/gv/tien-do");
    const tienDo = page.getByTestId("tien-do");
    await expect(tienDo).toContainText("Dũng Lớp B");
    for (const ten of ["An", "Bình", "Chi"]) {
      await expect(tienDo.getByText(ten, { exact: true })).toHaveCount(0);
    }
    await page.goto("/gv/cai-dat");
    await expect(page.getByTestId("mo-loi-giai")).toBeChecked();
  });

  test("GV lớp A không thấy HS lớp B, cài đặt lớp A không đổi", async ({ page }) => {
    await page.goto("/dang-nhap");
    await vaoLop(page, "gv@demo.local", "giaovien123");
    await page.waitForURL(/\/gv/);
    await page.goto("/gv/tien-do");
    const tienDo = page.getByTestId("tien-do");
    for (const ten of ["An", "Bình", "Chi"]) {
      await expect(tienDo.getByText(ten, { exact: true })).not.toHaveCount(0);
    }
    await expect(tienDo).not.toContainText("Dũng Lớp B");
    await page.goto("/gv/cai-dat");
    await expect(page.getByTestId("mo-loi-giai")).not.toBeChecked();
  });

  test("HS lớp B vào được trang học, không lỗi khi lớp chưa giao bài", async ({ page }) => {
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.dung@demo.local", "hocsinh123");
    await page.waitForURL(/\/hs/);
    await expect(page.locator("body")).not.toContainText("Application error");
  });
});
