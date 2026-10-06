import { ChiTietBai, DongBaiLam } from '../../../api/hoc-sinh';
import { NhapPhieu, nhapTuBaiLam, oKetLuan, yeuCauNop } from './phieu';

const KHAI_BAO = ['dong_bien', 'nghich_bien', 'cuc_dai', 'cuc_tieu'];

const NHAP: NhapPhieu = {
  txd: '\\mathbb{R}\\setminus\\{-3\\}',
  dh: ['\\frac{3}{(x+3)^2}', ''],
  nghiem: [
    { latex: '', loai: 'NGHIEM' },
    { latex: '-3', loai: 'KHONG_XD' },
  ],
  moc: ['-3'],
  dau: { 0: '+', 1: '||', 2: '+' },
  mui: { 0: 'TANG', 2: 'TANG' },
  kl: { dong_bien: '(-\\infty;-3) và (-3;+\\infty)', nghich_bien: ' ', cuc_dai: '' },
};

describe('yeuCauNop', () => {
  it('mỗi bước gửi đúng thân như v0: dòng trống bị bỏ, điểm không xác định đổi thành câu kèm loai', () => {
    expect(yeuCauNop('B.DH.TXD', NHAP, KHAI_BAO)).toEqual({ maBuoc: 'B.DH.TXD', dong: [{ dong: 0, latex: '\\mathbb{R}\\setminus\\{-3\\}' }] });
    expect(yeuCauNop('B.DH.DAOHAM', NHAP, KHAI_BAO)).toEqual({
      maBuoc: 'B.DH.DAOHAM',
      dong: [{ dong: 0, latex: '\\frac{3}{(x+3)^2}' }],
    });
    expect(yeuCauNop('B.DH.NGHIEM', NHAP, KHAI_BAO)).toEqual({
      maBuoc: 'B.DH.NGHIEM',
      dong: [{ dong: 0, latex: "y' không xác định tại -3", loai: 'KHONG_XD' }],
    });
  });

  it('không có nghiệm nào: gửi câu «không có nghiệm»; đạo hàm để trống vẫn gửi một dòng rỗng để máy chủ báo', () => {
    const rong: NhapPhieu = { ...NHAP, dh: [''], nghiem: [{ latex: ' ', loai: 'NGHIEM' }] };
    expect(yeuCauNop('B.DH.NGHIEM', rong, KHAI_BAO).dong).toEqual([{ dong: 0, latex: 'không có nghiệm', loai: 'NGHIEM' }]);
    expect(yeuCauNop('B.DH.DAOHAM', rong, KHAI_BAO).dong).toEqual([{ dong: 0, latex: '' }]);
  });

  it('bảng xét dấu: mốc theo thứ tự em nhập, mọi ô khoảng (kể cả trống), ô tại mốc và mũi tên chỉ khi đã chọn', () => {
    expect(yeuCauNop('B.DH.XETDAU', { ...NHAP, dau: { 0: '+', 2: '+' }, mui: { 0: 'TANG' } }, KHAI_BAO)).toEqual({
      maBuoc: 'B.DH.XETDAU',
      bang: [
        { hang: 'X', k: 0, giaTri: '-3' },
        { hang: 'DAU_YPHAY', k: 0, giaTri: '+' },
        { hang: 'DAU_YPHAY', k: 2, giaTri: '+' },
        { hang: 'BIEN_THIEN', k: 0, giaTri: 'TANG' },
      ],
    });
    expect(yeuCauNop('B.DH.XETDAU', { ...NHAP, moc: [], dau: {}, mui: {} }, KHAI_BAO).bang).toEqual([
      { hang: 'DAU_YPHAY', k: 0, giaTri: '' },
    ]);
  });

  it('kết luận: một dòng mỗi ô khai báo, kèm loai; ô trống gửi «không có …» cho cực trị, chuỗi rỗng cho khoảng', () => {
    expect(yeuCauNop('B.DH.KETLUAN', NHAP, KHAI_BAO)).toEqual({
      maBuoc: 'B.DH.KETLUAN',
      dong: [
        { dong: 0, latex: '(-\\infty;-3) và (-3;+\\infty)', loai: 'DONG_BIEN' },
        { dong: 1, latex: '', loai: 'NGHICH_BIEN' },
        { dong: 2, latex: 'không có cực đại', loai: 'CUC_DAI' },
        { dong: 3, latex: 'không có cực tiểu', loai: 'CUC_TIEU' },
      ],
    });
  });
});

describe('nhapTuBaiLam', () => {
  function bai(cacBuoc: ChiTietBai['baiLam']['cacBuoc'], khaiBao = KHAI_BAO): ChiTietBai {
    return {
      maBai: 'GEN-huu_ti-5',
      de: { text: 'Tìm các khoảng đơn điệu và cực trị.', latex: 'y = \\frac{x}{x + 3}' },
      kyNang: 'T12.DH.03',
      tenKyNang: 'Tính đơn điệu của hàm số',
      muc4: 'VAN_DUNG',
      dangTraLoi: 'TU_LUAN_5_BUOC',
      buocBatDau: null,
      khaiBaoKetLuan: khaiBao,
      cacBuoc: [],
      baiLam: { trangThai: 'DANG_LAM', cacBuoc },
      coTheMoLoiGiai: false,
    };
  }
  const buoc = (maBuoc: string, dong: DongBaiLam[] = [], bang: ChiTietBai['baiLam']['cacBuoc'][number]['bang'] = []) => ({
    maBuoc,
    dong,
    bang,
    ketQua: null,
    thongBao: null,
    oSai: [],
  });

  it('chưa làm gì: một ô trống cho đạo hàm và nghiệm, bảng không mốc', () => {
    expect(nhapTuBaiLam(bai([]))).toEqual({
      txd: '',
      dh: [''],
      nghiem: [{ latex: '', loai: 'NGHIEM' }],
      moc: [],
      dau: {},
      mui: {},
      kl: {},
    });
  });

  it('vào lại bài: dựng lại theo `loai` đã lưu, kể cả điểm không xác định và ô «không có» để trống', () => {
    const daGui = bai([
      buoc('B.DH.TXD', [{ dong: 0, latex: '\\mathbb{R}\\setminus\\{-3\\}' }]),
      buoc('B.DH.DAOHAM', [
        { dong: 1, latex: '=\\frac{3}{(x+3)^2}' },
        { dong: 0, latex: '\\frac{(x+3)-x}{(x+3)^2}' },
      ]),
      buoc('B.DH.NGHIEM', [{ dong: 0, latex: "y' không xác định tại -3", loai: 'KHONG_XD' }]),
      buoc('B.DH.XETDAU', [], [
        { hang: 'BIEN_THIEN', k: 2, giaTri: 'TANG' },
        { hang: 'X', k: 0, giaTri: '-3' },
        { hang: 'DAU_YPHAY', k: 1, giaTri: '||' },
      ]),
      buoc('B.DH.KETLUAN', [
        { dong: 0, latex: '(-\\infty;-3)', loai: 'DONG_BIEN' },
        { dong: 1, latex: '', loai: 'NGHICH_BIEN' },
        { dong: 2, latex: 'không có cực đại', loai: 'CUC_DAI' },
        { dong: 3, latex: 'x = 0', loai: 'CUC_TIEU' },
      ]),
    ]);
    expect(nhapTuBaiLam(daGui)).toEqual({
      txd: '\\mathbb{R}\\setminus\\{-3\\}',
      dh: ['\\frac{(x+3)-x}{(x+3)^2}', '=\\frac{3}{(x+3)^2}'],
      nghiem: [{ latex: '-3', loai: 'KHONG_XD' }],
      moc: ['-3'],
      dau: { 1: '||' },
      mui: { 2: 'TANG' },
      kl: { dong_bien: '(-\\infty;-3)', nghich_bien: '', cuc_dai: '', cuc_tieu: 'x = 0' },
    });
  });

  it('gửi rồi đọc lại cho cùng thân yêu cầu (kết luận và nghiệm đi trọn một vòng)', () => {
    const daGui = bai([
      buoc('B.DH.NGHIEM', [...yeuCauNop('B.DH.NGHIEM', NHAP, KHAI_BAO).dong!]),
      buoc('B.DH.KETLUAN', [...yeuCauNop('B.DH.KETLUAN', NHAP, KHAI_BAO).dong!]),
    ]);
    const n = nhapTuBaiLam(daGui);
    expect(yeuCauNop('B.DH.NGHIEM', n, KHAI_BAO)).toEqual(yeuCauNop('B.DH.NGHIEM', NHAP, KHAI_BAO));
    expect(yeuCauNop('B.DH.KETLUAN', n, KHAI_BAO)).toEqual(yeuCauNop('B.DH.KETLUAN', NHAP, KHAI_BAO));
  });
});

describe('oKetLuan', () => {
  it('chỉ các ô core khai báo, theo thứ tự đó, với testid của v0', () => {
    expect(oKetLuan(['dong_bien', 'nghich_bien']).map((o) => [o.ma, o.loai, o.testId, o.congThuc])).toEqual([
      ['dong_bien', 'DONG_BIEN', 'latex-db', false],
      ['nghich_bien', 'NGHICH_BIEN', 'latex-nb', false],
    ]);
  });
});
