-- Bài không theo khung năm bước hỏi gia sư ở phạm vi toàn bài (ma_buoc NULL).
-- Giữ khóa ngoại để mã bước được gửi tường minh vẫn phải tồn tại.
-- Quyền học sinh/lớp, chế độ và hạn giao bài vẫn kiểm trong tầng ứng dụng.
ALTER TABLE yeu_cau_gia_su ALTER COLUMN ma_buoc DROP NOT NULL;
