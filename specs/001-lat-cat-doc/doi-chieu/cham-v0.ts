// Đối chiếu chấm từng bước với v0 (T023, SC-006). Chạy NGUYÊN mã v0 trên dịch vụ toán build từ checkout: thân SolveClient
// của apps/web/components/solve-client.tsx (state, payload(), submit()) tới trước phần hiển thị, nopBuoc của
// apps/web/lib/actions/hs.ts, mathJob của apps/web/lib/math.ts, cổng trang apps/web/app/hs/luyen/[id]/page.tsx. Học sinh tổng
// hợp gõ lời giải mẫu của v0-bai.json vào state qua setter của chính SolveClient, theo bảng biến thể BIEN_THE và một chính
// sách sửa bài (chay). Ghi cham-v0.json cho DoiChieuChamV0Test của core.
// Chạy từ gốc repo (Node ≥ 23.6, Docker), các nguồn phải đã commit:
//   node specs/001-lat-cat-doc/doi-chieu/cham-v0.ts
// THU=<tệp> chạy cả khi nguồn chưa commit và ghi kết quả vào <tệp> thay vì tệp vàng: để thử bảng biến thể trước khi commit.
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import path from 'node:path';
import vm from 'node:vm';
import { BUOC } from '../../../apps/web/lib/levels.ts';
import { mathJob } from '../../../apps/web/lib/math.ts';
import { chayDichVuToan, doan, giua, nguonGit, type DichVuToan } from './chung.ts';

const GOC = path.resolve(import.meta.dirname, '..', '..', '..');
const DOI_CHIEU = 'specs/001-lat-cat-doc/doi-chieu/';
const SC = 'apps/web/components/solve-client.tsx';
const HS = 'apps/web/lib/actions/hs.ts';
const TRANG = 'apps/web/app/hs/luyen/[id]/page.tsx';
const NGUON = [SC, HS, 'apps/web/lib/math.ts', 'apps/web/lib/levels.ts', TRANG, DOI_CHIEU + 'v0-bai.json', DOI_CHIEU + 'cham-v0.ts',
  DOI_CHIEU + 'chung.ts'];
const TRAN_BYTE = 2 * 1024 * 1024;
const TOI_DA_LAN_NOP = 15;
/** Khóa giá trị trả về của nopBuoc không ghi, vì giá trị do giả lập của script quyết định, không do mã v0; kèm giá trị giả lập. */
const KHONG_GHI: Record<string, { lyDo: string; gia: unknown }> = {
  sub_id: { lyDo: 'UUID ngẫu nhiên', gia: undefined },
  tiep_theo: { lyDo: 'recommend giả lập (gợi ý bài kế chưa có ở core)', gia: null },
  loi_giai: { lyDo: 'cờ lớp giả lập; core mở lời giải qua NopBai/MoLoiGiai', gia: null },
  nghi_doan_mo: { lyDo: 'sự kiện nhập ngoài phạm vi', gia: false },
  de_xuat_gui_gv: { lyDo: 'ketSauNopSai giả lập (tutor)', gia: false },
  buoc_de_xuat: { lyDo: 'ketSauNopSai giả lập (tutor)', gia: null },
};

type Ma = (typeof BUOC)[number]['ma'];
const ORDER: readonly Ma[] = BUOC.map((b) => b.ma);
const [TXD, DAOHAM, NGHIEM, XETDAU, KETLUAN] = ORDER;
const KET_QUA = ['DAT', 'SAI', 'KHONG_KIEM_DUOC'] as const;

/** Ô nhập của SolveClient, đúng tên state trong solve-client.tsx. */
type TrangThai = {
  txd: string;
  dh: string[];
  roots: { latex: string; loai: 'NGHIEM' | 'KHONG_XD' }[];
  points: string[];
  signs: Record<number, string>;
  arrows: Record<number, string>;
  kl: { db: string; nb: string; cd: string; ct: string };
};
/** Bước nào sở hữu ô nào: gõ bước s là đặt đúng các ô này, qua setter của SolveClient. */
const O_CUA_BUOC: Record<Ma, readonly (keyof TrangThai)[]> = {
  [TXD]: ['txd'], [DAOHAM]: ['dh'], [NGHIEM]: ['roots'], [XETDAU]: ['points', 'signs', 'arrows'], [KETLUAN]: ['kl'],
};
const SETTER = { txd: 'setTxd', dh: 'setDh', roots: 'setRoots', points: 'setPoints', signs: 'setSigns', arrows: 'setArrows', kl: 'setKl' } as const;

/** Hàng `problems` của v0 mà nopBuoc và trang làm bài đọc, dựng từ v0-bai.json. */
type HangBaiV0 = { id: string; status: string; hamSympy: string | null; buocBatDau: string | null; dangTraLoi: string | null;
  skillCode: string; mucDo4: string; statementText: string };
/** Lời giải máy (dạng kiểm định) trong cot_v0.baiLam. */
type BaiLamMay = { TXD: string; dao_ham: string; y_phay_bang_0: string[]; y_phay_khong_xd: string[];
  bang: { moc: string[]; dau: string[]; dau_tai_diem: string[]; chieu: string[] };
  ket_luan: { dong_bien: string[]; nghich_bien: string[]; cuc_dai_x: string[]; cuc_tieu_x: string[];
    gia_tri_cuc_dai: string[]; gia_tri_cuc_tieu: string[] } };
/** Bài cổng v0 cho làm. `hoiCucTri` là kết quả deHoiCucTri thật của v0 trên đề. */
type BaiMau = { hang: HangBaiV0; batDau: number; hoiCucTri: boolean; baiLam: BaiLamMay };

type BoQua = { boQua: string };
const boQua = (lyDo: string): BoQua => ({ boQua: lyDo });
type BienThe = {
  ma: string;
  moTa: string;
  /** Các bước học sinh gõ sai, theo thứ tự khung. Rỗng: lời giải mẫu. */
  buoc: readonly Ma[];
  /** Gặp kết quả không đạt lần đầu thì vẫn bấm sang bước kế (như học sinh bỏ qua chỗ đỏ), rồi mới sửa. */
  diTiep?: true;
  /** Ô sai của các bước trong `buoc`, hay lý do biến thể không áp dụng cho bài. Hàm thuần. */
  sai: (b: BaiMau, dung: TrangThai) => Partial<TrangThai> | BoQua;
};

const R = '\\mathbb{R}';
const coChu = (roots: TrangThai['roots']) => roots.filter((r) => r.latex);
/** Không còn dòng nghiệm nào thì giữ dòng trống ban đầu của SolveClient (v0 gửi «không có nghiệm»). */
const dongNghiem = (roots: TrangThai['roots']): TrangThai['roots'] => (roots.length ? roots : [{ latex: '', loai: 'NGHIEM' }]);
const daoDau = (s: string) => (s === '+' ? '-' : s === '-' ? '+' : s);
const khoang = (k: string) => k.replace(/\s+/g, '').replace(/oo/g, '∞');
/** Điểm bị loại khỏi TXĐ (`R \ {a}`), hay null khi TXĐ là ℝ. */
const ngoaiTxd = (b: BaiMau) => /^R \\ \{(.+)\}$/.exec(b.baiLam.TXD)?.[1] ?? null;

/** Thêm mốc `v` (lớn hơn mọi mốc) vào cuối bảng: khoảng cuối tách đôi cùng dấu, cùng chiều; dấu tại mốc là 0. */
function themMoc(d: TrangThai, v: string): Pick<TrangThai, 'points' | 'signs' | 'arrows'> {
  if (d.points.some((p) => Number(p) >= Number(v))) throw new Error('mốc thêm phải lớn hơn mọi mốc: ' + v);
  const n = d.points.length;
  return {
    points: [...d.points, v],
    signs: { ...d.signs, [2 * n + 1]: '0', [2 * n + 2]: d.signs[2 * n] },
    arrows: { ...d.arrows, [2 * n + 2]: d.arrows[2 * n] },
  };
}

/** Bỏ mốc thứ `j`: hai khoảng kề gộp một, giữ dấu và chiều của khoảng trái; ô sau dồn về trước hai chỉ số. */
function boMoc(d: TrangThai, j: number): Pick<TrangThai, 'points' | 'signs' | 'arrows'> {
  const don = (o: Record<number, string>) => Object.fromEntries(Object.entries(o)
    .filter(([k]) => Number(k) !== 2 * j + 1 && Number(k) !== 2 * j + 2)
    .map(([k, v]) => [Number(k) > 2 * j + 2 ? Number(k) - 2 : Number(k), v]));
  return { points: d.points.filter((_, i) => i !== j), signs: don(d.signs), arrows: don(d.arrows) };
}

/**
 * Biến thể: mỗi dòng nhắm một đường chấm của dịch vụ toán (cột «nhắm» ở README là dự đoán; tệp vàng ghi kết quả thật). Bốn
 * loại của services/math/tests/test_khung_ngan_seed.py (dao_db_nb, doi_dau, thieu_moc, gop_U) viết lại ở dạng ô giao diện.
 */
const BIEN_THE: readonly BienThe[] = [
  { ma: 'dung', moTa: 'lời giải mẫu gõ đúng từng bước', buoc: [], sai: () => ({}) },
  { ma: 'txd_sai', moTa: 'TXĐ sai: ℝ thành ℝ∖{0}, ℝ∖{a} thành ℝ', buoc: [TXD],
    sai: (_b, d) => ({ txd: d.txd === R ? '\\mathbb{R}\\setminus\\{0\\}' : R }) },
  { ma: 'txd_trong', moTa: 'bỏ trống TXĐ', buoc: [TXD], sai: () => ({ txd: '' }) },
  { ma: 'dao_ham_hai_dong', moTa: 'hai dòng đạo hàm không tương đương, dòng trống ở giữa (v0 bỏ dòng trống, giữ số dòng)', buoc: [DAOHAM],
    sai: (_b, d) => ({ dh: [d.dh[0], '', d.dh[0] + '+1'] }) },
  { ma: 'dao_ham_trong', moTa: 'bỏ trống đạo hàm', buoc: [DAOHAM], sai: () => ({ dh: [''] }) },
  { ma: 'thieu_nghiem', moTa: 'thiếu nghiệm cuối (một nghiệm thì còn dòng trống, v0 gửi «không có nghiệm»)', buoc: [NGHIEM],
    sai: (b, d) => (b.baiLam.y_phay_bang_0.length ? { roots: dongNghiem(coChu(d.roots).slice(0, -1)) } : boQua('không có nghiệm để bỏ')) },
  { ma: 'thua_nghiem', moTa: 'thêm nghiệm x = 100', buoc: [NGHIEM],
    sai: (_b, d) => ({ roots: [...coChu(d.roots), { latex: 'x = 100', loai: 'NGHIEM' }] }) },
  { ma: 'diem_ngoai_txd', moTa: 'thêm điểm bị loại khỏi TXĐ như điểm y′ không xác định', buoc: [NGHIEM],
    sai: (b, d) => {
      const a = ngoaiTxd(b);
      return a === null ? boQua('TXĐ là ℝ, không có điểm bị loại') : { roots: [...coChu(d.roots), { latex: 'x = ' + a, loai: 'KHONG_XD' }] };
    } },
  { ma: 'nghiem_chu', moTa: 'nghiệm viết bằng chữ x = a', buoc: [NGHIEM], sai: () => ({ roots: [{ latex: 'x = a', loai: 'NGHIEM' }] }) },
  { ma: 'thua_diem_xuyen_buoc', moTa: 'thêm nghiệm x = 100 và mốc 100 (dấu 0) vào bảng', buoc: [NGHIEM, XETDAU], diTiep: true,
    sai: (_b, d) => ({ roots: [...coChu(d.roots), { latex: 'x = 100', loai: 'NGHIEM' }], ...themMoc(d, '100') }) },
  { ma: 'thieu_diem_xuyen_buoc', moTa: 'thiếu nghiệm cuối và bảng dựng theo nghiệm còn lại', buoc: [NGHIEM, XETDAU], diTiep: true,
    sai: (b, d) => {
      const bo = b.baiLam.y_phay_bang_0.at(-1);
      if (bo === undefined) return boQua('không có nghiệm để bỏ');
      return { roots: dongNghiem(coChu(d.roots).filter((r) => r.latex !== 'x = ' + bo)), ...boMoc(d, d.points.indexOf(bo)) };
    } },
  { ma: 'doi_dau', moTa: 'đổi dấu y′ ở mọi khoảng', buoc: [XETDAU],
    sai: (_b, d) => ({ signs: Object.fromEntries(Object.entries(d.signs).map(([k, v]) => [k, Number(k) % 2 ? v : daoDau(v)])) }) },
  { ma: 'dao_moc', moTa: 'mốc theo thứ tự giảm (v0 không tự sắp, chốt 29/09)', buoc: [XETDAU],
    sai: (_b, d) => (d.points.length >= 2 ? { points: [...d.points].reverse() } : boQua('ít hơn hai mốc')) },
  { ma: 'thieu_moc', moTa: 'bỏ mốc đầu, gộp hai khoảng kề', buoc: [XETDAU],
    sai: (_b, d) => (d.points.length ? boMoc(d, 0) : boQua('không có mốc')) },
  { ma: 'o_dau_trong', moTa: 'bỏ trống ô dấu của khoảng đầu (v0 vẫn gửi ô k chẵn, giá trị rỗng)', buoc: [XETDAU],
    sai: (_b, d) => ({ signs: Object.fromEntries(Object.entries(d.signs).filter(([k]) => k !== '0')) }) },
  { ma: 'doc_hai', moTa: 'mốc đầu là mã Python __import__("os") (chặn F-01)', buoc: [XETDAU],
    sai: (_b, d) => ({ points: ['__import__("os")', ...d.points.slice(1)] }) },
  { ma: 'dao_db_nb', moTa: 'đổi chỗ khoảng đồng biến và nghịch biến', buoc: [KETLUAN], sai: (_b, d) => ({ kl: { ...d.kl, db: d.kl.nb, nb: d.kl.db } }) },
  { ma: 'gop_U', moTa: 'nối các khoảng bằng « U » (luật dấu U, 0002c)', buoc: [KETLUAN],
    sai: (b, d) => {
      const { dong_bien: db, nghich_bien: nb } = b.baiLam.ket_luan;
      return db.length < 2 && nb.length < 2 ? boQua('mỗi ô đơn điệu chỉ một khoảng')
        : { kl: { ...d.kl, db: db.map(khoang).join(' U '), nb: nb.map(khoang).join(' U ') } };
    } },
  { ma: 'cuc_tri_trong', moTa: 'bỏ trống ô cực đại (v0 gửi «không có cực đại»)', buoc: [KETLUAN],
    sai: (b, d) => (b.hoiCucTri && b.baiLam.ket_luan.cuc_dai_x.length ? { kl: { ...d.kl, cd: '' } }
      : boQua('đề không hỏi cực trị hay hàm không có cực đại')) },
  { ma: 'chi_tung_do', moTa: 'ô cực đại chỉ ghi tung độ y = …', buoc: [KETLUAN],
    sai: (b, d) => (b.hoiCucTri && b.baiLam.ket_luan.cuc_dai_x.length ? { kl: { ...d.kl, cd: 'y = ' + b.baiLam.ket_luan.gia_tri_cuc_dai[0] } }
      : boQua('đề không hỏi cực trị hay hàm không có cực đại')) },
  { ma: 'ket_luan_chu', moTa: 'ô đồng biến ghi chữ «tăng»', buoc: [KETLUAN], sai: (_b, d) => ({ kl: { ...d.kl, db: 'tăng' } }) },
];

/** Biểu thức SymPy của lời giải máy → chữ học sinh gõ: `**n` → `^{n}`, bỏ `*`, một `/` ở mức ngoài cùng → `\frac{A}{B}`. */
function goBieuThuc(e: string): string {
  let sau = 0;
  const chia: number[] = [];
  [...e].forEach((c, i) => {
    sau += c === '(' ? 1 : c === ')' ? -1 : 0;
    if (c === '/' && sau === 0) chia.push(i);
  });
  if (chia.length > 1) throw new Error('nhiều phép chia ở mức ngoài cùng: ' + e);
  if (chia.length === 1) return '\\frac{' + goBieuThuc(boNgoac(e.slice(0, chia[0]))) + '}{' + goBieuThuc(boNgoac(e.slice(chia[0] + 1))) + '}';
  return e.replace(/\*\*(\d+)/g, '^{$1}').replace(/\*/g, '').replace(/\s+/g, '');
}

/** Bỏ cặp ngoặc bọc trọn biểu thức, nếu có. */
function boNgoac(e: string): string {
  const t = e.trim();
  if (!t.startsWith('(') || !t.endsWith(')')) return t;
  let sau = 0;
  for (let i = 0; i < t.length - 1; i++) {
    sau += t[i] === '(' ? 1 : t[i] === ')' ? -1 : 0;
    if (sau === 0) return t;
  }
  return t.slice(1, -1);
}

/**
 * Lời giải máy → ô học sinh gõ. Dạng gõ: TXĐ `\mathbb{R}` như e2e luong-hoc-sinh.spec.ts, `ℝ ∖ {a}` như `_txd_latex` của
 * grader.py; đạo hàm như e2e (`3x^{2}-12x+9`); mỗi nghiệm một dòng `x = v` như `bai_lam_sang_payload`; mốc, dấu (`-`, không
 * `−`), chiều như nút bấm của bảng; ô kết luận như `ket_luan_o_nen` của kiemdinh `ca-dau-vao-doc-hai.yaml`. Hàm thuần.
 */
function trangThaiDung(b: BaiMau): TrangThai {
  const l = b.baiLam;
  const ngoai = ngoaiTxd(b);
  if (l.TXD !== 'R' && ngoai === null) throw new Error('TXĐ lạ: ' + l.TXD);
  const cucTri = (xs: string[], ys: string[]) => xs.map((x, i) => `x = ${x}, y = ${ys[i]}`).join('; ');
  return {
    txd: ngoai === null ? R : '\\mathbb{R}\\setminus\\{' + ngoai.replace(/,\s*/g, ';') + '\\}',
    dh: [goBieuThuc(l.dao_ham)],
    roots: dongNghiem([
      ...l.y_phay_bang_0.map((v) => ({ latex: 'x = ' + v, loai: 'NGHIEM' as const })),
      ...l.y_phay_khong_xd.map((v) => ({ latex: 'x = ' + v, loai: 'KHONG_XD' as const })),
    ]),
    points: l.bang.moc.slice(1, -1),
    signs: Object.fromEntries([...l.bang.dau.map((s, j) => [2 * j, s]), ...l.bang.dau_tai_diem.map((s, j) => [2 * j + 1, s])]),
    arrows: Object.fromEntries(l.bang.chieu.map((c, j) => [2 * j, ({ tang: 'TANG', giam: 'GIAM' } as Record<string, string>)[c] ?? c])),
    kl: {
      db: l.ket_luan.dong_bien.map(khoang).join(' và '),
      nb: l.ket_luan.nghich_bien.map(khoang).join(' và '),
      cd: b.hoiCucTri ? cucTri(l.ket_luan.cuc_dai_x, l.ket_luan.gia_tri_cuc_dai) : '',
      ct: b.hoiCucTri ? cucTri(l.ket_luan.cuc_tieu_x, l.ket_luan.gia_tri_cuc_tieu) : '',
    },
  };
}

/** Tay cầm của một lần dựng SolveClient: state và hàm của lần dựng đó, như closure của React. */
type TayCam = {
  step: number;
  grade: Record<string, unknown> | null;
  events: unknown[];
  setTxd(v: unknown): void; setDh(v: unknown): void; setRoots(v: unknown): void; setPoints(v: unknown): void;
  setSigns(v: unknown): void; setArrows(v: unknown): void; setKl(v: unknown): void; setStep(v: number): void;
  payload(nopToi: Ma): { nop_toi: string; cac_buoc: unknown[]; events: unknown[] };
  submit(): Promise<void>;
};
/** Một phiên SolveClient: mỗi `dung()` gọi lại hàm thành phần với state giữ theo thứ tự hook, như React. */
type Phien = { dung(): TayCam };
type V0 = { taoPhien(h: HangBaiV0): Phien; choLam(h: HangBaiV0): boolean; deHoiCucTri(de: string): boolean };

type LanGoi = { bam: string; chu: string; phanHoi: Record<string, unknown> };
type LanCham = { yeu_cau: unknown; phan_hoi: Record<string, unknown>; tra_ve: Record<string, unknown>; ghi: Record<string, unknown> };
type LanNop = { bam: string; nop_toi: Ma; ket_qua: string; buoc_sau: Ma | null };
type KichBan = { ma: string; bien_the: string; trang_thai: { dung: TrangThai; sai: Partial<TrangThai> }; lan_nop: LanNop[] }
  | { ma: string; bien_the: string; bo_qua: string };
type TepVang = {
  nguon: { git: Record<string, string>; dich_vu_toan: Omit<DichVuToan['nguon'], 'cay_git'> };
  bien_the: { ma: string; buoc: readonly Ma[]; di_tiep?: true; mo_ta: string }[];
  khong_cham: { ma: string; trang_thai: string; dang_tra_loi: string | null; co_ham: boolean }[];
  kich_ban: KichBan[];
  lan_cham: Record<string, LanCham>;
};

const doc = (tep: string) => readFileSync(path.join(GOC, tep), 'utf8');

/**
 * Cắt mã v0, bỏ kiểu, chạy trong vm. React tối thiểu: useState, useRef giữ ô theo thứ tự gọi như React, useEffect bỏ qua
 * (không đụng localStorage). Mọi định danh tự do của các đoạn cắt có giả lập ở `ngu`; thiếu thì ReferenceError ở lần chạy đầu.
 */
function napV0(ghiDb: (bang: string, o: Record<string, unknown>) => void, hangDangLam: () => HangBaiV0): V0 {
  const sc = doc(SC);
  const hs = doc(HS);
  const ma = [
    doan(hs, HS, 'const ORDER_BUOC = ', ';\n'),
    doan(hs, HS, 'function laDauU(', '\n}\n'),
    doan(hs, HS, 'export async function nopBuoc(', '\n}\n').replace(/^export /, ''),
    doan(sc, SC, 'function deHoiCucTri(', '\n}\n'),
    doan(sc, SC, 'const ORDER = ', ';\n'),
    giua(sc, SC, 'export function SolveClient(', '  const badStep =').replace(/^export /, '')
      + '  return { step, grade, events, payload, submit, setTxd, setDh, setRoots, setPoints, setSigns, setArrows, setKl, setStep };\n}\n',
    '({ SolveClient, deHoiCucTri, choLam: (p) => !(' + doan(doc(TRANG), TRANG, 'p.status !== "DA_PHAT_HANH"', '"TU_LUAN_5_BUOC")') + ') })',
  ].join('\n');
  const js = stripTypeScriptTypes(ma, { mode: 'strip' });

  let o: unknown[] = [];
  let i = 0;
  const bang = (ten: string) => ({ ten, id: ten + '.id' });
  let soId = 0;
  const ngu = {
    BUOC,
    AI_MAC_DINH: Object.freeze({}),
    useState: (dau: unknown) => {
      const k = i++;
      if (!(k in o)) o[k] = dau;
      return [o[k], (v: unknown) => void (o[k] = typeof v === 'function' ? v(o[k]) : v)];
    },
    useRef: (dau: unknown) => {
      const k = i++;
      if (!(k in o)) o[k] = { current: dau };
      return o[k];
    },
    useEffect: () => void i++,
    requireRole: async () => ({ id: 'hoc-sinh-tong-hop' }),
    assertMayLearn: async () => {},
    dungMotLuot: async () => true,
    khoDb: {},
    GIOI_HAN: { nopBuoc: {} },
    db: {
      select: () => ({
        from: (t: { ten: string }) => ({
          where: () => ({
            limit: async () => {
              if (t.ten !== 'problems') throw new Error('nopBuoc đọc bảng ngoài dự kiến: ' + t.ten);
              return [hangDangLam()];
            },
          }),
        }),
      }),
      insert: (t: { ten: string }) => ({ values: async (v: Record<string, unknown>) => ghiDb(t.ten, v) }),
    },
    ...Object.fromEntries(['problems', 'solutions', 'submissions', 'submissionSteps', 'submissionTables', 'submissionTableCells',
      'inputEvents', 'gradingResults'].map((t) => [t, bang(t)])),
    eq: (a: unknown, b: unknown) => ({ eq: [a, b] }),
    loadConfig: async () => ({ nguong_doan_mo_so_lan_doi_o: 4 }),
    nghiDoanMo: (ev: unknown[]) => {
      if (ev.length) throw new Error('cham-v0 không ghi sự kiện nhập');
      return null;
    },
    mathJob,
    applyMastery: async () => {},
    ketSauNopSai: async () => false,
    revalidatePath: () => {},
    recommend: async () => null,
    caiDatLopCuaHs: async () => ({ moLoiGiaiSauKhiNop: false }),
    loiGiaiHocSinh: () => {
      throw new Error('lời giải chỉ đọc khi lớp bật cờ');
    },
    crypto: { randomUUID: () => 'id-' + ++soId },
    // v0 bắt lỗi dịch vụ toán rồi trả «Máy chấm đang bận»: ở đây là lỗi sinh, không ghi.
    console: {
      error: (...a: unknown[]) => {
        throw new Error('v0 báo lỗi: ' + a.map(String).join(' '));
      },
    },
  };
  const ra = vm.runInNewContext(js, ngu, { filename: 'v0 (solve-client.tsx, hs.ts, page.tsx)' }) as {
    SolveClient(props: Record<string, unknown>): TayCam;
    deHoiCucTri(de: string): boolean;
    choLam(p: HangBaiV0): boolean;
  };
  return {
    deHoiCucTri: ra.deHoiCucTri,
    choLam: ra.choLam,
    taoPhien(h) {
      const cua: unknown[] = [];
      let soHook = -1;
      // Như page.tsx truyền; các prop khác chỉ phần hiển thị (không cắt) đọc, để mặc định.
      const props = { problemId: h.id, title: h.statementText, buocBatDau: h.buocBatDau };
      return {
        dung() {
          o = cua;
          i = 0;
          const t = ra.SolveClient(props);
          if (soHook >= 0 && i !== soHook) throw new Error('SolveClient gọi số hook khác lần dựng trước: ' + i + ' / ' + soHook);
          soHook = i;
          return t;
        },
      };
    },
  };
}

/**
 * fetch toàn tiến trình: mathJob của v0 đi qua đây. Mỗi thân khác nhau tới dịch vụ toán một lần; thân lặp lại dùng phản hồi
 * đã có (bộ chấm tất định theo thân). Lỗi HTTP hay phong bì lỗi của sandbox (quá tải, hết giờ) là lỗi sinh, không ghi.
 */
function ghiFetch(url: string, goi: LanGoi[]): void {
  const that = globalThis.fetch;
  const daCo = new Map<string, string>();
  globalThis.fetch = async (u: string | URL | Request, init?: RequestInit) => {
    if (String(u) !== url + '/v1/grade' || init?.method !== 'POST' || typeof init.body !== 'string') {
      throw new Error('cham-v0 chỉ cho gọi POST /v1/grade: ' + String(u));
    }
    const chu = init.body;
    const bam = createHash('sha256').update(chu, 'utf8').digest('hex');
    let tra = daCo.get(bam);
    if (tra === undefined) {
      const r = await that(u, init);
      tra = await r.text();
      if (!r.ok) throw new Error('dịch vụ toán lỗi ' + r.status + ': ' + tra.slice(0, 300));
      const p = JSON.parse(tra);
      if (Object.keys(p).every((k) => ['ket_qua', 'loai_ket_qua', 'trang_thai', 'ly_do', 'cho_phep'].includes(k))) {
        throw new Error('phong bì lỗi của sandbox, không phải phán quyết: ' + tra);
      }
      daCo.set(bam, tra);
    }
    goi.push({ bam, chu, phanHoi: JSON.parse(tra) });
    return new Response(tra, { status: 200, headers: { 'content-type': 'application/json' } });
  };
}

const json = (v: unknown) => JSON.stringify(v);

/** Một kịch bản: một học sinh, một phiên SolveClient mới, chính sách sửa bài (README, «Chính sách học sinh»). */
async function chay(v0: V0, b: BaiMau, bt: BienThe, goi: LanGoi[], ghiDb: { bang: string; o: Record<string, unknown> }[],
    lanCham: Map<string, LanCham>): Promise<KichBan> {
  const dau = { ma: b.hang.id, bien_the: bt.ma };
  const deCho = bt.buoc.find((s) => ORDER.indexOf(s) < b.batDau);
  if (deCho) return { ...dau, bo_qua: `bước ${deCho} do đề cho (bài bắt đầu ở ${ORDER[b.batDau]})` };
  const dung = trangThaiDung(b);
  const sai = bt.sai(b, dung);
  if ('boQua' in sai) return { ...dau, bo_qua: sai.boQua };
  const oSai = bt.buoc.flatMap((s) => O_CUA_BUOC[s]);
  const la = Object.keys(sai).filter((f) => !oSai.includes(f as keyof TrangThai));
  if (la.length) throw new Error(`${bt.ma} sửa ô ngoài các bước của nó: ${la}`);
  if (bt.buoc.length && oSai.every((f) => json(sai[f] ?? dung[f]) === json(dung[f]))) return { ...dau, bo_qua: 'không đổi bài làm' };

  const phien = v0.taoPhien(b.hang);
  let ui = phien.dung();
  let daSua = false;
  let daDiTiep = false;
  let daGoSai = false;
  const lanNop: LanNop[] = [];
  const ten = `${b.hang.id} / ${bt.ma}`;
  for (;;) {
    if (lanNop.length >= TOI_DA_LAN_NOP) throw new Error(`${ten}: quá ${TOI_DA_LAN_NOP} lần nộp`);
    const s = ORDER[ui.step];
    const goSai = !daSua && bt.buoc.includes(s);
    daGoSai ||= goSai;
    for (const f of O_CUA_BUOC[s]) ui[SETTER[f]](structuredClone(goSai ? (sai[f] ?? dung[f]) : dung[f]));
    ui = phien.dung();
    const guiDi = ui.payload(s);
    const goiTruoc = goi.length;
    const ghiTruoc = ghiDb.length;
    await ui.submit();
    const buocTruoc = ui.step;
    ui = phien.dung();

    if (goi.length !== goiTruoc + 1) throw new Error(`${ten}: một lần nộp phải gọi máy chấm đúng một lần`);
    const lg = goi[goi.length - 1];
    const yeuCau = JSON.parse(lg.chu);
    if (yeuCau.nop_toi !== s || json(yeuCau.cac_buoc) !== json(guiDi.cac_buoc) || guiDi.events.length) {
      throw new Error(`${ten}: thân chấm khác payload máy học sinh (bước đã gõ mà chưa nộp?)`);
    }
    if (lanNop.some((l) => l.bam === lg.bam)) throw new Error(`${ten}: hai lần nộp cùng yêu cầu`);
    const res = ui.grade;
    if (!res || res.ok !== true) throw new Error(`${ten}: nopBuoc không trả kết quả chấm: ${json(res)}`);
    const traVe = Object.fromEntries(Object.entries(res).filter(([k]) => !(k in KHONG_GHI)));
    for (const [k, { gia }] of Object.entries(KHONG_GHI)) {
      if (k !== 'sub_id' && json(res[k]) !== json(gia)) throw new Error(`${ten}: ${k} khác giá trị giả lập: ${json(res[k])}`);
    }
    const ghi = ghiDb.slice(ghiTruoc).filter((g) => g.bang === 'gradingResults');
    if (ghi.length !== 1) throw new Error(`${ten}: phải ghi đúng một hàng gradingResults`);
    const hang = Object.fromEntries(Object.entries(ghi[0].o).filter(([k]) => k !== 'id' && k !== 'submissionId'));
    if (Object.values(hang).some((v) => v === undefined) || Object.values(traVe).some((v) => v === undefined)) {
      throw new Error(`${ten}: giá trị undefined sẽ mất khỏi tệp vàng`);
    }
    const lc: LanCham = { yeu_cau: yeuCau, phan_hoi: lg.phanHoi, tra_ve: traVe, ghi: hang };
    const cu = lanCham.get(lg.bam);
    if (cu && json(cu) !== json(lc)) throw new Error(`${ten}: cùng yêu cầu mà v0 trả hay ghi khác`);
    if (!cu) lanCham.set(lg.bam, lc);
    const buocSau = ui.step !== buocTruoc ? ORDER[ui.step] : null;
    lanNop.push({ bam: lg.bam, nop_toi: s, ket_qua: String(res.ket_qua), buoc_sau: buocSau });

    if (res.finished === true) break;
    if (res.ket_qua === 'DAT') {
      if (buocSau === null) throw new Error(`${ten}: DAT mà v0 không sang bước`);
      continue;
    }
    if (!daGoSai) throw new Error(`${ten}: lời giải mẫu gõ ra không đạt ở ${s}: ${json(res)}`);
    if (daSua) throw new Error(`${ten}: sửa rồi vẫn không đạt ở ${s}: ${json(res)}`);
    if (bt.diTiep && !daDiTiep && ui.step < ORDER.length - 1) {
      daDiTiep = true;
      ui.setStep(ui.step + 1);
      ui = phien.dung();
      continue;
    }
    daSua = true;
    const somNhat = Math.min(...bt.buoc.map((x) => ORDER.indexOf(x)));
    if (somNhat < ui.step) {
      ui.setStep(somNhat);
      ui = phien.dung();
    }
  }
  return { ...dau, trang_thai: { dung, sai }, lan_nop: lanNop };
}

/** Bất biến trước khi ghi tệp vàng. */
function kiemBatBien(t: TepVang, bai: BaiMau[]): void {
  const daChay = t.kich_ban.filter((k) => 'lan_nop' in k);
  for (const b of bai) {
    const k = daChay.find((x) => x.ma === b.hang.id && x.bien_the === 'dung');
    if (!k || !('lan_nop' in k)) throw new Error(b.hang.id + ': thiếu kịch bản dung');
    const cuoi = t.lan_cham[k.lan_nop.at(-1)!.bam];
    if (k.lan_nop.length !== ORDER.length - b.batDau || k.lan_nop.some((l) => l.ket_qua !== 'DAT') || cuoi.tra_ve.finished !== true) {
      throw new Error(b.hang.id + ': lời giải mẫu phải DAT từng bước tới hết bài');
    }
  }
  if (t.kich_ban.length !== bai.length * BIEN_THE.length) throw new Error('thiếu tích chéo bài × biến thể');
  for (const bt of BIEN_THE) {
    if (!daChay.some((k) => k.bien_the === bt.ma)) throw new Error('biến thể không chạy bài nào: ' + bt.ma);
  }
  const nop = daChay.flatMap((k) => ('lan_nop' in k ? k.lan_nop : []));
  for (const s of ORDER) {
    for (const kq of KET_QUA) {
      if (!nop.some((l) => l.nop_toi === s && l.ket_qua === kq)) throw new Error(`không có lần nộp ${s} ${kq}`);
    }
  }
  const dung = new Set(nop.map((l) => l.bam));
  if (Object.keys(t.lan_cham).some((b) => !dung.has(b)) || [...dung].some((b) => !(b in t.lan_cham))) {
    throw new Error('lan_cham và kich_ban không khớp nhau');
  }
}

async function main(): Promise<void> {
  const thu = process.env.THU;
  const git = thu ? {} : nguonGit(GOC, NGUON);
  const vb = JSON.parse(doc(DOI_CHIEU + 'v0-bai.json')) as { bai: { ma: string; trang_thai_phat_hanh: string; dang_tra_loi: string | null;
    cot_v0: { hamSympy: string | null; buocBatDau: string | null; skillCode: string; mucDo4: string; statementText: string; baiLam: BaiLamMay } }[] };
  const goi: LanGoi[] = [];
  const ghiDb: { bang: string; o: Record<string, unknown> }[] = [];
  let hangDangLam: HangBaiV0 | null = null;
  const v0 = napV0((bang, o) => void ghiDb.push({ bang, o }), () => hangDangLam!);
  const hang = vb.bai.map((x): HangBaiV0 => ({ id: x.ma, status: x.trang_thai_phat_hanh, hamSympy: x.cot_v0.hamSympy,
    buocBatDau: x.cot_v0.buocBatDau, dangTraLoi: x.dang_tra_loi, skillCode: x.cot_v0.skillCode, mucDo4: x.cot_v0.mucDo4,
    statementText: x.cot_v0.statementText }));
  const bai: BaiMau[] = hang.filter(v0.choLam).map((h) => ({
    hang: h,
    batDau: h.buocBatDau ? ORDER.indexOf(h.buocBatDau as Ma) : 0,
    hoiCucTri: v0.deHoiCucTri(h.statementText),
    baiLam: vb.bai.find((x) => x.ma === h.id)!.cot_v0.baiLam,
  }));

  const batDau = Date.now();
  const toan = await chayDichVuToan(GOC);
  const kichBan: KichBan[] = [];
  const lanCham = new Map<string, LanCham>();
  try {
    process.env.MATH_SERVICE_URL = toan.url;
    ghiFetch(toan.url, goi);
    for (const b of bai) {
      hangDangLam = b.hang;
      for (const bt of BIEN_THE) kichBan.push(await chay(v0, b, bt, goi, ghiDb, lanCham));
    }
  } finally {
    toan.dung();
  }
  const { cay_git: cayToan, ...dichVuToan } = toan.nguon;
  const t: TepVang = {
    nguon: { git: { ...git, 'services/math': cayToan }, dich_vu_toan: dichVuToan },
    bien_the: BIEN_THE.map((x) => ({ ma: x.ma, buoc: x.buoc, ...(x.diTiep ? { di_tiep: x.diTiep } : {}), mo_ta: x.moTa })),
    khong_cham: hang.filter((h) => !v0.choLam(h))
      .map((h) => ({ ma: h.id, trang_thai: h.status, dang_tra_loi: h.dangTraLoi, co_ham: h.hamSympy !== null })),
    kich_ban: kichBan,
    lan_cham: Object.fromEntries(lanCham),
  };
  kiemBatBien(t, bai);
  const chu = JSON.stringify(t, null, 2) + '\n';
  if (Buffer.byteLength(chu) > TRAN_BYTE) throw new Error('tệp vàng vượt ' + TRAN_BYTE + ' byte: ' + Buffer.byteLength(chu));
  if (!thu) {
    const sau = nguonGit(GOC, NGUON);
    if (json(sau) !== json(git)) throw new Error('nguồn đổi trong lúc sinh');
  }
  writeFileSync(thu ?? path.join(GOC, DOI_CHIEU, 'cham-v0.json'), chu, 'utf8');

  const daChay = kichBan.filter((k) => 'lan_nop' in k);
  const nop = daChay.flatMap((k) => ('lan_nop' in k ? k.lan_nop : []));
  console.log(`v0: ${bai.length} bài chấm được, ${t.khong_cham.length} bài không chấm (${t.khong_cham.map((k) => k.ma).join(', ')})`);
  console.log(`${BIEN_THE.length} biến thể, ${daChay.length} kịch bản chạy, ${kichBan.length - daChay.length} bỏ qua; ${nop.length} lần nộp, `
    + `${lanCham.size} yêu cầu khác nhau; ${Buffer.byteLength(chu)} byte; ${Math.round((Date.now() - batDau) / 1000)} s`);
  console.log(['bước', ...KET_QUA].join('\t'));
  for (const s of ORDER) console.log([s, ...KET_QUA.map((kq) => nop.filter((l) => l.nop_toi === s && l.ket_qua === kq).length)].join('\t'));
  console.log(`services/math ${cayToan}, ảnh ${toan.anh}`);
}

await main();
