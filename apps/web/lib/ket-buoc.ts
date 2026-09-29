import { and, desc, eq } from "drizzle-orm";
import { db } from "./db";
import { escalations, tutorSessions } from "./db/schema";
import { BUOC } from "./levels";

/**
 * Kẹt theo bước (No chốt 11:37/11:50, UXT-07-k, UX-09): sau khi hết thang gợi ý của MỘT bước, mỗi lần xin đáp án / xin gợi ý /
 * nộp sai ở bước đó được đếm; đủ 2 lần -> đề xuất «Gửi thầy cô» và tạo ĐÚNG MỘT cảnh báo kẹt (gắn bài + bước). Sang bước
 * khác đếm lại từ 0. Trạng thái nằm trong tutor_sessions.hint_caps (jsonb) cùng các cấp gợi ý.
 */
export const KHOA_KET = {
  /** 1 khi bước đã từng mở tới cấp cuối (bất kể khóa loại lỗi). */
  daHet: (buoc: string) => `${buoc}|DA_HET`,
  /** Số lần đếm sau khi hết thang. */
  dem: (buoc: string) => `${buoc}|HET`,
  /** Id bài nộp sai cuối cùng đã được đếm (hoặc mốc lúc hết thang). */
  saiDem: (buoc: string) => `${buoc}|SAI_DEM`,
  /** 1 khi đã tạo cảnh báo kẹt cho bước. */
  baoGv: (buoc: string) => `${buoc}|BAO_GV`,
};

export const SO_LAN_SAU_HET_DE_GUI_GV = 2;

/** Chip «Gợi ý bước này» (tutor-panel) — nhắc lại cấp đang mở, không tăng cấp (UXT-07-a/d). */
export function laNhacLaiGoiY(text: string): boolean {
  return /^gợi ý bước này[.!?]?$/i.test((text || "").trim());
}

export function laMaBuoc(ma: string | null | undefined): ma is string {
  return !!ma && BUOC.some((b) => b.ma === ma);
}

export function tenBuoc(ma: string | null | undefined): string {
  return BUOC.find((b) => b.ma === ma)?.ten || "đang làm";
}

export async function taoCanhBaoKetBuoc(opts: {
  studentId: string;
  problem: { id: string; code: string; skillCode: string | null };
  maBuoc: string;
  tenBuoc: string;
}) {
  await db.insert(escalations).values({
    id: crypto.randomUUID(),
    studentId: opts.studentId,
    skillCode: opts.problem.skillCode || "T12.DH.03",
    problemId: opts.problem.id,
    maBuoc: opts.maBuoc,
    loai: "KET",
    reason: `Kẹt ở bước ${opts.tenBuoc} bài ${opts.problem.code}: đã hết thang gợi ý, xin thêm/thử lại vẫn chưa được.`,
  });
}

async function phienGiaSu(studentId: string, problemId: string) {
  return (
    await db
      .select()
      .from(tutorSessions)
      .where(and(eq(tutorSessions.studentId, studentId), eq(tutorSessions.problemId, problemId)))
      .orderBy(desc(tutorSessions.startedAt))
      .limit(1)
  )[0];
}

/**
 * Gọi ngay sau khi chấm một bài nộp SAI ở bước `buoc`: nếu bước đã hết thang và bài nộp này chưa được đếm thì đếm một lần;
 * đủ ngưỡng thì tạo cảnh báo kẹt (một lần cho mỗi bước). Trả true nếu bước này đang ở trạng thái «Gửi thầy cô».
 */
export async function ketSauNopSai(opts: {
  studentId: string;
  problem: { id: string; code: string; skillCode: string | null };
  buoc: string;
  subId: string;
}): Promise<boolean> {
  const sess = await phienGiaSu(opts.studentId, opts.problem.id);
  if (!sess) return false;
  const caps: Record<string, number> = { ...((sess.hintCaps as Record<string, number> | null) || {}) };
  const { buoc, subId } = opts;
  if (caps[KHOA_KET.daHet(buoc)] !== 1) return Boolean(caps[KHOA_KET.baoGv(buoc)]);
  if (String(caps[KHOA_KET.saiDem(buoc)] ?? "") === subId) return Boolean(caps[KHOA_KET.baoGv(buoc)]);
  caps[KHOA_KET.dem(buoc)] = (caps[KHOA_KET.dem(buoc)] || 0) + 1;
  caps[KHOA_KET.saiDem(buoc)] = subId as unknown as number;
  if (caps[KHOA_KET.dem(buoc)] >= SO_LAN_SAU_HET_DE_GUI_GV && !caps[KHOA_KET.baoGv(buoc)]) {
    await taoCanhBaoKetBuoc({ studentId: opts.studentId, problem: opts.problem, maBuoc: buoc, tenBuoc: tenBuoc(buoc) });
    caps[KHOA_KET.baoGv(buoc)] = 1;
  }
  await db.update(tutorSessions).set({ hintCaps: caps }).where(eq(tutorSessions.id, sess.id));
  return Boolean(caps[KHOA_KET.baoGv(buoc)]);
}

/** Các bước của bài đã ở trạng thái «Gửi thầy cô» (để tải lại trang vẫn hiện khối đề xuất). */
export async function buocDaDeXuatGuiGv(studentId: string, problemId: string): Promise<string[]> {
  const sess = await phienGiaSu(studentId, problemId);
  const caps = (sess?.hintCaps as Record<string, number> | null) || {};
  return BUOC.map((b) => b.ma).filter((ma) => Boolean(caps[KHOA_KET.baoGv(ma)]));
}
