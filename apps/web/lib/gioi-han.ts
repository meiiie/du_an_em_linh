/**
 * F-10: giới hạn tần suất lưu trong DB (bảng rate_limit_events).
 * - Đăng nhập: 5 lần sai / 15 phút theo email+IP -> khoá tới khi cửa sổ trôi qua; đăng nhập đúng xoá bộ đếm.
 * - Gia sư: câu hỏi ≤ 1000 ký tự; 30 câu / 10 phút và 300 câu / ngày mỗi học sinh.
 * - Nộp bước: 40 lần / phút mỗi học sinh.
 */
import { sql } from "./db";

export const GIOI_HAN = {
  dangNhap: { soLan: 5, giay: 15 * 60 },
  giaSuNgan: { soLan: 30, giay: 10 * 60 },
  giaSuNgay: { soLan: 300, giay: 24 * 60 * 60 },
  nopBuoc: { soLan: 40, giay: 60 },
  doDaiCauHoi: 1000,
} as const;

export type KhoGioiHan = {
  dem(khoa: string, giay: number): Promise<number>;
  ghi(khoa: string): Promise<void>;
  xoa(khoa: string): Promise<void>;
};

export const khoDb: KhoGioiHan = {
  async dem(khoa, giay) {
    const r = await sql<{ n: number }[]>`
      select count(*)::int as n from rate_limit_events
      where khoa = ${khoa} and tao_luc > now() - make_interval(secs => ${giay})`;
    return r[0]?.n ?? 0;
  },
  async ghi(khoa) {
    await sql`insert into rate_limit_events (khoa) values (${khoa})`;
    // dọn bản ghi cũ hơn 1 ngày của khoá này để bảng không phình
    await sql`delete from rate_limit_events where khoa = ${khoa} and tao_luc < now() - interval '1 day 1 hour'`;
  },
  async xoa(khoa) {
    await sql`delete from rate_limit_events where khoa = ${khoa}`;
  },
};

/** true nếu đã chạm/vượt giới hạn trong cửa sổ (không ghi thêm). */
export async function daChamGioiHan(kho: KhoGioiHan, khoa: string, gh: { soLan: number; giay: number }) {
  return (await kho.dem(khoa, gh.giay)) >= gh.soLan;
}

/** Kiểm rồi ghi một lượt nếu còn hạn mức. Trả false nếu bị chặn (không ghi). */
export async function dungMotLuot(kho: KhoGioiHan, khoa: string, ...ghs: { soLan: number; giay: number }[]) {
  for (const gh of ghs) if (await daChamGioiHan(kho, khoa, gh)) return false;
  await kho.ghi(khoa);
  return true;
}

export function khoaDangNhap(email: string, ip: string) {
  return `dang_nhap:${email.trim().toLowerCase()}|${ip || "-"}`;
}

export function ipTuHeader(h: { get(name: string): string | null }) {
  const xff = h.get("x-forwarded-for");
  if (xff) return xff.split(",")[0].trim();
  return h.get("x-real-ip")?.trim() || "-";
}

/** Kho trong bộ nhớ cho test đơn vị. */
export function khoBoNho(now: () => number = Date.now): KhoGioiHan {
  const m = new Map<string, number[]>();
  return {
    async dem(khoa, giay) {
      const t = now() - giay * 1000;
      return (m.get(khoa) || []).filter((x) => x > t).length;
    },
    async ghi(khoa) {
      m.set(khoa, [...(m.get(khoa) || []), now()]);
    },
    async xoa(khoa) {
      m.delete(khoa);
    },
  };
}
