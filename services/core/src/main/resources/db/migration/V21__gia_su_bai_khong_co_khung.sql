-- Gia sư vẫn mở cho bài tự luận trên giấy chưa có khung chấm từng bước.
-- NULL là ngữ cảnh toàn bài (API: BAI_LAM); các bước thật vẫn giữ khóa ngoại.
ALTER TABLE yeu_cau_gia_su ALTER COLUMN ma_buoc DROP NOT NULL;
