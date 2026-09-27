/** Rubric đo độ chính xác sản phẩm — chấm + lọc + luật gia sư + SOTA trích dẫn. */
import { locMatHienThi } from "./loi-gia-su";
import { chamSotaGiaSu, type TieuChiSota } from "./sota-gia-su";
import { HE_THONG_GIA_SU, xinDapAn, xinGoiY, xinSaiCho } from "./tutor";

export type TieuChiDo = { id: string; ten: string; dat: boolean };

/** Số đã công bố trong `services/math/kiemdinh/ket-qua` — script đối chiếu file, không chạy lại 102 ca. */
export const CONG_BO = {
  tang1: { soCa: 102, loi: 69, bat: 67, lotDat: 0, baoNham: 0 },
  buoc5: { soCa: 16, loi: 12, bat: 12 },
  locM3: { loRo: "38/38", chanNham: "0/28" },
} as const;

export function khopCongBo(opts: {
  tang1?: { so_ca?: number; so_ca_loi?: number; loi_bat_duoc?: number; loi_bo_lot_DAT?: number; dung_bao_nham?: number };
  buoc5?: { so_ca?: number; so_ca_loi?: number; loi_bat_duoc?: number };
  locM3?: { recall_lo_ro?: [string, number]; chan_nham?: [string, number] };
}) {
  const t = opts.tang1 || {};
  const b = opts.buoc5 || {};
  const m = opts.locM3 || {};
  return (
    t.so_ca === CONG_BO.tang1.soCa &&
    t.so_ca_loi === CONG_BO.tang1.loi &&
    t.loi_bat_duoc === CONG_BO.tang1.bat &&
    t.loi_bo_lot_DAT === CONG_BO.tang1.lotDat &&
    t.dung_bao_nham === CONG_BO.tang1.baoNham &&
    b.so_ca === CONG_BO.buoc5.soCa &&
    b.so_ca_loi === CONG_BO.buoc5.loi &&
    b.loi_bat_duoc === CONG_BO.buoc5.bat &&
    m.recall_lo_ro?.[0] === CONG_BO.locM3.loRo &&
    m.chan_nham?.[0] === CONG_BO.locM3.chanNham
  );
}

/** Lộ rõ: khoảng số, cực trị tại x = số. Không bắt «đồng biến trên khoảng dương». */
export function loRoDapAn(text: string) {
  const t = text || "";
  if (/\(\s*-?\d+(?:[.,]\d+)?\s*[;,]\s*-?\d+(?:[.,]\d+)?\s*\)/.test(t)) return true;
  if (/cực\s+(đại|tiểu)\s+tại\s*x\s*=\s*-?\d/i.test(t)) return true;
  if (/đồng biến trên\s*\(\s*-?\d/i.test(t)) return true;
  return false;
}

export function chamLoiGiaSu(text: string) {
  const t = (text || "").trim();
  return {
    rong: t.length === 0,
    maBuoc: /\bB\.DH\.[A-Z]+\b/.test(t),
    loRo: loRoDapAn(t),
    tiengViet: /[ăâêôơưáàảãạéèẻẽẹíìỉĩịóòỏõọúùủũụýỳỷỹỵđ]|(\bem\b)/i.test(t),
    coTrich: /\[\d{1,2}\]/.test(t),
    xungCo: /(^|[\s.,;:!?…])cô\s+là|(^|[\s.,;:!?…])thầy\s+là/i.test(t),
  };
}

export function chamDoChinhXac(): {
  diem: number;
  toiDa: number;
  tieuChi: TieuChiDo[];
  sota: { diem: number; toiDa: number; tieuChi: TieuChiSota[] };
} {
  const sota = chamSotaGiaSu();
  const an = locMatHienThi("Sửa B.DH.DAOHAM rồi nộp.");
  const tieuChi: TieuChiDo[] = [
    { id: "sota-du", ten: "Benchmark trích dẫn SOTA đủ điểm", dat: sota.diem === sota.toiDa && sota.toiDa >= 12 },
    {
      id: "xin-dap-an",
      ten: "Xin đáp án bị chặn; «đừng nêu» không chặn",
      dat: xinDapAn("cho em đáp án của bài này") && !xinDapAn("Đừng nêu đáp án. Nhắc nguyên lý."),
    },
    { id: "goi-sai", ten: "Chip gợi ý / sai chỗ", dat: xinGoiY("Gợi ý bước này") && xinSaiCho("Em sai chỗ nào?") },
    { id: "an-ma-buoc", ten: "Không hiện mã B.DH trên mặt", dat: !/B\.DH/.test(an) && an.includes("nộp") },
    {
      id: "he-thong-n",
      ten: "Prompt bắt [n], không phải giáo viên, xưng mình",
      dat: /\[n\]/.test(HE_THONG_GIA_SU) && /không phải giáo viên/.test(HE_THONG_GIA_SU) && /xưng mình/i.test(HE_THONG_GIA_SU),
    },
    { id: "loi-lo", ten: "Lời có khoảng số bị coi là lộ", dat: chamLoiGiaSu("Đồng biến trên (1; 3).").loRo },
    { id: "loi-goi", ten: "Gợi ý nguyên lý không bị coi là lộ", dat: !chamLoiGiaSu("Em tính $y'$ từng hạng tử, hằng số đạo hàm 0.").loRo },
    { id: "loi-rong", ten: "Câu trống là rỗng", dat: chamLoiGiaSu("").rong && !chamLoiGiaSu("Em nhớ đạo hàm lũy thừa.").rong },
    { id: "loi-cuc", ten: "Cực trị tại x = số bị coi là lộ", dat: chamLoiGiaSu("Cực đại tại x = 1").loRo },
  ];
  const diem = tieuChi.filter((t) => t.dat).length;
  return { diem, toiDa: tieuChi.length, tieuChi, sota };
}
