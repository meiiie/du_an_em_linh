-- Bước của bài có khung riêng (exercise_step_frames, ví dụ B.XS.*) không nằm trong step_templates.
-- Core kiểm bước thuộc đúng phiên bản đề trước khi ghi, nên bỏ khóa ngoại cứng sang step_templates.
ALTER TABLE yeu_cau_gia_su DROP CONSTRAINT IF EXISTS yeu_cau_gia_su_ma_buoc_fkey;
