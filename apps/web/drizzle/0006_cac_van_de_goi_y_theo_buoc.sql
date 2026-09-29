-- 29/09: danh sách đủ vấn đề của một lần chấm (giáo viên và mô hình thành thạo dùng cả danh sách)
ALTER TABLE grading_results ADD COLUMN IF NOT EXISTS cac_van_de jsonb;
-- Cấp gợi ý theo (bước, loại lỗi) thay cho một bộ đếm chung cả bài; số lần xin đáp án theo bước
ALTER TABLE tutor_sessions ADD COLUMN IF NOT EXISTS hint_caps jsonb;
ALTER TABLE tutor_sessions ADD COLUMN IF NOT EXISTS answer_requests_buoc text;
