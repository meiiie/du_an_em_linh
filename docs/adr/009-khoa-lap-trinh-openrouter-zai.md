# ADR 009 — Khóa lập trình OpenRouter và khóa coding Z.AI

## Bối cảnh

Giáo viên muốn dán khóa API nhà khác ngoài ChatGPT. Yêu cầu: khóa **lập trình / coding**, không API chat tiêu dùng, không đăng nhập chatgpt.com, không nhập URL (SSRF).

Tài liệu chính thức tới 2026-09-27:

- OpenRouter: `https://openrouter.ai/api/v1` + Bearer, trang khóa `https://openrouter.ai/keys`. OpenAI-compatible `POST /chat/completions`.
- Z.AI Coding Plan (OpenAI Chat Completions): `https://api.z.ai/api/coding/paas/v4`. Khác API chat tiêu dùng `https://api.z.ai/api/paas/v4`. Trang khóa `https://z.ai/manage-apikey/apikey-list`.

## Quyết định

- Hai nhà mới: `openrouter`, `zai`. Cùng luật ADR 007 (một lần HTTP, không fallback, không stream).
- Địa chỉ cứng trong mã. Không form URL. `LLM_BASE_URL` không đổi nhà này.
- Một ô khóa lớp; nghĩa theo nhà đang chọn. Env: `OPENROUTER_API_KEY` / `ZAI_API_KEY`.
- Mô hình mặc định lập trình / coding: `qwen/qwen3-coder` (OpenRouter), `glm-5.3-flashx` (Z.AI). Giáo viên đổi được.
- Màn Gia sư: hai bước như ChatGPT — mở trang khóa → dán một lần. Heading e2e «Kết nối ChatGPT» giữ.

## Hệ quả

Thiếu khóa nhà đã chọn → lỗi rõ, không giả thang gợi ý. Học sinh chỉ thấy nhà lớp đã bật. Probe vẫn `GET /models`.

Khóa lớp không dùng chung giữa các nhà: lớp chọn Z.AI thì `completeChat`/`probe` nhà cloud hoặc OpenRouter không lấy khóa đó làm Bearer. `cauHinhCongKhai` chỉ bật cờ sẵn của đúng nhà (env riêng vẫn được).
