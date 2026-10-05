-- Nộp bài có căn cứ (T020 phần 2, #87). Bài làm đã nộp ghim lần chấm làm căn cứ cho kết quả (result_grading_id): kết quả
-- nộp là phán quyết của một lần chấm của chính bài làm đó, không ai đặt tay được (FR-009), và màn giáo viên đọc lại lỗi
-- từng bước của bài đã nộp bằng id, kể cả sau khi đề đổi (băm yêu cầu chấm của đề cũ không dựng lại được).
--   1. DA_NOP khi và chỉ khi có căn cứ (CHECK).
--   2. Căn cứ thuộc chính bài làm này và có cùng kết quả: khóa ngoại nhiều cột (result_grading_id, id, result) tới
--      (id, submission_id, result) của grading_results. result của submissions không bao giờ là KHONG_CHAM_DUOC (V7), nên
--      căn cứ luôn là một lần chấm có phán quyết. MATCH SIMPLE: bài làm đang làm (ba cột căn cứ, kết quả trống theo CHECK)
--      không bị kiểm.
-- Trước V8 chưa có nơi nào nộp bài (Submission.submit chỉ test gọi), nên không có dòng DA_NOP nào phải ghim.
-- Khóa ngoại vòng với grading_results.submission_id (ON DELETE CASCADE): xóa bài làm (hay lớp, học sinh, bài) xóa luôn các
-- lần chấm của nó; khóa ngoại ở đây kiểm cuối câu lệnh, lúc dòng bài làm trỏ tới đã đi, nên dây chuyền vẫn chạy.

ALTER TABLE grading_results ADD CONSTRAINT grading_results_can_cu UNIQUE (id, submission_id, result);

ALTER TABLE submissions ADD COLUMN result_grading_id uuid;
ALTER TABLE submissions ADD CONSTRAINT submissions_nop_co_can_cu CHECK ((status = 'DA_NOP') = (result_grading_id IS NOT NULL));
ALTER TABLE submissions ADD CONSTRAINT submissions_can_cu_cua_chinh_bai_lam
    FOREIGN KEY (result_grading_id, id, result) REFERENCES grading_results (id, submission_id, result);
-- Xóa lần chấm (dây chuyền) kiểm khóa ngoại trên cột này.
CREATE INDEX submissions_can_cu ON submissions (result_grading_id);
