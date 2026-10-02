// Kiểu cho API giáo viên theo `specs/001-lat-cat-doc/contracts/api-core.md` (P2). Core chưa có các endpoint này: khi DTO
// bên core thành hình thì đối chiếu lại ở đây. Trường hợp đồng chưa định nghĩa hình dạng để `unknown`, không đoán.
import { Muc4 } from './hoc-sinh';

/** Trạng thái của cổng 3 tầng, giữ như v0. */
export type TrangThaiCong = 'DAT' | 'SAI' | 'KHONG_KIEM_DUOC' | 'GV_DUYET';

/** `GET /api/gv/lop`. */
export interface LopCuaGiaoVien {
  readonly tenLop: string;
  readonly siSo: number;
  readonly canhBaoKet: readonly { readonly hocSinh: string; readonly kyNang: string; readonly loai: 'KET' | 'NHO_GV' }[];
  readonly sanSangAi: { readonly nha: string; readonly congThucDaKhoa: boolean };
}

/** Một mục của hàng đợi `GET /api/gv/duyet`. Mục cũ chỉ có thao tác `KIEM_LAI`. */
export interface MucDuyet {
  readonly runId: string;
  readonly loai: 'BAI' | 'CONG_THUC_GIA_SU';
  readonly ma: string;
  readonly trangThai: 'SAI' | 'KHONG_KIEM_DUOC';
  readonly canCu: readonly { readonly tang: number; readonly trangThai: TrangThaiCong; readonly lyDo: string; readonly trichDan?: unknown }[];
  readonly cu: boolean;
  readonly thaoTac: 'DUYET' | 'KIEM_LAI' | 'SUA_BAI' | 'THEM_VAO_BANG';
}

/** Phản hồi `POST /api/gv/duyet/{runId}` (thân `{ ghiChu }`, bắt buộc). */
export interface KetQuaDuyet {
  readonly trangThai: 'GV_DUYET';
  readonly nguoiDuyet: string;
  readonly luc: string;
}

/** Một dòng của `GET /api/gv/ngan-hang`. */
export interface BaiTrongNganHang {
  readonly maBai: string;
  readonly muc4: Muc4;
  readonly muc3: string;
  readonly kyNang: string;
  readonly trangThai: string;
  readonly cu: boolean;
}

/** Thân `POST /api/gv/giao-bai`; thiếu `hocSinh` là giao cả lớp. */
export interface GiaoBai {
  readonly maBai: string;
  readonly hocSinh?: readonly string[];
  readonly han?: string;
}

/** Một dòng của `GET /api/gv/tai-lieu`. */
export interface TaiLieuCuaLop {
  readonly id: string;
  readonly tieuDe: string;
  readonly quyenDung: string;
  readonly soDoan: number;
  readonly phienBan: number;
}

/** `GET /api/gv/cong-thuc`. Khóa bảng (`POST /api/gv/cong-thuc/khoa`) trả 422 nếu dòng nào chưa qua (ADR 013). */
export interface BangCongThuc {
  readonly phienBan: number;
  readonly trangThai: 'NHAP' | 'KHOA';
  readonly cacDong: readonly {
    readonly id: string;
    readonly tieuDe: string;
    readonly latex: string;
    readonly phatBieu: string;
    readonly tang1: unknown;
    readonly trichDan: unknown;
  }[];
}

/** `GET` / `PUT /api/gv/cai-dat`. `nhaAi` phải nằm trong `cacNhaDuocBat`; khóa nhà do máy chủ quản lý (FR-019). */
export interface CaiDatLop {
  readonly moLoiGiaiSauKhiNop: boolean;
  readonly nhaAi: string;
  readonly choPhepMayCucBo: boolean;
  readonly cacNhaDuocBat: readonly string[];
}

/** Thân `PUT /api/gv/hoc-sinh/{id}/muc/{kyNang}`: ghi đè mức, bắt buộc lý do. */
export interface GhiDeMuc {
  readonly muc4: Muc4;
  readonly lyDo: string;
}

/** Thân `POST /api/gv/hoc-sinh/{id}/bai-ke`: bài kế chọn tay (bài đã phát hành). */
export interface ChonBaiKe {
  readonly maBai: string;
  readonly lyDo: string;
}

const ma = (s: string) => encodeURIComponent(s);

export const API_GV = {
  lop: '/api/gv/lop',
  duyet: '/api/gv/duyet',
  duyetMuc: (runId: string) => `/api/gv/duyet/${ma(runId)}`,
  nganHang: '/api/gv/ngan-hang',
  kiemNganHang: '/api/gv/ngan-hang/kiem',
  giaoBai: '/api/gv/giao-bai',
  taiLieu: '/api/gv/tai-lieu',
  congThuc: '/api/gv/cong-thuc',
  khoaCongThuc: '/api/gv/cong-thuc/khoa',
  tienDo: (muc: 3 | 4) => `/api/gv/tien-do?muc=${muc}`,
  hocSinh: (id: string) => `/api/gv/hoc-sinh/${ma(id)}`,
  mucHocSinh: (id: string, kyNang: string) => `/api/gv/hoc-sinh/${ma(id)}/muc/${ma(kyNang)}`,
  baiKe: (id: string) => `/api/gv/hoc-sinh/${ma(id)}/bai-ke`,
  caiDat: '/api/gv/cai-dat',
  giaSu: '/api/gv/gia-su',
} as const;
