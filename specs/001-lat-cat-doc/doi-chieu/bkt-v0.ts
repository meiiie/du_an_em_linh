// Đối chiếu mức hiểu với v0 (T052, research R7). Chạy NGUYÊN mã v0: DEFAULT_CFG, loadConfig, bktNext, clamp, applyMastery,
// IDX, mucSauBai của apps/web/lib/learning.ts; điều kiện gọi applyMastery và cờ finished trong nopBuoc của
// apps/web/lib/actions/hs.ts; MUC4, mucFromMastery của apps/web/lib/levels.ts (import thật). CSDL là bảng giả trong bộ nhớ:
// mastery_config là dòng seed.ts ghi (data/v0/bkt.json), error_types và step_templates như seed.ts nạp
// (data/supham/ma-loi-DH.csv, data/v0/khung-buoc.json), cột real làm tròn như PostgreSQL rồi đọc lại như postgres.js.
// Mỗi kịch bản là một học sinh tổng hợp nộp lần lượt các bài; ghi bkt-v0.json cho DoiChieuBktV0Test của core.
// Chạy từ gốc repo (Node ≥ 23.6), các nguồn phải đã commit:
//   node specs/001-lat-cat-doc/doi-chieu/bkt-v0.ts
// THU=<tệp> chạy cả khi nguồn chưa commit và ghi vào <tệp> thay vì tệp vàng.
import { readFileSync, writeFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import path from 'node:path';
import vm from 'node:vm';
import { MUC4, mucFromMastery } from '../../../apps/web/lib/levels.ts';
import { doan, nguonGit } from './chung.ts';

const GOC = path.resolve(import.meta.dirname, '..', '..', '..');
const DOI_CHIEU = 'specs/001-lat-cat-doc/doi-chieu/';
const LEARNING = 'apps/web/lib/learning.ts';
const HS = 'apps/web/lib/actions/hs.ts';
const BKT = 'data/v0/bkt.json';
const MA_LOI = 'data/supham/ma-loi-DH.csv';
const KHUNG = 'data/v0/khung-buoc.json';
const NGUON = [LEARNING, HS, 'apps/web/lib/levels.ts', BKT, MA_LOI, KHUNG, DOI_CHIEU + 'bkt-v0.ts', DOI_CHIEU + 'chung.ts'];
const THU = process.env.THU;

const doc = (tep: string) => readFileSync(path.join(GOC, tep), 'utf8');

type Hang = Record<string, unknown>;
type Cham = { ket_qua: 'DAT' | 'SAI' | 'KHONG_KIEM_DUOC'; buoc_sai: { ma_buoc: string } | null; ma_loi: string | null;
  do_tin_cay: number | null; toan_dung?: boolean; chua_xong: false };
type Bai = { ky_nang: string; muc: (typeof MUC4)[number] };
type Buoc = { loai: 'nop'; bai: Bai; cham: Cham; nghi: boolean } | { loai: 'xu_ly_canh_bao' };

/**
 * PostgreSQL ghi số vào cột real bằng float4 gần nhất; postgres.js đọc lại chuỗi chữ số ngắn nhất của số float đó ({@code +x}).
 * toPrecision(p) là số p chữ số gần nhất, nên p nhỏ nhất mà quay về đúng float là chuỗi ngắn nhất.
 */
function real(x: number): number {
  const f = Math.fround(x);
  for (let p = 1; p <= 9; p++) {
    const s = Number(f.toPrecision(p));
    if (Math.fround(s) === f) return s;
  }
  return f;
}

const TEN = Symbol('bảng');
type Bang = { [TEN]: string } & Record<string, { cot: string }>;
const bang = (ten: string) => new Proxy({}, { get: (_, k) => (k === TEN ? ten : { cot: k }) }) as Bang;
const COT_REAL: Record<string, string[]> = { masteryStates: ['mastery'], masteryEvents: ['delta', 'doTinCay'] };

/** CSDL giả: chỉ các lệnh applyMastery và loadConfig dùng (select … where [limit], insert … values, update … set … where). */
function taoDb(danhMuc: { ma_loi: Record<string, string | null>; buoc: Record<string, string | null> }, cauHinh: Hang) {
  const kho: Record<string, Hang[]> = {
    masteryConfig: [cauHinh],
    masteryStates: [],
    masteryEvents: [],
    escalations: [],
    errorTypes: Object.entries(danhMuc.ma_loi).map(([code, skillCode]) => ({ code, skillCode })),
    stepTemplates: Object.entries(danhMuc.buoc).map(([maBuoc, skillCode]) => ({ maBuoc, skillCode })),
  };
  const ghiVao = (ten: string, v: Hang): Hang => {
    const h = structuredClone(v);
    for (const c of COT_REAL[ten] ?? []) if (typeof h[c] === 'number') h[c] = real(h[c] as number);
    return h;
  };
  const cua = (t: Bang) => {
    const ten = t[TEN];
    if (!kho[ten]) throw new Error('applyMastery đụng bảng ngoài dự kiến: ' + ten);
    return { ten, hang: kho[ten] };
  };
  const db = {
    select: () => ({
      from: (t: Bang) => ({
        where: (dk: (h: Hang) => boolean) => {
          const ra = cua(t).hang.filter(dk).map((h) => structuredClone(h));
          return Object.assign(Promise.resolve(ra), { limit: async (n: number) => ra.slice(0, n) });
        },
      }),
    }),
    insert: (t: Bang) => ({
      values: async (v: Hang) => {
        const { ten, hang } = cua(t);
        hang.push(ten === 'escalations' ? { loai: 'KET', problemId: null, maBuoc: null, handledAt: null, ...ghiVao(ten, v) } : ghiVao(ten, v));
      },
    }),
    update: (t: Bang) => ({
      set: (v: Hang) => ({
        where: async (dk: (h: Hang) => boolean) => {
          const { ten, hang } = cua(t);
          for (const h of hang) if (dk(h)) Object.assign(h, ghiVao(ten, v));
        },
      }),
    }),
  };
  return { db, kho };
}

/** Cắt mã v0, bỏ kiểu, chạy trong vm với CSDL giả; mọi định danh tự do khác thiếu giả lập thì ReferenceError. */
function napV0(db: unknown) {
  const ln = doc(LEARNING);
  const hs = doc(HS);
  const goi = doan(hs, HS, 'if (!(laDauU(graded) && graded.toan_dung === true)) await applyMastery({', '\n  });\n');
  const ma = [
    doan(ln, LEARNING, 'const DEFAULT_CFG: BktConfig = ', '\n};\n'),
    doan(ln, LEARNING, 'export async function loadConfig(', '\n}\n').replace(/^export /, ''),
    doan(ln, LEARNING, 'export function bktNext(', '\n}\n').replace(/^export /, ''),
    doan(ln, LEARNING, 'function clamp(', '\n}\n'),
    doan(ln, LEARNING, 'export async function applyMastery(', '\n}\n').replace(/^export /, ''),
    doan(ln, LEARNING, 'const IDX = ', ';\n'),
    doan(ln, LEARNING, 'export function mucSauBai(', '\n}\n').replace(/^export /, ''),
    doan(hs, HS, 'function laDauU(', '\n}\n'),
    // Phần cuối nopBuoc của v0 sau khi chấm: cờ finished rồi điều kiện gọi applyMastery, nguyên văn.
    'async function sauKhiCham(user, subId, p, graded, lyDo, body) {',
    '  ' + doan(hs, HS, 'const finished = ', ';\n'),
    '  ' + goi,
    '  return ' + doan(goi, HS, '!(laDauU(graded)', 'true)') + ';',
    '}',
    '({ sauKhiCham })',
  ].join('\n');
  const js = stripTypeScriptTypes(ma, { mode: 'strip' });
  let soId = 0;
  const ngu = {
    db, MUC4, mucFromMastery,
    masteryConfig: bang('masteryConfig'), masteryStates: bang('masteryStates'), masteryEvents: bang('masteryEvents'),
    escalations: bang('escalations'), errorTypes: bang('errorTypes'), stepTemplates: bang('stepTemplates'),
    eq: (c: { cot: string }, v: unknown) => (h: Hang) => h[c.cot] === v,
    and: (...dk: ((h: Hang) => boolean)[]) => (h: Hang) => dk.every((d) => d(h)),
    crypto: { randomUUID: () => 'id-' + ++soId },
  };
  return vm.runInNewContext(js, ngu, { filename: 'v0 (learning.ts, hs.ts)' }) as {
    sauKhiCham(user: { id: string }, subId: string, p: { skillCode: string; mucDo4: string }, graded: Cham, lyDo: string | null,
      body: { nop_toi: string }): Promise<boolean>;
  };
}

// ---- Kịch bản ----------------------------------------------------------------------------------------------------

const dat = (): Cham => ({ ket_qua: 'DAT', buoc_sai: null, ma_loi: null, do_tin_cay: null, chua_xong: false });
const sai = (ma_loi: string | null = null, do_tin_cay: number | null = null, buoc: string | null = null, toan_dung?: boolean): Cham => ({
  ket_qua: 'SAI', buoc_sai: buoc ? { ma_buoc: buoc } : null, ma_loi, do_tin_cay, ...(toan_dung === undefined ? {} : { toan_dung }),
  chua_xong: false,
});
const kkd = (): Cham => ({ ket_qua: 'KHONG_KIEM_DUOC', buoc_sai: null, ma_loi: null, do_tin_cay: null, chua_xong: false });
const nop = (ky_nang: string, muc: Bai['muc'], cham: Cham, nghi = false): Buoc => ({ loai: 'nop', bai: { ky_nang, muc }, cham, nghi });
const XU_LY: Buoc = { loai: 'xu_ly_canh_bao' };
const [NB, TH, VD, VDC] = MUC4;

const VIET_TAY: { ma: string; mo_ta: string; buoc: Buoc[] }[] = [
  { ma: 'len_tung_nac_toi_vdc', mo_ta: 'SC-010: đúng ở bài mức TH, VD, VDC lên từng nấc tới Vận dụng cao rồi giữ', buoc: [
    nop('T12.DH.03', TH, dat()), nop('T12.DH.03', VD, dat()), nop('T12.DH.03', VDC, dat()), nop('T12.DH.03', VDC, dat())] },
  { ma: 'bai_de_khong_day_len', mo_ta: 'bài dễ hơn mức hiện tại không đẩy lên; bài đúng mức thì lên', buoc: [
    nop('T12.DH.01', NB, dat()), nop('T12.DH.01', NB, dat()), nop('T12.DH.01', NB, dat()), nop('T12.DH.01', TH, dat()),
    nop('T12.DH.01', TH, dat()), nop('T12.DH.01', VD, dat())] },
  { ma: 'ket_va_canh_bao', mo_ta: 'sai liền 3 lượt thì cảnh báo; đang mở thì không ghi thêm; xử lý rồi kẹt tiếp thì ghi mới; đúng thì về 0', buoc: [
    nop('T12.DH.05', TH, sai()), nop('T12.DH.05', TH, sai()), nop('T12.DH.05', TH, sai()), nop('T12.DH.05', TH, sai()), XU_LY,
    nop('T12.DH.05', TH, sai()), nop('T12.DH.05', TH, dat()), nop('T12.DH.05', TH, sai())] },
  { ma: 'chon_ky_nang', mo_ta: 'mã lỗi đủ tin cậy (kể cả đúng ngưỡng 0,65), dưới ngưỡng theo bước, mã lạ, bước ngoài khung', buoc: [
    nop('T12.DH.02', TH, sai('ERR.DH.01', 0.95, 'B.DH.DAOHAM')), nop('T12.DH.03', TH, sai('ERR.DH.06', 0.65, 'B.DH.XETDAU')),
    nop('T12.DH.05', TH, sai('ERR.DH.06', 0.6, 'B.DH.XETDAU')), nop('T12.DH.05', VD, sai('ERR.DH.03', 0.5, 'B.DH.NGHIEM')),
    nop('T12.DH.04', TH, sai('ERR.DH.99', 0.9, 'B.DH.KETLUAN')), nop('T12.DH.04', TH, sai(null, null, 'B.DH.TXD')),
    nop('T12.DH.04', TH, sai('ERR.DH.08', 0.6, 'B.DH.DOCBANG')), nop('T12.DH.06', VDC, sai('ERR.DH.08', 0.9, 'B.DH.KETLUAN')),
    nop('T12.DH.06', VDC, sai('', 0.9, null))] },
  { ma: 'khong_tinh', mo_ta: 'KHONG_KIEM_DUOC và lỗi trình bày dấu U không tính; dấu U sai toán thì tính', buoc: [
    nop('T12.DH.03', VD, kkd()), nop('T12.DH.03', VD, sai('ERR.DH.07', 0.9, 'B.DH.KETLUAN', true)),
    nop('T12.DH.03', VD, sai('ERR.DH.07', 0.9, 'B.DH.KETLUAN', false)), nop('T12.DH.03', VD, dat()),
    nop('T12.DH.03', VD, kkd())] },
  { ma: 'nghi_doan_mo', mo_ta: 'nghi đoán mò: mức, mastery, số lượt, kẹt giữ nguyên, mã lỗi vẫn ghi, không cảnh báo', buoc: [
    nop('T12.DH.03', TH, dat(), true), nop('T12.DH.03', TH, sai('ERR.DH.06', 0.9, 'B.DH.XETDAU'), true),
    nop('T12.DH.03', TH, sai()), nop('T12.DH.03', TH, sai()), nop('T12.DH.03', TH, sai('ERR.DH.05', 0.5), true),
    nop('T12.DH.03', TH, sai()), nop('T12.DH.03', TH, dat(), true), nop('T12.DH.03', TH, dat())] },
  { ma: 'tam_ma_loi_cuoi', mo_ta: 'mã lỗi gần đây: không trùng, mã đã có giữ chỗ, giữ 8 mã cuối', buoc:
    ['01', '02', '03', '01', '04', '05', '06', '07', '08', '09', '02', '10', '11'].map((s) => nop('T12.DH.04', TH, sai('ERR.DH.' + s, 0.5))) },
  { ma: 'tut_muc', mo_ta: 'lên Vận dụng cao rồi sai liền: xuống từng nấc, rời Vận dụng cao thì hết hoàn thành', buoc: [
    nop('T12.DH.05', TH, dat()), nop('T12.DH.05', VD, dat()), nop('T12.DH.05', VDC, dat()),
    ...Array.from({ length: 7 }, () => nop('T12.DH.05', VDC, sai())), nop('T12.DH.05', VDC, dat()), nop('T12.DH.05', VDC, dat())] },
];

/** mulberry32: dãy giả ngẫu nhiên cố định theo hạt, để tệp vàng sinh lại giống từng byte. */
function ngauNhien(hat: number) {
  let a = hat;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function kichBanNgauNhien(hat: number, maLoi: string[], buoc: string[]): Buoc[] {
  const r = ngauNhien(hat);
  const chon = <T,>(xs: readonly T[]) => xs[Math.floor(r() * xs.length)];
  const kyNang = ['T12.DH.01', 'T12.DH.02', 'T12.DH.03', 'T12.DH.05'];
  const tinCay = [null, 0.5, 0.6, 0.65, 0.7, 0.72, 0.8, 0.95];
  return Array.from({ length: 40 }, (): Buoc => {
    const x = r();
    const bai = (cham: Cham, nghi = false) => nop(chon(kyNang), chon(MUC4), cham, nghi);
    if (x < 0.45) return bai(dat());
    if (x < 0.8) return bai(sai(r() < 0.8 ? chon(maLoi) : null, chon(tinCay), r() < 0.8 ? chon(buoc) : null));
    if (x < 0.85) return bai(kkd());
    if (x < 0.9) return bai(sai('ERR.DH.07', chon(tinCay), 'B.DH.KETLUAN', r() < 0.5));
    if (x < 0.96) return bai(r() < 0.5 ? dat() : sai(chon(maLoi), chon(tinCay), chon(buoc)), true);
    return XU_LY;
  });
}

// ---- Chạy ----------------------------------------------------------------------------------------------------------

const nguon = THU ? { thu: 'chạy thử, nguồn chưa kiểm' } : { git: nguonGit(GOC, NGUON) };
const cauHinh = JSON.parse(doc(BKT)) as Hang;
const dongCsv = doc(MA_LOI).split(/\r?\n/).filter(Boolean);
const [iMa, iKn] = ['ma_loi', 'ky_nang_chinh'].map((c) => dongCsv[0].split(',').indexOf(c));
if (iMa !== 0 || iKn !== 1) throw new Error(MA_LOI + ': mã lỗi và kỹ năng phải là hai cột đầu (cột sau có ngoặc kép)');
const danhMuc = {
  ma_loi: Object.fromEntries(dongCsv.slice(1).map((d) => d.split(',')).filter((c) => c[0]).map((c) => [c[0], c[1] || null])),
  buoc: Object.fromEntries((JSON.parse(doc(KHUNG)) as { maBuoc: string; skillCode: string | null }[]).map((b) => [b.maBuoc, b.skillCode || null])),
};
const kichBan = [
  ...VIET_TAY,
  ...[11, 22, 33, 44, 55, 66].map((hat) => ({ ma: 'ngau_nhien_' + hat, mo_ta: 'dãy 40 lượt giả ngẫu nhiên, hạt ' + hat,
    buoc: kichBanNgauNhien(hat, Object.keys(danhMuc.ma_loi), Object.keys(danhMuc.buoc).concat('B.DH.DOCBANG')) })),
];

const ket: unknown[] = [];
const dem = { nop: 0, tinh: 0, khong_tinh: 0, canh_bao: 0, theo_luat: {} as Record<string, number>, muc_doi: 0 };
for (const kb of kichBan) {
  const { db, kho } = taoDb(danhMuc, cauHinh);
  const v0 = napV0(db);
  const hs = { id: 'hoc-sinh-' + kb.ma };
  const daGhi = [];
  for (const [i, b] of kb.buoc.entries()) {
    if (b.loai === 'xu_ly_canh_bao') {
      for (const e of kho.escalations) if (e.studentId === hs.id && !e.handledAt) e.handledAt = 'đã xử lý';
      daGhi.push({ loai: b.loai });
      continue;
    }
    const truoc = { suKien: kho.masteryEvents.length, canhBao: kho.escalations.length, trangThai: structuredClone(kho.masteryStates) };
    const tinh = await v0.sauKhiCham(hs, `bai-lam-${kb.ma}-${i}`, { skillCode: b.bai.ky_nang, mucDo4: b.bai.muc }, b.cham,
      b.nghi ? 'Ô bị đổi nhiều lần trước khi nộp' : null, { nop_toi: 'B.DH.KETLUAN' });
    const suKien = kho.masteryEvents.slice(truoc.suKien);
    if (suKien.length > 1) throw new Error(`${kb.ma}#${i}: một lần nộp ghi ${suKien.length} sự kiện`);
    const sk = suKien[0] ?? null;
    if (Boolean(sk) !== (tinh && b.cham.ket_qua !== 'KHONG_KIEM_DUOC')) throw new Error(`${kb.ma}#${i}: sự kiện lệch điều kiện gọi`);
    const mucTruoc = sk ? (truoc.trangThai.find((h) => h.skillCode === sk.skillCode)?.currentMucDo4 ?? null) : null;
    const trangThai = kho.masteryStates.filter((h) => h.studentId === hs.id)
      .map(({ studentId: _, ...h }) => h).sort((a, b2) => String(a.skillCode).localeCompare(String(b2.skillCode)));
    const sau = sk ? trangThai.find((h) => h.skillCode === sk.skillCode) : undefined;
    const canhBao = kho.escalations.slice(truoc.canhBao).map((e) => ({ skillCode: e.skillCode, reason: e.reason, loai: e.loai }));
    dem.nop++;
    if (sk) {
      dem.tinh++;
      dem.theo_luat[sk.ruleApplied as string] = (dem.theo_luat[sk.ruleApplied as string] ?? 0) + 1;
      if (mucTruoc !== null && mucTruoc !== sau?.currentMucDo4) dem.muc_doi++;
    } else dem.khong_tinh++;
    dem.canh_bao += canhBao.length;
    daGhi.push({
      loai: b.loai, bai: b.bai, cham: b.cham, nghi: b.nghi,
      su_kien: sk && { skillCode: sk.skillCode, delta: sk.delta, ruleApplied: sk.ruleApplied, buocSai: sk.buocSai ?? null,
        maLoi: sk.maLoi ?? null, doTinCay: sk.doTinCay ?? null, nghiDoanMo: sk.nghiDoanMo },
      muc_truoc: mucTruoc,
      canh_bao: canhBao,
      trang_thai: trangThai,
    });
  }
  ket.push({ ma: kb.ma, mo_ta: kb.mo_ta, buoc: daGhi });
}

const ra = THU ? path.resolve(THU) : path.join(GOC, DOI_CHIEU, 'bkt-v0.json');
writeFileSync(ra, JSON.stringify({ nguon, cau_hinh: cauHinh, danh_muc: danhMuc, dem, kich_ban: ket }, null, 1) + '\n', 'utf8');
console.log(`đã ghi ${kichBan.length} kịch bản, ${dem.nop} lần nộp (${dem.tinh} tính, ${dem.khong_tinh} không tính), `
  + `${dem.canh_bao} cảnh báo, ${dem.muc_doi} lần đổi mức vào ${path.relative(GOC, ra)}`);
console.log('theo luật:', dem.theo_luat);
