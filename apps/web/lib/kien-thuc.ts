/** Kho lớp cho gia sư: truy hồi có trích dẫn, không lời giải. Kong et al. IJCAI 2026 D2; Lewis et al. 2020; KITE BEA 2026. */

export const TU_KHOA_TAI_LIEU = ["đồng biến", "nghịch biến", "cực trị", "đạo hàm", "xét dấu"] as const;
export const TU_KHOA_CONG_THUC = ["đồng biến", "nghịch biến", "cực đại", "cực tiểu", "đạo hàm"] as const;

export const TU_KHOA_BUOC: Record<string, string[]> = {
  "B.DH.TXD": ["tập xác định", "xác định", "chia", "căn", "log"],
  "B.DH.DAOHAM": ["đạo hàm", "lũy thừa", "tổng", "thương", "hằng số"],
  "B.DH.NGHIEM": ["nghiệm", "tới hạn", "không xác định", "y' = 0"],
  "B.DH.XETDAU": ["xét dấu", "bảng", "khoảng", "thay số"],
  "B.DH.KETLUAN": ["đồng biến", "nghịch biến", "cực đại", "cực tiểu", "đổi dấu"],
};

export const KHUNG_NAM_BUOC = [
  { ma: "B.DH.TXD", ten: "Tập xác định" },
  { ma: "B.DH.DAOHAM", ten: "Đạo hàm" },
  { ma: "B.DH.NGHIEM", ten: "Nghiệm y′" },
  { ma: "B.DH.XETDAU", ten: "Xét dấu" },
  { ma: "B.DH.KETLUAN", ten: "Kết luận" },
] as const;

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

export type TrichDanHien = Pick<TrichDanKho, "loai" | "id" | "ten" | "trich">;

export function duongKhoTrichDan(t: Pick<TrichDanHien, "loai" | "id">) {
  return t.loai === "tai_lieu" ? `/hs/kho?muc=lieu#tl-${t.id}` : `/hs/kho#ct-${t.id}`;
}

export type KhoGoi = {
  taiLieu: TrichDanKho[];
  congThuc: TrichDanKho[];
};

export function khongDau(s: string) {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d");
}

export function vanBanKhop(text: string, term: string) {
  return text.toLowerCase().includes(term.toLowerCase()) || khongDau(text).includes(khongDau(term));
}

function tuKhoaHoi(cauHoi?: string) {
  return String(cauHoi || "")
    .toLowerCase()
    .split(/[^\p{L}\p{N}']+/u)
    .filter((w) => w.length >= 4)
    .slice(0, 8);
}

export function trichDoan(text: string, term: string, radius = 90) {
  const hay = text.toLowerCase();
  const needle = term.toLowerCase();
  let i = hay.indexOf(needle);
  if (i < 0) {
    const fold = khongDau(text);
    const j = fold.indexOf(khongDau(term));
    if (j < 0) return text.slice(0, 160).trim();
    const ratio = text.length / Math.max(fold.length, 1);
    i = Math.min(Math.max(0, text.length - 1), Math.round(j * ratio));
  }
  const a = Math.max(0, i - radius);
  const b = Math.min(text.length, i + Math.max(term.length, 1) + radius);
  return text.slice(a, b).trim();
}

export function chamDiemVanBan(text: string, terms: readonly string[]) {
  let n = 0;
  for (const t of terms) {
    if (vanBanKhop(text, t)) n += 1;
  }
  return n;
}

function chonHit(text: string, terms: readonly string[]) {
  return terms.find((t) => vanBanKhop(text, t)) || terms[0] || "";
}

export function chonKho(opts: {
  taiLieu: MauTaiLieu[];
  congThuc: MauCongThuc[];
  maBuoc?: string;
  cauHoi?: string;
}): KhoGoi {
  const buoc = TU_KHOA_BUOC[opts.maBuoc || ""] || [];
  const hoi = tuKhoaHoi(opts.cauHoi);
  const extraTai = [...TU_KHOA_TAI_LIEU, ...buoc, ...hoi];

  const docs = opts.taiLieu
    .filter((d) => d.licenseStatus !== "chua_ro")
    .map((d) => {
      const score = chamDiemVanBan(d.text, TU_KHOA_TAI_LIEU) + chamDiemVanBan(d.text, buoc) * 2 + chamDiemVanBan(d.text, hoi);
      return {
        score,
        item: {
          loai: "tai_lieu" as const,
          id: d.id,
          ten: d.title,
          trich: trichDoan(d.text, chonHit(d.text, extraTai)),
        },
      };
    })
    .filter((x) => x.score >= 2)
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
    .map((x) => x.item);

  const extraCt = [...TU_KHOA_CONG_THUC, ...buoc, ...hoi];
  const cts = opts.congThuc
    .map((c) => {
      const blob = `${c.title} ${c.latex} ${c.noiDung}`;
      const score = chamDiemVanBan(blob, TU_KHOA_CONG_THUC) + chamDiemVanBan(blob, buoc) * 2 + chamDiemVanBan(blob, hoi);
      return {
        score,
        item: {
          loai: "cong_thuc" as const,
          id: c.id,
          ten: c.title,
          trich: trichDoan(c.noiDung || c.title, chonHit(blob, extraCt)),
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
  let n = 0;
  for (const c of kho.congThuc) dong.push(`[${++n}] Công thức «${c.ten}»: ${c.trich}`);
  for (const d of kho.taiLieu) dong.push(`[${++n}] Tài liệu «${d.ten}»: ${d.trich}`);
  if (!dong.length) return "(Kho lớp chưa khớp đoạn nào — chỉ dùng thang gợi ý, không bịa công thức.)";
  return dong.join("\n").slice(0, 1200);
}

export function nhanTrichDan(kho: KhoGoi): TrichDanHien[] {
  return [...kho.congThuc, ...kho.taiLieu].map((x) => ({
    loai: x.loai,
    id: x.id,
    ten: x.ten,
    trich: x.trich,
  }));
}

export function docTrichDanLuu(raw: unknown): TrichDanHien[] {
  if (!Array.isArray(raw)) return [];
  return raw
    .filter((x) => x && typeof x === "object")
    .map((x) => {
      const o = x as Partial<TrichDanHien>;
      const loai: TrichDanHien["loai"] = o.loai === "tai_lieu" ? "tai_lieu" : "cong_thuc";
      return {
        loai,
        id: String(o.id || ""),
        ten: String(o.ten || ""),
        trich: String(o.trich || ""),
      };
    })
    .filter((x) => x.ten);
}

export function xemKhoTheoKhung(nguon: { taiLieu: MauTaiLieu[]; congThuc: MauCongThuc[] }) {
  return KHUNG_NAM_BUOC.map((b) => {
    const kho = chonKho({ ...nguon, maBuoc: b.ma });
    return { ma: b.ma, ten: b.ten, congThuc: kho.congThuc, taiLieu: kho.taiLieu };
  });
}
