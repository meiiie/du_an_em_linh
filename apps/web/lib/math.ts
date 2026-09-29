export type O = { hang: string; k: number | null };
export type BuocSai = { ma_buoc: string; dong: number | null; o: O | null };
/** Một vấn đề trong danh sách đủ (gốc theo thứ tự bước, rồi ô hệ quả). k, dong đếm từ 0; hàng HOA. */
export type VanDe = {
  id: string;
  loai_ket_qua: string;
  buoc_sai: BuocSai;
  nguyen_nhan?: string;
  ma_loi?: string | null;
  do_tin_cay?: number | null;
  ky_nang?: string;
  so_diem_thieu?: number;
  /** ERR.DH.24: dòng nghiệm (0-based) chứa mốc thừa (tô dòng đó, không tính thêm vấn đề). */
  dong_lien_quan?: number | null;
};

export type GradeResult = {
  ket_qua: "DAT" | "SAI" | "KHONG_KIEM_DUOC";
  loai_ket_qua: string;
  cac_van_de?: VanDe[];
  buoc_sai: { ma_buoc: string; dong: number | null; o: { hang: string; k: number | null } | null } | null;
  ma_loi: string | null;
  do_tin_cay: number | null;
  per_buoc: Record<string, string>;
  thong_bao: string;
  chua_xong?: boolean;
  /** Luật dấu U (0002c): true = kết luận đúng về toán, chỉ sai trình bày (U/∪); false = sai toán; thiếu = không áp dụng. */
  toan_dung?: boolean;
  phien_ban_chuan_hoa?: string;
  chuan_hoa?: { ma_buoc: string; dong: number; trang_thai_chuan_hoa: string; chuoi_chuan_hoa?: string }[];
};

const PATHS: Record<string, string> = {
  grade: "/v1/grade",
  verify: "/v1/verify",
  filter: "/v1/filter",
  generate: "/v1/generate",
  solve: "/v1/solve",
  extract_pdf: "/v1/extract",
};

/** Trần thời gian mỗi lời gọi dịch vụ toán (ms). Hết giờ -> ném lỗi; nơi gọi bộ lọc coi là CHẶN (fail closed). */
// Bộ lọc lộ đáp án: quá hạn LOC_TIMEOUT_MS (mặc định 2000 ms, chốt 11:37) thì hủy và CHẶN câu (fail closed ở gia-su-luot).
const LOC_TIMEOUT_MS = Number(process.env.LOC_TIMEOUT_MS) > 0 ? Number(process.env.LOC_TIMEOUT_MS) : 2000;
const HET_GIO: Record<string, number> = { filter: LOC_TIMEOUT_MS, grade: 25000, verify: 30000, generate: 30000, solve: 30000 };

export async function mathJob<T = Record<string, unknown>>(kind: string, payload: unknown, timeoutMs?: number): Promise<T> {
  const base = process.env.MATH_SERVICE_URL || "http://127.0.0.1:8000";
  const res = await fetch(base + PATHS[kind], {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
    cache: "no-store",
    signal: AbortSignal.timeout(timeoutMs ?? HET_GIO[kind] ?? 30000),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Dịch vụ toán lỗi ${res.status}: ${text.slice(0, 300)}`);
  }
  return (await res.json()) as T;
}
