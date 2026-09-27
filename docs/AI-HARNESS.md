# Harness gia sư AI

Quyết định: `docs/adr/007-ai-providers.md`. Sư phạm: `docs/adr/003-gia-su-khong-doc-loi-giai.md`.

## Thứ tự vẫn khóa

1. Luật xin đáp án / gợi ý / sai chỗ (`lib/tutor.ts`).
2. Thang 3 cấp đã kiểm — không bottom-out (VanLehn, Aleven).
3. Nhà đã **chọn tường minh** — không đoán.
4. Lọc SymPy (`POST /v1/filter`) trên **cả câu**. Vì thế **không stream token**. Lớp chính M3 (số phải dính ngữ cảnh đáp án), lớp phụ M1 (chuỗi LaTeX). Không dùng M2 giá trị trần — tránh chặn «giảm số mũ đi 1».
5. **SSE trạng thái** (`POST /api/hs/gia-su`): event `trang_thai` (`kho` → `goi` → `loc`) rồi `xong` với câu đã lọc. Nhịp Wiii / Open WebUI (event rồi câu đủ) — không copy mã AGPL. Cùng hàm `chayHoiGiaSu` với server action (không lệch lọc). Dừng = AbortController, không gửi lại.

Demo mặc định `offline`. Không khóa API vẫn làm bài được.

## Nhà

| id | Việc | Địa chỉ | Khóa |
| --- | --- | --- | --- |
| `offline` | Câu mẫu + thang gợi ý | không gọi mạng | không |
| `cloud` | OpenAI-compatible chính thức | `LLM_BASE_URL` (https, hoặc http loopback) | `LLM_API_KEY` hoặc khóa lớp |
| `openrouter` | Khóa lập trình OpenRouter | `https://openrouter.ai/api/v1` (cứng) | `OPENROUTER_API_KEY` hoặc khóa lớp |
| `zai` | Khóa coding Z.AI | `https://api.z.ai/api/coding/paas/v4` (cứng, không `/api/paas/v4`) | `ZAI_API_KEY` hoặc khóa lớp |
| `ollama` | Máy này | `127.0.0.1:11434/v1` | dummy `ollama` |
| `lmstudio` | Máy này | `127.0.0.1:1234/v1` | dummy `lm-studio` |

Học sinh được chọn offline luôn; Ollama/LM Studio nếu lớp bật `ai_allow_local`; nhà khóa (ChatGPT / OpenRouter / Z.AI) chỉ khi lớp chọn đúng nhà đó.

## Luật an toàn (học từ PR nội bộ + phần mềm mở)

Tham chiếu: [meiiie/lms-ibm-bob-hackathon#3](https://github.com/meiiie/lms-ibm-bob-hackathon/pull/3) (ChatGPT tùy chọn + Ollama/LM Studio), [Ollama OpenAI compat](https://github.com/ollama/ollama/blob/main/docs/openai.md), [LM Studio local server](https://lmstudio.ai/docs/app/api), [Open WebUI](https://github.com/open-webui/open-webui), [Continue](https://github.com/continuedev/continue), [LiteLLM](https://github.com/BerriAI/litellm).

- **Không fallback thầm.** Lỗi cloud/local trả lời bằng chữ «không chuyển nhà / không gửi lại». Không lấy `offlineText` thay cho mô hình.
- **Không phát lại** cùng một HTTP. Timeout 30 s (chat) / 8 s (probe). `max_tokens` 1600 (FlashX/thinking hết 400 chỉ cho reasoning). Không queue, không backoff.
- **Khóa lớp theo đúng nhà.** `docKhoaNha` chỉ dùng khóa dán khi lớp đang chọn nhà đó (hoặc khi đang dán/probe nhà đang nối). Khóa Z.AI không gửi sang OpenAI/OpenRouter; cờ `cloudReady` / `openrouterReady` / `zaiReady` tách theo nhà.
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
| `apps/web/lib/gia-su-luot.ts` | Một lượt gia sư (kho → gọi → lọc) — action và SSE dùng chung |
| `apps/web/lib/sse.ts` | Gói / đọc event trạng thái |
| `apps/web/app/api/hs/gia-su/route.ts` | SSE: `trang_thai` rồi `xong` |
| `apps/web/lib/llm.ts` | Xóa PII + ghi `llm_calls` |
| `apps/web/components/tutor-panel.tsx` | Cột phải, composer đáy |
| `apps/web/components/loi-gia-su.tsx` | Markdown + KaTeX flush trái — `\[ \]`, align, hàng rào latex |
| `apps/web/app/gv/cai-dat/page.tsx` | Chọn nhà, khóa lớp, thử kết nối |

## Kết nối ChatGPT (người không chuyên)

Giáo viên vào `/gv/ket-noi-ai` (cũng từ tổng quan). Hai bước cho từng nhà khóa: mở trang khóa chính thức → dán một lần. ChatGPT / OpenRouter / Z.AI. Học sinh thấy «… của lớp», không thấy khóa. OpenRouter mặc định `qwen/qwen3-coder`; Z.AI mặc định `glm-5.3-flashx` trên endpoint coding. Không nhập URL.

«Sign in with ChatGPT» chính thức (help.openai.com, 2026) chỉ là định danh cho đối tác có `client_id`. Không cấp quyền gọi mô hình. Codex CLI «sign in with ChatGPT» dùng endpoint nội bộ — không sao chép.

## Kho kiến thức

`lib/kien-thuc.ts` + `lib/kho-lop.ts`: truy hồi theo cụm từ, cộng trọng số bước, khớp không dấu (cùng luật tầng 2/3). Nhét vào prompt; nhắc tên mục đã lấy (Kong IJCAI 2026 D2; KITE BEA 2026). Giáo viên xem trước trên `/gv/ket-noi-ai` và `/gv/tai-lieu`. Học sinh xem `/hs/kho`. Không lời giải.

```bash
pnpm --filter web test:unit
```
