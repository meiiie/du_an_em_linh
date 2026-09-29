import { and, asc, desc, eq, inArray } from "drizzle-orm";
import { db } from "./db";
import { gradingResults, submissionSteps, submissionTableCells, submissionTables, submissions } from "./db/schema";
import { BUOC } from "./levels";

/**
 * UX-06: tiến trình làm bài của một học sinh, dựng lại từ lượt nộp gần nhất (mỗi lượt nộp gửi đủ các bước tới bước đang nộp).
 * Tải lại trang / vào lại từ /hs mở đúng bước đang dở, các bước đã đạt giữ nội dung. Không trả lời giải, không trả đáp án:
 * chỉ trả lại đúng những gì học sinh đã gõ và kết quả chấm của lượt đó.
 */
export type TienTrinh = {
  subId: string;
  step: number;
  txd: string;
  dh: string[];
  roots: { latex: string; loai: "NGHIEM" | "KHONG_XD" }[];
  points: string[];
  signs: Record<number, string>;
  arrows: Record<number, string>;
  kl: { db: string; nb: string; cd: string; ct: string };
  grade: {
    ket_qua: string;
    loai_ket_qua: string;
    buoc_sai: { ma_buoc: string; dong: number | null; o: { hang: string; k: number | null } | null } | null;
    cac_van_de?: unknown[];
    thong_bao: string;
    per_buoc?: Record<string, string>;
    finished?: boolean;
  } | null;
  perBuoc: Record<string, string>;
  finished: boolean;
};

const ORDER = BUOC.map((b) => b.ma) as string[];
const TIEN_TO_KHONG_XD = "y' không xác định tại ";

/** Bước đang dở: bước đầu tiên (từ bước bắt đầu) chưa đạt; đã xong thì bước cuối. */
export function buocDangDo(per: Record<string, string>, batDau: number, finished: boolean) {
  if (finished) return ORDER.length - 1;
  for (let i = Math.max(0, batDau); i < ORDER.length; i++) if (per[ORDER[i]] !== "DAT") return i;
  return ORDER.length - 1;
}

export async function tienTrinhBai(studentId: string, problemId: string, batDau: number): Promise<TienTrinh | null> {
  const sub = (
    await db
      .select()
      .from(submissions)
      .where(and(eq(submissions.studentId, studentId), eq(submissions.problemId, problemId)))
      .orderBy(desc(submissions.submittedAt))
      .limit(1)
  )[0];
  if (!sub) return null;
  const g = (await db.select().from(gradingResults).where(eq(gradingResults.submissionId, sub.id)).limit(1))[0];
  const per = ((g?.perBuoc as Record<string, string> | null) || {}) as Record<string, string>;
  const finished = sub.status === "da_cham" && sub.ketQua === "DAT";
  const dong = await db
    .select()
    .from(submissionSteps)
    .where(eq(submissionSteps.submissionId, sub.id))
    .orderBy(asc(submissionSteps.dong));
  const theoBuoc = (ma: string) => dong.filter((d) => d.maBuoc === ma);

  const txd = theoBuoc("B.DH.TXD")[0]?.latex || "";
  const dhRows = theoBuoc("B.DH.DAOHAM").map((d) => d.latex);
  const dh = dhRows.length ? dhRows : [""];
  const rootRows = theoBuoc("B.DH.NGHIEM")
    .map((d) => d.latex)
    .filter((l) => l.trim() && l.trim() !== "không có nghiệm");
  const roots = rootRows.length
    ? rootRows.map((l) =>
        l.startsWith(TIEN_TO_KHONG_XD)
          ? { latex: l.slice(TIEN_TO_KHONG_XD.length), loai: "KHONG_XD" as const }
          : { latex: l, loai: "NGHIEM" as const },
      )
    : [{ latex: "", loai: "NGHIEM" as const }];
  // Kết luận: dòng theo thứ tự đồng biến, nghịch biến, (cực đại, cực tiểu) như lúc gửi.
  const klRows = theoBuoc("B.DH.KETLUAN").map((d) => d.latex);
  const kl = { db: klRows[0] || "", nb: klRows[1] || "", cd: klRows[2] || "", ct: klRows[3] || "" };
  for (const k of ["cd", "ct"] as const) if (/^không có cực (đại|tiểu)$/i.test(kl[k].trim())) kl[k] = "";

  const points: string[] = [];
  const signs: Record<number, string> = {};
  const arrows: Record<number, string> = {};
  const bang = await db.select().from(submissionTables).where(eq(submissionTables.submissionId, sub.id));
  if (bang.length) {
    const cells = await db
      .select()
      .from(submissionTableCells)
      .where(inArray(submissionTableCells.tableId, bang.map((b) => b.id)))
      .orderBy(asc(submissionTableCells.k));
    for (const c of cells) {
      if (c.k == null) continue;
      const v = c.giaTri || "";
      if (c.hang === "X") points[c.k] = v;
      else if (c.hang === "DAU_YPHAY" && v) signs[c.k] = v;
      else if (c.hang === "BIEN_THIEN" && v) arrows[c.k] = v;
    }
  }

  const grade: TienTrinh["grade"] = g
    ? {
        ket_qua: g.ketQua,
        loai_ket_qua: g.loaiKetQua || g.ketQua,
        buoc_sai: (g.buocSai as NonNullable<TienTrinh["grade"]>["buoc_sai"]) || null,
        cac_van_de: (g.cacVanDe as unknown[] | null) || [],
        thong_bao: g.thongBao || "",
        per_buoc: per,
        finished,
      }
    : null;
  return {
    subId: sub.id,
    step: buocDangDo(per, batDau, finished),
    txd,
    dh,
    roots,
    points: points.filter((p) => p !== undefined),
    signs,
    arrows,
    kl,
    // Chỉ giữ kết quả chấm khi còn ý nghĩa ở bước đang mở: lượt SAI (tô lỗi) hoặc đã xong bài.
    grade: grade && (grade.ket_qua !== "DAT" || finished) ? grade : null,
    perBuoc: per,
    finished,
  };
}
