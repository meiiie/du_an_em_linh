-- 29/09: giáo viên giao bộ bài theo mức cho cả lớp hoặc từng học sinh, có hạn nộp
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS set_name text;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS due_at timestamptz;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS assigned_by uuid;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS assigned_at timestamptz;
CREATE INDEX IF NOT EXISTS assignments_student_idx ON assignments (student_id);
