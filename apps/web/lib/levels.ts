export const MUC4 = ["NHAN_BIET", "THONG_HIEU", "VAN_DUNG", "VAN_DUNG_CAO"] as const;
export type Muc4 = (typeof MUC4)[number];

export const LABEL4: Record<Muc4, string> = {
  NHAN_BIET: "Nhận biết",
  THONG_HIEU: "Thông hiểu",
  VAN_DUNG: "Vận dụng",
  VAN_DUNG_CAO: "Vận dụng cao",
};

/** Suy ra lúc hiển thị theo CV 7991: Vận dụng và Vận dụng cao gộp vào Vận dụng. */
export const TO3: Record<Muc4, "BIET" | "HIEU" | "VAN_DUNG"> = {
  NHAN_BIET: "BIET",
  THONG_HIEU: "HIEU",
  VAN_DUNG: "VAN_DUNG",
  VAN_DUNG_CAO: "VAN_DUNG",
};

export const LABEL3 = {
  BIET: "Biết",
  HIEU: "Hiểu",
  VAN_DUNG: "Vận dụng",
} as const;

export function labelBloom(code: string | null | undefined) {
  if (!code) return "—";
  const en: Record<string, string> = {
    REMEMBER: "Nhận biết",
    UNDERSTAND: "Thông hiểu",
    APPLY: "Vận dụng",
    ANALYZE: "Phân tích",
    EVALUATE: "Đánh giá",
    CREATE: "Sáng tạo",
  };
  if (en[code]) return en[code];
  if (MUC4.includes(code as Muc4)) return LABEL4[code as Muc4];
  return code;
}

export function labelMuc(code: string, view3: boolean) {
  if (!MUC4.includes(code as Muc4)) return code;
  if (!view3) return LABEL4[code as Muc4];
  return LABEL3[TO3[code as Muc4]];
}

export function nextNotch(code: string): Muc4 {
  const i = MUC4.indexOf(code as Muc4);
  if (i < 0) return "NHAN_BIET";
  return MUC4[Math.min(i + 1, MUC4.length - 1)];
}

export function mucFromMastery(p: number, thresholds: { THONG_HIEU: number; VAN_DUNG: number; VAN_DUNG_CAO: number }): Muc4 {
  if (p >= thresholds.VAN_DUNG_CAO) return "VAN_DUNG_CAO";
  if (p >= thresholds.VAN_DUNG) return "VAN_DUNG";
  if (p >= thresholds.THONG_HIEU) return "THONG_HIEU";
  return "NHAN_BIET";
}

export const BUOC = [
  { ma: "B.DH.TXD", ten: "Tập xác định", dang: "DONG" },
  { ma: "B.DH.DAOHAM", ten: "Đạo hàm", dang: "DONG" },
  { ma: "B.DH.NGHIEM", ten: "Nghiệm y′", dang: "DONG" },
  { ma: "B.DH.XETDAU", ten: "Bảng xét dấu", dang: "BANG" },
  { ma: "B.DH.KETLUAN", ten: "Kết luận", dang: "DONG" },
] as const;

export const STATUS_LABEL: Record<string, string> = {
  DA_PHAT_HANH: "Đã mở",
  CHO_GIAO_VIEN_DUYET: "Chờ duyệt",
  BI_CHAN: "Bị chặn",
  NHAP: "Nháp",
  GV_DUYET: "Đã duyệt",
  DAT: "Đạt",
  SAI: "Sai",
  KHONG_KIEM_DUOC: "Không kiểm được",
};
