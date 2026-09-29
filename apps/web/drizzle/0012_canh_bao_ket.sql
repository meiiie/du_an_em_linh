-- UX-09 / UXT-07-k: cảnh báo kẹt gắn bài + bước, phân biệt lời nhờ của học sinh, giáo viên đánh dấu «Đã xử lý»
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS problem_id uuid;
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS ma_buoc text;
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS loai text NOT NULL DEFAULT 'KET';
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS handled_by uuid;
