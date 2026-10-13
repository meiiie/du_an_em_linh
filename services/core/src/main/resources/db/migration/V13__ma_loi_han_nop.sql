-- Giữ cổng hạn của V10; mã SQL riêng giúp API trả 403 HET_HAN thay vì 409 dữ liệu trùng.
CREATE OR REPLACE FUNCTION workspace_assignment_deadline() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s submissions%ROWTYPE;
BEGIN
 IF TG_TABLE_NAME='submissions' THEN s:=NEW;
 ELSE SELECT * INTO s FROM submissions WHERE id=NEW.submission_id;
 END IF;
 IF EXISTS(SELECT 1 FROM assignments a WHERE a.class_id=s.class_id AND a.student_id=s.student_id AND a.problem_id=s.problem_id AND a.status='DA_GIAO' AND a.due_at<=clock_timestamp()) THEN
  RAISE EXCEPTION 'Bài đã hết hạn nộp' USING ERRCODE='P0403';
 END IF;
 RETURN NEW;
END $$;
