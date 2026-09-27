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

export type TrichDanHien = Pick<TrichDanKho, "loai" | "id" | "ten" | "trich"> & {
  so?: number;
  dung?: boolean;
};

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
  nhoId?: string[];
}): KhoGoi {
  const buoc = TU_KHOA_BUOC[opts.maBuoc || ""] || [];
  const hoi = tuKhoaHoi(opts.cauHoi);
  const nho = new Set((opts.nhoId || []).filter(Boolean));
  const extraTai = [...TU_KHOA_TAI_LIEU, ...buoc, ...hoi];

  const docs = opts.taiLieu
    .filter((d) => d.licenseStatus !== "chua_ro")
    .map((d) => {
      const score =
        chamDiemVanBan(d.text, TU_KHOA_TAI_LIEU) +
        chamDiemVanBan(d.text, buoc) * 2 +
        chamDiemVanBan(d.text, hoi) +
        (nho.has(d.id) ? 2 : 0);
      return {
        score,
        item: {
          loai: "tai_lieu" as const,
          id: d.id,
          ten: d.title,
          trich: trichDoan(d.text, chonHit(d.text, extraTai), 120),
        },
      };
    })
    .filter((x) => x.score >= 2)
    .sort((a, b) => b.score - a.score)
    .slice(0, 2)
    .map((x) => x.item);

  const extraCt = [...TU_KHOA_CONG_THUC, ...buoc, ...hoi];
  const cts = opts.congThuc
    .map((c) => {
      const blob = `${c.title} ${c.latex} ${c.noiDung}`;
      const score =
        chamDiemVanBan(blob, TU_KHOA_CONG_THUC) +
        chamDiemVanBan(blob, buoc) * 2 +
        chamDiemVanBan(blob, hoi) +
        (nho.has(c.id) ? 2 : 0);
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
    .slice(0, 2)
    .map((x) => x.item);

  return { taiLieu: docs, congThuc: cts };
}

/** Cùng thứ tự và số [n] với prompt — tối đa 2 công thức + 1 tài liệu. */
export function nhanTrichDan(kho: KhoGoi): TrichDanHien[] {
  return [...kho.congThuc.slice(0, 2), ...kho.taiLieu.slice(0, 1)].slice(0, 3).map((x, i) => ({
    loai: x.loai,
    id: x.id,
    ten: x.ten,
    trich: x.trich,
    so: i + 1,
  }));
}

export function dongKhoChoPrompt(kho: KhoGoi) {
  const dan = nhanTrichDan(kho);
  if (!dan.length) return "(Kho lớp chưa khớp đoạn nào — chỉ dùng thang gợi ý, không bịa công thức.)";
  return dan
    .map((x) => `[${x.so}] ${x.loai === "tai_lieu" ? "Tài liệu" : "Công thức"} «${x.ten}»: ${x.trich}`)
    .join("\n");
}

export function soTrichDanTrongLoi(loi: string) {
  return [...String(loi || "").matchAll(/\[(\d{1,2})\]/g)].map((m) => Number(m[1]));
}

/** Ưu tiên mục model viết [n] hoặc nhắc tên; không có thì giữ tập đã mở. */
export function locTrichDanTheoLoi(loi: string, dan: TrichDanHien[]): TrichDanHien[] {
  if (!dan.length) return [];
  const so = new Set(soTrichDanTrongLoi(loi));
  const theoSo = dan.filter((x) => x.so != null && so.has(x.so));
  if (theoSo.length) return theoSo.map((x) => ({ ...x, dung: true }));
  const fold = khongDau(loi);
  const theoTen = dan.filter((x) => x.ten && fold.includes(khongDau(x.ten)));
  if (theoTen.length) return theoTen.map((x) => ({ ...x, dung: true }));
  return dan.map((x) => ({ ...x, dung: false }));
}

/** [n] → liên kết kho; không đụng $$ và không đụng [n](url) đã có. */
export function ganNeoTrongLoi(loi: string, dan: TrichDanHien[]) {
  if (!dan.length) return loi || "";
  const bySo = new Map(dan.filter((x) => x.so != null).map((x) => [x.so as number, x]));
  return String(loi || "")
    .split("$$")
    .map((part, i) => {
      if (i % 2 === 1) return part;
      return part.replace(/\[(\d{1,2})\](?!\()/g, (m, n: string) => {
        const t = bySo.get(Number(n));
        return t ? `[${n}](${duongKhoTrichDan(t)})` : m;
      });
    })
    .join("$$");
}

export function docTrichDanLuu(raw: unknown): TrichDanHien[] {
  if (!Array.isArray(raw)) return [];
  return raw
    .filter((x) => x && typeof x === "object")
    .map((x) => {
      const o = x as Partial<TrichDanHien>;
      const loai: TrichDanHien["loai"] = o.loai === "tai_lieu" ? "tai_lieu" : "cong_thuc";
      const so = typeof o.so === "number" && o.so >= 1 && o.so <= 9 ? o.so : undefined;
      return {
        loai,
        id: String(o.id || ""),
        ten: String(o.ten || ""),
        trich: String(o.trich || ""),
        so,
        dung: o.dung === true,
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
