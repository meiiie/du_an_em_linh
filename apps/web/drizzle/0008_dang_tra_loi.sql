-- SP-06(d): bài không theo quy trình 5 bước (đúng/sai, trả lời ngắn, tham số) mang dạng trả lời riêng
ALTER TABLE problems ADD COLUMN IF NOT EXISTS dang_tra_loi text NOT NULL DEFAULT 'TU_LUAN_5_BUOC';
