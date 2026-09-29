import { and, eq } from "drizzle-orm";
import { db } from "./db";
import {
  consentRecords,
  errorTypes,
  escalations,
  masteryConfig,
  masteryEvents,
  masteryStates,
  problems,
  stepTemplates,
  submissions,
} from "./db/schema";
import { LABEL4, MUC4, mucFromMastery, type Muc4 } from "./levels";
import type { SessionUser } from "./auth";

export type BktConfig = {
  p_t: number;
  p_g: number;
  p_s: number;
  nguong_tin_cay_ma_loi: number;
  so_luot_ket: number;
  nguong_doan_mo_so_lan_doi_o: number;
  nguong_muc: { THONG_HIEU: number; VAN_DUNG: number; VAN_DUNG_CAO: number };
};

const DEFAULT_CFG: BktConfig = {
  p_t: 0.12,
  p_g: 0.2,
  p_s: 0.1,
  nguong_tin_cay_ma_loi: 0.65,
  so_luot_ket: 3,
  nguong_doan_mo_so_lan_doi_o: 4,
  nguong_muc: { THONG_HIEU: 0.4, VAN_DUNG: 0.62, VAN_DUNG_CAO: 0.82 },
};

export async function loadConfig(): Promise<BktConfig> {
  const rows = await db.select().from(masteryConfig).where(eq(masteryConfig.key, "bkt"));
  if (!rows[0]) return DEFAULT_CFG;
  return { ...DEFAULT_CFG, ...(rows[0].value as BktConfig) };
}

export function bktNext(p: number, correct: boolean, cfg: BktConfig) {
  const { p_t: pT, p_g: pG, p_s: pS } = cfg;
  if (correct) {
    const known = (p * (1 - pS)) / (p * (1 - pS) + (1 - p) * pG);
    return clamp(known + (1 - known) * pT);
  }
  const known = (p * pS) / (p * pS + (1 - p) * (1 - pG) || 1);
  return clamp(known);
}

function clamp(n: number) {
  if (Number.isNaN(n)) return 0.3;
  return Math.min(0.98, Math.max(0.02, n));
}

export async function assertMayLearn(user: SessionUser) {
  if (user.isSynthetic) return;
  const rows = await db
    .select()
    .from(consentRecords)
    .where(and(eq(consentRecords.subjectUserId, user.id), eq(consentRecords.purposeCode, "HOC_TAP")));
  const ok = rows.some((r) => r.grantedAt && !r.withdrawnAt);
  if (!ok) {
    throw new Error("Tài khoản chưa có bản ghi đồng ý của phụ huynh cho mục đích học tập.");
  }
}

export function nghiDoanMo(events: { o?: { hang: string; k: number | null } | null }[], nguong: number) {
  const counts = new Map<string, number>();
  for (const e of events) {
    if (!e.o) continue;
    const key = `${e.o.hang}:${e.o.k}`;
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  for (const [key, n] of counts) {
    if (n >= nguong) return `Ô ${key} bị đổi ${n} lần trước khi nộp (ngưỡng ${nguong}).`;
  }
  return null;
}

/**
 * SP-01: cập nhật thành thạo chỉ khi có bằng chứng thật.
 * - KHONG_KIEM_DUOC: không tính (loai_khoi_dem v0.2) cho tới khi giáo viên chấm.
 * - DAT giữa bài (chưa xong): không cập nhật, không đổi bộ đếm kẹt.
 * - Xong bài DAT: tính đúng. SAI: tính sai trên kỹ năng của lỗi GỐC đầu tiên (ô hệ quả không trừ, tru_he_qua=false).
 * - stuckCounter chỉ tăng khi có SAI thật.
 * SP-02: mức lên theo (kỹ năng, mức bài): chỉ bài ở mức ≥ mức hiện tại mới đẩy lên, mỗi lần tối đa một nấc.
 */
export async function applyMastery(opts: {
  studentId: string;
  submissionId: string;
  problemSkill: string | null;
  problemMuc?: string | null;
  ketQua: string;
  buocSai: { ma_buoc: string } | null;
  maLoi: string | null;
  doTinCay: number | null;
  nghi: boolean;
  finished: boolean;
}) {
  const cfg = await loadConfig();
  if (opts.ketQua === "KHONG_KIEM_DUOC" || (opts.ketQua === "DAT" && !opts.finished)) {
    return { skill: opts.problemSkill || "T12.DH.03", next: null, muc: null, rule: opts.ketQua === "DAT" ? "BUOC_DAT_GIUA_BAI" : "KHONG_KIEM_DUOC_KHOI_DEM" };
  }
  let skill = opts.problemSkill || "T12.DH.03";
  let rule = "THEO_KY_NANG_BAI";
  if (opts.ketQua === "SAI" && opts.maLoi && opts.doTinCay != null && opts.doTinCay >= cfg.nguong_tin_cay_ma_loi) {
    const err = await db.select().from(errorTypes).where(eq(errorTypes.code, opts.maLoi)).limit(1);
    if (err[0]?.skillCode) {
      skill = err[0].skillCode;
      rule = "THEO_MA_LOI";
    }
  } else if (opts.ketQua === "SAI" && opts.buocSai) {
    const step = await db.select().from(stepTemplates).where(eq(stepTemplates.maBuoc, opts.buocSai.ma_buoc)).limit(1);
    if (step[0]?.skillCode) skill = step[0].skillCode;
    rule = "THEO_BUOC";
  }
  const cur = await db
    .select()
    .from(masteryStates)
    .where(and(eq(masteryStates.studentId, opts.studentId), eq(masteryStates.skillCode, skill)))
    .limit(1);
  const prev = cur[0]?.mastery ?? 0.3;
  const correct = opts.finished && opts.ketQua === "DAT";
  const next = opts.nghi ? prev : bktNext(prev, correct, cfg);
  const delta = opts.nghi ? 0 : next - prev;
  const curMuc = (cur[0]?.currentMucDo4 as Muc4 | undefined) || mucFromMastery(prev, cfg.nguong_muc);
  const muc = mucSauBai(curMuc, next, correct, opts.problemMuc || null, cfg.nguong_muc);
  const errors = new Set<string>([...((cur[0]?.lastErrorCodes as string[]) || [])]);
  if (opts.maLoi) errors.add(opts.maLoi);
  const stuck = opts.nghi ? cur[0]?.stuckCounter || 0 : correct ? 0 : (cur[0]?.stuckCounter || 0) + 1;
  if (cur[0]) {
    await db
      .update(masteryStates)
      .set({
        mastery: next,
        currentMucDo4: opts.nghi ? cur[0].currentMucDo4 : muc,
        attempts: (cur[0].attempts || 0) + (opts.nghi ? 0 : 1),
        stuckCounter: stuck,
        lastErrorCodes: [...errors].slice(-8),
      })
      .where(and(eq(masteryStates.studentId, opts.studentId), eq(masteryStates.skillCode, skill)));
  } else {
    await db.insert(masteryStates).values({
      studentId: opts.studentId,
      skillCode: skill,
      mastery: next,
      currentMucDo4: muc,
      attempts: opts.nghi ? 0 : 1,
      stuckCounter: stuck,
      lastErrorCodes: [...errors].slice(-8),
    });
  }
  await db.insert(masteryEvents).values({
    id: crypto.randomUUID(),
    studentId: opts.studentId,
    skillCode: skill,
    submissionId: opts.submissionId,
    delta,
    ruleApplied: opts.nghi ? "NGHI_DOAN_MO" : rule,
    buocSai: opts.buocSai,
    maLoi: opts.maLoi,
    doTinCay: opts.doTinCay,
    nghiDoanMo: opts.nghi,
  });
  if (!opts.nghi && opts.ketQua === "SAI" && stuck >= cfg.so_luot_ket) {
    const open = await db
      .select()
      .from(escalations)
      .where(and(eq(escalations.studentId, opts.studentId), eq(escalations.skillCode, skill)));
    if (!open.some((e) => !e.handledAt)) {
      await db.insert(escalations).values({
        id: crypto.randomUUID(),
        studentId: opts.studentId,
        skillCode: skill,
        reason: `Kẹt ${stuck} lượt ở ${skill}`,
      });
    }
  }
  return { skill, next, muc, rule };
}

const IDX = (m: string | null | undefined) => MUC4.indexOf((m || "") as Muc4);

/** Mức sau một bài (thuần, có test): lên tối đa 1 nấc và chỉ khi bài ở mức ≥ mức hiện tại; xuống tối đa 1 nấc. */
export function mucSauBai(
  cur: Muc4,
  mastery: number,
  correct: boolean,
  problemMuc: string | null,
  nguong: BktConfig["nguong_muc"],
): Muc4 {
  const ci = Math.max(0, IDX(cur));
  const theoP = IDX(mucFromMastery(mastery, nguong));
  if (correct) {
    const pi = IDX(problemMuc);
    const bangChungDu = pi < 0 || pi >= ci; // bài dễ hơn mức hiện tại không đẩy lên
    if (theoP > ci && bangChungDu) return MUC4[Math.min(ci + 1, MUC4.length - 1)];
    return MUC4[ci];
  }
  if (theoP < ci) return MUC4[Math.max(ci - 1, 0)];
  return MUC4[ci];
}

type Pub = typeof problems.$inferSelect;

/**
 * SP-02: gợi ý bài theo kỹ năng yếu nhất; mức bài ≤ mức hiện tại + 1; lý do đúng với bài được chọn.
 * `boQua`: bài vừa làm xong (để "Bài tiếp theo" không trả lại chính nó).
 */
export async function recommend(studentId: string, boQua?: string | null) {
  const cfg = await loadConfig();
  const states = await db.select().from(masteryStates).where(eq(masteryStates.studentId, studentId));
  const pubs = (await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"))).filter(
    (p) => p.hamSympy && p.id !== boQua,
  );
  return chonBai(states, pubs, cfg);
}

export function chonBai(
  states: { skillCode: string; mastery: number; currentMucDo4: string; stuckCounter: number }[],
  pubs: Pub[],
  cfg: BktConfig,
): { problem: Pub; lyDo: string } | null {
  if (!pubs.length) return null;
  const theoMuc = (xs: Pub[]) => [...xs].sort((a, b) => IDX(a.mucDo4) - IDX(b.mucDo4));
  const weak = [...states].sort((a, b) => a.mastery - b.mastery || b.stuckCounter - a.stuckCounter)[0];
  if (!weak) {
    const p = theoMuc(pubs)[0];
    return { problem: p, lyDo: "Chưa có ước lượng thành thạo: bắt đầu từ bài mức thấp nhất đã mở." };
  }
  // Mức hiện tại đã được nâng (tối đa 1 nấc, chỉ khi có bằng chứng ở bài cùng mức) trong mucSauBai. Gợi ý bài Ở mức đó:
  // sau khi vừa lên nấc thì đây chính là "bài khó hơn một nấc". Không tự nhảy thêm nấc theo mastery (SP-02).
  const L = Math.max(0, IDX(weak.currentMucDo4));
  void cfg;
  const pool = pubs.filter((p) => p.skillCode === weak.skillCode);
  const tenMuc = (m: string | null) => LABEL4[(m || "") as Muc4] || m || "chưa gắn mức";
  if (weak.stuckCounter >= 2) {
    const de = theoMuc(pool.filter((p) => IDX(p.mucDo4) <= L)).reverse()[0];
    if (de) return { problem: de, lyDo: `Em đang kẹt ở kỹ năng ${weak.skillCode}: giữ mức ${tenMuc(de.mucDo4)}, chưa nâng nấc.` };
  }
  const same = pool.find((p) => IDX(p.mucDo4) === L);
  if (same) return { problem: same, lyDo: `Cùng mức ${tenMuc(same.mucDo4)}, cùng kỹ năng đang yếu nhất (${weak.skillCode}).` };
  const thap = theoMuc(pool.filter((p) => IDX(p.mucDo4) <= L + 1)).reverse()[0];
  if (thap) return { problem: thap, lyDo: `Chưa có bài mức ${tenMuc(MUC4[L])} cho ${weak.skillCode}; đây là bài cùng kỹ năng ở mức ${tenMuc(thap.mucDo4)}.` };
  const khac = theoMuc(pubs.filter((p) => IDX(p.mucDo4) <= L + 1 || IDX(p.mucDo4) < 0)).reverse()[0];
  if (khac) {
    return {
      problem: khac,
      lyDo: `Chưa có bài đã mở cho kỹ năng yếu nhất (${weak.skillCode}); bài này luyện ${khac.skillCode || "kỹ năng khác"} ở mức ${tenMuc(khac.mucDo4)}.`,
    };
  }
  return null;
}

/**
 * Chốt 11:37 (1): "bài tương tự dễ hơn" là một bài CỤ THỂ: cùng kỹ năng, thấp hơn đúng một mức, đã phát hành,
 * học sinh chưa làm (chưa có bài nộp). Không có thì trả null (gia sư nói bằng lời và gợi ý Gửi thầy cô).
 */
export async function baiDeHonMotMuc(studentId: string, p: { id: string; skillCode: string | null; mucDo4: string | null }) {
  const j = IDX(p.mucDo4);
  if (j <= 0 || !p.skillCode) return null;
  const muc = MUC4[j - 1];
  const pubs = (await db.select().from(problems).where(and(eq(problems.status, "DA_PHAT_HANH"), eq(problems.skillCode, p.skillCode))))
    .filter((q) => q.id !== p.id && q.hamSympy && q.mucDo4 === muc && (q.dangTraLoi || "TU_LUAN_5_BUOC") === "TU_LUAN_5_BUOC");
  if (!pubs.length) return null;
  const daLam = new Set(
    (await db.select({ pid: submissions.problemId }).from(submissions).where(eq(submissions.studentId, studentId))).map((r) => r.pid),
  );
  return pubs.find((q) => !daLam.has(q.id)) || null;
}

export function mucLabel(code: string) {
  return code as Muc4;
}
