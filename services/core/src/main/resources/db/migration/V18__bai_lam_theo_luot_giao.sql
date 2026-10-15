-- Chụp thời điểm lượt giao để bài làm dở cũ vẫn được lưu, nhưng không chặn lượt mới.
ALTER TABLE submissions ADD COLUMN assignment_at timestamptz NOT NULL DEFAULT 'epoch';
ALTER TABLE submissions DISABLE TRIGGER submissions_chi_nop_mot_lan;
UPDATE submissions s SET assignment_at=a.assigned_at FROM assignments a
WHERE a.class_id=s.class_id AND a.student_id=s.student_id AND a.problem_id=s.problem_id
AND s.started_at>=a.assigned_at;
ALTER TABLE submissions ENABLE TRIGGER submissions_chi_nop_mot_lan;
DROP INDEX submissions_mot_bai_dang_lam;
CREATE UNIQUE INDEX submissions_mot_bai_dang_lam
ON submissions(student_id,class_id,problem_id,content_version,assignment_at) WHERE status='DANG_LAM';

-- Mọi nơi ghi (kể cả seed/import) đều chụp cùng lượt giao.
CREATE FUNCTION submissions_chup_luot_giao() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    SELECT coalesce((SELECT a.assigned_at FROM assignments a
        WHERE a.class_id=NEW.class_id AND a.student_id=NEW.student_id AND a.problem_id=NEW.problem_id
        AND a.status='DA_GIAO' AND a.assigned_at<=NEW.started_at),'epoch'::timestamptz)
    INTO NEW.assignment_at;
    RETURN NEW;
END $$;
CREATE TRIGGER submissions_chup_luot_giao BEFORE INSERT ON submissions
FOR EACH ROW EXECUTE FUNCTION submissions_chup_luot_giao();
