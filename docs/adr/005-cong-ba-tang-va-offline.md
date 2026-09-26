# ADR 005 — Cổng ba tầng và gia sư không cần khóa API

Phát hành chỉ khi cả ba tầng Đạt, hoặc giáo viên duyệt các tầng Không kiểm được (ghi ai, lúc nào, vì sao → `GV_DUYET`). Bất kỳ Sai nào thì Bị chặn. Đổi nội dung đổi hash và phải kiểm lại. Đổi bảng công thức đánh dấu các lần kiểm cũ là `stale`; bài đã phát hành không bị gỡ tự động.

Tầng 2 của nguyên mẫu là tìm cụm từ trên văn bản đã nạp, bắt buộc có trích dẫn, bỏ qua tài liệu `chua_ro`. Không dùng pgvector. Không có trích dẫn thì Không kiểm được.

Cổng LLM nhận `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`. Không có khóa thì dùng câu mẫu tiếng Việt dựng từ thang gợi ý. Mọi lời gửi ra ngoài bị xóa email, số điện thoại và tên demo. Demo chạy hết không cần khóa.
