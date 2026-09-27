/** Kho lớp cho gia sư: truy hồi có trích dẫn, không lời giải. Kong et al. IJCAI 2026 D2; Lewis et al. 2020. */

export const TU_KHOA_TAI_LIEU = ["đồng biến", "nghịch biến", "cực trị", "đạo hàm", "xét dấu"] as const;
export const TU_KHOA_CONG_THUC = ["đồng biến", "nghịch biến", "cực đại", "cực tiểu", "đạo hàm"] as const;

export const TU_KHOA_BUOC: Record<string, string[]> = {
  "B.DH.TXD": ["tập xác định", "xác định", "chia", "căn", "log"],
  "B.DH.DAOHAM": ["đạo hàm", "lũy thừa", "tổng", "thương", "hằng số"],
  "B.DH.NGHIEM": ["nghiệm", "tới hạn", "không xác định", "y' = 0"],
  "B.DH.XETDAU": ["xét dấu", "bảng", "khoảng", "thay số"],
  "B.DH.KETLUAN": ["đồng biến", "nghịch biến", "cực đại", "cực tiểu", "đổi dấu"],
};

export type MauTaiLieu = {
  id: string;
  title: string;
  text: string;
  licenseStatus: string;
  version: number;
};

export type MauCongThuc = {
  id: string;
  title: string;
  latex: string;
  noiDung: string;
};

export type TrichDanKho = {
  loai: "tai_lieu" | "cong_thuc";
  id: string;
  ten: string;
  trich: string;
};

export type KhoGoi = {
  taiLieu: TrichDanKho[];
  congThuc: TrichDanKho[];
};

function khongDau(s: string) {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d");
}

export function trichDoan(text: string, term: string, radius = 90) {
  const hay = text.toLowerCase();
  const needle = term.toLowerCase();
  const i = hay.indexOf(needle);
  if (i < 0) return text.slice(0, 160).trim();
  const a = Math.max(0, i - radius);
  const b = Math.min(text.length, i + term.length + radius);
  return text.slice(a, b).trim();
}

export function chamDiemVanBan(text: string, terms: string[]) {
  const hay = text.toLowerCase();
  const fold = khongDau(text);
  let n = 0;
  for (const t of terms) {
    if (hay.includes(t.toLowerCase()) || fold.includes(khongDau(t))) n += 1;
  }
  return n;
}

export function chonKho(opts: {
  taiLieu: MauTaiLieu[];
  congThuc: MauCongThuc[];
  maBuoc?: string;
  cauHoi?: string;
}): KhoGoi {
  const extra = [
    ...TU_KHOA_TAI_LIEU,
    ...(TU_KHOA_BUOC[opts.maBuoc || ""] || []),
    ...String(opts.cauHoi || "")
      .toLowerCase()
      .split(/[^\p{L}\p{N}']+/u)
      .filter((w) => w.length >= 4)
      .slice(0, 8),
  ];
  const docs = opts.taiLieu
    .filter((d) => d.licenseStatus !== "chua_ro")
    .map((d) => {
      const score = chamDiemVanBan(d.text, extra);
      const hit = extra.find((t) => d.text.toLowerCase().includes(t.toLowerCase())) || extra[0];
      return {
        score,
        item: {
          loai: "tai_lieu" as const,
          id: d.id,
          ten: d.title,
          trich: trichDoan(d.text, hit),
        },
      };
    })
    .filter((x) => x.score >= 2)
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
    .map((x) => x.item);

  const cts = opts.congThuc
    .map((c) => {
      const blob = `${c.title} ${c.latex} ${c.noiDung}`;
      const score = chamDiemVanBan(blob, [...TU_KHOA_CONG_THUC, ...(TU_KHOA_BUOC[opts.maBuoc || ""] || [])]);
      return {
        score,
        item: {
          loai: "cong_thuc" as const,
          id: c.id,
          ten: c.title,
          trich: c.noiDung.slice(0, 180),
        },
      };
    })
    .filter((x) => x.score >= 1)
    .sort((a, b) => b.score - a.score)
    .slice(0, 4)
    .map((x) => x.item);

  return { taiLieu: docs, congThuc: cts };
}

export function dongKhoChoPrompt(kho: KhoGoi) {
  const dong: string[] = [];
  for (const c of kho.congThuc) dong.push(`Công thức «${c.ten}»: ${c.trich}`);
  for (const d of kho.taiLieu) dong.push(`Tài liệu «${d.ten}»: ${d.trich}`);
  if (!dong.length) return "(Kho lớp chưa khớp đoạn nào — chỉ dùng thang gợi ý, không bịa công thức.)";
  return dong.join("\n").slice(0, 1200);
}

export function nhanTrichDan(kho: KhoGoi) {
  return [...kho.congThuc, ...kho.taiLieu].map((x) => ({ loai: x.loai, ten: x.ten }));
}
