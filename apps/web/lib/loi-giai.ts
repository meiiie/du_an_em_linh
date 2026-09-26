type BaiLam = {
  TXD?: string;
  dao_ham?: string;
  y_phay_bang_0?: string[];
  y_phay_khong_xd?: string[];
  ket_luan?: {
    dong_bien?: string[];
    nghich_bien?: string[];
    cuc_dai_x?: string[];
    cuc_tieu_x?: string[];
    gia_tri_cuc_dai?: string[];
    gia_tri_cuc_tieu?: string[];
  };
};

export function loiGiaiHocSinh(baiLam: unknown, finalAnswer?: string | null): string | null {
  if (finalAnswer) return finalAnswer;
  if (!baiLam || typeof baiLam !== "object") return null;
  const bl = baiLam as BaiLam;
  const kl = bl.ket_luan || {};
  const lines: string[] = [];
  if (bl.TXD) lines.push(`Tập xác định: ${bl.TXD}.`);
  if (bl.dao_ham) lines.push(`Đạo hàm: ${bl.dao_ham}.`);
  if (bl.y_phay_bang_0?.length) lines.push(`y′ = 0 tại ${bl.y_phay_bang_0.join(", ")}.`);
  if (bl.y_phay_khong_xd?.length) lines.push(`y′ không xác định tại ${bl.y_phay_khong_xd.join(", ")}.`);
  if (kl.dong_bien?.length) lines.push(`Đồng biến trên ${kl.dong_bien.join(" và ")}.`);
  else if ("dong_bien" in kl) lines.push("Không đồng biến trên khoảng nào.");
  if (kl.nghich_bien?.length) lines.push(`Nghịch biến trên ${kl.nghich_bien.join(" và ")}.`);
  else if ("nghich_bien" in kl) lines.push("Không nghịch biến trên khoảng nào.");
  if (kl.cuc_dai_x?.length) {
    const ys = kl.gia_tri_cuc_dai || [];
    lines.push(`Cực đại tại ${kl.cuc_dai_x.map((x, i) => (ys[i] ? `x = ${x}, y = ${ys[i]}` : `x = ${x}`)).join("; ")}.`);
  } else if ("cuc_dai_x" in kl) lines.push("Không có cực đại.");
  if (kl.cuc_tieu_x?.length) {
    const ys = kl.gia_tri_cuc_tieu || [];
    lines.push(`Cực tiểu tại ${kl.cuc_tieu_x.map((x, i) => (ys[i] ? `x = ${x}, y = ${ys[i]}` : `x = ${x}`)).join("; ")}.`);
  } else if ("cuc_tieu_x" in kl) lines.push("Không có cực tiểu.");
  return lines.length ? lines.join(" ") : null;
}
