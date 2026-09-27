/**
 * Nạp dữ liệu tổng hợp và kiểm định từng bài qua cổng 3 tầng.
 * Không cần khóa LLM. Gọi dịch vụ toán qua HTTP nếu có MATH_SERVICE_URL,
 * không thì chạy tiến trình SymPy trong .venv.
 */
import { createHash, randomBytes, scryptSync } from "crypto";
import { spawnSync } from "child_process";
import { existsSync, readFileSync } from "fs";
import path from "path";
import { db, sql } from "../lib/db";
import {
  assignments,
  auditLogs,
  classSettings,
  classes,
  consentRecords,
  documents,
  enrollments,
  errorTypes,
  escalations,
  formulaSheets,
  formulas,
  hintLevels,
  masteryConfig,
  masteryStates,
  problems,
  reminders,
  skillPrerequisites,
  skills,
  solutions,
  stepTemplates,
  studySchedules,
  userRoles,
  users,
  verificationRuns,
  verificationTierResults,
} from "../lib/db/schema";

const GV = "11111111-1111-4111-8111-111111111111";
const AN = "22222222-2222-4222-8222-222222222222";
const BINH = "33333333-3333-4333-8333-333333333333";
const CHI = "44444444-4444-4444-8444-444444444444";
const LOP = "55555555-5555-4555-8555-555555555555";

const MUC4: Record<string, string> = {
  NB: "NHAN_BIET",
  TH: "THONG_HIEU",
  VD: "VAN_DUNG",
  VDC: "VAN_DUNG_CAO",
  NHAN_BIET: "NHAN_BIET",
  THONG_HIEU: "THONG_HIEU",
  VAN_DUNG: "VAN_DUNG",
  VAN_DUNG_CAO: "VAN_DUNG_CAO",
};

const MUC3: Record<string, string> = {
  NB: "BIET",
  TH: "HIEU",
  VD: "VAN_DUNG",
  VDC: "VAN_DUNG",
  BIET: "BIET",
  HIEU: "HIEU",
  VAN_DUNG: "VAN_DUNG",
};

function hashPassword(password: string) {
  const salt = randomBytes(16).toString("hex");
  const hash = scryptSync(password, salt, 32).toString("hex");
  return `scrypt$${salt}$${hash}`;
}

function contentHash(data: unknown) {
  return createHash("sha256").update(JSON.stringify(data)).digest("hex");
}

function repoRoot() {
  return path.resolve(__dirname, "../../..");
}

type Tier = {
  tang: number;
  trang_thai: string;
  ly_do?: string;
  loai_ket_qua?: string;
  buoc_sai?: unknown;
  ma_loi?: string;
  do_tin_cay?: number;
  trich_dan?: unknown;
  cong_thuc?: unknown;
};

type Verify = {
  trang_thai_tong: string;
  trang_thai_phat_hanh: string;
  tang: Tier[];
};

async function math<T>(kind: string, payload: unknown): Promise<T> {
  const paths: Record<string, string> = {
    solve: "/v1/solve",
    verify: "/v1/verify",
    generate: "/v1/generate",
  };
  const url = process.env.MATH_SERVICE_URL;
  const py = path.join(repoRoot(), "services/math/.venv/bin/python");
  if (!url && existsSync(py)) {
    const runner = path.join(repoRoot(), "services/math/app/job_runner.py");
    const run = spawnSync(py, [runner], {
      input: JSON.stringify({ kind, payload }),
      encoding: "utf8",
      cwd: path.join(repoRoot(), "services/math"),
      maxBuffer: 20 * 1024 * 1024,
    });
    if (run.status !== 0) {
      throw new Error((run.stderr || run.stdout || "math").slice(0, 500));
    }
    return JSON.parse(run.stdout) as T;
  }
  const base = url || "http://127.0.0.1:8000";
  const res = await fetch(base + paths[kind], {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await res.text());
  return (await res.json()) as T;
}

function parseCsv(text: string) {
  const rows: string[][] = [];
  let row: string[] = [];
  let cur = "";
  let quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          cur += '"';
          i++;
        } else quoted = false;
      } else cur += c;
    } else if (c === '"') quoted = true;
    else if (c === ",") {
      row.push(cur);
      cur = "";
    } else if (c === "\n") {
      row.push(cur);
      rows.push(row);
      row = [];
      cur = "";
    } else if (c !== "\r") cur += c;
  }
  if (cur || row.length) {
    row.push(cur);
    rows.push(row);
  }
  return rows.filter((r) => r.some((cell) => cell.trim()));
}

type HintBlock = { ma_buoc: string; cac_cap: { cap: number; noi_dung?: string | null; ly_do_trong?: string | null }[] };

function hintText(cap: { noi_dung?: string | null; ly_do_trong?: string | null }) {
  return cap.noi_dung || cap.ly_do_trong || "Em tự viết lại bước đang dở. Mình không đưa kết quả của bước này.";
}

async function napBai(opts: {
  code: string;
  skill: string;
  phu: string[];
  muc4: string;
  muc3: string | null;
  bloom: string | null;
  text: string;
  latex: string;
  ham: string | null;
  origin: string;
  baiLam: unknown;
  facts: unknown;
  hints: HintBlock[];
  corpus: { tai_lieu: unknown; cong_thuc: unknown };
}) {
  const verified = await math<Verify>("verify", {
    ham: opts.ham,
    bai_lam: opts.baiLam,
    ...opts.corpus,
  });
  const id = crypto.randomUUID();
  const hash = contentHash({ de: opts.text, bl: opts.baiLam, hints: opts.hints });
  await db.insert(problems).values({
    id,
    code: opts.code,
    skillCode: opts.skill,
    skillCodesPhu: opts.phu,
    mucDo4: opts.muc4,
    mucDoBo3: opts.muc3,
    bloomLevel: opts.bloom,
    difficulty: 0.5,
    statementText: opts.text,
    statementLatex: opts.latex,
    hamSympy: opts.ham,
    origin: opts.origin,
    status: verified.trang_thai_phat_hanh,
    contentHash: hash,
    createdBy: GV,
  });
  await db.insert(solutions).values({
    problemId: id,
    baiLam: opts.baiLam,
    protectedFacts: opts.facts ?? [],
    finalAnswer: null,
  });
  for (const block of opts.hints) {
    for (const cap of block.cac_cap) {
      await db.insert(hintLevels).values({
        problemId: id,
        maBuoc: block.ma_buoc,
        cap: cap.cap,
        noiDung: hintText(cap),
      });
    }
  }
  const runId = crypto.randomUUID();
  await db.insert(verificationRuns).values({
    id: runId,
    problemId: id,
    contentHash: hash,
    overallStatus: verified.trang_thai_tong,
    publishStatus: verified.trang_thai_phat_hanh,
    stale: false,
    createdAt: new Date(),
  });
  for (const t of verified.tang) {
    await db.insert(verificationTierResults).values({
      id: crypto.randomUUID(),
      runId,
      tier: t.tang,
      status: t.trang_thai,
      loaiKetQua: t.loai_ket_qua || null,
      buocSai: t.buoc_sai || null,
      errorCode: t.ma_loi || null,
      confidence: t.do_tin_cay ?? null,
      reasonText: t.ly_do || null,
      citation: t.trich_dan || t.cong_thuc || null,
      rawJson: t,
    });
  }
  console.log(`${opts.code} → ${verified.trang_thai_phat_hanh} (${verified.tang.map((t) => t.trang_thai).join("/")})`);
  return { id, status: verified.trang_thai_phat_hanh };
}

async function main() {
  const root = repoRoot();
  const catalog = JSON.parse(readFileSync(path.join(root, "data/supham/danh-muc-ky-nang-DH.json"), "utf8")) as {
    ky_nang: {
      ma: string;
      ten: string;
      lop: number;
      la_cot_loi_chu_de: boolean;
      yccd_gdpt2018: string;
      tien_quyet: { ma: string; muc_toi_thieu: string }[];
    }[];
  };
  const examples = JSON.parse(readFileSync(path.join(root, "data/supham/03-vi-du-bai-tap.json"), "utf8")) as {
    id: string;
    ky_nang_chinh: string;
    ky_nang_phu?: string[];
    muc_do_4: string;
    muc_do_bo_3: string | null;
    muc_bloom: string | null;
    de_bai: { van_ban: string; latex: string; ham_so_sympy: string | null };
    thang_goi_y?: HintBlock[];
  }[];
  const csv = parseCsv(readFileSync(path.join(root, "data/supham/ma-loi-DH.csv"), "utf8"));

  await sql.unsafe(`
    TRUNCATE TABLE
      sessions, reminders, study_schedules, escalations, mastery_events, mastery_states,
      tutor_messages, tutor_sessions, llm_calls, grading_results, input_events,
      submission_table_cells, submission_tables, submission_steps, submissions,
      assignments, content_reviews, verification_tier_results, verification_runs,
      hint_levels, solutions, problems, formulas, formula_sheets, documents,
      step_templates, error_types, skill_prerequisites, skills, class_settings,
      enrollments, classes, consent_records, audit_logs, user_roles, users,
      mastery_config
    RESTART IDENTITY CASCADE
  `);

  const gvHash = hashPassword("giaovien123");
  const hsHash = hashPassword("hocsinh123");
  await db.insert(users).values([
    {
      id: GV,
      email: "gv@demo.local",
      passwordHash: gvHash,
      displayName: "Giáo viên thử",
      birthYear: null,
      status: "active",
      isSynthetic: true,
      pseudonymId: "ps-gv",
    },
    {
      id: AN,
      email: "hs.an@demo.local",
      passwordHash: hsHash,
      displayName: "An",
      birthYear: 2008,
      status: "active",
      isSynthetic: true,
      pseudonymId: "ps-an",
    },
    {
      id: BINH,
      email: "hs.binh@demo.local",
      passwordHash: hsHash,
      displayName: "Bình",
      birthYear: 2008,
      status: "active",
      isSynthetic: true,
      pseudonymId: "ps-binh",
    },
    {
      id: CHI,
      email: "hs.chi@demo.local",
      passwordHash: hsHash,
      displayName: "Chi",
      birthYear: 2008,
      status: "active",
      isSynthetic: true,
      pseudonymId: "ps-chi",
    },
  ]);
  await db.insert(userRoles).values([
    { userId: GV, roleCode: "GV" },
    { userId: AN, roleCode: "HS" },
    { userId: BINH, roleCode: "HS" },
    { userId: CHI, roleCode: "HS" },
  ]);
  await db.insert(classes).values({ id: LOP, name: "12A1 thử", grade: 12, year: 2026 });
  await db.insert(enrollments).values([
    { classId: LOP, userId: GV, roleInClass: "GV" },
    { classId: LOP, userId: AN, roleInClass: "HS" },
    { classId: LOP, userId: BINH, roleInClass: "HS" },
    { classId: LOP, userId: CHI, roleInClass: "HS" },
  ]);
  await db.insert(classSettings).values({ classId: LOP, moLoiGiaiSauKhiNop: false });
  const now = new Date();
  for (const id of [AN, BINH, CHI]) {
    await db.insert(consentRecords).values({
      id: crypto.randomUUID(),
      subjectUserId: id,
      grantedByUserId: null,
      purposeCode: "HOC_TAP",
      policyVersion: "0.1-tong-hop",
      method: "du_lieu_tong_hop",
      grantedAt: now,
      withdrawnAt: null,
    });
  }
  await db.insert(auditLogs).values({
    id: crypto.randomUUID(),
    actorUserId: GV,
    action: "NAP_DU_LIEU_MAU",
    entity: "seed",
    entityId: "v0.1",
    at: now,
    reason: "Dữ liệu tổng hợp, không có học sinh thật",
  });

  for (const s of catalog.ky_nang) {
    await db.insert(skills).values({
      code: s.ma,
      topicCode: "DH12",
      name: s.ten,
      description: s.yccd_gdpt2018,
      grade: s.lop,
      isCore: s.la_cot_loi_chu_de,
    });
    for (const pre of s.tien_quyet) {
      await db.insert(skillPrerequisites).values({
        skillCode: s.ma,
        prerequisiteCode: pre.ma,
        mucToiThieu: pre.muc_toi_thieu,
      });
    }
  }

  const header = csv[0];
  if (header[0] !== "ma_loi") throw new Error("CSV mã lỗi không đúng tiêu đề");
  for (const row of csv.slice(1)) {
    const [code, skill, buoc, mota, , goi] = row;
    await db.insert(errorTypes).values({
      code,
      skillCode: skill || null,
      maBuoc: buoc || null,
      name: mota,
      goiYSua: goi || null,
    });
  }

  const steps = [
    ["B.DH.TXD", 1, "DONG", "T12.DH.02", "Tập xác định"],
    ["B.DH.DAOHAM", 2, "DONG", "T12.DH.02", "Tính đạo hàm"],
    ["B.DH.NGHIEM", 3, "DONG", "T12.DH.02", "Nghiệm y′ = 0 và điểm y′ không xác định"],
    ["B.DH.XETDAU", 4, "BANG", "T12.DH.03", "Bảng xét dấu"],
    ["B.DH.KETLUAN", 5, "DONG", "T12.DH.03", "Kết luận đơn điệu và cực trị"],
  ] as const;
  for (const [ma, thu, dang, skill, mota] of steps) {
    await db.insert(stepTemplates).values({
      maBuoc: ma,
      topicCode: "DH12",
      thuTu: thu,
      dangNhap: dang,
      skillCode: skill,
      moTa: mota,
    });
  }

  await db.insert(masteryConfig).values({
    key: "bkt",
    version: 1,
    value: {
      p_t: 0.12,
      p_g: 0.2,
      p_s: 0.1,
      nguong_tin_cay_ma_loi: 0.65,
      so_luot_ket: 3,
      nguong_doan_mo_so_lan_doi_o: 4,
      nguong_muc: { THONG_HIEU: 0.4, VAN_DUNG: 0.62, VAN_DUNG_CAO: 0.82 },
    },
  });

  const docId = crypto.randomUUID();
  const docText = [
    "Ghi chú tự soạn cho lớp 12A1 thử, không chép sách.",
    "Với hàm số xác định trên một khoảng: nếu đạo hàm không âm trên khoảng đó và chỉ bằng 0 tại hữu hạn điểm thì hàm đồng biến;",
    "nếu đạo hàm không dương và chỉ bằng 0 tại hữu hạn điểm thì hàm nghịch biến.",
    "Lập bảng xét dấu của đạo hàm trên từng khoảng xác định bởi nghiệm y' = 0 và điểm y' không xác định.",
    "Nếu đạo hàm đổi từ dương sang âm khi đi qua một điểm trong thì đó là cực đại; từ âm sang dương là cực tiểu.",
    "Điểm cực trị phải nằm trong một khoảng mở chứa trong tập xác định.",
  ].join(" ");
  await db.insert(documents).values({
    id: docId,
    title: "Ghi chú tự soạn: đơn điệu và cực trị",
    kind: "tu_soan",
    source: "giáo viên thử",
    licenseStatus: "tu_soan",
    textContent: docText,
    version: 1,
    uploadedBy: GV,
    createdAt: now,
  });
  await db.insert(documents).values({
    id: crypto.randomUUID(),
    title: "Đề mẫu tự soạn — cùng dạng đa thức bậc ba",
    kind: "de_mau",
    source: "giáo viên thử",
    licenseStatus: "tu_soan",
    textContent:
      "Đề tự soạn, không chép sách. Dạng: tìm khoảng đồng biến, nghịch biến của y = ax^3 + bx^2 + cx + d với a khác 0. " +
      "Học sinh đi đủ năm bước: tập xác định, đạo hàm, nghiệm y′, bảng xét dấu, kết luận. " +
      "Yêu cầu cần đạt công khai (CT GDPT môn Toán 2018, TT 32/2018/TT-BGDĐT, lớp 12): nhận biết đơn điệu từ dấu y′; thể hiện trên bảng biến thiên. " +
      "Không dùng làm lời giải chuẩn cho gia sư.",
    version: 1,
    uploadedBy: GV,
    createdAt: now,
  });
  await db.insert(documents).values({
    id: crypto.randomUUID(),
    title: "Tham khảo phương pháp — ôn đơn điệu thế nào",
    kind: "tham_khao",
    source: "ghi chú lớp thử",
    licenseStatus: "tu_soan",
    textContent:
      "Văn bản tự soạn. Ôn theo khoảng cách (spacing) và tự lấy lại (retrieval): mỗi buổi tự viết lại quy tắc đạo hàm rồi làm một bài, không mở đáp án trước. " +
      "Gia sư chỉ gợi ý quy trình, không bottom-out kết quả (VanLehn 2006; Aleven — instrumental help). " +
      "Khi kẹt cùng bước ba lần thì gửi thầy cô, chưa nâng mức vận dụng cao.",
    version: 1,
    uploadedBy: GV,
    createdAt: now,
  });

  const sheetId = crypto.randomUUID();
  await db.insert(formulaSheets).values({
    id: sheetId,
    classId: LOP,
    ownerTeacherId: GV,
    version: 1,
    status: "locked",
    lockedAt: now,
    note: "Bảng khóa kèm chủ đề đạo hàm — đơn điệu và cực trị",
  });
  const formulaRows = [
    ["Đạo hàm lũy thừa", "(x^n)' = n x^{n-1}", "Đạo hàm của x mũ n là n nhân x mũ n trừ 1. Hằng số có đạo hàm bằng 0."],
    ["Đạo hàm tổng", "(u+v)' = u' + v'", "Đạo hàm của tổng bằng tổng các đạo hàm."],
    ["Đạo hàm thương", "(u/v)' = (u'v - uv') / v^2", "Với thương, tử là u'v trừ uv', mẫu là v bình."],
    ["Đơn điệu", "y' \\ge 0 \\Rightarrow đồng biến", "Hàm đồng biến trên khoảng khi đạo hàm không âm và bằng 0 tại hữu hạn điểm; nghịch biến khi đạo hàm không dương theo cùng quy tắc."],
    ["Cực trị", "+ \\to - : cực đại", "Đạo hàm đổi từ dương sang âm thì cực đại; từ âm sang dương thì cực tiểu. Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị."],
    ["Điểm tới hạn", "y'=0 hoặc y' không xác định", "Điểm tới hạn gồm nghiệm của đạo hàm bằng 0 và điểm thuộc tập xác định mà đạo hàm không xác định."],
  ];
  const congThuc = [];
  for (const [title, latex, noi] of formulaRows) {
    const id = crypto.randomUUID();
    congThuc.push({ id, latex, noi_dung: noi, ten: title });
    await db.insert(formulas).values({
      id,
      formulaSheetId: sheetId,
      skillCode: "T12.DH.03",
      title,
      latex,
      noiDung: noi,
    });
  }
  const corpus = {
    tai_lieu: [{ id: docId, text: docText, license_status: "tu_soan", phien_ban: 1 }],
    cong_thuc: congThuc,
  };

  const published: string[] = [];
  for (const ex of examples) {
    let baiLam: unknown = null;
    let facts: unknown = [];
    let ham: string | null = null;
    let hints = ex.thang_goi_y || [];
    if (ex.de_bai.ham_so_sympy && !ex.de_bai.ham_so_sympy.includes("m")) {
      const solved = await math<{
        dat?: boolean;
        bai_lam?: unknown;
        su_kien?: unknown;
        thang_goi_y?: HintBlock[];
      }>("solve", { ham: ex.de_bai.ham_so_sympy });
      if (solved.dat && solved.bai_lam) {
        ham = ex.de_bai.ham_so_sympy;
        baiLam = solved.bai_lam;
        facts = solved.su_kien || [];
        if (!hints.length) hints = solved.thang_goi_y || [];
      }
    }
    const row = await napBai({
      code: ex.id,
      skill: ex.ky_nang_chinh,
      phu: ex.ky_nang_phu || [],
      muc4: MUC4[ex.muc_do_4] || ex.muc_do_4,
      muc3: ex.muc_do_bo_3 || MUC3[ex.muc_do_4] || null,
      bloom: ex.muc_bloom,
      text: ex.de_bai.van_ban,
      latex: ex.de_bai.latex,
      ham,
      origin: "SUPHAM",
      baiLam,
      facts,
      hints,
      corpus,
    });
    if (row.status === "DA_PHAT_HANH") published.push(row.id);
  }

  const may = [
    {
      code: "DH12-NB-01",
      ham: "x**2",
      muc4: "NHAN_BIET",
      muc3: "BIET",
      bloom: "NHAN_BIET",
      skill: "T12.DH.03",
    },
    {
      code: "DH12-TH-02",
      ham: "x**3 - 3*x",
      muc4: "THONG_HIEU",
      muc3: "HIEU",
      bloom: "THONG_HIEU",
      skill: "T12.DH.03",
    },
  ];
  for (const item of may) {
    const solved = await math<{
      dat?: boolean;
      bai_lam?: unknown;
      su_kien?: unknown;
      thang_goi_y?: HintBlock[];
      latex?: string;
    }>("solve", { ham: item.ham });
    if (!solved.dat || !solved.bai_lam) throw new Error(`Không giải được ${item.code}`);
    const row = await napBai({
      code: item.code,
      skill: item.skill,
      phu: [],
      muc4: item.muc4,
      muc3: item.muc3,
      bloom: item.bloom,
      text: `Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số y = ${solved.latex || item.ham}.`,
      latex: solved.latex || item.ham,
      ham: item.ham,
      origin: "MAY_GIAI",
      baiLam: solved.bai_lam,
      facts: solved.su_kien || [],
      hints: solved.thang_goi_y || [],
      corpus,
    });
    if (row.status === "DA_PHAT_HANH") published.push(row.id);
  }

  for (const gen of [
    { dang: "bac_ba", seed: 11 },
    { dang: "trung_phuong", seed: 7 },
    { dang: "huu_ti", seed: 5 },
  ]) {
    const g = await math<{
      loi?: string;
      ham?: string;
      latex?: string;
      de_bai?: string;
      muc_do_4?: string;
      muc_do_bo_3?: string;
      muc_bloom?: string;
      bai_lam?: unknown;
      su_kien?: unknown;
      thang_goi_y?: HintBlock[];
      ky_nang_chinh?: string;
    }>("generate", gen);
    if (g.loi || !g.ham || !g.bai_lam) {
      console.log(`GEN ${gen.dang} không dùng được: ${g.loi || "thiếu hàm"}`);
      continue;
    }
    const row = await napBai({
      code: `GEN-${gen.dang}-${gen.seed}`,
      skill: g.ky_nang_chinh || "T12.DH.03",
      phu: [],
      muc4: g.muc_do_4 || "VAN_DUNG",
      muc3: g.muc_do_bo_3 || "VAN_DUNG",
      bloom: g.muc_bloom || null,
      text: (g.de_bai || "").replace(/\$/g, ""),
      latex: g.latex || g.ham,
      ham: g.ham,
      origin: "THAM_SO_HOA",
      baiLam: g.bai_lam,
      facts: g.su_kien || [],
      hints: g.thang_goi_y || [],
      corpus,
    });
    if (row.status === "DA_PHAT_HANH") published.push(row.id);
  }

  const badSolved = await math<{ dat?: boolean; bai_lam?: { dao_ham?: string }; su_kien?: unknown; thang_goi_y?: HintBlock[] }>(
    "solve",
    { ham: "x**2" },
  );
  if (!badSolved.bai_lam) throw new Error("Không dựng được bài ví dụ bị chặn");
  badSolved.bai_lam.dao_ham = "3*x";
  await napBai({
    code: "DH12-DEMO-CHAN-01",
    skill: "T12.DH.03",
    phu: [],
    muc4: "NHAN_BIET",
    muc3: "BIET",
    bloom: "NHAN_BIET",
    text: "Ví dụ cổng chặn (không giao cho học sinh): lời giải của y = x² bị sửa đạo hàm thành 3x.",
    latex: "x^{2}",
    ham: "x**2",
    origin: "VI_DU_CONG",
    baiLam: badSolved.bai_lam,
    facts: badSolved.su_kien || [],
    hints: badSolved.thang_goi_y || [],
    corpus,
  });

  const levels: Record<string, { mastery: number; muc: string; stuck: number; errors: string[] }[]> = {
    [AN]: [
      { mastery: 0.55, muc: "THONG_HIEU", stuck: 0, errors: [] },
      { mastery: 0.48, muc: "THONG_HIEU", stuck: 0, errors: [] },
      { mastery: 0.22, muc: "NHAN_BIET", stuck: 1, errors: ["ERR.DH.06"] },
      { mastery: 0.4, muc: "THONG_HIEU", stuck: 0, errors: [] },
      { mastery: 0.3, muc: "NHAN_BIET", stuck: 0, errors: [] },
    ],
    [BINH]: [
      { mastery: 0.7, muc: "VAN_DUNG", stuck: 0, errors: [] },
      { mastery: 0.66, muc: "VAN_DUNG", stuck: 0, errors: [] },
      { mastery: 0.68, muc: "VAN_DUNG", stuck: 0, errors: ["ERR.DH.07"] },
      { mastery: 0.64, muc: "VAN_DUNG", stuck: 0, errors: [] },
      { mastery: 0.6, muc: "THONG_HIEU", stuck: 0, errors: [] },
    ],
    [CHI]: [
      { mastery: 0.86, muc: "VAN_DUNG_CAO", stuck: 0, errors: [] },
      { mastery: 0.31, muc: "NHAN_BIET", stuck: 3, errors: ["ERR.DH.01"] },
      { mastery: 0.84, muc: "VAN_DUNG_CAO", stuck: 0, errors: [] },
      { mastery: 0.8, muc: "VAN_DUNG", stuck: 0, errors: [] },
      { mastery: 0.78, muc: "VAN_DUNG", stuck: 0, errors: [] },
    ],
  };
  const core = ["T12.DH.01", "T12.DH.02", "T12.DH.03", "T12.DH.04", "T12.DH.05"];
  for (const [student, rows] of Object.entries(levels)) {
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      await db.insert(masteryStates).values({
        studentId: student,
        skillCode: core[i],
        mastery: row.mastery,
        currentMucDo4: row.muc,
        attempts: 4,
        stuckCounter: row.stuck,
        lastErrorCodes: row.errors,
      });
    }
  }

  await db.insert(escalations).values({
    id: crypto.randomUUID(),
    studentId: CHI,
    skillCode: "T12.DH.02",
    reason: "Kẹt 3 lượt ở T12.DH.02 (tính đạo hàm). Bản ghi tổng hợp để thấy cảnh báo, không phải học sinh thật.",
    createdAt: now,
    handledAt: null,
  });

  const advice: Record<string, string> = {
    [AN]: "An đang ở mức Nhận biết với kỹ năng xét dấu. Mỗi buổi: viết đạo hàm, giải y' = 0, rồi thay một số vào từng khoảng trước khi kết luận. Chưa nhảy sang vận dụng cao.",
    [BINH]: "Bình đang ở mức Vận dụng. Giữ dạng vừa làm được, mỗi tuần thêm một bài nhích lên. Khi kết luận, viết từng khoảng riêng, không gộp qua điểm bị loại.",
    [CHI]: "Chi làm được vận dụng cao ở xét dấu nhưng đang kẹt ở bước đạo hàm. Tuần này chỉ ôn quy tắc đạo hàm (lũy thừa, tổng, thương) với bài ngắn, chưa thêm bài mới khó hơn.",
  };
  const slots = [
    { thu: "Thứ Hai", gio: "19:00", viec: "Ôn công thức đạo hàm" },
    { thu: "Thứ Tư", gio: "19:00", viec: "Làm một bài cùng mức" },
    { thu: "Thứ Sáu", gio: "19:30", viec: "Sửa dạng đã sai" },
    { thu: "Chủ Nhật", gio: "09:00", viec: "Một bài nhích một nấc nếu đủ ngưỡng" },
  ];
  for (const id of [AN, BINH, CHI]) {
    const sid = crypto.randomUUID();
    await db.insert(studySchedules).values({
      id: sid,
      studentId: id,
      weeklySlots: slots,
      methodAdvice: advice[id],
    });
    await db.insert(reminders).values([
      {
        id: crypto.randomUUID(),
        scheduleId: sid,
        channel: "in_app",
        title: "Buổi tối nay",
        body: "Mở một bài đã phát hành và nộp từng bước. Đừng hỏi đáp án.",
        sendAt: "19:00",
        status: "pending",
      },
      {
        id: crypto.randomUUID(),
        scheduleId: sid,
        channel: "in_app",
        title: "Nhắc cuối tuần",
        body: "Xem lại bước bị tô đỏ tuần này trước khi làm bài khó hơn.",
        sendAt: "Chủ Nhật 09:00",
        status: "pending",
      },
    ]);
  }

  for (const student of [AN, BINH, CHI]) {
    for (const problemId of published) {
      await db.insert(assignments).values({
        id: crypto.randomUUID(),
        problemId,
        studentId: student,
        status: "assigned",
      });
    }
  }

  const check = await sql<{ code: string; status: string }[]>`
    select code, status from problems order by code
  `;
  console.table(check);
  const cubic = check.find((r) => r.code === "DH12-03-VD-01");
  if (!cubic || cubic.status !== "DA_PHAT_HANH") {
    throw new Error("Bài mẫu DH12-03-VD-01 không qua cổng phát hành");
  }
  await sql.end();
}

main().catch(async (err) => {
  console.error(err);
  try {
    await sql.end();
  } catch {
    /* đã đóng */
  }
  process.exit(1);
});
