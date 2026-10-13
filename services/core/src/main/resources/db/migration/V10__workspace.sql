-- Assignment policy and personal study sessions; audit contains actions/reasons, never credentials or tutor prompts.
ALTER TABLE assignments ADD COLUMN mode varchar(10) NOT NULL DEFAULT 'ON_TAP' CHECK (mode IN ('ON_TAP','KIEM_TRA'));
CREATE TABLE study_sessions (
 id uuid PRIMARY KEY,
 student_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 title varchar(200) NOT NULL CHECK (btrim(title) <> ''),
 starts_at timestamptz NOT NULL,
 ends_at timestamptz NOT NULL,
 completed boolean NOT NULL DEFAULT false,
 CHECK (ends_at > starts_at AND ends_at <= starts_at + interval '8 hours')
);
CREATE INDEX study_sessions_student ON study_sessions(student_id,starts_at);
CREATE TABLE audit_log (
 id uuid PRIMARY KEY,
 actor_id uuid REFERENCES users(id) ON DELETE SET NULL,
 action varchar(40) NOT NULL,
 target varchar(100) NOT NULL,
 reason varchar(500) NOT NULL,
 created_at timestamptz NOT NULL
);
CREATE TABLE review_blocks (
 run_id uuid PRIMARY KEY REFERENCES verification_runs(id),
 teacher_id uuid REFERENCES users(id) ON DELETE SET NULL,
 reason varchar(500) NOT NULL CHECK (btrim(reason) <> ''),
 created_at timestamptz NOT NULL
);
-- A rejected run cannot later be approved, including a direct write bypassing the REST use case.
CREATE FUNCTION workspace_rejected_run() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.overall_status = 'GV_DUYET' AND EXISTS (SELECT 1 FROM review_blocks WHERE run_id=NEW.id) THEN
  RAISE EXCEPTION 'Lượt kiểm đã bị giáo viên chặn' USING ERRCODE='check_violation';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER workspace_rejected_run BEFORE UPDATE ON verification_runs FOR EACH ROW EXECUTE FUNCTION workspace_rejected_run();
-- Closing a due date is authoritative, including direct calls to practice endpoints.
CREATE FUNCTION workspace_assignment_deadline() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s submissions%ROWTYPE;
BEGIN
 IF TG_TABLE_NAME='submissions' THEN s:=NEW;
 ELSE SELECT * INTO s FROM submissions WHERE id=NEW.submission_id;
 END IF;
 IF EXISTS(SELECT 1 FROM assignments a WHERE a.class_id=s.class_id AND a.student_id=s.student_id AND a.problem_id=s.problem_id AND a.status='DA_GIAO' AND a.due_at<=clock_timestamp()) THEN
  RAISE EXCEPTION 'Bài đã hết hạn nộp' USING ERRCODE='check_violation';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER workspace_assignment_deadline BEFORE INSERT OR UPDATE ON submissions FOR EACH ROW EXECUTE FUNCTION workspace_assignment_deadline();
CREATE TRIGGER workspace_assignment_deadline BEFORE INSERT OR UPDATE ON submission_steps FOR EACH ROW EXECUTE FUNCTION workspace_assignment_deadline();
CREATE TRIGGER workspace_assignment_deadline BEFORE INSERT OR UPDATE ON submission_tables FOR EACH ROW EXECUTE FUNCTION workspace_assignment_deadline();
CREATE TRIGGER workspace_assignment_deadline BEFORE INSERT ON grading_results FOR EACH ROW EXECUTE FUNCTION workspace_assignment_deadline();
