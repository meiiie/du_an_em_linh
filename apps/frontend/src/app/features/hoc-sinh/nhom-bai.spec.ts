import { nhomBai, locTrangThai } from './nhom-bai';
import { BaiCuaHocSinh } from '../../api/hoc-sinh';
const bai = (maBai: string, trangThai: BaiCuaHocSinh['trangThai'], ketQua: BaiCuaHocSinh['ketQua'] = null): BaiCuaHocSinh => ({maBai, trangThai, ketQua, kyNang:'HS', tenKyNang:'Hàm số', deBai:'Đề', deBaiLatex:'x', muc4:'NHAN_BIET', han:null, soBuoc:5, soBuocDat:0});
describe('Nhóm bài học sinh', () => {
  it('dùng tên bộ khi có, thiếu thì nhóm theo chủ đề và đếm đạt từ kết quả máy chủ', () => {
    const ds = [bai('1','DA_NOP','DAT'), bai('2','CHUA_LAM'), {...bai('3','DA_NOP','SAI'), tenBo:'Ôn tuần 1'}];
    expect(nhomBai(ds).map(n=>({ten:n.ten, dat:n.dat, tong:n.hang.length}))).toEqual([{ten:'Hàm số',dat:1,tong:2},{ten:'Ôn tuần 1',dat:0,tong:1}]);
  });
  it('lọc chưa đạt không gom bài chưa làm hay bài đang chờ thầy cô', () => {
    const ds=[bai('1','CHUA_LAM'),bai('2','DANG_LAM'),bai('3','DA_NOP','SAI'),bai('4','DA_NOP','KHONG_KIEM_DUOC'),bai('5','DA_NOP','DAT')];
    expect(ds.filter(b=>locTrangThai(b,'chua-dat')).map(b=>b.maBai)).toEqual(['3']);
    expect(ds.filter(b=>locTrangThai(b,'chua-lam')).map(b=>b.maBai)).toEqual(['1']);
    expect(ds.filter(b=>locTrangThai(b,'dat')).map(b=>b.maBai)).toEqual(['5']);
  });
});
