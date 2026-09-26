export type GradeResult = {
  ket_qua: "DAT" | "SAI" | "KHONG_KIEM_DUOC";
  loai_ket_qua: string;
  buoc_sai: { ma_buoc: string; dong: number | null; o: { hang: string; k: number | null } | null } | null;
  ma_loi: string | null;
  do_tin_cay: number | null;
  per_buoc: Record<string, string>;
  thong_bao: string;
  chua_xong?: boolean;
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

export async function mathJob<T = Record<string, unknown>>(kind: string, payload: unknown): Promise<T> {
  const base = process.env.MATH_SERVICE_URL || "http://127.0.0.1:8000";
  const res = await fetch(base + PATHS[kind], {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Dịch vụ toán lỗi ${res.status}: ${text.slice(0, 300)}`);
  }
  return (await res.json()) as T;
}
