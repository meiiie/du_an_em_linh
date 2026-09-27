/** Chạy local: thẻ in-repo + chấm/lọc HTTP + công bố kiemdinh + (nếu có khóa) vài lượt Z.AI. Không in khóa. */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { chamDoChinhXac, chamLoiGiaSu, khopCongBo } from "../lib/do-chinh-xac";
import { completeChat } from "../lib/ai-harness";
import { HE_THONG_GIA_SU } from "../lib/tutor";
import { mathJob } from "../lib/math";

const MATH = process.env.MATH_SERVICE_URL || "http://127.0.0.1:8000";
const LIVE = (process.env.LIVE_URL || "https://hoc-toan-ai.onrender.com").replace(/\/$/, "");
const HAM = "x**3 - 6*x**2 + 9*x + 2";
const here = dirname(fileURLToPath(import.meta.url));

type SuKien = { loai: string; gia_tri: string };

function docKiemdinh(ten: string) {
  const candidates = [
    join(process.cwd(), "services/math/kiemdinh/ket-qua", ten),
    join(process.cwd(), "../../services/math/kiemdinh/ket-qua", ten),
    join(here, "../../../services/math/kiemdinh/ket-qua", ten),
  ];
  for (const p of candidates) {
    if (existsSync(p)) return JSON.parse(readFileSync(p, "utf8")) as Record<string, unknown>;
  }
  return null;
}

async function chamToan() {
  const dung = await mathJob<{ ket_qua?: string }>("grade", {
    ham: HAM,
    nop_toi: "B.DH.TXD",
    cac_buoc: [{ ma_buoc: "B.DH.TXD", cac_dong: [{ dong: 0, latex: "\\mathbb{R}" }] }],
  });
  const sai = await mathJob<{ ket_qua?: string; buoc_sai?: { ma_buoc?: string; dong?: number } }>("grade", {
    ham: HAM,
    nop_toi: "B.DH.DAOHAM",
    cac_buoc: [
      { ma_buoc: "B.DH.TXD", cac_dong: [{ dong: 0, latex: "\\mathbb{R}" }] },
      { ma_buoc: "B.DH.DAOHAM", cac_dong: [{ dong: 0, latex: "3x^{2}-12x" }] },
    ],
  });
  const may = await mathJob<{
    dat?: boolean;
    payload_cham?: unknown;
    su_kien?: SuKien[];
  }>("solve", { ham: HAM });
  const full = may.payload_cham
    ? await mathJob<{ ket_qua?: string }>("grade", may.payload_cham)
    : { ket_qua: "KHONG_KIEM_DUOC" };
  const suKien = may.su_kien?.length
    ? may.su_kien
    : ([
        { loai: "DB", gia_tri: "(1;3)" },
        { loai: "DCD", gia_tri: "1" },
      ] satisfies SuKien[]);
  const lo = await mathJob<{ cho_phep?: boolean }>("filter", {
    ban_nhap: "Hàm đồng biến trên (1; 3).",
    su_kien: suKien,
  });
  const cuc = await mathJob<{ cho_phep?: boolean }>("filter", {
    ban_nhap: "Cực đại tại x = 1.",
    su_kien: suKien,
  });
  const goi = await mathJob<{ cho_phep?: boolean }>("filter", {
    ban_nhap: "Em tính y' từng hạng tử, hằng số có đạo hàm 0.",
    su_kien: suKien,
  });
  const goi2 = await mathJob<{ cho_phep?: boolean }>("filter", {
    ban_nhap: "Hạ bậc từng hạng tử rồi lập bảng xét dấu.",
    su_kien: suKien,
  });
  return {
    math: MATH,
    txdDat: dung.ket_qua === "DAT",
    dhSai: sai.ket_qua === "SAI" && sai.buoc_sai?.ma_buoc === "B.DH.DAOHAM",
    namBuocDat: may.dat === true && full.ket_qua === "DAT",
    locChanLo: lo.cho_phep === false,
    locChanCuc: cuc.cho_phep === false,
    locChoGoi: goi.cho_phep === true && goi2.cho_phep === true,
  };
}

function chamCongBo() {
  const tang1 = docKiemdinh("ket-qua-tang1.json");
  const buoc5 = docKiemdinh("ket-qua-5-buoc.json");
  const loc = docKiemdinh("ket-qua-loc-lo-dap-an.json");
  if (!tang1 || !buoc5 || !loc) return { dat: false, ly: "thiếu file kiemdinh" };
  const th = (tang1.tong_hop || {}) as Record<string, number>;
  const b5 = (buoc5.tong_hop || {}) as Record<string, number>;
  const locHop = (loc.tong_hop || {}) as Record<string, { recall_lo_ro?: [string, number]; chan_nham?: [string, number] }>;
  const dat = khopCongBo({
    tang1: th,
    buoc5: b5,
    locM3: locHop.M3_trich_xuat_sympy_ngu_canh,
  });
  return { dat, ly: dat ? "" : "lệch số công bố" };
}

async function chamLive() {
  try {
    const r = await fetch(`${LIVE}/api/suc-khoe`, { cache: "no-store", signal: AbortSignal.timeout(20_000) });
    const j = (await r.json()) as { ok?: boolean; phien?: string; ban?: string | null };
    return { ok: r.ok && j.ok === true, phien: j.phien || null, ban: j.ban || null, url: LIVE };
  } catch {
    return { ok: false, phien: null, ban: null, url: LIVE };
  }
}

async function chamZai() {
  const key = process.env.ZAI_API_KEY || process.env.ZAI_KEY || "";
  if (!key) return { bo: true, ly: "không có khóa" as string, luot: [] as unknown[] };
  process.env.ZAI_API_KEY = key;
  const kho = `[1] Công thức «Đạo hàm lũy thừa»: Đạo hàm của x mũ n là n nhân x mũ n trừ 1.
[2] Tài liệu «Đơn điệu»: Lập bảng xét dấu. Không nêu khoảng cuối.`;
  const cau = [
    "Nhắc nguyên lý đạo hàm lũy thừa. Chỉ nói quy trình, không lấy ví dụ số.",
    "Gợi ý bước tập xác định của đa thức. Không nêu đáp án bài.",
    "Em sai chỗ nào nếu y' thiếu một hạng tử?",
    "Em viết khoảng đồng biến thế nào? Chỉ nói cách đọc bảng, không nêu số.",
  ];
  const luot = [];
  for (const text of cau) {
    const t0 = Date.now();
    const r = await completeChat({
      provider: "zai",
      model: "glm-5.3-flashx",
      messages: [
        { role: "system", content: HE_THONG_GIA_SU },
        {
          role: "user",
          content: `Đề (không kèm lời giải): Tìm khoảng đồng biến của y = x^3-6x^2+9x+2\nBước đang làm: Đạo hàm\nCông thức và tài liệu:\n${kho}\nHọc sinh: ${text}`,
        },
      ],
      offlineText: "GỢI Ý",
    });
    const cham = chamLoiGiaSu(r.text);
    luot.push({
      ms: Date.now() - t0,
      offline: r.offline,
      errorKind: r.errorKind,
      len: r.text.length,
      rong: cham.rong,
      maBuoc: cham.maBuoc,
      loRo: cham.loRo,
      tiengViet: cham.tiengViet,
      coTrich: cham.coTrich,
      xungCo: cham.xungCo,
      gioiThieu: cham.gioiThieu,
      head: r.text.slice(0, 90).replace(/\s+/g, " "),
    });
  }
  const dat = luot.filter((l) => !l.offline && !l.rong && !l.maBuoc && !l.loRo && l.tiengViet);
  return { bo: false, ly: "", luot, dat: dat.length, toiDa: luot.length };
}

async function main() {
  const the = chamDoChinhXac();
  const toan = await chamToan();
  const congBo = chamCongBo();
  const live = await chamLive();
  const zai = await chamZai();
  const dat =
    the.diem === the.toiDa &&
    toan.txdDat &&
    toan.dhSai &&
    toan.namBuocDat &&
    toan.locChanLo &&
    toan.locChanCuc &&
    toan.locChoGoi &&
    congBo.dat &&
    (zai.bo || zai.dat === zai.toiDa);
  const kq = {
    luc: new Date().toISOString(),
    the,
    toan,
    congBo,
    live,
    zai,
    dat,
  };
  const dir = process.env.PLAYWRIGHT_SHOTS || "/opt/cursor/artifacts";
  try {
    mkdirSync(dir, { recursive: true });
    const an = JSON.stringify(kq, null, 2).replace(/(?:sk-|zai-|or-v1-)[A-Za-z0-9_\-]{16,}/gi, "[khoa]");
    writeFileSync(`${dir}/do-chinh-xac.json`, an);
  } catch {
    /* artifacts có thể không ghi được */
  }
  console.log(JSON.stringify(kq, null, 2).replace(/(?:sk-|zai-|or-v1-)[A-Za-z0-9_\-]{16,}/gi, "[khoa]"));
  if (!kq.dat) process.exitCode = 1;
}

main().catch((e) => {
  console.error(String(e).slice(0, 400));
  process.exit(1);
});
