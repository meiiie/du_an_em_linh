-- Sơ đồ prototype. RBAC thực thi ở tầng ứng dụng.
-- Các policy RLS ở cuối là móc cho pilot; không FORCE vì app dùng một role sở hữu bảng.

CREATE TABLE users (
  id uuid PRIMARY KEY,
  email text UNIQUE NOT NULL,
  password_hash text NOT NULL,
  display_name text NOT NULL,
  birth_year int,
  status text NOT NULL DEFAULT 'active',
  is_synthetic boolean NOT NULL DEFAULT true,
  pseudonym_id text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
  user_id uuid NOT NULL REFERENCES users(id),
  role_code text NOT NULL,
  PRIMARY KEY (user_id, role_code)
);

CREATE TABLE classes (
  id uuid PRIMARY KEY,
  name text NOT NULL,
  grade int NOT NULL,
  year int NOT NULL
);

CREATE TABLE enrollments (
  class_id uuid NOT NULL REFERENCES classes(id),
  user_id uuid NOT NULL REFERENCES users(id),
  role_in_class text NOT NULL,
  PRIMARY KEY (class_id, user_id)
);

CREATE TABLE consent_records (
  id uuid PRIMARY KEY,
  subject_user_id uuid NOT NULL REFERENCES users(id),
  granted_by_user_id uuid REFERENCES users(id),
  purpose_code text NOT NULL,
  policy_version text NOT NULL,
  method text NOT NULL,
  granted_at timestamptz,
  withdrawn_at timestamptz,
  evidence_ref text
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY,
  actor_user_id uuid REFERENCES users(id),
  action text NOT NULL,
  entity text NOT NULL,
  entity_id text,
  at timestamptz NOT NULL DEFAULT now(),
  reason text
);

CREATE TABLE skills (
  code text PRIMARY KEY,
  topic_code text,
  name text NOT NULL,
  description text,
  grade int,
  is_core boolean NOT NULL DEFAULT false
);

CREATE TABLE skill_prerequisites (
  skill_code text NOT NULL REFERENCES skills(code),
  prerequisite_code text NOT NULL REFERENCES skills(code),
  muc_toi_thieu text,
  PRIMARY KEY (skill_code, prerequisite_code)
);

CREATE TABLE error_types (
  code text PRIMARY KEY,
  skill_code text REFERENCES skills(code),
  ma_buoc text,
  name text NOT NULL,
  description text,
  goi_y_sua text
);

CREATE TABLE step_templates (
  ma_buoc text PRIMARY KEY,
  topic_code text NOT NULL,
  thu_tu int NOT NULL,
  dang_nhap text NOT NULL,
  skill_code text REFERENCES skills(code),
  mo_ta text NOT NULL
);

CREATE TABLE documents (
  id uuid PRIMARY KEY,
  title text NOT NULL,
  kind text NOT NULL,
  source text,
  license_status text NOT NULL,
  text_content text NOT NULL DEFAULT '',
  version int NOT NULL DEFAULT 1,
  sha256 text,
  storage_key text,
  uploaded_by uuid REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE formula_sheets (
  id uuid PRIMARY KEY,
  class_id uuid REFERENCES classes(id),
  owner_teacher_id uuid REFERENCES users(id),
  version int NOT NULL,
  status text NOT NULL,
  locked_at timestamptz,
  note text
);

CREATE TABLE formulas (
  id uuid PRIMARY KEY,
  formula_sheet_id uuid NOT NULL REFERENCES formula_sheets(id),
  skill_code text REFERENCES skills(code),
  title text NOT NULL,
  latex text NOT NULL,
  noi_dung text NOT NULL
);

CREATE TABLE problems (
  id uuid PRIMARY KEY,
  code text UNIQUE NOT NULL,
  skill_code text REFERENCES skills(code),
  skill_codes_phu jsonb NOT NULL DEFAULT '[]',
  muc_do_4 text NOT NULL,
  muc_do_bo_3 text,
  bloom_level text,
  difficulty real,
  statement_text text NOT NULL,
  statement_latex text NOT NULL,
  ham_sympy text,
  origin text NOT NULL,
  status text NOT NULL,
  content_hash text NOT NULL,
  created_by uuid REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE solutions (
  problem_id uuid PRIMARY KEY REFERENCES problems(id),
  bai_lam jsonb,
  protected_facts jsonb NOT NULL DEFAULT '[]',
  final_answer text
);

CREATE TABLE hint_levels (
  problem_id uuid NOT NULL REFERENCES problems(id),
  ma_buoc text NOT NULL,
  cap int NOT NULL,
  noi_dung text NOT NULL,
  PRIMARY KEY (problem_id, ma_buoc, cap)
);

CREATE TABLE verification_runs (
  id uuid PRIMARY KEY,
  problem_id uuid NOT NULL REFERENCES problems(id),
  content_hash text NOT NULL,
  overall_status text NOT NULL,
  publish_status text NOT NULL,
  stale boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE verification_tier_results (
  id uuid PRIMARY KEY,
  run_id uuid NOT NULL REFERENCES verification_runs(id),
  tier int NOT NULL,
  status text NOT NULL,
  loai_ket_qua text,
  buoc_sai jsonb,
  error_code text,
  confidence real,
  reason_text text,
  citation jsonb,
  raw_json jsonb
);

CREATE TABLE content_reviews (
  id uuid PRIMARY KEY,
  problem_id uuid NOT NULL REFERENCES problems(id),
  content_hash text NOT NULL,
  reviewer_id uuid NOT NULL REFERENCES users(id),
  decision text NOT NULL,
  note text NOT NULL,
  at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE class_settings (
  class_id uuid PRIMARY KEY REFERENCES classes(id),
  mo_loi_giai_sau_khi_nop boolean NOT NULL DEFAULT false
);

CREATE TABLE assignments (
  id uuid PRIMARY KEY,
  problem_id uuid NOT NULL REFERENCES problems(id),
  student_id uuid NOT NULL REFERENCES users(id),
  status text NOT NULL DEFAULT 'assigned',
  UNIQUE (problem_id, student_id)
);

CREATE TABLE submissions (
  id uuid PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES users(id),
  problem_id uuid NOT NULL REFERENCES problems(id),
  status text NOT NULL,
  nghi_doan_mo boolean NOT NULL DEFAULT false,
  nghi_doan_mo_ly_do text,
  ket_qua text,
  submitted_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE submission_steps (
  id uuid PRIMARY KEY,
  submission_id uuid NOT NULL REFERENCES submissions(id),
  ma_buoc text NOT NULL,
  dong int NOT NULL,
  latex text NOT NULL,
  raw_input text,
  normalized_input text,
  normalizer_version text,
  normalize_status text
);

CREATE TABLE submission_tables (
  id uuid PRIMARY KEY,
  submission_id uuid NOT NULL REFERENCES submissions(id),
  ma_buoc text NOT NULL,
  loai_bang text NOT NULL
);

CREATE TABLE submission_table_cells (
  id uuid PRIMARY KEY,
  table_id uuid NOT NULL REFERENCES submission_tables(id),
  hang text NOT NULL,
  k int,
  gia_tri text
);

CREATE TABLE input_events (
  id uuid PRIMARY KEY,
  submission_id uuid NOT NULL REFERENCES submissions(id),
  ma_buoc text NOT NULL,
  o jsonb,
  gia_tri_cu text,
  gia_tri_moi text,
  thoi_diem timestamptz NOT NULL
);

CREATE TABLE grading_results (
  id uuid PRIMARY KEY,
  submission_id uuid NOT NULL REFERENCES submissions(id),
  ket_qua text NOT NULL,
  loai_ket_qua text,
  buoc_sai jsonb,
  ma_loi text,
  do_tin_cay real,
  per_buoc jsonb,
  thong_bao text
);

CREATE TABLE tutor_sessions (
  id uuid PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES users(id),
  problem_id uuid NOT NULL REFERENCES problems(id),
  state text NOT NULL,
  hint_cap int NOT NULL DEFAULT 0,
  answer_requests int NOT NULL DEFAULT 0,
  same_error_repeats int NOT NULL DEFAULT 0,
  last_buoc text,
  started_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE tutor_messages (
  id uuid PRIMARY KEY,
  session_id uuid NOT NULL REFERENCES tutor_sessions(id),
  role text NOT NULL,
  content text NOT NULL,
  redacted_content text,
  blocked_by_filter boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE llm_calls (
  id uuid PRIMARY KEY,
  purpose text NOT NULL,
  model text,
  provider text NOT NULL,
  pseudonym_id text,
  offline boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE mastery_config (
  key text PRIMARY KEY,
  value jsonb NOT NULL,
  version int NOT NULL DEFAULT 1
);

CREATE TABLE mastery_states (
  student_id uuid NOT NULL REFERENCES users(id),
  skill_code text NOT NULL REFERENCES skills(code),
  mastery real NOT NULL,
  current_muc_do_4 text NOT NULL,
  attempts int NOT NULL DEFAULT 0,
  stuck_counter int NOT NULL DEFAULT 0,
  last_error_codes jsonb NOT NULL DEFAULT '[]',
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (student_id, skill_code)
);

CREATE TABLE mastery_events (
  id uuid PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES users(id),
  skill_code text NOT NULL,
  submission_id uuid REFERENCES submissions(id),
  delta real NOT NULL,
  rule_applied text NOT NULL,
  buoc_sai jsonb,
  ma_loi text,
  do_tin_cay real,
  nghi_doan_mo boolean NOT NULL DEFAULT false,
  at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE study_schedules (
  id uuid PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES users(id),
  weekly_slots jsonb NOT NULL,
  method_advice text NOT NULL,
  created_by text NOT NULL DEFAULT 'he_thong'
);

CREATE TABLE reminders (
  id uuid PRIMARY KEY,
  schedule_id uuid NOT NULL REFERENCES study_schedules(id),
  channel text NOT NULL DEFAULT 'in_app',
  title text NOT NULL,
  body text NOT NULL,
  send_at text,
  status text NOT NULL DEFAULT 'pending'
);

CREATE TABLE escalations (
  id uuid PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES users(id),
  skill_code text NOT NULL,
  reason text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  handled_at timestamptz
);

CREATE TABLE sessions (
  token_hash text PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  expires_at timestamptz NOT NULL
);

CREATE INDEX idx_problems_status ON problems(status);
CREATE INDEX idx_submissions_student ON submissions(student_id, problem_id);
CREATE INDEX idx_tier_run ON verification_tier_results(run_id);

-- Móc RLS cho pilot (chưa FORCE). App prototype kiểm tra vai trò trong code.
ALTER TABLE submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE mastery_states ENABLE ROW LEVEL SECURITY;
CREATE POLICY submissions_owner ON submissions
  USING (student_id::text = current_setting('app.user_id', true) OR current_setting('app.role', true) = 'GV');
CREATE POLICY mastery_owner ON mastery_states
  USING (student_id::text = current_setting('app.user_id', true) OR current_setting('app.role', true) = 'GV');
