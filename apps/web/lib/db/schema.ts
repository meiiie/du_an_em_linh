import { boolean, integer, jsonb, pgTable, primaryKey, real, text, timestamp, uuid } from "drizzle-orm/pg-core";

export const users = pgTable("users", {
  id: uuid("id").primaryKey(),
  email: text("email").notNull(),
  passwordHash: text("password_hash").notNull(),
  displayName: text("display_name").notNull(),
  birthYear: integer("birth_year"),
  status: text("status").notNull(),
  isSynthetic: boolean("is_synthetic").notNull(),
  pseudonymId: text("pseudonym_id").notNull(),
});

export const userRoles = pgTable(
  "user_roles",
  {
    userId: uuid("user_id").notNull(),
    roleCode: text("role_code").notNull(),
  },
  (t) => [primaryKey({ columns: [t.userId, t.roleCode] })],
);

export const classes = pgTable("classes", {
  id: uuid("id").primaryKey(),
  name: text("name").notNull(),
  grade: integer("grade").notNull(),
  year: integer("year").notNull(),
});

export const enrollments = pgTable(
  "enrollments",
  {
    classId: uuid("class_id").notNull(),
    userId: uuid("user_id").notNull(),
    roleInClass: text("role_in_class").notNull(),
  },
  (t) => [primaryKey({ columns: [t.classId, t.userId] })],
);

export const consentRecords = pgTable("consent_records", {
  id: uuid("id").primaryKey(),
  subjectUserId: uuid("subject_user_id").notNull(),
  grantedByUserId: uuid("granted_by_user_id"),
  purposeCode: text("purpose_code").notNull(),
  policyVersion: text("policy_version").notNull(),
  method: text("method").notNull(),
  grantedAt: timestamp("granted_at", { withTimezone: true }),
  withdrawnAt: timestamp("withdrawn_at", { withTimezone: true }),
});

export const auditLogs = pgTable("audit_logs", {
  id: uuid("id").primaryKey(),
  actorUserId: uuid("actor_user_id"),
  action: text("action").notNull(),
  entity: text("entity").notNull(),
  entityId: text("entity_id"),
  at: timestamp("at", { withTimezone: true }).notNull(),
  reason: text("reason"),
});

export const skills = pgTable("skills", {
  code: text("code").primaryKey(),
  topicCode: text("topic_code"),
  name: text("name").notNull(),
  description: text("description"),
  grade: integer("grade"),
  isCore: boolean("is_core").notNull(),
});

export const errorTypes = pgTable("error_types", {
  code: text("code").primaryKey(),
  skillCode: text("skill_code"),
  maBuoc: text("ma_buoc"),
  name: text("name").notNull(),
  goiYSua: text("goi_y_sua"),
});

export const skillPrerequisites = pgTable(
  "skill_prerequisites",
  {
    skillCode: text("skill_code").notNull(),
    prerequisiteCode: text("prerequisite_code").notNull(),
    mucToiThieu: text("muc_toi_thieu"),
  },
  (t) => [primaryKey({ columns: [t.skillCode, t.prerequisiteCode] })],
);

export const stepTemplates = pgTable("step_templates", {
  maBuoc: text("ma_buoc").primaryKey(),
  topicCode: text("topic_code").notNull(),
  thuTu: integer("thu_tu").notNull(),
  dangNhap: text("dang_nhap").notNull(),
  skillCode: text("skill_code"),
  moTa: text("mo_ta").notNull(),
});

export const documents = pgTable("documents", {
  id: uuid("id").primaryKey(),
  title: text("title").notNull(),
  kind: text("kind").notNull(),
  source: text("source"),
  licenseStatus: text("license_status").notNull(),
  textContent: text("text_content").notNull(),
  version: integer("version").notNull(),
  uploadedBy: uuid("uploaded_by"),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull(),
});

export const formulaSheets = pgTable("formula_sheets", {
  id: uuid("id").primaryKey(),
  classId: uuid("class_id"),
  ownerTeacherId: uuid("owner_teacher_id"),
  version: integer("version").notNull(),
  status: text("status").notNull(),
  lockedAt: timestamp("locked_at", { withTimezone: true }),
  note: text("note"),
});

export const formulas = pgTable("formulas", {
  id: uuid("id").primaryKey(),
  formulaSheetId: uuid("formula_sheet_id").notNull(),
  skillCode: text("skill_code"),
  title: text("title").notNull(),
  latex: text("latex").notNull(),
  noiDung: text("noi_dung").notNull(),
});

export const problems = pgTable("problems", {
  id: uuid("id").primaryKey(),
  code: text("code").notNull(),
  skillCode: text("skill_code"),
  skillCodesPhu: jsonb("skill_codes_phu").notNull(),
  mucDo4: text("muc_do_4").notNull(),
  mucDoBo3: text("muc_do_bo_3"),
  bloomLevel: text("bloom_level"),
  difficulty: real("difficulty"),
  statementText: text("statement_text").notNull(),
  statementLatex: text("statement_latex").notNull(),
  hamSympy: text("ham_sympy"),
  // SP-06(d): dạng trả lời. TU_LUAN_5_BUOC = khung 5 bước; TN_DUNG_SAI, TRA_LOI_NGAN... là dạng khác (chưa có khung làm trên app)
  dangTraLoi: text("dang_tra_loi").notNull().default("TU_LUAN_5_BUOC"),
  buocBatDau: text("buoc_bat_dau"),
  origin: text("origin").notNull(),
  status: text("status").notNull(),
  contentHash: text("content_hash").notNull(),
  createdBy: uuid("created_by"),
});

export const solutions = pgTable("solutions", {
  problemId: uuid("problem_id").primaryKey(),
  baiLam: jsonb("bai_lam"),
  protectedFacts: jsonb("protected_facts").notNull(),
  finalAnswer: text("final_answer"),
});

export const hintLevels = pgTable(
  "hint_levels",
  {
    problemId: uuid("problem_id").notNull(),
    maBuoc: text("ma_buoc").notNull(),
    cap: integer("cap").notNull(),
    noiDung: text("noi_dung").notNull(),
  },
  (t) => [primaryKey({ columns: [t.problemId, t.maBuoc, t.cap] })],
);

export const verificationRuns = pgTable("verification_runs", {
  id: uuid("id").primaryKey(),
  problemId: uuid("problem_id").notNull(),
  contentHash: text("content_hash").notNull(),
  overallStatus: text("overall_status").notNull(),
  publishStatus: text("publish_status").notNull(),
  stale: boolean("stale").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull(),
});

export const verificationTierResults = pgTable("verification_tier_results", {
  id: uuid("id").primaryKey(),
  runId: uuid("run_id").notNull(),
  tier: integer("tier").notNull(),
  status: text("status").notNull(),
  loaiKetQua: text("loai_ket_qua"),
  buocSai: jsonb("buoc_sai"),
  errorCode: text("error_code"),
  confidence: real("confidence"),
  reasonText: text("reason_text"),
  citation: jsonb("citation"),
  rawJson: jsonb("raw_json"),
});

export const contentReviews = pgTable("content_reviews", {
  id: uuid("id").primaryKey(),
  problemId: uuid("problem_id").notNull(),
  contentHash: text("content_hash").notNull(),
  reviewerId: uuid("reviewer_id").notNull(),
  decision: text("decision").notNull(),
  note: text("note").notNull(),
  at: timestamp("at", { withTimezone: true }).notNull(),
});

export const classSettings = pgTable("class_settings", {
  classId: uuid("class_id").primaryKey(),
  moLoiGiaiSauKhiNop: boolean("mo_loi_giai_sau_khi_nop").notNull(),
  aiProvider: text("ai_provider").notNull().default("offline"),
  aiModel: text("ai_model"),
  aiAllowLocal: boolean("ai_allow_local").notNull().default(true),
  aiApiKey: text("ai_api_key"),
  aiOpenaiSub: text("ai_openai_sub"),
  aiOpenaiEmail: text("ai_openai_email"),
  aiConnectedAt: timestamp("ai_connected_at", { withTimezone: true }),
});

export const assignments = pgTable("assignments", {
  id: uuid("id").primaryKey(),
  problemId: uuid("problem_id").notNull(),
  studentId: uuid("student_id").notNull(),
  status: text("status").notNull(),
  setName: text("set_name"),
  dueAt: timestamp("due_at", { withTimezone: true }),
  assignedBy: uuid("assigned_by"),
  assignedAt: timestamp("assigned_at", { withTimezone: true }),
});

export const submissionSteps = pgTable("submission_steps", {
  id: uuid("id").primaryKey(),
  submissionId: uuid("submission_id").notNull(),
  maBuoc: text("ma_buoc").notNull(),
  dong: integer("dong").notNull(),
  latex: text("latex").notNull(),
  rawInput: text("raw_input"),
  normalizedInput: text("normalized_input"),
  normalizerVersion: text("normalizer_version"),
  normalizeStatus: text("normalize_status"),
});

export const submissionTables = pgTable("submission_tables", {
  id: uuid("id").primaryKey(),
  submissionId: uuid("submission_id").notNull(),
  maBuoc: text("ma_buoc").notNull(),
  loaiBang: text("loai_bang").notNull(),
});

export const submissionTableCells = pgTable("submission_table_cells", {
  id: uuid("id").primaryKey(),
  tableId: uuid("table_id").notNull(),
  hang: text("hang").notNull(),
  k: integer("k"),
  giaTri: text("gia_tri"),
});

export const inputEvents = pgTable("input_events", {
  id: uuid("id").primaryKey(),
  submissionId: uuid("submission_id").notNull(),
  maBuoc: text("ma_buoc").notNull(),
  o: jsonb("o"),
  giaTriCu: text("gia_tri_cu"),
  giaTriMoi: text("gia_tri_moi"),
  thoiDiem: timestamp("thoi_diem", { withTimezone: true }).notNull(),
});

export const submissions = pgTable("submissions", {
  id: uuid("id").primaryKey(),
  studentId: uuid("student_id").notNull(),
  problemId: uuid("problem_id").notNull(),
  status: text("status").notNull(),
  nghiDoanMo: boolean("nghi_doan_mo").notNull(),
  nghiDoanMoLyDo: text("nghi_doan_mo_ly_do"),
  ketQua: text("ket_qua"),
  submittedAt: timestamp("submitted_at", { withTimezone: true }).notNull(),
});

export const gradingResults = pgTable("grading_results", {
  id: uuid("id").primaryKey(),
  submissionId: uuid("submission_id").notNull(),
  ketQua: text("ket_qua").notNull(),
  loaiKetQua: text("loai_ket_qua"),
  buocSai: jsonb("buoc_sai"),
  maLoi: text("ma_loi"),
  doTinCay: real("do_tin_cay"),
  perBuoc: jsonb("per_buoc"),
  thongBao: text("thong_bao"),
  cacVanDe: jsonb("cac_van_de"),
  toanDung: boolean("toan_dung"),
});

export const tutorSessions = pgTable("tutor_sessions", {
  id: uuid("id").primaryKey(),
  studentId: uuid("student_id").notNull(),
  problemId: uuid("problem_id").notNull(),
  state: text("state").notNull(),
  hintCap: integer("hint_cap").notNull(),
  answerRequests: integer("answer_requests").notNull(),
  sameErrorRepeats: integer("same_error_repeats").notNull(),
  lastBuoc: text("last_buoc"),
  hintCaps: jsonb("hint_caps"),
  answerRequestsBuoc: text("answer_requests_buoc"),
  startedAt: timestamp("started_at", { withTimezone: true }).defaultNow().notNull(),
});

export const tutorMessages = pgTable("tutor_messages", {
  id: uuid("id").primaryKey(),
  sessionId: uuid("session_id").notNull(),
  role: text("role").notNull(),
  content: text("content").notNull(),
  redactedContent: text("redacted_content"),
  blockedByFilter: boolean("blocked_by_filter").notNull(),
  citation: jsonb("citation"),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow().notNull(),
});

export const llmCalls = pgTable("llm_calls", {
  id: uuid("id").primaryKey(),
  purpose: text("purpose").notNull(),
  model: text("model"),
  provider: text("provider").notNull(),
  pseudonymId: text("pseudonym_id"),
  offline: boolean("offline").notNull(),
});

export const masteryConfig = pgTable("mastery_config", {
  key: text("key").primaryKey(),
  value: jsonb("value").notNull(),
  version: integer("version").notNull(),
});

export const masteryStates = pgTable(
  "mastery_states",
  {
    studentId: uuid("student_id").notNull(),
    skillCode: text("skill_code").notNull(),
    mastery: real("mastery").notNull(),
    currentMucDo4: text("current_muc_do_4").notNull(),
    attempts: integer("attempts").notNull(),
    stuckCounter: integer("stuck_counter").notNull(),
    lastErrorCodes: jsonb("last_error_codes").notNull(),
  },
  (t) => [primaryKey({ columns: [t.studentId, t.skillCode] })],
);

export const masteryEvents = pgTable("mastery_events", {
  id: uuid("id").primaryKey(),
  studentId: uuid("student_id").notNull(),
  skillCode: text("skill_code").notNull(),
  submissionId: uuid("submission_id"),
  delta: real("delta").notNull(),
  ruleApplied: text("rule_applied").notNull(),
  buocSai: jsonb("buoc_sai"),
  maLoi: text("ma_loi"),
  doTinCay: real("do_tin_cay"),
  nghiDoanMo: boolean("nghi_doan_mo").notNull(),
});

export const studySchedules = pgTable("study_schedules", {
  id: uuid("id").primaryKey(),
  studentId: uuid("student_id").notNull(),
  weeklySlots: jsonb("weekly_slots").notNull(),
  methodAdvice: text("method_advice").notNull(),
});

export const reminders = pgTable("reminders", {
  id: uuid("id").primaryKey(),
  scheduleId: uuid("schedule_id").notNull(),
  channel: text("channel").notNull(),
  title: text("title").notNull(),
  body: text("body").notNull(),
  sendAt: text("send_at"),
  status: text("status").notNull(),
});

export const escalations = pgTable("escalations", {
  id: uuid("id").primaryKey(),
  studentId: uuid("student_id").notNull(),
  skillCode: text("skill_code").notNull(),
  reason: text("reason").notNull(),
  /** UX-09: bài và bước của cảnh báo (NULL = cảnh báo theo kỹ năng, không gắn bài). */
  problemId: uuid("problem_id"),
  maBuoc: text("ma_buoc"),
  /** KET = máy phát hiện kẹt; NHO_GV = học sinh bấm «Gửi thầy cô». */
  loai: text("loai").notNull().default("KET"),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow().notNull(),
  handledAt: timestamp("handled_at", { withTimezone: true }),
  handledBy: uuid("handled_by"),
});

export const sessions = pgTable("sessions", {
  tokenHash: text("token_hash").primaryKey(),
  userId: uuid("user_id").notNull(),
  expiresAt: timestamp("expires_at", { withTimezone: true }).notNull(),
});
