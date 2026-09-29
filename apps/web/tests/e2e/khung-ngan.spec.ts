import { expect, test } from "@playwright/test";
import postgres from "postgres";
import { vaoLop } from "./vao-lop";

/** Bài khung ngắn (Sư phạm, UXT-07-l): trang luyện mở ở bước bắt đầu; bước trước là dữ kiện đề cho, không bấm vào làm. */
const url = process.env.DATABASE_URL || "postgres://hoc_toan:hoc_toan@127.0.0.1:5432/hoc_toan";

test("8 bài khung ngắn đã phát hành; TH mở ở bước nghiệm, NB mở ở bước xét dấu", async ({ page }) => {
  const sql = postgres(url, { max: 1 });
  try {
    const rows = await sql<{ code: string; id: string; status: string; buoc_bat_dau: string }[]>`
      select code, id, status, buoc_bat_dau from problems where origin = 'SUPHAM_KHUNG_NGAN' order by code`;
    expect(rows.map((r) => r.code)).toEqual([
      "DH12-03-NB-01",
      "DH12-03-NB-02",
      "DH12-03-TH-01",
      "DH12-03-TH-02",
      "DH12-05-NB-01",
      "DH12-05-NB-02",
      "DH12-05-TH-01",
      "DH12-05-TH-02",
    ]);
    expect(rows.every((r) => r.status === "DA_PHAT_HANH")).toBe(true);
    const th = rows.find((r) => r.code === "DH12-03-TH-02")!;
    const nb = rows.find((r) => r.code === "DH12-05-NB-01")!;
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.chi@demo.local", "hocsinh123");
    await page.waitForURL(/\/hs/);

    await page.goto(`/hs/luyen/${th.id}`);
    await expect(page.getByTestId("solve-screen")).toBeVisible();
    await expect(page.getByTestId("step-B.DH.NGHIEM")).toHaveAttribute("aria-current", "step");
    for (const ma of ["B.DH.TXD", "B.DH.DAOHAM"]) await expect(page.getByTestId(`step-${ma}`)).toHaveAttribute("data-de-cho", "1");
    await expect(page.getByTestId("de-cho-san")).toBeVisible();
    await page.getByTestId("step-B.DH.TXD").click({ force: true });
    await expect(page.getByTestId("step-B.DH.NGHIEM")).toHaveAttribute("aria-current", "step");
    await expect(page.getByTestId("latex-txd")).toHaveCount(0);

    await page.goto(`/hs/luyen/${nb.id}`);
    await expect(page.getByTestId("step-B.DH.XETDAU")).toHaveAttribute("aria-current", "step");
    await expect(page.getByTestId("step-B.DH.NGHIEM")).toHaveAttribute("data-de-cho", "1");
  } finally {
    await sql.end();
  }
});
