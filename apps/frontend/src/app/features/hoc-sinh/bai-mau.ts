import { Injectable } from '@angular/core';

export type Muc4 = 'NHAN_BIET' | 'THONG_HIEU' | 'VAN_DUNG' | 'VAN_DUNG_CAO';

export const TEN_MUC: Record<Muc4, string> = {
  NHAN_BIET: 'Nhận biết',
  THONG_HIEU: 'Thông hiểu',
  VAN_DUNG: 'Vận dụng',
  VAN_DUNG_CAO: 'Vận dụng cao',
};

/** Năm bước của phiếu, tên như v0 (`apps/web/lib/de-hoc-sinh.ts` TEN_TRANG). */
export const NAM_BUOC = ['Tập xác định', 'Đạo hàm', 'Nghiệm y′', 'Xét dấu', 'Kết luận'];

/** Bài làm của học sinh: chưa làm, đang làm (số bước đã đạt) hay đạt cả năm bước. */
export type TrangThaiBai = { loai: 'chua-lam' } | { loai: 'dang-lam'; buocDat: number } | { loai: 'dat' };

export interface BaiGiao {
  readonly ma: string;
  readonly cauHoi: string;
  /** LaTeX của hàm số, vẽ bằng KaTeX trên bảng. */
  readonly ham: string;
  readonly muc: Muc4;
  readonly kyNang: string;
  readonly trangThai: TrangThaiBai;
}

export interface MucKyNang {
  readonly ten: string;
  readonly muc: Muc4;
}

/**
 * Bài mẫu để xem trước giao diện học sinh khi core chưa có API học sinh (T021, chờ ADR 015 #138). Đề lấy nguyên văn từ
 * ngân hàng bài v0 (`specs/001-lat-cat-doc/doi-chieu/v0-bai.json`, dữ liệu tổng hợp); trạng thái và mức là giả định để xem
 * đủ các kiểu hàng. Màn hiện dòng «bài mẫu» khi `laMau` là true. Có API thì thay lớp này, các màn giữ nguyên.
 */
@Injectable({ providedIn: 'root' })
export class BaiHocSinh {
  readonly laMau = true;

  readonly bai: readonly BaiGiao[] = [
    {
      ma: 'GEN-bac_ba-11',
      cauHoi: 'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số.',
      ham: 'y = x^{3} - 6x^{2} + 1',
      muc: 'VAN_DUNG',
      kyNang: 'Đơn điệu và cực trị',
      trangThai: { loai: 'dang-lam', buocDat: 3 },
    },
    {
      ma: 'GEN-trung_phuong-7',
      cauHoi: 'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số.',
      ham: 'y = x^{4} - 8x^{2} - 1',
      muc: 'VAN_DUNG',
      kyNang: 'Cực trị hàm trùng phương',
      trangThai: { loai: 'chua-lam' },
    },
    {
      ma: 'GEN-huu_ti-5',
      cauHoi: 'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số.',
      ham: 'y = \\dfrac{x}{x + 3}',
      muc: 'VAN_DUNG',
      kyNang: 'Đơn điệu hàm phân thức',
      trangThai: { loai: 'chua-lam' },
    },
    {
      ma: 'DH12-03-NB-01',
      cauHoi: 'Dựa vào dấu của y′, tìm các khoảng đồng biến, nghịch biến của hàm số.',
      ham: "y = x^{3} - 3x^{2} - 9x + 2,\\quad y' = 3(x + 1)(x - 3)",
      muc: 'NHAN_BIET',
      kyNang: 'Đọc dấu đạo hàm',
      trangThai: { loai: 'dat' },
    },
    {
      ma: 'DH12-03-TH-01',
      cauHoi: 'Không cần tính lại đạo hàm, tìm các điểm tới hạn và xét dấu y′.',
      ham: "y = x^{3} + 3x^{2} - 24x + 1,\\quad y' = 3x^{2} + 6x - 24",
      muc: 'THONG_HIEU',
      kyNang: 'Nghiệm của y′',
      trangThai: { loai: 'dat' },
    },
    {
      ma: 'DH12-06-VDC-01',
      cauHoi: 'Tìm giá trị của tham số m để hàm số đạt cực đại tại x = 1.',
      ham: 'y = x^{3} - 3mx^{2} + 3(m^{2} - 1)x',
      muc: 'VAN_DUNG_CAO',
      kyNang: 'Cực trị có tham số',
      trangThai: { loai: 'chua-lam' },
    },
  ];

  readonly mucKyNang: readonly MucKyNang[] = [
    { ten: 'Đọc dấu đạo hàm', muc: 'THONG_HIEU' },
    { ten: 'Nghiệm của y′', muc: 'THONG_HIEU' },
    { ten: 'Đơn điệu và cực trị', muc: 'NHAN_BIET' },
    { ten: 'Cực trị có tham số', muc: 'NHAN_BIET' },
  ];

  /** Bài kế tiếp: bài đang làm dở, không có thì bài chưa làm đầu tiên. */
  baiKeTiep(): BaiGiao | undefined {
    return this.bai.find((b) => b.trangThai.loai === 'dang-lam') ?? this.bai.find((b) => b.trangThai.loai === 'chua-lam');
  }
}
