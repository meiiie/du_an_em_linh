# ADR 007 — Nhà gia sư tường minh: khóa chính thức và mô hình local

## Bối cảnh

`callLLM` cũ: có `LLM_API_KEY` thì gọi một URL; lỗi thì trả câu mẫu như thể mô hình đã trả lời. Học sinh không biết đang offline. Giáo viên không chọn Ollama/LM Studio. PR [lms-ibm-bob-hackathon#3](https://github.com/meiiie/lms-ibm-bob-hackathon/pull/3) thêm ChatGPT + trợ lý local nhưng dùng device-OAuth không chính thức.

## Quyết định

- Bốn nhà: `offline` (mặc định) · `cloud` · `ollama` · `lmstudio`.
- Cloud = khóa API chính thức (env hoặc khóa lớp). Không đăng nhập chatgpt.com.
- Local chỉ `127.0.0.1` / `localhost` / `::1`, cổng mặc định Ollama 11434 và LM Studio 1234 (env được ghi đè nếu vẫn loopback).
- Một lần gọi, không retry, không fallback nhà khác, không stream (cần cả câu cho bộ lọc).
- Giáo viên đặt nhà lớp; học sinh được chọn offline và local nếu cửa lớp mở.

## Hệ quả

- Thiếu khóa mà chọn cloud → lỗi rõ, không giả thang gợi ý.
- Ollama tắt → lỗi rõ; phiếu và phiên đăng nhập còn.
- Thêm nhà mới phải là OpenAI-compatible `/v1/chat/completions` và đi qua cùng luật loopback / https.
