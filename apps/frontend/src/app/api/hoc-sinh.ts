// Kiểu cho API học sinh theo `specs/001-lat-cat-doc/contracts/api-core.md` (P2). Core chưa có các endpoint này: khi DTO
// bên core thành hình thì đối chiếu lại ở đây. Trường hợp đồng chưa định nghĩa hình dạng để `unknown`, không đoán.

/** Kết quả chấm một bước. `KHONG_CHAM_DUOC`: dịch vụ toán lỗi, không bao giờ coi là đạt. */
export type KetQuaCham = 'DAT' | 'SAI' | 'KHONG_CHAM_DUOC';

/** Bốn mức cho học sinh (ADR v0, «Không mở lại»). */
export type Muc4 = string;

export type LyDoBaiKe = 'CHUA_LOI' | 'CUNG_CO' | 'NANG_1_NAC' | 'DE_HON' | 'THAY_CO_GIAO';

/** `GET /api/hs/trang-hoc`. */
export interface TrangHoc {
  readonly ten: string;
  readonly viecHomNay: readonly unknown[];
  readonly baiKe: { readonly maBai: string; readonly tieuDe: string; readonly lyDo: LyDoBaiKe; readonly kyNang: string; readonly muc: Muc4 };
  readonly soBaiGiao: readonly unknown[];
  /** Hợp đồng ghi «kẹt»; tên trường JSON là `ket`. */
  readonly soKyNang: readonly { readonly kyNang: string; readonly muc4: Muc4; readonly ket: boolean }[];
  readonly hoanThanh: { readonly kyNang: readonly string[]; readonly chuDe: unknown };
}

/** Một dòng của `GET /api/hs/bai`: bài được giao hoặc đã phát hành cho lớp. */
export interface BaiCuaHocSinh {
  readonly maBai: string;
  readonly tieuDe: string;
  readonly muc4: Muc4;
  readonly han: string | null;
  readonly trangThai: string;
}

/** `GET /api/hs/bai/{maBai}`. Không có lời giải, dữ kiện bảo vệ hay đáp án cuối khi bài đang làm (FR-006). */
export interface ChiTietBai {
  readonly de: { readonly text: string; readonly latex: string };
  readonly cacBuoc: readonly { readonly maBuoc: string; readonly moTa: string; readonly dangNhap: string }[];
  readonly baiLam: {
    readonly cacBuoc: readonly {
      readonly maBuoc: string;
      readonly dong: readonly string[];
      readonly latex: string;
      readonly ketQua: KetQuaCham;
      readonly thongBao: string;
      readonly oSai: readonly unknown[];
    }[];
    readonly trangThai: string;
  };
  readonly coTheMoLoiGiai: boolean;
}

/** Thân `POST /api/hs/bai/{maBai}/buoc`. */
export interface NopBuoc {
  readonly maBuoc: string;
  readonly dong: readonly string[];
  readonly bang?: readonly { readonly hang: number; readonly k: number; readonly giaTri: string }[];
}

/** Phản hồi chấm một bước: không có giá trị đúng. */
export interface KetQuaBuoc {
  readonly ketQua: KetQuaCham;
  readonly thongBao: string;
  readonly oSai: readonly unknown[];
  readonly maLoi?: string;
  readonly buocKe?: string;
}

/** `POST /api/hs/bai/{maBai}/nop`. `loiGiai` chỉ có khi lớp bật cờ mở lời giải sau khi nộp. */
export interface KetQuaNop {
  readonly ketQua: KetQuaCham;
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
