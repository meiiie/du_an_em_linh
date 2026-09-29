/**
 * UXT-05-c: mô tả từng vấn đề của một lần nộp cho màn giáo viên (đủ danh sách, không thu gọn).
 * Chỉ dùng tên bước, tên ô, loại lỗi. Không có giá trị đáp án.
 */
const TEN_BUOC: Record<string, string> = {
  "B.DH.TXD": "Tập xác định",
  "B.DH.DAOHAM": "Đạo hàm",
  "B.DH.NGHIEM": "Nghiệm y′",
  "B.DH.XETDAU": "Bảng xét dấu",
  "B.DH.KETLUAN": "Kết luận",
};

const TEN_LOI: Record<string, string> = {
  SAI_TXD: "tập xác định sai",
  DIEM_THIEU: "thiếu điểm cần đặt mốc",
  DIEM_THUA: "có mốc không cần đặt",
  SAI_THU_TU_MOC: "mốc chưa theo thứ tự tăng dần",
  SAI_DAU: "dấu của y′ sai",
  SAI_GIA_TRI: "giá trị sai",
  SAI_BIEN_DOI: "biến đổi hoặc mũi tên sai",
  DAU_DOI_TRONG_KHOANG: "y′ đổi dấu trong khoảng (hệ quả)",
  SAI_KET_LUAN: "kết luận sai",
  KHONG_KIEM_DUOC: "máy chưa kiểm được",
};

/** Ô kết luận theo chỉ số dòng: thứ tự gửi của màn làm bài (Đồng biến, Nghịch biến, Cực đại, Cực tiểu). */
const O_KET_LUAN = ["Đồng biến", "Nghịch biến", "Cực đại", "Cực tiểu"];

export type VanDeLuu = {
  id?: string;
  loai_ket_qua?: string;
  ma_loi?: string | null;
  nguyen_nhan?: string;
  buoc_sai?: { ma_buoc?: string; dong?: number | null; o?: { hang?: string; k?: number | null } | null } | null;
};

export function tenBuocGv(ma: string | undefined): string {
  return (ma && TEN_BUOC[ma]) || ma || "—";
}

export function viTriVanDe(v: VanDeLuu): string | null {
  const bs = v.buoc_sai || {};
  const o = bs.o;
  if (o?.hang === "X") return o.k == null ? "hàng x" : `hàng x, mốc thứ ${o.k + 1}`;
  if (o?.hang === "DAU_YPHAY") return o.k == null ? "hàng dấu y′" : `hàng dấu y′, ô ${o.k % 2 === 0 ? "khoảng" : "tại mốc"} thứ ${Math.floor(o.k / 2) + 1}`;
  if (o?.hang === "BIEN_THIEN") return o.k == null ? "hàng biến thiên" : `hàng biến thiên, khoảng thứ ${Math.floor(o.k / 2) + 1}`;
  if (bs.dong != null) {
    if (bs.ma_buoc === "B.DH.KETLUAN" && O_KET_LUAN[bs.dong]) return `ô ${O_KET_LUAN[bs.dong]}`;
    return `dòng ${bs.dong + 1}`;
  }
  return null;
}

export function moTaVanDe(v: VanDeLuu): { buoc: string; viTri: string | null; loai: string; heQua: boolean; maLoi: string | null } {
  return {
    buoc: tenBuocGv(v.buoc_sai?.ma_buoc),
    viTri: viTriVanDe(v),
    loai: (v.loai_ket_qua && TEN_LOI[v.loai_ket_qua]) || v.loai_ket_qua || "",
    heQua: Boolean(v.nguyen_nhan),
    maLoi: v.ma_loi ?? null,
  };
}

export function docVanDe(raw: unknown): VanDeLuu[] {
  return Array.isArray(raw) ? (raw.filter((x) => x && typeof x === "object") as VanDeLuu[]) : [];
}
