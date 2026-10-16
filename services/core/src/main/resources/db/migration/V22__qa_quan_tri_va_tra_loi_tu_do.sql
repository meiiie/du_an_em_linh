-- Lane B: giữ nguyên lịch sử và căn cứ đã phát hành khi ẩn khỏi giao diện.
ALTER TABLE users ADD COLUMN archived_at timestamptz;
-- Tăng khi đặt lại mật khẩu hoặc khóa tài khoản; JWT cũ không sống lại khi mở khóa.
ALTER TABLE users ADD COLUMN session_epoch bigint NOT NULL DEFAULT 0 CHECK (session_epoch >= 0);
ALTER TABLE documents ADD COLUMN archived_at timestamptz;
-- Bước nhập dùng chung không phải chủ đề học. Seed nội dung trường đăng ký mã
-- sau khi đọc được nguồn; importer v0 chỉ ghi đúng danh mục từ nguồn v0.
ALTER TABLE step_templates ALTER COLUMN topic_code DROP NOT NULL;
CREATE INDEX verification_runs_ngan_hang ON verification_runs(class_id,subject_id,created_at DESC,id DESC)
    WHERE subject_kind='PROBLEM';
