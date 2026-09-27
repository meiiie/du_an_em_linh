# Harness gia sư AI

Quyết định: `docs/adr/007-ai-providers.md`. Sư phạm: `docs/adr/003-gia-su-khong-doc-loi-giai.md`.

## Thứ tự vẫn khóa

1. Luật xin đáp án / gợi ý / sai chỗ (`lib/tutor.ts`).
2. Thang 3 cấp đã kiểm — không bottom-out (VanLehn, Aleven).
3. Nhà đã **chọn tường minh** — không đoán.
4. Lọc SymPy (`POST /v1/filter`) trên **cả câu**. Vì thế không phát luồng từng token.

Demo mặc định `offline`. Không khóa API vẫn làm bài được.

## Nhà

| id | Việc | Địa chỉ | Khóa |
| --- | --- | --- | --- |
| `offline` | Câu mẫu + thang gợi ý | không gọi mạng | không |
| `cloud` | OpenAI-compatible chính thức | `LLM_BASE_URL` (https, hoặc http loopback) | `LLM_API_KEY` hoặc khóa lớp |
| `ollama` | Máy này | `127.0.0.1:11434/v1` | dummy `ollama` |
| `lmstudio` | Máy này | `127.0.0.1:1234/v1` | dummy `lm-studio` |

Học sinh được chọn offline luôn; Ollama/LM Studio nếu lớp bật `ai_allow_local`; cloud chỉ khi lớp chọn cloud.

## Luật an toàn (học từ PR nội bộ + phần mềm mở)

Tham chiếu: [meiiie/lms-ibm-bob-hackathon#3](https://github.com/meiiie/lms-ibm-bob-hackathon/pull/3) (ChatGPT tùy chọn + Ollama/LM Studio), [Ollama OpenAI compat](https://github.com/ollama/ollama/blob/main/docs/openai.md), [LM Studio local server](https://lmstudio.ai/docs/app/api), [Open WebUI](https://github.com/open-webui/open-webui), [Continue](https://github.com/continuedev/continue), [LiteLLM](https://github.com/BerriAI/litellm).

- **Không fallback thầm.** Lỗi cloud/local trả lời bằng chữ «không chuyển nhà / không gửi lại». Không lấy `offlineText` thay cho mô hình.
- **Không phát lại** cùng một HTTP. Timeout 20 s (chat) / 8 s (probe). Không queue, không backoff.
- **Local = loopback.** `10.x`, `192.168.x`, metadata `169.254.169.254` bị từ chối. Không quét LAN, không mượn cookie LMS.
- **Không device-OAuth ChatGPT.** PR #3 dùng `client_id` nội bộ (`app_EMoamEEZ73f0CkXaXp7hrann`) — ToS, vỡ im lặng, khóa máy. Ở đây chỉ khóa API chính thức.
- **Lỗi nhà không đăng xuất**, không xóa phiếu, không gửi lại câu hỏi.
- **Probe** chỉ `GET /models`. Không POST chat giả.
- **Khóa** không vào log, không vào prompt học sinh, không trả đủ về trình duyệt (chỉ `••••` + 4 ký tự).
- **Cloud URL** không nhập từ form (chống SSRF). Chỉ env.

## Composer (ChatGPT / Claude / Open WebUI, phiếu)

- `textarea` cao tối thiểu 44, Enter gửi, Shift+Enter xuống dòng, bỏ qua IME (gõ tiếng Việt).
- Nút Gửi **44×44**. Khi đang nghĩ → Dừng (tăng `requestId`, bỏ qua kết quả cũ).
- Nháp `sessionStorage`. Không phát luồng — hiện «Đang nghĩ…» rồi cả câu sau lọc.
- Chip gợi ý / sai chỗ / gửi thầy cô giữ nguyên.

## Mã

| Đường | Việc |
| --- | --- |
| `apps/web/lib/ai-catalog.ts` | Nhãn, loopback, chọn nhà — an toàn cho client |
| `apps/web/lib/ai-harness.ts` | `completeChat` / `probeProvider` — một lần HTTP |
| `apps/web/lib/llm.ts` | Xóa PII + ghi `llm_calls` |
| `apps/web/components/tutor-panel.tsx` | Composer |
| `apps/web/app/gv/cai-dat/page.tsx` | Chọn nhà, khóa lớp, thử kết nối |

## Kết nối ChatGPT (người không chuyên)

Giáo viên vào `/gv/ket-noi-ai`. Hai bước: mở trang khóa OpenAI (cùng tài khoản ChatGPT) → dán một lần. Học sinh không thấy khóa.

«Sign in with ChatGPT» chính thức (2026) chỉ là định danh. Bật khi có `OPENAI_OAUTH_CLIENT_ID` do OpenAI cấp. Không dùng client_id Codex/CLI.

## Kho kiến thức

`lib/kien-thuc.ts` + `lib/kho-lop.ts`: truy hồi tài liệu / công thức theo cụm từ (cùng luật tầng 2/3). Nhét vào prompt gia sư. Học sinh xem `/hs/kho`. Không lời giải.

```bash
pnpm --filter web test:unit
```
