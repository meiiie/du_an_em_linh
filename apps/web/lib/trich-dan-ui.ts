import type { TrichDanHien } from "./kien-thuc";

/** Chữ trong `[n](url)` — chỉ nhận 1–2 chữ số, không nhận «Đạo hàm». */
export function soTuChuThe(node: unknown): number | null {
  if (typeof node === "number" && node >= 1 && node <= 99 && Number.isInteger(node)) return node;
  if (typeof node === "string") {
    const t = node.trim();
    if (/^\d{1,2}$/.test(t)) return Number(t);
    return null;
  }
  if (Array.isArray(node) && node.length === 1) return soTuChuThe(node[0]);
  return null;
}

export function tenMoKho(loai: TrichDanHien["loai"]) {
  return loai === "tai_lieu" ? "Mở tài liệu" : "Mở công thức";
}

/** Một đoạn đang xem — không đổ cả tập. */
export function chonTrichHien(items: TrichDanHien[], moSo?: number | null) {
  if (!items.length) return null;
  return items.find((t) => t.so != null && t.so === moSo) || items.find((t) => t.dung) || items[0] || null;
}
