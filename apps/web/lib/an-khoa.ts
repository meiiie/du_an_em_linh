/** Che chuỗi giống khóa API — không in, không lưu mặt phiếu. */

export const HINH_KHOA = /(?:sk-|zai-|or-v1-)[A-Za-z0-9_\-]{16,}/gi;

export function anKhoa(text: string) {
  return (text || "").replace(HINH_KHOA, "[khoa]");
}
