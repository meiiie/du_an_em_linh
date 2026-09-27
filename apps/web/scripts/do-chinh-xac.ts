/** Chạy local: thẻ in-repo + chấm/lọc HTTP + (nếu có khóa) vài lượt Z.AI. Không in khóa. */
import { writeFileSync, mkdirSync } from "node:fs";
import { chamDoChinhXac, chamLoiGiaSu } from "../lib/do-chinh-xac";
import { completeChat } from "../lib/ai-harness";
import { HE_THONG_GIA_SU } from "../lib/tutor";
import { mathJob } from "../lib/math";

const MATH = process.env.MATH_SERVICE_URL || "http://127.0.0.1:8000";

async function chamToan() {
  const dung = await mathJob<{ ket_qua?: string }>("grade", {
    ham: "x**3 - 6*x**2 + 9*x + 2",
    nop_toi: "B.DH.TXD",
    cac_buoc: [{ ma_buoc: "B.DH.TXD", cac_dong: [{ dong: 0, latex: "\\mathbb{R}" }] }],
  });
  const sai = await mathJob<{ ket_qua?: string; buoc_sai?: { ma_buoc?: string; dong?: number } }>("grade", {
    ham: "x**3 - 6*x**2 + 9*x + 2",
    nop_toi: "B.DH.DAOHAM",
    cac_buoc: [
      { ma_buoc: "B.DH.TXD", cac_dong: [{ dong: 0, latex: "\\mathbb{R}" }] },
      { ma_buoc: "B.DH.DAOHAM", cac_dong: [{ dong: 0, latex: "3x^{2}-12x" }] },
    ],
  });
  const lo = await mathJob<{ cho_phep?: boolean }>("filter", {
    ban_nhap: "Hàm đồng biến trên (1; 3).",
    su_kien: [{ loai: "DB", gia_tri: "(1;3)" }],
  });
  const goi = await mathJob<{ cho_phep?: boolean }>("filter", {
    ban_nhap: "Em tính y' từng hạng tử, hằng số có đạo hàm 0.",
    su_kien: [{ loai: "DB", gia_tri: "(1;3)" }, { loai: "DCD", gia_tri: "1" }],
  });
  return {
    math: MATH,
    txdDat: dung.ket_qua === "DAT",
    dhSai: sai.ket_qua === "SAI" && sai.buoc_sai?.ma_buoc === "B.DH.DAOHAM",
    locChanLo: lo.cho_phep === false,
    locChoGoi: goi.cho_phep === true,
  };
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
      head: r.text.slice(0, 90).replace(/\s+/g, " "),
    });
  }
  const dat = luot.filter((l) => !l.offline && !l.rong && !l.maBuoc && !l.loRo && l.tiengViet);
  return { bo: false, ly: "", luot, dat: dat.length, toiDa: luot.length };
}

async function main() {
  const the = chamDoChinhXac();
  const toan = await chamToan();
  const zai = await chamZai();
  const kq = {
    luc: new Date().toISOString(),
    the,
    toan,
    zai,
    dat:
      the.diem === the.toiDa && toan.txdDat && toan.dhSai && toan.locChanLo && toan.locChoGoi && (zai.bo || zai.dat === zai.toiDa),
  };
  const dir = process.env.PLAYWRIGHT_SHOTS || "/opt/cursor/artifacts";
  try {
    mkdirSync(dir, { recursive: true });
    writeFileSync(`${dir}/do-chinh-xac.json`, JSON.stringify(kq, null, 2));
  } catch {
    /* artifacts có thể không ghi được */
  }
  console.log(JSON.stringify(kq, null, 2));
  if (!kq.dat) process.exitCode = 1;
}

main().catch((e) => {
  console.error(String(e).slice(0, 400));
  process.exit(1);
});
