# ADR 008 — Kết nối ChatGPT chính thức và kho kiến thức cho gia sư

## Bối cảnh

Giáo viên không chuyên không muốn tạo biến môi trường. Họ muốn «đăng nhập ChatGPT». Tới 2026-09-27:

- [Sign in with ChatGPT](https://help.openai.com/en/articles/20001410-sign-in-with-chatgpt) là **định danh** (tên/email) cho đối tác đã được cấp `client_id`. Không cấp quyền gọi mô hình.
- Device-OAuth / client_id Codex-CLI (PR #3, Docker Agent «chatgpt provider») dùng tài khoản Plus như API — **không chính thức**.
- Khóa API trên [platform.openai.com/api-keys](https://platform.openai.com/api-keys) là đường chính thức: cùng tài khoản ChatGPT.

Kong et al., IJCAI 2026: LLM-ITS cần truy hồi kho (D2) trước khi sinh, để giảm ảo giác. Lewis et al. 2020 (RAG). Aleven: gợi ý nguyên lý, không bottom-out.

## Quyết định

- Màn «Kết nối ChatGPT»: hai bước (mở ChatGPT/trang khóa → dán một lần). Học sinh không thấy khóa. Trạng thái đã kết nối ẩn form.
- OAuth PKCE chỉ chạy khi có `OPENAI_OAUTH_CLIENT_ID` do OpenAI cấp. Lưu email/sub, không lấy client_id nội bộ / Codex.
- Gia sư đọc kho lớp (tài liệu quyền rõ + công thức khóa) bằng cụm từ tầng 2/3, ưu tiên từ khóa bước (Kong D2; KITE 2026: nhét đoạn đã truy hồi). Không đọc `solutions` / `protected_facts`. Không pgvector.
- Giáo viên và học sinh xem cùng bản đồ «gia sư đọc theo 5 bước».

## Hệ quả

Thiếu khóa → offline. OAuth không đăng ký → nút «Tiếp tục với ChatGPT» ẩn, đường khóa vẫn dùng được. Trích đoạn khớp cả chữ không dấu.
