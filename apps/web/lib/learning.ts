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
} from "./db/schema";
import { mucFromMastery, nextNotch, type Muc4 } from "./levels";
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

export async function applyMastery(opts: {
  studentId: string;
  submissionId: string;
  problemSkill: string | null;
  ketQua: string;
  buocSai: { ma_buoc: string } | null;
  maLoi: string | null;
  doTinCay: number | null;
  nghi: boolean;
  finished: boolean;
}) {
  const cfg = await loadConfig();
  let skill = opts.problemSkill || "T12.DH.03";
  let rule = "THEO_KY_NANG_BAI";
  if (opts.maLoi && opts.doTinCay != null && opts.doTinCay >= cfg.nguong_tin_cay_ma_loi) {
    const err = await db.select().from(errorTypes).where(eq(errorTypes.code, opts.maLoi)).limit(1);
    if (err[0]?.skillCode) {
      skill = err[0].skillCode;
      rule = "THEO_MA_LOI";
    }
  } else if (opts.buocSai) {
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
  const muc = mucFromMastery(next, cfg.nguong_muc);
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
  if (!opts.nghi && stuck >= cfg.so_luot_ket) {
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

export async function recommend(studentId: string) {
  const cfg = await loadConfig();
  const states = await db.select().from(masteryStates).where(eq(masteryStates.studentId, studentId));
  const pubs = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  if (!pubs.length) return null;
  const weak = [...states].sort((a, b) => a.mastery - b.mastery)[0];
  if (!weak) return { problem: pubs[0], lyDo: "Chưa có ước lượng thành thạo, bắt đầu bài đã phát hành." };
  const targetUp = nextNotch(weak.currentMucDo4);
  const ready = weak.currentMucDo4 !== "VAN_DUNG_CAO" && weak.mastery >= cfg.nguong_muc[targetUp === "THONG_HIEU" ? "THONG_HIEU" : targetUp === "VAN_DUNG" ? "VAN_DUNG" : "VAN_DUNG_CAO"];
  const pool = pubs.filter((p) => p.skillCode === weak.skillCode);
  const harder = pool.find((p) => p.mucDo4 === targetUp);
  const same = pool.find((p) => p.mucDo4 === weak.currentMucDo4);
  const recent = ((weak.lastErrorCodes as string[]) || [])[0];
  if (recent && same) {
    return { problem: ready && harder ? harder : same, lyDo: ready && harder ? "Đủ ngưỡng nên nâng một nấc." : "Cùng dạng vừa sai, giữ mức hiện tại." };
  }
  return {
    problem: (ready && harder) || same || pool[0] || pubs[0],
    lyDo: ready && harder ? "Nâng một nấc so với mức hiện tại." : "Bài cùng kỹ năng đang yếu.",
  };
}

export function mucLabel(code: string) {
  return code as Muc4;
}
