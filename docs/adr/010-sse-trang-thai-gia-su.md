# ADR 010 — SSE trạng thái gia sư, không stream token

## Bối cảnh

Cột gia sư chờ cả lượt (5–15 s FlashX) rồi mới hết «Đang nghĩ…». Dừng chỉ tăng `seq`, request vẫn chạy. Học sinh không biết đang mở kho, gọi nhà, hay lọc.

Tham khảo (không copy mã AGPL): [meiiie/wiii](https://github.com/meiiie/wiii) SSE event rồi câu đủ + cùng guardrail sync/stream; Open WebUI / NekoCore-OS `step` → `done`. Khóa ADR 007: không stream token vì lọc cần cả câu.

## Quyết định

- `POST /api/hs/gia-su` trả `text/event-stream`: `trang_thai` (`kho`|`goi`|`loc`) rồi `xong`.
- Cùng `chayHoiGiaSu` với server action — không lệch lọc.
- Dừng = `AbortController` trên fetch và trên `completeChat`. Escape cùng việc đó.
- Cột phải dính, nhật ký co, composer đáy. Không bong bóng gradient.
- SSE 401/lỗi/`!body`: trả chữ lỗi. Không fallback thầm sang server action (tránh gọi model lần hai).

## Hệ quả

Học sinh thấy bước đang làm. Câu chỉ hiện sau lọc. Token thinking của nhà không lên UI.
