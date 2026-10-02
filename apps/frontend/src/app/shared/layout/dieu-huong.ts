import { TenBieuTuong } from '../ui/bieu-tuong';

export type KhuVuc = 'HS' | 'GV';

export interface MucDieuHuong {
  readonly duongDan: string;
  readonly nhan: string;
  readonly testId: string;
  readonly bieuTuong: TenBieuTuong;
}

/**
 * Ray điều hướng: nhãn, thứ tự, `data-testid` và biểu tượng chép từ v0 (`apps/web/lib/nav.ts`), đối chiếu bảng phụ lục
 * của `specs/001-lat-cat-doc/spec.md`. «Tạo đề» (`/gv/sinh-bai`) thuộc P3 nên chưa có trên ray.
 */
export const DIEU_HUONG: Record<KhuVuc, readonly MucDieuHuong[]> = {
  HS: [
    { duongDan: '/hs', nhan: 'Học', testId: 'nav-hs-lo-trinh', bieuTuong: 'home' },
    { duongDan: '/hs/bai', nhan: 'Đề bài', testId: 'nav-hs-bai', bieuTuong: 'book' },
    { duongDan: '/hs/lich', nhan: 'Lịch', testId: 'nav-hs-lich', bieuTuong: 'calendar' },
    { duongDan: '/hs/kho', nhan: 'Công thức', testId: 'nav-hs-kho', bieuTuong: 'kho' },
  ],
  GV: [
    { duongDan: '/gv', nhan: 'Lớp', testId: 'nav-gv-tong-quan', bieuTuong: 'home' },
    { duongDan: '/gv/duyet', nhan: 'Duyệt', testId: 'nav-gv-duyet', bieuTuong: 'inbox' },
    { duongDan: '/gv/ngan-hang', nhan: 'Đề bài', testId: 'nav-gv-ngan-hang', bieuTuong: 'bank' },
    { duongDan: '/gv/tai-lieu', nhan: 'Tài liệu', testId: 'nav-gv-tai-lieu', bieuTuong: 'file' },
    { duongDan: '/gv/cong-thuc', nhan: 'Công thức', testId: 'nav-gv-cong-thuc', bieuTuong: 'sigma' },
    { duongDan: '/gv/tien-do', nhan: 'Mức', testId: 'nav-gv-tien-do', bieuTuong: 'chart' },
    { duongDan: '/gv/ket-noi-ai', nhan: 'Gia sư', testId: 'nav-gv-ket-noi-ai', bieuTuong: 'plug' },
    { duongDan: '/gv/cai-dat', nhan: 'Cài lớp', testId: 'nav-gv-cai-dat', bieuTuong: 'settings' },
  ],
};

export const TEN_KHU_VUC: Record<KhuVuc, string> = { HS: 'Học sinh', GV: 'Giáo viên' };

/** Trang chủ của khu vực chỉ sáng khi đứng đúng trang đó, mục khác sáng cả ở trang con (như `navActive` của v0). */
export function laTrangChuKhuVuc(duongDan: string): boolean {
  return duongDan === '/hs' || duongDan === '/gv';
}
