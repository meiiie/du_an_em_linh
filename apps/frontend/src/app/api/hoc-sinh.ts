// Kiểu cho API học sinh theo `specs/001-lat-cat-doc/contracts/api-core.md` (T021, PR core «API học sinh»). Đổi DTO bên
// core thì sửa ở đây. Không trường nào cho lời giải hay đáp án (FR-006).

/** Kết quả chấm một bước. `KHONG_CHAM_DUOC`: dịch vụ toán lỗi, không bao giờ coi là đạt. */
export type KetQuaCham = 'DAT' | 'SAI' | 'KHONG_KIEM_DUOC' | 'KHONG_CHAM_DUOC';

/** Bốn mức cho học sinh (ADR v0, «Không mở lại»). */
export type Muc4 = 'NHAN_BIET' | 'THONG_HIEU' | 'VAN_DUNG' | 'VAN_DUNG_CAO';

export const TEN_MUC: Record<Muc4, string> = {
  NHAN_BIET: 'Nhận biết',
  THONG_HIEU: 'Thông hiểu',
  VAN_DUNG: 'Vận dụng',
  VAN_DUNG_CAO: 'Vận dụng cao',
};

export type TrangThaiBai = 'CHUA_LAM' | 'DANG_LAM' | 'DA_NOP';

export type LyDoBaiKe = 'CHUA_LOI' | 'CUNG_CO' | 'NANG_1_NAC' | 'DE_HON' | 'THAY_CO_GIAO';

/** Một dòng của `GET /api/hs/bai`: bài được giao cho em, đang phát hành ở lớp. */
export interface BaiCuaHocSinh {
  readonly maBai: string;
  readonly deBai: string;
  readonly deBaiLatex: string;
  /** Mã kỹ năng (vd `T12.DH.03`); màn hiện `tenKyNang`. */
  readonly kyNang: string;
  readonly tenKyNang: string;
  readonly muc4: Muc4;
  readonly han: string | null;
  readonly trangThai: TrangThaiBai;
  readonly soBuocDat: number;
  readonly soBuoc: number;
  /** Chỉ khi đã nộp. */
  readonly ketQua: Exclude<KetQuaCham, 'KHONG_CHAM_DUOC'> | null;
}

export interface ViTriSai {
  readonly maBuoc: string;
  readonly dong?: number;
  readonly hang?: string;
  readonly k?: number;
}

export interface DongBaiLam {
  readonly dong: number;
  readonly latex: string;
}

export interface OBang {
  readonly hang: string;
  readonly k: number;
  readonly giaTri: string;
}

/** `GET /api/hs/bai/{maBai}`. */
export interface ChiTietBai {
  readonly maBai: string;
  readonly de: { readonly text: string; readonly latex: string };
  readonly kyNang: string;
  readonly tenKyNang: string;
  readonly muc4: Muc4;
  readonly dangTraLoi: string;
  readonly buocBatDau: string | null;
  /** Các ô của bước kết luận em phải khai (core suy từ đề như v0 SP-03), vd `dong_bien`, `cuc_dai`. Không chứa đáp án. */
  readonly khaiBaoKetLuan: readonly string[];
  readonly cacBuoc: readonly { readonly maBuoc: string; readonly ten: string; readonly viec: string }[];
  readonly baiLam: {
    readonly trangThai: TrangThaiBai;
    readonly cacBuoc: readonly {
      readonly maBuoc: string;
      readonly dong: readonly DongBaiLam[];
      readonly bang: readonly OBang[];
      /** Lần chấm mới nhất cho nội dung hiện tại của bước; sửa bước sau lần chấm thì null. */
      readonly ketQua: KetQuaCham | null;
      readonly thongBao: string | null;
      readonly oSai: readonly ViTriSai[];
    }[];
  };
  readonly coTheMoLoiGiai: boolean;
}

/** Thân `POST /api/hs/bai/{maBai}/buoc`. `suKien`: sự kiện nhập mới kể từ lần nộp trước, tối đa 500. */
export interface NopBuoc {
  readonly maBuoc: string;
  readonly dong?: readonly { readonly dong: number; readonly latex: string; readonly loai?: string }[];
  readonly bang?: readonly OBang[];
  readonly suKien?: readonly {
    readonly maBuoc: string;
    readonly hang?: string;
    readonly k?: number;
    readonly giaTriCu?: string;
    readonly giaTriMoi: string;
    readonly luc: string;
  }[];
}

/** Phản hồi chấm một bước: không có giá trị đúng. */
export interface KetQuaBuoc {
  readonly ketQua: KetQuaCham;
  readonly thongBao: string;
  readonly oSai: readonly ViTriSai[];
  readonly maLoi?: string;
  readonly buocKe?: string;
}

/** `POST /api/hs/bai/{maBai}/nop`. `loiGiai` chỉ có khi lớp bật cờ mở lời giải sau khi nộp. */
export interface KetQuaNop {
  readonly ketQua: Exclude<KetQuaCham, 'KHONG_CHAM_DUOC'>;
  readonly mucHieu: readonly { readonly kyNang: string; readonly muc4Truoc: Muc4; readonly muc4Sau: Muc4 }[];
  readonly loiGiai?: unknown;
}

export type ChipGiaSu = 'GOI_Y' | 'SAI_CHO' | 'GUI_THAY_CO';

/** Thân `POST /api/hs/gia-su` (SSE). */
export interface HoiGiaSu {
  readonly maBai: string;
  readonly cauHoi?: string;
  readonly chip?: ChipGiaSu;
}

export interface TrichDan {
  readonly n: number;
  readonly loai: string;
  readonly id: string;
  readonly doan: string;
}

/** Sự kiện SSE của gia sư (ADR 010): chỉ trạng thái, rồi một câu đã lọc; không gửi từng phần câu. */
export type SuKienGiaSu =
  | { readonly loai: 'trang_thai'; readonly buoc: 'kho' | 'goi' | 'loc' }
  | {
      readonly loai: 'xong';
      readonly noiDung: string;
      readonly trichDan: readonly TrichDan[];
      readonly cheDo: 'thang_goi_y' | 'mo_hinh' | 'tu_choi';
      readonly nhan: string;
    }
  | { readonly loai: 'loi'; readonly thongBao: string };

/** Một lượt trong `GET /api/hs/gia-su/{maBai}`. */
export interface LuotGiaSu {
  readonly vaiTro: string;
  readonly noiDung: string;
  readonly trichDan: readonly TrichDan[];
  readonly nhan: string;
  readonly luc: string;
}

/** `GET /api/hs/lich`. */
export interface LichHoc {
  readonly tuan: readonly { readonly thu: string; readonly gio: string; readonly viec: string }[];
  readonly loiKhuyen: string;
  readonly nhacHomNay: readonly unknown[];
}

/** `GET /api/hs/kho`: đích của trích dẫn `[n]` là `#ct-<id>`, `#tl-<id>`. */
export interface KhoLop {
  readonly congThuc: readonly { readonly id: string; readonly tieuDe: string; readonly latex: string; readonly trichDan: unknown }[];
  readonly taiLieu: readonly { readonly id: string; readonly tieuDe: string; readonly doan: readonly unknown[] }[];
}

export const API_HS = {
  trangHoc: '/api/hs/trang-hoc',
  bai: '/api/hs/bai',
  chiTietBai: (maBai: string) => `/api/hs/bai/${encodeURIComponent(maBai)}`,
  nopBuoc: (maBai: string) => `/api/hs/bai/${encodeURIComponent(maBai)}/buoc`,
  nopBai: (maBai: string) => `/api/hs/bai/${encodeURIComponent(maBai)}/nop`,
  giaSu: '/api/hs/gia-su',
  lichSuGiaSu: (maBai: string) => `/api/hs/gia-su/${encodeURIComponent(maBai)}`,
  lich: '/api/hs/lich',
  kho: '/api/hs/kho',
} as const;
