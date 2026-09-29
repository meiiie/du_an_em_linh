-- Bài khung ngắn (Sư phạm bai-khung-ngan, SP-06): bước đầu tiên học sinh làm; NULL = đủ 5 bước từ TXĐ
ALTER TABLE problems ADD COLUMN IF NOT EXISTS buoc_bat_dau text;
-- Luật dấu U (0002c): cờ toan_dung của lần chấm (true = lỗi trình bày, toán đúng; NULL = không áp dụng / bộ chấm cũ)
ALTER TABLE grading_results ADD COLUMN IF NOT EXISTS toan_dung boolean
