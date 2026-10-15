-- Lane B: giữ nguyên lịch sử và căn cứ đã phát hành khi ẩn khỏi giao diện.
ALTER TABLE users ADD COLUMN archived_at timestamptz;
ALTER TABLE documents ADD COLUMN archived_at timestamptz;
INSERT INTO topics(code,name,grade) VALUES('TRALOI','Trả lời tự do',12) ON CONFLICT DO NOTHING;
INSERT INTO step_templates(step_code,topic_code,ordinal,input_kind,description)
VALUES('B.TD.TRALOI','TRALOI',1,'DONG','Bài làm tự luận') ON CONFLICT DO NOTHING;
CREATE INDEX verification_runs_ngan_hang ON verification_runs(class_id,subject_id,created_at DESC,id DESC)
    WHERE subject_kind='PROBLEM';
