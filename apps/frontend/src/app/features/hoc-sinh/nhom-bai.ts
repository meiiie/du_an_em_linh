import { BaiCuaHocSinh } from '../../api/hoc-sinh';

export interface NhomBai {
  id: string;
  ten: string;
  dat: number;
  hang: { bai: BaiCuaHocSinh; so: number }[];
}

/** Giữ số bài gốc khi tìm kiếm/lọc; tiến độ lấy từ kết quả máy chủ. */
export function nhomBai(ds: readonly BaiCuaHocSinh[]): NhomBai[] {
  const nhom = new Map<string, NhomBai>();
  ds.forEach((bai, i) => {
    const tenBo = bai.tenBo?.trim() || bai.title?.trim();
    const id = tenBo ? 'bo:' + (bai.maBo || tenBo) : 'chu-de:' + bai.kyNang;
    let n = nhom.get(id);
    if (!n) {
      n = { id, ten: tenBo || bai.tenKyNang || 'Bài được giao', dat: 0, hang: [] };
      nhom.set(id, n);
    }
    n.hang.push({ bai, so: i + 1 });
    if (bai.ketQua === 'DAT') n.dat++;
  });
  return [...nhom.values()];
}

export function locTrangThai(b: BaiCuaHocSinh, loc: string): boolean {
  switch (loc) {
    case 'chua-lam': return b.trangThai === 'CHUA_LAM';
    case 'chua-dat': return b.trangThai === 'DA_NOP' && b.ketQua === 'SAI';
    case 'dat': return b.ketQua === 'DAT';
    default: return true;
  }
}
