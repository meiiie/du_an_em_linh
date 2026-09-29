-- F-08 lớp phòng thủ thứ hai: Row Level Security (FORCE) trên dữ liệu học của HS.
-- Khoá theo current_setting('app.user_id', true), đặt bằng set_config(..., true) trong một giao dịch (lib/rls.ts).
-- Chưa đặt app.user_id (tác vụ hệ thống: seed, reset test, chấm, báo cáo GV đã lọc theo lớp ở tầng ứng dụng) thì cho qua.
-- Lưu ý: superuser Postgres luôn bỏ qua RLS; FORCE có hiệu lực với chủ bảng không phải superuser (như DB Render).
CREATE OR REPLACE FUNCTION rls_duoc_xem_hs(hs uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
  select coalesce(nullif(current_setting('app.user_id', true), ''), '') = ''
    or hs::text = current_setting('app.user_id', true)
    or exists (
      select 1 from enrollments g join enrollments h on h.class_id = g.class_id
      where g.user_id::text = current_setting('app.user_id', true) and g.role_in_class = 'GV'
        and h.user_id = hs and h.role_in_class = 'HS'
    )
$$;

ALTER TABLE submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE submissions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS rls_hs ON submissions;
CREATE POLICY rls_hs ON submissions USING (rls_duoc_xem_hs(student_id)) WITH CHECK (rls_duoc_xem_hs(student_id));

ALTER TABLE tutor_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE tutor_sessions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS rls_hs ON tutor_sessions;
CREATE POLICY rls_hs ON tutor_sessions USING (rls_duoc_xem_hs(student_id)) WITH CHECK (rls_duoc_xem_hs(student_id));

ALTER TABLE mastery_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE mastery_states FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS rls_hs ON mastery_states;
CREATE POLICY rls_hs ON mastery_states USING (rls_duoc_xem_hs(student_id)) WITH CHECK (rls_duoc_xem_hs(student_id));

ALTER TABLE grading_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE grading_results FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS rls_hs ON grading_results;
CREATE POLICY rls_hs ON grading_results
  USING (exists (select 1 from submissions s where s.id = submission_id))
  WITH CHECK (exists (select 1 from submissions s where s.id = submission_id));

ALTER TABLE tutor_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE tutor_messages FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS rls_hs ON tutor_messages;
CREATE POLICY rls_hs ON tutor_messages
  USING (exists (select 1 from tutor_sessions t where t.id = session_id))
  WITH CHECK (exists (select 1 from tutor_sessions t where t.id = session_id));
