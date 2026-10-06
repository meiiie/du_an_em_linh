import { ChiTietBai, NopBuoc, OBang } from '../../../api/hoc-sinh';

/** Mã năm bước của khung «Đơn điệu và cực trị» (ADR v0, «Không mở lại»). */
export const TXD = 'B.DH.TXD';
export const DAO_HAM = 'B.DH.DAOHAM';
export const NGHIEM = 'B.DH.NGHIEM';
export const XET_DAU = 'B.DH.XETDAU';
export const KET_LUAN = 'B.DH.KETLUAN';

/** Hàng của bảng xét dấu như v0 (`docs/chi-so-o-bang.md`): mốc theo chỉ số, ô dấu và ô mũi tên theo `k` (chẵn: khoảng). */
export const HANG_X = 'X';
export const HANG_DAU = 'DAU_YPHAY';
export const HANG_MUI = 'BIEN_THIEN';

export type LoaiNghiem = 'NGHIEM' | 'KHONG_XD';
export type Mui = 'TANG' | 'GIAM';

/** Một ô kết luận (khai báo do core suy từ đề, SP-03): nhãn chấm `loai`, chữ trên màn, testid như v0. */
export interface OKetLuan {
  readonly ma: string;
  readonly loai: string;
  readonly nhan: string;
  readonly testId: string;
  readonly congThuc: boolean;
  /** Để trống khi nộp thì gửi câu này (v0 UXT-02-b), để bộ chấm và giáo viên đọc được. */
  readonly trongThi?: string;
}

const KHAI_BAO: Record<string, Omit<OKetLuan, 'ma'>> = {
  dong_bien: { loai: 'DONG_BIEN', nhan: 'Đồng biến', testId: 'latex-db', congThuc: false },
  nghich_bien: { loai: 'NGHICH_BIEN', nhan: 'Nghịch biến', testId: 'latex-nb', congThuc: false },
  cuc_dai: { loai: 'CUC_DAI', nhan: 'Cực đại', testId: 'latex-cd', congThuc: true, trongThi: 'không có cực đại' },
  cuc_tieu: { loai: 'CUC_TIEU', nhan: 'Cực tiểu', testId: 'latex-ct', congThuc: true, trongThi: 'không có cực tiểu' },
};

export function oKetLuan(khaiBao: readonly string[]): OKetLuan[] {
  return khaiBao.map((ma) => ({ ma, ...(KHAI_BAO[ma] ?? { loai: ma.toUpperCase(), nhan: ma, testId: `latex-${ma}`, congThuc: false }) }));
}

/** Những gì học sinh đã nhập ở năm bước (nháp hiện trên màn). */
export interface NhapPhieu {
  txd: string;
  dh: string[];
  nghiem: { latex: string; loai: LoaiNghiem }[];
  moc: string[];
  dau: Record<number, string>;
  mui: Record<number, Mui>;
  kl: Record<string, string>;
}

/** v0 gửi điểm y′ không xác định bằng câu này, kèm `loai: KHONG_XD`. */
const TIEN_TO_KHONG_XD = "y' không xác định tại ";

/** Dựng nháp từ bài làm đã lưu ở core (vào lại bài mở đúng chỗ đã làm). */
export function nhapTuBaiLam(bai: ChiTietBai): NhapPhieu {
  const buoc = (ma: string) => bai.baiLam.cacBuoc.find((b) => b.maBuoc === ma);
  const dong = (ma: string) => [...(buoc(ma)?.dong ?? [])].sort((a, b) => a.dong - b.dong);
  const bang = buoc(XET_DAU)?.bang ?? [];
  const khai = oKetLuan(bai.khaiBaoKetLuan);
  const kl: Record<string, string> = {};
  for (const d of dong(KET_LUAN)) {
    const o = khai.find((x) => x.loai === d.loai);
    if (o) kl[o.ma] = d.latex === o.trongThi ? '' : d.latex;
  }
  const dh = dong(DAO_HAM).map((d) => d.latex);
  const nghiem = dong(NGHIEM)
    .filter((d) => d.latex !== 'không có nghiệm')
    .map((d) =>
      d.loai === 'KHONG_XD'
        ? { latex: d.latex.startsWith(TIEN_TO_KHONG_XD) ? d.latex.slice(TIEN_TO_KHONG_XD.length) : d.latex, loai: 'KHONG_XD' as const }
        : { latex: d.latex, loai: 'NGHIEM' as const },
    );
  const cua = (hang: string) => bang.filter((o) => o.hang === hang);
  return {
    txd: dong(TXD)[0]?.latex ?? '',
    dh: dh.length ? dh : [''],
    nghiem: nghiem.length ? nghiem : [{ latex: '', loai: 'NGHIEM' }],
    moc: cua(HANG_X)
      .sort((a, b) => a.k - b.k)
      .map((o) => o.giaTri),
    dau: Object.fromEntries(cua(HANG_DAU).map((o) => [o.k, o.giaTri])),
    mui: Object.fromEntries(cua(HANG_MUI).map((o) => [o.k, o.giaTri as Mui])),
    kl,
  };
}

/** Số dòng đã gửi của hàng nghiệm thứ `i` trên màn: hàng trống không gửi, các hàng sau đánh số lại (như v0). */
export function dongNghiem(n: NhapPhieu, i: number): number | undefined {
  if (!n.nghiem[i]?.latex.trim()) return undefined;
  return n.nghiem.slice(0, i).filter((r) => r.latex.trim()).length;
}

/** Thân `POST /api/hs/bai/{maBai}/buoc` cho một bước, như payload của v0 (`apps/web/components/solve-client.tsx`). */
export function yeuCauNop(maBuoc: string, n: NhapPhieu, khaiBao: readonly string[]): NopBuoc {
  switch (maBuoc) {
    case TXD:
      return { maBuoc, dong: [{ dong: 0, latex: n.txd }] };
    case DAO_HAM: {
      const dong = n.dh.map((latex, i) => ({ dong: i, latex })).filter((d) => d.latex.trim());
      return { maBuoc, dong: dong.length ? dong : [{ dong: 0, latex: '' }] };
    }
    case NGHIEM: {
      const dong = n.nghiem
        .filter((r) => r.latex.trim())
        .map((r, i) => ({ dong: i, latex: r.loai === 'KHONG_XD' ? TIEN_TO_KHONG_XD + r.latex : r.latex, loai: r.loai }));
      return { maBuoc, dong: dong.length ? dong : [{ dong: 0, latex: 'không có nghiệm', loai: 'NGHIEM' }] };
    }
    case XET_DAU: {
      const bang: OBang[] = n.moc.map((giaTri, k) => ({ hang: HANG_X, k, giaTri }));
      for (let j = 0; j <= n.moc.length; j++) bang.push({ hang: HANG_DAU, k: 2 * j, giaTri: n.dau[2 * j] ?? '' });
      for (let j = 0; j < n.moc.length; j++) if (n.dau[2 * j + 1]) bang.push({ hang: HANG_DAU, k: 2 * j + 1, giaTri: n.dau[2 * j + 1] });
      for (let j = 0; j <= n.moc.length; j++) if (n.mui[2 * j]) bang.push({ hang: HANG_MUI, k: 2 * j, giaTri: n.mui[2 * j] });
      return { maBuoc, bang };
    }
    case KET_LUAN:
      return {
        maBuoc,
        dong: oKetLuan(khaiBao).map((o, i) => {
          const chu = (n.kl[o.ma] ?? '').trim();
          return { dong: i, latex: chu || o.trongThi || '', loai: o.loai };
        }),
      };
    default:
      return { maBuoc };
  }
}
